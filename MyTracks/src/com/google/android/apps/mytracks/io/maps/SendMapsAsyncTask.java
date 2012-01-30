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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.maps.MapsClient;
import com.google.android.apps.mytracks.io.gdata.maps.MapsConstants;
import com.google.android.apps.mytracks.io.gdata.maps.MapsGDataConverter;
import com.google.android.apps.mytracks.io.gdata.maps.XmlMapsGDataParserFactory;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.parser.ParseException;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParserException;

/**
 * AsyncTask to send a track to Google Maps.
 * <p>
 * IMPORTANT: While this code is Apache-licensed, please notice that usage of
 * the Google Maps servers through this API is only allowed for the My Tracks
 * application. Other applications looking to upload maps data should look into
 * using the Google Fusion Tables API.
 *
 * @author Jimmy Shih
 */
public class SendMapsAsyncTask extends AsyncTask<Void, Integer, Boolean> {
  private static final String START_ICON_URL =
      "http://maps.google.com/mapfiles/ms/micons/green-dot.png";
  private static final String END_ICON_URL =
      "http://maps.google.com/mapfiles/ms/micons/red-dot.png";
  private static final int MAX_POINTS_PER_UPLOAD = 500;

  private static final int PROGRESS_FETCH_MAP_ID = 5;
  private static final int PROGRESS_UPLOAD_DATA_MIN = 10;
  private static final int PROGRESS_UPLOAD_DATA_MAX = 90;
  private static final int PROGRESS_UPLOAD_WAYPOINTS = 95;
  private static final int PROGRESS_COMPLETE = 100;

  private static final String TAG = SendMapsAsyncTask.class.getSimpleName();

  private SendMapsActivity activity;
  private final long trackId;
  private final Account account;
  private final String chooseMapId;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private final GDataClient gDataClient;
  private final MapsClient mapsClient;

  /**
   * True if can retry sending to Google Maps.
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
  private MapsGDataConverter mapsGDataConverter;
  private String authToken;
  private String mapId;
  int currentSegment;

  public SendMapsAsyncTask(
      SendMapsActivity activity, long trackId, Account account, String chooseMapId) {
    this.activity = activity;
    this.trackId = trackId;
    this.account = account;
    this.chooseMapId = chooseMapId;

    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    gDataClient = GDataClientFactory.getGDataClient(context);
    mapsClient = new MapsClient(
        gDataClient, new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));

    canRetry = true;
    completed = false;
    success = false;
  }

  /**
   * Sets the activity associated with this AyncTask.
   *
   * @param activity the activity.
   */
  public void setActivity(SendMapsActivity activity) {
    this.activity = activity;
    if (completed && activity != null) {
      activity.onAsyncTaskCompleted(success);
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
    closeClient();
    success = result;
    completed = true;
    if (success) {
      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track != null) {
        track.setMapId(mapId);
        myTracksProviderUtils.updateTrack(track);
      } else {
        Log.d(TAG, "No track");
      }
    }
    if (activity != null) {
      activity.onAsyncTaskCompleted(success);
    }
  }

  @Override
  protected void onCancelled() {
    closeClient();
  }

  /**
   * Closes the gdata client.
   */
  private void closeClient() {
    if (gDataClient != null) {
      gDataClient.close();
    }
  }

  /**
   * Uploads a track to Google Maps.
   *
   * @return true if success.
   */
  private boolean doUpload() {
    // Reset the per upload states
    mapsGDataConverter = null;
    authToken = null;
    mapId = null;
    currentSegment = 1;

    // Create a maps gdata converter
    try {
      mapsGDataConverter = new MapsGDataConverter();
    } catch (XmlPullParserException e) {
      Log.d(TAG, "Unable to create a maps gdata converter", e);
      return false;
    }
    
    // Get auth token
    try {
      authToken = AccountManager.get(context).blockingGetAuthToken(
          account, MapsConstants.SERVICE_NAME, false);
    } catch (OperationCanceledException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (AuthenticatorException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    }

    // Get the track
    Track track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.d(TAG, "Track is null");
      return false;
    }

    // Fetch the mapId, create a new map if necessary
    publishProgress(PROGRESS_FETCH_MAP_ID);
    if (!fetchSendMapId(track)) {
      Log.d("TAG", "Unable to upload all track points");
      return retryUpload();
    }

    // Upload all the track points plus the start and end markers
    publishProgress(PROGRESS_UPLOAD_DATA_MIN);
    if (!uploadAllTrackPoints(track)) {
      Log.d("TAG", "Unable to upload all track points");
      return retryUpload();
    }

    // Upload all the waypoints
    publishProgress(PROGRESS_UPLOAD_WAYPOINTS);
    if (!uploadWaypoints()) {
      Log.d("TAG", "Unable to upload waypoints");
      return false;
    }

    publishProgress(PROGRESS_COMPLETE);
    return true;
  }

  /**
   * Retries upload. Invalidates the authToken. If can retry, invokes
   * {@link SendMapsAsyncTask#doUpload()}. Returns false if cannot retry.
   */
  private boolean retryUpload() {
    if (isCancelled()) {
      return false;
    }

    AccountManager.get(context).invalidateAuthToken(MapsConstants.SERVICE_NAME, authToken);
    if (canRetry) {
      canRetry = false;
      return doUpload();
    }
    return false;
  }

