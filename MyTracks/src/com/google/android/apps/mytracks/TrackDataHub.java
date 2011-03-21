/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.TrackDataListener.ProviderState;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.maps.mytracks.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Track data hub, which receives data (both live and recorded) from many
 * different sources and distributes it to those interested after some standard
 * processing.
 *
 * @author Rodrigo Damazio
 */
public class TrackDataHub {

  private static final long MAX_LOCATION_AGE_MS = 60 * 1000;  // 1 minute
  private static final long MAX_NETWORK_AGE_MS = 1000 * 60 * 10;  // 10 minutes

  // Preference keys
  private final String SELECTED_TRACK_KEY;
  private final String RECORDING_TRACK_KEY;
  private final String MIN_REQUIRED_ACCURACY_KEY;
  private final String METRIC_UNITS_KEY;
  private final String SPEED_REPORTING_KEY;

  // Application services
  private final Context context;
  private final MyTracksProviderUtils providerUtils;

  // System services
  private final SensorManager sensorManager;
  private final LocationManager locationManager;
  private final SharedPreferences sharedPreferences;
  private final ContentResolver contentResolver;

  // Internal listeners (to receive data from the system)
  private final ContentObserver pointObserver;
  private final ContentObserver waypointObserver;
  private final ContentObserver trackObserver;
  private final LocationListener locationListener;
  private final OnSharedPreferenceChangeListener preferenceListener;
  private final SensorEventListener compassListener;

  // External listeners (to pass data to activities)
  private final Set<TrackDataListener> registeredListeners =
      new LinkedHashSet<TrackDataListener>();

  // Get content notifications on the main thread, send listener callbacks in another.
  // This ensures listener calls are serialized.
  private final HandlerThread listenerHandlerThread;
  private final Handler listenerHandler;
  private boolean started;

  // Cached preference values
  private int minRequiredAccuracy;
  private boolean useMetricUnits;
  private boolean reportSpeed;

  // Cached sensor readings
  private float declination;
  private long lastDeclinationUpdate;
  private float lastSeenMagneticHeading;
  private Location lastSeenLocation;
  private boolean hasFix;
  private boolean hasGoodFix;

  // Transient state about the selected track
  private long firstSeenLocationId;
  private long lastSeenLocationId;
  private long selectedTrackId;
  private long recordingTrackId;
  private int numLoadedPoints;
  private boolean hasProviderEnabled;

  /** Callback for when the tracks table is updated. */
  private class TrackObserverCallback implements Runnable {
    @Override
    public void run() {
      notifyTrackUpdated(getRegisteredListenerArray());
    }
  }

  /** Callback for when the waypoints table is updated. */
  private class WaypointObserverCallback implements Runnable {
    @Override
    public void run() {
      notifyWaypointUpdated(getRegisteredListenerArray());
    }
  }

  /** Callback for when the points table is updated. */
  private class PointObserverCallback implements Runnable {
    @Override
    public void run() {
      notifyPointsUpdated(true, getRegisteredListenerArray());
    }
  }

