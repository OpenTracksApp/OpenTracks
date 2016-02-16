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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Utilities for track icon.
 * 
 * @author Jimmy Shih
 */
public class TrackIconUtils {

  public static final String AIRPLANE = "AIRPLANE";
  public static final String BIKE = "BIKE";
  public static final String BOAT = "BOAT";
  public static final String DRIVE = "DRIVE";
  public static final String RUN = "RUN";
  public static final String SKI = "SKI";
  public static final String SNOW_BOARDING = "SNOW_BOARDING";
  public static final String WALK = "WALK";

  private static final int[] AIRPLANE_LIST = new int[] { R.string.activity_type_airplane,
      R.string.activity_type_commercial_airplane, R.string.activity_type_rc_airplane };
  private static final int[] BIKE_LIST = new int[] { R.string.activity_type_biking,
      R.string.activity_type_cycling, R.string.activity_type_dirt_bike,
      R.string.activity_type_motor_bike, R.string.activity_type_mountain_biking,
      R.string.activity_type_road_biking, R.string.activity_type_track_cycling };
  private static final int[] BOAT_LIST = new int[] { R.string.activity_type_boat,
      R.string.activity_type_ferry, R.string.activity_type_motor_boating,
      R.string.activity_type_rc_boat };
  private static final int[] DRIVE_LIST = new int[] { R.string.activity_type_atv,
      R.string.activity_type_driving, R.string.activity_type_driving_bus,
      R.string.activity_type_driving_car };
  private static final int[] RUN_LIST = new int[] { R.string.activity_type_running,
      R.string.activity_type_street_running, R.string.activity_type_track_running,
      R.string.activity_type_trail_running };
  private static final int[] SKI_LIST = new int[] {
      R.string.activity_type_cross_country_skiing, R.string.activity_type_skiing };
  private static final int[] SNOW_BOARDING_LIST = new int[] {
      R.string.activity_type_snow_boarding };
  private static final int[] WALK_LIST = new int[] { R.string.activity_type_hiking,
      R.string.activity_type_off_trail_hiking, R.string.activity_type_speed_walking,
      R.string.activity_type_trail_hiking, R.string.activity_type_walking };

  private static final LinkedHashMap<String, Pair<Integer, Integer>>
      MAP = new LinkedHashMap<String, Pair<Integer, Integer>>();

  static {
    MAP.put(
        RUN, new Pair<Integer, Integer>(R.string.activity_type_running, R.drawable.ic_track_run));
    MAP.put(
        WALK, new Pair<Integer, Integer>(R.string.activity_type_walking, R.drawable.ic_track_walk));
    MAP.put(
        BIKE, new Pair<Integer, Integer>(R.string.activity_type_biking, R.drawable.ic_track_bike));
    MAP.put(DRIVE,
        new Pair<Integer, Integer>(R.string.activity_type_driving, R.drawable.ic_track_drive));
    MAP.put(
        SKI, new Pair<Integer, Integer>(R.string.activity_type_skiing, R.drawable.ic_track_ski));
    MAP.put(SNOW_BOARDING, new Pair<Integer, Integer>(
        R.string.activity_type_snow_boarding, R.drawable.ic_track_snow_boarding));
    MAP.put(AIRPLANE,
        new Pair<Integer, Integer>(R.string.activity_type_airplane, R.drawable.ic_track_airplane));
    MAP.put(
        BOAT, new Pair<Integer, Integer>(R.string.activity_type_boat, R.drawable.ic_track_boat));
  }

  private static final float[] REVERT_COLOR_MATRIX = { -1.0f, 0, 0, 0, 255, // red
      0, -1.0f, 0, 0, 255, // green
      0, 0, -1.0f, 0, 255, // blue
      0, 0, 0, 1.0f, 0 // alpha
  };

  private TrackIconUtils() {}

  /**
   * Gets the icon drawable.
   * 
   * @param iconValue the icon value
   */
  public static int getIconDrawable(String iconValue) {
    if (iconValue == null || iconValue.equals("")) {
      return R.drawable.ic_track_generic;
    }
    Pair<Integer, Integer> pair = MAP.get(iconValue);
    return pair == null ? R.drawable.ic_track_generic : pair.second;
  }

