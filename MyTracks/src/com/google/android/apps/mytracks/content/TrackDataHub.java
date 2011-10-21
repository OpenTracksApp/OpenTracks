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
package com.google.android.apps.mytracks.content;

import static com.google.android.apps.mytracks.Constants.DEFAULT_MIN_REQUIRED_ACCURACY;
import static com.google.android.apps.mytracks.Constants.MAX_DISPLAYED_WAYPOINTS_POINTS;
import static com.google.android.apps.mytracks.Constants.MAX_LOCATION_AGE_MS;
import static com.google.android.apps.mytracks.Constants.MAX_NETWORK_AGE_MS;
import static com.google.android.apps.mytracks.Constants.TAG;
import static com.google.android.apps.mytracks.Constants.TARGET_DISPLAYED_TRACK_POINTS;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.DataSourceManager.DataSourceListener;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.DoubleBufferedLocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.TrackDataListener.ProviderState;
import com.google.android.apps.mytracks.content.TrackDataListeners.ListenerRegistration;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Track data hub, which receives data (both live and recorded) from many
 * different sources and distributes it to those interested after some standard
 * processing.
 *
 * TODO: Simplify the threading model here, it's overly complex and it's not obvious why
 *       certain race conditions won't happen.
 *
 * @author Rodrigo Damazio
 */
public class TrackDataHub {

  // Preference keys
  private final String SELECTED_TRACK_KEY;
  private final String RECORDING_TRACK_KEY;
  private final String MIN_REQUIRED_ACCURACY_KEY;
  private final String METRIC_UNITS_KEY;
  private final String SPEED_REPORTING_KEY;

  // Overridable constants
  private final int targetNumPoints;

  /** Types of data that we can expose. */
  public static enum ListenerDataType {
    /** Listen to when the selected track changes. */
    SELECTED_TRACK_CHANGED,

    /** Listen to when the tracks change. */
    TRACK_UPDATES,

    /** Listen to when the waypoints change. */
    WAYPOINT_UPDATES,

    /** Listen to when the current track points change. */
    POINT_UPDATES,

    /**
     * Listen to sampled-out points.
     * Listening to this without listening to {@link #SAMPLED_POINT_UPDATES}
     * makes no sense and may yield unexpected results.
     */
    SAMPLED_OUT_POINT_UPDATES,

    /** Listen to updates to the current location. */
    LOCATION_UPDATES,

    /** Listen to updates to the current heading. */
    COMPASS_UPDATES,

    /** Listens to changes in display preferences. */
    DISPLAY_PREFERENCES;
  }

  /** Listener which receives events from the system. */
  private class HubDataSourceListener implements DataSourceListener {
    @Override
    public void notifyTrackUpdated() {
      TrackDataHub.this.notifyTrackUpdated(getListenersFor(ListenerDataType.TRACK_UPDATES));
    }

    @Override
    public void notifyWaypointUpdated() {
      TrackDataHub.this.notifyWaypointUpdated(getListenersFor(ListenerDataType.WAYPOINT_UPDATES));
    }

    @Override
    public void notifyPointsUpdated() {
      TrackDataHub.this.notifyPointsUpdated(true, 0, 0,
          getListenersFor(ListenerDataType.POINT_UPDATES),
          getListenersFor(ListenerDataType.SAMPLED_OUT_POINT_UPDATES));
    }

    @Override
    public void notifyPreferenceChanged(String key) {
      TrackDataHub.this.notifyPreferenceChanged(key);
    }

    @Override
    public void notifyLocationProviderEnabled(boolean enabled) {
      hasProviderEnabled = enabled;
      TrackDataHub.this.notifyFixType();
    }

    @Override
    public void notifyLocationProviderAvailable(boolean available) {
      hasFix = available;
      TrackDataHub.this.notifyFixType();
    }

    @Override
    public void notifyLocationChanged(Location loc) {
      TrackDataHub.this.notifyLocationChanged(loc,
          getListenersFor(ListenerDataType.LOCATION_UPDATES));
    }

    @Override
    public void notifyHeadingChanged(float heading) {
      lastSeenMagneticHeading = heading;
      maybeUpdateDeclination();
      TrackDataHub.this.notifyHeadingChanged(getListenersFor(ListenerDataType.COMPASS_UPDATES));
    }
  }

  // Application services
  private final Context context;
  private final MyTracksProviderUtils providerUtils;
  private final SharedPreferences preferences;

