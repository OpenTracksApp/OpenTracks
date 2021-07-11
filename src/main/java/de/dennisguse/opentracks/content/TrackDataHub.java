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
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.handlers.EGM2008CorrectionManager;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;

/**
 * Track data hub.
 * Receives data from {@link de.dennisguse.opentracks.content.provider.CustomContentProvider} and distributes it to {@link TrackDataListener} after some processing.
 * <p>
 * {@link TrackPoint}s are filtered/downsampled with a dynamic sampling frequency.
 *
 * @author Rodrigo Damazio
 */
//TODO register contentobserver only for exact URL (incl. trackId) to not filter here.
public class TrackDataHub {

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
    private final Set<TrackDataListener> listeners;
    private final ContentProviderUtils contentProviderUtils;
    private final int targetNumPoints;

    private final EGM2008CorrectionManager egm2008Correction = new EGM2008CorrectionManager();

    //TODO Check if this is needed.
    private HandlerThread handlerThread;
    private Handler handler;

    private Track.Id selectedTrackId;

    private Distance recordingDistanceInterval;

    private TrackRecordingService.RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

    // Track points sampling state
    private int numLoadedPoints;
    private TrackPoint.Id firstSeenTrackPointId;
    private TrackPoint.Id lastSeenTrackPointId;
    private TrackStatisticsUpdater trackStatisticsUpdater;

    // Registered listeners
    private ContentObserver tracksTableObserver;
    private ContentObserver markersTableObserver;
    private ContentObserver trackPointsTableObserver;

    public TrackDataHub(Context context) {
        this(context, new ContentProviderUtils(context), TARGET_DISPLAYED_TRACKPOINTS);
    }

    @VisibleForTesting
    private TrackDataHub(Context context, ContentProviderUtils contentProviderUtils, int targetNumPoints) {
        this.context = context;
        this.listeners = new HashSet<>();
        this.contentProviderUtils = contentProviderUtils;
        this.targetNumPoints = targetNumPoints;
        resetSamplingState();
    }

    public void start() {
        if (isStarted()) {
            Log.i(TAG, "TrackDataHub already started, ignoring start.");
            return;
        }
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        //register listeners
        ContentResolver contentResolver = context.getContentResolver();
        tracksTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTracksTableUpdate(listeners);
            }
        };
        contentResolver.registerContentObserver(TracksColumns.CONTENT_URI, false, tracksTableObserver);

        markersTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyMarkersTableUpdate(listeners);
            }
        };
        contentResolver.registerContentObserver(MarkerColumns.CONTENT_URI, false, markersTableObserver);

        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                notifyTrackPointsTableUpdate(true, listeners);
            }
        };
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_ID, false, trackPointsTableObserver);
    }

    public void stop() {
        if (!isStarted()) {
            Log.i(TAG, "TrackDataHub not started, ignoring stop.");
            return;
        }

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
        trackStatisticsUpdater = null;
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
    public void registerTrackDataListener(final TrackDataListener trackDataListener) {
        handler.post(() -> {
            listeners.add(trackDataListener);
            if (isStarted()) {
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
        handler.post(() -> listeners.remove(trackDataListener));
    }

    /**
     * Returns true if the selected track is recording.
     */
    public boolean isSelectedTrackRecording() {
        return selectedTrackId != null && selectedTrackId.equals(recordingStatus.getTrackId());
    }

    /**
     * Loads data for all listeners. To be run in the {@link #handler} thread.
     */
    private void loadDataForAll() {
        resetSamplingState();
        if (listeners.isEmpty()) {
            return;
        }

        notifyTracksTableUpdate(listeners);

        for (TrackDataListener listener : listeners) {
            listener.clearTrackPoints();
        }
        notifyTrackPointsTableUpdate(true, listeners);
        notifyMarkersTableUpdate(listeners);
    }

    /**
     * Loads data for a listener; to be run in the {@link #handler} thread.
     *
     * @param trackDataListener the track data listener.
     */
    private void loadDataForListener(TrackDataListener trackDataListener) {
        Set<TrackDataListener> trackDataListeners = Collections.singleton(trackDataListener);

        //Track
        notifyTracksTableUpdate(trackDataListeners);

        //TrackPoints
        trackDataListener.clearTrackPoints();
        boolean isOnlyListener = listeners.size() == 1;
        if (isOnlyListener) {
            resetSamplingState();
        }
        notifyTrackPointsTableUpdate(isOnlyListener, trackDataListeners);

        //Markers
        notifyMarkersTableUpdate(trackDataListeners);
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
     */
    private void notifyTrackPointsTableUpdate(boolean updateSamplingState, Set<TrackDataListener> listeners) {
        if (listeners.isEmpty()) {
            return;
        }

        if (updateSamplingState && numLoadedPoints >= targetNumPoints) {
            // Reload and resample the track at a lower frequency.
            Log.i(TAG, "Resampling track after " + numLoadedPoints + " points.");
            resetSamplingState();
            for (TrackDataListener listener : listeners) {
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
                if (!isStarted()) {
                    break;
                }

                //Prevents a NPE if stop() is happening while notifyTrackPointsTableUpdate()
                TrackStatisticsUpdater currentUpdater = trackStatisticsUpdater;

                trackPoint = trackPointIterator.next();
                TrackPoint.Id trackPointId = trackPoint.getId();

                // Stop if past the last wanted point
                if (maxPointId != null && trackPointId.getId() > maxPointId.getId()) {
                    break;
                }

                egm2008Correction.correctAltitude(context, trackPoint);

                if (localFirstSeenTrackPointId == null) {
                    localFirstSeenTrackPointId = trackPointId;
                }

                if (samplingFrequency == -1) {
                    long numTotalPoints = Math.max(0L, lastTrackPointId.getId() - localFirstSeenTrackPointId.getId()); //TODO That is an assumption; should be derived from the DB.
                    samplingFrequency = 1 + (int) (numTotalPoints / targetNumPoints);
                }

                currentUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);

                // Also include the last point if the selected track is not recording.
                if ((localNumLoadedTrackPoints % samplingFrequency == 0) || (trackPointId == lastTrackPointId && !isSelectedTrackRecording())) {
                    for (TrackDataListener trackDataListener : listeners) {
                        trackDataListener.onSampledInTrackPoint(trackPoint, currentUpdater.getTrackStatistics(), currentUpdater.getSmoothedSpeed(), currentUpdater.getSmoothedAltitude());
                    }
                } else {
                    for (TrackDataListener trackDataListener : listeners) {
                        trackDataListener.onSampledOutTrackPoint(trackPoint, currentUpdater.getTrackStatistics());
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

        listeners.stream().forEach(TrackDataListener::onNewTrackPointsDone);
    }



    /**
     * Resets the track points sampling states.
     */
    private void resetSamplingState() {
        numLoadedPoints = 0;
        firstSeenTrackPointId = null;
        lastSeenTrackPointId = null;
        trackStatisticsUpdater = new TrackStatisticsUpdater();
    }

    private boolean isStarted() {
        return handlerThread != null;
    }

    public void setRecordingStatus(TrackRecordingService.RecordingStatus recordingStatus) {
        this.recordingStatus = recordingStatus;
    }

    public void setRecordingDistanceInterval(Distance recordingDistanceInterval) {
        this.recordingDistanceInterval = recordingDistanceInterval;
    }
}