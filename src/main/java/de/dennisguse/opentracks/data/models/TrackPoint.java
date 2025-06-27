/*
 * Copyright 2010 Google Inc.
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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

/**
 * Sensor and/or location information for a specific point in time.
 * <p>
 * Time is created using the {@link de.dennisguse.opentracks.services.handlers.MonotonicClock}, because system time jump backwards.
 * GPS time is ignored as for non-GPS events, we could not create GPS-based timestamps.
 */
//TODO Should be a record (with final properties)
public class TrackPoint {

    @Nullable
    private TrackPoint.Id id;

    //Requires: position.time must be non-null
    @NonNull
    private Position position;

    public enum Type {
        SEGMENT_START_MANUAL(-2), //Start of a segment due to user interaction (start, resume)

        SEGMENT_START_AUTOMATIC(-1), //Start of a segment due to too much distance from previous TrackPoint
        TRACKPOINT(0), //Was created due to sensor data (may contain GPS or other BLE data)

        // Was used to distinguish the source (i.e., GPS vs BLE sensor), but this was too complicated. Everything is now a TRACKPOINT again.
        @Deprecated
        SENSORPOINT(2),
        IDLE(3), //Device became idle

        SEGMENT_END_MANUAL(1); //End of a segment

        public final int type_db;


        Type(int type_db) {
            this.type_db = type_db;
        }

        @NonNull
        @Override
        public String toString() {
            return name() + "(" + type_db + ")";
        }

        public static Type getById(int id) {
            for (Type e : values()) {
                if (e.type_db == id) return e;
            }

            throw new RuntimeException("unknown id: " + id);
        }
    }

    @NonNull
    private Type type;

    private Distance sensorDistance;
    private HeartRate heartRate = null;
    private Cadence cadence = null;
    private Power power = null;
    private Float altitudeGain_m = null;
    private Float altitudeLoss_m = null;

    public TrackPoint(@Nullable TrackPoint.Id id, @NonNull Type type, @NonNull Position position) {
        this.id = id;
        this.type = type;
        this.position = position;
    }

    public TrackPoint(@NonNull Type type, @NonNull Instant time) {
        this(null, type, Position.of(time));
    }

    public TrackPoint(@NonNull Type type, @NonNull Position position) {
        this(null, type, position);
    }

    public static TrackPoint createSegmentStartManualWithTime(Instant time) {
        return new TrackPoint(Type.SEGMENT_START_MANUAL, time);
    }

