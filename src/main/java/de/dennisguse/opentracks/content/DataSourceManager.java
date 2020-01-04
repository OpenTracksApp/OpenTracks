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

package de.dennisguse.opentracks.content;

import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import java.util.EnumSet;
import java.util.Set;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * Creates observers/listeners and manages their registration with {@link DataSource}.
 * The observers/listeners calls {@link DataSourceListener} when data changes.
 *
 * @author Rodrigo Damazio
 */
class DataSourceManager {

    private static final String TAG = DataSourceManager.class.getSimpleName();

    private final DataSource dataSource;

    private final DataSourceListener dataSourceListener;

    // Registered listeners
    private final Set<TrackDataType> registeredListeners = EnumSet.noneOf(TrackDataType.class);
    private final Handler handler;
    private final TracksTableObserver tracksTableObserver;
    private final WaypointsTableObserver waypointsTableObserver;
    private final TrackPointsTableObserver trackPointsTableObserver;

    DataSourceManager(DataSource dataSource, DataSourceListener dataSourceListener) {
        this.dataSource = dataSource;
        this.dataSourceListener = dataSourceListener;

        handler = new Handler();
        tracksTableObserver = new TracksTableObserver();
        waypointsTableObserver = new WaypointsTableObserver();
        trackPointsTableObserver = new TrackPointsTableObserver();
    }

    /**
     * Updates listeners with data source.
     *
     * @param listeners the listeners
     */
    void updateListeners(EnumSet<TrackDataType> listeners) {
        EnumSet<TrackDataType> neededListeners = EnumSet.copyOf(listeners);

        // Map SAMPLED_OUT_POINT_UPDATES to POINT_UPDATES since they correspond to the same internal listener
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
            default:
                break;
        }
    }

    /**
     * Unregisters all listeners with data source.
     */
    void unregisterAllListeners() {
        for (TrackDataType trackDataType : TrackDataType.values()) {
            unregisterListener(trackDataType);
        }
    }

    /**
     * Observer when the tracks table is updated.
     *
     * @author Jimmy Shih
     */
    private class TracksTableObserver extends ContentObserver {

        TracksTableObserver() {
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

        WaypointsTableObserver() {
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

        TrackPointsTableObserver() {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            dataSourceListener.notifyTrackPointsTableUpdated();
        }
    }
}
