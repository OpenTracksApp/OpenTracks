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

import static com.google.android.apps.mytracks.Constants.MAX_LOCATION_AGE_MS;
import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.DataSourceManager.DataSourceListener;
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
import android.widget.Toast;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Track data hub, which receives data (both live and recorded) from many
 * different sources and distributes it to those interested after some standard
 * processing.
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
      TrackDataHub.this.notifyPointsUpdated(true, 0,
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
  private final HandlerThread listenerHandlerThread;
  private final Handler listenerHandler;

  /** Manager for external listeners (those from activities). */
  private final TrackDataListeners listeners;

  /** Wrapper for interacting with system data managers. */
  private final DataSourcesWrapper dataSources;

  /** Manager for system data listener registrations. */
  private final DataSourceManager dataSourceManager;

  /** Condensed listener for system data listener events. */
  private final DataSourceListener dataSourceListener = new HubDataSourceListener();

  /** Whether we've been started. */
  private boolean started;

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

  /**
   * Default constructor.
   */
  public TrackDataHub(Context ctx, SharedPreferences preferences,
      MyTracksProviderUtils providerUtils) {
    this(ctx, new DataSourcesWrapperImpl(ctx, preferences), new TrackDataListeners(),
         preferences, providerUtils);
  }

  /**
   * Injection constructor.
   */
  // @VisibleForTesting
  TrackDataHub(Context ctx, DataSourcesWrapper dataSources, TrackDataListeners listeners,
      SharedPreferences preferences, MyTracksProviderUtils providerUtils) {
    this.context = ctx;
    this.listeners = listeners;
    this.preferences = preferences;
    this.providerUtils = providerUtils;
    this.dataSources = dataSources;
    this.dataSourceManager = new DataSourceManager(dataSourceListener, dataSources);

    SELECTED_TRACK_KEY = context.getString(R.string.selected_track_key);
    RECORDING_TRACK_KEY = context.getString(R.string.recording_track_key);
    MIN_REQUIRED_ACCURACY_KEY = context.getString(R.string.min_required_accuracy_key);
    METRIC_UNITS_KEY = context.getString(R.string.metric_units_key);
    SPEED_REPORTING_KEY = context.getString(R.string.report_speed_key);

    listenerHandlerThread = new HandlerThread("trackDataContentThread");
    listenerHandlerThread.start();
    listenerHandler = new Handler(listenerHandlerThread.getLooper());
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

    // This may or may not register internal listeners, depending on whether
    // we already had external listeners.
    dataSourceManager.updateAllListeners(getNeededListenerTypes());
    loadSharedPreferences();

    // If there were listeners already registered, make sure they become up-to-date.
    // TODO: This should really only send new data (in a start-stop-start cycle).
    loadDataForAllListeners();
  }

  private void loadSharedPreferences() {
    selectedTrackId = preferences.getLong(SELECTED_TRACK_KEY, -1);
    useMetricUnits = preferences.getBoolean(METRIC_UNITS_KEY, true);
    reportSpeed = preferences.getBoolean(SPEED_REPORTING_KEY, true);
    minRequiredAccuracy = preferences.getInt(MIN_REQUIRED_ACCURACY_KEY,
        Constants.DEFAULT_MIN_REQUIRED_ACCURACY);
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

    // Unregister internal listeners even if there are external listeners registered.
    dataSourceManager.unregisterAllListeners();

    started = false;
  }

  /** Permanently invalidates and throws away all resources used by this class. */
  public void destroy() {
    if (started) {
      throw new IllegalStateException("Can only destroy the data hub after it's been stopped");
    }

    ApiFeatures.getInstance().getApiPlatformAdapter()
        .stopHandlerThread(listenerHandlerThread);
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
    Log.i(TAG, "Forcing location update");

    Location loc = dataSources.getLastKnownLocation();
    if (loc != null) {
      notifyLocationChanged(loc, getListenersFor(ListenerDataType.LOCATION_UPDATES));
    }
  }


  /** Returns the ID of the currently-selected track. */
  public long getSelectedTrackId() {
    if (!started) {
      loadSharedPreferences();
    }
    return selectedTrackId;
  }

  /** Returns whether there's a track currently selected. */
  public boolean isATrackSelected() {
    return getSelectedTrackId() > 0;
  }

  /** Returns whether we're currently recording a track. */
  public boolean isRecording() {
    if (!started) {
      loadSharedPreferences();
    }
    return preferences.getLong(RECORDING_TRACK_KEY, -1) > 0;
  }

  /** Returns whether the selected track is still being recorded. */
  public boolean isRecordingSelected() {
    if (!started) {
      loadSharedPreferences();
    }
    long recordingTrackId = preferences.getLong(RECORDING_TRACK_KEY, -1);
    return recordingTrackId  > 0 && recordingTrackId == selectedTrackId;
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
        preferences.edit().putLong(SELECTED_TRACK_KEY, trackId));
    selectedTrackId = trackId;

    // Force it to reload data from the beginning.
    firstSeenLocationId = -1;
    lastSeenLocationId = -1;
    numLoadedPoints = 0;

    loadDataForAllListeners();
  }

  /**
   * Unloads the currently-selected track.
   */
  public void unloadCurrentTrack() {
    loadTrack(-1);
  }

  public void registerTrackDataListener(
      TrackDataListener listener, EnumSet<ListenerDataType> dataTypes) {
    synchronized (listeners) {
      ListenerRegistration registration = listeners.registerTrackDataListener(listener, dataTypes);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!started) return;

      reloadDataForListener(registration);

      dataSourceManager.updateAllListeners(getNeededListenerTypes());
    }
  }

  public void unregisterTrackDataListener(TrackDataListener listener) {
    synchronized (listeners) {
      listeners.unregisterTrackDataListener(listener);

      // Don't load any data or start internal listeners if start() hasn't been
      // called. When it is called, we'll do both things.
      if (!started) return;

      dataSourceManager.updateAllListeners(getNeededListenerTypes());
    }
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   */
  public void reloadDataForListener(TrackDataListener listener) {
    ListenerRegistration registration;
    synchronized (listeners) {
      registration = listeners.getRegistration(listener);
    }
    reloadDataForListener(registration);
  }

  /**
   * Reloads all track data received so far into the specified listeners.
   */
  private void reloadDataForListener(final ListenerRegistration registration) {
    if (!started) {
      Log.w(TAG, "Not started, not reloading");
      return;
    }
    if (registration == null) {
      return;
    }

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
          if (reloadAll) notifyPointsCleared(listenerSet);

          notifyPointsUpdated(false,
              reloadAll ? 0 : registration.lastPointId + 1,
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
    if (!started) {
      Log.w(TAG, "Not started, not reloading");
      return;
    }
    synchronized (listeners) {
      if (!listeners.hasListeners()) {
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
        notifyPointsUpdated(true, 0, pointListeners, sampledOutPointListeners);

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
          Constants.DEFAULT_MIN_REQUIRED_ACCURACY);
    } else if (METRIC_UNITS_KEY.equals(key)) {
      useMetricUnits = preferences.getBoolean(METRIC_UNITS_KEY, true);
      notifyUnitsChanged();
    } else if (SPEED_REPORTING_KEY.equals(key)) {
      reportSpeed = preferences.getBoolean(SPEED_REPORTING_KEY, true);
      notifySpeedReportingChanged();
    }
  }

  /** Called when the speed/pace reporting preference changes. */
  private void notifySpeedReportingChanged() {
    if (!started) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        Set<TrackDataListener> displayListeners =
            getListenersFor(ListenerDataType.DISPLAY_PREFERENCES);        

        for (TrackDataListener listener : displayListeners) {
          // TODO: Do the reloading just once for all interested listeners
          if (listener.onReportSpeedChanged(reportSpeed)) {
            synchronized (listeners) {
              reloadDataForListener(listeners.getRegistration(listener));
            }
          }
        }
      }
    });
  }

  /** Called when the metric units setting changes. */
  private void notifyUnitsChanged() {
    if (!started) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        Set<TrackDataListener> displayListeners = getListenersFor(ListenerDataType.DISPLAY_PREFERENCES);        

        for (TrackDataListener listener : displayListeners) {
          if (listener.onUnitsChanged(useMetricUnits)) {
            synchronized (listeners) {
              reloadDataForListener(listeners.getRegistration(listener));
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

      // Give a global warning about this state.
      Toast.makeText(context, R.string.error_no_gps_location_provider, Toast.LENGTH_LONG).show();
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
  private void notifyLocationChanged(final Location location, boolean forceUpdate,
      final Set<TrackDataListener> listeners) {
    if (location == null) return;
    if (listeners.isEmpty()) return;

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

    runInListenerThread(new Runnable() {
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
        selectedTrackId, 0L, Constants.MAX_DISPLAYED_WAYPOINTS_POINTS);

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
   *        If false, then the whole track will be loaded, without affecting the store.
   * @param minPointId the first point ID to notify, inclusive
   */
  private void notifyPointsUpdated(final boolean keepState,
      final long minPointId, final Set<TrackDataListener> sampledListeners,
      final Set<TrackDataListener> sampledOutListeners) {
    if (sampledListeners.isEmpty() && sampledOutListeners.isEmpty()) return;

    runInListenerThread(new Runnable() {
      @Override
      public void run() {
        notifyPointsUpdatedSync(keepState, minPointId, sampledListeners, sampledOutListeners);
      }
    });
  }

  /**
   * Asynchronous version of the above method.
   */
  private void notifyPointsUpdatedSync(boolean keepState,
      long minPointId, Set<TrackDataListener> sampledListeners,
      Set<TrackDataListener> sampledOutListeners) {
    // If we're loading state, start from after the last seen point up to the last recorded one
    // (all new points)
    // If we're not loading state, then notify about all the previously-seen points.
    if (minPointId <= 0) {
      minPointId = keepState ? lastSeenLocationId + 1 : 0;
    }
    long maxPointId = keepState ? -1 : lastSeenLocationId;

    // TODO: Move (re)sampling to a separate class.
    if (numLoadedPoints >= Constants.MAX_DISPLAYED_TRACK_POINTS) {
      // We're about to exceed the maximum allowed number of points, so reload
      // the whole track with fewer points (the sampling frequency will be
      // lower). We do this for every listener even if we were loading just for
      // a few of them (why miss the oportunity?).

      Log.i(TAG, "Resampling point set after " + numLoadedPoints + " points.");
      firstSeenLocationId = -1;
      lastSeenLocationId = -1;
      numLoadedPoints = 0;
      synchronized (listeners) {
        sampledListeners = getListenersFor(ListenerDataType.POINT_UPDATES);
        sampledOutListeners = getListenersFor(ListenerDataType.SAMPLED_OUT_POINT_UPDATES);
      }
      maxPointId = -1;
      minPointId = 0;
      keepState = true;

      for (TrackDataListener listener : sampledListeners) {
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

    // Update the listener state
    // TODO: Optimize this (sampledOutListeners should be a subset of sampledListeners, plus
    //       getRegistration does a lookup for every listener, and this is in the critical path).
    updateListenersState(sampledListeners,
        currentSelectedTrackId, localLastSeenLocationId, pointSamplingFrequency);
    updateListenersState(sampledOutListeners,
        currentSelectedTrackId, localLastSeenLocationId, pointSamplingFrequency);

    for (TrackDataListener listener : sampledListeners) {
      listener.onNewTrackPointsDone();
    }
  }

  private void updateListenersState(Set<TrackDataListener> sampledListeners,
      long trackId, long lastPointId, int samplingFrequency) {
    synchronized (listeners) {
      for (TrackDataListener listener : sampledListeners) {
        ListenerRegistration registration = listeners.getRegistration(listener);
        if (registration != null) {
          registration.lastTrackId = trackId;
          registration.lastPointId = lastPointId;
          registration.lastSamplingFrequency = samplingFrequency;
        }
      }
    }
  }

  private void notifyNewPoint(Location location,
      long locationId,
      long lastStoredLocationId,
      int numLoadedPoints,
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
    boolean includeInSample =
        (numLoadedPoints % pointSamplingFrequency == 0 ||
         (!isRecordingSelected() && locationId == lastStoredLocationId));

    if (!includeInSample) {
      for (TrackDataListener listener : sampledOutListeners) {
        listener.onSampledOutTrackPoint(location);
      }
      return;
    }

    // Point is valid and included in sample.
    for (TrackDataListener listener : sampledListeners) {
      // No need to allocate a new location (we can safely reuse the existing).
      listener.onNewTrackPoint(location);
    }
  }

  // @VisibleForTesting
  protected void runInListenerThread(Runnable runnable) {
    listenerHandler.post(runnable);
  }

  private Set<TrackDataListener> getListenersFor(ListenerDataType type) {
    synchronized (listeners) {
      return listeners.getListenersFor(type);
    }
  }

  private EnumSet<ListenerDataType> getNeededListenerTypes() {
    EnumSet<ListenerDataType> neededTypes = listeners.getAllRegisteredTypes();

    // We always want preference updates.
    neededTypes.add(ListenerDataType.DISPLAY_PREFERENCES);

    return neededTypes;
  }
}
