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

package de.dennisguse.opentracks.content.data;

import android.location.Location;
import android.net.Uri;

import de.dennisguse.opentracks.stats.TripStatistics;

/**
 * A waypoint.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public final class Waypoint {

    private long id = -1L;
    private String name = "";
    private String description = "";
    private String category = "";
    private String icon = "";
    private long trackId = -1L;
    private double length = 0.0;
    private long duration = 0;
    private long startId = -1L;
    private long stopId = -1L;
    private Location location = null;
    private TripStatistics tripStatistics = null;
    private String photoUrl = "";

    public Waypoint() {
    }

    public Waypoint(String name, String description, String category, String icon, long trackId, double length, long duration, long startId, long stopId, Location location, TripStatistics tripStatistics, String photoUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.trackId = trackId;
        this.length = length;
        this.duration = duration;
        this.startId = startId;
        this.stopId = stopId;
        this.location = location;
        this.tripStatistics = tripStatistics;
        this.photoUrl = photoUrl;
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

    public boolean isTripStatistics() {
        return tripStatistics != null;
    }

    public TripStatistics getTripStatistics() {
        return tripStatistics;
    }

    public void setTripStatistics(TripStatistics tripStatistics) {
        this.tripStatistics = tripStatistics;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Uri getPhotoURI() {
        return Uri.parse(photoUrl);
    }

    public boolean hasPhoto() {
        return photoUrl != null && !"".equals(photoUrl);
    }
}
