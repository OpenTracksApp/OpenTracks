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
package com.google.android.apps.mytracks.io;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A helper class used to transmit tracks to Google MyMaps.
 * A new instance should be used for each upload.
 *
 * IMPORTANT: while this code is Apache-licensed, please notice that usage of
 * the Google Maps servers through this API is only allowed for the My Tracks
 * application. Other applications looking to upload maps data should look
 * into using the Fusion Tables API.
 *
 * @author Leif Hendrik Wilden
 */
public class SendToMyMaps implements Runnable {

  public static final String NEW_MAP_ID = "new";
  private static final int MAX_POINTS_PER_UPLOAD = 2048;

  private final Activity context;
  private final AuthManager auth;
  private final long trackId;
  private final ProgressIndicator progressIndicator;
  private final OnSendCompletedListener onCompletion;
  private final StringUtils stringUtils;
  private final MyTracksProviderUtils providerUtils;
  private String mapId;

  private MapsFacade mapsClient;

  // Progress status
  private int totalLocationsRead;
  private int totalLocationsPrepared;
  private int totalLocationsUploaded;
  private int totalLocations;
  private int totalSegmentsUploaded;

  public interface OnSendCompletedListener {
    void onSendCompleted(String mapId, boolean success, int statusMessage);
  }

  public SendToMyMaps(Activity context, String mapId, AuthManager auth,
      long trackId, ProgressIndicator progressIndicator,
      OnSendCompletedListener onCompletion) {
    this.context = context;
    this.mapId = mapId;
    this.auth = auth;
    this.trackId = trackId;
    this.progressIndicator = progressIndicator;
    this.onCompletion = onCompletion;
    this.stringUtils = new StringUtils(context);
    this.providerUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  public void run() {
    Log.d(TAG, "Sending to MyMaps: trackId = " + trackId);
    doUpload();
  }

  private void doUpload() {
    int statusMessageId = R.string.error_sending_to_my_maps;
    boolean success = true;
    try {
      progressIndicator.setProgressMessage(
          R.string.progress_message_reading_track);

      // Get the track meta-data
      Track track = providerUtils.getTrack(trackId);
      if (track == null) {
        Log.w(Constants.TAG, "Cannot get track.");
        return;
      }

      String originalDescription = track.getDescription();
      track.setDescription("<p>" + track.getDescription() + "</p><p>"
          + stringUtils.generateTrackDescription(track, null, null) + "</p>");

      mapsClient = new MapsFacade(context, auth);

      // Create a new map if necessary:
      boolean isNewMap = mapId.equals(NEW_MAP_ID);
      if (isNewMap) {
        SharedPreferences preferences = context.getSharedPreferences(
            Constants.SETTINGS_NAME, 0);
        boolean mapPublic = true;
        if (preferences != null) {
          mapPublic = preferences.getBoolean(
              context.getString(R.string.default_map_public_key), true);
        }

        progressIndicator.setProgressMessage(
            R.string.progress_message_creating_map);

        StringBuilder mapIdBuilder = new StringBuilder();
        success = mapsClient.createNewMap(
            track.getName(), track.getCategory(), originalDescription, mapPublic, mapIdBuilder);
        mapId = mapIdBuilder.toString();
      }

      // Upload all of the segments of the track plus start/end markers
      if (success) {
        success = uploadAllTrackPoints(track, originalDescription);
      }

      // Put waypoints.
      if (success) {
        Cursor c = providerUtils.getWaypointsCursor(
            track.getId(), 0,
            Constants.MAX_LOADED_WAYPOINTS_POINTS);
        if (c != null) {
          try {
            if (c.getCount() > 1 && c.moveToFirst()) {
              // This will skip the 1st waypoint (it carries the stats for the
              // last segment).
              ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>(c.getCount() - 1);
              while (c.moveToNext()) {
                Waypoint wpt = providerUtils.createWaypoint(c);
                waypoints.add(wpt);
              }

              success = mapsClient.uploadWaypoints(mapId, waypoints);
            }
          } finally {
            c.close();
          }
        } else {
          success = false;
        }

        if (!success) {
          Log.w(TAG, "SendToMyMaps: upload waypoints failed.");
        }
      }

      if (success) {
        statusMessageId = isNewMap
            ? R.string.sending_to_my_maps_success_new_map
            : R.string.sending_to_my_maps_success_existing_map;
      }
      Log.d(TAG, "SendToMyMaps: Done: " + success);
      progressIndicator.setProgressValue(100);
    } finally {
      if (mapsClient != null) {
        mapsClient.cleanUp();
      }

      final boolean finalSuccess = success;
      final int finalStatusMessageId = statusMessageId;
      context.runOnUiThread(new Runnable() {
        public void run() {
          if (onCompletion != null) {
            onCompletion.onSendCompleted(
                mapId, finalSuccess, finalStatusMessageId);
          }
        }
      });
    }
  }

  private boolean uploadAllTrackPoints(
      final Track track, String originalDescription) {
    SharedPreferences preferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, 0);
    boolean metricUnits = true;
    if (preferences != null) {
      metricUnits =
          preferences.getBoolean(context.getString(R.string.metric_units_key),
              true);
    }

    Cursor locationsCursor =
        providerUtils.getLocationsCursor(track.getId(), 0, -1, false);
    try {
      if (locationsCursor == null || !locationsCursor.moveToFirst()) {
        Log.w(TAG, "Unable to get any points to upload");
        return false;
      }

      totalLocationsRead = 0;
      totalLocationsPrepared = 0;
      totalLocationsUploaded = 0;
      totalLocations = locationsCursor.getCount();
      totalSegmentsUploaded = 0;

      // Limit the number of elevation readings. Ideally we would want around 250.
      int elevationSamplingFrequency =
          Math.max(1, (int) (totalLocations / 250.0));
      Log.d(TAG,
            "Using elevation sampling factor: " + elevationSamplingFrequency
            + " on " + totalLocations);
      double totalDistance = 0;

      Vector<Double> distances = new Vector<Double>();
      Vector<Double> elevations = new Vector<Double>();
      DoubleBuffer elevationBuffer =
          new DoubleBuffer(Constants.ELEVATION_SMOOTHING_FACTOR);

      List<Location> locations = new ArrayList<Location>(MAX_POINTS_PER_UPLOAD);
      progressIndicator.setProgressMessage(
          R.string.progress_message_reading_track);
      Location lastLocation = null;
      do {
        if (totalLocationsRead % 100 == 0) {
          updateProgress();
        }

        Location loc = providerUtils.createLocation(locationsCursor);
        locations.add(loc);

        if (totalLocationsRead == 0) {
          // Put a marker at the first point of the first valid segment:
          mapsClient.uploadMarker(mapId, track.getName(), track.getDescription(), loc, true);
        }

        // Add to the elevation profile.
        if (loc != null && LocationUtils.isValidLocation(loc)) {
          // All points go into the smoothing buffer...
          elevationBuffer.setNext(metricUnits ? loc.getAltitude()
              : loc.getAltitude() * UnitConversions.M_TO_FT);
          if (lastLocation != null) {
            double dist = lastLocation.distanceTo(loc);
            totalDistance += dist;
          }

          // ...but only a few points are really used to keep the url short.
          if (totalLocationsRead % elevationSamplingFrequency == 0) {
            distances.add(totalDistance);
            elevations.add(elevationBuffer.getAverage());
          }
        }

        // If the location was not valid, it's a segment split, so make sure the
        // distance between the previous segment and the new one is not accounted
        // for in the next iteration.
        lastLocation = loc;

        // Every now and then, upload the accumulated points
        if (totalLocationsRead % MAX_POINTS_PER_UPLOAD
            == MAX_POINTS_PER_UPLOAD - 1) {
          if (!prepareAndUploadPoints(track, locations)) {
            return false;
          }
        }

        totalLocationsRead++;
      } while (locationsCursor.moveToNext());

      // Do a final upload with what's left
      if (!prepareAndUploadPoints(track, locations)) {
        return false;
      }

      // Put an end marker at the last point of the last valid segment:
      if (lastLocation != null) {
        track.setDescription("<p>" + originalDescription + "</p><p>"
            + stringUtils.generateTrackDescription(
                track, distances, elevations)
            + "</p>");
        return mapsClient.uploadMarker(mapId, track.getName(), track.getDescription(),
            lastLocation, false);
      }

      return true;
    } finally {
      if (locationsCursor != null) {
        locationsCursor.close();
      }
    }
  }


