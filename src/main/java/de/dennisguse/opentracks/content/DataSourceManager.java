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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import java.util.EnumSet;
import java.util.Set;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * Creates observers/listeners and manages their registration.
 * The observers/listeners calls {@link DataSourceListener} when data changes.
 *
 * @author Rodrigo Damazio
 */
class DataSourceManager {

    private static final String TAG = DataSourceManager.class.getSimpleName();

    private final ContentResolver contentResolver;

    // Registered listeners
    private final Set<TrackDataType> registeredListeners = EnumSet.noneOf(TrackDataType.class);
    private final ContentObserver tracksTableObserver;
    private final ContentObserver waypointsTableObserver;
    private final ContentObserver trackPointsTableObserver;

    DataSourceManager(Context context, final DataSourceListener dataSourceListener) {
        contentResolver = context.getContentResolver();

        Handler handler = new Handler();
        tracksTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                dataSourceListener.notifyTracksTableUpdated();
            }
        };
        waypointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                dataSourceListener.notifyWaypointsTableUpdated();
            }
        };
        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                dataSourceListener.notifyTrackPointsTableUpdated();
            }
        };
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
                contentResolver.registerContentObserver(TracksColumns.CONTENT_URI, false, tracksTableObserver);
                break;
            case WAYPOINTS_TABLE:
                contentResolver.registerContentObserver(WaypointsColumns.CONTENT_URI, false, waypointsTableObserver);
                break;
            case SAMPLED_IN_TRACK_POINTS_TABLE:
                contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);
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
                contentResolver.unregisterContentObserver(tracksTableObserver);
                break;
            case WAYPOINTS_TABLE:
                contentResolver.unregisterContentObserver(waypointsTableObserver);
                break;
            case SAMPLED_IN_TRACK_POINTS_TABLE:
                contentResolver.unregisterContentObserver(trackPointsTableObserver);
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
     * Listener to be invoked when observed data changes changes.
     *
     * @author Jimmy Shih
     */
    public interface DataSourceListener {

        /**
         * Notifies when the tracks table is updated.
         */
        void notifyTracksTableUpdated();

        /**
         * Notifies when the waypoints table is updated.
         */
        void notifyWaypointsTableUpdated();

        /**
         * Notifies when the track points table is updated.
         */
        void notifyTrackPointsTableUpdated();
    }
}