  /** Listener for when preferences change. */
  private class HubSharedPreferenceListener implements OnSharedPreferenceChangeListener {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      notifyPreferenceChanged(key);
    }
  }

  /**
   * Generic content observer which will call a given {@link Runnable} in the
   * given handler if the content has changed and we're recording the selected
   * track.
   */
  private class TrackContentObserver extends ContentObserver {
    private final Runnable callback;

    public TrackContentObserver(Handler contentHandler, Runnable callback) {
      super(contentHandler);

      this.callback = callback;
    }

    @Override
    public void onChange(boolean selfChange) {
      Log.v(TAG, "TrackContentObserver.onChange");

      // We want to filter only updates from the selected track, but since
      // we can't see what the update is, we'll let two cases pass:
      // 1 - The point(s) was(ere) changed because it's a recording track
      //     (and thus we care about it if the recording is the selected one)
      // 2 - The point(s) was(ere) changed because it's syncing a track
      //     (and thus there will be no new points for the selected one)
      if (!isRecordingSelected()) {
        return;
      }

      // Update can potentially be lengthy, put it in its own thread:
      listenerHandler.post(callback);
    }
  }

  /** Listener for the current location (independent from track data). */
  private class CurrentLocationListener implements
      LocationListener {
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      if (!LocationManager.GPS_PROVIDER.equals(provider)) return;

      hasProviderEnabled = (status == LocationProvider.AVAILABLE);
      notifyFixType();
    }

    @Override
    public void onProviderEnabled(String provider) {
      if (!LocationManager.GPS_PROVIDER.equals(provider)) return;

      hasProviderEnabled = true;
      notifyFixType();
    }

    @Override
    public void onProviderDisabled(String provider) {
      if (!LocationManager.GPS_PROVIDER.equals(provider)) return;

      hasProviderEnabled = false;
      notifyFixType();
    }

    @Override
    public void onLocationChanged(Location location) {
      notifyLocationChanged(location);
    }
  }

  /** Listener for compass readings. */
  private class CompassListener implements
      SensorEventListener {
    @Override
    public void onSensorChanged(SensorEvent event) {
      lastSeenMagneticHeading = event.values[0];
      maybeUpdateDeclination();
      notifyHeadingChanged(getRegisteredListenerArray());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
      // Do nothing
    }
  }

  public TrackDataHub(Context ctx, MyTracksProviderUtils providerUtils) {
    this.context = ctx;
    this.providerUtils = providerUtils;

    SELECTED_TRACK_KEY = context.getString(R.string.selected_track_key);
    RECORDING_TRACK_KEY = context.getString(R.string.recording_track_key);
    MIN_REQUIRED_ACCURACY_KEY = context.getString(R.string.min_required_accuracy_key);
    METRIC_UNITS_KEY = context.getString(R.string.metric_units_key);
    SPEED_REPORTING_KEY = context.getString(R.string.report_speed_key);

    listenerHandlerThread = new HandlerThread("trackDataContentThread");
    listenerHandlerThread.start();
    listenerHandler = new Handler(listenerHandlerThread.getLooper());

    sharedPreferences = ctx.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    preferenceListener = new HubSharedPreferenceListener();

    sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
    locationManager =
        (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

    contentResolver = ctx.getContentResolver();
    Handler contentHandler = new Handler();
    pointObserver = new TrackContentObserver(contentHandler, new PointObserverCallback());
    waypointObserver = new TrackContentObserver(contentHandler, new WaypointObserverCallback());
    trackObserver = new TrackContentObserver(contentHandler, new TrackObserverCallback());

    compassListener = new CompassListener();
    locationListener = new CurrentLocationListener();
  }

  /**
   * Starts listening to data sources and reporting the data to external
   * listeners.
   */
  public void start() {
    Log.i(TAG, "TrackDataHub.start");
    if (started) {
      Log.w(TAG, "Already started, ignoring");
      return;
    }
    started = true;

    sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
    selectedTrackId = sharedPreferences.getLong(SELECTED_TRACK_KEY, -1);
    recordingTrackId = sharedPreferences.getLong(RECORDING_TRACK_KEY, -1);
    useMetricUnits = sharedPreferences.getBoolean(METRIC_UNITS_KEY, true);
    reportSpeed = sharedPreferences.getBoolean(SPEED_REPORTING_KEY, true);
    minRequiredAccuracy = sharedPreferences.getInt(MIN_REQUIRED_ACCURACY_KEY,
        Constants.DEFAULT_MIN_REQUIRED_ACCURACY);

    if (recordingTrackId > 0) {
      Intent startIntent = new Intent(context, TrackRecordingService.class);
      context.startService(startIntent);
    }

    // This may or may not register internal listeners, depending on whether
    // we already had external listeners.
    updateInternalListeners();

    // If there were listeners already registered, make sure they become up-to-date.
    // TODO: This should really only send new data (in a start-stop-start cycle).
    reloadDataFor(getRegisteredListenerArray());
  }

  /**
   * Stops listening to data sources and reporting the data to external
   * listeners.
   */
  public void stop() {
    Log.i(TAG, "TrackDataHub.stop");
    if (!started) {
      Log.w(TAG, "Not started, ignoring");
      return;
    }

    sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);

    // Unregister internal listeners even if there are external listeners registered.
    unregisterInternalListeners();

    started = false;
  }

  /** Permanently invalidates and throws away all resources used by this class. */
  public void destroy() {
    if (started) {
      throw new IllegalStateException("Can only destroy the data hub after it's been stopped");
    }

    listenerHandlerThread.quit();
  }

  /** Updates known magnetic declination if needed. */
  private void maybeUpdateDeclination() {
    if (lastSeenLocation == null) {
      // We still don't know where we are.
      return;
    }

    // Update the variation every hour
    long now = System.currentTimeMillis();
    if (now - lastDeclinationUpdate < 60 * 60 * 1000) {
      return;
    }

    lastDeclinationUpdate = now;
    long timestamp = lastSeenLocation.getTime();
    if (timestamp == 0) {
      // Hack for Samsung phones which don't populate the time field
      timestamp = now;
    }

    GeomagneticField field = new GeomagneticField(
        (float) lastSeenLocation.getLatitude(),
        (float) lastSeenLocation.getLongitude(),
        (float) lastSeenLocation.getAltitude(),
        timestamp);
    declination = field.getDeclination();
  }

  /**
   * Forces the current location to be updated and reported to all listeners.
   * The reported location may be from the network provider if the GPS provider
   * is not available or doesn't have a fix.
   */
  public void forceUpdateLocation() {
    checkStarted();

    // TODO: Let's look at more advanced algorithms to determine the best
    // current location.
    if (locationManager == null) {
      return;
    }

    final long now = System.currentTimeMillis();
    Location loc = locationManager.getLastKnownLocation(
        Constants.GPS_PROVIDER);
    if (loc == null || loc.getTime() < now - MAX_LOCATION_AGE_MS) {
      // We don't have a recent GPS fix, just use cell towers if available
      loc = locationManager.getLastKnownLocation(
          LocationManager.NETWORK_PROVIDER);
      if (loc == null || loc.getTime() < now - MAX_NETWORK_AGE_MS) {
        // We don't have a recent cell tower location, let the user know:
        Toast.makeText(context, context.getString(R.string.status_no_location),
            Toast.LENGTH_LONG).show();
        return;
      } else {
       // Let the user know we have only an approximate location:
       Toast.makeText(context, context.getString(R.string.status_approximate_location),
           Toast.LENGTH_LONG).show();
      }
    }

    notifyLocationChanged(loc, getRegisteredListenerArray());
  }

  /** Returns the ID of the currently-selected track. */
  public long getSelectedTrackId() {
    checkStarted();
    return selectedTrackId;
  }

  /** Returns whether there's a track currently selected. */
  public boolean isATrackSelected() {
    checkStarted();
    return selectedTrackId > 0;
  }

  /** Returns whether we're currently recording a track. */
  public boolean isRecording() {
    checkStarted();
    return recordingTrackId > 0;
  }

  /** Returns whether the selected track is still being recorded. */
  public boolean isRecordingSelected() {
    checkStarted();
    return isRecording() && recordingTrackId == selectedTrackId;
  }

  /**
   * Loads the given track and makes it the currently-selected one.
   * It is ok to call this method before {@link start}, and in that case
   * the data will only be passed to listeners when {@link start} is called.
   *
   * @param trackId the ID of the track to load
   */
  public void loadTrack(long trackId) {
    if (trackId == selectedTrackId) {
      Log.w(TAG, "Not reloading track, id=" + trackId);
      return;
    }

    // Save the selection to memory and flash.
    ApiFeatures.getInstance().getApiPlatformAdapter().applyPreferenceChanges(
        sharedPreferences.edit().putLong(SELECTED_TRACK_KEY, trackId));
    selectedTrackId = trackId;

    // Force it to reload data from the beginning.
    firstSeenLocationId = -1;
    lastSeenLocationId = -1;
    numLoadedPoints = 0;

    reloadDataFor(getRegisteredListenerArray());
  }

  /**
   * Unloads the currently-selected track.
   */
  public void unloadCurrentTrack() {
    loadTrack(-1);
  }

  /**
   * Registers a listener to send data to.
   * It is ok to call this method before {@link start}, and in that case
   * the data will only be passed to listeners when {@link start} is called.
   *
   * @param listener the listener to register
   */
  public void registerTrackDataListener(final TrackDataListener listener) {
    Log.d(TAG, "Registered track data listener: " + listener);
    synchronized (registeredListeners) {
      registeredListeners.add(listener);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!started) return;

      reloadDataFor(listener);
    }

    updateInternalListeners();
  }

  /**
   * Unregisters a listener to send data to.
   *
   * @param listener the listener to unregister
   */
  public void unregisterTrackDataListener(TrackDataListener listener) {
    Log.d(TAG, "Unregistered track data listener: " + listener);
    synchronized (registeredListeners) {
      registeredListeners.remove(listener);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!started) return;
    }
    updateInternalListeners();
  }

  /** Updates the internal (sensor, position, etc) listeners. */
  private void updateInternalListeners() {
    boolean hasListeners;
    synchronized (registeredListeners) {
      hasListeners = registeredListeners.isEmpty();
    }

    if (hasListeners) {
      unregisterInternalListeners();
    } else {
      registerInternalListeners();
    }
  }

  /** Registers all internal (sensor, position, etc.) listeners. */
  private void registerInternalListeners() {
    // Listen to data provider
    contentResolver.registerContentObserver(
        TracksColumns.CONTENT_URI, false, trackObserver);
    contentResolver.registerContentObserver(
        WaypointsColumns.CONTENT_URI, false, waypointObserver);
    contentResolver.registerContentObserver(
        TrackPointsColumns.CONTENT_URI, false, pointObserver);

    // Listen to compass
    Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    if (compass != null) {
      Log.d(Constants.TAG,
          "TrackDataHub: Now registering sensor listeners.");
      sensorManager.registerListener(compassListener, compass, SensorManager.SENSOR_DELAY_UI);
    }

    // Listen to GPS
    LocationProvider gpsProvider =
        locationManager.getProvider(Constants.GPS_PROVIDER);
    if (gpsProvider == null) {
      Toast.makeText(context, R.string.error_no_gps_location_provider, Toast.LENGTH_LONG).show();
      hasProviderEnabled = false;
      return;
    } else {
      Log.d(Constants.TAG, "TrackDataHub: Using location provider "
          + gpsProvider.getName());
    }
    locationManager.requestLocationUpdates(gpsProvider.getName(),
        0 /*minTime*/, 0 /*minDist*/, locationListener);
    hasProviderEnabled = locationManager.isProviderEnabled(Constants.GPS_PROVIDER);

    // Listen to network location
    try {
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
          1000 * 60 * 5 /*minTime*/, 0 /*minDist*/, locationListener);
    } catch (RuntimeException e) {
      // If anything at all goes wrong with getting a cell location do not
      // abort. Cell location is not essential to this app.
      Log.w(Constants.TAG,
          "Could not register network location listener.");
    }
  }

  /** Unregisters all internal (sensor, position, etc.) listeners. */
  private void unregisterInternalListeners() {
    locationManager.removeUpdates(locationListener);
    sensorManager.unregisterListener(compassListener);
    contentResolver.unregisterContentObserver(trackObserver);
    contentResolver.unregisterContentObserver(waypointObserver);
    contentResolver.unregisterContentObserver(pointObserver);
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   */
  public void reloadDataFor(final TrackDataListener... listeners) {
    if (listeners.length == 0) {
      Log.d(TAG, "No listeners, not reloading");
      return;
    }
    if (!started) {
      Log.w(TAG, "Not started, not reloading");
      return;
    }

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        Track track = providerUtils.getTrack(selectedTrackId);
 
        // Ignore the return values here, we're already sending the full data set anyway
        for (TrackDataListener listener : listeners) {
          listener.onUnitsChanged(useMetricUnits);
          listener.onReportSpeedChanged(reportSpeed);
        }
 
        notifySelectedTrackChanged(track, listeners);
        notifyPointsCleared(listeners);
        notifyPointsUpdated(false, listeners);
        notifyWaypointUpdated(listeners);

        if (lastSeenLocation != null) {
          notifyLocationChanged(lastSeenLocation, true, listeners);
        } else {
          notifyFixType();
        }

        notifyHeadingChanged(listeners);
      }
    });
  }

  /**
   * Called when a preference changes.
   *
   * @param key the key to the preference that changed
   */
  private void notifyPreferenceChanged(String key) {
    if (key.equals(RECORDING_TRACK_KEY)) {
      recordingTrackId = sharedPreferences.getLong(RECORDING_TRACK_KEY, -1);
    } else if (key.equals(MIN_REQUIRED_ACCURACY_KEY)) {
      minRequiredAccuracy = sharedPreferences.getInt(RECORDING_TRACK_KEY,
          Constants.DEFAULT_MIN_REQUIRED_ACCURACY);
    } else if (key.equals(METRIC_UNITS_KEY)) {
      useMetricUnits = sharedPreferences.getBoolean(METRIC_UNITS_KEY, true);
      notifyUnitsChanged();
    } else if (key.equals(SPEED_REPORTING_KEY)) {
      reportSpeed = sharedPreferences.getBoolean(SPEED_REPORTING_KEY, true);
      notifySpeedReportingChanged();
    }
  }

  /** Called when the speed/pace reporting preference changes. */
  private void notifySpeedReportingChanged() {
    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        TrackDataListener[] listeners = getRegisteredListenerArray();        

        boolean reloadData = false;
        for (TrackDataListener listener : listeners) {
          reloadData |= listener.onReportSpeedChanged(reportSpeed);
        }

        if (reloadData) {
          reloadDataFor(listeners);
        }
      }
    });
  }

  /** Called when the metric units setting changes. */
  private void notifyUnitsChanged() {
    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        TrackDataListener[] listeners = getRegisteredListenerArray();        

        boolean reloadData = false;
        for (TrackDataListener listener : listeners) {
          reloadData |= listener.onUnitsChanged(useMetricUnits);
        }

        if (reloadData) {
          reloadDataFor(listeners);
        }
      }
    });
  }

  /** Notifies about the current GPS fix state. */
  private void notifyFixType() {
    final TrackDataListener.ProviderState state;
    if (!hasProviderEnabled) {
      state = ProviderState.DISABLED;
    } else if (!hasFix) {
      state = ProviderState.NO_FIX;
    } else if (!hasGoodFix) {
      state = ProviderState.BAD_FIX;
    } else {
      state = ProviderState.GOOD_FIX;
    }

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : registeredListeners) {
          listener.onProviderStateChange(state);
        }
      }
    });
  }

  /**
   * Notifies the the current location has changed, without any filtering.
   * If the state of GPS fix has changed, that will also be reported.
   *
   * @param location the current location
   * @param listeners the listeners to notify
   */
  private void notifyLocationChanged(Location location, TrackDataListener... listeners) {
    notifyLocationChanged(location, false, listeners);
  }

  /**
   * Notifies that the current location has changed, without any filtering.
   * If the state of GPS fix has changed, that will also be reported.
   *
   * @param location the current location
   * @param forceUpdate whether to force the notifications to happen
   * @param listeners the listeners to notify
   */
  private void notifyLocationChanged(final Location location, boolean forceUpdate, final TrackDataListener... listeners) {
    if (location == null) return;

    boolean isGpsLocation = location.getProvider().equals(LocationManager.GPS_PROVIDER);

    boolean oldHasFix = hasFix;
    boolean oldHasGoodFix = hasGoodFix;

    // We consider a good fix to be a recent one with reasonable accuracy.
    if (isGpsLocation) {
      lastSeenLocation = location;
      hasFix = (location != null && System.currentTimeMillis() - location.getTime() <= MAX_LOCATION_AGE_MS);
      hasGoodFix = (location != null && location.getAccuracy() <= minRequiredAccuracy);
      if (hasFix != oldHasFix || hasGoodFix != oldHasGoodFix || forceUpdate) {
        notifyFixType();
      }
    }

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.onCurrentLocationChanged(location);
        }
      }
    });
  }

  /**
   * Notifies that the current heading has changed.
   *
   * @param listeners the listeners to notify
   */
  private void notifyHeadingChanged(final TrackDataListener... listeners) {
    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.onCurrentHeadingChanged(lastSeenMagneticHeading + declination);
        }
      }
    });
  }

  /**
   * Notifies that a new track has been selected..
   *
   * @param track the new selected track
   * @param listeners the listeners to notify
   */
  private void notifySelectedTrackChanged(final Track track, final TrackDataListener... listeners) {
    Log.d(TAG, "New track selected, id=" + (track != null ? track.getId() : "none"));

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.onSelectedTrackChanged(track, isRecordingSelected());

          if (track != null) {
            listener.onTrackUpdated(track);
          }
        }
      }
    });
  }

  /**
   * Notifies that the currently-selected track's data has been updated.
   *
   * @param listeners the listeners to notify
   */
  private void notifyTrackUpdated(final TrackDataListener... listeners) {
    final Track track = providerUtils.getTrack(selectedTrackId);

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : registeredListeners) {
          listener.onTrackUpdated(track);
        }
      }
    });
  }

  /**
   * Notifies that waypoints have been updated.
   * We assume few waypoints, so we reload them all every time.
   *
   * @param listeners the listeners to notify
   */
  private void notifyWaypointUpdated(final TrackDataListener... listeners) {
    // Always reload all the waypoints.
    final Cursor cursor = providerUtils.getWaypointsCursor(
        selectedTrackId, 0, Constants.MAX_DISPLAYED_WAYPOINTS_POINTS);

    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : registeredListeners) {
          listener.clearWaypoints();
        }

        try {
          if (cursor != null && cursor.moveToFirst()) {
            do {
              Waypoint waypoint = providerUtils.createWaypoint(cursor);
              if (!LocationUtils.isValidLocation(waypoint.getLocation())) {
                continue;
              }
    
              for (TrackDataListener listener : listeners) {
                listener.onNewWaypoint(waypoint);
              }
            } while (cursor.moveToNext());
          }
        } finally {
          if (cursor != null) {
            cursor.close();
          }
        }
    
        for (TrackDataListener listener : listeners) {
          listener.onNewWaypointsDone();
        }
      }
    });
  }

  /**
   * Tells listeners to clear the current list of points.
   *
   * @param listeners the listeners to  notify
   */
  private void notifyPointsCleared(final TrackDataListener... listeners) {
    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.clearTrackPoints();
        }
      }
    });
  }

  /**
   * Notifies the given listeners about track points in the given ID range.
   *
   * @param minPointId the first point ID to notify, inclusive
   * @param maxPointId the last poind ID to notify, inclusive
   * @param keepState whether to load and save state about the already-notified points.
   *        If true, only new points are reported.
   *        If false, then the whole track will be loaded, without affecting the store.
   * @param listeners the listeners to notify
   */
  private void notifyPointsUpdated(final boolean keepState, final TrackDataListener... listeners) {
    listenerHandler.post(new Runnable() {
      @Override
      public void run() {
        notifyPointsUpdatedSync(keepState, listeners);
      }
    });
  }

  /**
   * Asynchronous version of the above method.
   */
  private void notifyPointsUpdatedSync(boolean keepState, TrackDataListener[] listeners) {
    // If we're loading state, start from after the last seen point up to the last recorded one
    // (all new points)
    // If we're not loading state, then notify about all the previously-seen points.
    long minPointId = keepState ? lastSeenLocationId + 1 : 0;
    long maxPointId = keepState ? -1 : lastSeenLocationId;

    if (numLoadedPoints >= Constants.MAX_DISPLAYED_TRACK_POINTS) {
      // We're about to exceed the maximum allowed number of points, so reload
      // the whole track with fewer points (the sampling frequency will be
      // lower). We do this for every listener even if we were loading just for
      // a few of them (why miss the oportunity?).

      firstSeenLocationId = -1;
      lastSeenLocationId = -1;
      numLoadedPoints = 0;
      listeners = getRegisteredListenerArray();
      maxPointId = -1;
      minPointId = 0;
      keepState = true;

      for (TrackDataListener listener : listeners) {
        listener.clearTrackPoints();
      }
    }

    // Keep the originally selected track ID so we can stop if it changes.
    long currentSelectedTrackId = selectedTrackId;

    // If we're ignoring state, start from the beginning of the track
    int localNumLoadedPoints = keepState ? numLoadedPoints : 0;
    long localFirstSeenLocationId = keepState ? firstSeenLocationId : 0;
    long localLastSeenLocationId = minPointId;
    long lastStoredLocationId = providerUtils.getLastLocationId(currentSelectedTrackId);
    int pointSamplingFrequency = -1;

    // Create a double-buffering location provider.
    MyTracksProviderUtils.DoubleBufferedLocationFactory locationFactory =
        new MyTracksProviderUtils.DoubleBufferedLocationFactory();
    LocationIterator it = providerUtils.getLocationIterator(
        currentSelectedTrackId, minPointId, false, locationFactory);

    while (it.hasNext()) {
      if (currentSelectedTrackId != selectedTrackId) {
        // The selected track changed beneath us, stop.
        break;
      }

      Location location = it.next();
      long locationId = it.getLocationId();

      // If past the last wanted point, stop.
      // This happens when adding a new listener after data has already been loaded,
      // in which case we only want to bring that listener up to the point where the others
      // were. In case it does happen, we should be wasting few points (only the ones not
      // yet notified to other listeners).
      if (maxPointId > 0 && locationId > maxPointId) {
        break;
      }

      if (localFirstSeenLocationId == -1) {
        // This was our first point, keep its ID
        localFirstSeenLocationId = locationId;
      }

      if (pointSamplingFrequency == -1) {
        // Now we already have at least one point, calculate the sampling
        // frequency.
        long numTotalPoints = lastStoredLocationId - localFirstSeenLocationId;
        pointSamplingFrequency =
            (int) (1 + numTotalPoints / Constants.TARGET_DISPLAYED_TRACK_POINTS);
      }

      notifyNewPoint(location, locationId, lastStoredLocationId,
          localNumLoadedPoints, pointSamplingFrequency, listeners);

      localNumLoadedPoints++;
      localLastSeenLocationId = locationId;
    }
    it.close();

    if (keepState) {
      numLoadedPoints = localNumLoadedPoints;
      firstSeenLocationId = localFirstSeenLocationId;
      lastSeenLocationId = localLastSeenLocationId;
    }

    for (TrackDataListener listener : listeners) {
      listener.onNewTrackPointsDone();
    }
  }

  private void notifyNewPoint(Location location,
      long locationId,
      long lastStoredLocationId,
      int numLoadedPoints,
      int pointSamplingFrequency,
      TrackDataListener[] listeners) {
    boolean isValid = LocationUtils.isValidLocation(location);
    if (!isValid) {
      // Invalid points are segment splits - report those separately.
      // TODO: Always send last valid point before and first valid point after a split
      for (TrackDataListener listener : listeners) {
        listener.onSegmentSplit();
      }
      return;
    }

    // Include a point if it fits one of the following criteria:
    // - Has the mod for the sampling frequency (includes first point).
    // - Is the last point and we are not recording this track.
    boolean includeInSample =
        (numLoadedPoints % pointSamplingFrequency == 0 ||
         (!isRecordingSelected() && locationId == lastStoredLocationId));

    if (!includeInSample) {
      for (TrackDataListener listener : listeners) {
        listener.onSampledOutTrackPoint(location);
      }
      return;
    }

    // Point is valid and included in sample.
    for (TrackDataListener listener : listeners) {
      // No need to allocate a new location (we can safely reuse the existing).
      listener.onNewTrackPoint(location);
    }
  }

  /** Returns an array with all the currently-registered listeners. */
  private TrackDataListener[] getRegisteredListenerArray() {
    synchronized (registeredListeners) {
      TrackDataListener listenerArray[] = new TrackDataListener[registeredListeners.size()];
      return registeredListeners.toArray(listenerArray);
    }
  }

  /** Verifies that {@link #start} has been called. */
  private void checkStarted() {
    if (!started) {
      throw new IllegalStateException("TrackDataHub has not been started.");
    }
  }
}
