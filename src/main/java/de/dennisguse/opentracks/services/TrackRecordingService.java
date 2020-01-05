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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.provider.TrackPointFactory;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.services.tasks.AnnouncementPeriodicTaskFactory;
import de.dennisguse.opentracks.services.tasks.PeriodicTaskExecutor;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.stats.TripStatisticsUpdater;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A background service that registers a location listener and records track points.
 * Track points are saved to the {@link CustomContentProvider}.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackRecordingService extends Service {

    // TODO Move to a different place.
    @Deprecated
    public static final double PAUSE_LATITUDE = 100.0;
    @Deprecated
    public static final double RESUME_LATITUDE = 200.0;

    // Anything faster than that (in meters per second) will be considered moving.
    private static final String TAG = TrackRecordingService.class.getSimpleName();

    public static final double MAX_NO_MOVEMENT_SPEED = 0.224;

    // The following variables are set in onCreate:
    private ExecutorService executorService;
    private ContentProviderUtils contentProviderUtils;
    private Handler handler;
    private LocationManagerConnector locationManagerConnector;
    private PeriodicTaskExecutor voiceExecutor;
    private TrackRecordingServiceNotificationManager notificationManager;
    private LocationListenerPolicy locationListenerPolicy;

    private long recordingTrackId;
    private boolean recordingTrackPaused;
    private int recordingDistanceInterval;
    private int maxRecordingDistance;
    private int recordingGpsAccuracy;
    private long currentRecordingInterval;

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
            if (PreferencesUtils.isKey(context, R.string.min_recording_interval_key, key)) {
                int minRecordingInterval = PreferencesUtils.getMinRecordingInterval(context);
                if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptBatteryLife(context)) {
                    // Choose battery life over moving time accuracy.
                    locationListenerPolicy = new AdaptiveLocationListenerPolicy(30 * UnitConversions.ONE_SECOND_MS, 5 * UnitConversions.ONE_MINUTE_MS, 5);
                } else if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptAccuracy(context)) {
                    // Get all the updates.
                    locationListenerPolicy = new AdaptiveLocationListenerPolicy(UnitConversions.ONE_SECOND_MS, 30 * UnitConversions.ONE_SECOND_MS, 0);
                } else {
                    locationListenerPolicy = new AbsoluteLocationListenerPolicy(minRecordingInterval * UnitConversions.ONE_SECOND_MS);
                }
            }
            if (PreferencesUtils.isKey(context, R.string.recording_distance_interval_key, key)) {
                recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(context);
            }
            if (PreferencesUtils.isKey(context, R.string.max_recording_distance_key, key)) {
                maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance(context);
            }
            if (PreferencesUtils.isKey(context, R.string.recording_gps_accuracy_key, key)) {
                recordingGpsAccuracy = PreferencesUtils.getRecordingGPSAccuracy(context);
            }
        }
    };

    // The following variables are set when recording:
    private WakeLock wakeLock;
    private BluetoothRemoteSensorManager remoteSensorManager;

    private TripStatisticsUpdater trackTripStatisticsUpdater;
    private Location lastLocation;
    private boolean currentSegmentHasLocation;
    private boolean isIdle;

    private TrackRecordingServiceBinder binder = new TrackRecordingServiceBinder(this);
    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(final Location location) {
            if (locationManagerConnector == null
                    || executorService == null
                    || executorService.isShutdown()
                    || executorService.isTerminated()) {
                return;
            }
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    onLocationChangedAsync(location);
                }
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.w(TAG, "LocationListener.onStatusChanged(): is not implemented.");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.w(TAG, "LocationListener.onProviderEnabled(): is not implemented.");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.w(TAG, "LocationListener.onProviderDisabled(): is not implemented.");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        contentProviderUtils = new ContentProviderUtils(this);
        handler = new Handler();
        locationManagerConnector = new LocationManagerConnector(this, handler.getLooper());
        voiceExecutor = new PeriodicTaskExecutor(this, new AnnouncementPeriodicTaskFactory());


        notificationManager = new TrackRecordingServiceNotificationManager(this);

        // onSharedPreferenceChanged might not set recordingTrackId.
        recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

        PreferencesUtils.register(this, sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

        // Try to restart the previous recording track in case the service has been restarted by the system, which can sometimes happen.
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track != null) {
            restartTrack(track);
        } else {
            if (isRecording()) {
                Log.w(TAG, "track is null, but recordingTrackId not -1L. " + recordingTrackId);
                updateRecordingState(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, true);
            }
            showNotification(false);
        }
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
        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }

        // Reverse order from onCreate
        showNotification(false); //TODO Why?

        unregisterLocationListener();

        PreferencesUtils.unregister(this, sharedPreferenceChangeListener);

        try {
            voiceExecutor.shutdown();
        } finally {
            voiceExecutor = null;
        }

        locationManagerConnector = null;
        contentProviderUtils = null;

        binder.detachFromService();
        binder = null;

        // This should be the next to last operation
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);

        // Shutdown the executorService last to avoid sending events to a dead executor.
        executorService.shutdown();
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

    public TripStatistics getTripStatistics() {
        if (trackTripStatisticsUpdater == null) {
            return null;
        }
        return trackTripStatisticsUpdater.getTripStatistics();
    }

    public long getTotalTime() {
        if (trackTripStatisticsUpdater == null) {
            return 0;
        }
        if (!isPaused()) {
            trackTripStatisticsUpdater.updateTime(System.currentTimeMillis());
        }
        return trackTripStatisticsUpdater.getTripStatistics().getTotalTime();
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

        Location location = getLastValidTrackPointInCurrentSegment(recordingTrackId);
        if (location == null) {
            Log.i(TAG, "Could not create a waypoint as location is unknown.");
            return -1L;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = getString(R.string.marker_waypoint_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";

        TripStatistics stats = trackTripStatisticsUpdater.getTripStatistics();
        double length = stats.getTotalDistance();
        long duration = stats.getTotalTime();

        // Insert waypoint
        Waypoint waypoint = new Waypoint(name, description, category, icon, recordingTrackId, length, duration, location, photoUrl);
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
        trackTripStatisticsUpdater = new TripStatisticsUpdater(now);

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
        track.setTripStatistics(trackTripStatisticsUpdater.getTripStatistics());
        contentProviderUtils.updateTrack(track);

        startRecording();
        return trackId;
    }

    private void restartTrack(Track track) {
        Log.d(TAG, "Restarting track: " + track.getId());

        TripStatistics tripStatistics = track.getTripStatistics();
        trackTripStatisticsUpdater = new TripStatisticsUpdater(tripStatistics.getStartTime());

        try (TrackPointIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), -1L, false, TrackPointFactory.DEFAULT_LOCATION_FACTORY)) {
            trackTripStatisticsUpdater.addLocation(locationIterator, recordingDistanceInterval);
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

        // Update shared preferences
        recordingTrackPaused = false;
        PreferencesUtils.setBoolean(this, R.string.recording_track_paused_key, false);

        // Update database
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track != null) {
            Location resume = new Location(LocationManager.GPS_PROVIDER);
            resume.setLongitude(0);
            resume.setLatitude(RESUME_LATITUDE);
            resume.setTime(System.currentTimeMillis());
            insertLocation(track, resume, null);
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
        lastLocation = null;
        currentSegmentHasLocation = false;
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
        registerLocationListener();
        showNotification(true);
    }

    void endCurrentTrack() {
        if (!isRecording()) {
            Log.d(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        // Need to remember the recordingTrackId before setting it to -1L
        long trackId = recordingTrackId;
        boolean paused = recordingTrackPaused;

        // Update shared preferences
        updateRecordingState(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, true);

        // Update database
        Track track = contentProviderUtils.getTrack(trackId);
        if (track != null) {
            // If not paused, add the last location
            if (!paused) {
                insertLocation(track, lastLocation, getLastValidTrackPointInCurrentSegment(trackId));

                // Update the recording track time
                updateRecordingTrack(track, false);
            }

            String trackName = TrackNameUtils.getTrackName(this, trackId, track.getTripStatistics().getStartTime());
            if (trackName != null && !trackName.equals(track.getName())) {
                track.setName(trackName);
                contentProviderUtils.updateTrack(track);
            }
        }
        endRecording(true);
    }

    void pauseCurrentTrack() {
        if (!isRecording() || isPaused()) {
            Log.d(TAG, "Ignore pauseCurrentTrack. Not recording or paused.");
            return;
        }

        // Update shared preferences
        recordingTrackPaused = true;
        PreferencesUtils.setBoolean(this, R.string.recording_track_paused_key, true);

        // Update database
        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track != null) {
            insertLocation(track, lastLocation, getLastValidTrackPointInCurrentSegment(track.getId()));

            Location pause = new Location(LocationManager.GPS_PROVIDER);
            pause.setLongitude(0);
            pause.setLatitude(PAUSE_LATITUDE);
            pause.setTime(System.currentTimeMillis());
            insertLocation(track, pause, null);
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
        lastLocation = null;

        stopGps(trackStopped);
    }

    /**
     * Stops gps.
     *
     * @param shutdown true to shutdown self
     */
    void stopGps(boolean shutdown) {
        if (!isRecording()) return;

        unregisterLocationListener();
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
    private Location getLastValidTrackPointInCurrentSegment(long trackId) {
        if (!currentSegmentHasLocation) {
            return null;
        }
        return contentProviderUtils.getLastValidTrackPoint(trackId);
    }

    /**
     * Updates the recording states.
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

    void onLocationChangedAsync(Location location) {
        if (!isRecording() || isPaused()) {
            Log.w(TAG, "Ignore onLocationChangedAsync. Not recording or paused.");
            return;
        }

        Track track = contentProviderUtils.getTrack(recordingTrackId);
        if (track == null) {
            Log.w(TAG, "Ignore onLocationChangedAsync. No track.");
            return;
        }

        if (!LocationUtils.isValidLocation(location)) {
            Log.w(TAG, "Ignore onLocationChangedAsync. location is invalid.");
            return;
        }

        notificationManager.updateLocation(this, location, recordingGpsAccuracy);

        if (!location.hasAccuracy() || location.getAccuracy() >= recordingGpsAccuracy) {
            Log.d(TAG, "Ignore onLocationChangedAsync. Poor accuracy.");
            return;
        }

        //TODO Necessary?
        // Fix for phones that do not set the time field
        if (location.getTime() == 0L) {
            location.setTime(System.currentTimeMillis());
        }

        Location lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(track.getId());
        long idleTime = 0L;
        if (lastValidTrackPoint != null && location.getTime() > lastValidTrackPoint.getTime()) {
            idleTime = location.getTime() - lastValidTrackPoint.getTime();
        }
        locationListenerPolicy.updateIdleTime(idleTime);
        if (currentRecordingInterval != locationListenerPolicy.getDesiredPollingInterval()) {
            registerLocationListener();
        }

        SensorDataSet sensorDataSet = getSensorDataSet();
        if (sensorDataSet != null) {
            location = new TrackPoint(location, sensorDataSet);
        }

        // Always insert the first segment location
        if (!currentSegmentHasLocation) {
            insertLocation(track, location, null);
            currentSegmentHasLocation = true;
            lastLocation = location;
            return;
        }

        if (!LocationUtils.isValidLocation(lastValidTrackPoint)) {
            // Should not happen. The current segment should have a location. Just insert the current location.
            insertLocation(track, location, null);
            lastLocation = location;
            return;
        }

        double distanceToLastTrackLocation = location.distanceTo(lastValidTrackPoint);
        if (distanceToLastTrackLocation > maxRecordingDistance) {
            insertLocation(track, lastLocation, lastValidTrackPoint);

            Location pause = new Location(LocationManager.GPS_PROVIDER);
            pause.setLongitude(0);
            pause.setLatitude(PAUSE_LATITUDE);
            pause.setTime(lastLocation.getTime());
            insertLocation(track, pause, null);

            insertLocation(track, location, null);
            isIdle = false;
        } else if (sensorDataSet != null || distanceToLastTrackLocation >= recordingDistanceInterval) {
            insertLocation(track, lastLocation, lastValidTrackPoint);
            insertLocation(track, location, null);
            isIdle = false;
        } else if (!isIdle && location.hasSpeed() && location.getSpeed() < MAX_NO_MOVEMENT_SPEED) {
            insertLocation(track, lastLocation, lastValidTrackPoint);
            insertLocation(track, location, null);
            isIdle = true;
        } else if (isIdle && location.hasSpeed() && location.getSpeed() >= MAX_NO_MOVEMENT_SPEED) {
            insertLocation(track, lastLocation, lastValidTrackPoint);
            insertLocation(track, location, null);
            isIdle = false;
        } else {
            Log.d(TAG, "Not recording location, idle");
        }
        lastLocation = location;
    }

    /**
     * Inserts a location.
     *
     * @param track               the track
     * @param location            the location
     * @param lastValidTrackPoint the last valid track point, can be null
     */
    private void insertLocation(Track track, Location location, Location lastValidTrackPoint) {
        if (location == null) {
            Log.w(TAG, "Ignore insertLocation. location is null.");
            return;
        }
        // Do not insert if inserted already
        if (lastValidTrackPoint != null && lastValidTrackPoint.getTime() == location.getTime()) {
            Log.w(TAG, "Ignore insertLocation. location time same as last valid track point time.");
            return;
        }

        try {
            contentProviderUtils.insertTrackPoint(location, track.getId());
            trackTripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
            updateRecordingTrack(track, LocationUtils.isValidLocation(location));
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
     * Increase the number of points if it is a new and valid track point.
     *
     * @param track                  the track
     * @param increaseNumberOfPoints true to increase the number of points
     */
    private void updateRecordingTrack(Track track, boolean increaseNumberOfPoints) {
        if (increaseNumberOfPoints) {
            track.setNumberOfPoints(track.getNumberOfPoints() + 1);
        }

        trackTripStatisticsUpdater.updateTime(System.currentTimeMillis());
        track.setTripStatistics(trackTripStatisticsUpdater.getTripStatistics());
        contentProviderUtils.updateTrack(track);
    }

    SensorDataSet getSensorDataSet() {
        if (remoteSensorManager == null || !remoteSensorManager.isEnabled() || !remoteSensorManager.isSensorDataSetValid()) {
            return null;
        }
        return remoteSensorManager.getSensorDataSet();
    }

    private void registerLocationListener() {
        if (locationManagerConnector == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        try {
            long interval = locationListenerPolicy.getDesiredPollingInterval();
            locationManagerConnector.requestLocationUpdates(interval, locationListenerPolicy.getMinDistance_m(), locationListener);
            currentRecordingInterval = interval;
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not register location listener.", e);
        }
    }

    private void unregisterLocationListener() {
        if (locationManagerConnector == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        locationManagerConnector.removeLocationUpdates(locationListener);
    }

    private void showNotification(boolean isGpsStarted) {
        // Dijkstra If
        if (isRecording() && isPaused()) {
            stopForeground(true);
            notificationManager.cancelNotification();

        }
        if (!isRecording() && !isGpsStarted) {
            stopForeground(true);
            notificationManager.cancelNotification();
        }
        if (isRecording() && !isPaused()) {
            Intent intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
                    .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, recordingTrackId);
            PendingIntent pendingIntent = TaskStackBuilder.create(this)
                    .addParentStack(TrackDetailActivity.class)
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
    }
}
