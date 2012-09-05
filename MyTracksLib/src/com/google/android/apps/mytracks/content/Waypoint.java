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
 * A waypoint.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public final class Waypoint implements Parcelable {

  public static final int TYPE_WAYPOINT = 0;
  public static final int TYPE_STATISTICS = 1;

  private long id = -1L;
  private String name = "";
  private String description = "";
  private String category = "";
  private String icon = "";
  private long trackId = -1L;
  private int type = 0;
  private double length = 0.0;
  private long duration = 0;
  private long startId = -1L;
  private long stopId = -1L;
  private Location location = null;
  private TripStatistics tripStatistics = null;

  public Waypoint() {}
  
  public Waypoint(String name, String description, String category, String icon, long trackId,
      int type, double length, long duration, long startId, long stopId, Location location,
      TripStatistics tripStatistics) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.icon = icon;
    this.trackId = trackId;
    this.type = type;
    this.length = length;
    this.duration = duration;
    this.startId = startId;
    this.stopId = stopId;
    this.location = location;
    this.tripStatistics = tripStatistics;
  }

  private Waypoint(Parcel source) {
    id = source.readLong();
    name = source.readString();
    description = source.readString();
    category = source.readString();
    icon = source.readString();
    trackId = source.readLong();
    type = source.readInt();
    length = source.readDouble();
    duration = source.readLong();
    startId = source.readLong();
    stopId = source.readLong();
 
    ClassLoader classLoader = getClass().getClassLoader();
    byte hasLocation = source.readByte();
    if (hasLocation > 0) {
      location = source.readParcelable(classLoader);
    }
    byte hasStats = source.readByte();
    if (hasStats > 0) {
      tripStatistics = source.readParcelable(classLoader);
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
    dest.writeString(icon);
    dest.writeLong(trackId);
    dest.writeInt(type);
    dest.writeDouble(length);
    dest.writeLong(duration);
    dest.writeLong(startId);
    dest.writeLong(stopId);
    dest.writeByte(location == null ? (byte) 0 : (byte) 1);
    if (location != null) {
      dest.writeParcelable(location, 0);
    }
    dest.writeByte(tripStatistics == null ? (byte) 0 : (byte) 1);
    if (tripStatistics != null) {
      dest.writeParcelable(tripStatistics, 0);
    }
  }

  public static final Parcelable.Creator<Waypoint> CREATOR = new Parcelable.Creator<Waypoint>() {
      @Override
    public Waypoint createFromParcel(Parcel in) {
      return new Waypoint(in);
    }

      @Override
    public Waypoint[] newArray(int size) {
      return new Waypoint[size];
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

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public long getTrackId() {
    return trackId;
  }

  public void setTrackId(long trackId) {
    this.trackId = trackId;
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

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public TripStatistics getTripStatistics() {
    return tripStatistics;
  }

  public void setTripStatistics(TripStatistics tripStatistics) {
    this.tripStatistics = tripStatistics;
  }
}
