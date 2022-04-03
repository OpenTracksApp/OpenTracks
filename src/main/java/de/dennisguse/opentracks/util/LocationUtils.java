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
package de.dennisguse.opentracks.util;

import android.location.Location;

import de.dennisguse.opentracks.data.models.Distance;

/**
 * Utility class for decimating tracks at a given level of precision.
 *
 * @author Leif Hendrik Wilden
 */
public class LocationUtils {

    private static final String TAG = LocationUtils.class.getSimpleName();

    private LocationUtils() {
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     *
     * @param location the location to test
     * @return true if the location is a valid location.
     */
    public static boolean isValidLocation(Location location) {
        return location != null
                && Math.abs(location.getLatitude()) <= 90
                && Math.abs(location.getLongitude()) <= 180;
    }

    public static boolean fulfillsAccuracy(Location location, Distance thresholdHorizontalAccuracy) {
        return location.hasAccuracy() &&
                Distance.of(location.getAccuracy())
                        .lessThan(thresholdHorizontalAccuracy);

    }
}
