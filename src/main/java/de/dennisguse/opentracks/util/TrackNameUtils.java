/*
 * Copyright 2010 Google Inc.
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

import androidx.annotation.VisibleForTesting;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.dennisguse.opentracks.R;

/**
 * Utilities for track name.
 *
 * @author Matthew Simmons
 */
public class TrackNameUtils {

    //TODO Could be available in java.time?
    @VisibleForTesting
    static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";

    private TrackNameUtils() {
    }

    /**
     * Gets the track name.
     *
     * @param context   the context
     * @param trackId   the track id
     * @param startTime the track start time
     */
    public static String getTrackName(Context context, long trackId, long startTime) {
        String trackName = PreferencesUtils.getString(context, R.string.track_name_key, PreferencesUtils.TRACK_NAME_DEFAULT);

        if (trackName.equals(context.getString(R.string.settings_recording_track_name_date_local_value))) {
            return StringUtils.formatDateTime(context, startTime);
        } else if (trackName.equals(context.getString(R.string.settings_recording_track_name_date_iso_8601_value))) {
            return new SimpleDateFormat(ISO_8601_FORMAT, Locale.US).format(startTime);
        } else {
            return context.getString(R.string.track_name_format, trackId);
        }
    }
}
