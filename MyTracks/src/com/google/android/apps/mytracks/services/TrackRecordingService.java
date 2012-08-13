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

package com.google.android.apps.mytracks.services;

import static com.google.android.apps.mytracks.Constants.RESUME_TRACK_EXTRA_NAME;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.content.DescriptionGeneratorImpl;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProvider;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.content.WaypointCreationRequest.WaypointType;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.services.sensors.SensorManagerFactory;
import com.google.android.apps.mytracks.services.tasks.AnnouncementPeriodicTaskFactory;
import com.google.android.apps.mytracks.services.tasks.PeriodicTaskExecutor;
import com.google.android.apps.mytracks.services.tasks.SplitPeriodicTaskFactory;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackNameUtils;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A background service that registers a location listener and records track
 * points. Track points are saved to the {@link MyTracksProvider}.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service {

  private static final String TAG = TrackRecordingService.class.getSimpleName();
  // One second in milliseconds
  private static final long ONE_SECOND = 1000;
  // One minute in milliseconds
  private static final long ONE_MINUTE = 60 * ONE_SECOND;
  @VisibleForTesting
  static final int MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS = 3;

  // The following variables are set in onCreate:
  private Context context;
  private MyTracksProviderUtils myTracksProviderUtils;
  private LocationManager locationManager;
  private PeriodicTaskExecutor announcementExecutor;
  private PeriodicTaskExecutor splitExecutor;
  private ExecutorService executorService;
  private SharedPreferences sharedPreferences;
  private long recordingTrackId;
  private LocationListenerPolicy locationListenerPolicy;
  private int minRecordingDistance;
  private int maxRecordingDistance;
  private int minRequiredAccuracy;
  private int autoResumeTrackTimeout;
  private long currentRecordingInterval;

  // The following variables are set when recording:
  private TripStatisticsBuilder trackTripStatisticsBuilder;
  private TripStatisticsBuilder markerTripStatisticsBuilder;
  private WakeLock wakeLock;
  private SensorManager sensorManager;

  // Timer to periodically invoke checkLocationListener
  private final Timer timer = new Timer();

  // Handler for the timer to post a runnable to the main thread
  private final Handler handler = new Handler();

  private ServiceBinder binder = new ServiceBinder(this);

  /*
   * Note that sharedPreferenceChangeListener cannot be an anonymous inner
   * class. Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (key == null
              || PreferencesUtils.getKey(context, R.string.recording_track_id_key).equals(key)) {
            long id = PreferencesUtils.getLong(context, R.string.recording_track_id_key);
            /*
             * Only the TrackRecordingService can stop a recording and set the
             * recordingTrackId to -1L.
             */
            if (id != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
              recordingTrackId = id;
            }
          }
          if (key == null
              || PreferencesUtils.getKey(context, R.string.metric_units_key).equals(key)) {
            boolean metricUnits = PreferencesUtils.getBoolean(
                context, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
            announcementExecutor.setMetricUnits(metricUnits);
            splitExecutor.setMetricUnits(metricUnits);
          }
          if (key == null || PreferencesUtils.getKey(context, R.string.announcement_frequency_key)
              .equals(key)) {
            announcementExecutor.setTaskFrequency(PreferencesUtils.getInt(
                context, R.string.announcement_frequency_key,
                PreferencesUtils.ANNOUNCEMENT_FREQUENCY_DEFAULT));
          }
          if (key == null
              || PreferencesUtils.getKey(context, R.string.split_frequency_key).equals(key)) {
            splitExecutor.setTaskFrequency(PreferencesUtils.getInt(
                context, R.string.split_frequency_key, PreferencesUtils.SPLIT_FREQUENCY_DEFAULT));
          }
          if (key == null || PreferencesUtils.getKey(context, R.string.min_recording_interval_key)
              .equals(key)) {
            int minRecordingInterval = PreferencesUtils.getInt(context,
                R.string.min_recording_interval_key,
                PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT);
            switch (minRecordingInterval) {
              case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE:
                // Choose battery life over moving time accuracy.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(
                    30 * ONE_SECOND, 5 * ONE_MINUTE, 5);
                break;
              case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_ACCURACY:
                // Get all the updates.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(
                    ONE_SECOND, 30 * ONE_SECOND, 0);
                break;
              default:
                locationListenerPolicy = new AbsoluteLocationListenerPolicy(
                    minRecordingInterval * ONE_SECOND);
            }
          }
          if (key == null || PreferencesUtils.getKey(context, R.string.min_recording_distance_key)
              .equals(key)) {
            minRecordingDistance = PreferencesUtils.getInt(context,
                R.string.min_recording_distance_key,
                PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
          }
          if (key == null || PreferencesUtils.getKey(context, R.string.max_recording_distance_key)
              .equals(key)) {
            maxRecordingDistance = PreferencesUtils.getInt(context,
                R.string.max_recording_distance_key,
                PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT);
          }
          if (key == null
              || PreferencesUtils.getKey(context, R.string.min_required_accuracy_key).equals(key)) {
            minRequiredAccuracy = PreferencesUtils.getInt(context,
                R.string.min_required_accuracy_key, PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT);
          }
          if (key == null || PreferencesUtils.getKey(
              context, R.string.auto_resume_track_timeout_key).equals(key)) {
            autoResumeTrackTimeout = PreferencesUtils.getInt(context,
                R.string.auto_resume_track_timeout_key,
                PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);
          }
        }
      };

  private LocationListener locationListener = new LocationListener() {
      @Override
    public void onProviderDisabled(String provider) {
      // Do nothing
    }

      @Override
    public void onProviderEnabled(String provider) {
      // Do nothing
    }

      @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      // Do nothing
    }

      @Override
    public void onLocationChanged(final Location location) {
      if (executorService.isShutdown() || executorService.isTerminated()) {
        return;
      }
      executorService.submit(new Runnable() {
          @Override
        public void run() {
          onLocationChangedAsync(location);
        }
      });
    }
  };

  private TimerTask checkLocationListener = new TimerTask() {
      @Override
    public void run() {
      if (isRecording()) {
        handler.post(new Runnable() {
          public void run() {
            registerLocationListener();
          }
        });
      }
    }
  };

  /*
   * Note that this service, through the AndroidManifest.xml, is configured to
   * allow both MyTracks and third party apps to invoke it. For the onCreate
   * callback, we cannot tell whether the caller is MyTracks or a third party
   * app, thus it cannot start/stop a recording or write/update MyTracks
   * database.
   */
  @Override
  public void onCreate() {
    super.onCreate();
    context = this;
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    announcementExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTaskFactory());
    splitExecutor = new PeriodicTaskExecutor(this, new SplitPeriodicTaskFactory());
    executorService = Executors.newSingleThreadExecutor();
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    // onSharedPreferenceChanged might not set recordingTrackId.
    recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

    // Require announcementExecutor and splitExecutor to be created.
    sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

    // Require locationManager and all preferences set.
    registerLocationListener();

    timer.schedule(checkLocationListener, 5 * ONE_MINUTE, ONE_MINUTE);

    /*
     * Try to restart the previous recording track in case the service has been
     * restarted by the system, which can sometimes happen.
     */
    Track track = myTracksProviderUtils.getTrack(recordingTrackId);
    if (track != null) {
      restartTrack(track);
    } else {
      if (recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
        Log.w(TAG, "recordingTrackId not -1L, but recordingTrack is null. " + recordingTrackId);
        recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        PreferencesUtils.setLong(this, R.string.recording_track_id_key, recordingTrackId);
      }
      showNotification();
    }
  }

  /*
   * Note that this service, through the AndroidManifest.xml, is configured to
   * allow both MyTracks and third party apps to invoke it. For the onStart
   * callback, we cannot tell whether the caller is MyTracks or a third party
   * app, thus it cannot start/stop a recording or write/update MyTracks
   * database.
   */
  @Override
  public void onStart(Intent intent, int startId) {
    handleStartCommand(intent, startId);
  }

  /*
   * Note that this service, through the AndroidManifest.xml, is configured to
   * allow both MyTracks and third party apps to invoke it. For the
   * onStartCommand callback, we cannot tell whether the caller is MyTracks or a
   * third party app, thus it cannot start/stop a recording or write/update
   * MyTracks database.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    handleStartCommand(intent, startId);
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onDestroy() {
    showNotification();

    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    checkLocationListener.cancel();
    checkLocationListener = null;
    timer.cancel();
    timer.purge();
    unregisterLocationListener();

    try {
      announcementExecutor.shutdown();
    } finally {
      announcementExecutor = null;
    }

    try {
      splitExecutor.shutdown();
    } finally {
      splitExecutor = null;
    }

    if (sensorManager != null) {
      SensorManagerFactory.releaseSystemSensorManager();
      sensorManager = null;
    }

    // Make sure we have no indirect references to this service.
    myTracksProviderUtils = null;
    locationManager = null;
    binder.detachFromService();
    binder = null;

    // This should be the next to last operation
    releaseWakeLock();

    /*
     * Shutdown the executor service last to avoid sending events to a dead
     * executor.
     */
    executorService.shutdown();
    super.onDestroy();
  }

  /**
   * Returns true if the service is recording.
   */
  public boolean isRecording() {
    return recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  }

  /**
   * Gets the trip statistics.
   */
  public TripStatistics getTripStatistics() {
    if (trackTripStatisticsBuilder == null) {
      return null;
    }
    return trackTripStatisticsBuilder.getTripStatistics();
  }

  /**
   * Inserts a waypoint.
   * 
   * @param waypointCreationRequest the waypoint creation request
   * @return the waypoint id
   */
  public long insertWaypoint(WaypointCreationRequest waypointCreationRequest) {
    if (!isRecording()) {
      throw new IllegalStateException("Cannot insert marker when not recording!");
    }
    boolean isStatistics = waypointCreationRequest.getType() == WaypointType.STATISTICS;

    String name;
    if (waypointCreationRequest.getName() != null) {
      name = waypointCreationRequest.getName();
    } else {
      int nextMarkerNumber = myTracksProviderUtils.getNextMarkerNumber(
          recordingTrackId, isStatistics);
      if (nextMarkerNumber == -1) {
        nextMarkerNumber = 0;
      }
      name = getString(
          isStatistics ? R.string.marker_split_name_format : R.string.marker_name_format,
          nextMarkerNumber);
    }

    TripStatistics tripStatistics;
    String description;
    if (isStatistics) {
      long now = System.currentTimeMillis();
      markerTripStatisticsBuilder.pauseAt(now);
      tripStatistics = markerTripStatisticsBuilder.getTripStatistics();
      markerTripStatisticsBuilder = new TripStatisticsBuilder(now);
      description = new DescriptionGeneratorImpl(this).generateWaypointDescription(tripStatistics);
    } else {
      tripStatistics = null;
      description = waypointCreationRequest.getDescription() != null ? waypointCreationRequest
          .getDescription()
          : "";
    }

    String category = waypointCreationRequest.getCategory() != null ? waypointCreationRequest
        .getCategory()
        : "";
    String icon = getString(
        isStatistics ? R.string.marker_statistics_icon_url : R.string.marker_waypoint_icon_url);
    int type = isStatistics ? Waypoint.TYPE_STATISTICS : Waypoint.TYPE_WAYPOINT;

    double length;
    long duration;
    Location location = myTracksProviderUtils.getLastTrackLocation(recordingTrackId);
    if (location != null && trackTripStatisticsBuilder != null
        && trackTripStatisticsBuilder.getTripStatistics() != null) {
      TripStatistics trackTripStatistics = trackTripStatisticsBuilder.getTripStatistics();
      length = trackTripStatistics.getTotalDistance();
      duration = location.getTime() - trackTripStatistics.getStartTime();
    } else {
      if (!waypointCreationRequest.isTrackStatistics()) {
        return -1L;
      }
      // For track statistics, make it an impossible location
      location = new Location("");
      location.setLatitude(100);
      location.setLongitude(180);
      length = 0;
      duration = 0;
    }
    Waypoint waypoint = new Waypoint(name, description, category, icon, recordingTrackId, type,
        length, duration, -1L, -1L, location, tripStatistics);
    Uri uri = myTracksProviderUtils.insertWaypoint(waypoint);
    return Long.parseLong(uri.getLastPathSegment());
  }

  /**
   * Starts the service as a foreground service.
   * 
   * @param notification the notification for the foreground service
   */
  @VisibleForTesting
  protected void startForegroundService(Notification notification) {
    startForeground(1, notification);
  }

  /**
   * Stops the service as a foreground service.
   */
  @VisibleForTesting
  protected void stopForegroundService() {
    stopForeground(true);
  }

  /**
   * Handles start command.
   * 
   * @param intent the intent
   * @param startId the start id
   */
  private void handleStartCommand(Intent intent, int startId) {
    // Check if the service is called to resume track (from phone reboot)
    if (intent != null && intent.getBooleanExtra(RESUME_TRACK_EXTRA_NAME, false)) {
      if (!shouldResumeTrack(myTracksProviderUtils.getTrack(recordingTrackId))) {
        Log.i(TAG, "Stop resume track.");
        stopRecording();
        stopSelfResult(startId);
        return;
      }
    }
  }

  /**
   * Returns true if should resume.
   * 
   * @param track the track
   */
  private boolean shouldResumeTrack(Track track) {
    if (track == null) {
      Log.d(TAG, "Not resuming. Track is null.");
      return false;
    }
    int retries = PreferencesUtils.getInt(this, R.string.auto_resume_track_current_retry_key,
        PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT);
    if (retries >= MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS) {
      Log.d(TAG, "Not resuming. Exceeded maximum retry attempts.");
      return false;
    }
    PreferencesUtils.setInt(this, R.string.auto_resume_track_current_retry_key, retries + 1);

    if (autoResumeTrackTimeout == PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_NEVER) {
      Log.d(TAG, "Not resuming. Auto-resume track timeout set to never.");
      return false;
    } else if (autoResumeTrackTimeout == PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_ALWAYS) {
      Log.d(TAG, "Resuming. Auto-resume track timeout set to always.");
      return true;
    }

    if (track.getTripStatistics() == null) {
      Log.d(TAG, "Not resuming. No trip statistics.");
      return false;
    }
    long stopTime = track.getTripStatistics().getStopTime();
    return stopTime > 0
        && (System.currentTimeMillis() - stopTime) <= autoResumeTrackTimeout * ONE_MINUTE;
  }

  /**
   * Starts a new track.
   * 
   * @return the track id
   */
  private long startNewTrack() {
    if (isRecording()) {
      Log.d(TAG, "Ignore startNewTrack. Already recording.");
      return -1L;
    }
    long now = System.currentTimeMillis();
    trackTripStatisticsBuilder = new TripStatisticsBuilder(now);
    markerTripStatisticsBuilder = new TripStatisticsBuilder(now);

    Track track = new Track();
    track.setStartId(-1L);
    TripStatistics tripStatistics = track.getTripStatistics();
    tripStatistics.setStartTime(now);
    Uri uri = myTracksProviderUtils.insertTrack(track);

    recordingTrackId = Long.parseLong(uri.getLastPathSegment());
    track.setId(recordingTrackId);
    track.setName(TrackNameUtils.getTrackName(this, recordingTrackId, now, null));
    track.setCategory(PreferencesUtils.getString(
        this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT));
    myTracksProviderUtils.updateTrack(track);

    insertWaypoint(WaypointCreationRequest.DEFAULT_START_TRACK);
    PreferencesUtils.setLong(this, R.string.recording_track_id_key, recordingTrackId);
    PreferencesUtils.setInt(this, R.string.auto_resume_track_current_retry_key, 0);

    registerLocationListener();
    startRecording();
    return recordingTrackId;
  }

  /**
   * Restart a track.
   * 
   * @param track the track
   */
  private void restartTrack(Track track) {
    Log.d(TAG, "Restarting track: " + track.getId());

    TripStatistics tripStatistics = track.getTripStatistics();
    trackTripStatisticsBuilder = new TripStatisticsBuilder(tripStatistics.getStartTime());

    long markerStartTime;
    Waypoint waypoint = myTracksProviderUtils.getLastStatisticsWaypoint(recordingTrackId);
    if (waypoint != null && waypoint.getTripStatistics() != null) {
      markerStartTime = waypoint.getTripStatistics().getStopTime();
    } else {
      markerStartTime = tripStatistics.getStartTime();
    }
    markerTripStatisticsBuilder = new TripStatisticsBuilder(markerStartTime);

    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getLocationsCursor(
          recordingTrackId, -1, Constants.MAX_LOADED_TRACK_POINTS, true);
      if (cursor == null) {
        Log.e(TAG, "Cursor is null.");
      } else {
        if (cursor.moveToLast()) {
          Location lastValidTripLocation = null;
          Location lastValidMarkerLocation = null;
          do {
            Location location = myTracksProviderUtils.createLocation(cursor);
            if (LocationUtils.isValidLocation(location)) {
              trackTripStatisticsBuilder.addLocation(
                  location, lastValidTripLocation, location.getTime(), false, minRecordingDistance);
              lastValidTripLocation = location;
              if (location.getTime() > markerStartTime) {
                markerTripStatisticsBuilder.addLocation(
                    location, lastValidMarkerLocation, location.getTime(), false, minRecordingDistance);
                lastValidMarkerLocation = location;
              }
            }
          } while (cursor.moveToPrevious());
        }
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "RuntimeException", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    startRecording();
  }

  /**
   * Common code for starting a recording, new track or restart track.
   */
  private void startRecording() {
    acquireWakeLock();
    sensorManager = SensorManagerFactory.getSystemSensorManager(this);
    showNotification();
    sendTrackBroadcast(R.string.track_started_broadcast_action, recordingTrackId);
    announcementExecutor.restore();
    splitExecutor.restore();
  }

  /**
   * Ends the current track.
   */
  private void endCurrentTrack() {
    if (!isRecording()) {
      Log.d(TAG, "Ignore endCurrentTrack. Not recording.");
      return;
    }
    announcementExecutor.shutdown();
    splitExecutor.shutdown();

    // Need to remember the trackId before calling stopRecording
    long trackId = recordingTrackId;
    stopRecording();

    Track track = myTracksProviderUtils.getTrack(recordingTrackId);
    if (track != null) {
      updateTrack(track, myTracksProviderUtils.getLastTrackLocationId(recordingTrackId), false);
    }

    if (sensorManager != null) {
      SensorManagerFactory.releaseSystemSensorManager();
      sensorManager = null;
    }

    releaseWakeLock();
    showNotification();
    sendTrackBroadcast(R.string.track_stopped_broadcast_action, trackId);
    stopSelf();
  }

  /**
   * Common code for stopping a recording.
   */
  private void stopRecording() {
    recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    PreferencesUtils.setLong(this, R.string.recording_track_id_key, recordingTrackId);
  }

  /**
   * Update track.
   * 
   * @param track the track
   * @param lastTrackPointId the last track point id
   * @param isLastTrackPointNew true if the last track point id is new
   */
  private void updateTrack(Track track, long lastTrackPointId, boolean isLastTrackPointNew) {
    if (lastTrackPointId >= 0) {
      if (track.getStartId() < 0) {
        track.setStartId(lastTrackPointId);
      }
      track.setStopId(lastTrackPointId);
      if (isLastTrackPointNew) {
        track.setNumberOfPoints(track.getNumberOfPoints() + 1);
      }
    }
    if (!isLastTrackPointNew) {
      trackTripStatisticsBuilder.pauseAt(System.currentTimeMillis());
    }
    track.setTripStatistics(trackTripStatisticsBuilder.getTripStatistics());
    myTracksProviderUtils.updateTrack(track);
  }

  /**
   * Called when location changed.
   * 
   * @param location the location
   */
  private void onLocationChangedAsync(Location location) {
    try {
      if (!isRecording()) {
        Log.w(TAG, "Ignore onLocationChangedAsync. Not recording.");
        return;
      }

      if (!LocationUtils.isValidLocation(location)) {
        Log.w(TAG, "Ignore onLocationChangedAsync. Not valid location.");
        return;
      }

      if (location.getAccuracy() > minRequiredAccuracy) {
        Log.d(TAG, "Ignore onLocationChangedAsync. Poor accuracy.");
        return;
      }

      Track track = myTracksProviderUtils.getTrack(recordingTrackId);
      if (track == null) {
        Log.d(TAG, "Ignore onLocationChangedAsync. recodingTrack is null.");
        return;
      }

      Location lastLocation = myTracksProviderUtils.getLastTrackLocation(recordingTrackId);
      if (!LocationUtils.isValidLocation(lastLocation)) {
        lastLocation = null;
      }

      // Update location listener policy
      locationListenerPolicy.updateIdleTime(
          lastLocation != null ? location.getTime() - lastLocation.getTime() : 0L);
      if (currentRecordingInterval != locationListenerPolicy.getDesiredPollingInterval()) {
        registerLocationListener();
      }

      boolean hasSensorData = sensorManager != null && sensorManager.isEnabled()
          && sensorManager.getSensorDataSet() != null && sensorManager.isSensorDataSetValid();
      long now = System.currentTimeMillis();
      markerTripStatisticsBuilder.addLocation(location, lastLocation, now, hasSensorData, minRecordingDistance);
      if (trackTripStatisticsBuilder.addLocation(
          location, lastLocation, now, hasSensorData, minRecordingDistance)) {
        try {
          if (lastLocation != null && location.distanceTo(lastLocation)
              > maxRecordingDistance) {
            Log.d(TAG, "Inserting a separator track point.");
            Location separator = new Location(LocationManager.GPS_PROVIDER);
            separator.setLongitude(0);
            separator.setLatitude(100);
            separator.setTime(lastLocation.getTime());
            myTracksProviderUtils.insertTrackPoint(separator, recordingTrackId);
          }
  
          if (hasSensorData) {
            location = new MyTracksLocation(location, sensorManager.getSensorDataSet());
          }
          Uri uri = myTracksProviderUtils.insertTrackPoint(location, recordingTrackId);
          int trackPointId = Integer.parseInt(uri.getLastPathSegment());
  
          updateTrack(track, trackPointId, true);
        } catch (SQLiteException e) {
          /*
           * Insert failed, most likely because of SqlLite error code 5
           * (SQLite_BUSY). This is expected to happen extremely rarely (if our
           * listener gets invoked twice at about the same time).
           */
          Log.w(TAG, "SQLiteException", e);
          return;
        }
        announcementExecutor.update();
        splitExecutor.update();
      }
    } catch (Error e) {
      Log.e(TAG, "Error in onLocationChangedAsync", e);
      throw e;
    } catch (RuntimeException e) {
      Log.e(TAG, "RuntimeException in onLocationChangedAsync", e);
      throw e;
    }
  }

  /**
   * Registers the location listener.
   */
  private void registerLocationListener() {
    unregisterLocationListener();
    try {
      long interval = locationListenerPolicy.getDesiredPollingInterval();
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval,
          locationListenerPolicy.getMinDistance(), locationListener);
      currentRecordingInterval = interval;
    } catch (RuntimeException e) {
      Log.e(TAG, "Could not register location listener.", e);
    }
  }

  /**
   * Unregisters the location manager.
   */
  private void unregisterLocationListener() {
    if (locationManager == null) {
      Log.e(TAG, "locationManager is null.");
      return;
    }
    locationManager.removeUpdates(locationListener);
  }

  /**
   * Acquires the wake lock.
   */
  private void acquireWakeLock() {
    try {
      PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
      if (powerManager == null) {
        Log.e(TAG, "powerManager is null.");
        return;
      }
      if (wakeLock == null) {
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        if (wakeLock == null) {
          Log.e(TAG, "wakeLock is null.");
          return;
        }
      }
      if (!wakeLock.isHeld()) {
        wakeLock.acquire();
        if (!wakeLock.isHeld()) {
          Log.e(TAG, "Unable to hold wakeLock.");
        }
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "Caught unexpected exception", e);
    }
  }

  /**
   * Releases the wake lock.
   */
  private void releaseWakeLock() {
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      wakeLock = null;
    }
  }

  /**
   * Shows the notification.
   */
  private void showNotification() {
    if (isRecording()) {
      Intent intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
          .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, recordingTrackId);
      TaskStackBuilder taskStackBuilder = TaskStackBuilder.from(this);
      taskStackBuilder.addNextIntent(intent);

      NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentIntent(
          taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
          .setContentText(getString(R.string.track_record_notification))
          .setContentTitle(getString(R.string.my_tracks_app_name)).setOngoing(true)
          .setSmallIcon(R.drawable.my_tracks_notification_icon).setWhen(System.currentTimeMillis());
      startForegroundService(builder.getNotification());
    } else {
      stopForegroundService();
    }
  }

  /**
   * Sends track broadcast.
   * 
   * @param actionId the intent action id
   * @param trackId the track id
   */
  private void sendTrackBroadcast(int actionId, long trackId) {
    Intent intent = new Intent().setAction(getString(actionId))
        .putExtra(getString(R.string.track_id_broadcast_extra), trackId);
    sendBroadcast(intent, getString(R.string.permission_notification_value));
    if (PreferencesUtils.getBoolean(
        this, R.string.allow_access_key, PreferencesUtils.ALLOW_ACCESS_DEFAULT)) {
      sendBroadcast(intent, getString(R.string.broadcast_notifications_permission));
    }
  }

  /**
   * TODO: There is a bug in Android that leaks Binder instances. This bug is
   * especially visible if we have a non-static class, as there is no way to
   * nullify reference to the outer class (the service). A workaround is to use
   * a static class and explicitly clear service and detach it from the
   * underlying Binder. With this approach, we minimize the leak to 24 bytes per
   * each service instance. For more details, see the following bug:
   * http://code.google.com/p/android/issues/detail?id=6426.
   */
  private static class ServiceBinder extends ITrackRecordingService.Stub {
    private TrackRecordingService trackRecordingService;
    private DeathRecipient deathRecipient;

    public ServiceBinder(TrackRecordingService trackRecordingService) {
      this.trackRecordingService = trackRecordingService;
    }

    @Override
    public boolean isBinderAlive() {
      return trackRecordingService != null;
    }

    @Override
    public boolean pingBinder() {
      return isBinderAlive();
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) {
      deathRecipient = recipient;
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
      if (!isBinderAlive()) {
        return false;
      }
      deathRecipient = null;
      return true;
    }

    @Override
    public boolean isRecording() {
      if (!canAccess()) {
        return false;
      }
      return trackRecordingService.isRecording();
    }

    @Override
    public long getRecordingTrackId() {
      if (!canAccess()) {
        return -1L;
      }
      return trackRecordingService.recordingTrackId;
    }

    @Override
    public long startNewTrack() {
      if (!canAccess()) {
        return -1L;
      }
      return trackRecordingService.startNewTrack();
    }

    @Override
    public long insertWaypoint(WaypointCreationRequest waypointCreationRequest) {
      if (!canAccess()) {
        return -1L;
      }
      return trackRecordingService.insertWaypoint(waypointCreationRequest);
    }

    @Override
    public void endCurrentTrack() {
      if (!canAccess()) {
        return;
      }
      trackRecordingService.endCurrentTrack();
    }

    @Override
    public void recordLocation(Location location) {
      if (!canAccess()) {
        return;
      }
      trackRecordingService.locationListener.onLocationChanged(location);
    }

    @Override
    public byte[] getSensorData() {
      if (!canAccess()) {
        return null;
      }
      if (trackRecordingService.sensorManager == null) {
        Log.d(TAG, "sensorManager is null.");
        return null;
      }
      if (trackRecordingService.sensorManager.getSensorDataSet() == null) {
        Log.d(TAG, "Sensor data set is null.");
        return null;
      }
      return trackRecordingService.sensorManager.getSensorDataSet().toByteArray();
    }

    @Override
    public int getSensorState() {
      if (!canAccess()) {
        return Sensor.SensorState.NONE.getNumber();
      }
      if (trackRecordingService.sensorManager == null) {
        Log.d(TAG, "sensorManager is null.");
        return Sensor.SensorState.NONE.getNumber();
      }
      return trackRecordingService.sensorManager.getSensorState().getNumber();
    }

    /**
     * Returns true if the RPC caller is from the same application or if the
     * "Allow access" setting indicates that another app can invoke this
     * service's RPCs.
     */
    private boolean canAccess() {
      // As a precondition for access, must check if the service is available.
      if (trackRecordingService == null) {
        throw new IllegalStateException("The track recording service has been detached!");
      }
      if (Process.myPid() == Binder.getCallingPid()) {
        return true;
      } else {
        return PreferencesUtils.getBoolean(trackRecordingService, R.string.allow_access_key,
            PreferencesUtils.ALLOW_ACCESS_DEFAULT);
      }
    }

    /**
     * Detaches from the track recording service. Clears the reference to the
     * outer class to minimize the leak.
     */
    private void detachFromService() {
      trackRecordingService = null;
      attachInterface(null, null);

      if (deathRecipient != null) {
        deathRecipient.binderDied();
      }
    }
  }
}
