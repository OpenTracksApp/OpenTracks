/*
 * Copyright 2008 Google Inc.
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

import java.util.ArrayList;

/**
 * A class representing a (GPS) Track.
 *
 * @author Leif Hendrik Wilden
 */
public class Track implements Parcelable {

  /**
   * Creator for a Track object
   */
  public static class Creator implements Parcelable.Creator<Track> {

    public Track createFromParcel(Parcel source) {
      Track track = new Track();
      track.id = source.readLong();
      track.name = source.readString();
      track.description = source.readString();
      track.mapId = source.readString();
      track.category = source.readString();
      track.startId = source.readLong();
      track.stopId = source.readLong();
      track.startTime = source.readLong();
      track.stopTime = source.readLong();
      track.totalDistance = source.readDouble();
      track.totalTime = source.readLong();
      track.movingTime = source.readLong();
      track.averageSpeed = source.readDouble();
      track.averageMovingSpeed = source.readDouble();
      track.maxSpeed = source.readDouble();
      track.minElevation = source.readDouble();
      track.maxElevation = source.readDouble();
      track.totalElevationGain = source.readDouble();
      track.minGrade = source.readDouble();
      track.maxGrade = source.readDouble();
      track.left = source.readInt();
      track.top = source.readInt();
      track.right = source.readInt();
      track.bottom = source.readInt();
      track.numberOfPoints = source.readInt();
      for (int i = 0; i < track.numberOfPoints; ++i) {
        Location loc = source.readParcelable(null);
        track.locations.add(loc);
      }
      return track;
    }

    public Track[] newArray(int size) {
      return new Track[size];
    }
  }

  public static final Creator CREATOR = new Creator();

  /**
   * The track points.
   */
  private ArrayList<Location> locations = new ArrayList<Location>();

  private long id = -1;
  private String name = "";
  private String description = "";
  private String mapId = "";
  private long startId = -1;
  private long stopId = -1;
  private String category = "";

  // Fields derived from locations. Use setDerivedFields() to set them:
  //-------------------------------------------------------------------

  private long startTime = -1;
  private long stopTime = -1;
  private int numberOfPoints = 0;
  private double totalDistance = 0;
  private long totalTime = 0;
  private long movingTime = 0;
  private int left;
  private int top;
  private int right;
  private int bottom;

  private double averageSpeed = 0;
  private double averageMovingSpeed = 0;
  private double maxSpeed = 0;
  private double minElevation = 0;
  private double maxElevation = 0;
  private double totalElevationGain = 0;
  private double minGrade = 0;
  private double maxGrade = 0;

  public Track() {
    left = Integer.MAX_VALUE;
    top = Integer.MIN_VALUE;
    right = Integer.MIN_VALUE;
    bottom = Integer.MAX_VALUE;
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(mapId);
    dest.writeString(category);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeLong(startTime);
    dest.writeLong(stopTime);
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
    dest.writeInt(left);
    dest.writeInt(top);
    dest.writeInt(right);
    dest.writeInt(bottom);
    dest.writeInt(numberOfPoints);
    for (int i = 0; i < numberOfPoints; ++i) {
      dest.writeParcelable(locations.get(i), 0);
    }
  }

  // Getters and setters:
  //---------------------

  public int getLeft() {
    return left;
  }

  public int getTop() {
    return top;
  }

  public int getRight() {
    return right;
  }

  public int getBottom() {
    return bottom;
  }

  public void setBounds(int aLeft, int aTop, int aRight, int aBottom) {
    left = aLeft;
    top = aTop;
    right = aRight;
    bottom = aBottom;
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

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStopTime() {
    return stopTime;
  }

  public void setStopTime(long stopTime) {
    this.stopTime = stopTime;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getMapId() {
    return mapId;
  }

  public void setMapId(String mapId) {
    this.mapId = mapId;
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

  public int getNumberOfPoints() {
    return numberOfPoints;
  }

  public void setNumberOfPoints(int numberOfPoints) {
    this.numberOfPoints = numberOfPoints;
  }

  public void addLocation(Location l) {
    locations.add(l);
  }

  public ArrayList<Location> getLocations() {
    return locations;
  }

  public void setLocations(ArrayList<Location> locations) {
    this.locations = locations;
  }
}
