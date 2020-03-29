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

import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * This class extends the standard Android location with extra information.
 *
 * @author Sandor Dornbush
 */
public class TrackPoint {

    private Location location;

    public TrackPoint() {
        this.location = new Location("");
    }

    public TrackPoint(@NonNull Location location) {
        this.location = location;
    }

    public TrackPoint(@NonNull Location location, SensorDataSet sensorDataSet) {
        this.location = location;
        this.sensorDataSet = sensorDataSet;
    }

    public TrackPoint(@NonNull TrackPoint trackPoint, SensorDataSet sensorDataSet) {
        this.location = trackPoint.getLocation();
        this.sensorDataSet = sensorDataSet;
    }

    private SensorDataSet sensorDataSet = null;

    public TrackPoint(double latitude, double longitude, Double altitude, long time) {
        location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        if (altitude != null) {
            location.setAltitude(altitude);
        }
        location.setTime(time);
    }

    public static TrackPoint createPause() {
        return createPauseWithTime(System.currentTimeMillis());
    }

    public static TrackPoint createPauseWithTime(long time) {
        Location pause = new Location(LocationManager.GPS_PROVIDER);
        pause.setLongitude(0);
        pause.setLatitude(TrackPointsColumns.PAUSE_LATITUDE);
        pause.setTime(time);
        return new TrackPoint(pause);
    }

    public static TrackPoint createResume() {
        return createResumeWithTime(System.currentTimeMillis());
    }

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

    public SensorDataSet getSensorDataSet() {
        return sensorDataSet;
    }

    public void setSensorDataSet(SensorDataSet sensorDataSet) {
        this.sensorDataSet = sensorDataSet;
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
}
