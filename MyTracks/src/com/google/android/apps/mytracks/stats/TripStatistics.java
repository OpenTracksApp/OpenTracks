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
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;
import android.util.Log;

/**
 * Statistics keeper for a trip.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TripStatistics {
  /**
   * Statistical data about the trip, which can be displayed to the user.
   */
  private final TripStatisticsData data = new TripStatisticsData();

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
  public TripStatistics() {
    data.startTime = System.currentTimeMillis();
  }

  /**
   * Creates a new trip at the given start time.
   * 
   * @param startTime the time that the trip started
   */
  public TripStatistics(long startTime) {
    data.startTime = startTime;
  }

  /**
   * Creates a new trip using the waypoint for start time and other information.
   * 
   * @param waypoint the waypoint to get starting information from
   */
  public TripStatistics(Waypoint waypoint) {
    data.startTime = waypoint.getStartTime();
    data.totalTime = waypoint.getTotalTime();
    data.movingTime = waypoint.getMovingTime();
    data.totalDistance = waypoint.getTotalDistance();
    data.totalElevationGain = waypoint.getTotalElevationGain();
    data.maxSpeed = waypoint.getMaxSpeed();
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
    totalLocations++;

    double elevationDifference = updateElevation(currentLocation.getAltitude());

    // Update the "instant" values:
    data.totalTime = systemTime - data.startTime;
    data.currentSpeed = currentLocation.getSpeed();

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
        && data.currentSpeed < MyTracksConstants.MAX_NO_MOVEMENT_SPEED) {
      lastLocation = currentLocation;
      return false;
    }

    data.totalDistance += lastMovingLocation.distanceTo(currentLocation);
    updateSpeed(currentLocation.getTime(), data.currentSpeed,
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
    data.latitudeExtremities.update(location.getLatitude());
    data.longitudeExtremities.update(location.getLongitude());
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
    data.elevationExtremities.update(smoothedElevation);
    double elevationDifference = elevationBuffer.isFull()
        ? smoothedElevation - oldSmoothedElevation
        : 0.0;
    if (elevationDifference > 0) {
      data.totalElevationGain += elevationDifference;
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
    data.movingTime += timeDifference;

    if (isValidSpeed(updateTime, speed, lastLocationTime, lastLocationSpeed,
        speedBuffer)) {
      speedBuffer.setNext(speed);
      if (speed > data.maxSpeed) {
        data.maxSpeed = speed;
      }
      double movingSpeed = getAverageMovingSpeed();
      if (speedBuffer.isFull() && (movingSpeed > data.maxSpeed)) {
        data.maxSpeed = movingSpeed;
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
    data.currentGrade = elevationDifference / smoothedDistance;
    gradeBuffer.setNext(data.currentGrade);
    data.gradeExtremities.update(gradeBuffer.getAverage());
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

  /**
   * Pauses the track at the current time.
   */
  public void pause() {
    pauseAt(System.currentTimeMillis());
  }

  /**
   * Pauses the track at the give time.
   * 
   * @param time the time to pause at
   */
  public void pauseAt(long time) {
    data.totalTime = time - data.startTime;
    lastLocation = null; // Make sure the counter restarts.
    paused = true;
  }

  /**
   * Resumes the current track.
   */
  public void resume() {
    // TODO: The total time is bogus after this, it will include the paused time
    paused = false;
  }

  /**
   * Gets the total time that this track has been active.
   * 
   * @return The total number of milliseconds the track was active
   */
  public long getTotalTime() {
    return paused
        ? data.totalTime
        : System.currentTimeMillis() - data.startTime;
  }

  /**
   * Gets the moving time.
   * 
   * @return The total number of milliseconds the user was moving
   */
  public long getMovingTime() {
    return data.movingTime;
  }

  /**
   * Sets the moving time.
   * 
   * @parm movingTime The total number of milliseconds the user was moving
   */
  public void setMovingTime(long movingTime) {
    data.movingTime = movingTime;
  }

  /**
   * Gets the total distance the user traveled.
   * 
   * @return The total distance traveled in meters
   */
  public double getTotalDistance() {
    return data.totalDistance;
  }

  /**
   * Gets the the average speed the user traveled.
   * 
   * @return The average speed in m/s
   */
  public double getAverageSpeed() {
    return getTotalDistance() / ((double) getTotalTime() / 1000);
  }

  /**
   * Gets the the average speed the user traveled when they were actively
   * moving.
   * 
   * @return The average moving speed in m/s
   */
  public double getAverageMovingSpeed() {
    return getTotalDistance() / ((double) getMovingTime() / 1000);
  }

  /**
   * Gets the the maximum speed for this track.
   * 
   * @return The maximum speed in m/s.
   */
  public double getMaxSpeed() {
    return data.maxSpeed;
  }

  /**
   * Gets the the current speed for this track.
   * 
   * @return The current speed in m/s
   */
  public double getCurrentSpeed() {
    return data.currentSpeed;
  }

  @Override
  public String toString() {
    return "TripStatistics:" + " Start Time: " + data.startTime
        + " Total Time: " + data.totalTime + " Moving Time: " + data.movingTime
        + " Total Distance: " + data.totalDistance
        + " Elevation Gain: " + data.totalElevationGain
        + " Min Elevation: " + getMinElevation()
        + " Max Elevation: " + getMaxElevation()
        + " Average Speed: " + getAverageMovingSpeed()
        + " Min Grade: " + getMinGrade() + " Max Grade: " + getMaxGrade()
        + " Total Locations: " + totalLocations;
  }

  /**
   * Gets the minimum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be more than the current elevation.
   * 
   * @return The smallest elevation reading for this trip
   */
  public double getMinElevation() {
    return data.elevationExtremities.getMin();
  }

  /**
   * Gets the maximum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be less than the current elevation.
   * 
   * @return The largest elevation reading for this trip
   */
  public double getMaxElevation() {
    return data.elevationExtremities.getMax();
  }

  /**
   * Gets the total elevation gain for this trip. This is calculated as the sum
   * of all positive differences in the smoothed elevation.
   * 
   * @return The elevation gain in meters for this trip
   */
  public double getTotalElevationGain() {
    return data.totalElevationGain;
  }

  /**
   * Gets the time that this track started.
   * 
   * @return The number of milliseconds since epoch to the time when this track
   *         started
   */
  public long getStartTime() {
    return data.startTime;
  }

  /**
   * Gets the current grade.
   * 
   * @return The current grade
   */
  public double getGrade() {
    return data.currentGrade;
  }

  /**
   * Gets the maximum grade for this trip.
   * 
   * @return The maximum grade for this trip
   */
  public double getMaxGrade() {
    return data.gradeExtremities.getMax();
  }

  /**
   * Gets the minimum grade for this trip.
   * 
   * @return The minimum grade for this trip
   */
  public double getMinGrade() {
    return data.gradeExtremities.getMin();
  }

  /**
   * Returns the amount of time the user has been idle or 0 if they are moving.
   */
  public long getIdleTime() {
    return lastLocation.getTime() - lastMovingLocation.getTime();
  }

  /**

   * Returns the leftmost position (lowest longitude) of the track.
   */
  public int getLeft() {
    return (int) (data.longitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the rightmost position (highest longitude) of the track.
   */
  public int getRight() {
    return (int) (data.longitudeExtremities.getMax() * 1E6);
  }

  /**
   * Returns the bottommost position (lowest latitude) of the track.
   */
  public int getBottom() {
    return (int) (data.latitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the topmost position (highest latitude) of the track.
   */
  public int getTop() {
    return (int) (data.latitudeExtremities.getMax() * 1E6);
  }

  public TripStatisticsData getData() {
    return data;
  }

  /**
   * Fills the given track with statistics about itself, calculated by this
   * statitics class.
   */
  public void fillStatisticsForTrack(Track track) {
    track.setTotalDistance(getTotalDistance());
    track.setTotalTime(getTotalTime());
    track.setMovingTime(getMovingTime());
    track.setAverageSpeed(getAverageSpeed());
    track.setAverageMovingSpeed(getAverageMovingSpeed());
    track.setMaxSpeed(getMaxSpeed());
    track.setMinElevation(getMinElevation());
    track.setMaxElevation(getMaxElevation());
    track.setTotalElevationGain(getTotalElevationGain());
    track.setMinGrade(getMinGrade());
    track.setMaxGrade(getMaxGrade());

    track.setBounds(getLeft(), getTop(), getRight(), getBottom());
  }

  /**
   * Write all of the statistics fields to the waypoint.
   * @param waypoint The waypoint to write the trip statistics to
   */
  public void fillStatisticsForWaypoint(Waypoint waypoint) {
    waypoint.setTotalDistance(getTotalDistance());
    waypoint.setTotalTime(getTotalTime());
    waypoint.setMovingTime(getMovingTime());
    waypoint.setAverageSpeed(getAverageSpeed());
    waypoint.setAverageMovingSpeed(getAverageMovingSpeed());
    waypoint.setMaxSpeed(getMaxSpeed());
    waypoint.setMinElevation(getMinElevation());
    waypoint.setMaxElevation(getMaxElevation());
    waypoint.setTotalElevationGain(getTotalElevationGain());
    waypoint.setMinGrade(getMinGrade());
    waypoint.setMaxGrade(getMaxGrade());
  }
}

