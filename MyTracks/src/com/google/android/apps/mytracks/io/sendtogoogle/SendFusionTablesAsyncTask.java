// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.MethodOverride;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.Strings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * AsyncTask to send a track to Google Fusion Tables.
 *
 * @author jshih@google.com (Jimmy Shih)
 */
public class SendFusionTablesAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String APP_NAME_PREFIX = "Google-MyTracks-";
  private static final String SQL_KEY = "sql=";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String SERVICE_ID = "fusiontables";
  private static final String FUSION_TABLES_BASE_URL =
      "https://www.google.com/fusiontables/api/query";
  private static final int MAX_POINTS_PER_UPLOAD = 2048;
  private static final String GDATA_VERSION = "2";

  private static final int PROGRESS_CREATE_TABLE = 0;
  private static final int PROGRESS_UNLIST_TABLE = 5;
  private static final int PROGRESS_UPLOAD_DATA_MIN = 10;
  private static final int PROGRESS_UPLOAD_DATA_MAX = 90;
  private static final int PROGRESS_UPLOAD_WAYPOINTS = 95;
  private static final int PROGRESS_COMPLETE = 100;

  // See http://support.google.com/fusiontables/bin/answer.py?hl=en&answer=185991
  private static final String MARKER_TYPE_START = "large_green";
  private static final String MARKER_TYPE_END = "large_red";
  private static final String MARKER_TYPE_WAYPOINT = "large_yellow";

  private static final String TAG = SendFusionTablesAsyncTask.class.getSimpleName();
  
  private SendFusionTablesActivity activity;

  private final Context context;
  private final Account account;
  private final long trackId;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private final HttpRequestFactory httpRequestFactory;

  /**
   * True if can retry sending to Google Fusion Tables.
   */
  private boolean canRetry;

  /**
   * True if the AsyncTask has completed.
   */
  private boolean completed;

  /**
   * True if the result is success.
   */
  private boolean success;
  
  // The following variables are for per upload states
  private String authToken;
  private String tableId;
  int currentSegment;

  public SendFusionTablesAsyncTask(
      SendFusionTablesActivity activity, Account account, long trackId) {
    this.activity = activity;
    this.account = account;
    this.trackId = trackId;

    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    HttpTransport transport = ApiFeatures.getInstance().getApiAdapter().getHttpTransport();
    httpRequestFactory = transport.createRequestFactory(new MethodOverride());

    canRetry = true;
    completed = false;
    success = false;
  }

  /**
   * Sets the activity associated with this AyncTask.
   *
   * @param activity the activity.
   */
  public void setActivity(SendFusionTablesActivity activity) {
    this.activity = activity;
    if (completed) {
      activity.onAsyncTaskCompleted(success, tableId);
    }
  }

  @Override
  protected void onPreExecute() {
    activity.showProgressDialog();
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    return doUpload();
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (activity != null) {
      activity.setProgressDialogValue(values[0]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (success) {
      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track != null) {
        track.setTableId(tableId);
        myTracksProviderUtils.updateTrack(track);
      } else {
        Log.d(TAG, "No track");
      }
    }
    if (activity != null) {
      activity.onAsyncTaskCompleted(success, tableId);
    }
  }

  /**
   * Uploads a track to Google Fusion Tables.
   *
   * @return true if success.
   */
  private boolean doUpload() {
    // Reset the per upload states
    authToken = null;
    tableId = null;
    currentSegment = 1;

    try {
      authToken = AccountManager.get(context).blockingGetAuthToken(account, SERVICE_ID, false);
    } catch (OperationCanceledException e) {
      return retryUpload();
    } catch (AuthenticatorException e) {
      return retryUpload();
    } catch (IOException e) {
      return retryUpload();
    }

    Track track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.d(TAG, "Track is null");
      return false;
    }

    // Create a new table
    publishProgress(PROGRESS_CREATE_TABLE);
    if (!createNewTable(track)) {
      // Retry upload in case the auth token is invalid
      return retryUpload();
    }

    // Unlist table
    publishProgress(PROGRESS_UNLIST_TABLE);
    if (!unlistTable()) {
      return false;
    }

    // Upload all the track points plus the start and end markers
    publishProgress(PROGRESS_UPLOAD_DATA_MIN);
    if (!uploadAllTrackPoints(track)) {
      return false;
    }

    // Upload all the waypoints
    publishProgress(PROGRESS_UPLOAD_WAYPOINTS);
    if (!uploadWaypoints()) {
      return false;
    }

    publishProgress(PROGRESS_COMPLETE);
    return true;
  }

  /**
   * Retries upload. Invalidates the authToken. If can retry, invokes
   * {@link SendFusionTablesAsyncTask#doUpload()}. Returns false if cannot
   * retry.
   */
  private boolean retryUpload() {
    if (isCancelled()) {
      return false;
    }

    AccountManager.get(context).invalidateAuthToken(SERVICE_ID, authToken);
    if (canRetry) {
      canRetry = false;
      return doUpload();
    }
    return false;
  }

  /**
   * Creates a new table.
   *
   * @param track the track
   * @return true if success.
   */
  private boolean createNewTable(Track track) {
    String query = "CREATE TABLE '" + SendFusionTablesUtils.escapeSqlString(track.getName())
        + "' (name:STRING,description:STRING,geometry:LOCATION,marker:STRING)";
    return sendQuery(query, true);
  }

  /**
   * Unlists a table.
   *
   * @return true if success.
   */
  private boolean unlistTable() {
    String query = "UPDATE TABLE " + tableId + " SET VISIBILITY = UNLISTED";
    return sendQuery(query, false);
  }

  /**
   * Uploads all the points in a track.
   *
   * @param track the track
   * @return true if success.
   */
  private boolean uploadAllTrackPoints(Track track) {
    Cursor locationsCursor = null;
    try {
      SharedPreferences prefs = context.getSharedPreferences(
          Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
      boolean metricUnits = prefs.getBoolean(context.getString(R.string.metric_units_key), true);

      locationsCursor = myTracksProviderUtils.getLocationsCursor(trackId, 0, -1, false);
      if (locationsCursor == null) {
        Log.d(TAG, "Location cursor is null");
        return false;
      }
      int locationsCount = locationsCursor.getCount();
      List<Location> locations = new ArrayList<Location>(MAX_POINTS_PER_UPLOAD);
      Location lastLocation = null;

      // Limit the number of elevation readings. Ideally we would want around
      // 250.
      int elevationSamplingFrequency = Math.max(1, (int) (locationsCount / 250.0));
      double totalDistance = 0;
      DoubleBuffer elevationBuffer = new DoubleBuffer(Constants.ELEVATION_SMOOTHING_FACTOR);
      Vector<Double> distances = new Vector<Double>();
      Vector<Double> elevations = new Vector<Double>();

      for (int i = 0; i < locationsCount; i++) {
        locationsCursor.moveToPosition(i);

        Location location = myTracksProviderUtils.createLocation(locationsCursor);
        locations.add(location);

        int readCount = i + 1;
        
        if (readCount == 1) {
          // Create a start marker
          String name = context.getString(R.string.marker_label_start, track.getName());
          if (!createNewPoint(name, "", location, MARKER_TYPE_START)) {
            Log.d(TAG, "Unable to create the start marker");
            return false;
          }
        }

        // Add to the distances and elevations vectors
        if (LocationUtils.isValidLocation(location)) {
          // All points go into the smoothing buffer
          elevationBuffer.setNext(metricUnits ? location.getAltitude()
              : location.getAltitude() * UnitConversions.M_TO_FT);
          if (lastLocation != null) {
            totalDistance += (double) lastLocation.distanceTo(location);
          }
          if (readCount % elevationSamplingFrequency == 0) {
            distances.add(totalDistance);
            elevations.add(elevationBuffer.getAverage());
          }
        }

        // Upload periodically
        if (readCount % MAX_POINTS_PER_UPLOAD == MAX_POINTS_PER_UPLOAD) {
          if (!prepareAndUploadPoints(track, locations, false)) {
            Log.d(TAG, "Unable to upload points");
            return false;
          }
          updateProgress(readCount, locationsCount);
          locations.clear();
        }
        lastLocation = location;
      }

      // Do a final upload with the remaining locations
      if (!prepareAndUploadPoints(track, locations, true)) {
        Log.d(TAG, "Unable to upload points");
        return false;
      }

      // Create an end marker
      if (lastLocation != null) {
        StringUtils stringUtils = new StringUtils(context);
        track.setDescription("<p>" + track.getDescription() + "</p><p>"
            + stringUtils.generateTrackDescription(track, distances, elevations) + "</p>");
        String name = context.getString(R.string.marker_label_end, track.getName());
        if (!createNewPoint(name, track.getDescription(), lastLocation, MARKER_TYPE_END)) {
          Log.d(TAG, "Unable to create the end marker");
          return false;
        }
      }

      return true;
    } finally {
      if (locationsCursor != null) {
        locationsCursor.close();
      }
    }
  }

  /**
   * Prepares and uploads a list of locations from a track.
   *
   * @param track the track
   * @param locations the locations from the track
   * @param lastBatch true if it is the last batch of locations
   */
  private boolean prepareAndUploadPoints(Track track, List<Location> locations, boolean lastBatch) {
    int numLocations = locations.size();
    if (numLocations < 2) {
      Log.d(TAG, "Not preparing/uploading, too few points");
      return true;
    }

    // Prepare locations
    ArrayList<Track> splitTracks = prepareLocations(track, locations);

    boolean onlyOneSegment = lastBatch && currentSegment == 1 && splitTracks.size() == 1;
    
    // Upload segments
    for (Track splitTrack : splitTracks) {
      if (!onlyOneSegment) {
        splitTrack.setName(context.getString(
            R.string.send_google_track_part_label, splitTrack.getName(), currentSegment));
      }
      if (!createNewLineString(splitTrack)) {
        Log.d(TAG, "Upload points failed");
        return false;
      }
      currentSegment++;
    }
    return true;
  }

  /**
   * Prepares a list of locations to send to Google Fusion Tables.
   * Splits the locations into segments if necessary.
   *
   * @param track the track
   * @param locations the list of locations
   * @return an array of split segments.
   */
  private ArrayList<Track> prepareLocations(Track track, List<Location> locations) {
    ArrayList<Track> splitTracks = new ArrayList<Track>();

    // Create a new segment
    Track segment = new Track();
    segment.setId(track.getId());
    segment.setName(track.getName());
    segment.setDescription("");
    segment.setCategory(track.getCategory());
    
    TripStatistics segmentStats = segment.getStatistics();
    TripStatistics trackStats = track.getStatistics();
    segmentStats.setStartTime(trackStats.getStartTime());
    segmentStats.setStopTime(trackStats.getStopTime());
    boolean startNewTrackSegment = false;
    for (Location loc : locations) {
      // Latitude is greater than 90 if the location is invalid. Do not add to the
      // segment.
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
        segmentStats = segment.getStatistics();
      }

      if (loc.getLatitude() <= 90) {
        segment.addLocation(loc);
        
        // For a new segment, sets its start time using the first available
        // location time.
        if (segmentStats.getStartTime() < 0) {
          segmentStats.setStartTime(loc.getTime());
        }
        segmentStats.setStopTime(loc.getTime());
      }
    }

    prepareTrackSegment(segment, splitTracks);

    return splitTracks;
  }

  /**
   * Prepares a track segment for sending to Google Fusion Tables. The main
   * steps are:
   * <ul>
   * <li>set the stop time</li>
   * <li>decimate locations precision</li>
   * </ul>
   * The prepared track will be added to the splitTracks.
   *
   * @param segment the track segment
   * @param splitTracks an array of track segments
   */
  private void prepareTrackSegment(Track segment, ArrayList<Track> splitTracks) {
    // For a new segment, sets it stop time
    TripStatistics segmentStats = segment.getStatistics();
    ArrayList<Location> locations = segment.getLocations();
    if (segmentStats.getStopTime() < 0 && locations.size() > 0) {
      Location lastLocation = locations.get(locations.size() - 1);
      segmentStats.setStopTime(lastLocation.getTime());
    }

    // Decimate to 2 meter precision. Fusion Tables doesn't like too many
    // points.
    LocationUtils.decimate(segment, 2.0);
    splitTracks.add(segment);
  }

  /**
   * Uploads all the waypoints.
   *
   * @return true if success.
   */
  private boolean uploadWaypoints() {
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getWaypointsCursor(
          trackId, 0, Constants.MAX_LOADED_WAYPOINTS_POINTS);
      if (cursor != null && cursor.moveToFirst()) {
        // This will skip the first waypoint (it carries the stats for the
        // track).
        while (cursor.moveToNext()) {
          Waypoint wpt = myTracksProviderUtils.createWaypoint(cursor);
          if (!createNewPoint(
              wpt.getName(), wpt.getDescription(), wpt.getLocation(), MARKER_TYPE_WAYPOINT)) {
            Log.d(TAG, "Upload waypoints failed");
            return false;
          }
        }
      }
      return true;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Creates a new row in Google Fusion Tables representing a marker as a
   * point.
   *
   * @param name the marker name
   * @param description the marker description
   * @param location the marker location
   * @param type the marker type
   * @return true if success.
   */
  private boolean createNewPoint(
      String name, String description, Location location, String type) {
    String query = "INSERT INTO " + tableId + " (name,description,geometry,marker) VALUES "
        + SendFusionTablesUtils.formatSqlValues(
            name, description, SendFusionTablesUtils.getKmlPoint(location), type);
    return sendQuery(query, false);
  }

  /**
   * Creates a new row in Google Fusion Tables representing the track as a
   * line segment.
   *
   * @param track the track
   * @return true if success.
   */
  private boolean createNewLineString(Track track) {
    String query = "INSERT INTO " + tableId + " (name,description,geometry) VALUES "
        + SendFusionTablesUtils.formatSqlValues(track.getName(), track.getDescription(),
            SendFusionTablesUtils.getKmlLineString(track.getLocations()));
    return sendQuery(query, false);
  }

  /**
   * Sends a query to Google Fusion Tables.
   *
   * @param query the Fusion Tables SQL query
   * @param setTableId true to set the table id
   * @return true if success.
   */
  private boolean sendQuery(String query, boolean setTableId) {
    Log.d(TAG, "SendQuery: " + query);

    if (isCancelled()) {
      return false;
    }

    GenericUrl url = new GenericUrl(FUSION_TABLES_BASE_URL);
    String sql = SQL_KEY + URLEncoder.encode(query);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(Strings.toBytesUtf8(sql));
    InputStreamContent inputStreamContent = new InputStreamContent(null, inputStream);
    HttpRequest request;
    try {
      request = httpRequestFactory.buildPostRequest(url, inputStreamContent);
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
      return false;
    }

    GoogleHeaders headers = new GoogleHeaders();
    headers.setApplicationName(APP_NAME_PREFIX + SystemUtils.getMyTracksVersion(context));
    headers.gdataVersion = GDATA_VERSION;
    headers.setGoogleLogin(authToken);
    headers.setContentType(CONTENT_TYPE);
    request.setHeaders(headers);

    HttpResponse response;
    try {
      response = request.execute();
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
      return false;
    }
    boolean isSuccess = response.isSuccessStatusCode();
    if (isSuccess) {
      InputStream content;
      try {
        content = response.getContent();
      } catch (IOException e) {
        Log.d(TAG, e.getMessage());
        return false;
      }
      if (setTableId) {
        tableId = SendFusionTablesUtils.getTableId(content);
        if (tableId == null) {
          Log.d(TAG, "tableId is null");
          return false;
        }
      }
    } else {
      Log.d(TAG,
          "sendQuery failed: " + response.getStatusMessage() + ": " + response.getStatusCode());
      return false;
    }
    return true;
  }

  /**
   * Updates the progress based on the number of locations uploaded.
   *
   * @param uploaded the number of uploaded locations
   * @param total the number of total locations
   */
  private void updateProgress(int uploaded, int total) {
    double totalPercentage = uploaded / total;
    double scaledPercentage = totalPercentage
        * (PROGRESS_UPLOAD_DATA_MAX - PROGRESS_UPLOAD_DATA_MIN) + PROGRESS_UPLOAD_DATA_MIN;
    publishProgress((int) scaledPercentage);
  }
}

