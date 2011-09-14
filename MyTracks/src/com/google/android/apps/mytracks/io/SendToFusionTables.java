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
package com.google.android.apps.mytracks.io;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.QueryFunction;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.MethodOverrideIntercepter;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.javanet.NetHttpTransport;
import com.google.api.client.util.Strings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * A helper class used to transmit tracks to Google Fusion Tables.
 * A new instance should be used for each upload.
 *
 * @author Leif Hendrik Wilden
 */
public class SendToFusionTables implements Runnable {

  /**
   * Listener invoked when sending to fusion tables completes.
   */
  public interface OnSendCompletedListener {
    void onSendCompleted(String tableId, boolean success, int statusMessage);
  }

  /** The GData service id for Fusion Tables. */
  public static final String SERVICE_ID = "fusiontables";

  /** The path for viewing a map visualization of a table. */
  private static final String FUSIONTABLES_MAP =
      "https://www.google.com/fusiontables/embedviz?" +
      "viz=MAP&q=select+col0,+col1,+col2,+col3+from+%s+&h=false&" +
      "lat=%f&lng=%f&z=%d&t=1&l=col2";

  /** Standard base feed url for Fusion Tables. */
  private static final String FUSIONTABLES_BASE_FEED_URL =
      "https://www.google.com/fusiontables/api/query";

  private static final int MAX_POINTS_PER_UPLOAD = 2048;

  private static final String GDATA_VERSION = "2";

  // This class reports upload status to the user as a completion percentage
  // using a progress bar.  Progress is defined as follows:
  //
  //       0%   Getting track metadata
  //       5%   Creating Fusion Table (GData to FT server)
  //  10%-90%   Uploading the track data to Fusion Tables
  //      95%   Uploading waypoints
  //     100%   Done
  private static final int PROGRESS_INITIALIZATION = 0;
  private static final int PROGRESS_FUSION_TABLE_CREATE = 5;
  private static final int PROGRESS_UPLOAD_DATA_MIN = 10;
  private static final int PROGRESS_UPLOAD_DATA_MAX = 90;
  private static final int PROGRESS_UPLOAD_WAYPOINTS = 95;
  private static final int PROGRESS_COMPLETE = 100;

  private final Activity context;
  private final AuthManager auth;
  private final long trackId;
  private final ProgressIndicator progressIndicator;
  private final OnSendCompletedListener onCompletion;
  private final StringUtils stringUtils;
  private final MyTracksProviderUtils providerUtils;

  // Progress status
  private int totalLocationsRead;
  private int totalLocationsPrepared;
  private int totalLocationsUploaded;
  private int totalLocations;
  private int totalSegmentsUploaded;

  private HttpTransport transport;
  private String tableId;

  private static String MARKER_TYPE_START = "large_green";
  private static String MARKER_TYPE_END = "large_red";
  private static String MARKER_TYPE_WAYPOINT = "large_yellow";

  static {
    // We manually assign the transport to avoid having HttpTransport try to
    // load it via reflection (which breaks due to ProGuard).
    HttpTransport.setLowLevelHttpTransport(new NetHttpTransport());
  }

  public SendToFusionTables(Activity context, AuthManager auth,
      long trackId, ProgressIndicator progressIndicator,
      OnSendCompletedListener onCompletion) {
    this.context = context;
    this.auth = auth;
    this.trackId = trackId;
    this.progressIndicator = progressIndicator;
    this.onCompletion = onCompletion;
    this.stringUtils = new StringUtils(context);
    this.providerUtils = MyTracksProviderUtils.Factory.get(context);

    GoogleHeaders headers = new GoogleHeaders();
    headers.setApplicationName("Google-MyTracks-" + SystemUtils.getMyTracksVersion(context));
    headers.gdataVersion = GDATA_VERSION;

    transport = new HttpTransport();
    MethodOverrideIntercepter.setAsFirstFor(transport);
    transport.defaultHeaders = headers;
  }

  @Override
  public void run() {
    Log.d(Constants.TAG, "Sending to Fusion tables: trackId = " + trackId);
    doUpload();
  }

  public static String getMapVisualizationUrl(Track track) {
    if (track == null || track.getStatistics() == null || track.getTableId() == null) {
      Log.w(TAG, "Unable to get track URL");
      return null;
    }

    // TODO(leifhendrik): Determine correct bounding box and zoom level that will show the entire track.
    TripStatistics stats = track.getStatistics();
    double latE6 = stats.getBottom() + (stats.getTop() - stats.getBottom()) / 2;
    double lonE6 = stats.getLeft() + (stats.getRight() - stats.getLeft()) / 2;
    int z = 15;

    // We explicitly format with Locale.US because we need the latitude and
    // longitude to be formatted in a locale-independent manner.  Specifically,
    // we need the decimal separator to be a period rather than a comma.
    return String.format(Locale.US, FUSIONTABLES_MAP, track.getTableId(),
        latE6 / 1.E6, lonE6 / 1.E6, z);
  }

