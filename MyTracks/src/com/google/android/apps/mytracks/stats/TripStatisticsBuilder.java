/*
 * Copyright 2009 Google Inc.
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

package com.google.android.apps.mytracks.stats;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.common.annotations.VisibleForTesting;

import android.location.Location;
import android.util.Log;

/**
 * Builder for {@link TripStatistics}. For keeping track statistics as new
 * locations are added.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TripStatisticsBuilder {

  // The trip statistics.
  private final TripStatistics tripStatistics;

  // A buffer of the recent speed readings (m/s) for calculating max speed.
  private final DoubleBuffer speedBuffer = new DoubleBuffer(Constants.SPEED_SMOOTHING_FACTOR);

  // A buffer of the recent elevation readings (m).
  private final DoubleBuffer elevationBuffer = new DoubleBuffer(
      Constants.ELEVATION_SMOOTHING_FACTOR);

  // A buffer of the recent distance readings for calculating grade.
  private final DoubleBuffer distanceBuffer = new DoubleBuffer(Constants.DISTANCE_SMOOTHING_FACTOR);

  // A buffer of the recent grade calculations
  private final DoubleBuffer gradeBuffer = new DoubleBuffer(Constants.GRADE_SMOOTHING_FACTOR);

  /**
   * Creates a new {@link TripStatistics} starting at a start time.
   * 
   * @param startTime the start time
   */
  public TripStatisticsBuilder(long startTime) {
    tripStatistics = new TripStatistics();
    tripStatistics.setStartTime(startTime);
  }

  /**
   * Pauses the {@link TripStatistics} at a stop time.
   * 
   * @param stopTime the stop time
   */
  public void pauseAt(long stopTime) {
    tripStatistics.setStopTime(stopTime);
    tripStatistics.setTotalTime(stopTime - tripStatistics.getStartTime());
  }

  /**
   * Gets the {@link TripStatistics}.
   */
  public TripStatistics getTripStatistics() {
    // Take a snapshot - we don't want anyone messing with our tripStatistics
    return new TripStatistics(tripStatistics);
  }

  /**
   * Adds a location.
   * 
   * @param location the location
   * @param lastLocation the last location
   */
  public void addLocation(Location location, Location lastLocation) {
    addLocation(location, lastLocation, location.getTime(), false,
        PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
  }

  /**
   * Adds a location. This will update all of the internal variables with this
   * new location.
   * 
   * @param location the location
   * @param lastLocation the last location
   * @param time the time
   * @param alwaysAdd true to always return true
   * @param minRecordingDistance the min recording distance
   * @return true if the location should be added
   */
  public boolean addLocation(Location location, Location lastLocation, long time, boolean alwaysAdd,
      int minRecordingDistance) {
    pauseAt(time);

    double elevationDifference = updateElevation(location.getAltitude());
    tripStatistics.updateLatitudeExtremities(location.getLatitude());
    tripStatistics.updateLongitudeExtremities(location.getLongitude());

    // If lastLocation is null, returns true.
    if (lastLocation == null) {
      return true;
    }

    double distance = lastLocation.distanceTo(location);
    if (distance < minRecordingDistance && location.getSpeed() < Constants.MAX_NO_MOVEMENT_SPEED) {
      return alwaysAdd;
    }

    long movingTime = location.getTime() - lastLocation.getTime();
    if (movingTime < 0) {
      Log.e(TAG, "Negative moving time: " + movingTime);
      return alwaysAdd;
    }

    tripStatistics.addTotalDistance(distance);
    tripStatistics.addMovingTime(movingTime);
    updateSpeed(
        location.getTime(), location.getSpeed(), lastLocation.getTime(), lastLocation.getSpeed());
    updateGrade(distance, elevationDifference);
    return true;
  }

  /**
   * Gets the smoothed elevation over several readings. The elevation readings
   * is noisy so the smoothed elevation is better than the raw elevation for
   * many tasks.
   */
  @VisibleForTesting
  double getSmoothedElevation() {
    return elevationBuffer.getAverage();
  }

  /**
   * Updates a speed reading. Assumes the user is moving.
   * 
   * @param time the time
   * @param speed the speed
   * @param lastLocationTime the last location time
   * @param lastLocationSpeed the last location speed
   */
  @VisibleForTesting
  void updateSpeed(long time, double speed, long lastLocationTime, double lastLocationSpeed) {
    if (!isValidSpeed(time, speed, lastLocationTime, lastLocationSpeed, speedBuffer)) {
      Log.d(TAG, "Invalid speed. speed: " + speed + " lastLocationSpeed: " + lastLocationSpeed);
      return;
    }
    speedBuffer.setNext(speed);
    if (speed > tripStatistics.getMaxSpeed()) {
      tripStatistics.setMaxSpeed(speed);
    }
    double movingSpeed = tripStatistics.getAverageMovingSpeed();
    if (speedBuffer.isFull() && movingSpeed > tripStatistics.getMaxSpeed()) {
      tripStatistics.setMaxSpeed(movingSpeed);
    }
  }

  /**
   * Updates an elevation reading.
   * 
   * @param elevation the elevation
   */
  @VisibleForTesting
  double updateElevation(double elevation) {
    double oldAverage = elevationBuffer.getAverage();
    elevationBuffer.setNext(elevation);
    double newAverage = elevationBuffer.getAverage();
    tripStatistics.updateElevationExtremities(newAverage);
    double elevationDifference = elevationBuffer.isFull() ? newAverage - oldAverage : 0.0;
    if (elevationDifference > 0) {
      tripStatistics.addTotalElevationGain(elevationDifference);
    }
    return elevationDifference;
  }

  /**
   * Updates a grade reading.
   * 
   * @param distance the distance the user just traveled
   * @param elevationDifference the elevation difference between the current
   *          reading and the previous reading
   */
  @VisibleForTesting
  void updateGrade(double distance, double elevationDifference) {
    distanceBuffer.setNext(distance);
    double smoothedDistance = distanceBuffer.getAverage();

    /*
     * With the error in the altitude measurement it is dangerous to divide by
     * anything less than 5.
     */
    if (!elevationBuffer.isFull() || !distanceBuffer.isFull() || smoothedDistance < 5.0) {
      return;
    }
    gradeBuffer.setNext(elevationDifference / smoothedDistance);
    tripStatistics.updateGradeExtremities(gradeBuffer.getAverage());
  }

  /**
   * Returns true if the speed is valid.
   * 
   * @param time the time
   * @param speed the speed
   * @param lastLocationTime the last location time
   * @param lastLocationSpeed the last location speed
   * @param speedBuffer a buffer of speed readings
   */
  public static boolean isValidSpeed(long time, double speed, long lastLocationTime,
      double lastLocationSpeed, DoubleBuffer speedBuffer) {

    /*
     * There are a lot of noisy speed readings. Do the cheapest checks first,
     * most expensive last.
     */
    if (speed == 0) {
      return false;
    }

    /*
     * The following code will ignore unlikely readings. 128 m/s seems to be an
     * internal android error code.
     */
    if (Math.abs(speed - 128) < 1) {
      return false;
    }

    /*
     * See if the speed seems physically likely. Ignore any speeds that imply
     * acceleration greater than 2g.
     */
    long timeDifference = time - lastLocationTime;
    double speedDifference = Math.abs(lastLocationSpeed - speed);
    if (speedDifference > Constants.MAX_ACCELERATION * timeDifference) {
      return false;
    }

    /*
     * Only check if the speed buffer is full. Check that the speed is less than
     * 10X the smoothed average and the speed difference doesn't imply 2g
     * acceleration.
     */
    if (!speedBuffer.isFull()) {
      return true;
    }
    double average = speedBuffer.getAverage();
    double diff = Math.abs(average - speed);
    return (speed < average * 10) && (diff < Constants.MAX_ACCELERATION * timeDifference);
  }
}
