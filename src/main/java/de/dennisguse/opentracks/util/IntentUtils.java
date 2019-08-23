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
import android.util.Pair;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.DescriptionGeneratorImpl;
import de.dennisguse.opentracks.content.ShareContentProvider;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.Waypoint;

/**
 * Utilities for creating intents.
 *
 * @author Jimmy Shih
 */
public class IntentUtils {

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

    /**
     * Creates an intent to share a track file with an app.
     *
     * @param context         the context
     * @param trackId         the track id
     */
    public static Intent newShareFileIntent(Context context, long trackId) {
        Track track = ContentProviderUtils.Factory.get(context).getTrack(trackId);
        String trackDescription = track == null ? "" : new DescriptionGeneratorImpl(context).generateTrackDescription(track, false);

        Pair<Uri, String> uriAndMime = ShareContentProvider.createURI(new long[]{trackId});

        return new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uriAndMime.first)
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_track_share_file_body, trackDescription))
                .setType(uriAndMime.second)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    public static Intent newShowCoordinateOnMapIntent(Waypoint waypoint) {
        return newShowCoordinateOnMapIntent(waypoint.getLocation().getLatitude(), waypoint.getLocation().getLongitude(), waypoint.getName());
    }

    public static Intent newShowCoordinateOnMapIntent(double latitude, double longitude, String label) {
        //SEE https://developer.android.com/guide/components/intents-common.html#Maps
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String uri = "geo:0,0?q=" + latitude + "," + longitude;
        if (label != null && label.length() > 0) {
            uri += "(" + label + ")";
        }
        intent.setData(Uri.parse(uri));
        return intent;
    }
}
