/*
 * Copyright 2010 Google Inc.
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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Statistical data about a trip.
 * The data in this class should be filled out by {@link TripStatistics}.
 *
 * TODO: Remove delegate methods from TripStatistics
 *       (detail is getTotalTime)
 * TODO: Make Waypoint and Track use this instead of having the same fields
 *
 * @author Rodrigo Damazio
 */
public class TripStatisticsData implements Parcelable {

  /**
   * The start time for the trip. This is system time which might not match gps
   * time.
   */
  long startTime;

  /**
   * The total time that we believe the user was traveling in milliseconds.
   */
  long movingTime;

  /**
   * The total time of the trip in milliseconds.
   * This is only updated when new points are received, so it may be stale.
   */
  long totalTime;

  /**
   * The total distance in meters that the user traveled on this trip.
   */
  double totalDistance;

  /**
   * The total elevation gained on this trip in meters.
   */
  double totalElevationGain;

  /**
   * The maximum speed in meters/second reported that we believe to be a valid
   * speed.
   */
  double maxSpeed;

  /**
   * The current speed in meters/second as reported by the gps.
   */
  double currentSpeed;

  /**
   * The current grade. This value is very noisy and not reported to the user.
   */
  double currentGrade;

  /**
   * The min and max latitude values seen in this trip.
   */
  final ExtremityMonitor latitudeExtremities = new ExtremityMonitor();

  /**
   * The min and max longitude values seen in this trip.
   */
  final ExtremityMonitor longitudeExtremities = new ExtremityMonitor();

  /**
   * The min and max elevation seen on this trip in meters.
   */
  final ExtremityMonitor elevationExtremities = new ExtremityMonitor();

  /**
   * The minimum and maximum grade calculations on this trip.
   */
  final ExtremityMonitor gradeExtremities = new ExtremityMonitor();

  /**
   * Gets the time that this track started.
   * 
   * @return The number of milliseconds since epoch to the time when this track
   *         started
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Gets the total time that this track has been active.
   * 
   * @return The total number of milliseconds the track was active
   */
  public long getTotalTime() {
    return totalTime;
  }

  /**
   * Gets the total distance the user traveled.
   * 
   * @return The total distance traveled in meters
   */
  public double getTotalDistance() {
    return totalDistance;
  }

  /**
   * Gets the the maximum speed for this track.
   * 
   * @return The maximum speed in m/s.
   */
  public double getMaxSpeed() {
    return maxSpeed;
  }

  /**
   * Gets the the current speed for this track.
   * 
   * @return The current speed in m/s
   */
  public double getCurrentSpeed() {
    return currentSpeed;
  }

  /**
   * Gets the moving time.
   * 
   * @return The total number of milliseconds the user was moving
   */
  public long getMovingTime() {
    return movingTime;
  }

  /**
   * Gets the total elevation gain for this trip. This is calculated as the sum
   * of all positive differences in the smoothed elevation.
   * 
   * @return The elevation gain in meters for this trip
   */
  public double getTotalElevationGain() {
    return totalElevationGain;
  }

  /**
   * Gets the current grade.
   * 
   * @return The current grade
   */
  public double getCurrentGrade() {
    return currentGrade;
  }

  /**
   * Returns the leftmost position (lowest longitude) of the track.
   */
  public int getLeft() {
    return (int) (longitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the rightmost position (highest longitude) of the track.
   */
  public int getRight() {
    return (int) (longitudeExtremities.getMax() * 1E6);
  }

  /**
   * Returns the bottommost position (lowest latitude) of the track.
   */
  public int getBottom() {
    return (int) (latitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the topmost position (highest latitude) of the track.
   */
  public int getTop() {
    return (int) (latitudeExtremities.getMax() * 1E6);
  }

  /**
   * Gets the minimum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be more than the current elevation.
   * 
   * @return The smallest elevation reading for this trip
   */
  public double getMinElevation() {
    return elevationExtremities.getMin();
  }

  /**
   * Gets the maximum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be less than the current elevation.
   * 
   * @return The largest elevation reading for this trip
   */
  public double getMaxElevation() {
    return elevationExtremities.getMax();
  }

  /**
   * Gets the maximum grade for this trip.
   * 
   * @return The maximum grade for this trip
   */
  public double getMaxGrade() {
    return gradeExtremities.getMax();
  }

  /**
   * Gets the minimum grade for this trip.
   * 
   * @return The minimum grade for this trip
   */
  public double getMinGrade() {
    return gradeExtremities.getMin();
  }

  // Parcelable interface and creator

  /**
   * Creator of statistics data from parcels.
   */
  public static class Creator
      implements Parcelable.Creator<TripStatisticsData> {

    @Override
    public TripStatisticsData createFromParcel(Parcel source) {
      TripStatisticsData data = new TripStatisticsData();

      data.startTime = source.readLong();
      data.movingTime = source.readLong();
      data.totalTime = source.readLong();
      data.totalDistance = source.readDouble();
      data.totalElevationGain = source.readDouble();
      data.maxSpeed = source.readDouble();
      data.currentSpeed = source.readDouble();
      data.currentGrade = source.readDouble();

      double minLat = source.readDouble();
      double maxLat = source.readDouble();
      data.latitudeExtremities.set(minLat, maxLat);

      double minLong = source.readDouble();
      double maxLong = source.readDouble();
      data.longitudeExtremities.set(minLong, maxLong);

      double minElev = source.readDouble();
      double maxElev = source.readDouble();
      data.elevationExtremities.set(minElev, maxElev);

      double minGrade = source.readDouble();
      double maxGrade = source.readDouble();
      data.gradeExtremities.set(minGrade, maxGrade);

      return data;
    }

    @Override
    public TripStatisticsData[] newArray(int size) {
      return new TripStatisticsData[size];
    }
  }

  public static final Creator CREATOR = new Creator();

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(startTime);
    dest.writeLong(movingTime);
    dest.writeLong(totalTime);
    dest.writeDouble(totalDistance);
    dest.writeDouble(totalElevationGain);
    dest.writeDouble(maxSpeed);
    dest.writeDouble(currentSpeed);
    dest.writeDouble(currentGrade);

    dest.writeDouble(latitudeExtremities.getMin());
    dest.writeDouble(latitudeExtremities.getMax());
    dest.writeDouble(longitudeExtremities.getMin());
    dest.writeDouble(longitudeExtremities.getMax());
    dest.writeDouble(elevationExtremities.getMin());
    dest.writeDouble(elevationExtremities.getMax());
    dest.writeDouble(gradeExtremities.getMin());
    dest.writeDouble(gradeExtremities.getMax());
  }
}