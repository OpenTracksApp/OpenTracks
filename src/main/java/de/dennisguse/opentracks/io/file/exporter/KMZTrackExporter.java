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

package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.ui.markers.MarkerUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * KMZ track exporter.
 *
 * @author Jimmy Shih
 */
public class KMZTrackExporter implements TrackExporter {

    private static final String KMZ_IMAGES_DIR = "images";
    public static final String KMZ_KML_FILE = "doc.kml";

    private static final String TAG = KMZTrackExporter.class.getSimpleName();
    private static final int BUFFER_SIZE = 4096;

    private final ContentProviderUtils contentProviderUtils;
    private final KMLTrackExporter fileTrackExporter;

    private final boolean exportPhotos;
    private final Context context;

    public KMZTrackExporter(Context context, ContentProviderUtils contentProviderUtils, KMLTrackExporter trackExporter, boolean exportPhotos) {
        this.context = context;
        this.contentProviderUtils = contentProviderUtils;
        this.fileTrackExporter = trackExporter;
        this.exportPhotos = exportPhotos;
    }

    @Override
    public boolean writeTrack(List<Track> tracks, @NonNull OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            // Add kml file
            ZipEntry zipEntry = new ZipEntry(KMZ_KML_FILE);
            zipOutputStream.putNextEntry(zipEntry);

            boolean success = fileTrackExporter.writeTrack(tracks, zipOutputStream);
            zipOutputStream.closeEntry();
            if (!success) {
                Log.e(TAG, "Unable to write kml in kmz");
                return false;
            }

            // Add photos
            if (exportPhotos) addImages(context, tracks ,zipOutputStream);
            return true;
        } catch (InterruptedException | IOException e) {
            Log.e(TAG, "Unable to write track", e);
            return false;
        }
    }

    private void addImages(Context context, List<Track> tracks, ZipOutputStream zipOutputStream) throws InterruptedException, IOException {
        for (Track track : tracks) {
            try (Cursor cursor = contentProviderUtils.getMarkerCursor(track.getId(), null, -1)) {
                if (cursor != null && cursor.moveToFirst()) {
                    for (int i = 0; i < cursor.getCount(); i++) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        Marker marker = contentProviderUtils.createMarker(cursor);
                        if (marker.hasPhoto()) {
                            Uri uriPhoto = marker.getPhotoUrl();
                            boolean existsPhoto = MarkerUtils.buildInternalPhotoFile(context, track.getId(), uriPhoto) != null;
                            if (existsPhoto) {
                                addImage(context, zipOutputStream, uriPhoto, marker);
                            }
                        }

                        cursor.moveToNext();
                    }
                }
            }
        }
    }

    private void addImage(Context context, ZipOutputStream zipOutputStream, Uri uri, Marker marker) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            ZipEntry zipEntry = new ZipEntry(buildKmzImageFilePath(marker));
            zipOutputStream.putNextEntry(zipEntry);

            if (inputStream == null) throw new FileNotFoundException();

            readToOutputStream(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();

            Log.i(TAG, "added an image to zip");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "could not get image via FileProvider via uri " + uri);
        }
    }

    private void readToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int byteCount;
        while ((byteCount = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, byteCount);
        }
    }

    /**
     * Builds and returns the path for the image that will be saved inside KMZ_IMAGES_DIR for the marker.
     */
    public static String buildKmzImageFilePath(Marker marker) {
        String ext = FileUtils.getExtension(marker.getPhotoUrl());
        ext = ext == null ? "" : "." + ext;
        return KMZ_IMAGES_DIR + File.separatorChar + FileUtils.sanitizeFileName(marker.getId().id() + ext);
    }
}