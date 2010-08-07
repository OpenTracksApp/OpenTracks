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

import com.google.android.apps.mytracks.MyTracksConstants;

import android.location.Location;
import android.util.Log;

/**
 * Statistics keeper for a trip.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TripStatisticsBuilder {
  /**
   * Statistical data about the trip, which can be displayed to the user.
   */
  private final TripStatistics data;

  /**
   * The last location that the gps reported.
   */
  private Location lastLocation = null;

  /**
   * The last location that contributed to the stats. It is also the last
   * location the user was found to be moving.
   */
  private Location lastMovingLocation = null;

  /**
   * The current speed in meters/second as reported by the gps.
   */
  private double currentSpeed;

  /**
   * The current grade. This value is very noisy and not reported to the user.
   */
  private double currentGrade;

  /**
   * Is the trip currently paused?
   * All trips start paused.
   */
  private boolean paused = true;

  /**
   * A buffer of the last speed readings in meters/second.
   */
  private final DoubleBuffer speedBuffer =
      new DoubleBuffer(MyTracksConstants.SPEED_SMOOTHING_FACTOR);

  /**
   * A buffer of the recent elevation readings in meters.
   */
  private final DoubleBuffer elevationBuffer =
      new DoubleBuffer(MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);

  /**
   * A buffer of the distance between recent gps readings in meters.
   */
  private final DoubleBuffer distanceBuffer =
      new DoubleBuffer(MyTracksConstants.DISTANCE_SMOOTHING_FACTOR);

  /**
   * A buffer of the recent grade calculations.
   */
  private final DoubleBuffer gradeBuffer =
      new DoubleBuffer(MyTracksConstants.GRADE_SMOOTHING_FACTOR);

  /**
   * The total number of locations in this trip.
   */
  private long totalLocations = 0;

  /**
   * Creates a new trip starting at the current system time.
   */
  public TripStatisticsBuilder() {
    data = new TripStatistics();
  }

  /**
   * Creates a new trip, starting with existing statistics data.
   *
   * @param statsData the statistics data to copy and start from
   */
  public TripStatisticsBuilder(TripStatistics statsData) {
    data = new TripStatistics(statsData);
  }

  /**
   * Adds a location to the current trip. This will update all of the internal
   * variables with this new location.
   * 
   * @param currentLocation the current gps location
   * @param systemTime the time used for calculation of totalTime. This should
   *        be the phone's time (not GPS time)
   * @return true if the person is moving
   */
  public boolean addLocation(Location currentLocation, long systemTime) {
    if (paused) {
      Log.w(MyTracksConstants.TAG,
          "Tried to account for location while track is paused");
      return false;
    }

    totalLocations++;

    double elevationDifference = updateElevation(currentLocation.getAltitude());

    // Update the "instant" values:
    data.setTotalTime(systemTime - data.getStartTime());
    currentSpeed = currentLocation.getSpeed();

    // This was the 1st location added, remember it and do nothing else:
    if (lastLocation == null) {
      lastLocation = currentLocation;
      lastMovingLocation = currentLocation;
      return false;
    }

    updateBounds(currentLocation);

    // Don't do anything if we didn't move since last fix:
    double distance = lastLocation.distanceTo(currentLocation);
    if (distance < MyTracksConstants.MAX_NO_MOVEMENT_DISTANCE
        && currentSpeed < MyTracksConstants.MAX_NO_MOVEMENT_SPEED) {
      lastLocation = currentLocation;
      return false;
    }

    data.addTotalDistance(lastMovingLocation.distanceTo(currentLocation));
    updateSpeed(currentLocation.getTime(), currentSpeed,
        lastLocation.getTime(), lastLocation.getSpeed());

    updateGrade(distance, elevationDifference);
    lastLocation = currentLocation;
    lastMovingLocation = currentLocation;
    return true;
  }

  /**
   * Updates the track's bounding box to include the given location.
   */
  private void updateBounds(Location location) {
    data.updateLatitudeExtremities(location.getLatitude());
    data.updateLongitudeExtremities(location.getLongitude());
  }

  /**
   * Updates the elevation measurements.
   * 
   * @param elevation the current elevation
   */
  // @VisibleForTesting
  double updateElevation(double elevation) {
    double oldSmoothedElevation = getSmoothedElevation();
    elevationBuffer.setNext(elevation);
    double smoothedElevation = getSmoothedElevation();
    data.updateElevationExtremities(smoothedElevation);
    double elevationDifference = elevationBuffer.isFull()
        ? smoothedElevation - oldSmoothedElevation
        : 0.0;
    if (elevationDifference > 0) {
      data.addTotalElevationGain(elevationDifference);
    }
    return elevationDifference;
  }

  /**
   * Updates the speed measurements.
   * 
   * @param updateTime the time of the speed update
   * @param speed the current speed
   * @param lastLocationTime the time of the last speed update
   * @param lastLocationSpeed the speed of the last update
   */
  // @VisibleForTesting
  void updateSpeed(long updateTime, double speed, long lastLocationTime,
      double lastLocationSpeed) {
    // We are now sure the user is moving.
    long timeDifference = updateTime - lastLocationTime;
    if (timeDifference < 0) {
      Log.e(MyTracksConstants.TAG,
          "Found negative time change: " + timeDifference);
    }
    data.addMovingTime(timeDifference);

    if (isValidSpeed(updateTime, speed, lastLocationTime, lastLocationSpeed,
        speedBuffer)) {
      speedBuffer.setNext(speed);
      if (speed > data.getMaxSpeed()) {
        data.setMaxSpeed(speed);
      }
      double movingSpeed = data.getAverageMovingSpeed();
      if (speedBuffer.isFull() && (movingSpeed > data.getMaxSpeed())) {
        data.setMaxSpeed(movingSpeed);
      }
    } else {
      Log.d(MyTracksConstants.TAG,
          "TripStatistics ignoring big change: Raw Speed: " + speed
          + " old: " + lastLocationSpeed + " [" + toString() + "]");
    }
  }

  /**
   * Checks to see if this is a valid speed.
   * 
   * @param updateTime The time at the current reading
   * @param speed The current speed
   * @param lastLocationTime The time at the last location
   * @param lastLocationSpeed Speed at the last location
   * @param speedBuffer A buffer of recent readings
   * @return True if this is likely a valid speed
   */
  public static boolean isValidSpeed(long updateTime, double speed,
      long lastLocationTime, double lastLocationSpeed,
      DoubleBuffer speedBuffer) {

    // We don't want to count 0 towards the speed.
    if (speed == 0) {
      return false;
    }
    // We are now sure the user is moving.
    long timeDifference = updateTime - lastLocationTime;

    // There are a lot of noisy speed readings.
    // Do the cheapest checks first, most expensive last.
    // The following code will ignore unlikely to be real readings.
    // - 128 m/s seems to be an internal android error code.
    if (Math.abs(speed - 128) < 1) {
      return false;
    }

    // Another check for a spurious reading. See if the path seems physically
    // likely. Ignore any speeds that imply accelaration greater than 2g's
    // Really who can accelerate faster?
    double speedDifference = Math.abs(lastLocationSpeed - speed);
    if (speedDifference > MyTracksConstants.MAX_ACCELERATION * timeDifference) {
      return false;
    }

    // There are three additional checks if the reading gets this far:
    // - Only use the speed if the buffer is full
    // - Check that the current speed is less than 10x the recent smoothed speed
    // - Double check that the current speed does not imply crazy acceleration
    double smoothedSpeed = speedBuffer.getAverage();
    double smoothedDiff = Math.abs(smoothedSpeed - speed);
    return !speedBuffer.isFull() ||
        (speed < smoothedSpeed * 10
         && smoothedDiff < MyTracksConstants.MAX_ACCELERATION * timeDifference);
  }

  /**
   * Updates the grade measurements.
   * 
   * @param distance the distance the user just traveled
   * @param elevationDifference the elevation difference between the current
   *        reading and the previous reading
   */
  // @VisibleForTesting
  void updateGrade(double distance, double elevationDifference) {
    distanceBuffer.setNext(distance);
    double smoothedDistance = distanceBuffer.getAverage();

    // With the error in the altitude measurement it is dangerous to divide
    // by anything less than 5.
    if (!elevationBuffer.isFull() || !distanceBuffer.isFull()
        || smoothedDistance < 5.0) {
      return;
    }
    currentGrade = elevationDifference / smoothedDistance;
    gradeBuffer.setNext(currentGrade);
    data.updateGradeExtremities(gradeBuffer.getAverage());
  }

  /**
   * Pauses the track at the current time.
   */
  public void pause() {
    pauseAt(System.currentTimeMillis());
  }

  /**
   * Pauses the track at the given time.
   * 
   * @param time the time to pause at
   */
  public void pauseAt(long time) {
    if (paused) { return; }

    data.setStopTime(time);
    data.setTotalTime(time - data.getStartTime());
    lastLocation = null; // Make sure the counter restarts.
    paused = true;
  }

  /**
   * Resumes the current track at the current time.
   */
  public void resume() {
    resumeAt(System.currentTimeMillis());
  }

  /**
   * Resumes the current track at the given time.
   *
   * @param time the time to resume at
   */
  public void resumeAt(long time) {
    if (!paused) { return; }

    // TODO: The times are bogus if the track is paused then resumed again
    data.setStartTime(time);
    data.setStopTime(-1);
    paused = false;
  }

  @Override
  public String toString() {
    return "TripStatistics { Data: " + data.toString()
         + "; Total Locations: " + totalLocations
         + "; Paused: " + paused
         + "; Current speed: " + currentSpeed
         + "; Current grade: " + currentGrade
         + "}";
  }

  /**
   * Returns the amount of time the user has been idle or 0 if they are moving.
   */
  public long getIdleTime() {
    return lastLocation.getTime() - lastMovingLocation.getTime();
  }

  /**
   * Gets the current elevation smoothed over several readings. The elevation
   * data is very noisy so it is better to use the smoothed elevation than the
   * raw elevation for many tasks.
   * 
   * @return The elevation smoothed over several readings
   */
  public double getSmoothedElevation() {
    return elevationBuffer.getAverage();
  }

  public TripStatistics getStatistics() {
    // Take a snapshot - we do't want anyone messing with our internals
    return new TripStatistics(data);
  }
}

