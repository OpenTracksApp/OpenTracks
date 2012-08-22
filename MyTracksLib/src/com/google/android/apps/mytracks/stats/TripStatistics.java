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
 * Statistical data about a trip. The data in this class should be filled out by
 * TripStatisticsBuilder.
 * <p>
 * TODO: hashCode and equals
 * 
 * @author Rodrigo Damazio
 */
public class TripStatistics implements Parcelable {

  // The trip start time. This is the system time, might not match the GPs time.
  private long startTime = -1L;

  // The trip stop time. This is the system time, might not match the GPS time.
  private long stopTime = -1L;

  // The total trip distance (meters).
  private double totalDistance;

  // The total time (ms). Updated when new points are received, may be stale.
  private long totalTime;

  // The total moving time (ms). Based on when we believe the user is traveling.
  private long movingTime;

  // The min and max latitude seen in this trip.
  private final ExtremityMonitor latitudeExtremities = new ExtremityMonitor();

  // The min and max longitude seen in this trip.
  private final ExtremityMonitor longitudeExtremities = new ExtremityMonitor();

  // The maximum speed (meters/second) that we believe is valid.
  private double maxSpeed;

  // The min and max elevation (meters) seen on this trip.
  private final ExtremityMonitor elevationExtremities = new ExtremityMonitor();

  // The total elevation gained (meters).
  private double totalElevationGain;

  // The min and max grade seen on this trip.
  private final ExtremityMonitor gradeExtremities = new ExtremityMonitor();

  /**
   * Default constructor.
   */
  public TripStatistics() {}

  /**
   * Copy constructor.
   * 
   * @param other another statistics data object to copy from
   */
  public TripStatistics(TripStatistics other) {
    this.startTime = other.startTime;
    this.stopTime = other.stopTime;
    this.totalDistance = other.totalDistance;
    this.totalTime = other.totalTime;
    this.movingTime = other.movingTime;
    this.latitudeExtremities.set(
        other.latitudeExtremities.getMin(), other.latitudeExtremities.getMax());
    this.longitudeExtremities.set(
        other.longitudeExtremities.getMin(), other.longitudeExtremities.getMax());
    this.maxSpeed = other.maxSpeed;
    this.elevationExtremities.set(
        other.elevationExtremities.getMin(), other.elevationExtremities.getMax());
    this.totalElevationGain = other.totalElevationGain;
    this.gradeExtremities.set(other.gradeExtremities.getMin(), other.gradeExtremities.getMax());
  }

  /**
   * Combines these statistics with those from another object. This assumes that
   * the time periods covered by each do not intersect.
   * 
   * @param other another statistics data object
   */
  public void merge(TripStatistics other) {
    startTime = Math.min(startTime, other.startTime);
    stopTime = Math.max(stopTime, other.stopTime);
    totalDistance += other.totalDistance;
    totalTime += other.totalTime;
    movingTime += other.movingTime;
    if (other.latitudeExtremities.hasData()) {
      latitudeExtremities.update(other.latitudeExtremities.getMin());
      latitudeExtremities.update(other.latitudeExtremities.getMax());
    }
    if (other.longitudeExtremities.hasData()) {
      longitudeExtremities.update(other.longitudeExtremities.getMin());
      longitudeExtremities.update(other.longitudeExtremities.getMax());
    }
    maxSpeed = Math.max(maxSpeed, other.maxSpeed);
    if (other.elevationExtremities.hasData()) {
      elevationExtremities.update(other.elevationExtremities.getMin());
      elevationExtremities.update(other.elevationExtremities.getMax());
    }
    totalElevationGain += other.totalElevationGain;
    if (other.gradeExtremities.hasData()) {
      gradeExtremities.update(other.gradeExtremities.getMin());
      gradeExtremities.update(other.gradeExtremities.getMax());
    }
  }

  /**
   * Gets the trip start time. The number of milliseconds since epoch.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Gets the trip stop time. The number of milliseconds since epoch.
   */
  public long getStopTime() {
    return stopTime;
  }

  /**
   * Gets the total distance the user traveled in meters.
   */
  public double getTotalDistance() {
    return totalDistance;
  }

  /**
   * Gets the total time in milliseconds that this track has been active. This
   * statistic is only updated when a new point is added to the statistics, so
   * it may be off. If you need to calculate the proper total time, use
   * {@link #getStartTime} with the current time.
   */
  public long getTotalTime() {
    return totalTime;
  }

  /**
   * Gets the moving time in milliseconds.
   */
  public long getMovingTime() {
    return movingTime;
  }

  /**
   * Gets the topmost position (highest latitude) of the track, in signed
   * degrees.
   */
  public double getTopDegrees() {
    return latitudeExtremities.getMax();
  }

  /**
   * Gets the topmost position (highest latitude) of the track, in signed
   * millions of degrees.
   */
  public int getTop() {
    return (int) (latitudeExtremities.getMax() * 1E6);
  }

