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

import androidx.annotation.NonNull;

/**
 * This class extends the standard Android location with extra information.
 *
 * @author Sandor Dornbush
 */
public class TrackPoint {

    private final Location location;

    private Float heartRate_bpm = null;
    private Float cyclingCadence_rpm = null;
    private Float power = null;
    private float elevationGain = Float.NaN;

    public TrackPoint() {
        this.location = new Location("");
    }

    public TrackPoint(@NonNull Location location) {
        this.location = location;
    }

    public TrackPoint(@NonNull TrackPoint trackPoint) {
        this.location = trackPoint.getLocation();

        this.heartRate_bpm = trackPoint.getHeartRate_bpm();
        this.cyclingCadence_rpm = trackPoint.getCyclingCadence_rpm();
        this.power = trackPoint.getPower();
    }

    public TrackPoint(double latitude, double longitude, Double altitude, long time) {
        location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        if (altitude != null) {
            location.setAltitude(altitude);
        }
        location.setTime(time);
    }

    @Deprecated //See #316
    public static TrackPoint createPause() {
        return createPauseWithTime(System.currentTimeMillis());
    }

    @Deprecated //See #316
    public static TrackPoint createPauseWithTime(long time) {
        Location pause = new Location(LocationManager.GPS_PROVIDER);
        pause.setLongitude(0);
        pause.setLatitude(TrackPointsColumns.PAUSE_LATITUDE);
        pause.setTime(time);
        return new TrackPoint(pause);
    }

    @Deprecated //See #316
    public static TrackPoint createResume() {
        return createResumeWithTime(System.currentTimeMillis());
    }

    @Deprecated //See #316
    public static TrackPoint createResumeWithTime(long time) {
        Location resume = new Location(LocationManager.GPS_PROVIDER);
        resume.setLongitude(0);
        resume.setLatitude(TrackPointsColumns.RESUME_LATITUDE);
        resume.setTime(time);
        return new TrackPoint(resume);
    }

    public @NonNull
    Location getLocation() {
        return location;
    }

    public boolean hasElevationGain() {
        return !Float.isNaN(elevationGain);
    }

    public float getElevationGain() {
        return elevationGain;
    }

    public void setElevationGain(float elevationGain) {
        this.elevationGain = elevationGain;
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
        return "time=" + getTime() + ": lat=" + getLatitude() + " lng=" + getLongitude() + " acc=" + getAccuracy();
    }
}
