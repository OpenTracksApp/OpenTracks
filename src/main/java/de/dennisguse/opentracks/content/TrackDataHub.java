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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
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
 * Track data hub.
 * Receives data from {@link de.dennisguse.opentracks.content.provider.CustomContentProvider} and distributes it to {@link TrackDataListener} after some processing.
 *
 * {@link TrackPoint}s are filtered/downsampled with a dynamic sampling frequency.
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
    private Track.Id selectedTrackId;
    private Track.Id recordingTrackId;
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
                notifyTracksTableUpdate(trackDataManager.getListenerTracks());
            }
        };
        contentResolver.registerContentObserver(TracksColumns.CONTENT_URI, false, tracksTableObserver);

        waypointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyWaypointsTableUpdate(trackDataManager.getListenerWaypoints());
            }
        };
        contentResolver.registerContentObserver(WaypointsColumns.CONTENT_URI, false, waypointsTableObserver);

        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTrackPointsTableUpdate(true, trackDataManager.getListenerTrackPoints_SampledIn(), trackDataManager.getListenerTrackPoints_SampledOut());
            }
        };
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);


        PreferencesUtils.register(context, this);
        onSharedPreferenceChanged(null, null);
        runInHandlerThread(() -> {
            if (started) {
                loadDataForAll();
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

    public void loadTrack(final @NonNull Track.Id trackId) {
        runInHandlerThread(() -> {
            if (trackId.equals(selectedTrackId)) {
                Log.i(TAG, "Not reloading track " + trackId.getId());
                return;
            }
            selectedTrackId = trackId;
            loadDataForAll();
        });
    }

    /**
     * Registers a {@link TrackDataListener}.
     *
     * @param trackDataListener the track data listener
     */
    public void registerTrackDataListener(final TrackDataListener trackDataListener, final boolean tracksTable, final boolean waypointsTable, final boolean trackPointsTable_SampleIn, final boolean trackPointsTable_SampleOut) {
        runInHandlerThread(() -> {
            trackDataManager.registerTrackDataListener(trackDataListener, tracksTable, waypointsTable, trackPointsTable_SampleIn, trackPointsTable_SampleOut);
            if (started) {
                loadDataForListener(trackDataListener);
            }
        });
    }

    /**
     * Unregisters a {@link TrackDataListener}.
     *
     * @param trackDataListener the track data listener
     */
    public void unregisterTrackDataListener(final TrackDataListener trackDataListener) {
        runInHandlerThread(() -> trackDataManager.unregisterTrackDataListener(trackDataListener));
    }

    /**
     * Returns true if the selected track is recording.
     */
    public boolean isSelectedTrackRecording() {
        return selectedTrackId != null && selectedTrackId.equals(recordingTrackId) && PreferencesUtils.isRecording(recordingTrackId);
    }

    /**
     * Returns true if the selected track is paused.
     */
    public boolean isSelectedTrackPaused() {
        return selectedTrackId != null && selectedTrackId.equals(recordingTrackId) && recordingTrackPaused;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        runInHandlerThread(() -> {
            if (PreferencesUtils.isKey(context, R.string.recording_track_id_key, key)) {
                recordingTrackId = PreferencesUtils.getRecordingTrackId(context);
            }
            if (PreferencesUtils.isKey(context, R.string.recording_track_paused_key, key)) {
                recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(context);
            }
        });
    }

    /**
     * Loads data for all listeners. To be run in the {@link #handler} thread.
     */
    private void loadDataForAll() {
        resetSamplingState();
        if (!trackDataManager.hasListeners()) {
            return;
        }

        notifyTracksTableUpdate(trackDataManager.getListenerTracks());

        for (TrackDataListener listener : trackDataManager.getListenerTrackPoints_SampledIn()) {
            listener.clearTrackPoints();
        }
        notifyTrackPointsTableUpdate(true, trackDataManager.getListenerTrackPoints_SampledIn(), trackDataManager.getListenerTrackPoints_SampledOut());
        notifyWaypointsTableUpdate(trackDataManager.getListenerWaypoints());
    }

    /**
     * Loads data for a listener; to be run in the {@link #handler} thread.
     *
     * @param trackDataListener the track data listener.
     */
    private void loadDataForListener(TrackDataListener trackDataListener) {
        Set<TrackDataListener> trackDataListeners = Collections.singleton(trackDataListener);

        if (trackDataManager.listensForTracks(trackDataListener)) {
            notifyTracksTableUpdate(trackDataListeners);
        }

        boolean hasSampledIn = trackDataManager.listensForTrackPoints_SampledIn(trackDataListener);
        boolean hasSampledOut = trackDataManager.listensForTrackPoints_SampledOut(trackDataListener);
        if (hasSampledIn || hasSampledOut) {
            trackDataListener.clearTrackPoints();
            boolean isOnlyListener = trackDataManager.getNumberOfListeners() == 1;
            if (isOnlyListener) {
                resetSamplingState();
            }
            Set<TrackDataListener> sampledOutListeners = hasSampledOut ? trackDataListeners : Collections.emptySet();
            notifyTrackPointsTableUpdate(isOnlyListener, trackDataListeners, sampledOutListeners);
        }

        if (trackDataManager.listensForWaypoints(trackDataListener)) {
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

        try (Cursor cursor = contentProviderUtils.getWaypointCursor(selectedTrackId, null, MAX_DISPLAYED_WAYPOINTS)) {
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

        int localNumLoadedTrackPoints = updateSamplingState ? numLoadedPoints : 0;
        long localFirstSeenTrackPointId = updateSamplingState ? firstSeenTrackPointId : -1L;
        long localLastSeenTrackPointIdId = updateSamplingState ? lastSeenTrackPointId : -1L;
        long maxPointId = updateSamplingState ? -1L : lastSeenTrackPointId;

        if (selectedTrackId == null) {
            Log.w(TAG, "This should not happen, but it does"); //TODO
            return;
        }

        long lastTrackPointId = contentProviderUtils.getLastTrackPointId(selectedTrackId);
        int samplingFrequency = -1;
        boolean includeNextPoint = false;

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(selectedTrackId, localLastSeenTrackPointIdId + 1, false)) {

            while (trackPointIterator.hasNext()) {
                TrackPoint trackPoint = trackPointIterator.next();
                long trackPointId = trackPointIterator.getTrackPointId();

                // Stop if past the last wanted point
                if (maxPointId != -1L && trackPointId > maxPointId) {
                    break;
                }

                if (localFirstSeenTrackPointId == -1) {
                    localFirstSeenTrackPointId = trackPointId;
                }

                if (samplingFrequency == -1) {
                    long numTotalPoints = Math.max(0L, lastTrackPointId - localFirstSeenTrackPointId);
                    samplingFrequency = 1 + (int) (numTotalPoints / targetNumPoints);
                }

                if (!LocationUtils.isValidLocation(trackPoint.getLocation())) { //This can be split markers (not anymore supported feature)
                    includeNextPoint = true;
                } else {
                    // Also include the last point if the selected track is not recording.
                    if (includeNextPoint || (localNumLoadedTrackPoints % samplingFrequency == 0) || (trackPointId == lastTrackPointId && !isSelectedTrackRecording())) {
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

                localNumLoadedTrackPoints++;
                localLastSeenTrackPointIdId = trackPointId;
            }
        }

        if (updateSamplingState) {
            numLoadedPoints = localNumLoadedTrackPoints;
            firstSeenTrackPointId = localFirstSeenTrackPointId;
            lastSeenTrackPointId = localLastSeenTrackPointIdId;
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