  /**
   * Gets the bottommost position (lowest latitude) of the track, in signed
   * degrees.
   */
  public double getBottomDegrees() {
    return latitudeExtremities.getMin();
  }

  /**
   * Gets the bottommost position (lowest latitude) of the track, in signed
   * millions of degrees.
   */
  public int getBottom() {
    return (int) (latitudeExtremities.getMin() * 1E6);
  }

  /**
   * Gets the leftmost position (lowest longitude) of the track, in signed
   * degrees.
   */
  public double getLeftDegrees() {
    return longitudeExtremities.getMin();
  }

  /**
   * Gets the leftmost position (lowest longitude) of the track, in signed
   * millions of degrees.
   */
  public int getLeft() {
    return (int) (longitudeExtremities.getMin() * 1E6);
  }

  /**
   * Gets the rightmost position (highest longitude) of the track, in signed
   * degrees.
   */
  public double getRightDegrees() {
    return longitudeExtremities.getMax();
  }

  /**
   * Gets the rightmost position (highest longitude) of the track, in signed
   * millions of degrees.
   */
  public int getRight() {
    return (int) (longitudeExtremities.getMax() * 1E6);
  }

  /**
   * Gets the mean latitude position of the track, in signed degrees.
   */
  public double getMeanLatitude() {
    return (getBottomDegrees() + getTopDegrees()) / 2.0;
  }

  /**
   * Gets the mean longitude position of the track, in signed degrees.
   */
  public double getMeanLongitude() {
    return (getLeftDegrees() + getRightDegrees()) / 2.0;
  }

  /**
   * Gets the average speed in meters/second. This calculation only takes into
   * account the displacement until the last point that was accounted for in
   * statistics.
   */
  public double getAverageSpeed() {
    if (totalTime == 0L) {
      return 0.0;
    }
    return totalDistance / ((double) totalTime / 1000.0);
  }

  /**
   * Gets the average moving speed in meters/second.
   */
  public double getAverageMovingSpeed() {
    if (movingTime == 0L) {
      return 0.0;
    }
    return totalDistance / ((double) movingTime / 1000.0);
  }

  /**
   * Gets the maximum speed in meters/second.
   */
  public double getMaxSpeed() {
    return maxSpeed;
  }

  /**
   * Gets the minimum elevation. This is calculated from the smoothed elevation
   * so this can actually be more than the current elevation.
   */
  public double getMinElevation() {
    return elevationExtremities.getMin();
  }

  /**
   * Gets the maximum elevation. This is calculated from the smoothed elevation
   * so this can actually be less than the current elevation.
   */
  public double getMaxElevation() {
    return elevationExtremities.getMax();
  }

  /**
   * Gets the total elevation gain in meters. This is calculated as the sum of
   * all positive differences in the smoothed elevation.
   */
  public double getTotalElevationGain() {
    return totalElevationGain;
  }

  /**
   * Gets the minimum grade for this trip.
   */
  public double getMinGrade() {
    return gradeExtremities.getMin();
  }

  /**
   * Gets the maximum grade for this trip.
   */
  public double getMaxGrade() {
    return gradeExtremities.getMax();
  }