  // Get content notifications on the main thread, send listener callbacks in another.
  // This ensures listener calls are serialized.
  private HandlerThread listenerHandlerThread;
  private Handler listenerHandler;

  /** Manager for external listeners (those from activities). */
  private final TrackDataListeners dataListeners;

  /** Wrapper for interacting with system data managers. */
  private DataSourcesWrapper dataSources;

  /** Manager for system data listener registrations. */
  private DataSourceManager dataSourceManager;

  /** Condensed listener for system data listener events. */
  private final DataSourceListener dataSourceListener = new HubDataSourceListener();

  // Cached preference values
  private int minRequiredAccuracy;
  private boolean useMetricUnits;
  private boolean reportSpeed;

  // Cached sensor readings
  private float declination;
  private long lastDeclinationUpdate;
  private float lastSeenMagneticHeading;

  // Cached GPS readings
  private Location lastSeenLocation;
  private boolean hasProviderEnabled = true;
  private boolean hasFix;
  private boolean hasGoodFix;

  // Transient state about the selected track
  private long selectedTrackId;
  private long firstSeenLocationId;
  private long lastSeenLocationId;
  private int numLoadedPoints;
  private int lastSamplingFrequency;
  private DoubleBufferedLocationFactory locationFactory;

  private boolean started = false;
  
  /**
   * Builds a new {@link TrackDataHub} instance.
   */
  public synchronized static TrackDataHub newInstance(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    MyTracksProviderUtils providerUtils = MyTracksProviderUtils.Factory.get(context);
    return new TrackDataHub(context,
        new TrackDataListeners(),
        preferences, providerUtils,
        TARGET_DISPLAYED_TRACK_POINTS);
  }

  /**
   * Injection constructor.
   */
  // @VisibleForTesting
  TrackDataHub(Context ctx, TrackDataListeners listeners, SharedPreferences preferences,
      MyTracksProviderUtils providerUtils, int targetNumPoints) {
    this.context = ctx;
    this.dataListeners = listeners;
    this.preferences = preferences;
    this.providerUtils = providerUtils;
    this.targetNumPoints = targetNumPoints;
    this.locationFactory = new DoubleBufferedLocationFactory();

    SELECTED_TRACK_KEY = context.getString(R.string.selected_track_key);
    RECORDING_TRACK_KEY = context.getString(R.string.recording_track_key);
    MIN_REQUIRED_ACCURACY_KEY = context.getString(R.string.min_required_accuracy_key);
    METRIC_UNITS_KEY = context.getString(R.string.metric_units_key);
    SPEED_REPORTING_KEY = context.getString(R.string.report_speed_key);

    resetState();
  }

  /**
   * Starts listening to data sources and reporting the data to external
   * listeners.
   */
  public void start() {
    Log.i(TAG, "TrackDataHub.start");
    if (isStarted()) {
      Log.w(TAG, "Already started, ignoring");
      return;
    }
    started = true;

    listenerHandlerThread = new HandlerThread("trackDataContentThread");
    listenerHandlerThread.start();
    listenerHandler = new Handler(listenerHandlerThread.getLooper());
    dataSources = newDataSources();
    dataSourceManager = new DataSourceManager(dataSourceListener, dataSources);

    // This may or may not register internal listeners, depending on whether
    // we already had external listeners.
    dataSourceManager.updateAllListeners(getNeededListenerTypes());
    loadSharedPreferences();

    // If there were listeners already registered, make sure they become up-to-date.
    loadDataForAllListeners();
  }

  // @VisibleForTesting
  protected DataSourcesWrapper newDataSources() {
    return new DataSourcesWrapperImpl(context, preferences);
  }

  /**
   * Stops listening to data sources and reporting the data to external
   * listeners.
   */
  public void stop() {
    Log.i(TAG, "TrackDataHub.stop");
    if (!isStarted()) {
      Log.w(TAG, "Not started, ignoring");
      return;
    }
    
    // Unregister internal listeners even if there are external listeners registered.
    dataSourceManager.unregisterAllListeners();
    listenerHandlerThread.getLooper().quit();

    started = false;

    dataSources = null;
    dataSourceManager = null;
    listenerHandlerThread = null;
    listenerHandler = null;
  }

  private boolean isStarted() {
    return started;
  }

