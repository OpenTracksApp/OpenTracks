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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * NOTE: A marker is indirectly (via it's {@link Position}) assigned to one {@link TrackPoint} via position.time.
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

    //Some data might not be used.
    private final Position position;

    private Uri photoUrl = null;

    public Marker(@Nullable Track.Id trackId, @NonNull TrackPoint trackPoint) {
        this.trackId = trackId;

        if (!trackPoint.hasLocation())
            throw new RuntimeException("Marker requires a trackpoint with a location.");

        this.position = trackPoint.getPosition();
    }

    public Marker(@Nullable Track.Id trackId, @NonNull Position position) {
        this.trackId = trackId;
        Objects.requireNonNull(position);
        this.position = position;
    }

    public Marker(@Nullable Track.Id trackId, @NonNull TrackPoint trackPoint, String name, String description, String category, String icon, Uri photoUrl) {
        this(trackId, trackPoint);
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.photoUrl = photoUrl;
    }

    /**
     * May be null if the Marker was not loaded from the database.
     */
    @Nullable
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Instant getTime() {
        return position.time();
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

    public Position getPosition() {
        return position;
    }

    public boolean hasAccuracy() {
        return position.hasHorizontalAccuracy();
    }

    public Distance getAccuracy() {
        return position.horizontalAccuracy();
    }

    public boolean hasAltitude() {
        return position.hasAltitude();
    }

    public Altitude getAltitude() {
        return position.altitude();
    }

    public boolean hasBearing() {
        return position.hasBearing();
    }

    public Float getBearing() {
        return position.bearing();
    }

    public Uri getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(Uri photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean hasPhoto() {
        return photoUrl != null;
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
