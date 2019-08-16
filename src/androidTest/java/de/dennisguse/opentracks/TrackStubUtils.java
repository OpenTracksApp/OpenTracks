/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks;

import android.location.Location;

import de.dennisguse.opentracks.content.SensorDataSetLocation;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * Commons utilities for creating stubs of track, location. The class will be
 * enriched if needs more similar stubs for test.
 *
 * @author Youtao Liu
 */
public class TrackStubUtils {

    public static final double INITIAL_ALTITUDE = 22;
    public static final long INITIAL_TIME = 1000L;
    private static final String LOCATION_PROVIDER = "gps";
    private static final double INITIAL_LATITUDE = 22;
    private static final double INITIAL_LONGITUDE = 22;
    private static final float INITIAL_ACCURACY = 5;
    private static final float INITIAL_SPEED = 10;
    private static final float INITIAL_BEARING = 3.0f;
    // Used to change the value of latitude, longitude, and altitude.
    private static final double DIFFERENCE = 0.01;

    /**
     * Gets a a {@link Track} stub with specified number of locations.
     *
     * @param numberOfLocations the number of locations for the track
     * @return a track stub.
     */
    public static Track createTrack(int numberOfLocations) {
        Track track = new Track();
        for (int i = 0; i < numberOfLocations; i++) {
            track.addLocation(createSensorDataSetLocation(INITIAL_LATITUDE + i * DIFFERENCE, INITIAL_LONGITUDE
                    + i * DIFFERENCE, INITIAL_ALTITUDE + i * DIFFERENCE));
        }

        return track;
    }

    /**
     * Create a MyTracks location with default values.
     *
     * @return a track stub.
     */
    public static SensorDataSetLocation createSensorDataSetLocation() {
        return createSensorDataSetLocation(INITIAL_LATITUDE, INITIAL_LONGITUDE, INITIAL_ALTITUDE);
    }

    /**
     * Creates a {@link SensorDataSetLocation} stub with specified values.
     *
     * @return a SensorDataSetLocation stub.
     */
    public static SensorDataSetLocation createSensorDataSetLocation(double latitude, double longitude, double altitude) {
        // Initial Location
        Location loc = new Location(LOCATION_PROVIDER);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setAltitude(altitude);
        loc.setAccuracy(INITIAL_ACCURACY);
        loc.setSpeed(INITIAL_SPEED);
        loc.setTime(INITIAL_TIME);
        loc.setBearing(INITIAL_BEARING);
        SensorDataSet sd = new SensorDataSet(Float.NaN, Float.NaN);

        return new SensorDataSetLocation(loc, sd);
    }

}