  /**
   * Gets the icon activity type.
   * 
   * @param iconValue the icon value
   */
  public static int getIconActivityType(String iconValue) {
    if (iconValue == null || iconValue.equals("")) {
      return R.string.activity_type_walking;
    }
    Pair<Integer, Integer> pair = MAP.get(iconValue);
    return pair == null ? R.string.activity_type_walking : pair.first;
  }

  /**
   * Gets all icon values.
   */
  public static List<String> getAllIconValues() {
    List<String> values = new ArrayList<String>();
    for (String value : MAP.keySet()) {
      values.add(value);
    }
    return values;
  }

  /**
   * Gets the icon value.
   * 
   * @param context the context
   * @param activityType the activity type
   */
  public static String getIconValue(Context context, String activityType) {
    if (activityType == null || activityType.equals("")) {
      return "";
    }
    if (inList(context, activityType, AIRPLANE_LIST)) {
      return AIRPLANE;
    }
    if (inList(context, activityType, BIKE_LIST)) {
      return BIKE;
    }
    if (inList(context, activityType, BOAT_LIST)) {
      return BOAT;
    }
    if (inList(context, activityType, DRIVE_LIST)) {
      return DRIVE;
    }
    if (inList(context, activityType, RUN_LIST)) {
      return RUN;
    }
    if (inList(context, activityType, SKI_LIST)) {
      return SKI;
    }
    if (inList(context, activityType, SNOW_BOARDING_LIST)) {
      return SNOW_BOARDING;
    }
    if (inList(context, activityType, WALK_LIST)) {
      return WALK;
    }
    return "";
  }

  public static void setIconSpinner(Spinner spinner, String iconValue) {
    @SuppressWarnings("unchecked")
    ArrayAdapter<StringBuilder> adapter = (ArrayAdapter<StringBuilder>) spinner.getAdapter();
    StringBuilder stringBuilder = adapter.getItem(0);
    stringBuilder.delete(0, stringBuilder.length());
    stringBuilder.append(iconValue);
    adapter.notifyDataSetChanged();
  }

  public static ArrayAdapter<StringBuilder> getIconSpinnerAdapter(
      final Context context, String iconValue) {
    return new ArrayAdapter<StringBuilder>(context, android.R.layout.simple_spinner_item,
        new StringBuilder[] { new StringBuilder(iconValue) }) {
        @Override
      public View getView(int position, View convertView, android.view.ViewGroup parent) {
        ImageView imageView = convertView != null ? (ImageView) convertView
            : new ImageView(getContext());
        Bitmap source = BitmapFactory.decodeResource(
            context.getResources(), TrackIconUtils.getIconDrawable(getItem(position).toString()));
        imageView.setImageBitmap(source);
        imageView.setPadding(4, 4, -4, -4);
        return imageView;
      }
    };
  }

  /**
   * Returns true if the activity type is in the list.
   * 
   * @param context the context
   * @param activityType the activity type
   * @param list the list
   */
  private static boolean inList(Context context, String activityType, int[] list) {
    for (int i : list) {
      if (context.getString(i).equals(activityType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the menu icon color.
   * 
   * @param menu the menu
   */
  public static void setMenuIconColor(Menu menu) {
    if (ApiAdapterFactory.getApiAdapter().revertMenuIconColor()) {
      int size = menu.size();
      for (int i = 0; i < size; i++) {
        MenuItem menuitem = menu.getItem(i);
        revertMenuIconColor(menuitem);
      }
    }
  }

  /**
   * Sets the menu icon color.
   * 
   * @param menuitem the menu item
   */
  public static void setMenuIconColor(MenuItem menuitem) {
    if (ApiAdapterFactory.getApiAdapter().revertMenuIconColor()) {
      revertMenuIconColor(menuitem);
    }
  }

  /**
   * Reverts the menu icon color.
   * 
   * @param menuitem the menu item
   */
  private static void revertMenuIconColor(MenuItem menuitem) {
    Drawable drawable = menuitem.getIcon();
    drawable.setColorFilter(new ColorMatrixColorFilter(REVERT_COLOR_MATRIX));
  }
}
