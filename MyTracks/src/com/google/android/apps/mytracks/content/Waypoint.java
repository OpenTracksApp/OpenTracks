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

import com.google.android.apps.mytracks.stats.TripStatistics;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A way point. It has a location, meta data such as name, description,
 * category, and icon, plus it can store track statistics for a "sub-track".
 *
 * TODO: hashCode and equals
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public final class Waypoint implements Parcelable {

  /**
   * Creator for a Waypoint object
   */
  public static class Creator implements Parcelable.Creator<Waypoint> {

    public Waypoint createFromParcel(Parcel source) {
      ClassLoader classLoader = getClass().getClassLoader();
      Waypoint waypoint = new Waypoint();
      waypoint.id = source.readLong();
      waypoint.name = source.readString();
      waypoint.description = source.readString();
      waypoint.category = source.readString();
      waypoint.icon = source.readString();
      waypoint.trackId = source.readLong();
      waypoint.type = source.readInt();
      waypoint.startId = source.readLong();
      waypoint.stopId = source.readLong();
      waypoint.stats = source.readParcelable(classLoader);
      byte hasLocation = source.readByte();
      if (hasLocation > 0) {
        waypoint.location = source.readParcelable(classLoader);
      }
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

  /** Start track point id */
  private long startId = -1;
  /** Stop track point id */
  private long stopId = -1;

  private TripStatistics stats = new TripStatistics();

  /** The length of the track, without smoothing. */
  private double length;

  /** The total duration of the track (not from the last waypoint) */
  private long duration;

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(category);
    dest.writeString(icon);
    dest.writeLong(trackId);
    dest.writeInt(type);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeParcelable(stats, 0);
    dest.writeByte(location == null ? (byte) 0 : (byte) 1);
    if (location != null) {
      dest.writeParcelable(location, 0);
    }
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

  public void setLocation(Location location) {
    this.location = location;
  }

  public TripStatistics getStatistics() {
    return stats;
  }

  public void setStatistics(TripStatistics stats) {
    this.stats = stats;
  }

  // WARNING: These fields are used for internal state keeping. You probably
  // want to look at getStatistics instead.

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
}
