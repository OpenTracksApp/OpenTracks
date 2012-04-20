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
import android.widget.TextView;

/**
 * Utilities for updating the statistics UI labels and values.
 *
 * @author Jimmy Shih
 */
public class StatsUtils {

  private StatsUtils() {}

  /**
   * Sets the speed labels.
   *
   * @param activity the activity
   * @param reportSpeed true to report speed
   * @param includeLocationSpeed true to include the current location speed
   */
  public static void setSpeedLabels(
      Activity activity, boolean reportSpeed, boolean includeLocationSpeed) {
    StatsUtils.setSpeedLabel(activity, R.id.stats_max_speed_label, R.string.stats_max_speed,
        R.string.stats_fastest_pace, reportSpeed);
    StatsUtils.setSpeedLabel(activity, R.id.stats_average_speed_label, R.string.stats_average_speed,
        R.string.stats_average_pace, reportSpeed);
    StatsUtils.setSpeedLabel(activity, R.id.stats_average_moving_speed_label,
        R.string.stats_average_moving_speed, R.string.stats_average_moving_pace, reportSpeed);
    if (includeLocationSpeed) {
      StatsUtils.setSpeedLabel(
          activity, R.id.stats_speed_label, R.string.stats_speed, R.string.stats_pace, reportSpeed);
    }
  }

  /**
   * Sets the trip statistics values.
   * 
   * @param activity the activity
   * @param tripStatistics the trip statistics
   * @param metricUnits true to display in metric units
   * @param reportSpeed true to report speed
   */
  public static void setTripStatisticsValues(
      Activity activity, TripStatistics tripStatistics, boolean metricUnits, boolean reportSpeed) {
    double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
    double maxSpeed = tripStatistics == null ? Double.NaN : tripStatistics.getMaxSpeed();
    long totalTime = tripStatistics == null ? -1L : tripStatistics.getTotalTime();
    double averageSpeed = tripStatistics == null ? Double.NaN : tripStatistics.getAverageSpeed();
    long movingTime = tripStatistics == null ? -1L : tripStatistics.getMovingTime();
    double averageMovingSpeed = tripStatistics == null ? Double.NaN
        : tripStatistics.getAverageMovingSpeed();
    double elevationGain = tripStatistics == null ? Double.NaN
        : tripStatistics.getTotalElevationGain();
    double minElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMinElevation();
    double maxElevation = tripStatistics == null ? Double.NaN : tripStatistics.getMaxElevation();
    double minGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMinGrade();
    double maxGrade = tripStatistics == null ? Double.NaN : tripStatistics.getMaxGrade();

    StatsUtils.setDistanceValue(
        activity, R.id.stats_total_distance_value, totalDistance, metricUnits);
    StatsUtils.setSpeedValue(
        activity, R.id.stats_max_speed_value, maxSpeed, metricUnits, reportSpeed);
    StatsUtils.setTimeValue(activity, R.id.stats_total_time_value, totalTime);
    StatsUtils.setSpeedValue(
        activity, R.id.stats_average_speed_value, averageSpeed, metricUnits, reportSpeed);
    StatsUtils.setTimeValue(activity, R.id.stats_moving_time_value, movingTime);
    StatsUtils.setSpeedValue(activity, R.id.stats_average_moving_speed_value, averageMovingSpeed,
        metricUnits, reportSpeed);
    StatsUtils.setAltitudeValue(
        activity, R.id.stats_elevation_gain_value, elevationGain, metricUnits);
    StatsUtils.setAltitudeValue(
        activity, R.id.stats_min_elevation_value, minElevation, metricUnits);
    StatsUtils.setAltitudeValue(
        activity, R.id.stats_max_elevation_value, maxElevation, metricUnits);
    StatsUtils.setGradeValue(activity, R.id.stats_min_grade_value, minGrade);
    StatsUtils.setGradeValue(activity, R.id.stats_max_grade_value, maxGrade);
  }

