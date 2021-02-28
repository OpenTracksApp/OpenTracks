/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.KmzTrackExporter;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Imports a KMZ file.
 *
 * @author Jimmy Shih
 */
public class KmzTrackImporter implements TrackImporter {

    private static final String TAG = KmzTrackImporter.class.getSimpleName();

    private static final List<String> KMZ_IMAGES_EXT = Arrays.asList("jpeg", "jpg", "png");

    private static final int BUFFER_SIZE = 4096;

    private final Context context;
    private final Uri uriKmzFile;

    /**
     * @param context the context
     * @param uriFile URI of the kmz file.
     */
    public KmzTrackImporter(Context context, Uri uriFile) {
        this.context = context;
        this.uriKmzFile = uriFile;
    }

    @Override
    public @NonNull
    List<Track.Id> importFile(InputStream inputStream) {
        List<Track.Id> trackIds = findAndParseKmlFile(inputStream);

        ArrayList<Track.Id> trackIdsWithImages = new ArrayList<>();

        for (Track.Id trackId : trackIds) {
            if (copyKmzImages(trackId)) {
                trackIdsWithImages.add(trackId);
                deleteOrphanImages(context, trackId);
            } else {
                cleanImport(context, trackId);
                return new ArrayList<>();
            }
        }

        return trackIdsWithImages;
    }

    /**
     * Copies all images that are inside KMZ to OpenTracks external storage.
     *
     * @return false if there are errors or true otherwise.
     */
    private boolean copyKmzImages(Track.Id trackId) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uriKmzFile);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Thread interrupted");
                    return false;
                }

                String fileName = zipEntry.getName();
                if (hasImageExtension(fileName)) {
                    readAndSaveImageFile(zipInputStream, trackId, importNameForFilename(fileName));
                }

                zipInputStream.closeEntry();
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            return false;
        }
    }

    /**
     * From path fileName generates an import unique name and returns it.
     * The name generator is simple: change the path fileName with '-' instead of File.separatorChar.
     *
     * @param fileName the file name.
     */
    public static String importNameForFilename(String fileName) {
        // TODO this tricky code for maintain backward compatibility must be deleted some day.
        /*
         * In versions before v3.5.0 photo URL in KML files were wrong.
         * For compatibility reasons it checks if fileName begins with "content://" or "file://".
         * All fileName begins with "content:/" or "file://" are cooked.
         * We cannot guess what's the folder name where images are so we use "images" that was the folder name expected in versions before v3.5.0.
         */
        if (fileName.startsWith("content://") || fileName.startsWith("file://")) {
            fileName = "images/" + fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
        }

        return fileName.replace(File.separatorChar, '-');
    }

    /**
     * Returns true if fileName ends with some of the KMZ_IMAGES_EXT suffixes.
     * Otherwise returns false.
     */
    private boolean hasImageExtension(String fileName) {
        if (fileName == null) {
            return false;
        }

        String fileExt = FileUtils.getExtension(fileName.toLowerCase());
        if (fileExt == null) {
            return false;
        }

        return KMZ_IMAGES_EXT.contains(fileExt);
    }

    /**
     * Finds KmzTrackExporter.KMZ_KML_FILE file inside kmz file (inputStream) and it parses it.
     *
     * @param inputStream kmz input stream.
     */
    private List<Track.Id> findAndParseKmlFile(InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            ArrayList<Track.Id> trackIds = new ArrayList<>();

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Thread interrupted");
                    throw new RuntimeException(context.getString(R.string.import_thread_interrupted));
                }

                String fileName = zipEntry.getName();
                if (KmzTrackExporter.KMZ_KML_FILE.equals(fileName)) {
                    List<Track.Id> trackId = parseKml(zipInputStream);
                    if (trackId.isEmpty()) {
                        Log.d(TAG, "Unable to parse kml in kmz");
                        throw new ImportParserException(context.getString(R.string.import_unable_to_import_file, fileName));
                    }
                    trackIds.addAll(trackId);
                }

                zipInputStream.closeEntry();
            }
            if (trackIds.isEmpty()) {
                Log.d(TAG, "Unable to find doc.kml in kmz");
                throw new ImportParserException(context.getString(R.string.import_no_kml_file_found));
            }
            return trackIds;
        } catch (ImportParserException | ImportAlreadyExistsException e) {
            Log.e(TAG, "Unable to import file", e);
            throw e;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            throw new ImportParserException(e);
        }
    }

    /**
     * Deletes all images that remained in external storage that doesn't have a marker associated.
     *
     * @param context the Context object.
     * @param trackId the id of the Track.
     */
    private void deleteOrphanImages(Context context, Track.Id trackId) {
        // 1.- Gets all photo names in the markers of the track identified by id.
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        List<Marker> markers = contentProviderUtils.getMarkers(trackId);
        List<String> photosName = new ArrayList<>();
        for (Marker marker : markers) {
            if (marker.hasPhoto()) {
                String photoUrl = Uri.decode(marker.getPhotoUrl());
                photosName.add(photoUrl.substring(photoUrl.lastIndexOf(File.separatorChar) + 1));
            }
        }

        // 2.- Deletes all orphan photos from external storage.
        File dir = FileUtils.getPhotoDir(context, trackId);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!photosName.contains(file.getName())) {
                    file.delete();
                }
            }
            if (dir.listFiles().length == 0) {
                dir.delete();
            }
        }
    }

    /**
     * Cleans up import.
     *
     * @param trackId the trackId
     */
    private void cleanImport(Context context, Track.Id trackId) {
        if (PreferencesUtils.isRecording(trackId)) {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
            contentProviderUtils.deleteTrack(context, trackId);
        }
    }

    private List<Track.Id> parseKml(ZipInputStream zipInputStream) {
        KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(getKml(zipInputStream))) {
            return kmlFileTrackImporter.importFile(byteArrayInputStream);
        } catch (ImportParserException | ImportAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportParserException(e);
        }
    }

    /**
     * Gets the kml as byte array.
     *
     * @param zipInputStream the zip input stream
     */
    private byte[] getKml(ZipInputStream zipInputStream) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = zipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, count);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * Reads an image file (zipInputStream) and save it in a file called fileName inside photo folder.
     *
     * @param zipInputStream the zip input stream
     * @param trackId        the track's id which image belongs to.
     * @param fileName       the file name
     */
    private void readAndSaveImageFile(ZipInputStream zipInputStream, Track.Id trackId, String fileName) throws IOException {
        if (trackId == null || fileName.equals("")) {
            return;
        }

        File dir = FileUtils.getPhotoDir(context, trackId);
        File file = new File(dir, fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = zipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, count);
            }
        }
    }
}
