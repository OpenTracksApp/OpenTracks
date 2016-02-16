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
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.stats.TripStatisticsUpdater;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.database.Cursor;
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
    CYCLING, RUNNING, WALKING, INVALID
  }

  private CalorieUtils() {}

  /**
   * Resting VO2 is 3.5 milliliters per kilogram of body weight per minute
   * (ml/kg/min). The resting VO2 is the same for everyone.
   */
  private static final double RESTING_VO2 = 3.5;

  /**
   * Standard gravity in meters per second squared (m/s^2).
   */
  private static final double EARTH_GRAVITY = 9.81;

  /**
   * Lumped constant for all frictional losses (tires, bearings, chain).
   */
  private static final double K1 = 0.0053;

  /**
   * Lumped constant for aerodynamic drag (kg/m)
   */
  private static final double K2 = 0.185;

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
    if (TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.WALK)) {
      return ActivityType.WALKING;
    } else if (TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.RUN)) {
      return ActivityType.RUNNING;
    } else if (TrackIconUtils.getIconValue(context, activityType).equals(TrackIconUtils.BIKE)) {
      return ActivityType.CYCLING;
    }
    return ActivityType.INVALID;
  }

  /**
   * Updates calories for a track and its waypoints.
   * 
   * @param context the context
   * @param track the track
   * @return an array of two doubles, first is the track calorie, second is the
   *         last statistics waypoint calorie
   */
  public static double[] updateTrackCalorie(Context context, Track track) {
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    ActivityType activityType = getActivityType(context, track.getCategory());

    if (activityType == ActivityType.INVALID) {
      clearCalorie(myTracksProviderUtils, track);
      return new double[] { 0.0, 0.0 };
    }

    TripStatisticsUpdater trackTripStatisticsUpdater = new TripStatisticsUpdater(
        track.getTripStatistics().getStartTime());
    TripStatisticsUpdater markerTripStatisticsUpdater = new TripStatisticsUpdater(
        track.getTripStatistics().getStartTime());
    int recordingDistanceInterval = PreferencesUtils.getInt(context,
        R.string.recording_distance_interval_key,
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);
    double weight = PreferencesUtils.getFloat(
        context, R.string.weight_key, PreferencesUtils.getDefaultWeight(context));
    LocationIterator locationIterator = null;
    Cursor cursor = null;

    try {
      Waypoint waypoint = null;

      locationIterator = myTracksProviderUtils.getTrackPointLocationIterator(
          track.getId(), -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
      cursor = myTracksProviderUtils.getWaypointCursor(track.getId(), -1L, -1);

      if (cursor != null && cursor.moveToFirst()) {
        /*
         * Yes, this will skip the first waypoint and that is intentional as the
         * first waypoint holds the stats for the track.
         */
        waypoint = getNextStatisticsWaypoint(myTracksProviderUtils, cursor);
      }

      while (locationIterator.hasNext()) {
        Location location = locationIterator.next();
        trackTripStatisticsUpdater.addLocation(
            location, recordingDistanceInterval, true, activityType, weight);
        markerTripStatisticsUpdater.addLocation(
            location, recordingDistanceInterval, true, activityType, weight);

        if (waypoint != null && waypoint.getLocation().getTime() == location.getTime()
            && waypoint.getLocation().getLatitude() == location.getLatitude()
            && waypoint.getLocation().getLongitude() == location.getLongitude()) {
          waypoint.getTripStatistics()
              .setCalorie(markerTripStatisticsUpdater.getTripStatistics().getCalorie());
          myTracksProviderUtils.updateWaypoint(waypoint);
          markerTripStatisticsUpdater = new TripStatisticsUpdater(location.getTime());
          waypoint = getNextStatisticsWaypoint(myTracksProviderUtils, cursor);
        }
      }
    } finally {
      if (locationIterator != null) {
        locationIterator.close();
      }
      if (cursor != null) {
        cursor.close();
      }
    }
    double trackCalorie = trackTripStatisticsUpdater.getTripStatistics().getCalorie();
    track.getTripStatistics().setCalorie(trackCalorie);
    myTracksProviderUtils.updateTrack(track);
    return new double[] {
        trackCalorie, markerTripStatisticsUpdater.getTripStatistics().getCalorie() };
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
      Location start, Location stop, double grade, double weight, ActivityType activityType) {
    if (activityType == ActivityType.INVALID) {
      return 0.0;
    }

    if (grade < 0) {
      grade = 0.0;
    }

    // Speed in m/s
    double speed = (start.getSpeed() + stop.getSpeed()) / 2.0;
    // Duration in min
    double duration = (double) (stop.getTime() - start.getTime()) * UnitConversions.MS_TO_S
        * UnitConversions.S_TO_MIN;

    if (activityType == ActivityType.CYCLING) {
      /*
       * Power in watt (Joule/second). See
       * http://en.wikipedia.org/wiki/Bicycle_performance.
       */
      double power = EARTH_GRAVITY * weight * speed * (K1 + grade) + K2 * (speed * speed * speed);

      // WorkRate in kgm/min
      double workRate = power * UnitConversions.W_TO_KGM;

      /*
       * VO2 in kgm/min/kg 1.8 = oxygen cost of producing 1 kgm/min of power
       * output. 7 = oxygen cost of unloaded cycling plus resting oxygen
       * consumption
       */
      double vo2 = (1.8 * workRate / weight) + 7;

      // Calorie in kcal
      return vo2 * duration * weight * UnitConversions.KGM_TO_KCAL;
    } else {
      double vo2 = activityType == ActivityType.RUNNING ? getRunningVo2(speed, grade)
          : getWalkingVo2(speed, grade);

      /*
       * Calorie in kcal (mL/kg/min * min * kg * L/mL * kcal/L)
       */
      return vo2 * duration * weight * UnitConversions.ML_TO_L * UnitConversions.L_TO_KCAL;
    }
  }

  /**
   * Clears calorie in the track and its waypoints.
   * 
   * @param myTracksProviderUtils the my tracks provider utils
   * @param track the track
   */
  private static void clearCalorie(MyTracksProviderUtils myTracksProviderUtils, Track track) {
    track.getTripStatistics().setCalorie(0);
    myTracksProviderUtils.updateTrack(track);
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getWaypointCursor(track.getId(), -1L, -1);
      if (cursor != null && cursor.moveToFirst()) {
        /*
         * Yes, this will skip the first waypoint and that is intentional as the
         * first waypoint holds the stats for the track.
         */
        Waypoint waypoint = getNextStatisticsWaypoint(myTracksProviderUtils, cursor);
        while (waypoint != null) {
          waypoint.getTripStatistics().setCalorie(0);
          myTracksProviderUtils.updateWaypoint(waypoint);
          waypoint = getNextStatisticsWaypoint(myTracksProviderUtils, cursor);
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Gets the next statistics waypoint from a cursor.
   * 
   * @param myTracksProviderUtils the my tracks provider utils
   * @param cursor the cursor
   */
  private static Waypoint getNextStatisticsWaypoint(
      MyTracksProviderUtils myTracksProviderUtils, Cursor cursor) {
    if (cursor == null) {
      return null;
    }
    while (cursor.moveToNext()) {
      Waypoint waypoint = myTracksProviderUtils.createWaypoint(cursor);
      if (waypoint.getType() == WaypointType.STATISTICS) {
        return waypoint;
      }
    }
    return null;
  }

  /**
   * Gets the running VO2 in ml/kg/min. This equation is appropriate for speeds
   * greater than 5 mi/hr (or 3 mi/hr or greater if the subject is truly
   * jogging).
   * 
   * @param speed the speed in m/s
   * @param grade the grade
   */
  private static double getRunningVo2(double speed, double grade) {
    // Change from m/s to m/min
    speed = speed / UnitConversions.S_TO_MIN;

    /*
     * 0.2 = oxygen cost per meter of moving each kg of body weight while
     * running (horizontally). 0.9 = oxygen cost per meter of moving total body
     * mass against gravity (vertically).
     */
    return 0.2 * speed + 0.9 * speed * grade + RESTING_VO2;
  }

  /**
   * Gets the walking VO2 in ml/kg/min. This equation is appropriate for speed
   * from 1.9 to 4 mi/hr.
   * 
   * @param speed the speed in m/s
   * @param grade the grade
   */
  private static double getWalkingVo2(double speed, double grade) {
    // Change from m/s to m/min
    speed = speed / UnitConversions.S_TO_MIN;

    /*
     * 0.1 = oxygen cost per meter of moving each kilogram (kg) of body weight
     * while walking (horizontally). 1.8 = oxygen cost per meter of moving total
     * body mass against gravity (vertically).
     */
    return 0.1 * speed + 1.8 * speed * grade + RESTING_VO2;
  }
}