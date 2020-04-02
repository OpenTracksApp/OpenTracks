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
     * Registers for content changes.
     */
    public void start() {
        contentResolver.registerContentObserver(TracksColumns.CONTENT_URI, false, tracksTableObserver);
        contentResolver.registerContentObserver(WaypointsColumns.CONTENT_URI, false, waypointsTableObserver);
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);
    }

    /**
     * Unregisters from content changes.
     */
    public void stop() {
        contentResolver.unregisterContentObserver(tracksTableObserver);
        contentResolver.unregisterContentObserver(waypointsTableObserver);
        contentResolver.unregisterContentObserver(trackPointsTableObserver);
    }

    /**
     * Listener to be invoked when observed data changes changes.
     *
     * @author Jimmy Shih
     */
    interface DataSourceListener {

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
