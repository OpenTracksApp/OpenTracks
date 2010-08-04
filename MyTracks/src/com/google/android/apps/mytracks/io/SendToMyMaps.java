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

import com.google.android.apps.mymaps.MyMapsFeature;
import com.google.android.apps.mymaps.MyMapsGDataConverter;
import com.google.android.apps.mymaps.MyMapsGDataWrapper;
import com.google.android.apps.mymaps.MyMapsGDataWrapper.QueryFunction;
import com.google.android.apps.mymaps.MyMapsMapMetadata;
import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackBuffer;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.MyTracksUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.maps.MapsClient;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * A helper class used to transmit tracks to Google MyMaps.
 *
 * @author Leif Hendrik Wilden
 */
public class SendToMyMaps {

  private static final String END_ICON_URL =
      "http://maps.google.com/mapfiles/ms/micons/red-dot.png";
  private static final String START_ICON_URL =
      "http://maps.google.com/mapfiles/ms/micons/green-dot.png";

  private final Activity context;
  private final AuthManager auth;
  private final long trackId;
  private final ProgressIndicator progressIndicator;
  private final StringUtils stringUtils;
  private final MyTracksProviderUtils providerUtils;
  private String mapId;

  private String statusMessage = "";
  private boolean success = true;
  private Runnable onCompletion;

  private HandlerThread handlerThread;
  private Handler handler;

  private String originalDescription;

  private MyMapsGDataWrapper wrapper;
  private MyMapsGDataConverter gdataConverter;

  private Location last = null;

  private Vector<Double> distances;
  private Vector<Double> elevations;
  private DoubleBuffer elevationBuffer;

