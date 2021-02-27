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

    // Anything faster than that (in meters per second) will be considered moving.
    private static final double MAX_NO_MOVEMENT_SPEED = 0.224;

    private TrackPoint.Id id;

    private Instant time;
    private Double latitude;
    private Double longitude;
    private Float accuracy;
    private Double altitude_m;
    private Float speed_mps;
    private Float bearing;

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
    private Float elevationGain = null;
    private Float elevationLoss = null;

    public TrackPoint(@NonNull Type type) {
        this.type = type;
    }

    public TrackPoint(@NonNull Location location) {
        this(Type.TRACKPOINT);

        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude_m = location.getAltitude();
        this.speed_mps = location.getSpeed();
        this.accuracy = location.getAccuracy();

        setTime(Instant.now());
    }

    public TrackPoint(@NonNull Type type, Instant time) {
        this(type);
        this.time = time;
    }

    public TrackPoint(double latitude, double longitude, Double altitude, Instant time) {
        this(Type.TRACKPOINT);
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude_m = altitude;
        this.time = time;
    }

    @Deprecated //See #316
    public static TrackPoint createSegmentStartManual() {
        return createSegmentStartManualWithTime(Instant.now());
    }

    public static TrackPoint createSegmentStartManualWithTime(Instant time) {
        return new TrackPoint(Type.SEGMENT_START_MANUAL, time);
    }

    @Deprecated //See #316
    public static TrackPoint createSegmentStartAutomatic() {
        return createSegmentStartAutomaticWithTime(Instant.now());
    }

    public static TrackPoint createSegmentStartAutomaticWithTime(Instant time) {
        return new TrackPoint(Type.SEGMENT_START_AUTOMATIC, time);
    }

    public static TrackPoint createSegmentEnd() {
        return createSegmentEndWithTime(Instant.now());
    }

    public static TrackPoint createSegmentEndWithTime(Instant time) {
        return new TrackPoint(Type.SEGMENT_END_MANUAL, time);
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public void setType(@NonNull Type type) {
        this.type = type;
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

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

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
        if (hasAccuracy()) {
            location.setAccuracy(accuracy);
        }
        if (hasAltitude()) {
            location.setAltitude(altitude_m);
        }

        return location;
    }

    public boolean hasElevationGain() {
        return elevationGain != null;
    }

    public Float getElevationGain() {
        return elevationGain;
    }

    public void setElevationGain(Float elevationGain) {
        this.elevationGain = elevationGain;
    }

    public boolean hasElevationLoss() {
        return elevationLoss != null;
    }

    public float getElevationLoss() {
        return elevationLoss;
    }

    public void setElevationLoss(Float elevationLoss) {
        this.elevationLoss = elevationLoss;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public boolean isRecent() {
        return Instant.now()
                .isBefore(time.plus(MAX_LOCATION_AGE));
    }


    public boolean hasAltitude() {
        return altitude_m != null;
    }

    public double getAltitude() {
        return altitude_m;
    }

    public void setAltitude(double altitude) {
        this.altitude_m = altitude;
    }

    public boolean hasSpeed() {
        return speed_mps != null;
    }

    public float getSpeed() {
        return speed_mps;
    }

    public void setSpeed(Float speed) {
        this.speed_mps = speed;
    }

    public boolean isMoving() {
        return hasSpeed() && getSpeed() >= MAX_NO_MOVEMENT_SPEED;
    }

    public boolean hasBearing() {
        return bearing != null;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public boolean hasAccuracy() {
        return accuracy != null;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float horizontalAccuracy) {
        this.accuracy = horizontalAccuracy;
    }

    public float distanceTo(@NonNull TrackPoint dest) {
        return getLocation().distanceTo(dest.getLocation());
    }

    public boolean fulfillsAccuracy(int poorAccuracy) {
        return hasAccuracy() && accuracy < poorAccuracy;
    }

    public float bearingTo(@NonNull TrackPoint dest) {
        return getLocation().bearingTo(dest.getLocation());
    }

    public float bearingTo(@NonNull Location dest) {
        return getLocation().bearingTo(dest);
    }

    // Sensor data
    public boolean hasSensorData() {
        return hasHeartRate() || hasCyclingCadence() || hasPower();
    }

    public boolean hasHeartRate() {
        return heartRate_bpm != null && heartRate_bpm > 0;
    }

    public float getHeartRate_bpm() {
        return heartRate_bpm;
    }

    public void setHeartRate_bpm(Float heartRate_bpm) {
        this.heartRate_bpm = heartRate_bpm;
    }

    public boolean hasCyclingCadence() {
        return cyclingCadence_rpm != null;
    }

    public Float getCyclingCadence_rpm() {
        return cyclingCadence_rpm;
    }

    public void setCyclingCadence_rpm(Float cyclingCadence_rpm) {
        this.cyclingCadence_rpm = cyclingCadence_rpm;
    }

    public boolean hasPower() {
        return power != null;
    }

    public Float getPower() {
        return power;
    }

    public void setPower(Float power) {
        this.power = power;
    }

    @NonNull
    @Override
    public String toString() {
        String result = "time=" + getTime() + " (type=" + getType() + ")";
        if (!hasLocation()) {
            return result;
        }
        result += ": lat=" + getLatitude() + " lng=" + getLongitude();
        if (!hasAccuracy()) {
            return result;
        }

        return result + " acc=" + getAccuracy();
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
