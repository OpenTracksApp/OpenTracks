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
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Track data hub.
 * Receives data from {@link de.dennisguse.opentracks.content.provider.CustomContentProvider} and distributes it to {@link TrackDataListener} after some processing.
 * <p>
 * {@link TrackPoint}s are filtered/downsampled with a dynamic sampling frequency.
 *
 * @author Rodrigo Damazio
 */
public class TrackDataHub implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Target number of track points displayed by the diagrams (recommended).
     * We may display more than this number of points.
     */
    private static final int TARGET_DISPLAYED_TRACKPOINTS = 5000;

    /**
     * Maximum number of markers to displayed in the diagrams.
     */
    @VisibleForTesting
    private static final int MAX_DISPLAYED_MARKERS = 128;

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
    private TrackPoint.Id firstSeenTrackPointId;
    private TrackPoint.Id lastSeenTrackPointId;

    // Registered listeners
    private ContentObserver tracksTableObserver;
    private ContentObserver markersTableObserver;
    private ContentObserver trackPointsTableObserver;

    public TrackDataHub(Context context) {
        this(context, new TrackDataManager(), new ContentProviderUtils(context), TARGET_DISPLAYED_TRACKPOINTS);
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

        markersTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyMarkersTableUpdate(trackDataManager.getListenerMarkers());
            }
        };
        contentResolver.registerContentObserver(MarkerColumns.CONTENT_URI, false, markersTableObserver);

        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTrackPointsTableUpdate(true, trackDataManager.getListenerTrackPoints_SampledIn(), trackDataManager.getListenerTrackPoints_SampledOut());
            }
        };
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);


        PreferencesUtils.register(context, this);
        onSharedPreferenceChanged(null, null);
        handler.post(() -> {
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
        contentResolver.unregisterContentObserver(markersTableObserver);
        contentResolver.unregisterContentObserver(trackPointsTableObserver);

        if (handlerThread != null) {
            handlerThread.getLooper().quit();
            handlerThread = null;
        }
        handler = null;
    }

    public void loadTrack(final @NonNull Track.Id trackId) {
        handler.post(() -> {
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
    public void registerTrackDataListener(final TrackDataListener trackDataListener, final boolean tracksTable, final boolean markersTable, final boolean trackPointsTable_SampleIn, final boolean trackPointsTable_SampleOut) {
        handler.post(() -> {
            trackDataManager.registerTrackDataListener(trackDataListener, tracksTable, markersTable, trackPointsTable_SampleIn, trackPointsTable_SampleOut);
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
        handler.post(() -> trackDataManager.unregisterTrackDataListener(trackDataListener));
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
        handler.post(() -> {
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
        notifyMarkersTableUpdate(trackDataManager.getListenerMarkers());
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

        if (trackDataManager.listensForMarkers(trackDataListener)) {
            notifyMarkersTableUpdate(trackDataListeners);
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
     * Notifies marker table update.
     * Currently, reloads all the markers up to {@link #MAX_DISPLAYED_MARKERS}. To be run in the {@link #handler} thread.
     *
     * @param trackDataListeners the track data listeners to notify
     */
    private void notifyMarkersTableUpdate(Set<TrackDataListener> trackDataListeners) {
        if (trackDataListeners.isEmpty()) {
            return;
        }

        for (TrackDataListener trackDataListener : trackDataListeners) {
            trackDataListener.clearMarkers();
        }

        try (Cursor cursor = contentProviderUtils.getMarkerCursor(selectedTrackId, null, MAX_DISPLAYED_MARKERS)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Marker marker = contentProviderUtils.createMarker(cursor);
                    for (TrackDataListener trackDataListener : trackDataListeners) {
                        trackDataListener.onNewMarker(marker);
                    }
                } while (cursor.moveToNext());
            }
        }

        for (TrackDataListener trackDataListener : trackDataListeners) {
            trackDataListener.onNewMarkersDone();
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
        TrackPoint.Id localFirstSeenTrackPointId = updateSamplingState ? firstSeenTrackPointId : null;
        TrackPoint.Id localLastSeenTrackPointIdId = updateSamplingState ? lastSeenTrackPointId : null;
        TrackPoint.Id maxPointId = updateSamplingState ? null : lastSeenTrackPointId;

        if (selectedTrackId == null) {
            Log.w(TAG, "This should not happen, but it does"); //TODO
            return;
        }

        TrackPoint.Id lastTrackPointId = contentProviderUtils.getLastTrackPointId(selectedTrackId);
        int samplingFrequency = -1;


        TrackPoint.Id next = null;
        if (localLastSeenTrackPointIdId != null) {
            next = new TrackPoint.Id(localLastSeenTrackPointIdId.getId() + 1); //TODO startTrackPointId + 1 is an assumption assumption; should be derived from the DB.
        }

        TrackPoint trackPoint = null;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(selectedTrackId, next)) {

            while (trackPointIterator.hasNext()) {
                trackPoint = trackPointIterator.next();
                TrackPoint.Id trackPointId = trackPoint.getId();

                // Stop if past the last wanted point
                if (maxPointId != null && trackPointId.getId() > maxPointId.getId()) {
                    break;
                }

                if (localFirstSeenTrackPointId == null) {
                    localFirstSeenTrackPointId = trackPointId;
                }

                if (samplingFrequency == -1) {
                    long numTotalPoints = Math.max(0L, lastTrackPointId.getId() - localFirstSeenTrackPointId.getId()); //TODO That is an assumption; should be derived from the DB.
                    samplingFrequency = 1 + (int) (numTotalPoints / targetNumPoints);
                }


                // Also include the last point if the selected track is not recording.
                if ((localNumLoadedTrackPoints % samplingFrequency == 0) || (trackPointId == lastTrackPointId && !isSelectedTrackRecording())) {
                    for (TrackDataListener trackDataListener : sampledInListeners) {
                        trackDataListener.onSampledInTrackPoint(trackPoint);
                    }
                } else {
                    for (TrackDataListener trackDataListener : sampledOutListeners) {
                        trackDataListener.onSampledOutTrackPoint(trackPoint);
                    }
                }

                localNumLoadedTrackPoints++;
            }
        }

        if (trackPoint != null) {
            localLastSeenTrackPointIdId = trackPoint.getId();
        }

        if (updateSamplingState) {
            numLoadedPoints = localNumLoadedTrackPoints;
            firstSeenTrackPointId = localFirstSeenTrackPointId;
            lastSeenTrackPointId = localLastSeenTrackPointIdId;
        }

        for (TrackDataListener listener : sampledInListeners) {
            listener.onNewTrackPointsDone(trackPoint);
        }
    }

    /**
     * Resets the track points sampling states.
     */
    private void resetSamplingState() {
        numLoadedPoints = 0;
        firstSeenTrackPointId = null;
        lastSeenTrackPointId = null;
    }
}