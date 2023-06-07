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

package de.dennisguse.opentracks.data.models;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * NOTE: A marker is indirectly (via it's location) assigned to one {@link TrackPoint} with trackPoint.hasLocation() == true.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
//TODO All data should be final; no default values.
public final class Marker {

    private Id id;
    private String name = "";
    private String description = "";
    private String category = "";
    private String icon = "";
    private Track.Id trackId;

    private final Instant time;
    private Double latitude;
    private Double longitude;
    @Deprecated //Not needed
    private Distance accuracy;
    private Altitude altitude;
    private Float bearing;

    //TODO It is the distance from the track starting point; rename to something more meaningful
    private Distance length;
    private Duration duration;

    @Deprecated //TODO Make an URI instead of String
    private String photoUrl = "";

    public Marker(@Nullable Track.Id trackId, Instant time) {
        this.trackId = trackId;
        this.time = time;
    }

    @Deprecated //TODO Marker cannot be created without length AND duration!
    public Marker(@Nullable Track.Id trackId, @NonNull TrackPoint trackPoint) {
        this.trackId = trackId;

        this.time = trackPoint.getTime();

        if (!trackPoint.hasLocation())
            throw new RuntimeException("Marker requires a trackpoint with a location.");

        setTrackPoint(trackPoint);

        this.length = Distance.of(0); //TODO Not cool!
        this.duration = Duration.ofMillis(0); //TODO Not cool!
    }

    @Deprecated
    public Marker(String name, String description, String category, String icon, @NonNull Track.Id trackId, @NonNull TrackStatistics statistics, @NonNull TrackPoint trackPoint, String photoUrl) {
        this(trackId, trackPoint);
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.length = statistics.getTotalDistance();
        this.duration = statistics.getTotalTime();
        this.photoUrl = photoUrl;
    }

    //TODO Is somehow part of the initialization process. Can we at least limit visibility?
    public void setTrackPoint(TrackPoint trackPoint) {
        this.latitude = trackPoint.getLatitude();
        this.longitude = trackPoint.getLongitude();
        if (trackPoint.hasHorizontalAccuracy()) this.accuracy = trackPoint.getHorizontalAccuracy();
        if (trackPoint.hasAltitude()) this.altitude = trackPoint.getAltitude();
        if (trackPoint.hasBearing()) this.bearing = trackPoint.getBearing();
    }

    /**
     * May be null if the it was not loaded from the database.
     */
    @Nullable
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Instant getTime() {
        return time;
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

    public Track.Id getTrackId() {
        return trackId;
    }

    @Deprecated
    public void setTrackId(@NonNull Track.Id trackId) {
        this.trackId = trackId;
    }

    public boolean hasLocation() {
        return latitude != null || longitude != null;
    }

    public Location getLocation() {
        Location location = new Location("");
        location.setTime(time.toEpochMilli());
        if (hasLocation()) {
            location.setLatitude(latitude);
            location.setLongitude(longitude);
        }
        if (hasBearing()) {
            location.setBearing(bearing);
        }
        if (hasAccuracy()) {
            location.setAccuracy((float) accuracy.toM());
        }
        if (hasAltitude()) {
            location.setAltitude(altitude.toM());
        }

        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasAccuracy() {
        return accuracy != null;
    }

    public Distance getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Distance accuracy) {
        this.accuracy = accuracy;
    }

    public boolean hasAltitude() {
        return altitude != null;
    }

    public Altitude getAltitude() {
        return altitude;
    }

    public void setAltitude(Altitude altitude) {
        this.altitude = altitude;
    }

    public boolean hasBearing() {
        return bearing != null;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public Distance getLength() {
        return length;
    }

    public void setLength(Distance length) {
        this.length = length;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(@NonNull Duration duration) {
        this.duration = duration;
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

    public record Id(long id) implements Parcelable {

        @NonNull
        @Override
        public String toString() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(id);
        }

        public static final Creator<Id> CREATOR = new Creator<>() {
            public Id createFromParcel(Parcel in) {
                return new Id(in.readLong());
            }

            public Id[] newArray(int size) {
                return new Id[size];
            }
        };
    }
}
