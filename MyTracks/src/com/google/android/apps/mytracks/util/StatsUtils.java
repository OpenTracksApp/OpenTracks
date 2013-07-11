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

import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Utilities for updating the statistics UI labels and values.
 * 
 * @author Jimmy Shih
 */
public class StatsUtils {

  private static final String COORDINATE_DEGREE = "\u00B0";
  private static final String GRADE_PERCENTAGE = "%";

  private static final String ELEVATION_FORMAT = "%1$.2f";
  private static final String GRADE_FORMAT = "%1$d";

  private StatsUtils() {}

  /**
   * Sets the location values.
   * 
   * @param activity the activity
   * @param location the location
   * @param isRecording true if recording
   */
  public static void setLocationValues(Activity activity, Location location, boolean isRecording) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(activity);
    boolean reportSpeed = PreferencesUtils.getBoolean(
        activity, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);

    // Set speed/pace
    double speed = isRecording && location != null && location.hasSpeed() ? location.getSpeed()
        : Double.NaN;
    setSpeed(activity, R.id.stats_speed, R.string.stats_speed, R.string.stats_pace, speed,
        metricUnits, reportSpeed);

    // Set elevation
    boolean showGradeElevation = PreferencesUtils.getBoolean(activity,
        R.string.stats_show_grade_elevation_key,
        PreferencesUtils.STATS_SHOW_GRADE_ELEVATION_DEFAULT);
    View elevation = activity.findViewById(R.id.stats_elevation);

    if (showGradeElevation && isRecording) {
      double altitude = location != null && location.hasAltitude() ? location.getAltitude()
          : Double.NaN;
      elevation.setVisibility(View.VISIBLE);
      setElevationValue(activity, R.id.stats_elevation, -1, altitude, metricUnits);
    } else {
      elevation.setVisibility(View.GONE);
    }

    // Set coordinate
    boolean showCoordinate = PreferencesUtils.getBoolean(activity,
        R.string.stats_show_coordinate_key, PreferencesUtils.STATS_SHOW_COORDINATE_DEFAULT);
    View coordinateHorizontalLine = activity.findViewById(R.id.stats_coordinate_horizontal_line);
    View coordinateContainer = activity.findViewById(R.id.stats_coordinate_container);