  @Override
  protected void finalize() throws Throwable {
    if (isStarted() || listenerHandlerThread.isAlive()) {
      Log.e(TAG, "Forgot to stop() TrackDataHub");
    }

    super.finalize();
  }

  private void loadSharedPreferences() {
    selectedTrackId = preferences.getLong(SELECTED_TRACK_KEY, -1);
    useMetricUnits = preferences.getBoolean(METRIC_UNITS_KEY, true);
    reportSpeed = preferences.getBoolean(SPEED_REPORTING_KEY, true);
    minRequiredAccuracy = preferences.getInt(MIN_REQUIRED_ACCURACY_KEY,
        DEFAULT_MIN_REQUIRED_ACCURACY);
  }

  /** Updates known magnetic declination if needed. */
  private void maybeUpdateDeclination() {
    if (lastSeenLocation == null) {
      // We still don't know where we are.
      return;
    }

    // Update the declination every hour
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

    declination = getDeclinationFor(lastSeenLocation, timestamp);
    Log.i(TAG, "Updated magnetic declination to " + declination);
  }

  // @VisibleForTesting
  protected float getDeclinationFor(Location location, long timestamp) {
    GeomagneticField field = new GeomagneticField(
        (float) location.getLatitude(),
        (float) location.getLongitude(),
        (float) location.getAltitude(),
        timestamp);
    return field.getDeclination();
  }

  /**
   * Forces the current location to be updated and reported to all listeners.
   * The reported location may be from the network provider if the GPS provider
   * is not available or doesn't have a fix.
   */
  public void forceUpdateLocation() {
    if (!isStarted()) {
      Log.w(TAG, "Not started, not forcing location update");
      return;
    }
    Log.i(TAG, "Forcing location update");

    Location loc = dataSources.getLastKnownLocation();
    if (loc != null) {
      notifyLocationChanged(loc, getListenersFor(ListenerDataType.LOCATION_UPDATES));
    }
  }


  /** Returns the ID of the currently-selected track. */
  public long getSelectedTrackId() {
    if (!isStarted()) {
      loadSharedPreferences();
    }
    return selectedTrackId;
  }

  /** Returns whether there's a track currently selected. */
  public boolean isATrackSelected() {
    return getSelectedTrackId() > 0;
  }

  /** Returns whether the selected track is still being recorded. */
  public boolean isRecordingSelected() {
    if (!isStarted()) {
      loadSharedPreferences();
    }
    long recordingTrackId = preferences.getLong(RECORDING_TRACK_KEY, -1);
    return recordingTrackId > 0 && recordingTrackId == selectedTrackId;
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

    // Save the selection to memory and flush.
    selectedTrackId = trackId;
    ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(
        preferences.edit().putLong(SELECTED_TRACK_KEY, trackId));

    // Force it to reload data from the beginning.
    Log.d(TAG, "Loading track");
    resetState();

    loadDataForAllListeners();
  }

  /**
   * Resets the internal state of what data has already been loaded into listeners.
   */
  private void resetState() {
    firstSeenLocationId = -1;
    lastSeenLocationId = -1;
    numLoadedPoints = 0;
    lastSamplingFrequency = -1;
  }

  /**
   * Unloads the currently-selected track.
   */
  public void unloadCurrentTrack() {
    loadTrack(-1);
  }

