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
      String totalDistance, String startTime, String description) {

    if (isRecording) {
      iconId = isPaused ? R.drawable.status_paused : R.drawable.status_recording;
      iconContentDescriptionId = isPaused ? R.string.icon_pause_recording
          : R.string.icon_record_track;
    }

    ImageView iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
    iconImageView.setImageResource(iconId);
    iconImageView.setContentDescription(context.getString(iconContentDescriptionId));

    TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
    nameTextView.setText(name);

    TextView categoryTextView = (TextView) view.findViewById(R.id.list_item_category);
    setTextView(categoryTextView, category);

    TextView totalTimeTextView = (TextView) view.findViewById(R.id.list_item_total_time);
    setTextView(totalTimeTextView, isRecording ? null : totalTime);

    TextView totalDistanceTextView = (TextView) view.findViewById(R.id.list_item_total_distance);
    setTextView(totalDistanceTextView, isRecording ? null : totalDistance);

    TextView startTimeTextView = (TextView) view.findViewById(R.id.list_item_start_time);
    setTextView(startTimeTextView, isRecording ? null : startTime);

    TextView recordingTextView = (TextView) view.findViewById(R.id.list_item_recording);
    setTextView(recordingTextView, isRecording ? context.getString(
        isPaused ? R.string.generic_paused : R.string.generic_recording)
        : null);

    TextView descriptionTextView = (TextView) view.findViewById(R.id.list_item_description);
    setTextView(descriptionTextView, isRecording ? null : description);
  }

  /**
   * Sets a text view.
   * 
   * @param textView the text view
   * @param value the value for the text view
   */
  private static void setTextView(TextView textView, String value) {
    if (value == null || value.length() == 0) {
      textView.setVisibility(View.GONE);
    } else {
      textView.setVisibility(View.VISIBLE);
      textView.setText(value);
    }
  }
}
