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

import androidx.annotation.VisibleForTesting;
import androidx.core.app.TaskStackBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.CustomContentProvider;
import de.dennisguse.opentracks.content.DescriptionGeneratorImpl;
import de.dennisguse.opentracks.content.LocationFactory;
import de.dennisguse.opentracks.content.LocationIterator;
import de.dennisguse.opentracks.content.SensorDataSetLocation;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.content.Waypoint.WaypointType;
import de.dennisguse.opentracks.content.WaypointCreationRequest;
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

    private static final int NOTIFICATION_ID = 123;

    public static final double PAUSE_LATITUDE = 100.0;
    public static final double RESUME_LATITUDE = 200.0;

    // Anything faster than that (in meters per second) will be considered moving.
    public static final double MAX_NO_MOVEMENT_SPEED = 0.224;
    private static final String TAG = TrackRecordingService.class.getSimpleName();
    // 1 minute in milliseconds
    private static final long ONE_MINUTE = (long) (UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);
    // The following variables are set in onCreate:
    private ExecutorService executorService;
    private ContentProviderUtils contentProviderUtils;
    private Handler handler;
    private LocationManagerConnector locationManagerConnector;
    private PeriodicTaskExecutor voiceExecutor;
    private SharedPreferences sharedPreferences;
    private TrackRecordingServiceNotificationManager notificationManager;
    private long recordingTrackId;
    private boolean recordingTrackPaused;
    private LocationListenerPolicy locationListenerPolicy;
    private int recordingDistanceInterval;
    private int maxRecordingDistance;
    private int recordingGpsAccuracy;
    private long currentRecordingInterval;

    // The following variables are set when recording:
    private TripStatisticsUpdater trackTripStatisticsUpdater;
    // Note that sharedPreferenceChangeListener cannot be an anonymous inner class; anonymous inner class will get garbage collected.
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
                    locationListenerPolicy = new AdaptiveLocationListenerPolicy(30 * UnitConversions.ONE_SECOND, 5 * ONE_MINUTE, 5);
                } else if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptAccuracy(context)) {
                    // Get all the updates.
                    locationListenerPolicy = new AdaptiveLocationListenerPolicy(UnitConversions.ONE_SECOND, 30 * UnitConversions.ONE_SECOND, 0);
                } else {
                    locationListenerPolicy = new AbsoluteLocationListenerPolicy(minRecordingInterval * UnitConversions.ONE_SECOND);
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

    @Deprecated //TODO Should be unused
    private TripStatisticsUpdater markerTripStatisticsUpdater;
    private WakeLock wakeLock;
    private BluetoothRemoteSensorManager remoteSensorManager;
    private Location lastLocation;
    private boolean currentSegmentHasLocation;
    private boolean isIdle; // true if idle
    private ServiceBinder binder = new ServiceBinder(this);
    private LocationListener locationListener = new LocationListener() {

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

    private final Runnable registerLocationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording() && !isPaused()) {
                registerLocationListener();
            }
            handler.postDelayed(this, ONE_MINUTE);
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
        sharedPreferences = PreferencesUtils.getSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        notificationManager = new TrackRecordingServiceNotificationManager(this);

        // onSharedPreferenceChanged might not set recordingTrackId.
        recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

        // Require voiceExecutor and splitExecutor to be created.
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        handler.post(registerLocationRunnable);

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

        handler.removeCallbacks(registerLocationRunnable);
        unregisterLocationListener();

        // unregister sharedPreferences before shutting down splitExecutor and voiceExecutor
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

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
        releaseWakeLock();

        // Shutdown the executorService last to avoid sending events to a dead executor.
        executorService.shutdown();
        super.onDestroy();
    }

    /**
     * Returns true if the service is recording.
     */
    public boolean isRecording() {
        return PreferencesUtils.isRecording(recordingTrackId);
    }

    /**
     * Returns true if the current recording is paused.
     */
    public boolean isPaused() {
        return recordingTrackPaused;
    }

    /**
     * Gets the trip statistics.
     */
    public TripStatistics getTripStatistics() {
        if (trackTripStatisticsUpdater == null) {
            return null;
        }
        return trackTripStatisticsUpdater.getTripStatistics();
    }

    /**
     * Inserts a waypoint.
     *
     * @param waypointCreationRequest the waypoint creation request
     * @return the waypoint id
     */
    public long insertWaypoint(WaypointCreationRequest waypointCreationRequest) {
        if (!isRecording() || isPaused()) {
            return -1L;
        }

        WaypointType waypointType = waypointCreationRequest.getType();
        boolean isStatistics = waypointType == WaypointType.STATISTICS;

        // Get name
        String name;
        if (waypointCreationRequest.getName() != null) {
            name = waypointCreationRequest.getName();
        } else {
            int nextWaypointNumber = contentProviderUtils.getNextWaypointNumber(recordingTrackId, waypointType);
            if (nextWaypointNumber == -1) {
                nextWaypointNumber = 0;
            }
            name = getString(isStatistics ? R.string.marker_split_name_format : R.string.marker_name_format, nextWaypointNumber);
        }

        // Get category
        String category = waypointCreationRequest.getCategory() != null ? waypointCreationRequest.getCategory() : "";

        // Get tripStatistics, description, and icon
        TripStatistics tripStatistics;
        String description;
        String icon;
        if (isStatistics) {
            long now = System.currentTimeMillis();
            markerTripStatisticsUpdater.updateTime(now);
            tripStatistics = markerTripStatisticsUpdater.getTripStatistics();
            markerTripStatisticsUpdater = new TripStatisticsUpdater(now);
            description = new DescriptionGeneratorImpl(this).generateWaypointDescription(tripStatistics);
            icon = getString(R.string.marker_statistics_icon_url);
        } else {
            tripStatistics = null;
            description = waypointCreationRequest.getDescription() != null ? waypointCreationRequest.getDescription() : "";
            //TODO Bundle icon?
            icon = getString(R.string.marker_waypoint_icon_url);
        }

        // Get length and duration
        double length;
        long duration;
        Location location = getLastValidTrackPointInCurrentSegment(recordingTrackId);
        if (location != null && trackTripStatisticsUpdater != null) {
            TripStatistics stats = trackTripStatisticsUpdater.getTripStatistics();
            length = stats.getTotalDistance();
            duration = stats.getTotalTime();
        } else {
            if (!waypointCreationRequest.isTrackStatistics()) {
                return -1L;
            }
            // For track statistics, make it an impossible location
            location = new Location("");
            location.setLatitude(100);
            location.setLongitude(180);
            length = 0.0;
            duration = 0L;
        }

        String photoUrl = waypointCreationRequest.getPhotoUrl() != null ? waypointCreationRequest.getPhotoUrl() : "";

        // Insert waypoint
        Waypoint waypoint = new Waypoint(name, description, category, icon, recordingTrackId, waypointType, length, duration, -1L, -1L, location, tripStatistics, photoUrl);
        Uri uri = contentProviderUtils.insertWaypoint(waypoint);
        return Long.parseLong(uri.getLastPathSegment());
    }

    /**
     * Starts a new track.
     *
     * @return the track id
     */
    private long startNewTrack() {
        if (isRecording()) {
            Log.d(TAG, "Ignore startNewTrack. Already recording.");
            return -1L;
        }
        long now = System.currentTimeMillis();
        trackTripStatisticsUpdater = new TripStatisticsUpdater(now);
        markerTripStatisticsUpdater = new TripStatisticsUpdater(now);

        // Insert a track
        Track track = new Track();
        Uri uri = contentProviderUtils.insertTrack(track);
        long trackId = Long.parseLong(uri.getLastPathSegment());

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
        insertWaypoint(WaypointCreationRequest.DEFAULT_START_TRACK);

        startRecording(true);
        return trackId;
    }

    /**
     * Restart a track.
     *
     * @param track the track
     */
    private void restartTrack(Track track) {
        Log.d(TAG, "Restarting track: " + track.getId());

        TripStatistics tripStatistics = track.getTripStatistics();
        trackTripStatisticsUpdater = new TripStatisticsUpdater(tripStatistics.getStartTime());

        long markerStartTime;
        Waypoint waypoint = contentProviderUtils.getLastWaypoint(recordingTrackId, WaypointType.STATISTICS);
        if (waypoint != null && waypoint.getTripStatistics() != null) {
            markerStartTime = waypoint.getTripStatistics().getStopTime();
        } else {
            markerStartTime = tripStatistics.getStartTime();
        }
        markerTripStatisticsUpdater = new TripStatisticsUpdater(markerStartTime);

        try (LocationIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), -1L, false, LocationFactory.DEFAULT_LOCATION_FACTORY)) {

            while (locationIterator.hasNext()) {
                Location location = locationIterator.next();
                trackTripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
                if (location.getTime() > markerStartTime) {
                    markerTripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException", e);
        }
        startRecording(true);
    }

    /**
     * Resumes current track.
     */
    private void resumeCurrentTrack() {
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

        startRecording(false);
    }

    /**
     * Common code for starting a new track, resuming a track, or restarting after phone reboot.
     *
     * @param trackStarted true if track is started, false if track is resumed
     */
    private void startRecording(boolean trackStarted) {
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

    /**
     * Starts gps.
     */
    private void startGps() {
        wakeLock = SystemUtils.acquireWakeLock(this, wakeLock);
        registerLocationListener();
        showNotification(true);
    }

    /**
     * Ends the current track.
     */
    private void endCurrentTrack() {
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
                updateRecordingTrack(track, contentProviderUtils.getLastTrackPointId(trackId), false);
            }

            String trackName = TrackNameUtils.getTrackName(this, trackId, track.getTripStatistics().getStartTime());
            if (trackName != null && !trackName.equals(track.getName())) {
                track.setName(trackName);
                contentProviderUtils.updateTrack(track);
            }
        }
        endRecording(true);
    }

    /**
     * Pauses the current track.
     */
    private void pauseCurrentTrack() {
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
     * @param stop true to stop self
     */
    private void stopGps(boolean stop) {
        unregisterLocationListener();
        showNotification(false);
        releaseWakeLock();
        if (stop) {
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

    /**
     * Called when location changed.
     *
     * @param location the location
     */
    private void onLocationChangedAsync(Location location) {
        try {
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
                location = new SensorDataSetLocation(location, sensorDataSet);
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
        } catch (Error e) {
            Log.e(TAG, "Error in onLocationChangedAsync", e);
            throw e;
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException in onLocationChangedAsync", e);
            throw e;
        }
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
            Uri uri = contentProviderUtils.insertTrackPoint(location, track.getId());
            long trackPointId = Long.parseLong(uri.getLastPathSegment());
            trackTripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
            markerTripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
            updateRecordingTrack(track, trackPointId, LocationUtils.isValidLocation(location));
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
     * Updates the recording track time as well as the startId and the stopId.
     * Increase the number of points if it is a new and valid track point.
     *
     * @param track                  the track
     * @param lastTrackPointId       the last track point id
     * @param increaseNumberOfPoints true to increase the number of points
     */
    private void updateRecordingTrack(Track track, long lastTrackPointId, boolean increaseNumberOfPoints) {
        if (lastTrackPointId >= 0) {
            if (track.getStartId() < 0) {
                track.setStartId(lastTrackPointId);
            }
            track.setStopId(lastTrackPointId);
        }
        if (increaseNumberOfPoints) {
            track.setNumberOfPoints(track.getNumberOfPoints() + 1);
        }

        trackTripStatisticsUpdater.updateTime(System.currentTimeMillis());
        track.setTripStatistics(trackTripStatisticsUpdater.getTripStatistics());
        contentProviderUtils.updateTrack(track);
    }

    private SensorDataSet getSensorDataSet() {
        if (remoteSensorManager == null || !remoteSensorManager.isEnabled() || !remoteSensorManager.isSensorDataSetValid()) {
            return null;
        }
        return remoteSensorManager.getSensorDataSet();
    }

    /**
     * Registers the location listener.
     */
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

    /**
     * Unregisters the location manager.
     */
    private void unregisterLocationListener() {
        if (locationManagerConnector == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        locationManagerConnector.removeLocationUpdates(locationListener);
    }

    /**
     * Releases the wake lock.
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void showNotification(boolean isGpsStarted) {
        if ((isRecording() && isPaused()) || (!isRecording() && !isGpsStarted)) {
            stopForeground(true);
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
            startForeground(NOTIFICATION_ID, notificationManager.getNotification());
        }
        if (!isRecording() && isGpsStarted) {
            Intent intent = IntentUtils.newIntent(this, TrackListActivity.class);
            PendingIntent pendingIntent = TaskStackBuilder.create(this)
                    .addNextIntent(intent)
                    .getPendingIntent(0, 0);

            notificationManager.updatePendingIntent(pendingIntent);
            notificationManager.updateContent(getString(R.string.gps_starting));

            startForeground(NOTIFICATION_ID, notificationManager.getNotification());
        }
    }

    /**
     * TODO: There is a bug in Android that leaks Binder instances. This bug is
     * especially visible if we have a non-static class, as there is no way to
     * nullify reference to the outer class (the service). A workaround is to use
     * a static class and explicitly clear service and detach it from the
     * underlying Binder. With this approach, we minimize the leak to 24 bytes per
     * each service instance. For more details, see the following bug:
     * http://code.google.com/p/android/issues/detail?id=6426.
     */
    private static class ServiceBinder extends android.os.Binder implements ITrackRecordingService {
        private TrackRecordingService trackRecordingService;

        public ServiceBinder(TrackRecordingService trackRecordingService) {
            this.trackRecordingService = trackRecordingService;
        }

        @Override
        public void startGps() {
            if (!trackRecordingService.isRecording()) {
                trackRecordingService.startGps();
            }
        }

        public void stopGps() {
            if (!trackRecordingService.isRecording()) {
                trackRecordingService.stopGps(true);
            }
        }

        @Override
        public long startNewTrack() {
            return trackRecordingService.startNewTrack();
        }

        @Override
        public void pauseCurrentTrack() {
            trackRecordingService.pauseCurrentTrack();
        }

        @Override
        public void resumeCurrentTrack() {
            trackRecordingService.resumeCurrentTrack();
        }

        @Override
        public void endCurrentTrack() {
            trackRecordingService.endCurrentTrack();
        }

        @Override
        public boolean isRecording() {
            return trackRecordingService.isRecording();
        }

        @Override
        public boolean isPaused() {
            return trackRecordingService.isPaused();
        }

        @Override
        public long getRecordingTrackId() {
            return trackRecordingService.recordingTrackId;
        }

        @Override
        public long getTotalTime() {
            TripStatisticsUpdater updater = trackRecordingService.trackTripStatisticsUpdater;
            if (updater == null) {
                return 0;
            }
            if (!trackRecordingService.isPaused()) {
                updater.updateTime(System.currentTimeMillis());
            }
            return updater.getTripStatistics().getTotalTime();
        }

        @Override
        public long insertWaypoint(WaypointCreationRequest waypointCreationRequest) {
            return trackRecordingService.insertWaypoint(waypointCreationRequest);
        }

        @VisibleForTesting
        @Override
        public void insertTrackPoint(Location location) {
            trackRecordingService.onLocationChangedAsync(location);
        }

        @Override
        public SensorDataSet getSensorData() {
            if (trackRecordingService.remoteSensorManager == null) {
                Log.d(TAG, "remoteSensorManager is null.");
                return null;
            }
            if (trackRecordingService.getSensorDataSet() == null) {
                Log.d(TAG, "Sensor data set is null.");
                return null;
            }
            return trackRecordingService.remoteSensorManager.getSensorDataSet();
        }

        /**
         * Detaches from the track recording service. Clears the reference to the
         * outer class to minimize the leak.
         */
        private void detachFromService() {
            trackRecordingService = null;
        }
    }
}
