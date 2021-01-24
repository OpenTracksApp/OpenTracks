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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * NOTE: A marker is indirectly (via it's location) assigned to one {@link TrackPoint} with trackPoint.hasLocation() == true.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public final class Marker {

    private Id id;
    private String name = "";
    private String description = "";
    private String category = "";
    private String icon = "";
    private final Track.Id trackId;

    private final Instant time;
    private Double latitude;
    private Double longitude;
    private Float accuracy;
    private Double altitude_m;
    private Float bearing;

    //TODO It is the distance from the track starting point; rename to something more meaningful
    private double length = 0.0;
    private long duration = 0; //TODO Duration

    @Deprecated //TODO Make an URI instead of String
    private String photoUrl = "";

    public Marker(@Nullable Track.Id trackId, Instant time) {
        this.trackId = trackId;
        this.time = time;
    }

    public Marker(@Nullable Track.Id trackId, @NonNull TrackPoint trackPoint) {
        this.trackId = trackId;

        this.time = trackPoint.getTime();

        if (!trackPoint.hasLocation())
            throw new RuntimeException("Marker requires a trackpoint with a location.");

        this.latitude = trackPoint.getLatitude();
        this.longitude = trackPoint.getLongitude();
        if (trackPoint.hasAccuracy()) this.accuracy = trackPoint.getAccuracy();
        if (trackPoint.hasAltitude()) this.altitude_m = trackPoint.getAltitude();
        if (trackPoint.hasBearing()) this.bearing = trackPoint.getBearing();
    }

    public Marker(String name, String description, String category, String icon, @NonNull Track.Id trackId, double length, long duration, @NonNull TrackPoint trackPoint, String photoUrl) {
        this(trackId, trackPoint);
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.length = length;
        this.duration = duration;
        this.photoUrl = photoUrl;
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

    @NonNull
    public Track.Id getTrackId() {
        return trackId;
    }

    public boolean hasLocation() {
        return latitude != null || longitude != null;
    }

    @Nullable
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
            location.setAccuracy(accuracy);
        }
        if (hasAltitude()) {
            location.setAltitude(altitude_m);
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

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public boolean hasAltitude() {
        return altitude_m != null;
    }

    public Double getAltitude() {
        return altitude_m;
    }

    public void setAltitude(double altitude_m) {
        this.altitude_m = altitude_m;
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

    public static class Id implements Parcelable {

        private final long id;

        public Id(long id) {
            this.id = id;
        }

        //TOOD Limit visibility to TrackRecordingService / ContentProvider
        public long getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id id1 = (Id) o;
            return id == id1.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

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

        public static final Parcelable.Creator<Id> CREATOR = new Parcelable.Creator<Id>() {
            public Id createFromParcel(Parcel in) {
                return new Id(in.readLong());
            }

            public Id[] newArray(int size) {
                return new Id[size];
            }
        };
    }
}
