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
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

import de.dennisguse.opentracks.util.PreferencesUtils;

/**
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
    //TODO It is the distance from the track starting point; rename to something more meaningful
    private double length = 0.0;
    private long duration = 0;
    private final Location location;
    @Deprecated //TODO Make an URI instead of String
    private String photoUrl = "";

    public Marker(@Nullable Track.Id trackId) {
        this.trackId = trackId;
        location = null;
    }

    @VisibleForTesting
    public Marker(@NonNull Track.Id trackId, @NonNull TrackPoint trackPoint) {
        this(trackId, trackPoint.getLocation());
    }

    @Deprecated
    //TODO Used by AbstractFileImporter to create an intermediate marker before saving a new one into the database.
    public Marker(@NonNull Location location) {
        this(null, location);
    }

    public Marker(@NonNull Track.Id trackId, @NonNull Location location) {
        this.location = location;
        this.trackId = trackId;
    }

    public Marker(String name, String description, String category, String icon, @NonNull Track.Id trackId, double length, long duration, @NonNull Location location, String photoUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.trackId = trackId;
        this.length = length;
        this.duration = duration;
        this.location = location;
        this.photoUrl = photoUrl;
    }

    /**
     * May be null if the it was not loaded from the database.
     */
    public @Nullable
    Id getId() {
        return id;
    }

    public void setId(Id id) {
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

    public @NonNull
    Track.Id getTrackId() {
        return trackId;
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

    public @NonNull
    Location getLocation() {
        return location;
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

        @Deprecated //TODO Use a Id of null instead
        public boolean isValid() {
            return id != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
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
