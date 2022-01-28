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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.announcement.VoiceAnnouncementManager;
import de.dennisguse.opentracks.services.handlers.EGM2008CorrectionManager;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.SystemUtils;

public class TrackRecordingService extends Service implements TrackPointCreator.Callback {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    private static final Duration RECORDING_DATA_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static final RecordingStatus STATUS_DEFAULT = RecordingStatus.notRecording();
    public static final RecordingData NOT_RECORDING = new RecordingData(null, null, null);
    public static final GpsStatusValue STATUS_GPS_DEFAULT = GpsStatusValue.GPS_NONE;

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public TrackRecordingService getService() {
            return TrackRecordingService.this;
        }
    }

    private final Binder binder = new Binder();

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

    // The following variables are set in onCreate:
    private RecordingStatus recordingStatus;
    private MutableLiveData<RecordingStatus> recordingStatusObservable;
    private MutableLiveData<GpsStatusValue> gpsStatusObservable;
    private MutableLiveData<RecordingData> recordingDataObservable;

    // The following variables are set when recording:
    private WakeLock wakeLock;
    private Handler handler;

    private TrackPointCreator trackPointCreator;
    private TrackRecordingManager trackRecordingManager;

    private VoiceAnnouncementManager voiceAnnouncementManager;
    private TrackRecordingServiceNotificationManager notificationManager;

    private EGM2008CorrectionManager egm2008CorrectionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();

        recordingStatusObservable = new MutableLiveData<>();
        updateRecordingStatus(STATUS_DEFAULT);
        gpsStatusObservable = new MutableLiveData<>(STATUS_GPS_DEFAULT);
        recordingDataObservable = new MutableLiveData<>(NOT_RECORDING);

        egm2008CorrectionManager = new EGM2008CorrectionManager();
        trackRecordingManager = new TrackRecordingManager(this);
        trackRecordingManager.start();
        trackPointCreator = new TrackPointCreator(this);

        voiceAnnouncementManager = new VoiceAnnouncementManager(this);
        notificationManager = new TrackRecordingServiceNotificationManager(this);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null); //Some tests do not finish the recording completely
        handler = null;

        trackPointCreator.stop();
        trackPointCreator = null;
        trackRecordingManager.stop();
        trackRecordingManager = null;

        // Reverse order from onCreate
        showNotification(false); //TODO Why?
        notificationManager.stop();
        notificationManager = null;

        egm2008CorrectionManager = null;
        try {
            voiceAnnouncementManager.shutdown();
        } finally {
            voiceAnnouncementManager = null;
        }

        // This should be the next to last operation
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);

        updateRecordingStatus(STATUS_DEFAULT);
        recordingStatusObservable = null;
        gpsStatusObservable = null;
        recordingDataObservable = null;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public Binder onBind(Intent intent) {
        return binder;
    }

    @Deprecated //TODO Should be @VisibleForTesting
    public boolean isRecording() {
        return recordingStatus.isRecording();
    }

    @VisibleForTesting
    public boolean isPaused() {
        return recordingStatus.isPaused();
    }

    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        if (!isRecording() || isPaused()) {
            return null;
        }

        return trackRecordingManager.insertMarker(name, category, description, photoUrl);
    }

    public Track.Id startNewTrack() {
        if (isRecording()) {
            Log.w(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }

        // Set recording status
        Track.Id trackId = trackRecordingManager.startNewTrack(trackPointCreator);
        updateRecordingStatus(RecordingStatus.record(trackId));

        startRecording();
        return trackId;
    }

    public void resumeTrack(Track.Id trackId) {
        trackPointCreator.reset();
        trackRecordingManager.resumeExistingTrack(trackId, trackPointCreator);

        updateRecordingStatus(RecordingStatus.record(trackId));

        startRecording();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void resumeCurrentTrack() {
        if (!isRecording() || !isPaused()) {
            Log.w(TAG, "Ignore resumeCurrentTrack. Not recording or not paused.");
            return;
        }

        resumeTrack(recordingStatus.getTrackId());
    }

    /**
     * Common code for starting a new track or resuming a track.
     */
    private void startRecording() {
        // Update instance variables
        handler.postDelayed(updateRecordingData, RECORDING_DATA_UPDATE_INTERVAL.toMillis());

        startSensors();

        voiceAnnouncementManager.restore(trackRecordingManager.getTrackStatistics());
    }

    public void tryStartSensors() {
        if (isRecording()) return;

        startSensors();
    }

    private void startSensors() {
        wakeLock = SystemUtils.acquireWakeLock(this, wakeLock);
        trackPointCreator.start(this);
        showNotification(true);
    }

    public void endCurrentTrack() {
        if (!isRecording()) {
            Log.w(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        // Need to remember the recordingTrackId before setting it to null
        boolean wasPause = isPaused();
        Track.Id trackId = recordingStatus.getTrackId();

        // Set recording status
        updateRecordingStatus(STATUS_DEFAULT);

        if (!wasPause) {
            trackRecordingManager.end(trackPointCreator);
        }

        endRecording(true);

        ExportUtils.postWorkoutExport(this, trackId);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void pauseCurrentTrack() {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore pauseCurrentTrack. Not recording or paused.");
            return;
        }

        // Set recording status
        updateRecordingStatus(recordingStatus.pause());

        trackRecordingManager.pause(trackPointCreator);

        endRecording(false);

        notificationManager.updateContent(getString(R.string.generic_paused));
    }

    /**
     * Common code for ending a track or pausing a track.
     *
     * @param trackStopped true if track is stopped, false if track is paused
     */
    private void endRecording(boolean trackStopped) {
        stopUpdateRecordingData();
        if (!trackStopped) {
            updateRecordingDataWhileRecording();
        } else {
            recordingDataObservable.postValue(NOT_RECORDING);
        }

        voiceAnnouncementManager.shutdown();

        // Update instance variables
        trackPointCreator.stop();

        stopSensors(trackStopped);
    }

    public void stopSensorsAndShutdown() {
        stopSensors(true);
    }

    void stopSensors(boolean shutdown) {
        if (!isRecording()) return;

        trackPointCreator.stop();
        showNotification(false);
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);
        if (shutdown) {
            stopSelf();
        }
    }

    @Override
    public boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording or paused.");
            return false;
        }

        boolean stored = trackRecordingManager.onNewTrackPoint(trackPoint);
        notificationManager.updateTrackPoint(this, trackRecordingManager.getTrackStatistics(), trackPoint, thresholdHorizontalAccuracy);
        return stored;
    }

    @Override
    public void newGpsStatus(GpsStatusValue gpsStatusValue) {
        notificationManager.updateContent(getString(gpsStatusValue.message));

        //TODO This check should not be necessary, but prevents a crash; somehow the shutdown is not working correctly as we should not receive a notification then.
        if (gpsStatusObservable != null) {
            gpsStatusObservable.postValue(gpsStatusValue);
        }
    }

    private void showNotification(boolean isGpsStarted) {
        if (isRecording()) {
            startForeground(TrackRecordingServiceNotificationManager.NOTIFICATION_ID, notificationManager.setRecording(this));
        }
        if (!isRecording() && isGpsStarted) {
            startForeground(TrackRecordingServiceNotificationManager.NOTIFICATION_ID, notificationManager.setGPSonlyStarted(this));
        }
        if (!isRecording() && !isGpsStarted) {
            stopForeground(true);
            notificationManager.cancelNotification();
        }
    }

    @Deprecated
    @VisibleForTesting
    public TrackPointCreator getTrackPointCreator() {
        return trackPointCreator;
    }

    @Deprecated
    @VisibleForTesting
    public TrackRecordingManager getTrackRecordingManager() {
        return trackRecordingManager;
    }

    public LiveData<GpsStatusValue> getGpsStatusObservable() {
        return gpsStatusObservable;
    }

    public LiveData<RecordingData> getRecordingDataObservable() {
        return recordingDataObservable;
    }

    private void updateRecordingDataWhileRecording() {
        if (!recordingStatus.isRecording()) {
            Log.w(TAG, "Currently not recording; cannot update data.");
            return;
        }

        // Compute temporary track statistics using sensorData and update time.

        TrackPointCreator localTrackPointCreator = this.trackPointCreator;
        VoiceAnnouncementManager localVoiceAnnouncementManager = this.voiceAnnouncementManager;
        if (localTrackPointCreator == null || localVoiceAnnouncementManager == null) {
            // when this happens, no recording is running and we should not send any notifications.
            //TODO This implementation is not a good idea; rather solve the issue for this properly
            return;
        }

        Pair<Track, Pair<TrackPoint, SensorDataSet>> data = trackRecordingManager.get(trackPointCreator);
        if (data == null) {
            Log.w(TAG, "Requesting data if not recording is taking place, should not be done.");
            return;
        }
        TrackPoint trackPoint = data.second.first;
        egm2008CorrectionManager.correctAltitude(this, trackPoint);

        localVoiceAnnouncementManager.update(data.first);

        recordingDataObservable.postValue(new RecordingData(data.first, trackPoint, data.second.second));
    }

    @VisibleForTesting
    public void stopUpdateRecordingData() {
        handler.removeCallbacks(updateRecordingData);
    }

    public LiveData<RecordingStatus> getRecordingStatusObservable() {
        return recordingStatusObservable;
    }

    private void updateRecordingStatus(RecordingStatus status) {
        Log.i(TAG, "new status " + recordingStatus + " -> " + status);
        recordingStatus = status;
        recordingStatusObservable.postValue(recordingStatus);
    }
}