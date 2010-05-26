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
package com.google.android.apps.mytracks.content;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A way point. It has a location, meta data such as name, description,
 * category, and icon, plus it can store track statistics for a "sub-track".
 *
 * @author Leif Hendrik Wilden
 */
public final class Waypoint implements Parcelable {

  /**
   *  Creator for a Waypoint object
   */
  public static class Creator implements Parcelable.Creator<Waypoint> {

    public Waypoint createFromParcel(Parcel source) {
      Waypoint waypoint = new Waypoint();
      waypoint.id = source.readLong();
      waypoint.name = source.readString();
      waypoint.description = source.readString();
      waypoint.category = source.readString();
      waypoint.icon = source.readString();
      waypoint.trackId = source.readLong();
      waypoint.type = source.readInt();
      waypoint.length = source.readDouble();
      waypoint.duration = source.readLong();
      waypoint.startTime = source.readLong();
      waypoint.startId = source.readLong();
      waypoint.stopId = source.readLong();
      byte hasLocation = source.readByte();
      if (hasLocation > 0) {
        waypoint.location = source.readParcelable(null);
      }
      waypoint.totalDistance = source.readDouble();
      waypoint.totalTime = source.readLong();
      waypoint.movingTime = source.readLong();
      waypoint.averageSpeed = source.readDouble();
      waypoint.averageMovingSpeed = source.readDouble();
      waypoint.maxSpeed = source.readDouble();
      waypoint.minElevation = source.readDouble();
      waypoint.maxElevation = source.readDouble();
      waypoint.totalElevationGain = source.readDouble();
      waypoint.minGrade = source.readDouble();
      waypoint.maxGrade = source.readDouble();
      return waypoint;
    }

    public Waypoint[] newArray(int size) {
      return new Waypoint[size];
    }
  }

  public static final Creator CREATOR = new Creator();

  public static final int TYPE_WAYPOINT = 0;
  public static final int TYPE_STATISTICS = 1;

  private long id = -1;
  private String name = "";
  private String description = "";
  private String category = "";
  private String icon = "";
  private long trackId = -1;
  private int type = 0;

  private Location location;

  /** Length along the track in meters. */
  private double length = 0;
  /** Time along the track in seconds. */
  private long duration = 0;
  /** Start time of a segment */
  private long startTime = -1;
  /** Start track point id */
  private long startId = -1;
  /** Stop track point id */
  private long stopId = -1;

  private double totalDistance = 0;
  private long totalTime = 0;
  private long movingTime = 0;
  private double averageSpeed = 0;
  private double averageMovingSpeed = 0;
  private double maxSpeed = 0;
  private double minElevation = 0;
  private double maxElevation = 0;
  private double totalElevationGain = 0;
  private double minGrade = 0;
  private double maxGrade = 0;

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(category);
    dest.writeString(icon);
    dest.writeLong(trackId);
    dest.writeInt(type);
    dest.writeDouble(length);
    dest.writeLong(duration);
    dest.writeLong(startTime);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeByte(location == null ? (byte) 0 : (byte) 1);
    if (location != null) {
      dest.writeParcelable(location, 0);
    }
    dest.writeDouble(totalDistance);
    dest.writeLong(totalTime);
    dest.writeLong(movingTime);
    dest.writeDouble(averageSpeed);
    dest.writeDouble(averageMovingSpeed);
    dest.writeDouble(maxSpeed);
    dest.writeDouble(minElevation);
    dest.writeDouble(maxElevation);
    dest.writeDouble(totalElevationGain);
    dest.writeDouble(minGrade);
    dest.writeDouble(maxGrade);
  }

  /**
   * Combines the statistics from another (earlier) waypoint with this one.
   * This is needed when the user deletes a statistic waypoint.
   *
   * @param other the other waypoint
   */
  public void combine(Waypoint other) {
    totalTime += other.totalTime;
    movingTime += other.movingTime;
    totalDistance += other.totalDistance;
    totalElevationGain += other.totalElevationGain;
    averageSpeed = totalDistance / totalTime;
    averageMovingSpeed = totalDistance / movingTime;
    maxSpeed = Math.max(maxSpeed, other.maxSpeed);
    minElevation = Math.min(minElevation, other.minElevation);
    maxElevation = Math.max(maxElevation, other.maxElevation);
    minGrade = Math.min(minGrade, other.minGrade);
    maxGrade = Math.max(maxGrade, other.maxGrade);
  }

  // Getters and setters:
  //---------------------

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Location getLocation() {
    return location;
  }

  public void setTrackId(long trackId) {
    this.trackId = trackId;
  }

  public int describeContents() {
    return 0;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public double getTotalDistance() {
    return totalDistance;
  }

  public void setTotalDistance(double totalDistance) {
    this.totalDistance = totalDistance;
  }

  public long getTotalTime() {
    return totalTime;
  }

  public void setTotalTime(long totalTime) {
    this.totalTime = totalTime;
  }

  public long getMovingTime() {
    return movingTime;
  }

  public void setMovingTime(long movingTime) {
    this.movingTime = movingTime;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getTrackId() {
    return trackId;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public double getLength() {
    return length;
  }

  public void setLength(double length) {
    this.length = length;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStartId() {
    return startId;
  }

  public void setStartId(long startId) {
    this.startId = startId;
  }

  public long getStopId() {
    return stopId;
  }

  public void setStopId(long stopId) {
    this.stopId = stopId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public double getAverageSpeed() {
    return averageSpeed;
  }

  public void setAverageSpeed(double averageSpeed) {
    this.averageSpeed = averageSpeed;
  }

  public double getAverageMovingSpeed() {
    return averageMovingSpeed;
  }

  public void setAverageMovingSpeed(double averageMovingSpeed) {
    this.averageMovingSpeed = averageMovingSpeed;
  }

  public double getMaxSpeed() {
    return maxSpeed;
  }

  public void setMaxSpeed(double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  public double getMinElevation() {
    return minElevation;
  }

  public void setMinElevation(double minElevation) {
    this.minElevation = minElevation;
  }

  public double getMaxElevation() {
    return maxElevation;
  }

  public void setMaxElevation(double maxElevation) {
    this.maxElevation = maxElevation;
  }

  public double getTotalElevationGain() {
    return totalElevationGain;
  }

  public void setTotalElevationGain(double totalElevationGain) {
    this.totalElevationGain = totalElevationGain;
  }

  public double getMinGrade() {
    return minGrade;
  }

  public void setMinGrade(double minGrade) {
    this.minGrade = minGrade;
  }

  public double getMaxGrade() {
    return maxGrade;
  }

  public void setMaxGrade(double maxGrade) {
    this.maxGrade = maxGrade;
  }

  public void setLocation(Location location) {
    this.location = location;
  }
}
