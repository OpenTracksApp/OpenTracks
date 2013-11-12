/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatisticsUpdater;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.location.Location;

/**
 * Utilities to calculate calories.
 * 
 * @author youtaol
 */
public class CalorieUtils {

  /**
   * Activity types.
   */
  public enum ActivityType {
    CYCLING, FOOT, INVALID
  }

  private CalorieUtils() {}

  /**
   * Resting VO2 is 3.5 milliliters per kilogram of body weight per minute
   * (ml/kg/min). The resting VO2 is the same for everyone.
   */
  private static final double RESTING_VO2 = 3.5;

  /**
   * Ratio to convert liter to kcal.
   */
  private static final double L_TO_KCAL = 5.0;

  /**
   * Standard gravity in meters per second squared (m/s^2).
   */
  private static final double EARTH_GRAVITY = 9.80665;

  /**
   * Lumped constant for all frictional losses (tires, bearings, chain).
   */
  private static final double K1 = 0.0053;

  /**
   * Lumped constant for aerodynamic drag.
   */
  private static final double K2 = 0.185;

  /**
   * Critical speed running in meters per minute. Converts 4.5 miles per hour to
   * meters per minute. 4 miles per hour is regarded as the max speed of walking
   * and 5 miles per hour is regarded as the minimal speed of running, so we use
   * 4.5 miles per hour as the critical speed. <a href=
   * "http://www.ideafit.com/fitness-library/calculating-caloric-expenditure-0"
   * >Reference</a>
   */
  @VisibleForTesting
  static final double CRTICAL_SPEED_RUNNING = 4.5 * UnitConversions.MI_TO_KM
      * UnitConversions.KM_TO_M / UnitConversions.HR_TO_MIN;