  /**
   * Sets the trip start time.
   * 
   * @param startTime the trip start time in milliseconds since the epoch
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /**
   * Sets the trip stop time.
   * 
   * @param stopTime the stop time in milliseconds since the epoch
   */
  public void setStopTime(long stopTime) {
    this.stopTime = stopTime;
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
   * Adds to the current total distance.
   * 
   * @param distance the distance to add in meters
   */
  void addTotalDistance(double distance) {
    totalDistance += distance;
  }

  /**
   * Sets the trip total time.
   * 
   * @param totalTime the trip total time in milliseconds
   */
  public void setTotalTime(long totalTime) {
    this.totalTime = totalTime;
  }

  /**
   * Sets the trip total moving time.
   * 
   * @param movingTime the trip total moving time in milliseconds
   */
  public void setMovingTime(long movingTime) {
    this.movingTime = movingTime;
  }

  /**
   * Adds to the trip total moving time.
   * 
   * @param time the time in milliseconds
   */
  void addMovingTime(long time) {
    movingTime += time;
  }

  /**
   * Sets the bounding box for this trip. The unit for all parameters is signed
   * millions of degree (degrees * 1E6).
   * 
   * @param leftE6 the leftmost longitude reached
   * @param topE6 the topmost latitude reached
   * @param rightE6 the rightmost longitude reached
   * @param bottomE6 the bottommost latitude reached
   */
  public void setBounds(int leftE6, int topE6, int rightE6, int bottomE6) {
    latitudeExtremities.set(bottomE6 / 1E6, topE6 / 1E6);
    longitudeExtremities.set(leftE6 / 1E6, rightE6 / 1E6);
  }

  /**
   * Updates a new latitude value.
   * 
   * @param latitude the latitude value in signed decimal degrees
   */
  void updateLatitudeExtremities(double latitude) {
    latitudeExtremities.update(latitude);
  }

  /**
   * Updates a new longitude value.
   * 
   * @param longitude the longitude value in signed decimal degrees
   */
  void updateLongitudeExtremities(double longitude) {
    longitudeExtremities.update(longitude);
  }

  /**
   * Sets the maximum speed.
   * 
   * @param maxSpeed the maximum speed in meters/second
   */
  public void setMaxSpeed(double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  /**
   * Sets the minimum elevation.
   * 
   * @param elevation the minimum elevation in meters
   */
  public void setMinElevation(double elevation) {
    elevationExtremities.setMin(elevation);
  }

  /**
   * Sets the maximum elevation.
   * 
   * @param elevation the maximum elevation in meters
   */
  public void setMaxElevation(double elevation) {
    elevationExtremities.setMax(elevation);
  }

  /**
   * Updates a new elevation.
   * 
   * @param elevation the elevation value in meters
   */
  void updateElevationExtremities(double elevation) {
    elevationExtremities.update(elevation);
  }

  /**
   * Sets the total elevation gain.
   * 
   * @param totalElevationGain the elevation gain in meters
   */
  public void setTotalElevationGain(double totalElevationGain) {
    this.totalElevationGain = totalElevationGain;
  }

  /**
   * Adds to the total elevation gain.
   * 
   * @param gain the elevation gain in meters
   */
  void addTotalElevationGain(double gain) {
    totalElevationGain += gain;
  }

  /**
   * Sets the minimum grade.
   * 
   * @param grade the grade as a fraction (-1.0 would mean vertical downwards)
   */
  public void setMinGrade(double grade) {
    gradeExtremities.setMin(grade);
  }

  /**
   * Sets the maximum grade.
   * 
   * @param grade the grade as a fraction (1.0 would mean vertical upwards)
   */
  public void setMaxGrade(double grade) {
    gradeExtremities.setMax(grade);
  }

  /**
   * Updates a new grade value.
   * 
   * @param grade the grade value as a fraction
   */
  void updateGradeExtremities(double grade) {
    gradeExtremities.update(grade);
  }

  @Override
  public String toString() {
    return "TripStatistics { Start Time: " + getStartTime() + "; Stop Time: " + getStopTime()
        + "; Total Time: " + getTotalTime() + "; Total Distance: " + getTotalDistance()
        + "; Total Time: " + getTotalTime() + "; Moving Time: " + getMovingTime()
        + "; Min Latitude: " + getBottomDegrees() + "; Max Latitude: " + getTopDegrees()
        + "; Min Longitude: " + getLeftDegrees() + "; Max Longitude: " + getRightDegrees()
        + "; Max Elevation: " + getMaxElevation() + "; Max Speed: " + getMaxSpeed()
        + "; Min Elevation: " + getMinElevation() + "; Max Elevation: " + getMaxElevation()
        + "; Elevation Gain: " + getTotalElevationGain() + "; Min Grade: " + getMinGrade()
        + "; Max Grade: " + getMaxGrade() + "}";
  }

  /**
   * Creator of statistics data from parcels.
   */
  public static class Creator implements Parcelable.Creator<TripStatistics> {

    @Override
    public TripStatistics createFromParcel(Parcel source) {
      TripStatistics data = new TripStatistics();

      data.startTime = source.readLong();
      data.stopTime = source.readLong();
      data.totalDistance = source.readDouble();
      data.totalTime = source.readLong();
      data.movingTime = source.readLong();

      double minLat = source.readDouble();
      double maxLat = source.readDouble();
      data.latitudeExtremities.set(minLat, maxLat);

      double minLong = source.readDouble();
      double maxLong = source.readDouble();
      data.longitudeExtremities.set(minLong, maxLong);

      data.maxSpeed = source.readDouble();

      double minElev = source.readDouble();
      double maxElev = source.readDouble();
      data.elevationExtremities.set(minElev, maxElev);
      data.totalElevationGain = source.readDouble();

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
    dest.writeLong(stopTime);
    dest.writeDouble(totalDistance);
    dest.writeLong(totalTime);
    dest.writeLong(movingTime);
    dest.writeDouble(latitudeExtremities.getMin());
    dest.writeDouble(latitudeExtremities.getMax());
    dest.writeDouble(longitudeExtremities.getMin());
    dest.writeDouble(longitudeExtremities.getMax());
    dest.writeDouble(maxSpeed);
    dest.writeDouble(elevationExtremities.getMin());
    dest.writeDouble(elevationExtremities.getMax());
    dest.writeDouble(totalElevationGain);
    dest.writeDouble(gradeExtremities.getMin());
    dest.writeDouble(gradeExtremities.getMax());
  }
}