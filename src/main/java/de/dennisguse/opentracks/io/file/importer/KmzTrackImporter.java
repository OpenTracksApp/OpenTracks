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

import de.dennisguse.opentracks.content.data.Waypoint;
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

    public static final List<String> KMZ_IMAGES_EXT = Arrays.asList("jpeg", "jpg", "png");

    private static final int BUFFER_SIZE = 4096;

    private final Context context;
    private final long importTrackId;
    private Uri uriKmzFile;

    /**
     * Constructor.
     *
     * @param context       the context
     * @param importTrackId track id to import to. This should not be -1L so that images in the kmz file can be imported.
     * @param uriFile       URI of the kmz file.
     */
    KmzTrackImporter(Context context, long importTrackId, Uri uriFile) {
        this.context = context;
        this.importTrackId = importTrackId;
        this.uriKmzFile = uriFile;
    }

    @Override
    public long importFile(InputStream inputStream) {
        long trackId;

        if (!copyKmzImages()) {
            cleanImport(context, importTrackId);
            return -1L;
        }

        trackId = findAndParseKmlFile(inputStream);
        if (trackId == -1L) {
            cleanImport(context, importTrackId);
            return -1L;
        }

        deleteOrphanImages(context, trackId);

        return trackId;
    }

    /**
     * Copies all images that are inside KMZ to OpenTracks external storage.
     *
     * @return false if there are errors or true otherwise.
     */
    private boolean copyKmzImages() {
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
                    readAndSaveImageFile(zipInputStream, importNameForFilename(fileName));
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
    private boolean hasImageExtension(String fileName)  {
        if (fileName == null) {
            return false;
        }

        String fileExt = FileUtils.getExtension(fileName.toLowerCase());
        if (fileExt == null) {
            return false;
        }

        if (KMZ_IMAGES_EXT.contains(fileExt)) {
            return true;
        }

        return false;
    }

    /**
     * Finds KmzTrackExporter.KMZ_KML_FILE file inside kmz file (inputStream) and it parses it.
     *
     * @param inputStream kmz input stream.
     * @return            -1 if error or the id of the track otherwise.
     */
    private long findAndParseKmlFile(InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            long trackId = -1L;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Thread interrupted");
                    return -1L;
                }

                String fileName = zipEntry.getName();
                if (KmzTrackExporter.KMZ_KML_FILE.equals(fileName)) {
                    trackId = parseKml(zipInputStream);
                    if (trackId == -1L) {
                        Log.d(TAG, "Unable to parse kml in kmz");
                        return -1L;
                    }
                }

                zipInputStream.closeEntry();
            }

            return trackId;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            return -1L;
        }
    }

    /**
     * Deletes all images that remained in external storage that doesn't have waypoint (marker) associated.
     *
     * @param context the Context object.
     * @param trackId the id of the Track.
     */
    private void deleteOrphanImages(Context context, long trackId) {
        if (trackId != 1L) {
            // 1.- Gets all photo names in the waypoints of the track identified by id.
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
            List<Waypoint> waypoints = contentProviderUtils.getWaypoints(trackId);
            List<String> photosName = new ArrayList<>();
            for (Waypoint w : waypoints) {
                if (w.hasPhoto()) {
                    String photoUrl = Uri.decode(w.getPhotoUrl());
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
    }

    /**
     * Cleans up import.
     *
     * @param trackId the trackId
     */
    private void cleanImport(Context context, long trackId) {
        if (PreferencesUtils.isRecording(trackId)) {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
            contentProviderUtils.deleteTrack(context, trackId);
        }

        if (importTrackId != -1L) {
            File dir = FileUtils.getPhotoDir(context, importTrackId);
            if (dir.exists() && dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
                dir.delete();
            }
        }
    }

    /**
     * Parses kml
     *
     * @param zipInputStream the zip input stream
     * @return the imported track id or -1L
     */
    private long parseKml(ZipInputStream zipInputStream) throws IOException {
        KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context, importTrackId);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(getKml(zipInputStream))) {
            return kmlFileTrackImporter.importFile(byteArrayInputStream);
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
     * @param fileName       the file name
     */
    private void readAndSaveImageFile(ZipInputStream zipInputStream, String fileName) throws IOException {
        if (importTrackId == -1L || fileName.equals("")) {
            return;
        }

        File dir = FileUtils.getPhotoDir(context, importTrackId);
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
