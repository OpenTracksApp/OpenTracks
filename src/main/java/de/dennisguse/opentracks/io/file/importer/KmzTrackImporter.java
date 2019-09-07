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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.dennisguse.opentracks.content.ContentProviderUtils;
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

    private static final int BUFFER_SIZE = 4096;

    private final Context context;
    private final long importTrackId;

    /**
     * Constructor.
     *
     * @param context       the context
     * @param importTrackId track id to import to. This should not be -1L so that images in the kmz file can be imported.
     */
    KmzTrackImporter(Context context, long importTrackId) {
        this.context = context;
        this.importTrackId = importTrackId;
    }

    @Override
    public long importFile(InputStream inputStream) {
        long trackId = importTrackId;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "Thread interrupted");
                    cleanImport(trackId);
                    return -1L;
                }
                String fileName = zipEntry.getName();
                if (fileName.equals(KmzTrackExporter.KMZ_KML_FILE)) {
                    trackId = parseKml(zipInputStream);
                    if (trackId == -1L) {
                        Log.d(TAG, "Unable to parse kml in kmz");
                        cleanImport(trackId);
                        return -1L;
                    }
                } else {
                    String prefix = KmzTrackExporter.KMZ_IMAGES_DIR + File.separatorChar;
                    if (fileName.startsWith(prefix)) {
                        readImageFile(zipInputStream, fileName.substring(prefix.length()));
                    }
                }
                zipInputStream.closeEntry();
            }
            return trackId;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            cleanImport(trackId);
            return -1L;
        }
    }

    /**
     * Cleans up import.
     *
     * @param trackId the trackId
     */
    private void cleanImport(long trackId) {
        if (trackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
            ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(context);
            contentProviderUtils.deleteTrack(context, trackId);
        }

        if (importTrackId != -1L) {
            File dir = FileUtils.getPhotoDir(importTrackId);
            if (FileUtils.isDirectory(dir)) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
                dir.delete();
                FileUtils.updateMediaScanner(context, Uri.fromFile(dir));
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
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context, importTrackId);
            byteArrayInputStream = new ByteArrayInputStream(getKml(zipInputStream));
            return kmlFileTrackImporter.importFile(byteArrayInputStream);
        } finally {
            if (byteArrayInputStream != null) {
                byteArrayInputStream.close();
            }
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
     * Reads an image file.
     *
     * @param zipInputStream the zip input stream
     * @param fileName       the file name
     */
    private void readImageFile(ZipInputStream zipInputStream, String fileName) throws IOException {
        if (importTrackId == -1L) {
            return;
        }
        if (fileName.equals("")) {
            return;
        }
        File dir = FileUtils.getPhotoDir(importTrackId);
        FileUtils.ensureDirectoryExists(dir);
        File file = new File(dir, fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = zipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, count);
            }

            FileUtils.updateMediaScanner(context, Uri.fromFile(file));
        }
    }
}
