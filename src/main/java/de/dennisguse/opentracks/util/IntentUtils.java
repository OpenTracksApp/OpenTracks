/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.DescriptionGenerator;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.ShareContentProvider;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

/**
 * Utilities for creating intents.
 *
 * @author Jimmy Shih
 */
public class IntentUtils {

    private static final String TAG = IntentUtils.class.getSimpleName();

    private static final String JPEG_EXTENSION = "jpeg";

    private IntentUtils() {
    }

    /**
     * Creates an intent with {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
     *
     * @param context the context
     * @param cls     the class
     */
    public static Intent newIntent(Context context, Class<?> cls) {
        return new Intent(context, cls).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Creates an intent to share a track file with an app.
     *
     * @param context  the context
     * @param trackIds the track ids
     */
    public static Intent newShareFileIntent(Context context, Track.Id... trackIds) {
        if (trackIds.length == 0) {
            throw new RuntimeException("Need to share at least one track.");
        }

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

        String trackDescription = "";
        if (trackIds.length == 1) {
            Track track = contentProviderUtils.getTrack(trackIds[0]);
            trackDescription = track == null ? "" : new DescriptionGenerator(context).generateTrackDescription(track, false);
        }

        ArrayList<Uri> uris = new ArrayList<>();
        for (Track.Id trackId : trackIds) {
            Track track = contentProviderUtils.getTrack(trackId);
            if (track == null) {
                Log.e(TAG, "TrackId " + trackId.getId() + " could not be resolved.");
                continue;
            }

            Pair<Uri, String> uriTrackFile = ShareContentProvider.createURI(trackId, track.getName(), PreferencesUtils.getExportTrackFileFormat(PreferencesUtils.getSharedPreferences(context), context));
            Pair<Uri, String> uriSharePicture = ShareContentProvider.createURI(trackId, track.getName(), TrackFileFormat.SHARE_PICTURE_PNG);

            uris.addAll(Arrays.asList(uriSharePicture.first, uriTrackFile.first));
        }

        return new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_track_share_file_body, trackDescription));
    }

    /**
     * Creates an intent to share a track file with an app.
     *
     * @param context   the context
     * @param markerIds the marker ids
     * @return an Intent or null (if nothing can be shared).
     */
    @Nullable
    public static Intent newShareFileIntent(Context context, Marker.Id... markerIds) {
        if (markerIds.length == 0) {
            throw new RuntimeException("Need to share at least one marker.");
        }

        String mime = null;

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        ArrayList<Uri> uris = new ArrayList<>();
        for (Marker.Id markerId : markerIds) {
            Marker marker = contentProviderUtils.getMarker(markerId);
            if (marker == null) {
                Log.e(TAG, "MarkerId " + markerId.getId() + " could not be resolved.");
                continue;
            }
            if (marker.getPhotoURI() == null) {
                Log.e(TAG, "MarkerId " + markerId.getId() + " has no picture.");
                continue;
            }

            mime = context.getContentResolver().getType(marker.getPhotoURI());

            uris.add(marker.getPhotoURI());
        }

        if (uris.isEmpty()) {
            return null;
        }

        /*
         * Because the #166 bug, when you import KMZ tracks then it creates file:/// from markers with photo.
         * The photos should be content:/// not file:/// because getType(uri) always returns null for file:///
         * In .setType, to avoid side effects because the #166 bug described above it checks if mime is null.
         * If it is then it hardcode "images/*".
         */
        return new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType(mime != null ? mime : "image/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_image_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_image_body));
    }

    public static void showCoordinateOnMap(Context context, Marker marker) {
        showCoordinateOnMap(context, marker.getLatitude(), marker.getLongitude(), marker.getName());
    }

    /**
     * Send intent to show coordinates on a map (needs an another app).
     *
     * @param context   the context
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param label     the label
     */
    private static void showCoordinateOnMap(Context context, double latitude, double longitude, String label) {
        //SEE https://developer.android.com/guide/components/intents-common.html#Maps
        String uri = "geo:0,0?q=" + latitude + "," + longitude;
        if (label != null && label.length() > 0) {
            uri += "(" + label + ")";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));


        context.startActivity(Intent.createChooser(intent, null));
    }


    /**
     * Sends a take picture request to the camera app.
     * The picture is then stored in the track's folder.
     *
     * @param context the context
     * @param trackId the track id
     */
    public static Pair<Intent, Uri> createTakePictureIntent(Context context, Track.Id trackId) {
        File dir = FileUtils.getPhotoDir(context, trackId);

        String fileName = SimpleDateFormat.getDateTimeInstance().format(new Date());
        File file = new File(dir, FileUtils.buildUniqueFileName(dir, fileName, JPEG_EXTENSION));

        Uri photoUri = FileProvider.getUriForFile(context, FileUtils.FILEPROVIDER, file);
        Log.d(TAG, "Taking photo to URI: " + photoUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        return new Pair<>(intent, photoUri);
    }

    public static void persistDirectoryAccessPermission(Context context, Uri directoryUri) {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        context.getContentResolver().takePersistableUriPermission(directoryUri, flags);
    }
}