  private void doUpload() {
    ((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(auth.getAuthToken());
    int statusMessageId = R.string.error_sending_to_fusiontables;
    boolean success = true;
    try {
      progressIndicator.setProgressValue(PROGRESS_INITIALIZATION);
      progressIndicator.setProgressMessage(R.string.progress_message_reading_track);

      // Get the track meta-data
      Track track = providerUtils.getTrack(trackId);
      if (track == null) {
        Log.w(Constants.TAG, "Cannot get track.");
        return;
      }

      String originalDescription = track.getDescription();

      // Create a new table:
      progressIndicator.setProgressValue(PROGRESS_FUSION_TABLE_CREATE);
      progressIndicator.setProgressMessage(R.string.progress_message_creating_fusiontable);
      if (!createNewTable(track) || !makeTableUnlisted()) {
        return;
      }

      progressIndicator.setProgressValue(PROGRESS_UPLOAD_DATA_MIN);
      progressIndicator.setProgressMessage(R.string.progress_message_sending_fusiontables);

      // Upload all of the segments of the track plus start/end markers
      if (!uploadAllTrackPoints(track, originalDescription)) {
        return;
      }

      progressIndicator.setProgressValue(PROGRESS_UPLOAD_WAYPOINTS);

      // Upload all the waypoints.
      if (!uploadWaypoints(track)) {
        return;
      }

      statusMessageId = R.string.status_new_fusiontable_has_been_created;
      Log.d(Constants.TAG, "SendToFusionTables: Done: " + success);
      progressIndicator.setProgressValue(PROGRESS_COMPLETE);
    } finally {
      final boolean finalSuccess = success;
      final int finalStatusMessageId = statusMessageId;
      context.runOnUiThread(new Runnable() {
        public void run() {
          if (onCompletion != null) {
            onCompletion.onSendCompleted(
                tableId, finalSuccess, finalStatusMessageId);
          }
        }
      });
    }
  }

  /**
   * Creates a new table.
   * If successful sets {@link #tableId}.
   *
   * @return true in case of success.
   */
  private boolean createNewTable(Track track) {
    Log.d(Constants.TAG, "Creating a new fusion table.");
    String query = "CREATE TABLE '" + sqlEscape(track.getName()) +
        "' (name:STRING,description:STRING,geometry:LOCATION,marker:STRING)";
    return runUpdate(query);
  }

  private boolean makeTableUnlisted() {
    Log.d(Constants.TAG, "Setting visibility to unlisted.");
    String query = "UPDATE TABLE " + tableId + " SET VISIBILITY = UNLISTED";
    return runUpdate(query);
  }

  /**
   * Formats given values SQL style. Escapes single quotes with a backslash.
   *
   * @param values the values to format
   * @return the values formatted as: ('value1','value2',...,'value_n').
   */
  private static String values(String... values) {
    StringBuilder builder = new StringBuilder("(");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append('\'');
      builder.append(sqlEscape(values[i]));
      builder.append('\'');
    }
    builder.append(')');
    return builder.toString();
  }

  private static String sqlEscape(String value) {
    return value.replaceAll("'", "''");
  }

  /**
   * Creates a new row representing a marker.
   *
   * @param name the marker name
   * @param description the marker description
   * @param the marker location
   * @return true in case of success.
   */
  private boolean createNewPoint(String name, String description, Location location,
      String marker) {
    Log.d(Constants.TAG, "Creating a new row with a point.");
    String query = "INSERT INTO " + tableId + " (name,description,geometry,marker) VALUES "
        + values(name, description, getKmlPoint(location), marker);
    return runUpdate(query);
  }

  /**
   * Creates a new row representing a line segment.
   *
   * @param track the track/segment to draw
   * @return true in case of success.
   */
  private boolean createNewLineString(Track track) {
    Log.d(Constants.TAG, "Creating a new row with a point.");
    String query = "INSERT INTO " + tableId
        + " (name,description,geometry) VALUES "
        + values(track.getName(), track.getDescription(), getKmlLineString(track));
    return runUpdate(query);
  }

  private boolean uploadAllTrackPoints(final Track track, String originalDescription) {

    SharedPreferences preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    boolean metricUnits = true;
    if (preferences != null) {
      metricUnits = preferences.getBoolean(context.getString(R.string.metric_units_key), true);
    }

    Cursor locationsCursor = providerUtils.getLocationsCursor(track.getId(), 0, -1, false);
    try {
      if (locationsCursor == null || !locationsCursor.moveToFirst()) {
        Log.w(Constants.TAG, "Unable to get any points to upload");
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
      Log.d(Constants.TAG,
          "Using elevation sampling factor: " + elevationSamplingFrequency
          + " on " + totalLocations);
      double totalDistance = 0;

      Vector<Double> distances = new Vector<Double>();
      Vector<Double> elevations = new Vector<Double>();
      DoubleBuffer elevationBuffer = new DoubleBuffer(Constants.ELEVATION_SMOOTHING_FACTOR);

      List<Location> locations = new ArrayList<Location>(MAX_POINTS_PER_UPLOAD);
      Location lastLocation = null;
      do {
        if (totalLocationsRead % 100 == 0) {
          updateTrackDataUploadProgress();
        }

        Location loc = providerUtils.createLocation(locationsCursor);
        locations.add(loc);

        if (totalLocationsRead == 0) {
          // Put a marker at the first point of the first valid segment:
          String name = track.getName() + " " + context.getString(R.string.start);
          createNewPoint(name, "", loc, MARKER_TYPE_START);
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
        if (totalLocationsRead % MAX_POINTS_PER_UPLOAD == MAX_POINTS_PER_UPLOAD - 1) {
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
            + stringUtils.generateTrackDescription(track, distances, elevations)
            + "</p>");
        String name = track.getName() + " " + context.getString(R.string.end);
        return createNewPoint(name, track.getDescription(), lastLocation, MARKER_TYPE_END);
      }

      return true;
    } finally {
      if (locationsCursor != null) {
        locationsCursor.close();
      }
    }
  }

  /**
   * Appends the given location to the string in the format:
   * longitude,latitude[,altitude]
   *
   * @param location the location to be added
   * @param builder the string builder to use
   */
  private void appendCoordinate(Location location, StringBuilder builder) {
    builder
        .append(location.getLongitude())
        .append(",")
        .append(location.getLatitude());
    if (location.hasAltitude()) {
      builder.append(",");
      builder.append(location.getAltitude());
    }
  }

  /**
   * Gets a KML Point tag for the given location.
   *
   * @param location The location.
   * @return the kml.
   */
  private String getKmlPoint(Location location) {
    StringBuilder builder = new StringBuilder("<Point><coordinates>");
    appendCoordinate(location, builder);
    builder.append("</coordinates></Point>");
    return builder.toString();
  }

  /**
   * Returns a KML Point tag for the given location.
   *
   * @param location The location.
   * @return the kml.
   */
  private String getKmlLineString(Track track) {
    StringBuilder builder = new StringBuilder("<LineString><coordinates>");
    for (Location location : track.getLocations()) {
      appendCoordinate(location, builder);
      builder.append(' ');
    }
    builder.append("</coordinates></LineString>");
    return builder.toString();
  }

  private boolean prepareAndUploadPoints(Track track, List<Location> locations) {
    updateTrackDataUploadProgress();

    int numLocations = locations.size();
    if (numLocations < 2) {
      Log.d(Constants.TAG, "Not preparing/uploading too few points");
      totalLocationsUploaded += numLocations;
      return true;
    }

    // Prepare/pre-process the points
    ArrayList<Track> splitTracks = prepareLocations(track, locations);

    // Start uploading them
    for (Track splitTrack : splitTracks) {
      if (totalSegmentsUploaded > 1) {
        splitTrack.setName(splitTrack.getName() + " "
            + String.format(
                context.getString(R.string.part), totalSegmentsUploaded));
      }
      totalSegmentsUploaded++;
      Log.d(Constants.TAG,
          "SendToFusionTables: Prepared feature for upload w/ "
          + splitTrack.getLocations().size() + " points.");

      // Transmit tracks via GData feed:
      // -------------------------------
      Log.d(Constants.TAG,
            "SendToFusionTables: Uploading to table " + tableId + " w/ auth " + auth);
      if (!uploadTrackPoints(splitTrack)) {
        Log.e(Constants.TAG, "Uploading failed");
        return false;
      }
    }

    locations.clear();
    totalLocationsUploaded += numLocations;
    updateTrackDataUploadProgress();
    return true;
  }

  /**
   * Prepares a buffer of locations for transmission to google fusion tables.
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
        updateTrackDataUploadProgress();
      }
      if (loc.getLatitude() > 90) {
        startNewTrackSegment = true;
      }

      if (startNewTrackSegment) {
        // Close up the last segment.
        prepareTrackSegment(segment, splitTracks);

        Log.d(Constants.TAG,
            "MyTracksSendToFusionTables: Starting new track segment...");
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
   * Prepares a track segment for sending to google fusion tables.
   * The main steps are:
   *  - correcting end time
   *  - decimating locations
   *  - splitting into smaller tracks.
   *
   * The final track pieces will be put in the array list splitTracks.
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
     * Decimate to 2 meter precision. Fusion tables doesn't like too many
     * points:
     */
    LocationUtils.decimate(segment, 2.0);

    /* If the track still has > 2500 points, split it in pieces: */
    final int maxPoints = 2500;
    if (segment.getLocations().size() > maxPoints) {
      splitTracks.addAll(LocationUtils.split(segment, maxPoints));
    } else if (segment.getLocations().size() >= 2) {
      splitTracks.add(segment);
    }
  }

  private boolean uploadTrackPoints(Track splitTrack) {
    int numLocations = splitTrack.getLocations().size();
    if (numLocations < 2) {
      // Need at least two points for a polyline:
      Log.w(Constants.TAG, "Not uploading too few points");
      return true;
    }
    return createNewLineString(splitTrack);
  }

  /**
   * Uploads all of the waypoints associated with this track to a table.
   *
   * @param track The track to upload waypoints for.
   *
   * @return True on success.
   */
  private boolean uploadWaypoints(final Track track) {
    // TODO: Stream through the waypoints in chunks.
    // I am leaving the number of waypoints very high which should not be a
    // problem because we don't try to load them into objects all at the
    // same time.
    boolean success = true;
    Cursor c = null;
    try {
      c = providerUtils.getWaypointsCursor(
          track.getId(), 0,
          Constants.MAX_LOADED_WAYPOINTS_POINTS);
      if (c != null) {
        if (c.moveToFirst()) {
          // This will skip the 1st waypoint (it carries the stats for the
          // last segment).
          while (c.moveToNext()) {
            Waypoint wpt = providerUtils.createWaypoint(c);
            Log.d(Constants.TAG, "SendToFusionTables: Creating waypoint.");
            success = createNewPoint(wpt.getName(), wpt.getDescription(), wpt.getLocation(),
                MARKER_TYPE_WAYPOINT);
            if (!success) {
              break;
            }
          }
        }
      }
      if (!success) {
        Log.w(Constants.TAG, "SendToFusionTables: upload waypoints failed.");
      }
      return success;
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  private void updateTrackDataUploadProgress() {
    // The percent of the total that represents the completed part of this
    // segment.  We calculate it as an absolute percentage, and then scale it
    // to fit the completion percentage range alloted to track data upload.
    double totalPercentage =
        (totalLocationsRead + totalLocationsPrepared + totalLocationsUploaded)
        / (totalLocations * 3.0);

    double scaledPercentage = totalPercentage
        * (PROGRESS_UPLOAD_DATA_MAX - PROGRESS_UPLOAD_DATA_MIN) + PROGRESS_UPLOAD_DATA_MIN;

    progressIndicator.setProgressValue((int) scaledPercentage);
  }

  /**
   * Runs an update query. Handles authentication.
   *
   * @param query The given SQL like query
   * @return true in case of success
   */
  private boolean runUpdate(final String query) {
    GDataWrapper<HttpTransport> wrapper = new GDataWrapper<HttpTransport>();
    wrapper.setAuthManager(auth);
    wrapper.setRetryOnAuthFailure(true);
    wrapper.setClient(transport);
    Log.d(Constants.TAG, "GData connection prepared: " + this.auth);
    wrapper.runQuery(new QueryFunction<HttpTransport>() {
      @Override
      public void query(HttpTransport client)
          throws IOException, GDataWrapper.ParseException, GDataWrapper.HttpException,
          GDataWrapper.AuthenticationException {
        HttpRequest request = transport.buildPostRequest();
        request.headers.contentType = "application/x-www-form-urlencoded";
        GenericUrl url = new GenericUrl(FUSIONTABLES_BASE_FEED_URL);
        request.url = url;
        InputStreamContent isc = new InputStreamContent();
        String sql = "sql=" + URLEncoder.encode(query, "UTF-8");
        isc.inputStream = new ByteArrayInputStream(Strings.toBytesUtf8(sql));
        request.content = isc;

        Log.d(Constants.TAG, "Running update query " + url.toString() + ": " + sql);
        HttpResponse response;
        try {
          response = request.execute();
        } catch (HttpResponseException e) {
          throw new GDataWrapper.HttpException(e.response.statusCode, e.response.statusMessage);
        }
        boolean success = response.isSuccessStatusCode;
        if (success) {
          byte[] result = new byte[1024];
          int read = response.getContent().read(result);
          String s = new String(result, 0, read, "UTF8");
          String[] lines = s.split(Strings.LINE_SEPARATOR);
          if (lines[0].equals("tableid")) {
            tableId = lines[1];
            Log.d(Constants.TAG, "tableId = " + tableId);
          } else {
            Log.w(Constants.TAG, "Unrecognized response: " + lines[0]);
          }
        } else {
          Log.d(Constants.TAG, "Query failed: " + response.statusMessage + " (" +
              response.statusCode + ")");
          throw new GDataWrapper.HttpException(response.statusCode, response.statusMessage);
        }
      }
    });
    return wrapper.getErrorType() == GDataWrapper.ERROR_NO_ERROR;
  }
}
