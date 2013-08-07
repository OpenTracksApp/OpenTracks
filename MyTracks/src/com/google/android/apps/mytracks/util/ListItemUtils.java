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
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

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
   * @param sharedOwner if shared with me track, the owner, else null
   */
  public static void setListItem(Context context, View view, boolean isRecording, boolean isPaused,
      int iconId, int iconContentDescriptionId, String name, String category, String totalTime,
      String totalDistance, long startTime, String description, String sharedOwner) {

    if (isRecording) {
      iconId = isPaused ? R.drawable.ic_track_paused : R.drawable.ic_track_recording;
      iconContentDescriptionId = isPaused ? R.string.icon_pause_recording
          : R.string.icon_record_track;
    }

    ImageView iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
    iconImageView.setImageResource(iconId);
    iconImageView.setContentDescription(context.getString(iconContentDescriptionId));

    TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
    nameTextView.setText(name);

    TextView timeDistanceTextView = (TextView) view.findViewById(R.id.list_item_time_distance);
    if (isRecording) {
      timeDistanceTextView.setTextColor(context.getResources()
          .getColor(isPaused ? android.R.color.white : R.color.recording_text));
    } else {
      // Need to match the style set in list_item.xml
      timeDistanceTextView.setTextAppearance(context, R.style.TextSmall);
    }
    setTextView(timeDistanceTextView,
        getTimeDistance(context, isRecording, isPaused, sharedOwner, totalTime, totalDistance));

    String[] startTimeDisplay = getStartTime(isRecording, context, startTime);
    TextView dateTextView = (TextView) view.findViewById(R.id.list_item_date);
    setTextView(dateTextView, startTimeDisplay[0]);

    TextView timeTextView = (TextView) view.findViewById(R.id.list_item_time);
    setTextView(timeTextView, startTimeDisplay[1]);

    TextView descriptionTextView = (TextView) view.findViewById(R.id.list_item_description);
    setTextView(descriptionTextView, getDescription(isRecording, category, description));
  }

  /**
   * Gets the time/distance text.
   * 
   * @param context the context
   * @param isRecording true if recording
   * @param isPaused true if paused
   * @param sharedOwner the shared owner
   * @param totalTime the total time
   * @param totalDistance the total distance
   */
  private static String getTimeDistance(Context context, boolean isRecording, boolean isPaused,
      String sharedOwner, String totalTime, String totalDistance) {
    if (isRecording) {
      return context.getString(isPaused ? R.string.generic_paused : R.string.generic_recording);
    }
    StringBuffer buffer = new StringBuffer();
    if (sharedOwner != null && sharedOwner.length() != 0) {
      buffer.append(sharedOwner);
    }
    if (totalTime != null && totalTime.length() != 0) {
      if (buffer.length() != 0) {
        buffer.append(" \u2027 ");
      }
      buffer.append(totalTime);
    }
    if (totalDistance != null && totalDistance.length() != 0) {
      if (buffer.length() != 0) {
        buffer.append(" ");
      }
      buffer.append("(" + totalDistance + ")");
    }
    return buffer.toString();
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

    StringBuffer buffer = new StringBuffer();

    buffer.append("[" + category + "]");
    if (description != null && description.length() != 0) {
      buffer.append(" " + description);
    }
    return buffer.toString();
  }

  /**
   * Gets the start time text.
   * 
   * @param isRecording true if recording
   * @param context the context
   * @param startTime the start time
   * @return array of two strings.
   */
  private static String[] getStartTime(boolean isRecording, Context context, long startTime) {
    if (isRecording || startTime == 0L) {
      return new String[] { null, null };
    }
    if (DateUtils.isToday(startTime)) {
      return new String[] { DateUtils.getRelativeTimeSpanString(
          startTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
          DateUtils.FORMAT_ABBREV_RELATIVE).toString(), null };
    }
    int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL;
    if (!isThisYear(startTime)) {
      flags |= DateUtils.FORMAT_NUMERIC_DATE;
    }
    return new String[] { DateUtils.formatDateTime(context, startTime, flags),
        DateUtils.formatDateTime(
            context, startTime, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME) };
  }

  /**
   * True if the time is this year.
   * 
   * @param time the time
   */
  private static boolean isThisYear(long time) {
    Calendar now = Calendar.getInstance();
    Calendar calendar = Calendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    calendar.setTimeInMillis(time);
    return now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR);
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
