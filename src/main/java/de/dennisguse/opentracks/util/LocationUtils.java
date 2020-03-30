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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * Utility class for decimating tracks at a given level of precision.
 *
 * @author Leif Hendrik Wilden
 */
public class LocationUtils {

    private static final String TAG = LocationUtils.class.getSimpleName();

    // 1 minute in milliseconds
    private static final long MAX_LOCATION_AGE_MS = (long) (UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);

    private LocationUtils() {
    }

    /**
     * Computes the distance on the two sphere between the point c0 and the line segment c1 to c2.
     *
     * @param c0 the first coordinate
     * @param c1 the beginning of the line segment
     * @param c2 the end of the lone segment
     * @return the distance in m (assuming spherical earth)
     */
    private static double distance(final Location c0, final Location c1, final Location c2) {
        if (c1.equals(c2)) {
            return c2.distanceTo(c0);
        }

        final double s0lat = c0.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s0lng = c0.getLongitude() * UnitConversions.DEG_TO_RAD;
        final double s1lat = c1.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s1lng = c1.getLongitude() * UnitConversions.DEG_TO_RAD;
        final double s2lat = c2.getLatitude() * UnitConversions.DEG_TO_RAD;
        final double s2lng = c2.getLongitude() * UnitConversions.DEG_TO_RAD;

        double s2s1lat = s2lat - s1lat;
        double s2s1lng = s2lng - s1lng;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);

        if (u <= 0) {
            return c0.distanceTo(c1);
        }

        if (u >= 1) {
            return c0.distanceTo(c2);
        }

        Location sa = new Location("");
        sa.setLatitude(c0.getLatitude() - c1.getLatitude());
        sa.setLongitude(c0.getLongitude() - c1.getLongitude());

        Location sb = new Location("");
        sb.setLatitude(u * (c2.getLatitude() - c1.getLatitude()));
        sb.setLongitude(u * (c2.getLongitude() - c1.getLongitude()));

        return sa.distanceTo(sb);
    }

    /**
     * Decimates the given trackPoints for a given zoom level.
     * This uses a Douglas-Peucker decimation algorithm.
     *
     * @param tolerance in meters
     * @param trackPoints input
     * @param decimated output
     */
    //TODO What was it used for? Sharing data with other apps?
    private static void decimate(double tolerance, List<TrackPoint> trackPoints, List<TrackPoint> decimated) {
        final int n = trackPoints.size();
        if (n < 1) {
            return;
        }
        int idx;
        int maxIdx = 0;
        Stack<int[]> stack = new Stack<>();
        double[] dists = new double[n];
        dists[0] = 1;
        dists[n - 1] = 1;
        double maxDist;
        double dist;
        int[] current;

        if (n > 2) {
            int[] stackVal = new int[]{0, (n - 1)};
            stack.push(stackVal);
            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;
                for (idx = current[0] + 1; idx < current[1]; ++idx) {
                    dist = LocationUtils.distance(trackPoints.get(idx).getLocation(), trackPoints.get(current[0]).getLocation(), trackPoints.get(current[1]).getLocation());
                    if (dist > maxDist) {
                        maxDist = dist;
                        maxIdx = idx;
                    }
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist;
                    int[] stackValCurMax = {current[0], maxIdx};
                    stack.push(stackValCurMax);
                    int[] stackValMaxCur = {maxIdx, current[1]};
                    stack.push(stackValMaxCur);
                }
            }
        }

        int i = 0;
        idx = 0;
        decimated.clear();
        for (TrackPoint l : trackPoints) {
            if (dists[idx] != 0) {
                decimated.add(l);
                i++;
            }
            idx++;
        }
        Log.d(TAG, "Decimating " + n + " points to " + i + " w/ tolerance = " + tolerance);
    }

    /**
     * Decimates the given track for the given precision.
     *
     * @param track     a track
     * @param precision desired precision in meters
     */
    public static void decimate(Track track, double precision) {
        ArrayList<TrackPoint> decimated = new ArrayList<>();
        decimate(precision, track.getTrackPoints(), decimated);
        track.setTrackPoints(decimated);
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @param location the location to test
     * @return true if the location is a valid location.
     */
    //TODO Move to TrackPoint
    public static boolean isValidLocation(Location location) {
        return location != null
                && Math.abs(location.getLatitude()) <= 90
                && Math.abs(location.getLongitude()) <= 180;
    }

    /**
     * Returns true if a location is old.
     *
     * @param location the location
     */
    public static boolean isLocationOld(Location location) {
        return !LocationUtils.isValidLocation(location) || (System.currentTimeMillis() - location.getTime() > MAX_LOCATION_AGE_MS);
    }
}
