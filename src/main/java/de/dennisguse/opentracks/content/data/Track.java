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

package de.dennisguse.opentracks.content.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * A track.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class Track {

    private Track.Id id;
    private UUID uuid = UUID.randomUUID();

    private String name = "";
    private String description = "";
    private String category = "";

    private String icon = "";

    private TrackStatistics trackStatistics = new TrackStatistics();

    public Track() {
    }

    /**
     * May be null if the track was not loaded from the database.
     */
    @Nullable
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public TrackStatistics getTrackStatistics() {
        return trackStatistics;
    }

    public void setTrackStatistics(TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
    }

    public static class Id implements Parcelable {

        private final long id;

        public Id(long id) {
            this.id = id;
        }

        protected Id(Parcel in) {
            id = in.readLong();
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

        @Override
        public int describeContents() {
            return 0;
        }

        @NonNull
        @Override
        public String toString() {
            return String.valueOf(id);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(id);
        }

        public static final Parcelable.Creator<Track.Id> CREATOR = new Parcelable.Creator<Track.Id>() {
            public Track.Id createFromParcel(Parcel in) {
                return new Track.Id(in.readLong());
            }

            public Track.Id[] newArray(int size) {
                return new Track.Id[size];
            }
        };
    }
}