  public void registerTrackDataListener(
      TrackDataListener listener, EnumSet<ListenerDataType> dataTypes) {
    synchronized (dataListeners) {
      ListenerRegistration registration =
          dataListeners.registerTrackDataListener(listener, dataTypes);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!isStarted()) return;

      loadNewDataForListener(registration);

      dataSourceManager.updateAllListeners(getNeededListenerTypes());
    }
  }

  public void unregisterTrackDataListener(TrackDataListener listener) {
    synchronized (dataListeners) {
      dataListeners.unregisterTrackDataListener(listener);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!isStarted()) return;

      dataSourceManager.updateAllListeners(getNeededListenerTypes());
    }
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   */
  public void reloadDataForListener(TrackDataListener listener) {
    ListenerRegistration registration;
    synchronized (dataListeners) {
      registration = dataListeners.getRegistration(listener);
      registration.resetState();
      loadNewDataForListener(registration);
    }
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   *
   * Assumes it's called from a block that synchronizes on {@link #dataListeners}.
   */
  private void loadNewDataForListener(final ListenerRegistration registration) {
    if (!isStarted()) {
      Log.w(TAG, "Not started, not reloading");
      return;
    }
    if (registration == null) {
      Log.w(TAG, "Not reloading for null registration");
      return;
    }

    // If a listener happens to be added after this method but before the Runnable below is
    // executed, it will have triggered a separate call to load data only up to the point this
    // listener got to. This is ensured by being synchronized on listeners.
    final boolean isOnlyListener = (dataListeners.getNumListeners() == 1);

    runInListenerThread(new Runnable() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        // Reload everything if either it's a different track, or the track has been resampled
        // (this also covers the case of a new registration).
        boolean reloadAll = registration.lastTrackId != selectedTrackId ||
                            registration.lastSamplingFrequency != lastSamplingFrequency;
        Log.d(TAG, "Doing a " + (reloadAll ? "full" : "partial") + " reload for " + registration);

        TrackDataListener listener = registration.listener;
        Set<TrackDataListener> listenerSet = Collections.singleton(listener);

        if (registration.isInterestedIn(ListenerDataType.DISPLAY_PREFERENCES)) {
          reloadAll |= listener.onUnitsChanged(useMetricUnits);
          reloadAll |= listener.onReportSpeedChanged(reportSpeed);
        }

        if (reloadAll && registration.isInterestedIn(ListenerDataType.SELECTED_TRACK_CHANGED)) {
          notifySelectedTrackChanged(selectedTrackId, listenerSet);
        }

        if (registration.isInterestedIn(ListenerDataType.TRACK_UPDATES)) {
          notifyTrackUpdated(listenerSet);
        }

        boolean interestedInPoints =
            registration.isInterestedIn(ListenerDataType.POINT_UPDATES);
        boolean interestedInSampledOutPoints =
            registration.isInterestedIn(ListenerDataType.SAMPLED_OUT_POINT_UPDATES);
        if (interestedInPoints || interestedInSampledOutPoints) {
          long minPointId = 0;
          int previousNumPoints = 0;

          if (reloadAll) {
            // Clear existing points and send them all again
            notifyPointsCleared(listenerSet);
          } else {
            // Send only new points
            minPointId = registration.lastPointId + 1;
            previousNumPoints = registration.numLoadedPoints;
          }

          // If this is the only listener we have registered, keep the state that we serve to it as
          // a reference for other future listeners.
          if (isOnlyListener && reloadAll) {
            resetState();
          }

          notifyPointsUpdated(isOnlyListener,
              minPointId,
              previousNumPoints,
              listenerSet,
              interestedInSampledOutPoints ? listenerSet : Collections.EMPTY_SET);
        }

        if (registration.isInterestedIn(ListenerDataType.WAYPOINT_UPDATES)) {
          notifyWaypointUpdated(listenerSet);
        }

        if (registration.isInterestedIn(ListenerDataType.LOCATION_UPDATES)) {
          if (lastSeenLocation != null) {
            notifyLocationChanged(lastSeenLocation, true, listenerSet);
          } else {
            notifyFixType();
          }
        }

        if (registration.isInterestedIn(ListenerDataType.COMPASS_UPDATES)) {
          notifyHeadingChanged(listenerSet);
        }
      }
    });
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   */
  private void loadDataForAllListeners() {
    if (!isStarted()) {
      Log.w(TAG, "Not started, not reloading");
      return;
    }
    synchronized (dataListeners) {
      if (!dataListeners.hasListeners()) {
        Log.d(TAG, "No listeners, not reloading");
        return;
      }
    }

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        // Ignore the return values here, we're already sending the full data set anyway
        for (TrackDataListener listener :
             getListenersFor(ListenerDataType.DISPLAY_PREFERENCES)) {
          listener.onUnitsChanged(useMetricUnits);
          listener.onReportSpeedChanged(reportSpeed);
        }

        notifySelectedTrackChanged(selectedTrackId,
            getListenersFor(ListenerDataType.SELECTED_TRACK_CHANGED));

        notifyTrackUpdated(getListenersFor(ListenerDataType.TRACK_UPDATES));

        Set<TrackDataListener> pointListeners =
            getListenersFor(ListenerDataType.POINT_UPDATES);
        Set<TrackDataListener> sampledOutPointListeners =
            getListenersFor(ListenerDataType.SAMPLED_OUT_POINT_UPDATES);
        notifyPointsCleared(pointListeners);
        notifyPointsUpdated(true, 0, 0, pointListeners, sampledOutPointListeners);

        notifyWaypointUpdated(getListenersFor(ListenerDataType.WAYPOINT_UPDATES));

        if (lastSeenLocation != null) {
          notifyLocationChanged(lastSeenLocation, true,
              getListenersFor(ListenerDataType.LOCATION_UPDATES));
        } else {
          notifyFixType();
        }

        notifyHeadingChanged(getListenersFor(ListenerDataType.COMPASS_UPDATES));
      }
    });
  }

  /**
   * Called when a preference changes.
   *
   * @param key the key to the preference that changed
   */
  private void notifyPreferenceChanged(String key) {
    if (MIN_REQUIRED_ACCURACY_KEY.equals(key)) {
      minRequiredAccuracy = preferences.getInt(MIN_REQUIRED_ACCURACY_KEY,
          DEFAULT_MIN_REQUIRED_ACCURACY);
    } else if (METRIC_UNITS_KEY.equals(key)) {
      useMetricUnits = preferences.getBoolean(METRIC_UNITS_KEY, true);
      notifyUnitsChanged();
    } else if (SPEED_REPORTING_KEY.equals(key)) {
      reportSpeed = preferences.getBoolean(SPEED_REPORTING_KEY, true);
      notifySpeedReportingChanged();
    } else if (SELECTED_TRACK_KEY.equals(key)) {
      long trackId = preferences.getLong(SELECTED_TRACK_KEY, -1);
      loadTrack(trackId);
    }
  }

  /** Called when the speed/pace reporting preference changes. */
  private void notifySpeedReportingChanged() {
    if (!isStarted()) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        Set<TrackDataListener> displayListeners =
            getListenersFor(ListenerDataType.DISPLAY_PREFERENCES);

        for (TrackDataListener listener : displayListeners) {
          // TODO: Do the reloading just once for all interested listeners
          if (listener.onReportSpeedChanged(reportSpeed)) {
            synchronized (dataListeners) {
              reloadDataForListener(listener);
            }
          }
        }
      }
    });
  }

  /** Called when the metric units setting changes. */
  private void notifyUnitsChanged() {
    if (!isStarted()) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        Set<TrackDataListener> displayListeners = getListenersFor(ListenerDataType.DISPLAY_PREFERENCES);

        for (TrackDataListener listener : displayListeners) {
          if (listener.onUnitsChanged(useMetricUnits)) {
            synchronized (dataListeners) {
              reloadDataForListener(listener);
            }
          }
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

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        // Notify to everyone.
        Log.d(TAG, "Notifying fix type: " + state);
        for (TrackDataListener listener :
             getListenersFor(ListenerDataType.LOCATION_UPDATES)) {
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
  private void notifyLocationChanged(Location location, Set<TrackDataListener> listeners) {
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
  private void notifyLocationChanged(Location location, boolean forceUpdate,
      final Set<TrackDataListener> listeners) {
    if (location == null) return;
    if (listeners.isEmpty()) return;

    boolean isGpsLocation = location.getProvider().equals(LocationManager.GPS_PROVIDER);

    boolean oldHasFix = hasFix;
    boolean oldHasGoodFix = hasGoodFix;

    long now = System.currentTimeMillis();
    if (isGpsLocation) {
      // We consider a good fix to be a recent one with reasonable accuracy.
      hasFix = !isLocationOld(location, now, MAX_LOCATION_AGE_MS);
      hasGoodFix = (location.getAccuracy() <= minRequiredAccuracy);
    } else {
      if (!isLocationOld(lastSeenLocation, now, MAX_LOCATION_AGE_MS)) {
        // This is a network location, but we have a recent/valid GPS location, just ignore this.
        return;
      }

      // We haven't gotten a GPS location in a while (or at all), assume we have no fix anymore.
      hasFix = false;
      hasGoodFix = false;

      // If the network location is recent, we'll use that.
      if (isLocationOld(location, now, MAX_NETWORK_AGE_MS)) {
        // Alas, we have no clue where we are.
        location = null;
      }
    }

    if (hasFix != oldHasFix || hasGoodFix != oldHasGoodFix || forceUpdate) {
      notifyFixType();
    }

    lastSeenLocation = location;
    final Location finalLoc = location;
    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.onCurrentLocationChanged(finalLoc);
        }
      }
    });
  }

  /**
   * Returns true if the given location is either invalid or too old.
   *
   * @param location the location to test
   * @param now the current timestamp in milliseconds
   * @param maxAge the maximum age in milliseconds
   * @return true if it's invalid or too old, false otherwise
   */
  private static boolean isLocationOld(Location location, long now, long maxAge) {
    return !LocationUtils.isValidLocation(location) || now - location.getTime() > maxAge;
  }

  /**
   * Notifies that the current heading has changed.
   *
   * @param listeners the listeners to notify
   */
  private void notifyHeadingChanged(final Set<TrackDataListener> listeners) {
    if (listeners.isEmpty()) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        float heading = lastSeenMagneticHeading + declination;
        for (TrackDataListener listener : listeners) {
          listener.onCurrentHeadingChanged(heading);
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
  private void notifySelectedTrackChanged(long trackId,
      final Set<TrackDataListener> listeners) {
    if (listeners.isEmpty()) return;

    Log.i(TAG, "New track selected, id=" + trackId);
    final Track track = providerUtils.getTrack(trackId);

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
          listener.onSelectedTrackChanged(track, isRecordingSelected());
        }
      }
    });
  }

  /**
   * Notifies that the currently-selected track's data has been updated.
   *
   * @param listeners the listeners to notify
   */
  private void notifyTrackUpdated(final Set<TrackDataListener> listeners) {
    if (listeners.isEmpty()) return;

    final Track track = providerUtils.getTrack(selectedTrackId);

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        for (TrackDataListener listener : listeners) {
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
  private void notifyWaypointUpdated(final Set<TrackDataListener> listeners) {
    if (listeners.isEmpty()) return;

    // Always reload all the waypoints.
    final Cursor cursor = providerUtils.getWaypointsCursor(
        selectedTrackId, 0L, MAX_DISPLAYED_WAYPOINTS_POINTS);

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Reloading waypoints");
        for (TrackDataListener listener : listeners) {
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
  private void notifyPointsCleared(final Set<TrackDataListener> listeners) {
    if (listeners.isEmpty()) return;

    runInListenerThread(new Runnable() {
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
   * @param keepState whether to load and save state about the already-notified points.
   *        If true, only new points are reported.
   *        If false, then the whole track will be loaded, without affecting the state.
   * @param minPointId the first point ID to notify, inclusive, or 0 to determine from
   *        internal state
   * @param previousNumPoints the number of points to assume were previously loaded for
   *        these listeners, or 0 to assume it's the kept state
   */
  private void notifyPointsUpdated(final boolean keepState,
      final long minPointId, final int previousNumPoints,
      final Set<TrackDataListener> sampledListeners,
      final Set<TrackDataListener> sampledOutListeners) {
    if (sampledListeners.isEmpty() && sampledOutListeners.isEmpty()) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        notifyPointsUpdatedSync(keepState, minPointId, previousNumPoints, sampledListeners, sampledOutListeners);
      }
    });
  }

  /**
   * Synchronous version of the above method.
   */
  private void notifyPointsUpdatedSync(boolean keepState,
      long minPointId, int previousNumPoints,
      Set<TrackDataListener> sampledListeners,
      Set<TrackDataListener> sampledOutListeners) {
    // If we're loading state, start from after the last seen point up to the last recorded one
    // (all new points)
    // If we're not loading state, then notify about all the previously-seen points.
    if (minPointId <= 0) {
      minPointId = keepState ? lastSeenLocationId + 1 : 0;
    }
    long maxPointId = keepState ? -1 : lastSeenLocationId;

    // TODO: Move (re)sampling to a separate class.
    if (numLoadedPoints >= targetNumPoints) {
      // We're about to exceed the maximum desired number of points, so reload
      // the whole track with fewer points (the sampling frequency will be
      // lower). We do this for every listener even if we were loading just for
      // a few of them (why miss the oportunity?).

      Log.i(TAG, "Resampling point set after " + numLoadedPoints + " points.");
      resetState();
      synchronized (dataListeners) {
        sampledListeners = getListenersFor(ListenerDataType.POINT_UPDATES);
        sampledOutListeners = getListenersFor(ListenerDataType.SAMPLED_OUT_POINT_UPDATES);
      }
      maxPointId = -1;
      minPointId = 0;
      previousNumPoints = 0;
      keepState = true;

      for (TrackDataListener listener : sampledListeners) {
        listener.clearTrackPoints();
      }
    }

    // Keep the originally selected track ID so we can stop if it changes.
    long currentSelectedTrackId = selectedTrackId;

    // If we're ignoring state, start from the beginning of the track
    int localNumLoadedPoints = previousNumPoints;
    if (previousNumPoints <= 0) {
      localNumLoadedPoints = keepState ? numLoadedPoints : 0;
    }
    long localFirstSeenLocationId = keepState ? firstSeenLocationId : -1;
    long localLastSeenLocationId = minPointId;
    long lastStoredLocationId = providerUtils.getLastLocationId(currentSelectedTrackId);
    int pointSamplingFrequency = -1;

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
        // It should be noted that a non-obvious consequence of this sampling is that
        // no matter how many points we get in the newest batch, we'll never exceed
        // MAX_DISPLAYED_TRACK_POINTS = 2 * TARGET_DISPLAYED_TRACK_POINTS before resampling.
        long numTotalPoints = lastStoredLocationId - localFirstSeenLocationId;
        numTotalPoints = Math.max(0L, numTotalPoints);
        pointSamplingFrequency = (int) (1 + numTotalPoints / targetNumPoints);
      }

      notifyNewPoint(location, locationId, lastStoredLocationId,
          localNumLoadedPoints, pointSamplingFrequency, sampledListeners, sampledOutListeners);

      localNumLoadedPoints++;
      localLastSeenLocationId = locationId;
    }
    it.close();

    if (keepState) {
      numLoadedPoints = localNumLoadedPoints;
      firstSeenLocationId = localFirstSeenLocationId;
      lastSeenLocationId = localLastSeenLocationId;
    }

    // Always keep the sampling frequency - if it changes we'll do a full reload above anyway.
    lastSamplingFrequency = pointSamplingFrequency;

    for (TrackDataListener listener : sampledListeners) {
      listener.onNewTrackPointsDone();

      // Update the listener state
      ListenerRegistration registration = dataListeners.getRegistration(listener);
      if (registration != null) {
        registration.lastTrackId = currentSelectedTrackId;
        registration.lastPointId = localLastSeenLocationId;
        registration.lastSamplingFrequency = pointSamplingFrequency;
        registration.numLoadedPoints = localNumLoadedPoints;
      }
    }
  }

  private void notifyNewPoint(Location location,
      long locationId,
      long lastStoredLocationId,
      int loadedPoints,
      int pointSamplingFrequency,
      Set<TrackDataListener> sampledListeners,
      Set<TrackDataListener> sampledOutListeners) {
    boolean isValid = LocationUtils.isValidLocation(location);
    if (!isValid) {
      // Invalid points are segment splits - report those separately.
      // TODO: Always send last valid point before and first valid point after a split
      for (TrackDataListener listener : sampledListeners) {
        listener.onSegmentSplit();
      }
      return;
    }

    // Include a point if it fits one of the following criteria:
    // - Has the mod for the sampling frequency (includes first point).
    // - Is the last point and we are not recording this track.
    boolean recordingSelected = isRecordingSelected();
    boolean includeInSample =
        (loadedPoints % pointSamplingFrequency == 0 ||
         (!recordingSelected && locationId == lastStoredLocationId));

    if (!includeInSample) {
      for (TrackDataListener listener : sampledOutListeners) {
        listener.onSampledOutTrackPoint(location);
      }
    } else {
      // Point is valid and included in sample.
      for (TrackDataListener listener : sampledListeners) {
        // No need to allocate a new location (we can safely reuse the existing).
        listener.onNewTrackPoint(location);
      }
    }
  }

  // @VisibleForTesting
  protected void runInListenerThread(Runnable runnable) {
    if (listenerHandler == null) {
      // Use a Throwable to ensure the stack trace is logged.
      Log.e(TAG, "Tried to use listener thread before start()", new Throwable());
      return;
    }

    listenerHandler.post(runnable);
  }

  private Set<TrackDataListener> getListenersFor(ListenerDataType type) {
    synchronized (dataListeners) {
      return dataListeners.getListenersFor(type);
    }
  }

  private EnumSet<ListenerDataType> getNeededListenerTypes() {
    EnumSet<ListenerDataType> neededTypes = dataListeners.getAllRegisteredTypes();

    // We always want preference updates.
    neededTypes.add(ListenerDataType.DISPLAY_PREFERENCES);

    return neededTypes;
  }
}
