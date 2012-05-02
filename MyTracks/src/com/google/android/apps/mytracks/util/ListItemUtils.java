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

import android.view.View;
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
   * @param view the list item view
   * @param name the name value
   * @param iconId the icon id
   * @param category the category value
   * @param totalTime the total time value
   * @param totalDistance the total distance value
   * @param startTime the start time value
   * @param description the description value
   */
  public static void setListItem(View view,
      String name,
      int iconId,
      String category,
      String totalTime,
      String totalDistance,
      String startTime,
      String description) {
    TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
    nameTextView.setText(name);
    nameTextView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);

    TextView categoryTextView = (TextView) view.findViewById(R.id.list_item_category);
    setTextView(categoryTextView, category);

    TextView totalTimeTextView = (TextView) view.findViewById(R.id.list_item_total_time);
    setTextView(totalTimeTextView, totalTime);

    TextView totalDistanceTextView = (TextView) view.findViewById(R.id.list_item_total_distance);
    setTextView(totalDistanceTextView, totalDistance);

    TextView startTimeTextView = (TextView) view.findViewById(R.id.list_item_start_time);
    setTextView(startTimeTextView, startTime);

    TextView descriptionTextView = (TextView) view.findViewById(R.id.list_item_description);
    setTextView(descriptionTextView, description);
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
