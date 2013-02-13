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

package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Utilities to display a list item.
 * 
 * @author Jimmy Shih
 */
public class ListItemUtils {

  private ListItemUtils() {}

  /**
   * Sets a list item.
   * 
   * @param context the context
   * @param view the list item view
   * @param isRecording true if recording
   * @param isPaused true if paused
   * @param name the name value
   * @param iconId the icon id
   * @param iconContentDescriptionId the icon content description id
   * @param category the category value
   * @param totalTime the total time value
   * @param totalDistance the total distance value
   * @param startTime the start time value
   * @param description the description value
   */
  public static void setListItem(Context context, View view, boolean isRecording, boolean isPaused,
      int iconId, int iconContentDescriptionId, String name, String category, String totalTime,
      String totalDistance, long startTime, String description) {

    if (isRecording) {
      iconId = isPaused ? R.drawable.track_paused : R.drawable.track_recording;
      iconContentDescriptionId = isPaused ? R.string.icon_pause_recording
          : R.string.icon_record_track;
    }

    ImageView iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
    iconImageView.setImageResource(iconId);
    iconImageView.setContentDescription(context.getString(iconContentDescriptionId));

    TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
    nameTextView.setText(name);

    TextView timeDistanceTextView = (TextView) view.findViewById(R.id.list_item_time_distance);
    setTextView(timeDistanceTextView, getTimeDistance(isRecording, totalTime, totalDistance), 0);

    TextView startTimeTextView = (TextView) view.findViewById(R.id.list_item_start_time);
    setTextView(
        startTimeTextView, isRecording ? null : StringUtils.formatTime(context, startTime), 0);

    TextView recordingTextView = (TextView) view.findViewById(R.id.list_item_recording);
    String value = isRecording ? context.getString(
        isPaused ? R.string.generic_paused : R.string.generic_recording)
        : null;
    int color = isRecording ? context.getResources()
        .getColor(isPaused ? android.R.color.white : R.color.red)
        : 0;
    setTextView(recordingTextView, value, color);

    TextView descriptionTextView = (TextView) view.findViewById(R.id.list_item_description);
    setTextView(descriptionTextView, getDescription(isRecording, category, description), 0);
  }

  /**
   * Gets the time/distance text.
   * 
   * @param isRecording true if recording
   * @param totalTime the total time
   * @param totalDistance the total distance
   */
  private static String getTimeDistance(boolean isRecording, String totalTime, String totalDistance) {
    if (isRecording) {
      return null;
    }
    if (totalDistance == null || totalDistance.length() == 0) {
      return totalTime;
    }
    if (totalTime == null || totalTime.length() == 0) {
      return totalDistance;
    }
    return totalTime + " (" + totalDistance + ")";
  }
  
  /**
   * Gets the description text.
   * 
   * @param isRecording true if recording
   * @param category the category
   * @param description the description
   */
  private static String getDescription(boolean isRecording, String category, String description) {
    if (isRecording) {
      return null;
    }
    if (category == null || category.length() == 0) {
      return description;
    }
    if (description == null || description.length() == 0) {
      return category;
    }
    return "[" + category + "] " + description;
  }
  
  /**
   * Sets a text view.
   * 
   * @param textView the text view
   * @param value the value for the text view
   */
  private static void setTextView(TextView textView, String value, int color) {
    if (value == null || value.length() == 0) {
      textView.setVisibility(View.GONE);
    } else {
      textView.setVisibility(View.VISIBLE);
      textView.setText(value);
      if (color != 0) {
        textView.setTextColor(color);
      }
    }
  }
}
