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
 * The data in this class should be filled out by {@link TripStatisticsBuilder}.
 *
 * TODO: hashCode and equals
 *
 * @author Rodrigo Damazio
 */
public class TripStatistics implements Parcelable {

  /**
   * The start time for the trip. This is system time which might not match gps
   * time.
   */
  private long startTime = -1;

  /**
   * The stop time for the trip. This is the system time which might not match
   * gps time.
   */
  private long stopTime = -1;

  /**
   * The total time that we believe the user was traveling in milliseconds.
   */
  private long movingTime;

  /**
   * The total time of the trip in milliseconds.
   * This is only updated when new points are received, so it may be stale.
   */
  private long totalTime;

  /**
   * The total distance in meters that the user traveled on this trip.
   */
  private double totalDistance;

  /**
   * The total elevation gained on this trip in meters.
   */
  private double totalElevationGain;

  /**
   * The maximum speed in meters/second reported that we believe to be a valid
   * speed.
   */
  private double maxSpeed;

  /**
   * The min and max latitude values seen in this trip.
   */
  private final ExtremityMonitor latitudeExtremities = new ExtremityMonitor();

  /**
   * The min and max longitude values seen in this trip.
   */
  private final ExtremityMonitor longitudeExtremities = new ExtremityMonitor();

  /**
   * The min and max elevation seen on this trip in meters.
   */
  private final ExtremityMonitor elevationExtremities = new ExtremityMonitor();

  /**
   * The minimum and maximum grade calculations on this trip.
   */
  private final ExtremityMonitor gradeExtremities = new ExtremityMonitor();

  /**
   * Default constructor.
   */
  public TripStatistics() {
  }

  /**
   * Copy constructor.
   *
   * @param other another statistics data object to copy from
   */
  public TripStatistics(TripStatistics other) {
    this.maxSpeed = other.maxSpeed;
    this.movingTime = other.movingTime;
    this.startTime = other.startTime;
    this.stopTime = other.stopTime;
    this.totalDistance = other.totalDistance;
    this.totalElevationGain = other.totalElevationGain;
    this.totalTime = other.totalTime;

    this.latitudeExtremities.set(other.latitudeExtremities.getMin(),
                                 other.latitudeExtremities.getMax());
    this.longitudeExtremities.set(other.longitudeExtremities.getMin(),
                                  other.longitudeExtremities.getMax());
    this.elevationExtremities.set(other.elevationExtremities.getMin(),
                                  other.elevationExtremities.getMax());
    this.gradeExtremities.set(other.gradeExtremities.getMin(),
                              other.gradeExtremities.getMax());
  }

  /**
   * Combines these statistics with those from another object.
   * This assumes that the time periods covered by each do not intersect.
   *
   * @param other the other waypoint
   */
  public void merge(TripStatistics other) {
    startTime = Math.min(startTime, other.startTime);
    stopTime = Math.max(stopTime, other.stopTime);
    totalTime += other.totalTime;
    movingTime += other.movingTime;
    totalDistance += other.totalDistance;
    totalElevationGain += other.totalElevationGain;
    maxSpeed = Math.max(maxSpeed, other.maxSpeed);

    latitudeExtremities.update(other.latitudeExtremities.getMax());
    latitudeExtremities.update(other.latitudeExtremities.getMin());
    longitudeExtremities.update(other.longitudeExtremities.getMax());
    longitudeExtremities.update(other.longitudeExtremities.getMin());
    elevationExtremities.update(other.elevationExtremities.getMax());
    elevationExtremities.update(other.elevationExtremities.getMin());
    gradeExtremities.update(other.gradeExtremities.getMax());
    gradeExtremities.update(other.gradeExtremities.getMin());
  }

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
   * Gets the time that this track stopped.
   * 
   * @return The number of milliseconds since epoch to the time when this track
   *         stopped
   */
  public long getStopTime() {
    return stopTime;
  }

