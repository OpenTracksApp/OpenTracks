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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Utilities to display a list item.
 * 
 * @author Jimmy Shih
 */
public class ListItemUtils {

  private static final int LIST_PREFERRED_ITEM_HEIGHT_DEFAULT = 128;

  private ListItemUtils() {}

  /**
   * Sets a list item.
   * 
   * @param activity the activity
   * @param view the list item view
   * @param isRecording true if recording
   * @param isPaused true if paused
   * @param iconId the icon id
   * @param iconContentDescriptionId the icon content description id
   * @param name the name value
   * @param sharedOwner if shared with me track, the owner, else null
   * @param totalTime the total time value
   * @param totalDistance the total distance value
   * @param markerCount the marker count
   * @param startTime the start time value
   * @param category the category value
   * @param description the description value
   * @param photoUrl the photo url
   */
  @SuppressWarnings("deprecation")
  public static void setListItem(Activity activity, View view, boolean isRecording,
      boolean isPaused, int iconId, int iconContentDescriptionId, String name, String sharedOwner,
      String totalTime, String totalDistance, int markerCount, long startTime, String category,
      String description, String photoUrl) {

    if (isRecording) {
      iconId = isPaused ? R.drawable.ic_track_paused : R.drawable.ic_track_recording;
      iconContentDescriptionId = isPaused ? R.string.icon_pause_recording
          : R.string.icon_record_track;
    }

    ImageView iconImageView = (ImageView) view.findViewById(R.id.list_item_icon);
    iconImageView.setImageResource(iconId);
    iconImageView.setContentDescription(activity.getString(iconContentDescriptionId));

    // Set name
    TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
    nameTextView.setText(name);

    // Set sharedOwner/totalTime/totalDistance
    TextView ownerTimeDistanceTextView = (TextView) view.findViewById(
        R.id.list_item_owner_time_distance);
    String ownerTimeDistance;
    if (isRecording) {
      ownerTimeDistanceTextView.setTextColor(activity.getResources()
          .getColor(isPaused ? android.R.color.white : R.color.recording_text));
      ownerTimeDistance = activity.getString(
          isPaused ? R.string.generic_paused : R.string.generic_recording);
    } else {
      // Match list_item_owner_time_distance in list_item.xml
      ownerTimeDistanceTextView.setTextAppearance(activity, R.style.TextSmall);
      ownerTimeDistance = getOwnerTimeDistance(sharedOwner, totalTime, totalDistance);
      if (markerCount > 0) {
        ownerTimeDistance += "  \u2027";
      }
    }
    setTextView(ownerTimeDistanceTextView, ownerTimeDistance);

    // Set markerCount
    ImageView markerCountIcon = (ImageView) view.findViewById(R.id.list_item_marker_count_icon);
    TextView markerCountTextView = (TextView) view.findViewById(R.id.list_item_marker_count);
    if (markerCount > 0) {
      markerCountIcon.setVisibility(View.VISIBLE);
      markerCountTextView.setVisibility(View.VISIBLE);

      int lineHeight = markerCountTextView.getLineHeight();
      LayoutParams layoutParams = markerCountIcon.getLayoutParams();
      layoutParams.width = lineHeight;
      layoutParams.height = lineHeight;

      markerCountTextView.setText(String.valueOf(markerCount));
    } else {
      markerCountIcon.setVisibility(View.GONE);
      markerCountTextView.setVisibility(View.GONE);
    }

    // Set date/time
    String[] dateTime = getDateTime(isRecording, activity, startTime);
    TextView dateTextView = (TextView) view.findViewById(R.id.list_item_date);
    setTextView(dateTextView, dateTime[0]);

    TextView timeTextView = (TextView) view.findViewById(R.id.list_item_time);
    setTextView(timeTextView, dateTime[1]);

    // Set category and description
    TextView categoryDescriptionTextView = (TextView) view.findViewById(
        R.id.list_item_category_description);
    String categoryDescription = isRecording ? null
        : StringUtils.getCategoryDescription(category, description);
    if (sharedOwner == null && totalTime == null && totalDistance == null && markerCount == 0) {
      setTextView(categoryDescriptionTextView, null);
      // Match list_item_category_description in list_item.xml
      ownerTimeDistanceTextView.setSingleLine(false);
      ownerTimeDistanceTextView.setMaxLines(2);
      setTextView(ownerTimeDistanceTextView, categoryDescription);
    } else {
      // Match list_item_owner_time_distance in list_item.xml
      ownerTimeDistanceTextView.setSingleLine(true);
      setTextView(categoryDescriptionTextView, categoryDescription);
    }

    ImageView photo = (ImageView) view.findViewById(R.id.list_item_photo);
    ImageView textGradient = (ImageView) view.findViewById(R.id.list_item_text_gradient);
    if (photoUrl == null || photoUrl.equals("")) {
      photo.setVisibility(View.GONE);
      textGradient.setVisibility(View.GONE);
    } else {
      photo.setVisibility(View.VISIBLE);
      textGradient.setVisibility(View.VISIBLE);
      Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
      PhotoUtils.setImageVew(
          photo, Uri.parse(photoUrl), defaultDisplay.getWidth(), getPhotoHeight(activity), false);
    }
  }
  
  /**
   * Gets a string for share owner, total time, and total distance.
   * 
   * @param sharedOwner the share owner. Can be null
   * @param totalTime the total time. Can be null
   * @param totalDistance the total distance. Can be null
   */
  private static String getOwnerTimeDistance(
      String sharedOwner, String totalTime, String totalDistance) {
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
   * Gets the date and time as an array of two strings.
   * 
   * @param isRecording true if recording
   * @param context the context
   * @param time the start time
   */
  private static String[] getDateTime(boolean isRecording, Context context, long time) {
    if (isRecording || time == 0L) {
      return new String[] { null, null };
    }
    if (DateUtils.isToday(time)) {
      return new String[] { DateUtils.getRelativeTimeSpanString(
          time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
          DateUtils.FORMAT_ABBREV_RELATIVE).toString(), null };
    }
    int dateFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL;
    if (!isThisYear(time)) {
      dateFlags |= DateUtils.FORMAT_NUMERIC_DATE;
    }
    int timeFlags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME;
    return new String[] { DateUtils.formatDateTime(context, time, dateFlags),
        DateUtils.formatDateTime(context, time, timeFlags) };
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

  /**
   * Gets the photo height.
   * 
   * @param context the context
   */
  private static int getPhotoHeight(Context context) {
    int[] attrs = new int[] { android.R.attr.listPreferredItemHeight };
    TypedArray typeArray = context.obtainStyledAttributes(attrs);
    int height = typeArray.getDimensionPixelSize(0, LIST_PREFERRED_ITEM_HEIGHT_DEFAULT);
    typeArray.recycle();
    return 2 * height;
  }
}
