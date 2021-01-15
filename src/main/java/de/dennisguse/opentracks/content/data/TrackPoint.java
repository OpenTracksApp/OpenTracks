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
import android.location.LocationManager;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * This class extends the standard Android location with extra information.
 * <p>
 * NOTE: default location will be latitude=0.0 and longitude=0.0 (this is not meaningful).
 * <p>
 * NOTE: For Locations provided by the GPS.
 * We are replacing the GPS-provided time using the system time.
 * Then we have the same timestamps for the user-driven events (aka start, pause, resume) and restore {@link de.dennisguse.opentracks.stats.TrackStatistics}.
 * Drawbacks:
 * * GPS-provided timestamp might be more precise (but also have GPS week rollover)
 * * System clock might be changed (and thus non-monotonic)
 * TODO: if these might be problems, we need to store both timestamps.
 *
 * @author Sandor Dornbush
 */
//TODO Merge constructors by use case; we have too many.
public class TrackPoint {

    private TrackPoint.Id id;

    private final Location location;

    //TODO Private
    public enum Type {
        SEGMENT_START_MANUAL(-2), //Start of a segment due to user interaction (start, resume); no useful coordinates

        SEGMENT_START_AUTOMATIC(-1), //Start of a segment due to too much distance from previous TrackPoint
        TRACKPOINT(0), //Normal trackpoint got from GPS

        SEGMENT_END_MANUAL(1); //End of a segment; no useful coordinates

        public final int type_db;

        Type(int type_db) {
            this.type_db = type_db;
        }

        @Override
        public String toString() {
            return "" + type_db;
        }

        public static Type getById(int id) {
            for (Type e : values()) {
                if (e.type_db == id) return e;
            }

            throw new RuntimeException("unknown id: " + id);
        }

        public boolean hasLocation() {
            return this == SEGMENT_START_AUTOMATIC || this == TRACKPOINT;
        }
    }

    private final Type type;

    private Float heartRate_bpm = null;
    private Float cyclingCadence_rpm = null;
    private Float power = null;
    private Float elevationGain = null;
    private Float elevationLoss = null;

    public TrackPoint() {
        this(Type.TRACKPOINT, new Location(""));
    }

    public TrackPoint(Type type) {
        this.type = type;
        this.location = new Location("");
    }

    public TrackPoint(@NonNull Location location) {
        this.type = Type.TRACKPOINT;
        this.location = location;
    }

    public TrackPoint(@NonNull Type type, @NonNull Location location) {
        this.type = type;
        this.location = location;
    }

    public TrackPoint(@NonNull TrackPoint trackPoint) {
        this.type = trackPoint.getType();
        this.location = trackPoint.getLocation();

        this.heartRate_bpm = trackPoint.getHeartRate_bpm();
        this.cyclingCadence_rpm = trackPoint.getCyclingCadence_rpm();
        this.power = trackPoint.getPower();

        this.elevationGain = trackPoint.getElevationGain();
        this.elevationLoss = trackPoint.getElevationLoss();
    }

    public TrackPoint(double latitude, double longitude, Double altitude, long time) {
        this.type = Type.TRACKPOINT;
        location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        if (altitude != null) {
            location.setAltitude(altitude);
        }
        location.setTime(time);
    }

    @Deprecated //See #316
    public static TrackPoint createSegmentStartManual() {
        return createSegmentStartManualWithTime(System.currentTimeMillis());
    }

    public static TrackPoint createSegmentStartManualWithTime(long time) {
        Location resume = new Location(LocationManager.GPS_PROVIDER);
        resume.setTime(time);
        return new TrackPoint(Type.SEGMENT_START_MANUAL, resume);
    }

    @Deprecated //See #316
    public static TrackPoint createSegmentStartAutomatic() {
        return createSegmentStartAutomaticWithTime(System.currentTimeMillis());
    }

    public static TrackPoint createSegmentStartAutomaticWithTime(long time) {
        Location resume = new Location(LocationManager.GPS_PROVIDER);
        resume.setTime(time);
        return new TrackPoint(Type.SEGMENT_START_AUTOMATIC, resume);
    }

    public static TrackPoint createSegmentEnd() {
        return createSegmentEndWithTime(System.currentTimeMillis());
    }

    public static TrackPoint createSegmentEndWithTime(@NonNull TrackPoint trackPoint) {
        return createSegmentEndWithTime(trackPoint.getTime());
    }

    public static TrackPoint createSegmentEndWithTime(long time) {
        Location pause = new Location(LocationManager.GPS_PROVIDER);
        pause.setTime(time);
        return new TrackPoint(Type.SEGMENT_END_MANUAL, pause);
    }

    @NonNull
    public Type getType() {
        return type;
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

    @Nullable
    public Location getLocation() {
        return location;
    }

    public boolean hasElevationGain() {
        return elevationGain != null;
    }

    public float getElevationGain() {
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

    public double getLatitude() {
        return location.getLatitude();
    }

    public void setLatitude(double latitude) {
        location.setLatitude(latitude);
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public void setLongitude(double longitude) {
        location.setLongitude(longitude);
    }

    public long getTime() {
        return location.getTime();
    }

    public void setTime(long time) {
        location.setTime(time);
    }

    public boolean hasAltitude() {
        return location.hasAltitude();
    }

    public double getAltitude() {
        return location.getAltitude();
    }

    public void setAltitude(double altitude) {
        location.setAltitude(altitude);
    }

    public boolean hasSpeed() {
        return location.hasSpeed();
    }

    public float getSpeed() {
        return location.getSpeed();
    }

    public void setSpeed(float speed) {
        location.setSpeed(speed);
    }

    public boolean hasBearing() {
        return location.hasBearing();
    }

    public float getBearing() {
        return location.getBearing();
    }

    public void setBearing(float bearing) {
        location.setBearing(bearing);
    }

    public boolean hasAccuracy() {
        return location.hasAccuracy();
    }

    public float getAccuracy() {
        return location.getAccuracy();
    }

    public void setAccuracy(float horizontalAccuracy) {
        location.setAccuracy(horizontalAccuracy);
    }

    public float distanceTo(@NonNull TrackPoint dest) {
        return location.distanceTo(dest.getLocation());
    }

    public float bearingTo(@NonNull TrackPoint dest) {
        return location.bearingTo(dest.getLocation());
    }

    public float bearingTo(@NonNull Location dest) {
        return location.bearingTo(dest);
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
        return "time=" + getTime() + " (type=" + getType() + "): lat=" + getLatitude() + " lng=" + getLongitude() + " acc=" + getAccuracy();
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