  /**
   * Gets the total time that this track has been active.
   * This statistic is only updated when a new point is added to the statistics,
   * so it may be off. If you need to calculate the proper total time, use
   * {@link #getStartTime} with the current time.
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
   * Gets the the average speed the user traveled.
   * This calculation only takes into account the displacement until the last
   * point that was accounted for in statistics.
   *
   * @return The average speed in m/s
   */
  public double getAverageSpeed() {
    return totalDistance / ((double) totalTime / 1000);
  }

  /**
   * Gets the the average speed the user traveled when they were actively
   * moving.
   * 
   * @return The average moving speed in m/s
   */
  public double getAverageMovingSpeed() {
    return totalDistance / ((double) movingTime / 1000);
  }

  /**
   * Gets the the maximum speed for this track.
   * 
   * @return The maximum speed in m/s
   */
  public double getMaxSpeed() {
    return maxSpeed;
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
   * Returns the leftmost position (lowest longitude) of the track, in signed
   * decimal degrees.
   */
  public int getLeft() {
    return (int) (longitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the rightmost position (highest longitude) of the track, in signed
   * decimal degrees.
   */
  public int getRight() {
    return (int) (longitudeExtremities.getMax() * 1E6);
  }

  /**
   * Returns the bottommost position (lowest latitude) of the track, in meters.
   */
  public int getBottom() {
    return (int) (latitudeExtremities.getMin() * 1E6);
  }

  /**
   * Returns the topmost position (highest latitude) of the track, in meters.
   */
  public int getTop() {
    return (int) (latitudeExtremities.getMax() * 1E6);
  }

  /**
   * Gets the minimum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be more than the current elevation.
   * 
   * @return The smallest elevation reading for this trip in meters
   */
  public double getMinElevation() {
    return elevationExtremities.getMin();
  }

  /**
   * Gets the maximum elevation seen on this trip. This is calculated from the
   * smoothed elevation so this can actually be less than the current elevation.
   * 
   * @return The largest elevation reading for this trip in meters
   */
  public double getMaxElevation() {
    return elevationExtremities.getMax();
  }

  /**
   * Gets the maximum grade for this trip.
   * 
   * @return The maximum grade for this trip as a fraction
   */
  public double getMaxGrade() {
    return gradeExtremities.getMax();
  }

  /**
   * Gets the minimum grade for this trip.
   * 
   * @return The minimum grade for this trip as a fraction
   */
  public double getMinGrade() {
    return gradeExtremities.getMin();
  }

  // Setters - to be used when restoring state or loading from the DB

  /**
   * Sets the start time for this trip.
   *
   * @param startTime the start time, in milliseconds since the epoch
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /**
   * Sets the stop time for this trip.
   *
   * @param stopTime the stop time, in milliseconds since the epoch
   */
  public void setStopTime(long stopTime) {
    this.stopTime = stopTime;
  }

  /**
   * Sets the total moving time.
   *
   * @param movingTime the moving time in milliseconds
   */
  public void setMovingTime(long movingTime) {
    this.movingTime = movingTime;
  }

  /**
   * Sets the total trip time.
   *
   * @param totalTime the total trip time in milliseconds
   */
  public void setTotalTime(long totalTime) {
    this.totalTime = totalTime;
  }

  /**
   * Sets the total trip distance.
   *
   * @param totalDistance the trip distance in meters
   */
  public void setTotalDistance(double totalDistance) {
    this.totalDistance = totalDistance;
  }

  /**
   * Sets the total elevation variation during the trip.
   *
   * @param totalElevationGain the elevation variation in meters
   */
  public void setTotalElevationGain(double totalElevationGain) {
    this.totalElevationGain = totalElevationGain;
  }

  /**
   * Sets the maximum speed reached during the trip.
   *
   * @param maxSpeed the maximum speed in meters per second
   */
  public void setMaxSpeed(double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  /**
   * Sets the minimum elevation reached during the trip.
   *
   * @param elevation the minimum elevation in meters
   */
  public void setMinElevation(double elevation) {
    elevationExtremities.setMin(elevation);
  }

  /**
   * Sets the maximum elevation reached during the trip.
   *
   * @param elevation the maximum elevation in meters
   */
  public void setMaxElevation(double elevation) {
    elevationExtremities.setMax(elevation);
  }

  /**
   * Sets the minimum grade obtained during the trip.
   *
   * @param grade the grade as a fraction (-1.0 would mean vertical downwards)
   */
  public void setMinGrade(double grade) {
    gradeExtremities.setMin(grade);
  }

  /**
   * Sets the maximum grade obtained during the trip).
   *
   * @param grade the grade as a fraction (1.0 would mean vertical upwards)
   */
  public void setMaxGrade(double grade) {
    gradeExtremities.setMax(grade);
  }

  /**
   * Sets the bounding box for this trip.
   *
   * @param left the westmost longitude reached
   * @param top the northmost latitude reached
   * @param right the eastmost longitude reached
   * @param bottom the southmost latitude reached
   */
  public void setBounds(int left, int top, int right, int bottom) {
    latitudeExtremities.set(bottom, top);
    longitudeExtremities.set(left, right);
  }

  // Data manipulation methods

  /**
   * Adds to the current total distance.
   *
   * @param distance the distance to add in meters
   */
  void addTotalDistance(double distance) {
    totalDistance += distance;
  }

  /**
   * Adds to the total elevation variation.
   *
   * @param gain the elevation variation in meters
   */
  void addTotalElevationGain(double gain) {
    totalElevationGain += gain;
  }

  /**
   * Adds to the total moving time of the trip.
   *
   * @param time the time in milliseconds
   */
  void addMovingTime(long time) {
    movingTime += time;
  }
  
  /**
   * Accounts for a new latitude value for the bounding box.
   *
   * @param latitude the latitude value in signed decimal degrees
   */
  void updateLatitudeExtremities(double latitude) {
    latitudeExtremities.update(latitude);
  }

  /**
   * Accounts for a new longitude value for the bounding box.
   *
   * @param longitude the longitude value in signed decimal degrees
   */
  void updateLongitudeExtremities(double longitude) {
    longitudeExtremities.update(longitude);
  }

  /**
   * Accounts for a new elevation value for the bounding box.
   *
   * @param elevation the elevation value in meters
   */
  void updateElevationExtremities(double elevation) {
    elevationExtremities.update(elevation);
  }

  /**
   * Accounts for a new grade value.
   *
   * @param grade the grade value as a fraction
   */
  void updateGradeExtremities(double grade) {
    gradeExtremities.update(grade);
  }

  // String conversion

  @Override
  public String toString() {
    return "TripStatistics { Start Time: " + getStartTime()
        + "; Total Time: " + getTotalTime()
        + "; Moving Time: " + getMovingTime()
        + "; Total Distance: " + getTotalDistance()
        + "; Elevation Gain: " + getTotalElevationGain()
        + "; Min Elevation: " + getMinElevation()
        + "; Max Elevation: " + getMaxElevation()
        + "; Average Speed: " + getAverageMovingSpeed()
        + "; Min Grade: " + getMinGrade()
        + "; Max Grade: " + getMaxGrade()
        + "}";
  }

  // Parcelable interface and creator

  /**
   * Creator of statistics data from parcels.
   */
  public static class Creator
      implements Parcelable.Creator<TripStatistics> {

    @Override
    public TripStatistics createFromParcel(Parcel source) {
      TripStatistics data = new TripStatistics();

      data.startTime = source.readLong();
      data.movingTime = source.readLong();
      data.totalTime = source.readLong();
      data.totalDistance = source.readDouble();
      data.totalElevationGain = source.readDouble();
      data.maxSpeed = source.readDouble();

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
    public TripStatistics[] newArray(int size) {
      return new TripStatistics[size];
    }
  }

  /**
   * Creator of {@link TripStatistics} from parcels.
   */
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