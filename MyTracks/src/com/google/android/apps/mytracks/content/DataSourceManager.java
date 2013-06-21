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

import static com.google.android.apps.mytracks.Constants.TAG;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import java.util.EnumSet;
import java.util.Set;

/**
 * Data source manager. Creates observers/listeners and manages their
 * registration with {@link DataSource}. The observers/listeners calls
 * {@link DataSourceListener} when data changes.
 * 
 * @author Rodrigo Damazio
 */
public class DataSourceManager {

  /**
   * Observer when the tracks table is updated.
   * 
   * @author Jimmy Shih
   */
  private class TracksTableObserver extends ContentObserver {

    public TracksTableObserver() {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      dataSourceListener.notifyTracksTableUpdated();
    }
  }

  /**
   * Observer when the waypoints table is updated.
   * 
   * @author Jimmy Shih
   */
  private class WaypointsTableObserver extends ContentObserver {

    public WaypointsTableObserver() {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      dataSourceListener.notifyWaypointsTableUpdated();
    }
  }

  /**
   * Observer when the track points table is updated.
   * 
   * @author Jimmy Shih
   */
  private class TrackPointsTableObserver extends ContentObserver {

    public TrackPointsTableObserver() {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      dataSourceListener.notifyTrackPointsTableUpdated();
    }
  }

  /**
   * Listener for preference changes.
   * 
   * @author Jimmy Shih
   */
  private class PreferenceListener implements OnSharedPreferenceChangeListener {

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      dataSourceListener.notifyPreferenceChanged(key);
    }
  }

  private final DataSource dataSource;
  private final DataSourceListener dataSourceListener;

  // Registered listeners
  private final Set<TrackDataType> registeredListeners = EnumSet.noneOf(TrackDataType.class);
  
  private final Handler handler;
  private final TracksTableObserver tracksTableObserver;
  private final WaypointsTableObserver waypointsTableObserver;
  private final TrackPointsTableObserver trackPointsTableObserver;
  private final PreferenceListener preferenceListener;

  public DataSourceManager(DataSource dataSource, DataSourceListener dataSourceListener) {
    this.dataSource = dataSource;
    this.dataSourceListener = dataSourceListener;

    handler = new Handler();
    tracksTableObserver = new TracksTableObserver();
    waypointsTableObserver = new WaypointsTableObserver();
    trackPointsTableObserver = new TrackPointsTableObserver();
    preferenceListener = new PreferenceListener();
  }

  /**
   * Updates listeners with data source.
   * 
   * @param listeners the listeners
   */
  public void updateListeners(EnumSet<TrackDataType> listeners) {
    EnumSet<TrackDataType> neededListeners = EnumSet.copyOf(listeners);

    /*
     * Map SAMPLED_OUT_POINT_UPDATES to POINT_UPDATES since they correspond to
     * the same internal listener
     */
    if (neededListeners.contains(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE)) {
      neededListeners.remove(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE);
      neededListeners.add(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE);
    }

    Log.d(TAG, "Updating listeners " + neededListeners);

    // Unnecessary = registered - needed
    Set<TrackDataType> unnecessaryListeners = EnumSet.copyOf(registeredListeners);
    unnecessaryListeners.removeAll(neededListeners);

    // Missing = needed - registered
    Set<TrackDataType> missingListeners = EnumSet.copyOf(neededListeners);
    missingListeners.removeAll(registeredListeners);

    // Remove unnecessary listeners
    for (TrackDataType trackDataType : unnecessaryListeners) {
      unregisterListener(trackDataType);
    }

    // Add missing listeners
    for (TrackDataType trackDataType : missingListeners) {
      registerListener(trackDataType);
    }

    // Update registered listeners
    registeredListeners.clear();
    registeredListeners.addAll(neededListeners);
  }

  /**
   * Registers a listener with data source.
   * 
   * @param trackDataType the listener data type
   */
  private void registerListener(TrackDataType trackDataType) {
    switch (trackDataType) {
      case TRACKS_TABLE:
        dataSource.registerContentObserver(TracksColumns.CONTENT_URI, tracksTableObserver);
        break;
      case WAYPOINTS_TABLE:
        dataSource.registerContentObserver(WaypointsColumns.CONTENT_URI, waypointsTableObserver);
        break;
      case SAMPLED_IN_TRACK_POINTS_TABLE:
        dataSource.registerContentObserver(
            TrackPointsColumns.CONTENT_URI, trackPointsTableObserver);
        break;
      case SAMPLED_OUT_TRACK_POINTS_TABLE:
        // Do nothing. SAMPLED_OUT_POINT_UPDATES is mapped to POINT_UPDATES.
        break;
      case PREFERENCE:
        dataSource.registerOnSharedPreferenceChangeListener(preferenceListener);
        break;
      default:
        break;
    }
  }

  /**
   * Unregisters a listener with data source.
   * 
   * @param trackDataType listener data type
   */
  private void unregisterListener(TrackDataType trackDataType) {
    switch (trackDataType) {
      case TRACKS_TABLE:
        dataSource.unregisterContentObserver(tracksTableObserver);
        break;
      case WAYPOINTS_TABLE:
        dataSource.unregisterContentObserver(waypointsTableObserver);
        break;
      case SAMPLED_IN_TRACK_POINTS_TABLE:
        dataSource.unregisterContentObserver(trackPointsTableObserver);
        break;
      case SAMPLED_OUT_TRACK_POINTS_TABLE:
        // Do nothing. SAMPLED_OUT_POINT_UPDATES is mapped to POINT_UPDATES.
        break;
      case PREFERENCE:
        dataSource.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        break;
      default:
        break;
    }
  }

  /**
   * Unregisters all listeners with data source.
   */
  public void unregisterAllListeners() {
    for (TrackDataType trackDataType : TrackDataType.values()) {
      unregisterListener(trackDataType);
    }
  }
}
