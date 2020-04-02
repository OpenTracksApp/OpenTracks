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
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Track data hub. Receives data from {@link de.dennisguse.opentracks.content.provider.CustomContentProvider} and distributes it to {@link TrackDataListener} after some processing.
 *
 * @author Rodrigo Damazio
 */
public class TrackDataHub implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Target number of track points displayed by the map overlay.
     * We may display more than this number of points.
     */
    @Deprecated
    private static final int TARGET_DISPLAYED_TRACK_POINTS = 5000;

    /**
     * Maximum number of waypoints to displayed.
     */
    @VisibleForTesting
    @Deprecated
    private static final int MAX_DISPLAYED_WAYPOINTS = 128;

    private static final String TAG = TrackDataHub.class.getSimpleName();

    private final Context context;
    private final TrackDataManager trackDataManager;
    private final ContentProviderUtils contentProviderUtils;
    private final int targetNumPoints;

    private boolean started;
    private HandlerThread handlerThread;
    private Handler handler;

    // Preference values
    private long selectedTrackId;
    private long recordingTrackId;
    private boolean recordingTrackPaused;

    // Track points sampling state
    private int numLoadedPoints;
    private long firstSeenTrackPointId;
    private long lastSeenTrackPointId;

    // Registered listeners
    private ContentObserver tracksTableObserver;
    private ContentObserver waypointsTableObserver;
    private ContentObserver trackPointsTableObserver;

    public TrackDataHub(Context context) {
        this(context, new TrackDataManager(), new ContentProviderUtils(context), TARGET_DISPLAYED_TRACK_POINTS);
    }

    @VisibleForTesting
    private TrackDataHub(Context context, TrackDataManager trackDataManager, ContentProviderUtils contentProviderUtils, int targetNumPoints) {
        this.context = context;
        this.trackDataManager = trackDataManager;
        this.contentProviderUtils = contentProviderUtils;
        this.targetNumPoints = targetNumPoints;
        resetSamplingState();
    }

    public void start() {
        if (started) {
            Log.i(TAG, "TrackDataHub already started, ignoring start.");
            return;
        }
        started = true;
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        //register listeners
        ContentResolver contentResolver = context.getContentResolver();
        tracksTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTracksTableUpdate(trackDataManager.getListeners(TrackDataType.TRACKS_TABLE));
            }
        };
        contentResolver.registerContentObserver(TracksColumns.CONTENT_URI, false, tracksTableObserver);

        waypointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyWaypointsTableUpdate(trackDataManager.getListeners(TrackDataType.WAYPOINTS_TABLE));
            }
        };
        contentResolver.registerContentObserver(WaypointsColumns.CONTENT_URI, false, waypointsTableObserver);

        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTrackPointsTableUpdate(true, trackDataManager.getListeners(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE), trackDataManager.getListeners(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE));
            }
        };
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);


        PreferencesUtils.register(context, this);
        onSharedPreferenceChanged(null, null);
        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (started) {
                    loadDataForAll();
                }
            }
        });
    }

    public void stop() {
        if (!started) {
            Log.i(TAG, "TrackDataHub not started, ignoring stop.");
            return;
        }

        PreferencesUtils.unregister(context, this);

        started = false;

        //Unregister listeners
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.unregisterContentObserver(tracksTableObserver);
        contentResolver.unregisterContentObserver(waypointsTableObserver);
        contentResolver.unregisterContentObserver(trackPointsTableObserver);

        if (handlerThread != null) {
            handlerThread.getLooper().quit();
            handlerThread = null;
        }
        handler = null;
    }

    public void loadTrack(final long trackId) {
        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (trackId == selectedTrackId) {
                    Log.i(TAG, "Not reloading track " + trackId);
                    return;
                }
                selectedTrackId = trackId;
                loadDataForAll();
            }
        });
    }

    /**
     * Registers a {@link TrackDataListener}.
     *
     * @param trackDataListener the track data listener
     */
    public void registerTrackDataListener(final TrackDataListener trackDataListener, boolean tracksTable, boolean waypointTable, boolean trackPointsTable_SampleIn, boolean trackPointsTable_SampleOut) {
        final EnumSet<TrackDataType> types = EnumSet.noneOf(TrackDataType.class);
        if (tracksTable) types.add(TrackDataType.TRACKS_TABLE);
        if (waypointTable) types.add(TrackDataType.WAYPOINTS_TABLE);
        if (trackPointsTable_SampleIn) types.add(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE);
        if (trackPointsTable_SampleOut) types.add(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE);

        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                trackDataManager.registerListener(trackDataListener, types);
                if (started) {
                    loadDataForListener(trackDataListener);
                }
            }
        });
    }

    /**
     * Unregisters a {@link TrackDataListener}.
     *
     * @param trackDataListener the track data listener
     */
    public void unregisterTrackDataListener(final TrackDataListener trackDataListener) {
        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                trackDataManager.unregisterListener(trackDataListener);
            }
        });
    }

    /**
     * Reloads data for a {@link TrackDataListener}.
     */
    public void reloadDataForListener(final TrackDataListener trackDataListener) {
        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                loadDataForListener(trackDataListener);
            }
        });
    }

    /**
     * Returns true if the selected track is recording.
     */
    public boolean isSelectedTrackRecording() {
        return selectedTrackId == recordingTrackId && PreferencesUtils.isRecording(recordingTrackId);
    }

    /**
     * Returns true if the selected track is paused.
     */
    public boolean isSelectedTrackPaused() {
        return selectedTrackId == recordingTrackId && recordingTrackPaused;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        runInHandlerThread(new Runnable() {
            @Override
            public void run() {
                if (PreferencesUtils.isKey(context, R.string.recording_track_id_key, key)) {
                    recordingTrackId = PreferencesUtils.getRecordingTrackId(context);
                }
                if (PreferencesUtils.isKey(context, R.string.recording_track_paused_key, key)) {
                    recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(context);
                }
            }
        });
    }

    /**
     * Loads data for all listeners. To be run in the {@link #handler} thread.
     */
    private void loadDataForAll() {
        resetSamplingState();
        if (trackDataManager.getNumberOfListeners() == 0) {
            return;
        }

        notifyTracksTableUpdate(trackDataManager.getListeners(TrackDataType.TRACKS_TABLE));

        for (TrackDataListener listener : trackDataManager.getListeners(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE)) {
            listener.clearTrackPoints();
        }
        notifyTrackPointsTableUpdate(true,
                trackDataManager.getListeners(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE),
                trackDataManager.getListeners(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE));
        notifyWaypointsTableUpdate(trackDataManager.getListeners(TrackDataType.WAYPOINTS_TABLE));
    }

    /**
     * Loads data for a listener; to be run in the {@link #handler} thread.
     *
     * @param trackDataListener the track data listener.
     */
    private void loadDataForListener(TrackDataListener trackDataListener) {
        Set<TrackDataListener> trackDataListeners = Collections.singleton(trackDataListener);
        EnumSet<TrackDataType> trackDataTypes = trackDataManager.getTrackDataTypes(trackDataListener);

        if (trackDataTypes.contains(TrackDataType.TRACKS_TABLE)) {
            notifyTracksTableUpdate(trackDataListeners);
        }

        boolean hasSampledIn = trackDataTypes.contains(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE);
        boolean hasSampledOut = trackDataTypes.contains(TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE);
        if (hasSampledIn || hasSampledOut) {
            trackDataListener.clearTrackPoints();
            boolean isOnlyListener = trackDataManager.getNumberOfListeners() == 1;
            if (isOnlyListener) {
                resetSamplingState();
            }
            Set<TrackDataListener> sampledOutListeners = hasSampledOut ? trackDataListeners : Collections.<TrackDataListener>emptySet();
            notifyTrackPointsTableUpdate(isOnlyListener, trackDataListeners, sampledOutListeners);
        }

        if (trackDataTypes.contains(TrackDataType.WAYPOINTS_TABLE)) {
            notifyWaypointsTableUpdate(trackDataListeners);
        }
    }

    /**
     * Notifies track table update; to be run in the {@link #handler} thread.
     *
     * @param trackDataListeners the track data listeners to notify
     */
    private void notifyTracksTableUpdate(Set<TrackDataListener> trackDataListeners) {
        if (trackDataListeners.isEmpty()) {
            return;
        }
        Track track = contentProviderUtils.getTrack(selectedTrackId);
        for (TrackDataListener trackDataListener : trackDataListeners) {
            trackDataListener.onTrackUpdated(track);
        }
    }

    /**
     * Notifies waypoint table update.
     * Currently, reloads all the waypoints up to {@link #MAX_DISPLAYED_WAYPOINTS}. To be run in the {@link #handler} thread.
     *
     * @param trackDataListeners the track data listeners to notify
     */
    private void notifyWaypointsTableUpdate(Set<TrackDataListener> trackDataListeners) {
        if (trackDataListeners.isEmpty()) {
            return;
        }

        for (TrackDataListener trackDataListener : trackDataListeners) {
            trackDataListener.clearWaypoints();
        }

        try (Cursor cursor = contentProviderUtils.getWaypointCursor(selectedTrackId, -1L, MAX_DISPLAYED_WAYPOINTS)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Waypoint waypoint = contentProviderUtils.createWaypoint(cursor);
                    if (!LocationUtils.isValidLocation(waypoint.getLocation())) {
                        continue;
                    }
                    for (TrackDataListener trackDataListener : trackDataListeners) {
                        trackDataListener.onNewWaypoint(waypoint);
                    }
                } while (cursor.moveToNext());
            }
        }

        for (TrackDataListener trackDataListener : trackDataListeners) {
            trackDataListener.onNewWaypointsDone();
        }
    }

    /**
     * Notifies track points table update; to be run in the {@link #handler} thread.
     *
     * @param updateSamplingState true to update the sampling state
     * @param sampledInListeners  the sampled-in listeners
     * @param sampledOutListeners the sampled-out listeners
     */
    private void notifyTrackPointsTableUpdate(boolean updateSamplingState, Set<TrackDataListener> sampledInListeners, Set<TrackDataListener> sampledOutListeners) {
        if (sampledInListeners.isEmpty() && sampledOutListeners.isEmpty()) {
            return;
        }

        if (updateSamplingState && numLoadedPoints >= targetNumPoints) {
            // Reload and resample the track at a lower frequency.
            Log.i(TAG, "Resampling track after " + numLoadedPoints + " points.");
            resetSamplingState();
            for (TrackDataListener listener : sampledInListeners) {
                listener.clearTrackPoints();
            }
        }

        int localNumLoadedPoints = updateSamplingState ? numLoadedPoints : 0;
        long localFirstSeenLocationId = updateSamplingState ? firstSeenTrackPointId : -1L;
        long localLastSeenLocationId = updateSamplingState ? lastSeenTrackPointId : -1L;
        long maxPointId = updateSamplingState ? -1L : lastSeenTrackPointId;

        long lastTrackPointId = contentProviderUtils.getLastTrackPointId(selectedTrackId);
        int samplingFrequency = -1;
        boolean includeNextPoint = false;

        try (TrackPointIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(selectedTrackId, localLastSeenLocationId + 1, false)) {

            while (locationIterator.hasNext()) {
                TrackPoint trackPoint = locationIterator.next();
                long locationId = locationIterator.getTrackPointId();

                // Stop if past the last wanted point
                if (maxPointId != -1L && locationId > maxPointId) {
                    break;
                }

                if (localFirstSeenLocationId == -1) {
                    localFirstSeenLocationId = locationId;
                }

                if (samplingFrequency == -1) {
                    long numTotalPoints = Math.max(0L, lastTrackPointId - localFirstSeenLocationId);
                    samplingFrequency = 1 + (int) (numTotalPoints / targetNumPoints);
                }

                if (!LocationUtils.isValidLocation(trackPoint.getLocation())) { //This can be split markers (not anymore supported feature)
                    includeNextPoint = true;
                } else {
                    // Also include the last point if the selected track is not recording.
                    if (includeNextPoint || (localNumLoadedPoints % samplingFrequency == 0) || (locationId == lastTrackPointId && !isSelectedTrackRecording())) {
                        includeNextPoint = false;
                        for (TrackDataListener trackDataListener : sampledInListeners) {
                            trackDataListener.onSampledInTrackPoint(trackPoint);
                        }
                    } else {
                        for (TrackDataListener trackDataListener : sampledOutListeners) {
                            trackDataListener.onSampledOutTrackPoint(trackPoint);
                        }
                    }
                }

                localNumLoadedPoints++;
                localLastSeenLocationId = locationId;
            }
        }

        if (updateSamplingState) {
            numLoadedPoints = localNumLoadedPoints;
            firstSeenTrackPointId = localFirstSeenLocationId;
            lastSeenTrackPointId = localLastSeenLocationId;
        }

        for (TrackDataListener listener : sampledInListeners) {
            listener.onNewTrackPointsDone();
        }
    }

    /**
     * Resets the track points sampling states.
     */
    private void resetSamplingState() {
        numLoadedPoints = 0;
        firstSeenTrackPointId = -1L;
        lastSeenTrackPointId = -1L;
    }

    /**
     * Run in the handler thread.
     *
     * @param runnable the runnable
     */
    @Deprecated //TODO: Why actually catch this problem: I guess it would be better to fail hard.
    @VisibleForTesting
    private void runInHandlerThread(Runnable runnable) {
        if (handler == null) {
            // Use a Throwable to ensure the stack trace is logged.
            Log.d(TAG, "handler is null.", new Throwable());
            return;
        }
        handler.post(runnable);
    }
}