  /**
   * Gets the activity type.
   * 
   * @param context the context
   * @param activityType the activity type
   */
  public static ActivityType getActivityType(Context context, String activityType) {
    if (activityType == null || activityType.equals("")) {
      return ActivityType.INVALID;
    }
    if (TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.WALK)
        || TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.RUN)) {
      return ActivityType.FOOT;
    } else if (TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.BIKE)) {
      return ActivityType.CYCLING;
    }
    return ActivityType.INVALID;
  }

  /**
   * Calculates the track calorie in kcal.
   * 
   * @param context the context
   * @param track the track
   * @param startTrackPointId the starting track point id. -1L to calculate the
   *          entire track
   */
  public static double getTrackCalorie(Context context, Track track, long startTrackPointId) {
    ActivityType activityType = getActivityType(context, track.getCategory());
    if (activityType == ActivityType.INVALID) {
      return 0.0;
    }

    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    LocationIterator iterator = myTracksProviderUtils.getTrackPointLocationIterator(
        track.getId(), startTrackPointId, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    TripStatisticsUpdater tripStatisticsUpdater = new TripStatisticsUpdater(
        track.getTripStatistics().getStartTime());
    int recordingDistanceInterval = PreferencesUtils.getInt(context,
        R.string.recording_distance_interval_key,
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);
    int statsWeight = PreferencesUtils.getInt(
        context, R.string.stats_weight_key, PreferencesUtils.STATS_WEIGHT_DEFAULT);
    while (iterator.hasNext()) {
      tripStatisticsUpdater.addLocation(
          iterator.next(), recordingDistanceInterval, true, activityType, statsWeight);
    }
    return tripStatisticsUpdater.getTripStatistics().getCalorie();
  }

  /**
   * Gets the calorie in kcal between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade
   * @param weight the weight in kilogram. For cycling, weight of the rider plus
   *          bike. For foot, weight of the user
   * @param activityType the activity type
   */
  public static double getCalorie(
      Location start, Location stop, double grade, int weight, ActivityType activityType) {
    if (activityType == ActivityType.INVALID) {
      return 0.0;
    }
    return ActivityType.CYCLING == activityType ? getCyclingCalorie(start, stop, grade, weight)
        : getFootCalorie(start, stop, grade, weight);
  }

  /**
   * Gets the cycling calorie in kcal between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade
   * @param weight the weight in kilogram of the rider plus bike
   */
  @VisibleForTesting
  static double getCyclingCalorie(Location start, Location stop, double grade, int weight) {
    // Gets duration in seconds
    double duration = (double) (stop.getTime() - start.getTime()) * UnitConversions.MS_TO_S;
    // Get speed in meters per second
    double speed = (start.getSpeed() + stop.getSpeed()) / 2.0;

    if (grade < 0) {
      grade = 0.0;
    }
    return getCyclingCalorie(speed, grade, weight, duration);
  }

  /**
   * Gets the cycling calorie in kcal using the following equation: <br>
   * P = g * m * Vg * (K1 + s) + K2 * (Va)^2 * Vg
   * <ul>
   * <li>P - Power in watt (Joule/second)</li>
   * <li>g - gravity, using 9.80665 meter/second^2</li>
   * <li>m - mass of the rider plus bike in kilogram</li>
   * <li>s - surface grade</li>
   * <li>Vg - ground relative speed in meter/second</li>
   * <li>Va - air relative speed. We assume the air speed is zero, so Va is
   * equal to Vg.</li>
   * <li>K1 - a constant which represents frictional losses, using 0.0053.</li>
   * <li>K2 - a constant representing aerodynamic drag, using 0.185.</li>
   * <li></li>
   * </ul>
   * 
   * @param speed the speed in meters per second
   * @param grade the grade
   * @param weight weight in kilogram of the rider plus bike
   * @param duration the duration in seconds
   */
  @VisibleForTesting
  static double getCyclingCalorie(double speed, double grade, int weight, double duration) {
    // Get the power in watt (Joule/second)
    double power = EARTH_GRAVITY * weight * speed * (K1 + grade) + K2 * (speed * speed * speed);

    // Get the calories in kcal
    return power * duration * UnitConversions.J_TO_KCAL;
  }

  /**
   * Gets the foot calorie in kcal between two locations.
   * 
   * @param start the start location
   * @param stop the stop location
   * @param grade the grade
   * @param weight the weight of the user in kilogram
   */
  @VisibleForTesting
  static double getFootCalorie(Location start, Location stop, double grade, int weight) {
    // Get speed in meters per second
    double averageSpeed = (start.getSpeed() + stop.getSpeed()) / 2.0;
    // Get VO2 in mL/kg/min
    double vo2 = getFootVo2(averageSpeed, grade);
    // Duration in minutes
    double duration = (double) (stop.getTime() - start.getTime()) * UnitConversions.MS_TO_S
        * UnitConversions.S_TO_MIN;
    /*
     * Get the calorie. The unit of calorie is kcal which is came from
     * (mL/kg/min * min * kg * L/mL * kcal/L)
     */
    return vo2 * duration * weight * UnitConversions.ML_TO_L * L_TO_KCAL;
  }

  /**
   * Gets the foot VO2 value in ml/kg/min.
   * 
   * @param speed the speed in meters per second
   * @param grade the grade
   */
  @VisibleForTesting
  static double getFootVo2(double speed, double grade) {
    if (grade < 0) {
      grade = 0.0;
    }
    return speed > CRTICAL_SPEED_RUNNING ? getHighSpeedFootVo2(speed, grade)
        : getLowSpeedFootVo2(speed, grade);
  }

  /**
   * Gets the high speed foot VO2 in ml/kg/min. This equation is appropriate for
   * speeds greater than 4.5 mph like running.
   * 
   * @param speed the speed in meters per second
   * @param grade the grade
   */
  @VisibleForTesting
  static double getHighSpeedFootVo2(double speed, double grade) {
    // Change from meters per second to meters per minute
    speed = speed / UnitConversions.S_TO_MIN;

    /*
     * 0.2 means oxygen cost per meter of moving each kilogram (kg) of body
     * weight while running (horizontally). 0.9 means oxygen cost per meter of
     * moving total body mass against gravity (vertically).
     */
    return 0.2 * speed + 0.9 * speed * Math.abs(grade) + RESTING_VO2;
  }

  /**
   * Gets the low speed foot VO2 in ml/kg/min. This equation is appropriate for
   * speed less than 4.5 mph like walking.
   * 
   * @param speed the speed in meters per second
   * @param grade the grade
   */
  @VisibleForTesting
  static double getLowSpeedFootVo2(double speed, double grade) {
    // Change from meters per second to meters per minute
    speed = speed / UnitConversions.S_TO_MIN;

    /*
     * 0.1 means oxygen cost per meter of moving each kilogram (kg) of body
     * weight while walking (horizontally). 1.8 means oxygen cost per meter of
     * moving total body mass against gravity (vertically).
     */
    return 0.1 * speed + 1.8 * speed * Math.abs(grade) + RESTING_VO2;
  }
}