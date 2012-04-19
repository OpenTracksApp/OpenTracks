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
import android.widget.TextView;

/**
 * Utilities for updating the statistics UI labels and values.
 *
 * @author Jimmy Shih
 */
public class StatsUtils {

  private StatsUtils() {}

  /**
   * Sets a speed label.
   *
   * @param activity the activity
   * @param id the speed label resource id
   * @param speedId the speed string id
   * @param paceId the pace string id
   * @param reportSpeed true to report speed
   */
  public static void setSpeedLabel(
      Activity activity, int id, int speedId, int paceId, boolean reportSpeed) {
    TextView textView = (TextView) activity.findViewById(id);
    textView.setText(reportSpeed ? speedId : paceId);
  }

  /**
   * Sets a speed value.
   *
   * @param activity the activity
   * @param id the speed value resource id
   * @param speed the speed in meters per second
   * @param reportSpeed true to report speed
   * @param metricUnits true to display in metric units
   */
  public static void setSpeedValue(
      Activity activity, int id, double speed, boolean reportSpeed, boolean metricUnits) {
    TextView textView = (TextView) activity.findViewById(id);
    speed *= UnitConversions.MS_TO_KMH;
    String value;
    if (metricUnits) {
      if (reportSpeed) {
        value = activity.getString(R.string.value_float_kilometer_hour, speed);
      } else {
        // convert from hours to minutes
        double pace = speed == 0 ? 0.0 : 60.0 / speed;
        value = activity.getString(R.string.value_float_minute_kilometer, pace);
      }
    } else {
      speed *= UnitConversions.KM_TO_MI;
      if (reportSpeed) {
        value = activity.getString(R.string.value_float_mile_hour, speed);
      } else {
        // convert from hours to minutes
        double pace = speed == 0 ? 0.0 : 60.0 / speed;
        value = activity.getString(R.string.value_float_minute_mile, pace);
      }
    }
    textView.setText(value);
  }

  /**
   * Sets a distance value.
   *
   * @param activity the activity
   * @param id the distance value resource id
   * @param distance the distance in meters
   * @param metricUnits true to display in metric units
   */
  public static void setDistanceValue(
      Activity activity, int id, double distance, boolean metricUnits) {
    TextView textView = (TextView) activity.findViewById(id);
    distance *= UnitConversions.M_TO_KM;
    String value;
    if (metricUnits) {
      value = activity.getString(R.string.value_float_kilometer, distance);
    } else {
      distance *= UnitConversions.KM_TO_MI;
      value = activity.getString(R.string.value_float_mile, distance);
    }
    textView.setText(value);
  }

  /**
   * Sets a time value.
   *
   * @param activity the activity
   * @param id the time value resource id
   * @param time the time
   */
  public static void setTimeValue(Activity activity, int id, long time) {
    TextView textView = (TextView) activity.findViewById(id);
    textView.setText(StringUtils.formatElapsedTime(time));
  }

  /**
   * Sets an altitude value.
   *
   * @param activity the activity
   * @param id the altitude value resource id
   * @param altitude the altitude in meters
   * @param metricUnits true to display in metric units
   */
  public static void setAltitudeValue(
      Activity activity, int id, double altitude, boolean metricUnits) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(altitude) || Double.isInfinite(altitude)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      if (metricUnits) {
        value = activity.getString(R.string.value_float_meter, altitude);
      } else {
        altitude *= UnitConversions.M_TO_FT;
        value = activity.getString(R.string.value_float_feet, altitude);
      }
    }
    textView.setText(value);
  }

  /**
   * Sets a grade value.
   *
   * @param activity the activity
   * @param id the grade value resource id
   * @param grade the grade in fraction between 0 and 1
   */
  public static void setGradeValue(Activity activity, int id, double grade) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(grade) || Double.isInfinite(grade)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      value = activity.getString(R.string.value_integer_percent, Math.round(grade * 100));
    }
    textView.setText(value);
  }
}