  private boolean prepareAndUploadPoints(Track track, List<Location> locations) {
    progressIndicator.setProgressMessage(
        R.string.progress_message_preparing_track);
    updateProgress();

    int numLocations = locations.size();
    if (numLocations < 2) {
      Log.d(TAG, "Not preparing/uploading too few points");
      totalLocationsUploaded += numLocations;
      return true;
    }

    // Prepare/pre-process the points
    ArrayList<Track> splitTracks = prepareLocations(track, locations);

    // Start uploading them
    progressIndicator.setProgressMessage(
        R.string.progress_message_sending_my_maps);
    for (Track splitTrack : splitTracks) {
      if (totalSegmentsUploaded > 1) {
        splitTrack.setName(splitTrack.getName() + " "
            + String.format(
                context.getString(R.string.track_part_format), totalSegmentsUploaded));
      }
      totalSegmentsUploaded++;
      Log.d(TAG,
          "SendToMyMaps: Prepared feature for upload w/ "
          + splitTrack.getLocations().size() + " points.");

      // Transmit tracks via GData feed:
      // -------------------------------
      Log.d(TAG,
            "SendToMyMaps: Uploading to map " + mapId + " w/ auth " + auth);
      if (!mapsClient.uploadTrackPoints(mapId, splitTrack.getName(), splitTrack.getLocations())) {
        Log.e(TAG, "Uploading failed");
        return false;
      }
    }

    locations.clear();
    totalLocationsUploaded += numLocations;
    updateProgress();
    return true;
  }

