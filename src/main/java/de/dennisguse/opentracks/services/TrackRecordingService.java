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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.exporter.ExportServiceResultReceiver;
import de.dennisguse.opentracks.services.handlers.EGM2008CorrectionManager;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.services.tasks.VoiceAnnouncementManager;
import de.dennisguse.opentracks.settings.SettingsActivity;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.ExportUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * A background service that registers a location listener and records track points.
 * Track points are saved to the {@link CustomContentProvider}.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service implements TrackPointCreator.Callback, ExportServiceResultReceiver.Receiver {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    private static final Duration RECORDING_DATA_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static final RecordingStatus STATUS_DEFAULT = RecordingStatus.notRecording();
    public static final RecordingData NOT_RECORDING = new RecordingData(null, null, null);
    public static final GpsStatusValue STATUS_GPS_DEFAULT = GpsStatusValue.GPS_NONE;

    // The following variables are setFrequency in onCreate:
    private VoiceAnnouncementManager voiceAnnouncementManager;
    private TrackRecordingServiceNotificationManager notificationManager;

    private TrackRecordingManager trackRecordingManager;

    private final EGM2008CorrectionManager egm2008CorrectionManager = new EGM2008CorrectionManager();

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

    private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Context context = TrackRecordingService.this;
            if (PreferencesUtils.isKey(context, R.string.stats_units_key, key)) {
                boolean metricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, context);
                notificationManager.setMetricUnits(metricUnits);
            }
            if (PreferencesUtils.isKey(context, R.string.voice_announcement_frequency_key, key)) {
                voiceAnnouncementManager.setFrequency(PreferencesUtils.getVoiceAnnouncementFrequency(sharedPreferences, context));
            }
            if (PreferencesUtils.isKey(context, new int[]{R.string.voice_announcement_distance_key, R.string.stats_units_key}, key)) {
                voiceAnnouncementManager.setFrequency(PreferencesUtils.getVoiceAnnouncementDistance(sharedPreferences, context));
            }

            trackPointCreator.onSharedPreferenceChanged(sharedPreferences, key);
            trackRecordingManager.onSharedPreferenceChanged(sharedPreferences, key);
        }
    };

    // The following variables are setFrequency when recording:
    private WakeLock wakeLock;

    private final Binder binder = new Binder();

    private TrackPointCreator trackPointCreator; //TODO Move to TrackRecordingManager?

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

        trackRecordingManager = new TrackRecordingManager(this);
        trackPointCreator = new TrackPointCreator(this);

        voiceAnnouncementManager = new VoiceAnnouncementManager(this);

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
        handler = null;

        trackPointCreator.stop();
        trackPointCreator = null;

        // Reverse order from onCreate
        showNotification(false); //TODO Why?

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferences = null;

        try {
            voiceAnnouncementManager.shutdown();
        } finally {
            voiceAnnouncementManager = null;
        }

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

    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        if (!isRecording() || isPaused()) {
            return null;
        }

        return trackRecordingManager.insertMarker(name, category, description, photoUrl);
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

        // Set recording status
        Track.Id trackId = trackRecordingManager.start(trackPointCreator.createSegmentStartManual());
        updateRecordingStatus(RecordingStatus.record(trackId));

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
        trackPointCreator.resetSensorData();
        trackRecordingManager.resume(trackId, trackPointCreator.createSegmentStartManual());

        // Set recording status
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

        startGps();

        voiceAnnouncementManager.restore(trackRecordingManager.getTrackStatistics());
    }

    public void tryStartGps() {
        if (isRecording()) return;

        startGps();
    }

    private void startGps() {
        wakeLock = SystemUtils.acquireWakeLock(this, wakeLock);
        trackPointCreator.start(this);
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
        Track.Id trackId = recordingStatus.getTrackId();

        // Set recording status
        updateRecordingStatus(STATUS_DEFAULT);

        if (!wasPause) {
            trackRecordingManager.end(trackPointCreator);
        }

        ExportUtils.postWorkoutExport(this, trackId, new ExportServiceResultReceiver(new Handler(), this));

        endRecording(true);
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
        handler.removeCallbacks(updateRecordingData);
        if (!trackStopped) {
            updateRecordingDataWhileRecording();
        } else {
            recordingDataObservable.postValue(NOT_RECORDING);
        }

        // Shutdown periodic tasks
        voiceAnnouncementManager.shutdown();

        // Update instance variables
        trackPointCreator.stop();

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

        trackPointCreator.stop();
        showNotification(false);
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);
        if (shutdown) {
            stopSelf();
        }
    }

    @Override
    public void newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording or paused.");
            return;
        }

        trackRecordingManager.onNewTrackPoint(trackPoint, thresholdHorizontalAccuracy);
        notificationManager.updateTrackPoint(this, trackRecordingManager.getTrackStatistics(), trackPoint, thresholdHorizontalAccuracy);
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
            Intent intent = IntentUtils.newIntent(this, TrackRecordingActivity.class)
                    .putExtra(TrackRecordingActivity.EXTRA_TRACK_ID, recordingStatus.getTrackId());
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

    @Deprecated
    @VisibleForTesting
    public TrackPointCreator getTrackPointCreator() {
        return trackPointCreator;
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

        private RecordingStatus(Track.Id trackId, boolean paused) {
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

        private static RecordingStatus notRecording() {
            return new RecordingStatus(null, false);
        }

        private static RecordingStatus record(@NonNull Track.Id trackId) {
            return new RecordingStatus(trackId, false);
        }

        private RecordingStatus pause() {
            return new RecordingStatus(getTrackId(), true);
        }

        public RecordingStatus stop() {
            return STATUS_DEFAULT;
        }

        @NonNull
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