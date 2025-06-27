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

package de.dennisguse.opentracks.ui.util;

import android.content.Context;
import android.widget.TextView;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Utilities to display a list item.
 *
 * @author Jimmy Shih
 */
public class ListItemUtils {

    private ListItemUtils() {
    }

    /**
     * Gets a string for share owner, total time, and total distance.
     *
     * @param totalTime     the total time. Can be null
     * @param totalDistance the total distance. Can be null
     */
    private static String getTimeDistance(String totalTime, String totalDistance) {
        StringBuilder builder = new StringBuilder();
        if (totalTime != null && totalTime.length() != 0) {
            if (builder.length() != 0) {
                builder.append(" ‧ ");
            }
            builder.append(totalTime);
        }
        if (totalDistance != null && totalDistance.length() != 0) {
            if (builder.length() != 0) {
                builder.append(" ");
            }
            builder.append("(").append(totalDistance).append(")");
        }
        return builder.toString();
    }

    public static String getTimeDistanceText(Context context, UnitSystem unitSystem, boolean isRecording, Duration totalTime, Distance totalDistance, int markerCount) {
        String timeDistanceText;
        if (isRecording) {
            timeDistanceText = context.getString(R.string.generic_recording);
        } else {
            // Match list_item_time_distance in list_item.xml

            String time = StringUtils.formatElapsedTime(totalTime);
            String distance = DistanceFormatter.Builder()
                    .setUnit(unitSystem)
                    .build(context)
                    .formatDistance(totalDistance);

            timeDistanceText = getTimeDistance(time, distance);
            if (markerCount > 0) {
                timeDistanceText += "  ‧";
            }
        }

        return timeDistanceText;
    }

    public static void setDateAndTime(Context context, TextView dateView, TextView timeView, Instant time, ZoneOffset timeZone) {
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(time, timeZone);
        String dateContent = StringUtils.formatDateTodayRelative(context, offsetDateTime);
        String pattern = "HH:mm";
        if (!offsetDateTime.getOffset().equals(OffsetDateTime.now().getOffset())) {
            pattern = "HH:mm x";
        }
        String timeContent = offsetDateTime.format(DateTimeFormatter.ofPattern(pattern));

        dateView.setText(dateContent);
        timeView.setText(timeContent);
    }
}
