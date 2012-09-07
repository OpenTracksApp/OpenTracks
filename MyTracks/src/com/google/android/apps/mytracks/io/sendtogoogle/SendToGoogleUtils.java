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
    Track segment = createNewSegment(
        track, locations.size() > 0 ? locations.get(0).getTime() : -1L);

    for (Location location : locations) {
      /*
       * Latitude is greater than 90 if the location is a pause/resume
       * separator.
       */
      if (location.getLatitude() > 90) {
        endSegment(segment, location.getTime(), splitTracks);
        segment = createNewSegment(track, location.getTime());
      } else {
        segment.addLocation(location);
      }
    }
    endSegment(segment, locations.size() > 0 ? locations.get(locations.size() - 1).getTime() : -1L,
        splitTracks);
    return splitTracks;
  }

  /**
   * Creates a new segment for a track.
   * 
   * @param track the track
   * @param startTime the segment start time
   */
  private static Track createNewSegment(Track track, long startTime) {
    Track segment = new Track();
    segment.setId(track.getId());
    segment.setName(track.getName());
    segment.setDescription("");
    segment.setCategory(track.getCategory());
    TripStatistics segmentTripStatistics = segment.getTripStatistics();
    segmentTripStatistics.setStartTime(startTime);
    return segment;
  }

  /**
   * Ends a segment. Adds to the array of track segments if the segment is
   * valid.
   * 
   * @param segment the segment
   * @param stopTime the stop time
   * @param splitTracks the array of track segments
   */
  @VisibleForTesting
  static boolean endSegment(Track segment, long stopTime, ArrayList<Track> splitTracks) {
    // Make sure the segment has at least 2 points
    if (segment.getLocations().size() < 2) {
      Log.d(TAG, "segment has less than 2 points");
      return false;
    }

    // Set its stop time
    segment.getTripStatistics().setStopTime(stopTime);

    /*
     * Decimate to 2 meter precision. Google Maps and Google Fusion Tables do
     * not like the locations to be too precise.
     */
    LocationUtils.decimate(segment, 2.0);

    splitTracks.add(segment);
    return true;
  }
}