  /**
   * Fetches the {@link SendMapsAsyncTask#mapId} instance variable for
   * sending a track to Google Maps.
   *
   * @param track the Track
   * @return true if able to fetch the mapId variable.
   */
  private boolean fetchSendMapId(Track track) {
    if (isCancelled()) {
      return false;
    }
  
    if (chooseMapId != null) {
      mapId = chooseMapId;
      return true;
    } else {
      SharedPreferences sharedPreferences = context.getSharedPreferences(
          Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
      boolean mapPublic = sharedPreferences.getBoolean(
          context.getString(R.string.default_map_public_key), true);
      try {
        String description = track.getCategory() + "\n" + track.getDescription() + "\n"
            + StringUtils.getCreatedByMyTracks(context, false);
        mapId = SendMapsUtils.createNewMap(
            track.getName(), description, mapPublic, mapsClient, authToken);
      } catch (ParseException e) {
        Log.d(TAG, "Unable to create a new map", e);
        return false;
      } catch (HttpException e) {
        Log.d(TAG, "Unable to create a new map", e);
        return false;
      } catch (IOException e) {
        Log.d(TAG, "Unable to create a new map", e);
        return false;
      }
      return mapId != null;
    }
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

      // Limit the number of elevation readings to 250.
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
          if (!uploadMarker(context.getString(R.string.marker_label_start, track.getName()), "",
              START_ICON_URL, location)) {
            Log.d(TAG, "Unable to create a start marker");
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
        if (readCount % MAX_POINTS_PER_UPLOAD == 0) {
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
        if (!uploadMarker(context.getString(R.string.marker_label_end, track.getName()),
            track.getDescription(), END_ICON_URL, lastLocation)) {
          Log.d(TAG, "Unable to create an end marker");
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
   * @return true if success.
   */
  private boolean prepareAndUploadPoints(Track track, List<Location> locations, boolean lastBatch) {
    // Prepare locations
    ArrayList<Track> splitTracks = SendToGoogleUtils.prepareLocations(track, locations);

    // Upload segments
    boolean onlyOneSegment = lastBatch && currentSegment == 1 && splitTracks.size() == 1;
    for (Track segment : splitTracks) {
      if (!onlyOneSegment) {
        segment.setName(context.getString(
            R.string.send_google_track_part_label, segment.getName(), currentSegment));
      }
      if (!uploadSegment(segment.getName(), segment.getLocations())) {
        Log.d(TAG, "Unable to upload segment");
        return false;
      }
      currentSegment++;
    }
    return true;
  }

  /**
   * Uploads a marker.
   * 
   * @param title marker title
   * @param description marker description
   * @param iconUrl marker marker icon
   * @param location marker location
   * @return true if success.
   */
  private boolean uploadMarker(
      String title, String description, String iconUrl, Location location) {
    if (isCancelled()) {
      return false;
    }
    try {
      if (!SendMapsUtils.uploadMarker(mapId, title, description, iconUrl, location, mapsClient,
          authToken, mapsGDataConverter)) {
        Log.d(TAG, "Unable to upload marker");
        return false;
      }
    } catch (ParseException e) {
      Log.d(TAG, "Unable to upload marker", e);
      return false;
    } catch (HttpException e) {
      Log.d(TAG, "Unable to upload marker", e);
      return false;
    } catch (IOException e) {
      Log.d(TAG, "Unable to upload marker", e);
      return false;
    }
    return true;
  }

  /**
   * Uploads a segment
   * 
   * @param title segment title
   * @param locations segment locations
   * @return true if success
   */
  private boolean uploadSegment(String title, ArrayList<Location> locations) {
    if (isCancelled()) {
      return false;
    }
    try {
      if (!SendMapsUtils.uploadSegment(
          mapId, title, locations, mapsClient, authToken, mapsGDataConverter)) {
        Log.d(TAG, "Unable to upload track points");
        return false;
      }
    } catch (ParseException e) {
      Log.d(TAG, "Unable to upload track points", e);
      return false;
    } catch (HttpException e) {
      Log.d(TAG, "Unable to upload track points", e);
      return false;
    } catch (IOException e) {
      Log.d(TAG, "Unable to upload track points", e);
      return false;
    }
    return true;
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
          if (isCancelled()) {
            return false;
          }
          Waypoint waypoint = myTracksProviderUtils.createWaypoint(cursor);
          try {
            if (!SendMapsUtils.uploadWaypoint(
                mapId, waypoint, mapsClient, authToken, mapsGDataConverter)) {
              Log.d(TAG, "Unable to upload waypoint");
              return false;
            }
          } catch (ParseException e) {
            Log.d(TAG, "Unable to upload waypoint", e);
            return false;
          } catch (HttpException e) {
            Log.d(TAG, "Unable to upload waypoint", e);
            return false;
          } catch (IOException e) {
            Log.d(TAG, "Unable to upload waypoint", e);
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
