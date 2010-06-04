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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;

import com.google.android.maps.GeoPoint;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Utility class for decimating tracks at a given level of precision.
 * 
 * @author Leif Hendrik Wilden
 */
public class MyTracksUtils {

  /**
   * Computes the distance on the two sphere between the point c0 and the line
   * segment c1 to c2.
   * 
   * @param c0 the first coordinate
   * @param c1 the beginning of the line segment
   * @param c2 the end of the lone segment
   * @return the distance in m (assuming spherical earth)
   */
  public static double distance(
      final Location c0, final Location c1, final Location c2) {
    if (c1.equals(c2)) {
      return c2.distanceTo(c0);
    }

    final double s0lat = c0.getLatitude() * UnitConversions.TO_RADIANS;
    final double s0lng = c0.getLongitude() * UnitConversions.TO_RADIANS;
    final double s1lat = c1.getLatitude() * UnitConversions.TO_RADIANS;
    final double s1lng = c1.getLongitude() * UnitConversions.TO_RADIANS;
    final double s2lat = c2.getLatitude() * UnitConversions.TO_RADIANS;
    final double s2lng = c2.getLongitude() * UnitConversions.TO_RADIANS;

    double s2s1lat = s2lat - s1lat;
    double s2s1lng = s2lng - s1lng;
    final double u =
        ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
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
   * Decimates the given locations for a given zoom level. This uses a
   * Douglas-Peucker decimation algorithm.
   * 
   * @param tolerance in meters
   * @param locations input
   * @param decimated output
   */
  public static void decimate(double tolerance, ArrayList<Location> locations,
      ArrayList<Location> decimated) {
    final int n = locations.size();
    if (n < 1) {
      return;
    }
    int idx;
    int maxIdx = 0;
    Stack<int[]> stack = new Stack<int[]>();
    double[] dists = new double[n];
    dists[0] = 1;
    dists[n - 1] = 1;
    double maxDist;
    double dist = 0.0;
    int[] current;

    if (n > 2) {
      int[] stackVal = new int[] {0, (n - 1)};
      stack.push(stackVal);
      while (stack.size() > 0) {
        current = stack.pop();
        maxDist = 0;
        for (idx = current[0] + 1; idx < current[1]; ++idx) {
          dist = MyTracksUtils.distance(
              locations.get(idx),
              locations.get(current[0]),
              locations.get(current[1]));
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
    for (Location l : locations) {
      if (dists[idx] != 0) {
        decimated.add(l);
        i++;
      }
      idx++;
    }
    Log.d(MyTracksConstants.TAG, "Decimating " + n + " points to " + i
        + " w/ tolerance = " + tolerance);
  }

  /**
   * Decimates the given track for the given precision.
   * 
   * @param track a track
   * @param precision desired precision in meters
   */
  public static void decimate(Track track, double precision) {
    ArrayList<Location> decimated = new ArrayList<Location>();
    decimate(precision, track.getLocations(), decimated);
    track.setLocations(decimated);
  }

  /**
   * Limits number of points by dropping any points beyond the given number of
   * points. Note: That'll actually discard points.
   * 
   * @param track a track
   * @param numberOfPoints maximum number of points
   */
  public static void cut(Track track, int numberOfPoints) {
    ArrayList<Location> locations = track.getLocations();
    while (locations.size() > numberOfPoints) {
      locations.remove(locations.size() - 1);
    }
  }

  /**
   * Splits a track in multiple tracks where each piece has less or equal than
   * maxPoints.
   * 
   * @param track the track to split
   * @param maxPoints maximum number of points for each piece
   * @return a list of one or more track pieces
   */
  public static ArrayList<Track> split(Track track, int maxPoints) {
    ArrayList<Track> result = new ArrayList<Track>();
    final int nTotal = track.getLocations().size();
    int n = 0;
    Track piece = null;
    do {
      piece = new Track();
      TripStatistics pieceStats = piece.getStatistics();
      piece.setId(track.getId());
      piece.setName(track.getName());
      piece.setDescription(track.getDescription());
      piece.setCategory(track.getCategory());
      List<Location> pieceLocations = piece.getLocations();
      for (int i = n; i < nTotal && pieceLocations.size() < maxPoints; i++) {
        piece.addLocation(track.getLocations().get(i));
      }
      int nPointsPiece = pieceLocations.size();
      if (nPointsPiece >= 2) {
        pieceStats.setStartTime(pieceLocations.get(0).getTime());
        pieceStats.setStopTime(pieceLocations.get(nPointsPiece - 1).getTime());
        result.add(piece);
      }
      n += (pieceLocations.size() - 1);
    } while (n < nTotal && piece != null && piece.getLocations().size() > 1);

    return result;
  }

  /**
   * Test if a given GeoPoint is valid, i.e. within physical bounds.
   * 
   * @param geoPoint the point to be tested
   * @return true, if it is a physical location on earth.
   */
  public static boolean isValidGeoPoint(GeoPoint geoPoint) {
    return Math.abs(geoPoint.getLatitudeE6()) < 90E6
        && Math.abs(geoPoint.getLongitudeE6()) <= 180E6;
  }

  /**
   * Checks if a given location is a valid (i.e. physically possible) location
   * on Earth. Note: The special separator locations (which have latitude =
   * 100) will not qualify as valid. Neither will locations with lat=0 and lng=0
   * as these are most likely "bad" measurements which often cause trouble.
   * 
   * @param location the location to test
   * @return true if the location is a valid location.
   */
  public static boolean isValidLocation(Location location) {
    return location != null && Math.abs(location.getLatitude()) <= 90
        && Math.abs(location.getLongitude()) <= 180;
  }

  /**
   * Gets a location from a GeoPoint.
   * 
   * @param p a GeoPoint
   * @return the corresponding location
   */
  public static Location getLocation(GeoPoint p) {
    Location result = new Location("");
    result.setLatitude(p.getLatitudeE6() / 1.0E6);
    result.setLongitude(p.getLongitudeE6() / 1.0E6);
    return result;
  }

  public static GeoPoint getGeoPoint(Location location) {
    return new GeoPoint((int) (location.getLatitude() * 1E6),
                        (int) (location.getLongitude() * 1E6));
  }

  /**
   * This is a utility class w/ only static memebers.
   */
  protected MyTracksUtils() {
  }
}
