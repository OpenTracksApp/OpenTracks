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
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.TaskStackBuilder;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.exporter.ExportServiceResultReceiver;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.HandlerServer;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
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

    private SharedPreferences sharedPreferences;

    private Distance recordingDistanceInterval;
    private Distance maxRecordingDistance;
    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Context context = TrackRecordingService.this;
            if (PreferencesUtils.isKey(context, R.string.stats_units_key, key)) {
                boolean metricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, context);
                voiceExecutor.setMetricUnits(metricUnits);
                notificationManager.setMetricUnits(metricUnits);
            }
            if (PreferencesUtils.isKey(context, R.string.voice_frequency_key, key)) {
                voiceExecutor.setTaskFrequency(PreferencesUtils.getVoiceFrequency(sharedPreferences, context));
            }
            if (PreferencesUtils.isKey(context, R.string.recording_distance_interval_key, key)) {
                recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(sharedPreferences, context);
            }
            if (PreferencesUtils.isKey(context, R.string.max_recording_distance_key, key)) {
                maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance(sharedPreferences, context);
            }

            handlerServer.onSharedPreferenceChanged(context, sharedPreferences, key);
        }
    };

    // The following variables are set when recording:
    private WakeLock wakeLock;
    private BluetoothRemoteSensorManager remoteSensorManager;
    private AltitudeSumManager altitudeSumManager;

    private TrackStatisticsUpdater trackStatisticsUpdater;
    private TrackPoint lastTrackPoint;
    private boolean isIdle;

    private final Binder binder = new Binder();

    private HandlerServer handlerServer;

    private final TrackRecordingServiceStatus serviceStatus = new TrackRecordingServiceStatus();

    @Override
    public void onCreate() {
        super.onCreate();

        handlerServer = new HandlerServer(this);

        contentProviderUtils = new ContentProviderUtils(this);
        voiceExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTaskFactory());

        notificationManager = new TrackRecordingServiceNotificationManager(this);

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public Binder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        serviceStatus.onStop();

        handlerServer.stop(this);
        handlerServer = null;

        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }

        if (altitudeSumManager != null) {
            altitudeSumManager.stop(this);
            altitudeSumManager = null;
        }

        // Reverse order from onCreate
        showNotification(false); //TODO Why?

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferences = null;

        try {
            voiceExecutor.shutdown();
        } finally {
            voiceExecutor = null;
        }

        contentProviderUtils = null;

        // This should be the next to last operation
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);

        super.onDestroy();
    }

    public boolean isRecording() {
        return serviceStatus.isRecording();
    }

    public boolean isPaused() {
        return serviceStatus.getRecordingTrackPaused();
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
            Integer nextMarkerNumber = contentProviderUtils.getNextMarkerNumber(serviceStatus.getRecordingTrackId());
            if (nextMarkerNumber == null) {
                nextMarkerNumber = 1;
            }
            name = getString(R.string.marker_name_format, nextMarkerNumber + 1);
        }

        TrackPoint trackPoint = getLastValidTrackPointInCurrentSegment(serviceStatus.getRecordingTrackId());
        if (trackPoint == null) {
            Log.i(TAG, "Could not create a marker as trackPoint is unknown.");
            return null;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = getString(R.string.marker_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";

        TrackStatistics stats = trackStatisticsUpdater.getTrackStatistics();

        // Insert marker
        Marker marker = new Marker(name, description, category, icon, serviceStatus.getRecordingTrackId(), stats, trackPoint, photoUrl);
        Uri uri = contentProviderUtils.insertMarker(marker);
        return new Marker.Id(ContentUris.parseId(uri));
    }

    /**
     * Starts a new track.
     *
     * @return the track id
     */
    public Track.Id startNewTrack() {
        if (isRecording()) {
            Log.d(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }

        // Insert a track
        Track track = new Track();
        Uri uri = contentProviderUtils.insertTrack(track);
        Track.Id trackId = new Track.Id(ContentUris.parseId(uri));

        // Set recording status
        serviceStatus.onChange(trackId, false);

        // Update database
        track.setId(trackId);

        TrackPoint segmentStartTrackPoint = TrackPoint.createSegmentStartManual();
        trackStatisticsUpdater = new TrackStatisticsUpdater();
        insertTrackPoint(track, segmentStartTrackPoint);

        //TODO Pass TrackPoint
        track.setName(TrackNameUtils.getTrackName(this, trackId, segmentStartTrackPoint.getTime()));

        String category = PreferencesUtils.getDefaultActivity(sharedPreferences, this);
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
    public void resumeTrack(Track.Id trackId) {
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId.getId() + " does not exists.");
            return;
        }

        // Sync the real time setting the stop time with current time.
        track.getTrackStatistics().setStopTime(Instant.now());
        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());

        insertTrackPoint(track, TrackPoint.createSegmentStartManual());

        // Set recording status
        serviceStatus.onChange(trackId, false);

        startRecording();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void resumeCurrentTrack() {
        if (!isRecording() || !isPaused()) {
            Log.d(TAG, "Ignore resumeCurrentTrack. Not recording or not paused.");
            return;
        }

        // Set recording status
        serviceStatus.onChange(false);

        // Update database
        Track track = contentProviderUtils.getTrack(serviceStatus.getRecordingTrackId());
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

        altitudeSumManager = new AltitudeSumManager();
        altitudeSumManager.start(this);

        lastTrackPoint = null;
        isIdle = false;

        startGps();

        // Restore periodic tasks
        voiceExecutor.restore();
    }

    public void tryStartGps() {
        if (isRecording()) return;

        startGps();
    }

    private void startGps() {
        wakeLock = SystemUtils.acquireWakeLock(this, wakeLock);
        handlerServer.start(this);
        showNotification(true);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public Track.Id endCurrentTrack() {
        if (!isRecording()) {
            Log.d(TAG, "Ignore endCurrentTrack. Not recording.");
            return null;
        }

        // Need to remember the recordingTrackId before setting it to null
        boolean wasPause = isPaused();
        Track.Id trackId = serviceStatus.getRecordingTrackId();

        // Set recording status
        serviceStatus.onChange(null, true);

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

        return trackId;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void pauseCurrentTrack() {
        if (!isRecording() || isPaused()) {
            Log.d(TAG, "Ignore pauseCurrentTrack. Not recording or paused.");
            return;
        }

        // Set recording status
        serviceStatus.onChange(true);

        // Update database
        Track track = contentProviderUtils.getTrack(serviceStatus.getRecordingTrackId());
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
        if (altitudeSumManager != null) {
            altitudeSumManager.stop(this);
            altitudeSumManager = null;
        }

        lastTrackPoint = null;

        handlerServer.stop(this);

        stopGps(trackStopped);
    }

    public void stopGpsAndShutdown() {
        stopGps(true);
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

    @Override
    public void newTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording or paused.");
            return;
        }

        Track track = contentProviderUtils.getTrack(serviceStatus.getRecordingTrackId());
        if (track == null) {
            Log.w(TAG, "Ignore newTrackPoint. No track.");
            return;
        }

        fillWithSensorDataSet(trackPoint);

        notificationManager.updateTrackPoint(this, track.getTrackStatistics(), trackPoint, recordingGpsAccuracy);

        //TODO Figure out how to avoid loading the lastValidTrackPoint from the database
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(track.getId());

        //Storing trackPoint

        // Always insert the first segment location
        if (!currentSegmentHasTrackPoint()) {
            insertTrackPoint(track, trackPoint);
            lastTrackPoint = trackPoint;
            return;
        }

        Distance distanceToLastTrackLocation = trackPoint.distanceToPrevious(lastValidTrackPoint);
        if (distanceToLastTrackLocation.greaterThan(maxRecordingDistance)) {
            insertTrackPointIfNewer(track, lastTrackPoint);

            trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
            insertTrackPoint(track, trackPoint);

            isIdle = false;
            lastTrackPoint = trackPoint;
            return;
        }

        if (trackPoint.hasSensorData() || distanceToLastTrackLocation.greaterOrEqualThan(recordingDistanceInterval)) {
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
        notificationManager.updateContent(getString(gpsStatusValue.message));
        serviceStatus.onChange(gpsStatusValue);
    }

    public void addListener(@NonNull TrackRecordingServiceStatus.Listener listener) {
        serviceStatus.addListener(listener);
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
            if (altitudeSumManager != null) {
                trackPoint.setAltitudeGain(altitudeSumManager.getAltitudeGain_m());
                trackPoint.setAltitudeLoss(altitudeSumManager.getAltitudeLoss_m());
                altitudeSumManager.reset();
            }
            if (remoteSensorManager != null) {
                fillWithSensorDataSet(trackPoint);
                remoteSensorManager.reset();
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

    public SensorDataSet getSensorDataSet() {
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
     * Returns the relative altitude gain (since last trackpoint).
     */
    public Float getAltitudeGain_m() {
        if (altitudeSumManager == null || !altitudeSumManager.isConnected()) {
            return null;
        }

        return altitudeSumManager.getAltitudeGain_m();
    }

    /**
     * Returns the relative altitude loss (since last trackpoint).
     */
    public Float getAltitudeLoss_m() {
        if (altitudeSumManager == null || !altitudeSumManager.isConnected()) {
            return null;
        }

        return altitudeSumManager.getAltitudeLoss_m();
    }

    private void showNotification(boolean isGpsStarted) {
        if (isRecording()) {
            Intent intent = IntentUtils.newIntent(this, TrackRecordingActivity.class)
                    .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, serviceStatus.getRecordingTrackId());
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

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public TrackRecordingService getService() {
            return TrackRecordingService.this;
        }
    }
}