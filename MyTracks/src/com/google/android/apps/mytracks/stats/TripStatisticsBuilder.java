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
 * Builder for {@link TripStatistics}. For keeping statistics as a track is
 * paused/resumed and new locations are added.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TripStatisticsBuilder {

  // The trip statistics.
  private final TripStatistics tripStatistics;

  // The minimum recording distance.
  private int minRecordingDistance = PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT;

  // True if the trip is paused. All trips start as paused.
  private boolean paused = true;

  // The last location as reported by GPS.
  private Location lastLocation;

  // The last moving location that contributed to the moving statistics.
  private Location lastMovingLocation;

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
   * Creates a new trip starting at a start time.
   * 
   * @param startTime the start time
   */
  public TripStatisticsBuilder(long startTime) {
    tripStatistics = new TripStatistics();
    resumeAt(startTime);
  }

  /**
   * Creates a new trip, starting with an existing {@link TripStatistics}.
   * 
   * @param other the existing {@link TripStatistics}
   */
  public TripStatisticsBuilder(TripStatistics other) {
    tripStatistics = new TripStatistics(other);
    if (tripStatistics.getStartTime() > 0) {
      resumeAt(tripStatistics.getStartTime());
    }
  }

  /**
   * Sets the min recording distance.
   * 
   * @param minRecordingDistance the min recording distance
   */
  public void setMinRecordingDistance(int minRecordingDistance) {
    this.minRecordingDistance = minRecordingDistance;
  }

  /**
   * Resumes the current track at a given time.
   * 
   * @param time the time
   */
  public void resumeAt(long time) {
    if (!paused) {
      return;
    }

    tripStatistics.setStartTime(time);
    tripStatistics.setStopTime(-1L);
    paused = false;
    lastLocation = null;
    lastMovingLocation = null;
    speedBuffer.reset();
    elevationBuffer.reset();
    distanceBuffer.reset();
    gradeBuffer.reset();
  }

  /**
   * Pauses the track at a given time.
   * 
   * @param time the time to pause at
   */
  public void pauseAt(long time) {
    if (paused) {
      return;
    }
    tripStatistics.setStopTime(time);
    // TODO: total time needs to take into account pauses
    tripStatistics.setTotalTime(time - tripStatistics.getStartTime());
    paused = true;
  }

  /**
   * Gets the trip statistics.
   */
  public TripStatistics getTripStatistics() {
    // Take a snapshot - we don't want anyone messing with our internals
    return new TripStatistics(tripStatistics);
  }

  /**
   * Returns the amount of time the user has been idle or 0 if he is moving.
   */
  public long getIdleTime() {
    if (lastLocation == null || lastMovingLocation == null) {
      return 0;
    }
    return lastLocation.getTime() - lastMovingLocation.getTime();
  }

  /**
   * Gets the smoothed elevation over several readings. The elevation readings
   * is noisy so the smoothed elevation is better than the raw elevation for
   * many tasks.
   */
  public double getSmoothedElevation() {
    return elevationBuffer.getAverage();
  }

  /**
   * Adds a location. This will update all of the internal variables with this
   * new location.
   * 
   * @param location the location
   * @param systemTime the system time for calculating totalTime. This should be
   *          the phone's system time (not GPS time)
   * @return true if the person is moving
   */
  public boolean addLocation(Location location, long systemTime) {
    if (paused) {
      Log.w(TAG, "Track is paused. Ignore addLocation.");
      return false;
    }

    tripStatistics.setTotalTime(systemTime - tripStatistics.getStartTime());

    double elevationDifference = updateElevation(location.getAltitude());
    tripStatistics.updateLatitudeExtremities(location.getLatitude());
    tripStatistics.updateLongitudeExtremities(location.getLongitude());

    // If this is the first location, remember it and return.
    if (lastLocation == null || lastMovingLocation == null) {
      lastLocation = location;
      lastMovingLocation = location;
      return false;
    }

    // Don't do anything more if we didn't move since the last location.
    double distance = lastLocation.distanceTo(location);
    if (distance < minRecordingDistance && location.getSpeed() < Constants.MAX_NO_MOVEMENT_SPEED) {
      lastLocation = location;
      return false;
    }

    long timeDifference = location.getTime() - lastLocation.getTime();
    if (timeDifference < 0) {
      Log.e(TAG, "Negative time difference: " + timeDifference);
      lastLocation = location;
      return false;
    }

    tripStatistics.addTotalDistance(lastMovingLocation.distanceTo(location));
    tripStatistics.addMovingTime(timeDifference);
    updateSpeed(
        location.getTime(), location.getSpeed(), lastLocation.getTime(), lastLocation.getSpeed());
    updateGrade(distance, elevationDifference);
    lastLocation = location;
    lastMovingLocation = location;
    return true;
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
