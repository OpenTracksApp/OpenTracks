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
 * A class representing a (GPS) Track.
 *
 * TODO: hashCode and equals
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class Track implements Parcelable {

  /**
   * Creator for a Track object
   */
  public static class Creator implements Parcelable.Creator<Track> {

    public Track createFromParcel(Parcel source) {
      ClassLoader classLoader = getClass().getClassLoader();
      Track track = new Track();
      track.id = source.readLong();
      track.name = source.readString();
      track.description = source.readString();
      track.mapId = source.readString();
      track.category = source.readString();
      track.startId = source.readLong();
      track.stopId = source.readLong();
      track.stats = source.readParcelable(classLoader);

      track.numberOfPoints = source.readInt();
      for (int i = 0; i < track.numberOfPoints; ++i) {
        Location loc = source.readParcelable(classLoader);
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
   * The track points (which may not have been loaded).
   */
  private ArrayList<Location> locations = new ArrayList<Location>();

  /**
   * The number of location points (present even if the points themselves were
   * not loaded).
   */
  private int numberOfPoints = 0;

  private long id = -1;
  private String name = "";
  private String description = "";
  private String mapId = "";
  private long startId = -1;
  private long stopId = -1;
  private String category = "";

  private TripStatistics stats = new TripStatistics();

  public Track() {
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(mapId);
    dest.writeString(category);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeParcelable(stats, 0);

    dest.writeInt(numberOfPoints);
    for (int i = 0; i < numberOfPoints; ++i) {
      dest.writeParcelable(locations.get(i), 0);
    }
  }

  // Getters and setters:
  //---------------------

  public int describeContents() {
    return 0;
  }

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

  public TripStatistics getStatistics() {
    return stats;
  }

  public void setStatistics(TripStatistics stats) {
    this.stats = stats;
  }
}
