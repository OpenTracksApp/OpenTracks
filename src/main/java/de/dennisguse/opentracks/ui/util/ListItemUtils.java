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
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import de.dennisguse.opentracks.R;
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
     * Sets a list item.
     *
     * @param context                  the context
     * @param view                     the list item view
     * @param isRecording              true if recording
     * @param iconId                   the icon id
     * @param iconContentDescriptionId the icon content description id
     * @param name                     the name value
     * @param totalTime                the total time value
     * @param totalDistance            the total distance value
     * @param markerCount              the marker count
     * @param offsetDateTime           the start time with offset
     * @param category                 the category value
     * @param description              the description value
     * @param hasPhoto                 true if this list item has photo
     */
    public static void setListItem(Context context, View view, boolean isRecording, int iconId, int iconContentDescriptionId, String name, String totalTime, String totalDistance, int markerCount, OffsetDateTime offsetDateTime, String category, String description, boolean hasPhoto) {
        // Set icon
        if (isRecording) {
            iconId = R.drawable.ic_track_recording;
            iconContentDescriptionId = R.string.image_record;
        }

        ImageView iconImageView = view.findViewById(R.id.list_item_icon);
        iconImageView.setImageResource(iconId);
        iconImageView.setContentDescription(context.getString(iconContentDescriptionId));

        // Set name
        TextView nameTextView = view.findViewById(R.id.list_item_name);
        setTextView(context, nameTextView, name, hasPhoto);

        // Set totalTime/totalDistance
        TextView timeDistanceTextView = view.findViewById(R.id.list_item_time_distance);
        String timeDistanceText;
        if (isRecording) {
            timeDistanceText = context.getString(R.string.generic_recording);
        } else {
            // Match list_item_time_distance in list_item.xml
            timeDistanceText = getTimeDistance(totalTime, totalDistance);
            if (markerCount > 0) {
                timeDistanceText += "  \u2027";
            }
        }
        setTextView(context, timeDistanceTextView, timeDistanceText, hasPhoto);

        // Set markerCount
        ImageView markerCountIcon = view.findViewById(R.id.list_item_marker_count_icon);
        TextView markerCountTextView = view.findViewById(R.id.list_item_marker_count);
        boolean hasMarker = markerCount > 0;
        markerCountIcon.setVisibility(hasMarker ? View.VISIBLE : View.GONE);
        String markerCountValue = hasMarker ? String.valueOf(markerCount) : null;
        if (hasMarker) {
            // Scale markerCountIcon
            int lineHeight = markerCountTextView.getLineHeight();
            LayoutParams layoutParams = markerCountIcon.getLayoutParams();
            layoutParams.width = lineHeight;
            layoutParams.height = lineHeight;
        }
        setTextView(context, markerCountTextView, markerCountValue, hasPhoto);

        // Set date/time
        TextView dateTextView = view.findViewById(R.id.list_item_date);
        TextView timeTextView = view.findViewById(R.id.list_item_time);
        String dateValue = null;
        String timeValue = null;
        if (!isRecording) {
            dateValue = StringUtils.formatDateTodayRelative(context, offsetDateTime);
            String pattern = "HH:mm";
            if (!offsetDateTime.getOffset().equals(OffsetDateTime.now().getOffset())) {
                pattern = "HH:mm x";
            }
            timeValue = offsetDateTime.format(DateTimeFormatter.ofPattern(pattern));
        }
        setTextView(context, dateTextView, dateValue, hasPhoto);
        setTextView(context, timeTextView, timeValue, hasPhoto);

        // Set category and description
        TextView categoryDescriptionTextView = view.findViewById(R.id.list_item_category_description);
        String categoryDescription = isRecording ? null : StringUtils.getCategoryDescription(category, description);

        // Place categoryDescription in either ownerTimeDistanceTextView or categoryDescriptionTextView
        if (timeDistanceTextView.getVisibility() == View.GONE && markerCountIcon.getVisibility() == View.GONE) {
            setTextView(context, categoryDescriptionTextView, null, hasPhoto);
            // Match list_item_category_description in list_item.xml
            timeDistanceTextView.setSingleLine(false);
            timeDistanceTextView.setMaxLines(2);
            setTextView(context, timeDistanceTextView, categoryDescription, hasPhoto);
        } else {
            // Match list_item_time_distance in list_item.xml
            timeDistanceTextView.setSingleLine(true);
            setTextView(context, categoryDescriptionTextView, categoryDescription, hasPhoto);
        }
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
                builder.append(" \u2027 ");
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

    /**
     * Sets a text view.
     *
     * @param context   the context
     * @param textView  the text view
     * @param value     the value for the text view
     * @param addShadow true to add shadow
     */
    public static void setTextView(Context context, TextView textView, String value, boolean addShadow) {
        if (value == null || value.length() == 0) {
            textView.setVisibility(View.GONE);
            return;
        }

        textView.setVisibility(View.VISIBLE);
        textView.setText(value);
        if (addShadow) {
            textView.setShadowLayer(5, 0, 2, ContextCompat.getColor(context, android.R.color.black));
        } else {
            textView.setShadowLayer(0, 0, 0, 0);
        }
    }
}