    public static TrackPoint createSegmentEndWithTime(Instant time) {
        return new TrackPoint(Type.SEGMENT_END_MANUAL, time);
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @Deprecated //Should not be needed.
    public TrackPoint setType(@NonNull Type type) {
        this.type = type;
        return this;
    }

    public boolean isIdleTriggered() {
        return type == Type.IDLE;
    }

    public boolean isSegmentManualStart() {
        return type == Type.SEGMENT_START_MANUAL;
    }

    public boolean isSegmentManualEnd() {
        return type == Type.SEGMENT_END_MANUAL;
    }

    public boolean wasCreatedManually() {
        return hasLocation() || hasSpeed();
    }

    /**
     * May be null if the track was not loaded from the database.
     */
    @Nullable
    public TrackPoint.Id getId() {
        return id;
    }

    public boolean hasLocation() {
        return position.hasLocation();
    }

    @NonNull
    public Position getPosition() {
        return position;
    }

    public TrackPoint setPosition(Position position) {
        this.position = position.with(this.position.time());
        return this;
    }

    @Deprecated
    @NonNull
    public Location getLocation() {
        return getPosition().toLocation();
    }

    public boolean hasAltitudeGain() {
        return altitudeGain_m != null;
    }

    public float getAltitudeGain() {
        return altitudeGain_m;
    }

    @Deprecated
    public TrackPoint setAltitudeGain(Float altitudeGain_m) {
        this.altitudeGain_m = altitudeGain_m;
        return this;
    }

    public boolean hasAltitudeLoss() {
        return altitudeLoss_m != null;
    }

    public float getAltitudeLoss() {
        return altitudeLoss_m;
    }

    @Deprecated
    public TrackPoint setAltitudeLoss(Float altitudeLoss_m) {
        this.altitudeLoss_m = altitudeLoss_m;
        return this;
    }

    @NonNull
    public Instant getTime() {
        return position.time();
    }

    public boolean hasAltitude() {
        return position.hasAltitude();
    }

    public Altitude getAltitude() {
        return position.altitude();
    }

    @Deprecated
    public TrackPoint setAltitude(Altitude altitude) {
        position = position.with(altitude);
        return this;
    }

    public boolean hasSpeed() {
        return position.hasSpeed();
    }

    public Speed getSpeed() {
        return position.speed();
    }

    @Deprecated
    public TrackPoint setSpeed(Speed speed) {
        this.position = position.with(speed);
        return this;
    }

    public boolean hasBearing() {
        return position.hasBearing();
    }

    public float getBearing() {
        return position.bearing();
    }

    public TrackPoint setBearing(Float bearing) {
        this.position = this.position.withBearing(bearing);
        return this;
    }

    public boolean hasHorizontalAccuracy() {
        return position.hasHorizontalAccuracy();
    }

    public Distance getHorizontalAccuracy() {
        return position.horizontalAccuracy();
    }

    @Deprecated
    public TrackPoint setHorizontalAccuracy(Distance horizontalAccuracy) {
        this.position = this.position.withHorizontalAccuracy(horizontalAccuracy);
        return this;
    }

    public boolean hasVerticalAccuracy() {
        return position.hasVerticalAccuracy();
    }

    public Distance getVerticalAccuracy() {
        return position.verticalAccuracy();
    }

    public TrackPoint setVerticalAccuracy(Distance verticalAccuracy) {
        this.position = this.position.withVerticalAccuracy(verticalAccuracy);
        return this;
    }

    @NonNull
    public Distance distanceToPrevious(@NonNull TrackPoint previous) {
        if (hasSensorDistance()) {
            return getSensorDistance();
        }
        return distanceToPreviousFromLocation(previous);
    }

    @NonNull
    public Distance distanceToPreviousFromLocation(@NonNull TrackPoint previous) {
        if (!hasLocation() || hasLocation() != previous.hasLocation()) {
            throw new RuntimeException("Cannot compute distance.");
        }

        return Distance.of(getLocation().distanceTo(previous.getLocation()));
    }

    public boolean fulfillsAccuracy(Distance thresholdHorizontalAccuracy) {
        return position.fulfillsAccuracy(thresholdHorizontalAccuracy);
    }

    public Optional<Float> bearingTo(@NonNull Position dest) {
        if (!dest.hasLocation() || !hasLocation()) {
            return Optional.empty();
        }
        return bearingTo(dest.toLocation());
    }

    public Optional<Float> bearingTo(@NonNull TrackPoint dest) {
        if (!dest.hasLocation() || !hasLocation()) {
            return Optional.empty();
        }
        return bearingTo(dest.getLocation());
    }

    //TODO Bearing requires a location; what do we do if we don't have any?
    @Deprecated
    public Optional<Float> bearingTo(@NonNull Location dest) {
        if (!hasLocation()) {
            return Optional.empty();
        }
        return Optional.of(getLocation().bearingTo(dest));
    }

    // Sensor data
    public boolean hasSensorDistance() {
        return sensorDistance != null;
    }

    public Distance getSensorDistance() {
        return sensorDistance;
    }

    public TrackPoint setSensorDistance(Distance distance_m) {
        this.sensorDistance = distance_m;
        return this;
    }

    public TrackPoint minusCumulativeSensorData(@NonNull TrackPoint lastTrackPoint) {
        if (hasSensorDistance() && lastTrackPoint.hasSensorDistance()) {
            sensorDistance = sensorDistance.minus(lastTrackPoint.getSensorDistance());
        }
        if (hasAltitudeGain() && lastTrackPoint.hasAltitudeGain()) {
            altitudeGain_m -= lastTrackPoint.altitudeGain_m;
        }
        if (hasAltitudeLoss() && lastTrackPoint.hasAltitudeLoss()) {
            altitudeLoss_m -= lastTrackPoint.altitudeLoss_m;
        }
        return this;
    }

    public boolean hasHeartRate() {
        return heartRate != null;
    }

    public HeartRate getHeartRate() {
        return heartRate;
    }

    public TrackPoint setHeartRate(HeartRate heartRate) {
        this.heartRate = heartRate;
        return this;
    }

    public TrackPoint setHeartRate(float heartRate) {
        return setHeartRate(HeartRate.of(heartRate));
    }

    public boolean hasCadence() {
        return cadence != null;
    }

    public Cadence getCadence() {
        return cadence;
    }

    public TrackPoint setCadence(Cadence cadence) {
        this.cadence = cadence;
        return this;
    }

    public TrackPoint setCadence(float cadence) {
        return setCadence(Cadence.of(cadence));
    }

    public boolean hasPower() {
        return power != null;
    }

    public Power getPower() {
        return power;
    }

    public TrackPoint setPower(Power power) {
        this.power = power;
        return this;
    }

    public TrackPoint setPower(float power) {
        return setPower(Power.of(power));
    }

    @NonNull
    @Override
    public String toString() {
        return "TrackPoint{" +
                "id=" + id +
                ", position=" + position +
                ", sensorDistance=" + sensorDistance +
                ", type=" + type +
                ", heartRate=" + heartRate +
                ", cadence=" + cadence +
                ", power=" + power +
                ", altitudeGain_m=" + altitudeGain_m +
                ", altitudeLoss_m=" + altitudeLoss_m +
                '}';
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
