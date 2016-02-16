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
import com.google.android.apps.mytracks.util.CalorieUtils.ActivityType;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/**
 * Utilities for updating the statistics UI labels and values.
 * 
 * @author Jimmy Shih
 */
public class StatsUtils {

  private static final String GRADE_PERCENTAGE = "%";
  private static final String GRADE_FORMAT = "%1$d";
  private static final String CALORIES_FORMAT = "%1$,.0f";

  private StatsUtils() {}

  /**
   * Sets the location values.
   * 
   * @param context the context
   * @param activity the activity for finding views. If null, the view cannot be
   *          null
   * @param view the containing view for finding views. If null, the activity
   *          cannot be null
   * @param location the location
   * @param isRecording true if recording
   */
  public static void setLocationValues(
      Context context, Activity activity, View view, Location location, boolean isRecording) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);
    boolean reportSpeed = PreferencesUtils.isReportSpeed(context);

    // Set speed/pace
    View speed = getView(activity, view, R.id.stats_speed);
    speed.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);

    if (isRecording) {
      double value = isRecording && location != null && location.hasSpeed() ? location.getSpeed()
          : Double.NaN;
      setSpeed(context, speed, R.string.stats_speed, R.string.stats_pace, value, metricUnits,
          reportSpeed);
    }

    // Set elevation
    boolean showGradeElevation = PreferencesUtils.getBoolean(
        context, R.string.stats_show_grade_elevation_key,
        PreferencesUtils.STATS_SHOW_GRADE_ELEVATION_DEFAULT) && isRecording;
    View elevation = getView(activity, view, R.id.stats_elevation);
    elevation.setVisibility(showGradeElevation ? View.VISIBLE : View.GONE);

    if (showGradeElevation) {
      double altitude = location != null && location.hasAltitude() ? location.getAltitude()
          : Double.NaN;
      setElevationValue(context, elevation, -1, altitude, metricUnits);
    }

    // Set coordinate
    boolean showCoordinate = PreferencesUtils.getBoolean(
        context, R.string.stats_show_coordinate_key, PreferencesUtils.STATS_SHOW_COORDINATE_DEFAULT)
        && isRecording;
    View coordinateSeparator = getView(activity, view, R.id.stats_coordinate_separator);
    View coordinateContainer = getView(activity, view, R.id.stats_coordinate_container);

    if (coordinateSeparator != null) {
      coordinateSeparator.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
    }
    coordinateContainer.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);

    if (showCoordinate) {
      double latitude = location != null ? location.getLatitude() : Double.NaN;
      double longitude = location != null ? location.getLongitude() : Double.NaN;
      setCoordinateValue(
          context, getView(activity, view, R.id.stats_latitude), R.string.stats_latitude, latitude);
      setCoordinateValue(context, getView(activity, view, R.id.stats_longitude),
          R.string.stats_longitude, longitude);
    }
  }

  /**
   * Sets the total time value.
   * 
   * @param activity the activity
   * @param totalTime the total time
   */
  public static void setTotalTimeValue(Activity activity, long totalTime) {
    setTimeValue(activity, activity.findViewById(R.id.stats_total_time), R.string.stats_total_time,
        totalTime);
  }

  /**
   * Sets the trip statistics values.
   * 
   * @param context the context
   * @param activity the activity for finding views. If null, then view cannot
   *          be null
   * @param view the containing view for finding views. If null, the activity
   *          cannot be null
   * @param tripStatistics the trip statistics
   * @param activityType the activity type
   * @param trackIconValue the track icon value or null to hide the track icon
   *          spinner
   */
  public static void setTripStatisticsValues(Context context, Activity activity, View view,
      TripStatistics tripStatistics, ActivityType activityType, String trackIconValue) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);
    boolean reportSpeed = PreferencesUtils.isReportSpeed(context);

    // Set total distance
    double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
    setDistanceValue(
        context, getView(activity, view, R.id.stats_distance), totalDistance, metricUnits);

    // Set calorie
    double calorie = tripStatistics == null || activityType == ActivityType.INVALID ? Double.NaN
        : tripStatistics.getCalorie();
    setCalorie(context, getView(activity, view, R.id.stats_calorie), calorie);

    Spinner spinner = (Spinner) getView(activity, view, R.id.stats_activity_type_icon);
    spinner.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);
    if (trackIconValue != null) {
      TrackIconUtils.setIconSpinner(spinner, trackIconValue);
    }

    // Set total time
    setTimeValue(context, getView(activity, view, R.id.stats_total_time), R.string.stats_total_time,
        tripStatistics != null ? tripStatistics.getTotalTime() : -1L);

    // Set moving time
    setTimeValue(context, getView(activity, view, R.id.stats_moving_time),
        R.string.stats_moving_time, tripStatistics != null ? tripStatistics.getMovingTime() : -1L);

    // Set average speed/pace
    double averageSpeed = tripStatistics != null ? tripStatistics.getAverageSpeed() : Double.NaN;
    setSpeed(context, getView(activity, view, R.id.stats_average_speed),
        R.string.stats_average_speed, R.string.stats_average_pace, averageSpeed, metricUnits,
        reportSpeed);

    // Set max speed/pace
    double maxSpeed = tripStatistics == null ? Double.NaN : tripStatistics.getMaxSpeed();
    setSpeed(context, getView(activity, view, R.id.stats_max_speed), R.string.stats_max_speed,
        R.string.stats_fastest_pace, maxSpeed, metricUnits, reportSpeed);

    // Set average moving speed/pace
    double averageMovingSpeed = tripStatistics != null ? tripStatistics.getAverageMovingSpeed()
        : Double.NaN;
    setSpeed(context, getView(activity, view, R.id.stats_average_moving_speed),
        R.string.stats_average_moving_speed, R.string.stats_average_moving_pace, averageMovingSpeed,
        metricUnits, reportSpeed);

    // Set grade/elevation
    boolean showGradeElevation = PreferencesUtils.getBoolean(context,
        R.string.stats_show_grade_elevation_key,
        PreferencesUtils.STATS_SHOW_GRADE_ELEVATION_DEFAULT);
    View gradeElevationSeparator = getView(activity, view, R.id.stats_grade_elevation_separator);
    View gradeElevationContainer = getView(activity, view, R.id.stats_grade_elevation_container);

    gradeElevationSeparator.setVisibility(showGradeElevation ? View.VISIBLE : View.GONE);
    gradeElevationContainer.setVisibility(showGradeElevation ? View.VISIBLE : View.GONE);

    if (showGradeElevation) {
      // Set grade
      double minGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMinGrade();
      double maxGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMaxGrade();
      setGradeValue(
          context, getView(activity, view, R.id.stats_grade_min), R.string.stats_min, minGrade);
      setGradeValue(
          context, getView(activity, view, R.id.stats_grade_max), R.string.stats_max, maxGrade);

      // Set elevation
      double elevationGain = tripStatistics == null ? Double.NaN
          : tripStatistics.getTotalElevationGain();
      double minElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMinElevation();
      double maxElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMaxElevation();
      setElevationValue(context, getView(activity, view, R.id.stats_elevation_gain),
          R.string.stats_gain, elevationGain, metricUnits);
      setElevationValue(context, getView(activity, view, R.id.stats_elevation_min),
          R.string.stats_min, minElevation, metricUnits);
      setElevationValue(context, getView(activity, view, R.id.stats_elevation_max),
          R.string.stats_max, maxElevation, metricUnits);
    }
  }

  /**
   * Sets speed.
   * 
   * @param context the context
   * @param view the containing view
   * @param speedLabelId the speed label id
   * @param paceLabelId the pace label id
   * @param speed the speed in meters per second
   * @param metricUnits true if metric units
   * @param reportSpeed true if report speed
   */
  private static void setSpeed(Context context, View view, int speedLabelId, int paceLabelId,
      double speed, boolean metricUnits, boolean reportSpeed) {
    String parts[] = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
    setItem(context, view, reportSpeed ? speedLabelId : paceLabelId, parts[0], parts[1]);
  }

  /**
   * Sets calorie.
   * 
   * @param context the context
   * @param view the containing view
   * @param calorie the value of calorie
   */
  private static void setCalorie(Context context, View view, double calorie) {
    String value = Double.isNaN(calorie) ? null
        : String.format(Locale.getDefault(), CALORIES_FORMAT, calorie);
    setItem(context, view, R.string.stats_calorie, value, context.getString(R.string.unit_calorie));
  }

  /**
   * Sets distance value.
   * 
   * @param context the context
   * @param view the containing view
   * @param distance the distance in meters
   * @param metricUnits true if metric units
   */
  private static void setDistanceValue(
      Context context, View view, double distance, boolean metricUnits) {
    String parts[] = StringUtils.getDistanceParts(context, distance, metricUnits);
    setItem(context, view, R.string.stats_distance, parts[0], parts[1]);
  }

  /**
   * Sets a time value.
   * 
   * @param context the context
   * @param view the containing view
   * @param labelId the label id
   * @param time the time
   */
  private static void setTimeValue(Context context, View view, int labelId, long time) {
    String value = time == -1L ? null : StringUtils.formatElapsedTime(time);
    setItem(context, view, labelId, value, null);
  }

  /**
   * Sets an elevation value.
   * 
   * @param context the context
   * @param view the containing view
   * @param labelId the label id
   * @param elevation the elevation in meters
   * @param metricUnits true if metric units
   */
  private static void setElevationValue(
      Context context, View view, int labelId, double elevation, boolean metricUnits) {
    String value;
    String unit;
    if (Double.isNaN(elevation) || Double.isInfinite(elevation)) {
      value = null;
      unit = null;
    } else {
      if (metricUnits) {
        value = StringUtils.formatDecimal(elevation);
        unit = context.getString(R.string.unit_meter);
      } else {
        elevation *= UnitConversions.M_TO_FT;
        value = StringUtils.formatDecimal(elevation);
        unit = context.getString(R.string.unit_feet);
      }
    }
    setItem(context, view, labelId, value, unit);
  }

  /**
   * Sets a grade value.
   * 
   * @param context the context
   * @param view the containing view
   * @param labelId the label id
   * @param grade the grade in fraction between 0 and 1
   */
  private static void setGradeValue(Context context, View view, int labelId, double grade) {
    String value = Double.isNaN(grade) || Double.isInfinite(grade) ? null
        : String.format(Locale.getDefault(), GRADE_FORMAT, Math.round(grade * 100));
    setItem(context, view, labelId, value, GRADE_PERCENTAGE);
  }

  /**
   * Sets a coordinate value.
   * 
   * @param context the context
   * @param view the containing view
   * @param labelId the label id
   * @param coordinate the coordinate in degrees
   */
  private static void setCoordinateValue(
      Context context, View view, int labelId, double coordinate) {
    String value = Double.isNaN(coordinate) || Double.isInfinite(coordinate) ? null
        : StringUtils.formatCoordinate(coordinate);
    setItem(context, view, labelId, value, null);
  }

  /**
   * Sets an item.
   * 
   * @param context the context
   * @param view the containing view
   * @param labelId the label id. -1 to hide the label
   * @param value the value, can be null
   * @param unit the unit. Null to hide the unit
   */
  private static void setItem(
      Context context, View view, int labelId, CharSequence value, CharSequence unit) {
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
      value = context.getString(R.string.value_unknown);
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

  /**
   * Get a view.
   * 
   * @param activity the activity
   * @param view the containing view
   * @param id the id
   */
  private static View getView(Activity activity, View view, int id) {
    if (activity != null) {
      return activity.findViewById(id);
    } else {
      return view.findViewById(id);
    }
  }
}