package de.dennisguse.opentracks.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.ZoneOffset;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.AltitudeCorrectionManager;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.TrackNameUtils;

public class TrackRecordingManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = TrackRecordingManager.class.getSimpleName();

    private static final AltitudeCorrectionManager ALTITUDE_CORRECTION_MANAGER = new AltitudeCorrectionManager();

    private final Runnable ON_IDLE = this::onIdle;

    private final ContentProviderUtils contentProviderUtils;
    private final Context context;
    private final IdleObserver idleObserver;

    private final Handler handler;

    private final TrackPointCreator trackPointCreator;

    private Distance recordingDistanceInterval;
    private Distance maxRecordingDistance;
    private Duration idleDuration;

    private Track.Id trackId;
    private TrackStatisticsUpdater trackStatisticsUpdater;

    private TrackPoint lastTrackPoint;
    private TrackPoint lastTrackPointUIWithSpeed;
    private TrackPoint lastTrackPointUIWithAltitude;

    private TrackPoint lastStoredTrackPoint;
    private TrackPoint lastStoredTrackPointWithLocation;

    TrackRecordingManager(Context context, TrackPointCreator trackPointCreator, IdleObserver idleObserver, Handler handler) {
        this.context = context;
        this.idleObserver = idleObserver;
        this.trackPointCreator = trackPointCreator;
        this.handler = handler;
        contentProviderUtils = new ContentProviderUtils(context);
    }

    Track.Id startNewTrack() {
        TrackPoint segmentStartTrackPoint = trackPointCreator.createSegmentStartManual();

        ZoneOffset zoneOffset = ZoneOffset.systemDefault().getRules().getOffset(segmentStartTrackPoint.getTime());
        Track track = new Track(zoneOffset);
        trackId = contentProviderUtils.insertTrack(track);
        track.setId(trackId);

        trackStatisticsUpdater = new TrackStatisticsUpdater();

        onNewTrackPoint(segmentStartTrackPoint);

        String activityTypeLocalized = PreferencesUtils.getDefaultActivityTypeLocalized();
        track.setActivityTypeLocalized(activityTypeLocalized);
        track.setActivityType(ActivityType.findByLocalizedString(context, activityTypeLocalized));
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        track.setName(TrackNameUtils.getTrackName(context, trackId, track.getStartTime()));
        contentProviderUtils.updateTrack(track);

        return trackId;
    }

    /**
     * @return if the recording could be started.
     */
    boolean resumeExistingTrack(@NonNull Track.Id resumeTrackId) {
        trackId = resumeTrackId;
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId.id() + " does not exists.");
            return false;
        }

        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());
        onNewTrackPoint(trackPointCreator.createSegmentStartManual());

        reset();

        return true;
    }

    void endCurrentTrack() {
        TrackPoint segmentEnd = trackPointCreator.createSegmentEnd();
        insertTrackPoint(segmentEnd, true);

        trackId = null;
        trackStatisticsUpdater = null;

        reset();
    }

    Pair<Track, Pair<TrackPoint, SensorDataSet>> getDataForUI() {
        TrackStatisticsUpdater tmpTrackStatisticsUpdater = new TrackStatisticsUpdater(trackStatisticsUpdater);
        Pair<TrackPoint, SensorDataSet> current = trackPointCreator.createCurrentTrackPoint(lastTrackPointUIWithSpeed, lastTrackPointUIWithAltitude, lastStoredTrackPointWithLocation);

        tmpTrackStatisticsUpdater.addTrackPoint(current.first);

        ALTITUDE_CORRECTION_MANAGER.correctAltitude(context, current.first);

        Track track = contentProviderUtils.getTrack(trackId); //Get copy
        if (track == null) {
            Log.w(TAG, "Requesting data if not recording is taking place, should not be done.");
            return null;
        }

        track.setTrackStatistics(tmpTrackStatisticsUpdater.getTrackStatistics());

        return new Pair<>(track, current);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void onIdle() {
        Log.d(TAG, "Becoming idle");
        onNewTrackPoint(trackPointCreator.createIdle());

        idleObserver.onIdle();
    }

    /**
     * @return TrackPoint was stored?
     */
    synchronized boolean onNewTrackPoint(@NonNull TrackPoint trackPoint) {
        if (trackPoint.hasSpeed()) {
            lastTrackPointUIWithSpeed = trackPoint;
        }
        if (trackPoint.hasAltitude()) {
            lastTrackPointUIWithAltitude = trackPoint;
        }

        if (trackPoint.getType() == TrackPoint.Type.IDLE) {
            insertTrackPoint(trackPoint, true);
            handler.removeCallbacks(ON_IDLE);
            return true;
        }
        //Storing trackPoint

        // Always insert the first segment location
        if (lastStoredTrackPoint == null) {
            insertTrackPoint(trackPoint, true);
            return true;
        }

        if (trackPoint.hasLocation() && lastStoredTrackPointWithLocation == null) {
            insertTrackPoint(trackPoint, true);

            scheduleNewIdleTimeout();
            return true;
        }

        if (!trackPoint.hasLocation() && !trackPoint.hasSensorDistance()) {
            Duration minStorageInterval = Duration.ofSeconds(10); // TODO Should be configurable.
            boolean shouldStore = lastStoredTrackPoint.getTime().plus(minStorageInterval)
                    .isBefore(trackPoint.getTime());
            if (!shouldStore) {
                Log.d(TAG, "Ignoring TrackPoint as it has no distance (and sensor data is not new enough).");
                return false;
            }

            insertTrackPoint(trackPoint, true);
            return true;
        }

        Distance distanceToLastStoredTrackPoint;
        if (trackPoint.hasLocation() && !lastStoredTrackPoint.hasLocation()) {
            distanceToLastStoredTrackPoint = trackPoint.distanceToPreviousFromLocation(lastStoredTrackPointWithLocation);
        } else {
            distanceToLastStoredTrackPoint = trackPoint.distanceToPrevious(lastStoredTrackPoint);
        }

        if (distanceToLastStoredTrackPoint.greaterThan(maxRecordingDistance)) {
            trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
            insertTrackPoint(trackPoint, true);

            scheduleNewIdleTimeout();
            return true;
        }

        if (distanceToLastStoredTrackPoint.greaterOrEqualThan(recordingDistanceInterval)) {
            insertTrackPoint(trackPoint, false);

            scheduleNewIdleTimeout();
            return true;
        }

        Log.d(TAG, "Not recording TrackPoint");
        lastTrackPoint = trackPoint;

        return false;
    }

    private void scheduleNewIdleTimeout() {
        if (idleDuration.isZero()) {
            Log.d(TAG, "idle functionality is disabled");
            return;
        }
        handler.removeCallbacks(ON_IDLE);
        handler.postDelayed(ON_IDLE, idleDuration.toMillis());
    }

    TrackStatistics getTrackStatistics() {
        return trackStatisticsUpdater == null ? null : trackStatisticsUpdater.getTrackStatistics();
    }

    private void insertTrackPoint(@NonNull TrackPoint trackPoint, boolean storeLastTrackPointIfUseful) {
        if (storeLastTrackPointIfUseful && lastTrackPoint != null) {
            if (lastStoredTrackPoint != null && lastTrackPoint.getTime().equals(lastStoredTrackPoint.getTime())) {
                // Do not insert if inserted already
                Log.w(TAG, "Ignore insertTrackPoint. trackPoint time same as last valid trackId point time.");
            } else {
                insertTrackPointHelper(lastTrackPoint);
                // Remove the sensorDistance from trackPoint that is already going  be stored with lastTrackPoint.
                trackPoint.minusCumulativeSensorData(lastTrackPoint);
            }
        }
        lastTrackPoint = null;

        insertTrackPointHelper(trackPoint);
    }

    private void insertTrackPointHelper(@NonNull TrackPoint trackPoint) {
        try {
            contentProviderUtils.insertTrackPoint(trackPoint, trackId);
            trackStatisticsUpdater.addTrackPoint(trackPoint);

            contentProviderUtils.updateTrackStatistics(trackId, trackStatisticsUpdater.getTrackStatistics());
            lastStoredTrackPoint = trackPoint;
            if (trackPoint.hasLocation()) {
                lastStoredTrackPointWithLocation = lastStoredTrackPoint;
            }
        } catch (SQLiteException e) {
            // TODO Remove; if this is a problem; use a synchronized method.
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
    }

    private void reset() {
        lastTrackPoint = null;
        lastTrackPointUIWithSpeed = null;
        lastTrackPointUIWithAltitude = null;

        lastStoredTrackPoint = null;
        lastStoredTrackPointWithLocation = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.recording_distance_interval_key, key)) {
            recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval();
        }
        if (PreferencesUtils.isKey(R.string.max_recording_distance_key, key)) {
            maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance();
        }
        if (PreferencesUtils.isKey(R.string.idle_duration_key, key)) {
            idleDuration = PreferencesUtils.getIdleDurationTimeout();
        }
    }

    public TrackPoint getLastStoredTrackPointWithLocation() {
        return lastStoredTrackPointWithLocation;
    }

    public interface IdleObserver {
        void onIdle();
    }
}
