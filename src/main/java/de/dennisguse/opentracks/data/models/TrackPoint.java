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
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;

/**
 * Sensor and/or location information for a specific point in time.
 * <p>
 * Time is created using the {@link de.dennisguse.opentracks.services.handlers.MonotonicClock}, because system time jump backwards.
 * GPS time is ignored as for non-GPS events, we could not create timestamps.
 */
public class TrackPoint {

    private static final Duration MAX_LOCATION_AGE = Duration.ofMinutes(1);

    private TrackPoint.Id id;

    @NonNull
    private final Instant time;

    private Double latitude;
    private Double longitude;
    private Distance horizontalAccuracy;
    private Distance verticalAccuracy;
    private Altitude altitude; //TODO use Altitude.WGS84
    private Speed speed;
    private Float bearing;
    private Distance sensorDistance;

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

    private HeartRate heartRate = null;
    private Cadence cadence = null;
    private Power power = null;
    private Float altitudeGain_m = null;
    private Float altitudeLoss_m = null;

    public TrackPoint(@NonNull Type type, @NonNull Instant time) {
        this.type = type;
        this.time = time;
    }

    public TrackPoint(@NonNull Location location, @NonNull Instant time) {
        this(Type.TRACKPOINT, location, time);
    }

    public TrackPoint(@NonNull Type type, @NonNull Location location, @NonNull Instant time) {
        this(type, time);

        setLocation(location);
    }

    @VisibleForTesting
    public TrackPoint(double latitude, double longitude, Altitude altitude, Instant time) {
        this(Type.TRACKPOINT, time);
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
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

    public TrackPoint setType(@NonNull Type type) {
        this.type = type;
        return this;
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

    public void setId(TrackPoint.Id id) {
        this.id = id;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public double getLatitude() {
        return latitude;
    }

    public TrackPoint setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public TrackPoint setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    //TODO Better return null, if no location is present aka latitude == null etc.
    @NonNull
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
        if (hasHorizontalAccuracy()) {
            location.setAccuracy((float) horizontalAccuracy.toM());
        }
        if (hasAltitude()) {
            location.setAltitude(altitude.toM());
        }
        if (hasSpeed()) {
            location.setSpeed((float) speed.toMPS());
        }

        return location;
    }

    public TrackPoint setLocation(@NonNull Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.hasAltitude() ? Altitude.WGS84.of(location.getAltitude()) : null;
        this.speed = location.hasSpeed() ? Speed.of(location.getSpeed()) : null;
        this.horizontalAccuracy = location.hasAccuracy() ? Distance.of(location.getAccuracy()) : null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.verticalAccuracy = location.hasVerticalAccuracy() ? Distance.of(location.getVerticalAccuracyMeters()) : null;
        }

        //TODO Should we copy the bearing?
        return this;
    }

    public boolean hasAltitudeGain() {
        return altitudeGain_m != null;
    }

    public float getAltitudeGain() {
        return altitudeGain_m;
    }

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

    public TrackPoint setAltitudeLoss(Float altitudeLoss_m) {
        this.altitudeLoss_m = altitudeLoss_m;
        return this;
    }

    @NonNull
    public Instant getTime() {
        return time;
    }

    public boolean isRecent() {
        return Instant.now()
                .isBefore(time.plus(MAX_LOCATION_AGE));
    }


    public boolean hasAltitude() {
        return altitude != null;
    }

    public Altitude getAltitude() {
        return altitude;
    }

    @VisibleForTesting
    public TrackPoint setAltitude(double altitude_m) {
        this.altitude = Altitude.WGS84.of(altitude_m);
        return this;
    }

    public TrackPoint setAltitude(Altitude altitude) {
        this.altitude = altitude;
        return this;
    }

    public boolean hasSpeed() {
        return speed != null;
    }

    public Speed getSpeed() {
        return speed;
    }

    public TrackPoint setSpeed(Speed speed) {
        this.speed = speed;
        return this;
    }
    public boolean hasBearing() {
        return bearing != null;
    }

    public float getBearing() {
        return bearing;
    }

    public TrackPoint setBearing(Float bearing) {
        this.bearing = bearing;
        return this;
    }

    public boolean hasHorizontalAccuracy() {
        return horizontalAccuracy != null;
    }

    public Distance getHorizontalAccuracy() {
        return horizontalAccuracy;
    }

    public TrackPoint setHorizontalAccuracy(Distance horizontalAccuracy) {
        this.horizontalAccuracy = horizontalAccuracy;
        return this;
    }

    public boolean hasVerticalAccuracy() {
        return verticalAccuracy != null;
    }

    public Distance getVerticalAccuracy() {
        return verticalAccuracy;
    }

    public TrackPoint setVerticalAccuracy(Distance horizontalAccuracy) {
        this.verticalAccuracy = horizontalAccuracy;
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
        return hasHorizontalAccuracy() && horizontalAccuracy.lessThan(thresholdHorizontalAccuracy);
    }

    public float bearingTo(@NonNull TrackPoint dest) {
        return bearingTo(dest.getLocation());
    }

    //TODO Bearing requires a location; what do we do if we don't have any?
    public float bearingTo(@NonNull Location dest) {
        return getLocation().bearingTo(dest);
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
                ", time=" + time +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", horizontalAccuracy=" + horizontalAccuracy +
                ", verticalAccuracy=" + verticalAccuracy +
                ", altitude=" + altitude +
                ", speed=" + speed +
                ", bearing=" + bearing +
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
