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
import java.util.Date;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.DescriptionGeneratorImpl;
import de.dennisguse.opentracks.content.ShareContentProvider;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

/**
 * Utilities for creating intents.
 *
 * @author Jimmy Shih
 */
public class IntentUtils {

    private final static String TAG = IntentUtils.class.getCanonicalName();

    private IntentUtils() {
    }

    /**
     * Creates an intent with {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
     *
     * @param context the context
     * @param cls     the class
     */
    public static Intent newIntent(Context context, Class<?> cls) {
        return new Intent(context, cls).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private static final String JPEG_EXTENSION = "jpeg";

    public static Intent newShowCoordinateOnMapIntent(Waypoint waypoint) {
        return newShowCoordinateOnMapIntent(waypoint.getLocation().getLatitude(), waypoint.getLocation().getLongitude(), waypoint.getName());
    }

    /**
     * Creates an intent to share a track file with an app.
     *
     * @param context the context
     * @param trackIds the track ids
     */
    public static Intent newShareFileIntent(Context context, long[] trackIds) {
        String trackDescription = "";
        if (trackIds.length == 1) {
            Track track = ContentProviderUtils.Factory.get(context).getTrack(trackIds[0]);
            trackDescription = track == null ? "" : new DescriptionGeneratorImpl(context).generateTrackDescription(track, false);
        }
        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, TrackFileFormat.KML);

        return new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uriAndMime.first)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_track_share_file_body, trackDescription))
                .setType(uriAndMime.second)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    public static Intent newShowCoordinateOnMapIntent(double latitude, double longitude, String label) {
        //SEE https://developer.android.com/guide/components/intents-common.html#Maps
        String uri = "geo:0,0?q=" + latitude + "," + longitude;
        if (label != null && label.length() > 0) {
            uri += "(" + label + ")";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        return intent;
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

        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(trackIds, TrackFileFormat.KML);
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uriAndMime.first, uriAndMime.second);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app installed that can show the tracks on a map.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends a take picture request to the camera app.
     * The picture is then stored in the track's folder.
     *
     * @param context the context
     * @param trackId the track id
     */
    public static Intent createTakePictureIntent(Context context, long trackId) {
        File dir = FileUtils.getPhotoDir(trackId);
        FileUtils.ensureDirectoryExists(dir);

        String fileName = SimpleDateFormat.getDateTimeInstance().format(new Date());
        File file = new File(dir, FileUtils.buildUniqueFileName(dir, fileName, JPEG_EXTENSION));

        Uri photoUri = FileProvider.getUriForFile(context, FileUtils.FILEPROVIDER, file);
        Log.d(TAG, "Taking photo to URI: " + photoUri);
        return new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
    }
}