  /**
   * Prepares a buffer of locations for transmission to google maps.
   *
   * @param track the original track with meta data
   * @param buffer a buffer of locations on the track
   * @return an array of tracks each with a sub section of the points in the
   *         original buffer
   */
  private ArrayList<Track> prepareLocations(
      Track track, Iterable<Location> locations) {
    ArrayList<Track> splitTracks = new ArrayList<Track>();

    // Create segments from each full track:
    Track segment = new Track();
    TripStatistics segmentStats = segment.getStatistics();
    TripStatistics trackStats = track.getStatistics();
    segment.setId(track.getId());
    segment.setName(track.getName());
    segment.setDescription(/* track.getDescription() */ "");
    segment.setCategory(track.getCategory());
    segmentStats.setStartTime(trackStats.getStartTime());
    segmentStats.setStopTime(trackStats.getStopTime());
    boolean startNewTrackSegment = false;
    for (Location loc : locations) {
      if (totalLocationsPrepared % 100 == 0) {
        updateProgress();
      }
      if (loc.getLatitude() > 90) {
        startNewTrackSegment = true;
      }

      if (startNewTrackSegment) {
        // Close up the last segment.
        prepareTrackSegment(segment, splitTracks);

        Log.d(TAG,
            "MyTracksSendToMyMaps: Starting new track segment...");
        startNewTrackSegment = false;
        segment = new Track();
        segment.setId(track.getId());
        segment.setName(track.getName());
        segment.setDescription(/* track.getDescription() */ "");
        segment.setCategory(track.getCategory());
      }

      if (loc.getLatitude() <= 90) {
        segment.addLocation(loc);
        if (segmentStats.getStartTime() < 0) {
          segmentStats.setStartTime(loc.getTime());
        }
      }
      totalLocationsPrepared++;
    }

    prepareTrackSegment(segment, splitTracks);

    return splitTracks;
  }

  /**
   * Prepares a track segment for sending to google maps.
   * The main steps are:
   *  - correcting end time
   *  - decimating locations
   *  - splitting into smaller tracks.
   *
   *  The final track pieces will be put in the array list splitTracks.
   *
   * @param segment the original segment of the track
   * @param splitTracks an array of smaller track segments
   */
  private void prepareTrackSegment(
      Track segment, ArrayList<Track> splitTracks) {
    TripStatistics segmentStats = segment.getStatistics();
    if (segmentStats.getStopTime() < 0
        && segment.getLocations().size() > 0) {
      segmentStats.setStopTime(segment.getLocations().size() - 1);
    }

    /*
     * Decimate to 2 meter precision. Mapshop doesn't like too many
     * points:
     */
    LocationUtils.decimate(segment, 2.0);

    /* It the track still has > 500 points, split it in pieces: */
    if (segment.getLocations().size() > 500) {
      splitTracks.addAll(LocationUtils.split(segment, 500));
    } else if (segment.getLocations().size() >= 2) {
      splitTracks.add(segment);
    }
  }

  /**
   * Sets the current upload progress.
   */
  private void updateProgress() {
    // The percent of the total that represents the completed part of this
    // segment.
    int totalPercentage =
        (totalLocationsRead + totalLocationsPrepared + totalLocationsUploaded) * 100
        / (totalLocations * 3);
    totalPercentage = Math.min(99, totalPercentage);
    progressIndicator.setProgressValue(totalPercentage);
  }
}
