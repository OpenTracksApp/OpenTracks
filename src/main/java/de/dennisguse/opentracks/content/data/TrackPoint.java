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
package de.dennisguse.opentracks.content.data;

import android.location.Location;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * This class extends the standard Android location with extra information.
 * <p>
 * NOTE: default location will be latitude=0.0 and longitude=0.0.
 * For {@link TrackPoint}s with `type == null` this is not meaningful.
 * <p>
 * NOTE: For Locations provided by the GPS.
 * We are replacing the GPS-provided time using the system time.
 * Then we have the same timestamps for the user-driven events (aka start, pause, resume) and restore {@link de.dennisguse.opentracks.stats.TrackStatistics}.
 * Drawbacks:
 * * GPS-provided timestamp might be more precise (but also have GPS week rollover)
 * * System clock might be changed (and thus non-monotonic)
 * TODO: if these might be a problem, we need to store both timestamps.
 *
 * @author Sandor Dornbush
 */
public class TrackPoint {

    private static final Duration MAX_LOCATION_AGE = Duration.ofMinutes(1);

    private TrackPoint.Id id;

    @NonNull
    private final Instant time;

    private Double latitude;
    private Double longitude;
    private Distance horizontalAccuracy;
    private Altitude altitude;
    private Speed speed;
    private Float bearing;
    private Distance sensorDistance;

    public enum Type {
        SEGMENT_START_MANUAL(-2), //Start of a segment due to user interaction (start, resume)

        SEGMENT_START_AUTOMATIC(-1), //Start of a segment due to too much distance from previous TrackPoint
        TRACKPOINT(0), //Just GPS data.

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

    private Float heartRate_bpm = null;
    private Float cyclingCadence_rpm = null;
    private Float power = null;
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

        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.hasAltitude() ? Altitude.WGS84.of(location.getAltitude()) : null;
        this.speed = location.hasSpeed() ? Speed.of(location.getSpeed()) : null;
        this.horizontalAccuracy = location.hasAccuracy() ? Distance.of(location.getAccuracy()) : null;

        //TODO Should we copy the bearing?
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

    public boolean isSegmentStart() {
        return type == Type.SEGMENT_START_AUTOMATIC || type == Type.SEGMENT_START_MANUAL;
    }

    public boolean isSegmentEnd() {
        return type == Type.SEGMENT_END_MANUAL;
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

    public boolean isMoving() {
        return hasSpeed() && getSpeed().isMoving();
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

    @Nullable
    public Distance distanceToPrevious(TrackPoint previous) {
        if (hasSensorDistance()) {
            return getSensorDistance();
        }
        if (previous == null || !(hasLocation() && previous.hasLocation())) {
            return null;
        }

        return Distance.of(getLocation().distanceTo(previous.getLocation()));
    }

    public boolean fulfillsAccuracy(Distance thresholdHorizontalAccuracy) {
        return hasHorizontalAccuracy() && horizontalAccuracy.lessThan(thresholdHorizontalAccuracy);
    }

    //TODO Bearing requires a location; what do we do if we don't have any?
    public float bearingTo(@NonNull TrackPoint dest) {
        return getLocation().bearingTo(dest.getLocation());
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

    public boolean hasSensorData() {
        return hasHeartRate() || hasCyclingCadence() || hasPower();
    }

    public boolean hasHeartRate() {
        return heartRate_bpm != null && heartRate_bpm > 0;
    }

    public float getHeartRate_bpm() {
        return heartRate_bpm;
    }

    public TrackPoint setHeartRate_bpm(Float heartRate_bpm) {
        this.heartRate_bpm = heartRate_bpm;
        return this;
    }

    public boolean hasCyclingCadence() {
        return cyclingCadence_rpm != null;
    }

    public float getCyclingCadence_rpm() {
        return cyclingCadence_rpm;
    }

    public TrackPoint setCyclingCadence_rpm(Float cyclingCadence_rpm) {
        this.cyclingCadence_rpm = cyclingCadence_rpm;
        return this;
    }

    public boolean hasPower() {
        return power != null;
    }

    public float getPower() {
        return power;
    }

    public TrackPoint setPower(Float power) {
        this.power = power;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        String result = "time=" + getTime() + " (type=" + getType() + ")";
        if (!hasLocation()) {
            return result;
        }
        result += ": lat=" + getLatitude() + " lng=" + getLongitude();
        if (!hasHorizontalAccuracy()) {
            return result;
        }

        return result + " acc=" + getHorizontalAccuracy();
    }

    public static class Id {

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
            TrackPoint.Id id1 = (TrackPoint.Id) o;
            return id == id1.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @NonNull
        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }
}