  /**
   * Sets the location values.
   * 
   * @param activity the activity
   * @param location the location
   * @param metricUnits true to display in metric units
   * @param reportSpeed true to report speed
   */
  public static void setLocationValues(
      Activity activity, Location location, boolean metricUnits, boolean reportSpeed) {
    double speed = location == null ? Double.NaN : location.getSpeed();
    double altitude = location == null ? Double.NaN : location.getAltitude();
    double latitude = location == null ? Double.NaN : location.getLatitude();
    double longitude = location == null ? Double.NaN : location.getLongitude();

    StatsUtils.setSpeedValue(activity, R.id.stats_speed_value, speed, metricUnits, reportSpeed);
    StatsUtils.setAltitudeValue(activity, R.id.stats_elevation_value, altitude, metricUnits);
    StatsUtils.setCoordinateValue(activity, R.id.stats_latitude_value, latitude);
    StatsUtils.setCoordinateValue(activity, R.id.stats_longitude_value, longitude);
  }

  /**
   * Sets the location elevation value.
   * 
   * @param activity the activity
   * @param elevation the elevation in meters
   * @param metricUnits true to display in metric units
   */
  public static void setLocationElevationValue(
      Activity activity, double elevation, boolean metricUnits) {
    StatsUtils.setAltitudeValue(activity, R.id.stats_elevation_value, elevation, metricUnits);
  }

  /**
   * Sets the total time value.
   * 
   * @param activity the activity
   * @param totalTime the total time
   */
  public static void setTotalTimeValue(Activity activity, long totalTime) {
    StatsUtils.setTimeValue(activity, R.id.stats_total_time_value, totalTime);
  }

  /**
   * Sets a speed label.
   *
   * @param activity the activity
   * @param id the speed label resource id
   * @param speedId the speed string id
   * @param paceId the pace string id
   * @param reportSpeed true to report speed
   */
  private static void setSpeedLabel(
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
   * @param metricUnits true to display in metric units
   * @param reportSpeed true to report speed
   */
  private static void setSpeedValue(
      Activity activity, int id, double speed, boolean metricUnits, boolean reportSpeed) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(speed) || Double.isInfinite(speed)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      speed *= UnitConversions.MS_TO_KMH;
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
  private static void setDistanceValue(
      Activity activity, int id, double distance, boolean metricUnits) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(distance) || Double.isInfinite(distance)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      distance *= UnitConversions.M_TO_KM;
      if (metricUnits) {
        value = activity.getString(R.string.value_float_kilometer, distance);
      } else {
        distance *= UnitConversions.KM_TO_MI;
        value = activity.getString(R.string.value_float_mile, distance);
      }
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
  private static void setTimeValue(Activity activity, int id, long time) {
    TextView textView = (TextView) activity.findViewById(id);
    String value = time == -1L ? activity.getString(R.string.value_unknown)
        : StringUtils.formatElapsedTime(time);
    textView.setText(value);
  }

  /**
   * Sets an altitude value.
   *
   * @param activity the activity
   * @param id the altitude value resource id
   * @param altitude the altitude in meters
   * @param metricUnits true to display in metric units
   */
  private static void setAltitudeValue(
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
  private static void setGradeValue(Activity activity, int id, double grade) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(grade) || Double.isInfinite(grade)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      value = activity.getString(R.string.value_integer_percent, Math.round(grade * 100));
    }
    textView.setText(value);
  }

  /**
   * Sets a coordinate value.
   *
   * @param activity the activity
   * @param id the coordinate value resource id
   * @param coordinate the coordinate in degrees
   */
  private static void setCoordinateValue(Activity activity, int id, double coordinate) {
    TextView textView = (TextView) activity.findViewById(id);
    String value;
    if (Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
      value = activity.getString(R.string.value_unknown);
    } else {
      value = activity.getString(
          R.string.value_coordinate_degree, Location.convert(coordinate, Location.FORMAT_DEGREES));
    }
    textView.setText(value);
  }
}
