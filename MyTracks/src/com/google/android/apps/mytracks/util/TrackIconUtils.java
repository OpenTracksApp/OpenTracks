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
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Utilities for track icon.
 *
 * @author Jimmy Shih
 */
public class TrackIconUtils {

  private static final String AIRPLANE = "AIRPLANE";
  public static final String BIKE = "BIKE";
  private static final String BOAT = "BOAT";
  public static final String DRIVE = "DRIVE";
  private static final String RUN = "RUN";
  private static final String SKI = "SKI";
  private static final String SNOW_BOARDING = "SNOW_BOARDING";
  public static final String WALK = "WALK";

  private TrackIconUtils() {}

  private static LinkedHashMap<String, Pair<Integer, Integer>> map =
      new LinkedHashMap<String, Pair<Integer, Integer>>();

  static {
    map.put(
        RUN, new Pair<Integer, Integer>(R.string.activity_type_running, R.drawable.ic_track_run));
    map.put(
        WALK, new Pair<Integer, Integer>(R.string.activity_type_walking, R.drawable.ic_track_walk));
    map.put(
        BIKE, new Pair<Integer, Integer>(R.string.activity_type_biking, R.drawable.ic_track_bike));
    map.put(DRIVE,
        new Pair<Integer, Integer>(R.string.activity_type_driving, R.drawable.ic_track_drive));
    map.put(
        SKI, new Pair<Integer, Integer>(R.string.activity_type_skiing, R.drawable.ic_track_ski));
    map.put(SNOW_BOARDING, new Pair<Integer, Integer>(
        R.string.activity_type_snow_boarding, R.drawable.ic_track_snow_boarding));
    map.put(AIRPLANE,
        new Pair<Integer, Integer>(R.string.activity_type_airplane, R.drawable.ic_track_airplane));
    map.put(
        BOAT, new Pair<Integer, Integer>(R.string.activity_type_boat, R.drawable.ic_track_boat));
  }

  private static int[] airplane = new int[] {
      R.string.activity_type_airplane, R.string.activity_type_commercial_airplane,
      R.string.activity_type_rc_airplane};
  private static int[] bike = new int[] {R.string.activity_type_biking,
      R.string.activity_type_cycling,
      R.string.activity_type_dirt_bike,
      R.string.activity_type_motor_bike,
      R.string.activity_type_mountain_biking,
      R.string.activity_type_road_biking,
      R.string.activity_type_track_cycling};
  private static int[] boat = new int[] {R.string.activity_type_boat, R.string.activity_type_ferry,
      R.string.activity_type_motor_boating, R.string.activity_type_rc_boat};
  private static int[] drive = new int[] {
      R.string.activity_type_atv, R.string.activity_type_driving,
      R.string.activity_type_driving_bus, R.string.activity_type_driving_car};
  private static int[] run = new int[] {
      R.string.activity_type_running, R.string.activity_type_street_running,
      R.string.activity_type_track_running, R.string.activity_type_trail_running};
  private static int[] ski =
      new int[] {R.string.activity_type_cross_country_skiing, R.string.activity_type_skiing};
  private static int[] snowBoarding = new int[] {R.string.activity_type_snow_boarding};
  private static int[] walk = new int[] {R.string.activity_type_hiking,
      R.string.activity_type_off_trail_hiking, R.string.activity_type_speed_walking,
      R.string.activity_type_trail_hiking, R.string.activity_type_walking};

  /**
   * Gets the icon drawable.
   *
   * @param iconValue the icon value
   */
  public static int getIconDrawable(String iconValue) {
    if (iconValue == null || iconValue.equals("")) {
      return R.drawable.ic_track_walk;
    }
    Pair<Integer, Integer> pair = map.get(iconValue);
    return pair == null ? R.drawable.ic_track_walk : pair.second;
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
    Pair<Integer, Integer> pair = map.get(iconValue);
    return pair == null ? R.string.activity_type_walking : pair.first;
  }

  /**
   * Gets all icon values.
   */
  public static List<String> getAllIconValues() {
    List<String> values = new ArrayList<String>();
    for (String value : map.keySet()) {
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
    if (inList(context, activityType, airplane)) {
      return AIRPLANE;
    }
    if (inList(context, activityType, bike)) {
      return BIKE;
    }
    if (inList(context, activityType, boat)) {
      return BOAT;
    }
    if (inList(context, activityType, drive)) {
      return DRIVE;
    }
    if (inList(context, activityType, run)) {
      return RUN;
    }
    if (inList(context, activityType, ski)) {
      return SKI;
    }
    if (inList(context, activityType, snowBoarding)) {
      return SNOW_BOARDING;
    }
    if (inList(context, activityType, walk)) {
      return WALK;
    }
    return "";
  }

  /**
   * Inverts the colors in a bitmap.
   *
   * @param source the source
   */
  public static Bitmap invertBitmap(Bitmap source) {
    Config config = source.getConfig();
    if (config == null) {
      config = Config.ARGB_8888;
    }
    Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), config);
    int height = source.getHeight();
    int width = source.getWidth();
    int pixelColor;
    int alpha;
    int red;
    int green;
    int blue;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        pixelColor = source.getPixel(x, y);
        alpha = Color.alpha(pixelColor);
        red = 255 - Color.red(pixelColor);
        green = 255 - Color.green(pixelColor);
        blue = 255 - Color.blue(pixelColor);
        output.setPixel(x, y, Color.argb(alpha, red, green, blue));
      }
    }
    return output;
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
}
