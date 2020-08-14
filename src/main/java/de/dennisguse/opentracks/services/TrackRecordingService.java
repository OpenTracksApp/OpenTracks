/*
 * Copyright 2008 Google Inc.
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

package de.dennisguse.opentracks.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.HandlerServer;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.services.tasks.AnnouncementPeriodicTaskFactory;
import de.dennisguse.opentracks.services.tasks.PeriodicTaskExecutor;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;
import de.dennisguse.opentracks.util.TrackPointUtils;

/**
 * A background service that registers a location listener and records track points.
 * Track points are saved to the {@link CustomContentProvider}.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service implements HandlerServer.HandlerServerInterface {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    // The following variables are set in onCreate:
    private ContentProviderUtils contentProviderUtils;
    private PeriodicTaskExecutor voiceExecutor;
    private TrackRecordingServiceNotificationManager notificationManager;

    private long recordingTrackId;
    private boolean recordingTrackPaused;
    private int recordingDistanceInterval;
    private int maxRecordingDistance;

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            Context context = TrackRecordingService.this;
            if (PreferencesUtils.isKey(TrackRecordingService.this, R.string.recording_track_id_key, key)) {
                // Only through the TrackRecordingService can one stop a recording and set the recordingTrackId to -1L.
                if (PreferencesUtils.isRecording(TrackRecordingService.this)) {
                    recordingTrackId = PreferencesUtils.getRecordingTrackId(TrackRecordingService.this);
                }
            }
            if (PreferencesUtils.isKey(context, R.string.recording_track_paused_key, key)) {
                recordingTrackPaused = PreferencesUtils.isRecordingTrackPaused(context);
            }
            if (PreferencesUtils.isKey(context, R.string.stats_units_key, key)) {
                boolean metricUnits = PreferencesUtils.isMetricUnits(context);
                voiceExecutor.setMetricUnits(metricUnits);
            }
            if (PreferencesUtils.isKey(context, R.string.voice_frequency_key, key)) {
                voiceExecutor.setTaskFrequency(PreferencesUtils.getVoiceFrequency(context));
            }
            if (PreferencesUtils.isKey(context, R.string.recording_distance_interval_key, key)) {
                recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(context);
            }
            if (PreferencesUtils.isKey(context, R.string.max_recording_distance_key, key)) {
                maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance(context);
            }

            handlerServer.onSharedPreferenceChanged(context, preferences, key);
        }
    };

    // The following variables are set when recording:
    private WakeLock wakeLock;
    private BluetoothRemoteSensorManager remoteSensorManager;

    private TrackStatisticsUpdater trackStatisticsUpdater;
    private TrackPoint lastTrackPoint;
    private boolean isIdle;

    private TrackRecordingServiceBinder binder = new TrackRecordingServiceBinder(this);

    private HandlerServer handlerServer;

    private List<BoundServiceListener> listeners = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        handlerServer = new HandlerServer(this);

        contentProviderUtils = new ContentProviderUtils(this);
        voiceExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTaskFactory());

        notificationManager = new TrackRecordingServiceNotificationManager(this);

        // onSharedPreferenceChanged might not set recordingTrackId.
        recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

        PreferencesUtils.register(this, sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

        restartTrackAfterServiceRestart();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        handlerServer.stop(this);
        handlerServer = null;

        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }

        // Reverse order from onCreate
        showNotification(false); //TODO Why?

        PreferencesUtils.unregister(this, sharedPreferenceChangeListener);

        try {
            voiceExecutor.shutdown();
        } finally {
            voiceExecutor = null;
        }

        contentProviderUtils = null;

        binder.detachFromService();
        binder = null;

        // This should be the next to last operation
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);

        super.onDestroy();
    }

    public boolean isRecording() {
        return PreferencesUtils.isRecording(recordingTrackId);
    }

    public boolean isPaused() {
        return recordingTrackPaused;
    }

    public long getRecordingTrackId() {
        return recordingTrackId;
    }

    public TrackStatistics getTrackStatistics() {
        if (trackStatisticsUpdater == null) {
            return null;
        }
        return trackStatisticsUpdater.getTrackStatistics();
    }

    public long getTotalTime() {
        if (trackStatisticsUpdater == null) {
            return 0;
        }
        if (!isPaused()) {
            trackStatisticsUpdater.updateTime(System.currentTimeMillis());
        }
        return trackStatisticsUpdater.getTrackStatistics().getTotalTime();
    }

    /**
     * Inserts a waypoint.
     *
     * @return the waypoint id
     */
    public long insertWaypoint(String name, String category, String description, String photoUrl) {
        if (!isRecording() || isPaused()) {
            return -1L;
        }

        if (name == null) {
            int nextWaypointNumber = contentProviderUtils.getNextWaypointNumber(recordingTrackId);
            if (nextWaypointNumber == -1) {
                nextWaypointNumber = 1;
            }
            name = getString(R.string.marker_name_format, nextWaypointNumber + 1);
        }

        TrackPoint trackPoint = getLastValidTrackPointInCurrentSegment(recordingTrackId);
        if (trackPoint == null) {
            Log.i(TAG, "Could not create a waypoint as trackPoint is unknown.");
            return -1L;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = getString(R.string.marker_waypoint_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";

        TrackStatistics stats = trackStatisticsUpdater.getTrackStatistics();
        double length = stats.getTotalDistance();
        long duration = stats.getTotalTime();

        // Insert waypoint
        Waypoint waypoint = new Waypoint(name, description, category, icon, recordingTrackId, length, duration, trackPoint.getLocation(), photoUrl);
        Uri uri = contentProviderUtils.insertWaypoint(waypoint);
        return ContentUris.parseId(uri);
    }

    /**
     * Starts a new track.
     *
     * @return the track id
     */
    long startNewTrack() {
        if (isRecording()) {
            Log.d(TAG, "Ignore startNewTrack. Already recording.");
            return -1L;
        }
        long now = System.currentTimeMillis();
        trackStatisticsUpdater = new TrackStatisticsUpdater(now);

        // Insert a track
        Track track = new Track();
        Uri uri = contentProviderUtils.insertTrack(track);
        long trackId = ContentUris.parseId(uri);

        // Update shared preferences
        updateRecordingState(trackId, false);

        // Update database
        track.setId(trackId);
        track.setName(TrackNameUtils.getTrackName(this, trackId, now));

        String category = PreferencesUtils.getDefaultActivity(this);
        track.setCategory(category);
        track.setIcon(TrackIconUtils.getIconValue(this, category));
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(track);

        startRecording();
        return trackId;
    }

    /**
     * Resumes the track identified by trackId.
     * It results in a pause/continue.
     *
     * @param trackId the id of the track to be resumed.
     */
    void resumeTrack(long trackId) {
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId + " does not exists.");
            return;
        }

        // Sync the real time setting the stop time with current time.
        track.getTrackStatistics().setStopTime_ms(System.currentTimeMillis());
        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());

        insertTrackPoint(track, TrackPoint.createPause());
        insertTrackPoint(track, TrackPoint.createResume());

        // Update shared preferences.
        updateRecordingState(trackId, false);

        startRecording();
    }

    /**
     * Try to restart the previous recording track in case the service has been restarted by the system, which can sometimes happen.
     */
    private void restartTrackAfterServiceRestart() {
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track == null) {
            if (isRecording()) {
                Log.w(TAG, "track is null, but recordingTrackId not -1L. " + recordingTrackId);
                updateRecordingState(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, true);
            }
            showNotification(false);
            return;
        }

        Log.d(TAG, "Restarting track: " + track.getId());

        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics().getStartTime_ms());

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), -1L, false)) {
            trackStatisticsUpdater.addTrackPoint(trackPointIterator, recordingDistanceInterval);
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException", e);
        }
        startRecording();
    }

    void resumeCurrentTrack() {
        if (!isRecording() || !isPaused()) {
            Log.d(TAG, "Ignore resumeCurrentTrack. Not recording or not paused.");
            return;
        }

        updateRecordingState(recordingTrackId, false);

        // Update database
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track != null) {
            insertTrackPoint(track, TrackPoint.createResume());
        }

        startRecording();
    }

    /**
     * Common code for starting a new track, resuming a track, or restarting after phone reboot.
     */
    private void startRecording() {
        // Update instance variables
        remoteSensorManager = new BluetoothRemoteSensorManager(this);
        remoteSensorManager.start();
        lastTrackPoint = null;
        isIdle = false;

        startGps();

        // Restore periodic tasks
        voiceExecutor.restore();
    }

    void tryStartGps() {
        if (isRecording()) return;

        startGps();
    }

    private void startGps() {
        wakeLock = SystemUtils.acquireWakeLock(this, wakeLock);
        handlerServer.start(this);
        showNotification(true);
    }

    void endCurrentTrack() {
        if (!isRecording()) {
            Log.d(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        // Need to remember the recordingTrackId before setting it to -1L
        long trackId = recordingTrackId;
        boolean wasPaused = recordingTrackPaused;

        updateRecordingState(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, true);

        // Update database
        Track track = contentProviderUtils.getTrack(trackId);
        if (track != null) {
            // If not wasPaused, add the last location
            if (!wasPaused) {
                if (lastTrackPoint != null) {
                    insertTrackPointIfNewer(track, lastTrackPoint);
                }

                // Update the recording track time
                updateTrackTotalTime(track);
            }
        }
        endRecording(true);
    }

    void pauseCurrentTrack() {
        if (!isRecording() || isPaused()) {
            Log.d(TAG, "Ignore pauseCurrentTrack. Not recording or paused.");
            return;
        }

        updateRecordingState(recordingTrackId, true);

        // Update database
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track != null) {
            if (lastTrackPoint != null) {
                insertTrackPointIfNewer(track, lastTrackPoint);
            }
            insertTrackPoint(track, TrackPoint.createPause());
        }

        endRecording(false);

        notificationManager.updateContent(getString(R.string.generic_paused));
    }

    /**
     * Common code for ending a track or pausing a track.
     *
     * @param trackStopped true if track is stopped, false if track is paused
     */
    private void endRecording(boolean trackStopped) {
        // Shutdown periodic tasks
        voiceExecutor.shutdown();

        // Update instance variables
        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }
        lastTrackPoint = null;

        handlerServer.stop(this);

        stopGps(trackStopped);
    }

    /**
     * Stops gps.
     *
     * @param shutdown true to shutdown self
     */
    void stopGps(boolean shutdown) {
        if (!isRecording()) return;

        handlerServer.stop(this);
        showNotification(false);
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);
        if (shutdown) {
            stopSelf();
        }
    }

    /**
     * Gets the last valid track point in the current segment.
     *
     * @param trackId the track id
     * @return the location or null
     */
    private TrackPoint getLastValidTrackPointInCurrentSegment(long trackId) {
        if (!currentSegmentHasTrackPoint()) {
            return null;
        }
        return contentProviderUtils.getLastValidTrackPoint(trackId);
    }

    private boolean currentSegmentHasTrackPoint() {
        return lastTrackPoint != null;
    }

    /**
     * Updates the recording states.
     * This will inform subscribed {@link OnSharedPreferenceChangeListener}.
     *
     * @param trackId the recording track id
     * @param paused  true if the recording is paused
     */
    private void updateRecordingState(long trackId, boolean paused) {
        recordingTrackId = trackId;
        PreferencesUtils.setLong(this, R.string.recording_track_id_key, trackId);
        recordingTrackPaused = paused;
        PreferencesUtils.setBoolean(this, R.string.recording_track_paused_key, recordingTrackPaused);
    }

    @Override
    public void newTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording or paused.");
            return;
        }

        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track == null) {
            Log.w(TAG, "Ignore newTrackPoint. No track.");
            return;
        }

        fillWithSensorDataSet(trackPoint);

        notificationManager.updateTrackPoint(this, trackPoint, recordingGpsAccuracy);

        TrackPointUtils.fixTime(trackPoint);

        //TODO Figure out how to avoid loading the lastValidTrackPoint from the database
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(track.getId());

        //Storing trackPoint

        // Always insert the first segment location
        if (!currentSegmentHasTrackPoint()) {
            insertTrackPoint(track, trackPoint);
            lastTrackPoint = trackPoint;
            return;
        }

        if (lastValidTrackPoint == null || !LocationUtils.isValidLocation(lastValidTrackPoint.getLocation())) {
            // For some reason the previous first trackPoint was not stored, but currentSegmentHasLocation set true.
            // Should not happen. The current segment should have a location. Just insert the current location.
            insertTrackPoint(track, trackPoint);
            lastTrackPoint = trackPoint;
            return;
        }

        double distanceToLastTrackLocation = trackPoint.distanceTo(lastValidTrackPoint);
        if (distanceToLastTrackLocation > maxRecordingDistance) {
            insertTrackPointIfNewer(track, lastTrackPoint);
            insertTrackPoint(track, TrackPoint.createPause());

            insertTrackPoint(track, trackPoint);
            isIdle = false;

            lastTrackPoint = trackPoint;
            return;
        }

        if (trackPoint.hasSensorData() || distanceToLastTrackLocation >= recordingDistanceInterval) {
            insertTrackPointIfNewer(track, lastTrackPoint);
            insertTrackPoint(track, trackPoint);
            isIdle = false;

            lastTrackPoint = trackPoint;
            return;
        }

        if (!isIdle && !TrackPointUtils.isMoving(trackPoint)) {
            insertTrackPointIfNewer(track, lastTrackPoint);
            insertTrackPoint(track, trackPoint);
            isIdle = true;

            lastTrackPoint = trackPoint;
            return;
        }

        if (isIdle && TrackPointUtils.isMoving(trackPoint)) {
            insertTrackPointIfNewer(track, lastTrackPoint);
            insertTrackPoint(track, trackPoint);
            isIdle = false;

            lastTrackPoint = trackPoint;
            return;
        }

        Log.d(TAG, "Not recording TrackPoint, idle");
        lastTrackPoint = trackPoint;
    }

    @Override
    public void newGpsStatus(GpsStatusValue gpsStatusValue) {
        notificationManager.updateContent(getString(gpsStatusValue.message));
        for (BoundServiceListener listener : listeners) {
            listener.onGpsStatusChange(gpsStatusValue);
        }
    }

    public void addListener(BoundServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Inserts a trackPoint if this trackPoint is different than lastValidTrackPoint.
     *
     * @param track      the track
     * @param trackPoint the trackPoint
     */
    private void insertTrackPointIfNewer(@NonNull Track track, @NonNull TrackPoint trackPoint) {
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(track.getId());
        if (TrackPointUtils.equalTime(trackPoint, lastValidTrackPoint)) {
            // Do not insert if inserted already
            Log.w(TAG, "Ignore insertTrackPoint. trackPoint time same as last valid track point time.");
            return;
        }

        insertTrackPoint(track, trackPoint);
    }

    /**
     * Inserts a trackPoint.
     *
     * @param track      the track
     * @param trackPoint the trackPoint
     */
    private void insertTrackPoint(@NonNull Track track, @NonNull TrackPoint trackPoint) {
        try {
            contentProviderUtils.insertTrackPoint(trackPoint, track.getId());
            trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
            updateTrackTotalTime(track);
        } catch (SQLiteException e) {
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
        voiceExecutor.update();
    }


    /**
     * Updates the recording track time.
     *
     * @param track the track
     */
    private void updateTrackTotalTime(Track track) {
        trackStatisticsUpdater.updateTime(System.currentTimeMillis());
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(track);
    }

    SensorDataSet getSensorDataSet() {
        if (remoteSensorManager == null || !remoteSensorManager.isEnabled()) {
            return null;
        }

        return remoteSensorManager.getSensorData();
    }

    private void fillWithSensorDataSet(TrackPoint trackPoint) {
        SensorDataSet sensorData = getSensorDataSet();
        if (sensorData != null) {
            sensorData.fillTrackPoint(trackPoint);
        }
    }

    private void showNotification(boolean isGpsStarted) {
        if (isRecording()) {
            Intent intent = IntentUtils.newIntent(this, TrackRecordingActivity.class)
                    .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, recordingTrackId);
            PendingIntent pendingIntent = TaskStackBuilder.create(this)
                    .addParentStack(TrackRecordingActivity.class)
                    .addNextIntent(intent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationManager.updatePendingIntent(pendingIntent);
            notificationManager.updateContent(getString(R.string.gps_starting));
            startForeground(TrackRecordingServiceNotificationManager.NOTIFICATION_ID, notificationManager.getNotification());
        }
        if (!isRecording() && isGpsStarted) {
            Intent intent = IntentUtils.newIntent(this, TrackListActivity.class);
            PendingIntent pendingIntent = TaskStackBuilder.create(this)
                    .addNextIntent(intent)
                    .getPendingIntent(0, 0);

            notificationManager.updatePendingIntent(pendingIntent);
            notificationManager.updateContent(getString(R.string.gps_starting));
            startForeground(TrackRecordingServiceNotificationManager.NOTIFICATION_ID, notificationManager.getNotification());
        }
        if (!isRecording() && !isGpsStarted) {
            stopForeground(true);
            notificationManager.cancelNotification();
        }
    }

    @VisibleForTesting
    public void setRemoteSensorManager(BluetoothRemoteSensorManager remoteSensorManager) {
        this.remoteSensorManager = remoteSensorManager;
    }
}