  /**
   * Prepares a buffer of locations for transmission to google maps.
   *
   * @param track the original track with meta data
   * @param buffer a buffer of locations on the track
   * @return an array of tracks each with a sub section of the points in the
   *         original buffer
   */
  private ArrayList<Track> prepareLocations(Track track,
                                            TrackBuffer buffer) {
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
    for (int i = 0; i < buffer.getLocationsLoaded(); ++i) {
      Location loc = buffer.get(i);
      if (loc.getLatitude() > 90) {
        startNewTrackSegment = true;
      }

      if (startNewTrackSegment) {
        // Close up the last segment.
        prepareTrackSegment(segment, splitTracks);

        Log.d(MyTracksConstants.TAG,
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
    MyTracksUtils.decimate(segment, 2.0);

    /* It the track still has > 500 points, split it in pieces: */
    if (segment.getLocations().size() > 500) {
      splitTracks.addAll(MyTracksUtils.split(segment, 500));
    } else if (segment.getLocations().size() >= 2) {
      splitTracks.add(segment);
    }
  }

  /**
   * Inserts a place mark. Second try if 1st try fails. Will throw exception on
   * 2nd failure.
   */
  private void insertMarker(Context context, MapsClient client,
                            String featureFeed, Track track, Location loc,
                            boolean isStart) throws IOException, Exception {
    Entry entry = gdataConverter.getEntryForFeature(
        buildMyMapsPlacemarkFeature(context, track, loc, isStart));
    Log.d(MyTracksConstants.TAG, "SendToMyMaps: Creating placemark "
        + entry.getTitle());
    try {
      client.createEntry(featureFeed, auth.getAuthToken(), entry);
      Log.d(MyTracksConstants.TAG, "SendToMyMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MyTracksConstants.TAG,
          "SendToMyMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, auth.getAuthToken(), entry);
      Log.d(MyTracksConstants.TAG,
          "SendToMyMaps: createEntry success on 2nd try!");
    }
  }

  private boolean uploadMarker(final Track track,
                               final Location location,
                               final boolean isStart) {
    boolean okay = wrapper.runQuery(new QueryFunction() {
      @Override
      public void query(MapsClient client)
          throws AuthenticationException, IOException, Exception {
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        insertMarker(context, client, featureFeed, track, location, isStart);
      }
    });
    return okay;
  }

  /**
   * Sets the current upload progress.
   *
   * @param track The track being uploaded
   * @param totalLocationsRead The total number of locations already read and
   *        uploaded
   * @param segmentSize The percent of total that each segment corresponds to
   * @param progressInSegment The approximate progress in this segment
   */
  private void setProgress(final Track track, int totalLocationsRead,
                           double segmentSize, double progressInSegment) {
    long totalLocations = track.getStopId() - track.getStartId();

    // The percent of the total that represents the completed part of this
    // segment.
    double percentSegmentInTotal = (segmentSize * progressInSegment);

    int progress = (int) ((100.0 * totalLocationsRead / totalLocations)
        + percentSegmentInTotal);
    progress = Math.min(99, progress);
    progressIndicator.setProgressValue(progress);
  }

  private boolean uploadAllTrackPoints(final Track track) {
    int totalLocationsRead = 0;
    // TODO: This might not actually be accurate.
    long totalLocations = track.getStopId() - track.getStartId();

    // Limit the number of elevation readings. Ideally we would want around 250.
    int elevationSamplingFrequency =
        Math.max(1, (int) (totalLocations / 250.0));
    Log.d(MyTracksConstants.TAG,
          "Using elevation sampling factor: " + elevationSamplingFrequency
          + " on " + totalLocations);
    double totalDistance = 0;
    Location lastLocation = null;

    TrackBuffer buffer = new TrackBuffer((int) Math.min(2048, totalLocations));
    int part = 1;

    SharedPreferences preferences = context.getSharedPreferences(
        MyTracksSettings.SETTINGS_NAME, 0);
    boolean metricUnits = true;
    if (preferences != null) {
      metricUnits = preferences.getBoolean(MyTracksSettings.METRIC_UNITS, true);
    }

    // For the progress monitor:
    // For each track segment 33% = read, 33% = prep & 33% = upload
    // Note: These numbers are very rough.

    // The size of each segment in the 100% progress
    double segmentSize = buffer.getSize() * 100.0 / totalLocations;
    while (success && buffer.getLastLocationRead() < track.getStopId()) {
      setProgress(track, totalLocationsRead, segmentSize, 0.0);
      progressIndicator.setProgressMessage(
          context.getString(R.string.progress_message_reading_track));

      providerUtils.getTrackPoints(track, buffer);
      if (buffer.getLocationsLoaded() <= 0) {
         continue;
      }
      if (totalLocationsRead == 0) {
        // Put a marker at the first point of the first valid segment:
        uploadMarker(track, buffer.get(0), true);
      }
      last = buffer.get(buffer.getLocationsLoaded() - 1);

      // Extract the elevation profile.
      for (int i = 0; i < buffer.getLocationsLoaded(); i++) {
        // Add all points to the smoothing buffer.
        Location l = buffer.get(i);
        if (l != null && MyTracksUtils.isValidLocation(l)) {
          elevationBuffer.setNext(metricUnits ? l.getAltitude()
              : l.getAltitude() * UnitConversions.M_TO_FT);
          if (lastLocation != null) {
            double dist = lastLocation.distanceTo(l);
            totalDistance += dist;
          }
          lastLocation = l;

          // Only add some data points to keep the url short.
          if (i % elevationSamplingFrequency == 0) {
            distances.add(totalDistance);
            elevations.add(elevationBuffer.getAverage());
          }
        }
      }

      setProgress(track, totalLocationsRead, segmentSize, 0.3333);
      progressIndicator.setProgressMessage(
          context.getString(R.string.progress_message_preparing_track));

      ArrayList<Track> splitTracks = prepareLocations(track, buffer);

      int i = 0;
      for (Track t : splitTracks) {
        if (part > 1) {
          t.setName(t.getName() + " "
              + String.format(context.getString(R.string.part), part));
        }
        part++;
        Log.d(MyTracksConstants.TAG,
            "SendToMyMaps: Prepared feature for upload w/ "
            + t.getLocations().size() + " points.");

        //
        // Transmit tracks via GData feed:
        // -------------------------------
        Log.d(MyTracksConstants.TAG,
              "SendToMyMaps: Uploading to map " + mapId + " w/ auth " + auth);
        if (!uploadTrackPoints(track, t,
                               totalLocationsRead, segmentSize)) {
          success = false;
          return false;
        }
        i++;
      }
      totalLocationsRead += buffer.getLocationsLoaded();
    }

    return true;
  }

  /**
   * Uploads a given list of tracks to Google MyMaps using the maps GData feed.
   *
   * @param track the track meta-data is used for the map name
   */
  private boolean uploadTrackPoints(final Track track, final Track currentTrack,
      int totalLocationsRead, double segmentSize) {
    setProgress(track, totalLocationsRead, segmentSize, 0.6666);
    progressIndicator.setProgressMessage(
        context.getString(R.string.progress_message_sending_mymaps));
    return wrapper.runQuery(new QueryFunction() {
      @Override
      public void query(MapsClient client)
          throws AuthenticationException, IOException, Exception {
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        Log.d(MyTracksConstants.TAG, "Feature feed url: " + featureFeed);
        uploadTrackPoints(track, currentTrack, client, featureFeed);
      }
    });
  }

  /**
   * Creates a new map for the given track.
   *
   * @param track The track that will be uploaded to this map
   * @return True on success.
   */
  private boolean createNewMap(final Track track) {
    progressIndicator.setProgressMessage(context
        .getString(R.string.progress_message_creating_map));
    return wrapper.runQuery(new QueryFunction() {
      @Override
      public void query(MapsClient client) throws IOException, Exception {
        Log.d(MyTracksConstants.TAG, "Creating a new map.");
        String mapFeed = MapsClient.getMapsFeed();
        Log.d(MyTracksConstants.TAG, "Map feed is " + mapFeed);
        MyMapsMapMetadata metaData = new MyMapsMapMetadata();
        metaData.setTitle(track.getName());
        metaData.setDescription(originalDescription + " - "
            + track.getCategory() + " - "
            + context.getString(R.string.new_map_description));
        SharedPreferences preferences = context.getSharedPreferences(
            MyTracksSettings.SETTINGS_NAME, 0);
        boolean mapPublic = true;
        if (preferences != null) {
          mapPublic = preferences.getBoolean(
              MyTracksSettings.DEFAULT_MAP_PUBLIC, true);
        }
        metaData.setSearchable(mapPublic);
        Entry entry = MyMapsGDataConverter.getMapEntryForMetadata(metaData);
        Log.d(MyTracksConstants.TAG, "Title: " + entry.getTitle());
        Entry map = client.createEntry(mapFeed, auth.getAuthToken(), entry);
        mapId = MapsClient.getMapIdFromMapEntryId(map.getId());
        Log.d(MyTracksConstants.TAG, "New map id is: " + mapId);
      }
    });
  }

  private boolean uploadTrackPoints(final Track track,
                                    Track splitTrack,
                                    MapsClient client,
                                    String featureFeed)
      throws IOException, Exception  {
    Entry entry = null;
    if (splitTrack.getId() != track.getId()
        || splitTrack.getLocations().size() < 2) {
      // Need at least two points for a polyline:
      return true;
    }

    // Put the line:
    entry = gdataConverter.getEntryForFeature(
        buildMyMapsLineFeature(splitTrack));
    Log.d(MyTracksConstants.TAG,
        "SendToMyMaps: Creating line " + entry.getTitle());
    try {
      client.createEntry(featureFeed, auth.getAuthToken(), entry);
      Log.d(MyTracksConstants.TAG, "SendToMyMaps: createEntry success!");
    } catch (IOException e) {
      Log.w(MyTracksConstants.TAG,
          "SendToMyMaps: createEntry 1st try failed. Trying again.");
      // Retry once (often IOException is thrown on a timeout):
      client.createEntry(featureFeed, auth.getAuthToken(), entry);
      Log.d(MyTracksConstants.TAG,
          "SendToMyMaps: createEntry success on 2nd try!");
    }
    return true;
  }

  /**
   * Uploads all of the waypoints associated with this track to map.
   *
   * @param track The track to upload waypoints for.
   *
   * @return True on success.
   */
  private boolean uploadWaypoints(final Track track) {
    return wrapper.runQuery(new QueryFunction() {
      public void query(MapsClient client) {
        // TODO: Stream through he waypoints in chunks.
        // I am leaving the number of waypoints very high which should not be a
        // problem because we don't try to load them into objects all at the
        // same time.
        Cursor c = null;
        c = providerUtils.getWaypointsCursor(
            track.getId(), 0,
            MyTracksConstants.MAX_LOADED_WAYPOINTS_POINTS);
        String featureFeed = MapsClient.getFeaturesFeed(mapId);
        if (c != null) {
          try {
            if (c.moveToFirst()) {
              // This will skip the 1st waypoint (it carries the stats for the
              // last segment).
              while (c.moveToNext()) {
                Waypoint wpt = providerUtils.createWaypoint(c);
                Entry entry = gdataConverter.getEntryForFeature(
                    buildMyMapsPlacemarkFeature(wpt));
                Log.d(MyTracksConstants.TAG,
                    "SendToMyMaps: Creating waypoint.");
                try {
                  client.createEntry(featureFeed, auth.getAuthToken(), entry);
                  Log.d(MyTracksConstants.TAG,
                      "SendToMyMaps: createEntry success!");
                } catch (IOException e) {
                  Log.w(MyTracksConstants.TAG,
                      "SendToMyMaps: createEntry 1st try failed. Retrying.");

                  // Retry once (often IOException is thrown on a timeout):
                  client.createEntry(featureFeed, auth.getAuthToken(), entry);
                  Log.d(MyTracksConstants.TAG,
                      "SendToMyMaps: createEntry success on 2nd try!");
                }
              }
            }
          } catch (ParseException e) {
            Log.w(MyTracksConstants.TAG, "ParseException caught.", e);
          } catch (HttpException e) {
            Log.w(MyTracksConstants.TAG, "HttpException caught.", e);
          } catch (IOException e) {
            Log.w(MyTracksConstants.TAG, "IOException caught.", e);
          } finally {
            c.close();
          }
        }
      }
    });
  }

  public SendToMyMaps(Activity context, String mapId, AuthManager auth,
      long trackId, ProgressIndicator progressIndicator) {
    this.context = context;
    this.mapId = mapId;
    this.auth = auth;
    this.trackId = trackId;
    this.progressIndicator = progressIndicator;
    this.stringUtils = new StringUtils(context);
    this.providerUtils = MyTracksProviderUtils.Factory.get(context);

    Log.d(MyTracksConstants.TAG, "Sending to MyMap: trackId = " + trackId);
    handlerThread = new HandlerThread("SendToMyMaps");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  public void run() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        doUpload();
      }
    });
  }

  public void setOnCompletion(Runnable onCompletion) {
    this.onCompletion = onCompletion;
  }

  public String getMapId() {
    return mapId;
  }

  public boolean wasSuccess() {
    return success;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  private void doUpload() {
    statusMessage = context.getString(R.string.error_sending_to_mymap);
    success = true;
    try {
      gdataConverter = new MyMapsGDataConverter();

      progressIndicator.setProgressValue(1);
      progressIndicator.setProgressMessage(
          context.getString(R.string.progress_message_reading_track));

      // Get the track meta-data
      Track track = providerUtils.getTrack(trackId);
      originalDescription = track.getDescription();
      track.setDescription("<p>" + track.getDescription() + "</p><p>"
          + stringUtils.generateTrackDescription(track, null, null) + "</p>");
      wrapper = new MyMapsGDataWrapper(context);
      wrapper.setAuthManager(auth);
      wrapper.setRetryOnAuthFailure(true);

      distances = new Vector<Double>();
      elevations = new Vector<Double>();
      elevationBuffer =
          new DoubleBuffer(MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);

      // Create a new map if necessary:
      if (mapId.equals("new")) {
        if (!createNewMap(track)) {
          success = false;
        }
      }

      // Upload all of the segments of the track.
      if (success) {
        if (!uploadAllTrackPoints(track)) {
          success = false;
        }
      }

      // Start end end markers.
      if (success) {
      // Put an end marker at the last point of the last valid segment:
        if (last != null) {
          track.setDescription("<p>" + originalDescription + "</p><p>"
              + stringUtils.generateTrackDescription(
                  track, distances, elevations)
              + "</p>");
          if (!uploadMarker(track, last, false)) {
            success = false;
          }
        }
      }

      // Put waypoints.
      if (success) {
        if (!uploadWaypoints(track)) {
          Log.w(MyTracksConstants.TAG,
              "SendToMyMaps: upload waypoints failed.");
          success = false;
        }
      }

      if (success) {
        if (mapId.equals("new")) {
          statusMessage =
              context.getString(R.string.status_new_mymap_has_been_created);
        } else {
          statusMessage =
              context.getString(R.string.status_tracks_have_been_uploaded);
        }
      }
      Log.d(MyTracksConstants.TAG, "SendToMyMaps: Done: " + success);
      progressIndicator.setProgressValue(100);
    } catch (XmlPullParserException e) {
      Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
    } finally {
      if (wrapper != null) {
        wrapper.cleanUp();
      }

      context.runOnUiThread(new Runnable() {
        public void run() {
          if (onCompletion != null) {
            onCompletion.run();
          }
        }
      });
    }
  }

  /**
   * Builds a MyMapsFeature from a track.
   *
   * @param track the track
   * @return a MyMapsFeature
   */
  private static MyMapsFeature buildMyMapsLineFeature(Track track) {
    MyMapsFeature myMapsFeature = new MyMapsFeature();
    myMapsFeature.generateAndroidId();
    myMapsFeature.setType(MyMapsFeature.LINE);
    if (track.getName().length() < 1) {
      // Features must have a name (otherwise GData upload may fail):
      myMapsFeature.setTitle("-");
    } else {
      myMapsFeature.setTitle(track.getName());
    }
    myMapsFeature.setColor(0x80FF0000);
    for (Location loc : track.getLocations()) {
      myMapsFeature.addPoint(MyTracksUtils.getGeoPoint(loc));
    }
    return myMapsFeature;
  }

  /**
   * Builds a placemark MyMapsFeature from a track.
   *
   * @param track the track
   * @param isStart true if it's the start of the track, or false for end
   * @return a MyMapsFeature
   */
  private static MyMapsFeature buildMyMapsPlacemarkFeature(Context context,
      Track track, Location loc, boolean isStart) {
    MyMapsFeature myMapsFeature = new MyMapsFeature();
    myMapsFeature.generateAndroidId();
    myMapsFeature.setType(MyMapsFeature.MARKER);
    if (isStart) {
      myMapsFeature.setIconUrl(START_ICON_URL);
    } else {
      myMapsFeature.setIconUrl(END_ICON_URL);
    }
    myMapsFeature.addPoint(MyTracksUtils.getGeoPoint(loc));
    String name = track.getName() + " "
        + (isStart ? context.getString(R.string.start)
                   : context.getString(R.string.end));
    myMapsFeature.setTitle(name);
    myMapsFeature.setDescription(isStart ? "" : track.getDescription());
    return myMapsFeature;
  }

  /**
   * Builds a MyMapsFeature from a track.
   *
   * @param wpt the waypoint
   * @return a MyMapsFeature
   */
  private static MyMapsFeature buildMyMapsPlacemarkFeature(Waypoint wpt) {
    MyMapsFeature myMapsFeature = new MyMapsFeature();
    myMapsFeature.generateAndroidId();
    myMapsFeature.setType(MyMapsFeature.MARKER);
    myMapsFeature.setIconUrl(wpt.getIcon());
    myMapsFeature.setDescription(wpt.getDescription());
    myMapsFeature.addPoint(MyTracksUtils.getGeoPoint(wpt.getLocation()));
    if (wpt.getName().length() < 1) {
      // Features must have a name (otherwise GData upload may fail):
      myMapsFeature.setTitle("-");
    } else {
      myMapsFeature.setTitle(wpt.getName());
    }
    myMapsFeature.setDescription(wpt.getDescription().replaceAll("\n", "<br>"));
    return myMapsFeature;
  }
}
