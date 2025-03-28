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
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * Imports a KMZ file.
 *
 * @author Jimmy Shih
 */
public class KMZTrackImporter {

    private static final String TAG = KMZTrackImporter.class.getSimpleName();

    private static final String KML_FILE_EXTENSION = ".kml";

    private static final List<String> KMZ_IMAGES_EXT = List.of("jpeg", "jpg", "png");

    private final Context context;
    private final TrackImporter trackImporter;

    public KMZTrackImporter(Context context, TrackImporter trackImporter) {
        this.context = context;
        this.trackImporter = trackImporter;
    }

    @NonNull
    public List<Track.Id> importFile(Uri fileUri) throws IOException {
        List<Track.Id> trackIds = findAndParseKmlFile(fileUri);

        List<Track.Id> trackIdsWithImages = new ArrayList<>();

        for (Track.Id trackId : trackIds) {
            if (copyKmzImages(fileUri, trackId)) {
                trackIdsWithImages.add(trackId);
                deleteOrphanImages(trackId);
            } else {
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
    private boolean copyKmzImages(Uri uri, Track.Id trackId) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
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

    private List<Track.Id> findAndParseKmlFile(Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            ArrayList<Track.Id> trackIds = new ArrayList<>();

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Thread interrupted");
                    throw new RuntimeException(context.getString(R.string.import_thread_interrupted));
                }

                String fileName = zipEntry.getName();
                if (fileName.endsWith(KML_FILE_EXTENSION)) {
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
        }
    }

    /**
     * Deletes all images that remained in external storage that doesn't have a marker associated.
     *
     * @param trackId the id of the Track.
     */
    private void deleteOrphanImages(Track.Id trackId) {
        // 1.- Gets all photo names in the markers of the track identified by id.
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        List<Marker> markers = contentProviderUtils.getMarkers(trackId);
        List<String> photosName = new ArrayList<>();
        for (Marker marker : markers) {
            if (marker.hasPhoto()) {
                String photoUrl = Uri.decode(marker.getPhotoUrl().toString()); //TODO Why Uri.decode()?
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

    private List<Track.Id> parseKml(ZipInputStream zipInputStream) throws IOException {
        XMLImporter kmlFileTrackImporter = new XMLImporter(new KMLTrackImporter(context, trackImporter));

        InputStream nonClosableInputStream = new FilterInputStream(zipInputStream) {
            @Override
            public void close() {
                // SAX2 always tries close InputStreams; but that would also close our ZIP file.
            }
        };

        return kmlFileTrackImporter.importFile(nonClosableInputStream);
    }

    /**
     * Reads an image file (zipInputStream) and save it in a file called fileName inside photo folder.
     *
     * @param zipInputStream the zip input stream
     * @param trackId        the track's id which image belongs to.
     * @param fileName       the file name
     */
    private void readAndSaveImageFile(ZipInputStream zipInputStream, Track.Id trackId, String fileName) throws IOException {
        if (trackId == null || "".equals(fileName)) {
            return;
        }

        File dir = FileUtils.getPhotoDir(context, trackId);
        File file = new File(dir, fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                zipInputStream.transferTo(fileOutputStream);
            } else {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = zipInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, count);
                }
            }
        }
    }
}
