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

  private long id = -1;
  private String name = "";
  private String description = "";
  private String category = "";
  private long startId = -1;
  private long stopId = -1;

  // The number of location points (present even if the points themselves are
  // not loaded)
  private int numberOfPoints = 0;
  private String mapId = "";
  private String tableId = "";
  private String icon = "";

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
    mapId = in.readString();
    tableId = in.readString();
    icon = in.readString();

    ClassLoader classLoader = getClass().getClassLoader();
    tripStatistics = in.readParcelable(classLoader);

    for (int i = 0; i < numberOfPoints; ++i) {
      Location loc = in.readParcelable(classLoader);
      locations.add(loc);
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
    dest.writeString(mapId);
    dest.writeString(tableId);
    dest.writeString(icon);
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

  public String getMapId() {
    return mapId;
  }

  public void setMapId(String mapId) {
    this.mapId = mapId;
  }

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public TripStatistics getTripStatistics() {
    return tripStatistics;
  }

  public void setTripStatistics(TripStatistics tripStatistics) {
    this.tripStatistics = tripStatistics;
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
