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

package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * A track.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
//TODO Do not default initialize attributes; might be confusing for debugging
public class Track {

    private Track.Id id;
    private UUID uuid = UUID.randomUUID();

    private String name = "";
    private String description = "";
    private String activityTypeLocalized = "";

    private ActivityType activityType;

    private ZoneOffset zoneOffset;

    private TrackStatistics trackStatistics = new TrackStatistics();

    @VisibleForTesting
    public Track() {
        this(ZoneOffset.UTC);
    }

    public Track(@NonNull ZoneOffset zoneOffset) {
        setZoneOffset(zoneOffset);
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

    public String getActivityTypeLocalized() {
        return activityTypeLocalized;
    }

    public void setActivityTypeLocalized(String activityTypeLocalized) {
        this.activityTypeLocalized = activityTypeLocalized;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public void setActivityTypeLocalizedAndUpdateActivityType(Context context, String activityTypeLocalized) {
        setActivityTypeLocalized(activityTypeLocalized);
        setActivityType(ActivityType.findByLocalizedString(context, activityTypeLocalized));
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    public void setZoneOffset(@NonNull ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
    }

    public OffsetDateTime getStartTime() {
        return trackStatistics
                .getStartTime().atOffset(zoneOffset);
    }

    public OffsetDateTime getStopTime() {
        return trackStatistics
                .getStopTime().atOffset(zoneOffset);
    }

    @NonNull
    public TrackStatistics getTrackStatistics() {
        return trackStatistics;
    }

    public void setTrackStatistics(@NonNull TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
    }

    @Override
    public String toString() {
        return "Track{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", activityTypeLocalized='" + activityTypeLocalized + '\'' +
                ", activityType=" + activityType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return id.equals(track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public record Id(long id) implements Parcelable {

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(id);
        }

        public static final Creator<Track.Id> CREATOR = new Creator<>() {
            public Track.Id createFromParcel(Parcel in) {
                return new Track.Id(in.readLong());
            }

            public Track.Id[] newArray(int size) {
                return new Track.Id[size];
            }
        };
    }
}
