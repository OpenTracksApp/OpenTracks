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

package de.dennisguse.opentracks.chart;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;

/**
 * Commons utilities for creating stubs of track, location.
 *
 * @author Youtao Liu
 */
//TODO Move to ChartPointTest
class TrackStubUtils {

    public static final double INITIAL_ALTITUDE = 22;
    public static final long INITIAL_TIME = 1000L;
    private static final double INITIAL_LATITUDE = 22;
    private static final double INITIAL_LONGITUDE = 22;
    private static final Distance INITIAL_ACCURACY = Distance.of(5);
    private static final Speed INITIAL_SPEED = Speed.of(10);
    private static final float INITIAL_BEARING = 3.0f;

    /**
     * Create a MyTracks location with default values.
     *
     * @return a track stub.
     */
    public static TrackPoint createDefaultTrackPoint() {
        return createDefaultTrackPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE, INITIAL_ALTITUDE);
    }

    /**
     * Creates a {@link TrackPoint} stub with specified values.
     *
     * @return a SensorDataSetLocation stub.
     */
    private static TrackPoint createDefaultTrackPoint(double latitude, double longitude, double altitude) {
        return new TrackPoint(TrackPoint.Type.TRACKPOINT,
                new Position(
                        Instant.ofEpochMilli(INITIAL_TIME),
                        latitude,
                        longitude,
                        INITIAL_ACCURACY,
                        Altitude.WGS84.of(altitude),
                        INITIAL_ACCURACY,
                        INITIAL_BEARING,
                        INITIAL_SPEED
                ));
    }
}
