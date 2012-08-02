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

package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.common.annotations.VisibleForTesting;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Commons utilities for sending a track to Google.
 * 
 * @author Jimmy Shih
 */
public class SendToGoogleUtils {

  private static final String TAG = SendToGoogleUtils.class.getSimpleName();

  private SendToGoogleUtils() {}

  /**
   * Prepares a list of locations to send to Google Maps or Google Fusion
   * Tables. Splits the locations into segments if necessary.
   *
   * @param track the track
   * @param locations the list of locations
   * @return an array of split segments.
   */
  public static ArrayList<Track> prepareLocations(Track track, List<Location> locations) {
    ArrayList<Track> splitTracks = new ArrayList<Track>();

    // Create a new segment
    Track segment = new Track();
    segment.setId(track.getId());
    segment.setName(track.getName());
    segment.setDescription("");
    segment.setCategory(track.getCategory());

    TripStatistics segmentStats = segment.getTripStatistics();
    TripStatistics trackStats = track.getTripStatistics();
    segmentStats.setStartTime(trackStats.getStartTime());
    segmentStats.setStopTime(trackStats.getStopTime());
    boolean startNewTrackSegment = false;
    for (Location loc : locations) {
      // Latitude is greater than 90 if the location is invalid. Do not add to
      // the segment.
      if (loc.getLatitude() > 90) {
        startNewTrackSegment = true;
      }

      if (startNewTrackSegment) {
        // Close the last segment
        prepareTrackSegment(segment, splitTracks);

        startNewTrackSegment = false;
        segment = new Track();
        segment.setId(track.getId());
        segment.setName(track.getName());
        segment.setDescription("");
        segment.setCategory(track.getCategory());
        segmentStats = segment.getTripStatistics();
      }

      if (loc.getLatitude() <= 90) {
        segment.addLocation(loc);

        // For a new segment, sets its start time using the first available
        // location time.
        if (segmentStats.getStartTime() < 0) {
          segmentStats.setStartTime(loc.getTime());
        }
      }
    }

    prepareTrackSegment(segment, splitTracks);

    return splitTracks;
  }

  /**
   * Prepares a track segment for sending to Google Maps or Google Fusion
   * Tables. The main steps are:
   * <ul>
   * <li>make sure the segment has at least 2 points</li>
   * <li>set the segment stop time if necessary</li>
   * <li>decimate locations precision</li>
   * </ul>
   * The prepared track will be added to the splitTracks.
   *
   * @param segment the track segment
   * @param splitTracks an array of track segments
   */
  @VisibleForTesting
  static boolean prepareTrackSegment(Track segment, ArrayList<Track> splitTracks) {
    // Make sure the segment has at least 2 points
    if (segment.getLocations().size() < 2) {
      Log.d(TAG, "segment has less than 2 points");
      return false;
    }

    // For a new segment, sets it stop time
    TripStatistics segmentStats = segment.getTripStatistics();
    if (segmentStats.getStopTime() < 0) {
      Location lastLocation = segment.getLocations().get(segment.getLocations().size() - 1);
      segmentStats.setStopTime(lastLocation.getTime());
    }

    // Decimate to 2 meter precision. Google Maps and Google Fusion Tables do
    // not like the locations to be too precise.
    LocationUtils.decimate(segment, 2.0);

    splitTracks.add(segment);
    return true;
  }
}