    if (showCoordinate && isRecording) {
      double latitude = location != null ? location.getLatitude() : Double.NaN;
      double longitude = location != null ? location.getLongitude() : Double.NaN;
      coordinateHorizontalLine.setVisibility(View.VISIBLE);
      coordinateContainer.setVisibility(View.VISIBLE);
      setCoordinateValue(activity, R.id.stats_latitude, R.string.stats_latitude, latitude);
      setCoordinateValue(activity, R.id.stats_longitude, R.string.stats_longitude, longitude);
    } else {
      coordinateHorizontalLine.setVisibility(View.GONE);
      coordinateContainer.setVisibility(View.GONE);
    }
  }

  /**
   * Sets the total time value.
   * 
   * @param activity the activity
   * @param totalTime the total time
   */
  public static void setTotalTimeValue(Activity activity, long totalTime) {
    setTimeValue(activity, R.id.stats_total_time, R.string.stats_total_time, totalTime);
  }

  /**
   * Sets the trip statistics values.
   * 
   * @param activity the activity
   * @param tripStatistics the trip statistics
   */
  public static void setTripStatisticsValues(Activity activity, TripStatistics tripStatistics) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(activity);
    boolean reportSpeed = PreferencesUtils.getBoolean(
        activity, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);

    // Set total distance
    double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
    setDistanceValue(activity, totalDistance, metricUnits);

    // Set total time
    setTimeValue(activity, R.id.stats_total_time, R.string.stats_total_time,
        tripStatistics != null ? tripStatistics.getTotalTime() : -1L);

    // Set average speed/pace
    double averageSpeed = tripStatistics != null ? tripStatistics.getAverageSpeed() : Double.NaN;
    setSpeed(activity, R.id.stats_average_speed, R.string.stats_average_speed,
        R.string.stats_average_pace, averageSpeed, metricUnits, reportSpeed);

    // Set moving time
    setTimeValue(activity, R.id.stats_moving_time, R.string.stats_moving_time,
        tripStatistics != null ? tripStatistics.getMovingTime() : -1L);

    // Set average moving speed/pace
    double averageMovingSpeed = tripStatistics != null ? tripStatistics.getAverageMovingSpeed()
        : Double.NaN;
    setSpeed(activity, R.id.stats_average_moving_time, R.string.stats_average_moving_speed,
        R.string.stats_average_moving_pace, averageMovingSpeed, metricUnits, reportSpeed);

    // Set max speed/pace
    double maxSpeed = tripStatistics == null ? Double.NaN : tripStatistics.getMaxSpeed();
    setSpeed(activity, R.id.stats_max_speed, R.string.stats_max_speed, R.string.stats_fastest_pace,
        maxSpeed, metricUnits, reportSpeed);

    // Set grade/elevation
    boolean showGradeElevation = PreferencesUtils.getBoolean(activity,
        R.string.stats_show_grade_elevation_key,
        PreferencesUtils.STATS_SHOW_GRADE_ELEVATION_DEFAULT);
    View gradeElevationHorizontalLine = activity.findViewById(
        R.id.stats_grade_elevation_horizontal_line);
    View gradeElevationContainer = activity.findViewById(R.id.stats_grade_elevation_container);

    if (showGradeElevation) {
      gradeElevationHorizontalLine.setVisibility(View.VISIBLE);
      gradeElevationContainer.setVisibility(View.VISIBLE);
      // Set grade
      double minGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMinGrade();
      double maxGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMaxGrade();
      setGradeValue(activity, R.id.stats_grade_min, R.string.stats_min, minGrade);
      setGradeValue(activity, R.id.stats_grade_max, R.string.stats_max, maxGrade);

      // Set elevation
      double elevationGain = tripStatistics == null ? Double.NaN
          : tripStatistics.getTotalElevationGain();
      double minElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMinElevation();
      double maxElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMaxElevation();
      setElevationValue(
          activity, R.id.stats_elevation_gain, R.string.stats_gain, elevationGain, metricUnits);
      setElevationValue(
          activity, R.id.stats_elevation_min, R.string.stats_min, minElevation, metricUnits);
      setElevationValue(
          activity, R.id.stats_elevation_max, R.string.stats_max, maxElevation, metricUnits);
    } else {
      gradeElevationHorizontalLine.setVisibility(View.GONE);
      gradeElevationContainer.setVisibility(View.GONE);
    }
  }

  /**
   * Sets speed.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param speedLabelId the speed label id
   * @param paceLabelId the pace label id
   * @param speed the speed in meters per second
   * @param metricUnits true if metric units
   * @param reportSpeed true if report speed
   */
  private static void setSpeed(Activity activity, int itemId, int speedLabelId, int paceLabelId,
      double speed, boolean metricUnits, boolean reportSpeed) {
    String parts[] = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);
    setItem(activity, itemId, reportSpeed ? speedLabelId : paceLabelId, parts[0], parts[1]);
  }

  /**
   * Sets distance value.
   * 
   * @param activity the activity
   * @param distance the distance in meters
   * @param metricUnits true if metric units
   */
  private static void setDistanceValue(Activity activity, double distance, boolean metricUnits) {
    String parts[] = StringUtils.getDistanceParts(activity, distance, metricUnits);
    setItem(activity, R.id.stats_distance, R.string.stats_distance, parts[0], parts[1]);
  }

  /**
   * Sets a time value.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param labelId the label id
   * @param time the time
   */
  private static void setTimeValue(Activity activity, int itemId, int labelId, long time) {
    String value = time == -1L ? activity.getString(R.string.value_unknown)
        : StringUtils.formatElapsedTime(time);
    setItem(activity, itemId, labelId, value, null);
  }

  /**
   * Sets an elevation value.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param labelId the label id
   * @param elevation the elevation in meters
   * @param metricUnits true if metric units
   */
  private static void setElevationValue(
      Activity activity, int itemId, int labelId, double elevation, boolean metricUnits) {
    String value;
    String unit;
    if (Double.isNaN(elevation) || Double.isInfinite(elevation)) {
      value = null;
      unit = null;
    } else {
      if (metricUnits) {
        value = String.format(Locale.getDefault(), ELEVATION_FORMAT, elevation);
        unit = activity.getString(R.string.unit_meter);
      } else {
        elevation *= UnitConversions.M_TO_FT;
        value = String.format(Locale.getDefault(), ELEVATION_FORMAT, elevation);
        unit = activity.getString(R.string.unit_feet);
      }
    }
    setItem(activity, itemId, labelId, value, unit);
  }

  /**
   * Sets a grade value.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param labelId the label id
   * @param grade the grade in fraction between 0 and 1
   */
  private static void setGradeValue(Activity activity, int itemId, int labelId, double grade) {
    String value = Double.isNaN(grade) || Double.isInfinite(grade) ? null
        : String.format(Locale.getDefault(), GRADE_FORMAT, Math.round(grade * 100));
    setItem(activity, itemId, labelId, value, GRADE_PERCENTAGE);
  }

  /**
   * Sets a coordinate value.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param labelId the label id
   * @param coordinate the coordinate in degrees
   */
  private static void setCoordinateValue(
      Activity activity, int itemId, int labelId, double coordinate) {
    String value = Double.isNaN(coordinate) || Double.isInfinite(coordinate) ? null
        : Location.convert(coordinate, Location.FORMAT_DEGREES);
    setItem(activity, itemId, labelId, value, COORDINATE_DEGREE);
  }

  /**
   * Sets an item.
   * 
   * @param activity the activity
   * @param itemId the item id
   * @param labelId the label id. -1 to hide the label
   * @param value the value, can be null
   * @param unit the unit. Null to hide the unit
   */
  private static void setItem(
      Activity activity, int itemId, int labelId, String value, String unit) {
    View view = activity.findViewById(itemId);
    TextView labelTextView = (TextView) view.findViewById(R.id.stats_label);
    TextView valueTextView = (TextView) view.findViewById(R.id.stats_value);
    TextView unitTextView = (TextView) view.findViewById(R.id.stats_unit);
    if (labelTextView == null || valueTextView == null || unitTextView == null) {
      return;
    }
    if (labelId == -1) {
      labelTextView.setVisibility(View.GONE);
    } else {
      labelTextView.setVisibility(View.VISIBLE);
      labelTextView.setText(labelId);
    }

    if (value == null) {
      value = activity.getString(R.string.value_unknown);
      unitTextView.setVisibility(View.GONE);
    } else {
      if (unit == null) {
        unitTextView.setVisibility(View.GONE);
      } else {
        unitTextView.setVisibility(View.VISIBLE);
        unitTextView.setText(unit);
      }
    }
    valueTextView.setText(value);
  }
}
