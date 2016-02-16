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

import com.google.android.apps.mytracks.stats.TripStatistics;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * A track.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class Track implements Parcelable {

  private long id = -1L;
  private String name = "";
  private String description = "";
  private String category = "";
  private long startId = -1L;
  private long stopId = -1L;

  /*
   * The number of location points (present even if the points themselves are
   * not loaded)
   */
  private int numberOfPoints = 0;
  private String icon = "";
  private String driveId = "";
  private long modifiedTime = -1L;
  private boolean sharedWithMe = false;
  private String sharedOwner = "";

  private TripStatistics tripStatistics = new TripStatistics();

  // Location points (which may not have been loaded)
  private ArrayList<Location> locations = new ArrayList<Location>();

  public Track() {}

  private Track(Parcel in) {
    id = in.readLong();
    name = in.readString();
    description = in.readString();
    category = in.readString();
    startId = in.readLong();
    stopId = in.readLong();
    numberOfPoints = in.readInt();
    icon = in.readString();
    driveId = in.readString();
    modifiedTime = in.readLong();
    sharedWithMe = in.readByte() == 1;
    sharedOwner = in.readString();

    ClassLoader classLoader = getClass().getClassLoader();
    tripStatistics = in.readParcelable(classLoader);

    for (int i = 0; i < numberOfPoints; ++i) {
      Location location = in.readParcelable(classLoader);
      locations.add(location);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(category);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeInt(numberOfPoints);
    dest.writeString(icon);
    dest.writeString(driveId);
    dest.writeLong(modifiedTime);
    dest.writeByte((byte) (sharedWithMe ? 1 : 0));
    dest.writeString(sharedOwner);

    dest.writeParcelable(tripStatistics, 0);
    for (int i = 0; i < numberOfPoints; ++i) {
      dest.writeParcelable(locations.get(i), 0);
    }
  }

  public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
      @Override
    public Track createFromParcel(Parcel in) {
      return new Track(in);
    }

      @Override
    public Track[] newArray(int size) {
      return new Track[size];
    }
  };

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public int getNumberOfPoints() {
    return numberOfPoints;
  }

  public void setNumberOfPoints(int numberOfPoints) {
    this.numberOfPoints = numberOfPoints;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDriveId() {
    return driveId;
  }

  public void setDriveId(String driveId) {
    this.driveId = driveId;
  }

  public long getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(long modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public boolean isSharedWithMe() {
    return sharedWithMe;
  }

  public void setSharedWithMe(boolean sharedWithMe) {
    this.sharedWithMe = sharedWithMe;
  }

  public String getSharedOwner() {
    return sharedOwner;
  }
  
  public void setSharedOwner(String sharedOwner) {
    this.sharedOwner = sharedOwner;
  }
  
  public TripStatistics getTripStatistics() {
    return tripStatistics;
  }

  public void setTripStatistics(TripStatistics tripStatistics) {
    this.tripStatistics = tripStatistics;
  }

  public void addLocation(Location location) {
    locations.add(location);
  }

  public ArrayList<Location> getLocations() {
    return locations;
  }

  public void setLocations(ArrayList<Location> locations) {
    this.locations = locations;
  }
}
