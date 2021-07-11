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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import de.dennisguse.opentracks.services.handlers.EGM2008CorrectionManager;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.HandlerServer;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.services.tasks.AnnouncementPeriodicTask;
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

    private static final Duration RECORDING_DATA_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static final RecordingStatus STATUS_DEFAULT = new RecordingStatus(null, false);
    public static final RecordingData NOT_RECORDING = new RecordingData(null, null, null);
    public static final GpsStatusValue STATUS_GPS_DEFAULT = GpsStatusValue.GPS_NONE;

    // The following variables are set in onCreate:
    private ContentProviderUtils contentProviderUtils;
    private PeriodicTaskExecutor voiceExecutor;
    private TrackRecordingServiceNotificationManager notificationManager;

    private Handler handler;
    private final Runnable updateRecordingData = new Runnable() {
        @Override
        public void run() {
            updateRecordingDataWhileRecording();
            Handler localHandler = TrackRecordingService.this.handler;
            if (localHandler == null) {
                // when this happens, no recording is running and we should not send any notifications.
                //TODO This implementation is not a good idea; rather solve the issue for this properly
                return;
            }
            localHandler.postDelayed(this, RECORDING_DATA_UPDATE_INTERVAL.toMillis());
        }
    };

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

    private EGM2008CorrectionManager egm2008CorrectionManager;

    private TrackStatisticsUpdater trackStatisticsUpdater;
    private TrackPoint lastTrackPoint;
    private boolean isIdle;

    private final Binder binder = new Binder();

    private HandlerServer handlerServer;

    private RecordingStatus recordingStatus;
    private MutableLiveData<RecordingStatus> recordingStatusObservable;
    private MutableLiveData<GpsStatusValue> gpsStatusObservable;
    private MutableLiveData<RecordingData> recordingDataObservable;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();

        recordingStatusObservable = new MutableLiveData<>();
        updateRecordingStatus(STATUS_DEFAULT);
        gpsStatusObservable = new MutableLiveData<>(STATUS_GPS_DEFAULT);
        recordingDataObservable = new MutableLiveData<>(NOT_RECORDING);

        handlerServer = new HandlerServer(this);

        contentProviderUtils = new ContentProviderUtils(this);
        voiceExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTask.Factory());

        notificationManager = new TrackRecordingServiceNotificationManager(this);

        egm2008CorrectionManager = new EGM2008CorrectionManager();

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
        handler = null;

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

        egm2008CorrectionManager = null;

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

        recordingStatusObservable = null;
        gpsStatusObservable = null;
        recordingDataObservable = null;

        super.onDestroy();
    }

    public boolean isRecording() {
        return recordingStatus.isRecording();
    }

    public boolean isPaused() {
        return recordingStatus.isPaused();
    }

    public Track.Id getRecordingTrackId() {
        return recordingStatus.getTrackId();
    }

    //TODO
    public TrackStatistics getTrackStatistics() {
        if (trackStatisticsUpdater == null) {
            return null;
        }
        return trackStatisticsUpdater.getTrackStatistics();
    }

    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        if (!isRecording() || isPaused()) {
            return null;
        }

        if (name == null) {
            Integer nextMarkerNumber = contentProviderUtils.getNextMarkerNumber(getRecordingTrackId());
            if (nextMarkerNumber == null) {
                nextMarkerNumber = 1;
            }
            name = getString(R.string.marker_name_format, nextMarkerNumber + 1);
        }

        TrackPoint trackPoint = getLastValidTrackPointInCurrentSegment(getRecordingTrackId());
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
        Marker marker = new Marker(name, description, category, icon, getRecordingTrackId(), stats, trackPoint, photoUrl);
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
            Log.w(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }

        // Insert a track
        Track track = new Track();
        Uri uri = contentProviderUtils.insertTrack(track);
        Track.Id trackId = new Track.Id(ContentUris.parseId(uri));

        // Set recording status
        updateRecordingStatus(new RecordingStatus(trackId, false));

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
        updateRecordingStatus(new RecordingStatus(trackId, false));

        startRecording();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void resumeCurrentTrack() {
        if (!isRecording() || !isPaused()) {
            Log.w(TAG, "Ignore resumeCurrentTrack. Not recording or not paused.");
            return;
        }

        // Set recording status
        updateRecordingStatus(new RecordingStatus(getRecordingTrackId(), false));

        // Update database
        Track track = contentProviderUtils.getTrack(getRecordingTrackId());
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

        handler.postDelayed(updateRecordingData, RECORDING_DATA_UPDATE_INTERVAL.toMillis());

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
    public void endCurrentTrack() {
        if (!isRecording()) {
            Log.w(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        // Need to remember the recordingTrackId before setting it to null
        boolean wasPause = isPaused();
        Track.Id trackId = getRecordingTrackId();

        // Set recording status
        updateRecordingStatus(STATUS_DEFAULT);

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

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void pauseCurrentTrack() {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore pauseCurrentTrack. Not recording or paused.");
            return;
        }

        // Set recording status
        updateRecordingStatus(new RecordingStatus(getRecordingTrackId(), true));

        // Update database
        Track track = contentProviderUtils.getTrack(getRecordingTrackId());
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
        handler.removeCallbacks(updateRecordingData);
        if (!trackStopped) {
            updateRecordingDataWhileRecording();
        } else {
            recordingDataObservable.postValue(NOT_RECORDING);
        }

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

        Track track = contentProviderUtils.getTrack(getRecordingTrackId());
        if (track == null) {
            Log.w(TAG, "Ignore newTrackPoint. No track.");
            return;
        }

        remoteSensorManager.fill(trackPoint);

        egm2008CorrectionManager.correctAltitude(this, trackPoint);

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

        //TODO This check should not be necessary, but prevents a crash; somehow the shutdown is not working correctly as we should not receive a notification then.
        if (gpsStatusObservable != null) {
            gpsStatusObservable.postValue(gpsStatusValue);
        }
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
            if (!TrackPoint.Type.SEGMENT_START_MANUAL.equals(trackPoint.getType())) {
                altitudeSumManager.fill(trackPoint);
                altitudeSumManager.reset();

                remoteSensorManager.fill(trackPoint);
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

    private void showNotification(boolean isGpsStarted) {
        if (isRecording()) {
            Intent intent = IntentUtils.newIntent(this, TrackRecordingActivity.class)
                    .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, getRecordingTrackId());
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

    // This is used to modify the state of this service while testing; must be called after startNewTrack().
    @Deprecated
    @VisibleForTesting
    public void setRemoteSensorManager(@NonNull BluetoothRemoteSensorManager remoteSensorManager) {
        this.remoteSensorManager = remoteSensorManager;
    }

    // This is used to modify the state of this service while testing; must be called after startNewTrack().
    @Deprecated
    @VisibleForTesting
    public void setAltitudeSumManager(@NonNull AltitudeSumManager altitudeSumManager) {
        this.altitudeSumManager = altitudeSumManager;
    }

    public LiveData<GpsStatusValue> getGpsStatusObservable() {
        return gpsStatusObservable;
    }

    public MutableLiveData<RecordingData> getRecordingDataObservable() {
        return recordingDataObservable;
    }

    private void updateRecordingDataWhileRecording() {
        if (!recordingStatus.isRecording()) {
            Log.w(TAG, "Currently not recording; cannot update data.");
            return;
        }
        Track track = contentProviderUtils.getTrack(recordingStatus.getTrackId());

        // Compute temporary track statistics using sensorData and update time.
        //TODO This somehow should happen in the HandlerServer as we create a new TrackPoint.
        TrackStatisticsUpdater tmpTrackStatisticsUpdater = new TrackStatisticsUpdater(trackStatisticsUpdater);
        TrackPoint tmpLastTrackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT);

        if (lastTrackPoint != null && lastTrackPoint.hasLocation()) {
            //TODO Should happen in TrackPoint? via constructor
            tmpLastTrackPoint.setSpeed(lastTrackPoint.getSpeed());
            tmpLastTrackPoint.setAltitude(lastTrackPoint.getAltitude());
            tmpLastTrackPoint.setLongitude(lastTrackPoint.getLongitude());
            tmpLastTrackPoint.setLatitude(lastTrackPoint.getLatitude());
        }

        BluetoothRemoteSensorManager localRemoteSensorManager = this.remoteSensorManager;
        AltitudeSumManager localAltitudeSumManager = this.altitudeSumManager;
        if (localAltitudeSumManager == null || localRemoteSensorManager == null) {
            // when this happens, no recording is running and we should not send any notifications.
            //TODO This implementation is not a good idea; rather solve the issue for this properly
            return;
        }
        localAltitudeSumManager.fill(tmpLastTrackPoint);
        SensorDataSet sensorDataSet = localRemoteSensorManager.getSensorDataSet();
        sensorDataSet.fillTrackPoint(tmpLastTrackPoint);
        tmpTrackStatisticsUpdater.addTrackPoint(tmpLastTrackPoint, recordingDistanceInterval);
        track.setTrackStatistics(tmpTrackStatisticsUpdater.getTrackStatistics());

        recordingDataObservable.postValue(new RecordingData(track, tmpLastTrackPoint, sensorDataSet));
    }

    public LiveData<RecordingStatus> getRecordingStatusObservable() {
        return recordingStatusObservable;
    }

    private void updateRecordingStatus(RecordingStatus status) {
        Log.i(TAG, "new status " + recordingStatus + " -> " + status);
        recordingStatus = status;
        recordingStatusObservable.postValue(recordingStatus);
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

    public static class RecordingStatus {
        private final Track.Id trackId;
        private final boolean paused;

        public RecordingStatus(Track.Id trackId, boolean paused) {
            this.trackId = trackId;
            this.paused = paused;
        }

        public Track.Id getTrackId() {
            return trackId;
        }

        public boolean isRecording() {
            return trackId != null;
        }

        public boolean isPaused() {
            return paused;
        }

        public boolean isRecordingAndNotPaused() {
            return isRecording() && !isPaused();
        }

//      TODO Use
//        public RecordingStatus pause() {
//            return new RecordingStatus(getTrackId(), true);
//        }
//
//        public RecordingStatus record(@NonNull Track.Id trackId) {
//            return new RecordingStatus(trackId, false);
//        }

        public RecordingStatus stop() {
            return STATUS_DEFAULT;
        }


        @Override
        public String toString() {
            return "RecordingStatus{" +
                    "trackId=" + trackId +
                    ", paused=" + paused +
                    '}';
        }
    }

    public static class RecordingData {

        private final Track track;

        private final TrackPoint latestTrackPoint;

        private final SensorDataSet sensorDataSet;

        /**
         * {@link Track} and {@link TrackPoint} must be immutable (i.e., their content does not change).
         */
        public RecordingData(Track track, TrackPoint lastTrackPoint, SensorDataSet sensorDataSet) {
            this.track = track;
            this.latestTrackPoint = lastTrackPoint;
            this.sensorDataSet = sensorDataSet;
        }

        public Track getTrack() {
            return track;
        }

        public String getTrackCategory() {
            if (track == null) {
                return "";
            }
            return track.getCategory();
        }

        @NonNull
        public TrackStatistics getTrackStatistics() {
            if (track == null) {
                return new TrackStatistics();
            }

            return track.getTrackStatistics();
        }

        public TrackPoint getLatestTrackPoint() {
            return latestTrackPoint;
        }

        public SensorDataSet getSensorDataSet() {
            return sensorDataSet;
        }
    }
}