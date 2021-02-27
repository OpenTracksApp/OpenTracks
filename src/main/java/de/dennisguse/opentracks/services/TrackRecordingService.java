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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.TaskStackBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.exporter.ExportServiceResultReceiver;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.HandlerServer;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.services.sensors.ElevationSumManager;
import de.dennisguse.opentracks.services.tasks.AnnouncementPeriodicTaskFactory;
import de.dennisguse.opentracks.services.tasks.PeriodicTaskExecutor;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;

/**
 * A background service that registers a location listener and records track points.
 * Track points are saved to the {@link CustomContentProvider}.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service implements HandlerServer.HandlerServerInterface, ExportServiceResultReceiver.Receiver {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    // The following variables are set in onCreate:
    private ContentProviderUtils contentProviderUtils;
    private PeriodicTaskExecutor voiceExecutor;
    private TrackRecordingServiceNotificationManager notificationManager;

    private Track.Id recordingTrackId;
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
    private ElevationSumManager elevationSumManager;

    private TrackStatisticsUpdater trackStatisticsUpdater;
    private TrackPoint lastTrackPoint;
    private boolean isIdle;

    private TrackRecordingServiceBinder binder = new TrackRecordingServiceBinder(this);

    private HandlerServer handlerServer;

    private List<TrackRecordingServiceCallback> listeners = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        handlerServer = new HandlerServer(this);

        contentProviderUtils = new ContentProviderUtils(this);
        voiceExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTaskFactory());

        notificationManager = new TrackRecordingServiceNotificationManager(this);

        // onSharedPreferenceChanged might not set recordingTrackId.
        recordingTrackId = null;

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
        if (listeners != null) {
            for (TrackRecordingServiceCallback listener : listeners) {
                listener.onGpsStatusChange(GpsStatusValue.GPS_NONE);
            }
            listeners.clear();
            listeners = null;
        }

        handlerServer.stop(this);
        handlerServer = null;

        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }

        if (elevationSumManager != null) {
            elevationSumManager.stop(this);
            elevationSumManager = null;
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

    public Track.Id getRecordingTrackId() {
        return recordingTrackId;
    }

    public TrackStatistics getTrackStatistics() {
        if (trackStatisticsUpdater == null) {
            return null;
        }
        return trackStatisticsUpdater.getTrackStatistics();
    }

    //TODO Throw exception, when not recording.
    public Duration getTotalTime() {
        if (trackStatisticsUpdater == null) {
            return Duration.ofSeconds(0);
        }
        if (isPaused()) {
            return trackStatisticsUpdater.getTrackStatistics().getTotalTime();
        }

        TrackStatistics statistics = trackStatisticsUpdater.getTrackStatistics();
        return Duration.between(statistics.getStopTime(), Instant.now())
                .plus(statistics.getTotalTime());
    }

    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        if (!isRecording() || isPaused()) {
            return null;
        }

        if (name == null) {
            int nextMarkerNumber = contentProviderUtils.getNextMarkerNumber(recordingTrackId);
            if (nextMarkerNumber == -1) {
                nextMarkerNumber = 1;
            }
            name = getString(R.string.marker_name_format, nextMarkerNumber + 1);
        }

        TrackPoint trackPoint = getLastValidTrackPointInCurrentSegment(recordingTrackId);
        if (trackPoint == null) {
            Log.i(TAG, "Could not create a marker as trackPoint is unknown.");
            return null;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = getString(R.string.marker_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";

        TrackStatistics stats = trackStatisticsUpdater.getTrackStatistics();
        double length = stats.getTotalDistance();
        long duration = stats.getTotalTime().toMillis();

        // Insert marker
        Marker marker = new Marker(name, description, category, icon, recordingTrackId, length, duration, trackPoint, photoUrl);
        Uri uri = contentProviderUtils.insertMarker(marker);
        return new Marker.Id(ContentUris.parseId(uri));
    }

    /**
     * Starts a new track.
     *
     * @return the track id
     */
    Track.Id startNewTrack() {
        if (isRecording()) {
            Log.d(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }

        // Insert a track
        Track track = new Track();
        Uri uri = contentProviderUtils.insertTrack(track);
        Track.Id trackId = new Track.Id(ContentUris.parseId(uri));

        // Update shared preferences
        updateRecordingState(trackId, false);

        // Update database
        track.setId(trackId);

        TrackPoint segmentStartTrackPoint = TrackPoint.createSegmentStartManual();
        trackStatisticsUpdater = new TrackStatisticsUpdater();
        insertTrackPoint(track, segmentStartTrackPoint);

        //TODO Pass TrackPoint
        track.setName(TrackNameUtils.getTrackName(this, trackId, segmentStartTrackPoint.getTime()));

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
    void resumeTrack(Track.Id trackId) {
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId.getId() + " does not exists.");
            return;
        }

        // Sync the real time setting the stop time with current time.
        track.getTrackStatistics().setStopTime(Instant.now());
        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());

        insertTrackPoint(track, TrackPoint.createSegmentStartManual());

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
                Log.w(TAG, "track is null, but recordingTrackId not -1L. " + recordingTrackId.getId());
                updateRecordingState(null, true);
            }
            showNotification(false);
            return;
        }

        Log.d(TAG, "Restarting track: " + track.getId());

        trackStatisticsUpdater = new TrackStatisticsUpdater();

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {
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
            insertTrackPoint(track, TrackPoint.createSegmentStartManual());
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

        elevationSumManager = new ElevationSumManager();
        elevationSumManager.start(this);

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
        boolean wasPause = isPaused();
        Track.Id trackId = recordingTrackId;

        updateRecordingState(null, true);

        if (!wasPause) {
            // Update database
            Track track = contentProviderUtils.getTrack(trackId);
            if (track != null) {
                if (lastTrackPoint != null) {
                    insertTrackPointIfNewer(track, lastTrackPoint);
                }

                insertTrackPoint(track, TrackPoint.createSegmentEnd());
            }
        }

        Track track = contentProviderUtils.getTrack(trackId);
        ExportUtils.postWorkoutExport(this, track, new ExportServiceResultReceiver(new Handler(), this));

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
            insertTrackPoint(track, TrackPoint.createSegmentEnd());
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
        if (elevationSumManager != null) {
            elevationSumManager.stop(this);
            elevationSumManager = null;
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
    private TrackPoint getLastValidTrackPointInCurrentSegment(Track.Id trackId) {
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
    private void updateRecordingState(Track.Id trackId, boolean paused) {
        recordingTrackId = trackId;
        long currentTrackId = trackId != null ? trackId.getId() : PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        PreferencesUtils.setLong(this, R.string.recording_track_id_key, currentTrackId);
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

        //TODO Figure out how to avoid loading the lastValidTrackPoint from the database
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(track.getId());

        //Storing trackPoint

        // Always insert the first segment location
        if (!currentSegmentHasTrackPoint()) {
            insertTrackPoint(track, trackPoint);
            lastTrackPoint = trackPoint;
            return;
        }

        double distanceToLastTrackLocation = trackPoint.distanceTo(lastValidTrackPoint);
        if (distanceToLastTrackLocation > maxRecordingDistance) {
            insertTrackPointIfNewer(track, lastTrackPoint);

            trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
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

        if (!isIdle && !trackPoint.isMoving()) {
            insertTrackPointIfNewer(track, lastTrackPoint);

            insertTrackPoint(track, trackPoint);

            isIdle = true;

            lastTrackPoint = trackPoint;
            return;
        }

        if (isIdle && trackPoint.isMoving()) {
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
        if (listeners != null) {
            notificationManager.updateContent(getString(gpsStatusValue.message));
            for (TrackRecordingServiceCallback listener : listeners) {
                listener.onGpsStatusChange(gpsStatusValue);
            }
        }
    }

    public void addListener(TrackRecordingServiceCallback listener) {
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
        if (lastValidTrackPoint != null && trackPoint.getTime().equals(lastValidTrackPoint.getTime())) {
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
            if (elevationSumManager != null) {
                trackPoint.setElevationGain(elevationSumManager.getElevationGain_m());
                trackPoint.setElevationLoss(elevationSumManager.getElevationLoss_m());
                elevationSumManager.reset();
            }
            contentProviderUtils.insertTrackPoint(trackPoint, track.getId());
            trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);

            track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
            contentProviderUtils.updateTrack(track);
        } catch (SQLiteException e) {
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
        voiceExecutor.update();
    }

    SensorDataSet getSensorDataSet() {
        if (remoteSensorManager == null) {
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

    /**
     * Returns the relative elevation gain (since last trackpoint).
     */
    Float getElevationGain_m() {
        if (elevationSumManager == null || !elevationSumManager.isConnected()) {
            return null;
        }

        return elevationSumManager.getElevationGain_m();
    }

    /**
     * Returns the relative elevation loss (since last trackpoint).
     */
    Float getElevationLoss_m() {
        if (elevationSumManager == null || !elevationSumManager.isConnected()) {
            return null;
        }

        return elevationSumManager.getElevationLoss_m();
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

    public GpsStatusValue getGpsStatus() {
        return handlerServer.getGpsStatus();
    }

    @Override
    public void onReceiveResult(final int resultCode, final Bundle resultData) {
        Log.w(TAG, "onReceiveResult: " + resultCode);
        if (resultCode != ExportServiceResultReceiver.RESULT_CODE_SUCCESS) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRAS_CHECK_EXPORT_DIRECTORY, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

}
