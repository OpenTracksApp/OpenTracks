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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.DescriptionGenerator;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
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
    public static Intent newShareFileIntent(Context context, long[] trackIds) {
        if (trackIds.length == 0) {
            throw new RuntimeException("Need to share at least one track.");
        }

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

        String trackDescription = "";
        if (trackIds.length == 1) {
            Track track = contentProviderUtils.getTrack(trackIds[0]);
            trackDescription = track == null ? "" : new DescriptionGenerator(context).generateTrackDescription(track, false);
        }

        String action = trackIds.length == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE;
        String mime = "";

        ArrayList<Uri> uris = new ArrayList<>();
        for (long trackId : trackIds) {
            Track track = contentProviderUtils.getTrack(trackId);
            if (track == null) {
                Log.e(TAG, "TrackId " + trackId + " could not be resolved.");
            }

            Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(new long[]{trackId}, track.getName(), TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES);
            uris.add(uriAndMime.first);
            mime = uriAndMime.second;
        }
        return new Intent(action)
                .setType(mime)
                .setAction(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_track_share_file_body, trackDescription));
    }

    /**
     * Creates an intent to share a waypoint image with an app.
     *
     * @param context the context.
     * @param uri     uri with the image to share.
     */
    public static Intent newShareImageIntent(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);

        /*
         * Because the #166 bug, when you import KMZ tracks then it creates file:/// from markers with photo.
         * The photos should be content:/// not file:/// because getType(uri) always returns null for file:///
         * In .setType, to avoid side effects because the #166 bug described above it checks if mime is null.
         * If it is then it hardcode "images/*".
         */
        return new Intent(Intent.ACTION_SEND)
                .setType(mime != null ? mime : "image/*")
                .setAction(Intent.ACTION_SEND)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_image_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_image_body));
    }

    public static void showCoordinateOnMap(Context context, Waypoint waypoint) {
        showCoordinateOnMap(context, waypoint.getLocation().getLatitude(), waypoint.getLocation().getLongitude(), waypoint.getName());
    }

    /**
     * Send intent to show coordinates on a map (needs an another app).
     *
     * @param context   the context
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param label     the label
     */
    public static void showCoordinateOnMap(Context context, double latitude, double longitude, String label) {
        //SEE https://developer.android.com/guide/components/intents-common.html#Maps
        String uri = "geo:0,0?q=" + latitude + "," + longitude;
        if (label != null && label.length() > 0) {
            uri += "(" + label + ")";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.app_not_installed_show_on_map), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Send intent to show tracks on a map (needs an another app).
     *
     * @param context  the context
     * @param trackIds the track ids
     */
    public static void showTrackOnMap(Context context, long[] trackIds) {
        if (trackIds.length == 0) {
            return;
        }

        if (IntentDashboardUtils.startDashboard(context, trackIds)) {
            return;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TrackPointsColumns.TRACKID, trackIds[0]);
        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, "SharingTrack", TrackFileFormat.KMZ_WITH_TRACKDETAIL);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.getString(R.string.app_not_installed_show_on_map), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends a take picture request to the camera app.
     * The picture is then stored in the track's folder.
     *
     * @param context the context
     * @param trackId the track id
     */
    public static Pair<Intent, Uri> createTakePictureIntent(Context context, long trackId) {
        File dir = FileUtils.getPhotoDir(context, trackId);

        String fileName = SimpleDateFormat.getDateTimeInstance().format(new Date());
        File file = new File(dir, FileUtils.buildUniqueFileName(dir, fileName, JPEG_EXTENSION));

        Uri photoUri = FileProvider.getUriForFile(context, FileUtils.FILEPROVIDER, file);
        Log.d(TAG, "Taking photo to URI: " + photoUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        return new Pair<>(intent, photoUri);
    }
}
