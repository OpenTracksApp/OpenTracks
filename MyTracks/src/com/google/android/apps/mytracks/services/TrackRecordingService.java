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
import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.services.sensors.SensorManagerFactory;
import com.google.android.apps.mytracks.services.tasks.PeriodicTaskExecutor;
import com.google.android.apps.mytracks.services.tasks.SplitTask;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.ApiPlatformAdapter;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A background service that registers a location listener and records track
 * points. Track points are saved to the MyTracksProvider.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service {

  static final int MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS = 3;

  private NotificationManager notificationManager;
  private LocationManager locationManager;
  private WakeLock wakeLock;

  private int minRecordingDistance =
      Constants.DEFAULT_MIN_RECORDING_DISTANCE;
  private int maxRecordingDistance =
      Constants.DEFAULT_MAX_RECORDING_DISTANCE;
  private int minRequiredAccuracy =
      Constants.DEFAULT_MIN_REQUIRED_ACCURACY;
  private int autoResumeTrackTimeout =
      Constants.DEFAULT_AUTO_RESUME_TRACK_TIMEOUT;

  private long recordingTrackId = -1;

  private long currentWaypointId = -1;

  /** The timer posts a runnable to the main thread via this handler. */
  private final Handler handler = new Handler();

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;

  private TripStatisticsBuilder statsBuilder;
  private TripStatisticsBuilder waypointStatsBuilder;

  /**
   * Current length of the recorded track. This length is calculated from the
   * recorded points (as compared to each location fix). It's used to overlay
   * waypoints precisely in the elevation profile chart.
   */
  private double length;

  /**
   * Status announcer executor.
   */
  private PeriodicTaskExecutor announcementExecutor;
  private PeriodicTaskExecutor splitExecutor;

  private SensorManager sensorManager;

  private PreferenceManager prefManager;

  /**
   * The interval in milliseconds that we have requested to be notified of gps
   * readings.
   */
  private long currentRecordingInterval;

  /**
   * The policy used to decide how often we should request gps updates.
   */
  private LocationListenerPolicy locationListenerPolicy =
      new AbsoluteLocationListenerPolicy(0);

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
      executorService.submit(
        new Runnable() {
          @Override
          public void run() {
            onLocationChangedAsync(location);
          }
        });
    }
  };

  /**
   * Task invoked by a timer periodically to make sure the location listener is
   * still registered.
   */
  private TimerTask checkLocationListener = new TimerTask() {
    @Override
    public void run() {
      // It's always safe to assume that if isRecording() is true, it implies
      // that onCreate() has finished.
      if (isRecording()) {
        handler.post(new Runnable() {
          public void run() {
            Log.d(Constants.TAG,
                "Re-registering location listener with TrackRecordingService.");
            unregisterLocationListener();
            registerLocationListener();
          }
        });
      }
    }
  };

  /**
   * This timer invokes periodically the checkLocationListener timer task.
   */
  private final Timer timer = new Timer();

  /**
   * Is the phone currently moving?
   */
  private boolean isMoving = true;

  /**
   * The most recent recording track.
   */
  private Track recordingTrack;

  /**
   * Is the service currently recording a track?
   */
  private boolean isRecording;

  /**
   * Last good location the service has received from the location listener
   */
  private Location lastLocation;

  /**
   * Last valid location (i.e. not a marker) that was recorded.
   */
  private Location lastValidLocation;

  /**
   * A service to run tasks outside of the main thread.
   */
  private ExecutorService executorService;

  private ServiceBinder binder = new ServiceBinder(this);

  /*
   * Application lifetime events:
   */

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "TrackRecordingService.onCreate");
    
    /*
     * Use the MyTracksProviderUtils that access the MyTracksProvider because
     * this service can be called by MyTracks or another app.
     */
    providerUtils = MyTracksProviderUtils.Factory.get(this);
    notificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    setUpTaskExecutors();
    executorService = Executors.newSingleThreadExecutor();

    prefManager = new PreferenceManager(this);

    registerLocationListener();

    /*
     * After 5 min, check every minute that location listener still is
     * registered and spit out additional debugging info to the logs:
     */
    timer.schedule(checkLocationListener, 1000 * 60 * 5, 1000 * 60);

    // Try to restore previous recording state in case this service has been
    // restarted by the system, which can sometimes happen.
    recordingTrack = getRecordingTrack();
    if (recordingTrack != null) {
      restoreStats(recordingTrack);
      isRecording = true;
    } else {
      if (recordingTrackId != -1) {
        // Make sure we have consistent state in shared preferences.
        Log.w(TAG, "TrackRecordingService.onCreate: "
            + "Resetting an orphaned recording track = " + recordingTrackId);
      }
      prefManager.setRecordingTrack(recordingTrackId = -1);
    }
    showNotification();
  }

  @Override
  public void onStart(Intent intent, int startId) {
    handleStartCommand(intent, startId);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    handleStartCommand(intent, startId);
    return START_STICKY;
  }
  
  /**
   * Handles onStart and onStartCommand. This service, through the
   * AndroidManifest.xml, is configured to allow both MyTracks and other
   * applications to invoke it. With only the intent, we cannot tell whether the
   * caller is MyTracks or another app. Thus when starting the service through
   * this method, we cannot read/write MyTracks data or start/stop a recording.
   */
  private void handleStartCommand(Intent intent, int startId) {
    Log.d(TAG, "TrackRecordingService.handleStartCommand: " + startId);

    if (intent == null) {
      return;
    }

    // Check if called on phone reboot with resume intent.
    if (intent.getBooleanExtra(RESUME_TRACK_EXTRA_NAME, false)) {
      resumeTrack(startId);
    }
  }

  private boolean isTrackInProgress() {
    return recordingTrackId != -1 || isRecording;
  }
  
  private void resumeTrack(int startId) {
    Log.d(TAG, "TrackRecordingService: requested resume");

    // Make sure that the current track exists and is fresh enough.
    if (recordingTrack == null || !shouldResumeTrack(recordingTrack)) {
      Log.i(TAG,
          "TrackRecordingService: Not resuming, because the previous track ("
          + recordingTrack + ") doesn't exist or is too old");
      isRecording = false;
      prefManager.setRecordingTrack(recordingTrackId = -1);
      stopSelfResult(startId);
      return;
    }

    Log.i(TAG, "TrackRecordingService: resuming");
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "TrackRecordingService.onBind");
    return binder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.d(TAG, "TrackRecordingService.onUnbind");
    return super.onUnbind(intent);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "TrackRecordingService.onDestroy");

    isRecording = false;
    showNotification();
    prefManager.shutdown();
    prefManager = null;
    checkLocationListener.cancel();
    checkLocationListener = null;
    timer.cancel();
    timer.purge();
    unregisterLocationListener();
    shutdownTaskExecutors();
    if (sensorManager != null) {
      sensorManager.shutdown();
      sensorManager = null;
    }

    // Make sure we have no indirect references to this service.
    locationManager = null;
    notificationManager = null;
    providerUtils = null;
    binder.detachFromService();
    binder = null;

    // This should be the last operation.
    releaseWakeLock();

    // Shutdown the executor service last to avoid sending events to a dead executor.
    executorService.shutdown();
    super.onDestroy();
  }

  private void setAutoResumeTrackRetries(int retryAttempts) {
    Log.d(TAG, "Updating auto-resume retry attempts to: " + retryAttempts);
    prefManager.setAutoResumeTrackCurrentRetry(retryAttempts);
  }

  private boolean shouldResumeTrack(Track track) {
    Log.d(TAG, "shouldResumeTrack: autoResumeTrackTimeout = "
        + autoResumeTrackTimeout);

    // Check if we haven't exceeded the maximum number of retry attempts.
    SharedPreferences sharedPreferences =
        getSharedPreferences(Constants.SETTINGS_NAME, 0);
    int retries = sharedPreferences.getInt(
        getString(R.string.auto_resume_track_current_retry_key), 0);
    Log.d(TAG,
        "shouldResumeTrack: Attempting to auto-resume the track ("
        + (retries + 1) + "/" + MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS + ")");
    if (retries >= MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS) {
      Log.i(TAG,
          "shouldResumeTrack: Not resuming because exceeded the maximum "
          + "number of auto-resume retries");
      return false;
    }

    // Increase number of retry attempts.
    setAutoResumeTrackRetries(retries + 1);

    // Check for special cases.
    if (autoResumeTrackTimeout == 0) {
      // Never resume.
      Log.d(TAG,
          "shouldResumeTrack: Auto-resume disabled (never resume)");
      return false;
    } else if (autoResumeTrackTimeout == -1) {
      // Always resume.
      Log.d(TAG,
          "shouldResumeTrack: Auto-resume forced (always resume)");
      return true;
    }

    // Check if the last modified time is within the acceptable range.
    long lastModified =
        track.getStatistics() != null ? track.getStatistics().getStopTime() : 0;
    Log.d(TAG,
        "shouldResumeTrack: lastModified = " + lastModified
        + ", autoResumeTrackTimeout: " + autoResumeTrackTimeout);
    return lastModified > 0 && System.currentTimeMillis() - lastModified <=
        autoResumeTrackTimeout * 60L * 1000L;
  }

  /*
   * Setup/shutdown methods.
   */

  /**
   * Tries to acquire a partial wake lock if not already acquired. Logs errors
   * and gives up trying in case the wake lock cannot be acquired.
   */
  private void acquireWakeLock() {
    try {
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      if (pm == null) {
        Log.e(TAG,
            "TrackRecordingService: Power manager not found!");
        return;
      }
      if (wakeLock == null) {
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            TAG);
        if (wakeLock == null) {
          Log.e(TAG,
              "TrackRecordingService: Could not create wake lock (null).");
          return;
        }
      }
      if (!wakeLock.isHeld()) {
        wakeLock.acquire();
        if (!wakeLock.isHeld()) {
          Log.e(TAG,
              "TrackRecordingService: Could not acquire wake lock.");
        }
      }
    } catch (RuntimeException e) {
      Log.e(TAG,
          "TrackRecordingService: Caught unexpected exception: "
          + e.getMessage(), e);
    }
  }

  /**
   * Releases the wake lock if it's currently held.
   */
  private void releaseWakeLock() {
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      wakeLock = null;
    }
  }

  /**
   * Shows the notification message and icon in the notification bar.
   */
  private void showNotification() {
    final ApiPlatformAdapter apiPlatformAdapter =
        ApiFeatures.getInstance().getApiPlatformAdapter();
    if (isRecording) {
      Notification notification = new Notification(
          R.drawable.arrow_320, null /* tickerText */,
          System.currentTimeMillis());
      PendingIntent contentIntent = PendingIntent.getActivity(
          this, 0 /* requestCode */, new Intent(this, MyTracks.class),
          0 /* flags */);
      notification.setLatestEventInfo(this, getString(R.string.app_name),
          getString(R.string.recording_your_track), contentIntent);
      notification.flags += Notification.FLAG_NO_CLEAR;
      apiPlatformAdapter.startForeground(this, notificationManager, 1,
          notification);
    } else {
      apiPlatformAdapter.stopForeground(this, notificationManager, 1);
    }
  }

  private void setUpTaskExecutors() {
    announcementExecutor = new PeriodicTaskExecutor(
        this, new StatusAnnouncerFactory(ApiFeatures.getInstance()));
    splitExecutor = new PeriodicTaskExecutor(this, new SplitTask.Factory());
  }

  private void shutdownTaskExecutors() {
    Log.d(TAG, "TrackRecordingService.shutdownExecuters");
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
  }

  private void registerLocationListener() {
    if (locationManager == null) {
      Log.e(TAG,
          "TrackRecordingService: Do not have any location manager.");
      return;
    }
    Log.d(TAG,
        "Preparing to register location listener w/ TrackRecordingService...");
    try {
      long desiredInterval = locationListenerPolicy.getDesiredPollingInterval();
      locationManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER, desiredInterval,
          locationListenerPolicy.getMinDistance(),
          // , 0 /* minDistance, get all updates to properly time pauses */
          locationListener);
      currentRecordingInterval = desiredInterval;
      Log.d(TAG,
          "...location listener now registered w/ TrackRecordingService @ "
          + currentRecordingInterval);
    } catch (RuntimeException e) {
      Log.e(TAG,
          "Could not register location listener: " + e.getMessage(), e);
    }
  }

  private void unregisterLocationListener() {
    if (locationManager == null) {
      Log.e(TAG,
          "TrackRecordingService: Do not have any location manager.");
      return;
    }
    locationManager.removeUpdates(locationListener);
    Log.d(TAG,
        "Location listener now unregistered w/ TrackRecordingService.");
  }

  /*
   * Recording lifecycle.
   */

  public long startNewTrack() {
    Log.d(TAG, "TrackRecordingService.startNewTrack");
    if (isTrackInProgress()) {
      return -1L;
    }

    long startTime = System.currentTimeMillis();
    acquireWakeLock();

    Track track = new Track();
    TripStatistics trackStats = track.getStatistics();
    trackStats.setStartTime(startTime);
    track.setStartId(-1);
    Uri trackUri = providerUtils.insertTrack(track);
    recordingTrackId = Long.parseLong(trackUri.getLastPathSegment());
    track.setId(recordingTrackId);
    track.setName(new DefaultTrackNameFactory(this).newTrackName(
        recordingTrackId, startTime));
    isRecording = true;
    isMoving = true;

    providerUtils.updateTrack(track);
    statsBuilder = new TripStatisticsBuilder(startTime);
    statsBuilder.setMinRecordingDistance(minRecordingDistance);
    waypointStatsBuilder = new TripStatisticsBuilder(startTime);
    waypointStatsBuilder.setMinRecordingDistance(minRecordingDistance);
    currentWaypointId = insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
    length = 0;
    showNotification();
    registerLocationListener();
    sensorManager = SensorManagerFactory.getSensorManager(this);
    if (sensorManager != null) {
      sensorManager.onStartTrack();
    }

    // Reset the number of auto-resume retries.
    setAutoResumeTrackRetries(0);
    // Persist the current recording track.
    prefManager.setRecordingTrack(recordingTrackId);

    // Notify the world that we're now recording.
    sendTrackBroadcast(
        R.string.track_started_broadcast_action, recordingTrackId);
    announcementExecutor.restore();
    splitExecutor.restore();

    return recordingTrackId;
  }

  private void restoreStats(Track track) {
    Log.d(TAG,
        "Restoring stats of track with ID: " + track.getId());

    TripStatistics stats = track.getStatistics();
    statsBuilder = new TripStatisticsBuilder(stats.getStartTime());
    statsBuilder.setMinRecordingDistance(minRecordingDistance);

    length = 0;
    lastValidLocation = null;

    Waypoint waypoint = providerUtils.getFirstWaypoint(recordingTrackId);
    if (waypoint != null && waypoint.getStatistics() != null) {
      currentWaypointId = waypoint.getId();
      waypointStatsBuilder = new TripStatisticsBuilder(
          waypoint.getStatistics());
    } else {
      // This should never happen, but we got to do something so life goes on:
      waypointStatsBuilder = new TripStatisticsBuilder(stats.getStartTime());
      currentWaypointId = -1;
    }
    waypointStatsBuilder.setMinRecordingDistance(minRecordingDistance);

    Cursor cursor = null;
    try {
      cursor = providerUtils.getLocationsCursor(
          recordingTrackId, -1, Constants.MAX_LOADED_TRACK_POINTS,
          true);
      if (cursor != null) {
        if (cursor.moveToLast()) {
          do {
            Location location = providerUtils.createLocation(cursor);
            if (LocationUtils.isValidLocation(location)) {
              statsBuilder.addLocation(location, location.getTime());
              if (lastValidLocation != null) {
                length += location.distanceTo(lastValidLocation);
              }
              lastValidLocation = location;
            }
          } while (cursor.moveToPrevious());
        }
        statsBuilder.getStatistics().setMovingTime(stats.getMovingTime());
        statsBuilder.pauseAt(stats.getStopTime());
        statsBuilder.resumeAt(System.currentTimeMillis());
      } else {
        Log.e(TAG, "Could not get track points cursor.");
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "Error while restoring track.", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    announcementExecutor.restore();
    splitExecutor.restore();
  }

  private void onLocationChangedAsync(Location location) {
    Log.d(TAG, "TrackRecordingService.onLocationChanged");

    try {
      // Don't record if the service has been asked to pause recording:
      if (!isRecording) {
        Log.w(TAG,
            "Not recording because recording has been paused.");
        return;
      }

      // This should never happen, but just in case (we really don't want the
      // service to crash):
      if (location == null) {
        Log.w(TAG,
            "Location changed, but location is null.");
        return;
      }

      // Don't record if the accuracy is too bad:
      if (location.getAccuracy() > minRequiredAccuracy) {
        Log.d(TAG,
            "Not recording. Bad accuracy.");
        return;
      }

      // At least one track must be available for appending points:
      recordingTrack = getRecordingTrack();
      if (recordingTrack == null) {
        Log.d(TAG,
            "Not recording. No track to append to available.");
        return;
      }

      // Update the idle time if needed.
      locationListenerPolicy.updateIdleTime(statsBuilder.getIdleTime());
      addLocationToStats(location);
      if (currentRecordingInterval !=
          locationListenerPolicy.getDesiredPollingInterval()) {
        registerLocationListener();
      }

      Location lastRecordedLocation = providerUtils.getLastLocation();
      double distanceToLastRecorded = Double.POSITIVE_INFINITY;
      if (lastRecordedLocation != null) {
        distanceToLastRecorded = location.distanceTo(lastRecordedLocation);
      }
      double distanceToLast = Double.POSITIVE_INFINITY;
      if (lastLocation != null) {
        distanceToLast = location.distanceTo(lastLocation);
      }
      boolean hasSensorData = sensorManager != null
          && sensorManager.isEnabled()
          && sensorManager.getSensorDataSet() != null
          && sensorManager.isDataValid();

      // If the user has been stationary for two recording just record the first
      // two and ignore the rest. This code will only have an effect if the
      // maxRecordingDistance = 0
      if (distanceToLast == 0 && !hasSensorData) {
        if (isMoving) {
          Log.d(TAG, "Found two identical locations.");
          isMoving = false;
          if (lastLocation != null && lastRecordedLocation != null
              && !lastRecordedLocation.equals(lastLocation)) {
            // Need to write the last location. This will happen when
            // lastRecordedLocation.distance(lastLocation) <
            // minRecordingDistance
            if (!insertLocation(lastLocation, lastRecordedLocation, recordingTrackId)) {
              return;
            }
          }
        } else {
          Log.d(TAG,
              "Not recording. More than two identical locations.");
        }
      } else if (distanceToLastRecorded > minRecordingDistance
          || hasSensorData) {
        if (lastLocation != null && !isMoving) {
          // Last location was the last stationary location. Need to go back and
          // add it.
          if (!insertLocation(lastLocation, lastRecordedLocation, recordingTrackId)) {
            return;
          }
          isMoving = true;
        }

        // If separation from last recorded point is too large insert a
        // separator to indicate end of a segment:
        boolean startNewSegment =
            lastRecordedLocation != null
                && lastRecordedLocation.getLatitude() < 90
                && distanceToLastRecorded > maxRecordingDistance
                && recordingTrack.getStartId() >= 0;
        if (startNewSegment) {
          // Insert a separator point to indicate start of new track:
          Log.d(TAG, "Inserting a separator.");
          Location separator = new Location(LocationManager.GPS_PROVIDER);
          separator.setLongitude(0);
          separator.setLatitude(100);
          separator.setTime(lastRecordedLocation.getTime());
          providerUtils.insertTrackPoint(separator, recordingTrackId);
        }

        if (!insertLocation(location, lastRecordedLocation, recordingTrackId)) {
          return;
        }
      } else {
        Log.d(TAG, String.format(
            "Not recording. Distance to last recorded point (%f m) is less than"
            + " %d m.", distanceToLastRecorded, minRecordingDistance));
        // Return here so that the location is NOT recorded as the last location.
        return;
      }
    } catch (Error e) {
      // Probably important enough to rethrow.
      Log.e(TAG, "Error in onLocationChanged", e);
      throw e;
    } catch (RuntimeException e) {
      // Safe usually to trap exceptions.
      Log.e(TAG,
          "Trapping exception in onLocationChanged", e);
      throw e;
    }
    lastLocation = location;
  }

  /**
   * Inserts a new location in the track points db and updates the corresponding
   * track in the track db.
   *
   * @param location the location to be inserted
   * @param lastRecordedLocation the last recorded location before this one (or
   *        null if none)
   * @param trackId the id of the track
   * @return true if successful. False if SQLite3 threw an exception.
   */
  private boolean insertLocation(Location location, Location lastRecordedLocation, long trackId) {

    // Keep track of length along recorded track (needed when a waypoint is
    // inserted):
    if (LocationUtils.isValidLocation(location)) {
      if (lastValidLocation != null) {
        length += location.distanceTo(lastValidLocation);
      }
      lastValidLocation = location;
    }

    // Insert the new location:
    try {
      Location locationToInsert = location;
      if (sensorManager != null && sensorManager.isEnabled()) {
        SensorDataSet sd = sensorManager.getSensorDataSet();
        if (sd != null && sensorManager.isDataValid()) {
          locationToInsert = new MyTracksLocation(location, sd);
        }
      }
      Uri pointUri = providerUtils.insertTrackPoint(locationToInsert, trackId);
      int pointId = Integer.parseInt(pointUri.getLastPathSegment());

      // Update the current track:
      if (lastRecordedLocation != null
          && lastRecordedLocation.getLatitude() < 90) {
        ContentValues values = new ContentValues();
        TripStatistics stats = statsBuilder.getStatistics();
        if (recordingTrack.getStartId() < 0) {
          values.put(TracksColumns.STARTID, pointId);
          recordingTrack.setStartId(pointId);
        }
        values.put(TracksColumns.STOPID, pointId);
        values.put(TracksColumns.STOPTIME, System.currentTimeMillis());
        values.put(TracksColumns.NUMPOINTS,
            recordingTrack.getNumberOfPoints() + 1);
        values.put(TracksColumns.MINLAT, stats.getBottom());
        values.put(TracksColumns.MAXLAT, stats.getTop());
        values.put(TracksColumns.MINLON, stats.getLeft());
        values.put(TracksColumns.MAXLON, stats.getRight());
        values.put(TracksColumns.TOTALDISTANCE, stats.getTotalDistance());
        values.put(TracksColumns.TOTALTIME, stats.getTotalTime());
        values.put(TracksColumns.MOVINGTIME, stats.getMovingTime());
        values.put(TracksColumns.AVGSPEED, stats.getAverageSpeed());
        values.put(TracksColumns.AVGMOVINGSPEED, stats.getAverageMovingSpeed());
        values.put(TracksColumns.MAXSPEED, stats.getMaxSpeed());
        values.put(TracksColumns.MINELEVATION, stats.getMinElevation());
        values.put(TracksColumns.MAXELEVATION, stats.getMaxElevation());
        values.put(TracksColumns.ELEVATIONGAIN, stats.getTotalElevationGain());
        values.put(TracksColumns.MINGRADE, stats.getMinGrade());
        values.put(TracksColumns.MAXGRADE, stats.getMaxGrade());
        getContentResolver().update(TracksColumns.CONTENT_URI,
            values, "_id=" + recordingTrack.getId(), null);
        updateCurrentWaypoint();
      }
    } catch (SQLiteException e) {
      // Insert failed, most likely because of SqlLite error code 5
      // (SQLite_BUSY). This is expected to happen extremely rarely (if our
      // listener gets invoked twice at about the same time).
      Log.w(TAG,
          "Caught SQLiteException: " + e.getMessage(), e);
      return false;
    }
    announcementExecutor.update();
    splitExecutor.update();
    return true;
  }

  private void updateCurrentWaypoint() {
    if (currentWaypointId >= 0) {
      ContentValues values = new ContentValues();
      TripStatistics waypointStats = waypointStatsBuilder.getStatistics();
      values.put(WaypointsColumns.STARTTIME, waypointStats.getStartTime());
      values.put(WaypointsColumns.LENGTH, length);
      values.put(WaypointsColumns.DURATION, System.currentTimeMillis()
          - statsBuilder.getStatistics().getStartTime());
      values.put(WaypointsColumns.TOTALDISTANCE,
          waypointStats.getTotalDistance());
      values.put(WaypointsColumns.TOTALTIME, waypointStats.getTotalTime());
      values.put(WaypointsColumns.MOVINGTIME, waypointStats.getMovingTime());
      values.put(WaypointsColumns.AVGSPEED, waypointStats.getAverageSpeed());
      values.put(WaypointsColumns.AVGMOVINGSPEED,
          waypointStats.getAverageMovingSpeed());
      values.put(WaypointsColumns.MAXSPEED, waypointStats.getMaxSpeed());
      values.put(WaypointsColumns.MINELEVATION,
          waypointStats.getMinElevation());
      values.put(WaypointsColumns.MAXELEVATION,
          waypointStats.getMaxElevation());
      values.put(WaypointsColumns.ELEVATIONGAIN,
          waypointStats.getTotalElevationGain());
      values.put(WaypointsColumns.MINGRADE, waypointStats.getMinGrade());
      values.put(WaypointsColumns.MAXGRADE, waypointStats.getMaxGrade());
      getContentResolver().update(WaypointsColumns.CONTENT_URI,
          values, "_id=" + currentWaypointId, null);
    }
  }

  private void addLocationToStats(Location location) {
    if (LocationUtils.isValidLocation(location)) {
      long now = System.currentTimeMillis();
      statsBuilder.addLocation(location, now);
      waypointStatsBuilder.addLocation(location, now);
    }
  }

  /*
   * Application lifetime events: ============================
   */

  public long insertWaypoint(WaypointCreationRequest request) {
    if (!isRecording()) {
      throw new IllegalStateException(
          "Unable to insert waypoint marker while not recording!");
    }
    if (request == null) {
      request = WaypointCreationRequest.DEFAULT_MARKER;
    }
    Waypoint wpt = new Waypoint();
    switch (request.getType()) {
      case MARKER:
        buildMarker(wpt, request);
        break;
      case STATISTICS:
        buildStatisticsMarker(wpt);
        break;
    }
    wpt.setTrackId(recordingTrackId);
    wpt.setLength(length);
    if (lastLocation == null
        || statsBuilder == null || statsBuilder.getStatistics() == null) {
      // A null location is ok, and expected on track start.
      // Make it an impossible location.
      Location l = new Location("");
      l.setLatitude(100);
      l.setLongitude(180);
      wpt.setLocation(l);
    } else {
      wpt.setLocation(lastLocation);
      wpt.setDuration(lastLocation.getTime()
          - statsBuilder.getStatistics().getStartTime());
    }
    Uri uri = providerUtils.insertWaypoint(wpt);
    return Long.parseLong(uri.getLastPathSegment());
  }

  private void buildMarker(Waypoint wpt, WaypointCreationRequest request) {
    wpt.setType(Waypoint.TYPE_WAYPOINT);
    if (request.getIconUrl() == null) {
      wpt.setIcon(getString(R.string.waypoint_icon_url));
    } else {
      wpt.setIcon(request.getIconUrl());
    }
    if (request.getName() == null) {
      wpt.setName(getString(R.string.waypoint));
    } else {
      wpt.setName(request.getName());
    }
    if (request.getDescription() != null) {
      wpt.setDescription(request.getDescription());
    }
  }

  /**
   * Build a statistics marker.
   * A statistics marker holds the stats for the* last segment up to this marker.
   *
   * @param waypoint The waypoint which will be populated with stats data.
   */
  private void buildStatisticsMarker(Waypoint waypoint) {
    StringUtils utils = new StringUtils(TrackRecordingService.this);

    // Set stop and total time in the stats data
    final long time = System.currentTimeMillis();
    waypointStatsBuilder.pauseAt(time);

    // Override the duration - it's not the duration from the last waypoint, but
    // the duration from the beginning of the whole track
    waypoint.setDuration(time - statsBuilder.getStatistics().getStartTime());

    // Set the rest of the waypoint data
    waypoint.setType(Waypoint.TYPE_STATISTICS);
    waypoint.setName(getString(R.string.statistics));
    waypoint.setStatistics(waypointStatsBuilder.getStatistics());
    waypoint.setDescription(utils.generateWaypointDescription(waypoint));
    waypoint.setIcon(getString(R.string.stats_icon_url));

    waypoint.setStartId(providerUtils.getLastLocationId(recordingTrackId));

    // Create a new stats keeper for the next marker.
    waypointStatsBuilder = new TripStatisticsBuilder(time);
  }

  private void endCurrentTrack() {
    Log.d(TAG, "TrackRecordingService.endCurrentTrack");
    if (!isTrackInProgress()) {
      return;
    }

    announcementExecutor.shutdown();
    splitExecutor.shutdown();
    isRecording = false;
    Track recordedTrack = providerUtils.getTrack(recordingTrackId);
    if (recordedTrack != null) {
      TripStatistics stats = recordedTrack.getStatistics();
      stats.setStopTime(System.currentTimeMillis());
      stats.setTotalTime(stats.getStopTime() - stats.getStartTime());
      long lastRecordedLocationId =
          providerUtils.getLastLocationId(recordingTrackId);
      ContentValues values = new ContentValues();
      if (lastRecordedLocationId >= 0 && recordedTrack.getStopId() >= 0) {
        values.put(TracksColumns.STOPID, lastRecordedLocationId);
      }
      values.put(TracksColumns.STOPTIME, stats.getStopTime());
      values.put(TracksColumns.TOTALTIME, stats.getTotalTime());
      getContentResolver().update(TracksColumns.CONTENT_URI, values,
          "_id=" + recordedTrack.getId(), null);
    }
    showNotification();
    long recordedTrackId = recordingTrackId;
    prefManager.setRecordingTrack(recordingTrackId = -1);

    if (sensorManager != null) {
      sensorManager.shutdown();
      sensorManager = null;
    }

    releaseWakeLock();

    // Notify the world that we're no longer recording.
    sendTrackBroadcast(
        R.string.track_stopped_broadcast_action, recordedTrackId);

    stopSelf();
  }

  private void sendTrackBroadcast(int actionResId, long trackId) {
    Intent broadcastIntent = new Intent().setAction(getString(actionResId)).putExtra(
        getString(R.string.track_id_broadcast_extra), trackId);
    sendBroadcast(broadcastIntent, getString(R.string.mytracks_notifications_permission));
    
    SharedPreferences sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    if (sharedPreferences.getBoolean(getString(R.string.share_data_key), false)) {
      sendBroadcast(broadcastIntent, getString(R.string.broadcast_notifications_permission));
    }
  }

  /*
   * Data/state access.
   */

  private Track getRecordingTrack() {
    if (recordingTrackId < 0) {
      return null;
    }

    return providerUtils.getTrack(recordingTrackId);
  }

  public boolean isRecording() {
    return isRecording;
  }

  public TripStatistics getTripStatistics() {
    return statsBuilder.getStatistics();
  }

  Location getLastLocation() {
    return lastLocation;
  }

  long getRecordingTrackId() {
    return recordingTrackId;
  }

  void setRecordingTrackId(long recordingTrackId) {
    this.recordingTrackId = recordingTrackId;
  }

  void setMaxRecordingDistance(int maxRecordingDistance) {
    this.maxRecordingDistance = maxRecordingDistance;
  }

  void setMinRecordingDistance(int minRecordingDistance) {
    this.minRecordingDistance = minRecordingDistance;
    if (statsBuilder != null && waypointStatsBuilder != null) {
      statsBuilder.setMinRecordingDistance(minRecordingDistance);
      waypointStatsBuilder.setMinRecordingDistance(minRecordingDistance);
    }
  }

  void setMinRequiredAccuracy(int minRequiredAccuracy) {
    this.minRequiredAccuracy = minRequiredAccuracy;
  }

  void setLocationListenerPolicy(LocationListenerPolicy locationListenerPolicy) {
    this.locationListenerPolicy = locationListenerPolicy;
  }

  void setAutoResumeTrackTimeout(int autoResumeTrackTimeout) {
    this.autoResumeTrackTimeout = autoResumeTrackTimeout;
  }

  void setAnnouncementFrequency(int announcementFrequency) {
    announcementExecutor.setTaskFrequency(announcementFrequency);
  }

  void setSplitFrequency(int frequency) {
    splitExecutor.setTaskFrequency(frequency);
  }

  void setMetricUnits(boolean metric) {
    announcementExecutor.setMetricUnits(metric);
    splitExecutor.setMetricUnits(metric);
  }


  /**
   * TODO: There is a bug in Android that leaks Binder instances.  This bug is
   * especially visible if we have a non-static class, as there is no way to
   * nullify reference to the outer class (the service).
   * A workaround is to use a static class and explicitly clear service
   * and detach it from the underlying Binder.  With this approach, we minimize
   * the leak to 24 bytes per each service instance.
   *
   * For more details, see the following bug:
   * http://code.google.com/p/android/issues/detail?id=6426.
   */
  private static class ServiceBinder extends ITrackRecordingService.Stub {
    private TrackRecordingService service;
    private DeathRecipient deathRecipient;

    public ServiceBinder(TrackRecordingService service) {
      this.service = service;
    }

    // Logic for letting the actual service go up and down.

    @Override
    public boolean isBinderAlive() {
      // Pretend dead if the service went down.
      return service != null;
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

    /**
     * Clears the reference to the outer class to minimize the leak.
     */
    private void detachFromService() {
      this.service = null;
      attachInterface(null, null);

      if (deathRecipient != null) {
        deathRecipient.binderDied();
      }
    }

    /**
     * Checks if the service is available. If not, throws an
     * {@link IllegalStateException}.
     */
    private void checkService() {
      if (service == null) {
        throw new IllegalStateException("The service has been already detached!");
      }
    }
    
    /**
     * Returns true if the RPC caller is from the same application or if the
     * sharing setting indicates that another app can invoke this service's
     * RPCs. 
     */
    private boolean canAccess() {
      
      // As a precondition for access, must check if the service is available.
      checkService();
      
      if (Process.myPid() == Binder.getCallingPid()) {
        return true;
      } else {
        SharedPreferences sharedPreferences = service.getSharedPreferences(
            Constants.SETTINGS_NAME, 0);
        return sharedPreferences.getBoolean(service.getString(R.string.share_data_key), false);
      }
    }

    // Service method delegates.

    @Override
    public boolean isRecording() {
      if (!canAccess()) {
        return false;
      }
      return service.isRecording();
    }

    @Override
    public long getRecordingTrackId() {
      if (!canAccess()) {
        return -1L;
      }
      return service.recordingTrackId;
    }

    @Override
    public long startNewTrack() {
      if (!canAccess()) {
        return -1L;
      }
      return service.startNewTrack();
    }

    /**
     * Inserts a waypoint marker in the track being recorded.
     *
     * @param request Details of the waypoint to insert
     * @return the unique ID of the inserted marker
     */
    public long insertWaypoint(WaypointCreationRequest request) {
      if (!canAccess()) {
        return -1L;
      }
      return service.insertWaypoint(request);
    }

    @Override
    public void endCurrentTrack() {
      if (!canAccess()) {
        return;
      }
      service.endCurrentTrack();
    }

    @Override
    public void recordLocation(Location loc) {
      if (!canAccess()) {
        return;
      }
      service.locationListener.onLocationChanged(loc);
    }

    @Override
    public byte[] getSensorData() {
      if (!canAccess()) {
        return null;
      }
      if (service.sensorManager == null) {
        Log.d(TAG, "No sensor manager for data.");
        return null;
      }
      if (service.sensorManager.getSensorDataSet() == null) {
        Log.d(TAG, "Sensor data set is null.");
        return null;
      }
      return service.sensorManager.getSensorDataSet().toByteArray();
    }

    @Override
    public int getSensorState() {
      if (!canAccess()) {
        return Sensor.SensorState.NONE.getNumber();
      }      
      if (service.sensorManager == null) {
        Log.d(TAG, "No sensor manager for data.");
        return Sensor.SensorState.NONE.getNumber();
      }
      return service.sensorManager.getSensorState().getNumber();
    }
  }

}
