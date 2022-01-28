package de.dennisguse.opentracks.services;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.time.ZoneOffset;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;

class TrackRecordingManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = TrackRecordingManager.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final Context context;

    private Distance recordingDistanceInterval;
    private Distance maxRecordingDistance;

    private Track.Id trackId;
    private TrackStatisticsUpdater trackStatisticsUpdater;

    private TrackPoint lastTrackPoint;
    private TrackPoint lastStoredTrackPoint;
    private TrackPoint lastStoredTrackPointWithLocation;

    TrackRecordingManager(Context context) {
        this.context = context;
        contentProviderUtils = new ContentProviderUtils(context);
    }

    public void start() {
        PreferencesUtils.registerOnSharedPreferenceChangeListener(this);
    }

    public void stop() {
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(this);
    }

    Track.Id startNewTrack(TrackPointCreator trackPointCreator) {
        TrackPoint segmentStartTrackPoint = trackPointCreator.createSegmentStartManual();
        // Create new track
        ZoneOffset zoneOffset = ZoneOffset.systemDefault().getRules().getOffset(segmentStartTrackPoint.getTime());
        Track track = new Track(zoneOffset);
        trackId = contentProviderUtils.insertTrack(track);
        track.setId(trackId);

        trackStatisticsUpdater = new TrackStatisticsUpdater();

        onNewTrackPoint(segmentStartTrackPoint);

        String category = PreferencesUtils.getDefaultActivity();
        track.setCategory(category);
        track.setIcon(TrackIconUtils.getIconValue(context, category));
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        //TODO Pass TrackPoint
        track.setName(TrackNameUtils.getTrackName(context, trackId, track.getStartTime()));
        contentProviderUtils.updateTrack(track);

        return trackId;
    }

    //TODO Handle non-existing trackId? Start a new track or exception?
    void resumeExistingTrack(@NonNull Track.Id resumeTrackId, @NonNull TrackPointCreator trackPointCreator) {
        trackId = resumeTrackId;
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId.getId() + " does not exists.");
            return;
        }

        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());
        onNewTrackPoint(trackPointCreator.createSegmentStartManual());

        lastTrackPoint = null;
        lastStoredTrackPoint = null;
        lastStoredTrackPointWithLocation = null;
    }

    void pause(TrackPointCreator trackPointCreator) {
        insertTrackPoint(trackPointCreator.createSegmentEnd());

        lastTrackPoint = null;
        lastStoredTrackPoint = null;
        lastStoredTrackPointWithLocation = null;
    }

    void end(TrackPointCreator trackPointCreator) {
        TrackPoint segmentEnd = trackPointCreator.createSegmentEnd();
        insertTrackPoint(segmentEnd);

        trackId = null;
        trackStatisticsUpdater = null;

        lastTrackPoint = null;
        lastStoredTrackPoint = null;
        lastStoredTrackPointWithLocation = null;
    }

    Pair<Track, Pair<TrackPoint, SensorDataSet>> get(TrackPointCreator trackPointCreator) {
        if (trackPointCreator == null) {
            return null;
        }
        TrackStatisticsUpdater tmpTrackStatisticsUpdater = new TrackStatisticsUpdater(trackStatisticsUpdater);
        Pair<TrackPoint, SensorDataSet> current = trackPointCreator.createCurrentTrackPoint(lastTrackPoint);

        tmpTrackStatisticsUpdater.addTrackPoint(current.first);

        Track track = contentProviderUtils.getTrack(trackId); //Get copy
        if (track == null) {
            Log.w(TAG, "Requesting data if not recording is taking place, should not be done.");
            return null;
        }

        track.setTrackStatistics(tmpTrackStatisticsUpdater.getTrackStatistics());

        return new Pair<>(track, current);
    }

    public Marker.Id insertMarker(String name, String category, String description, String photoUrl) {
        if (name == null) {
            Integer nextMarkerNumber = contentProviderUtils.getNextMarkerNumber(trackId);
            if (nextMarkerNumber == null) {
                nextMarkerNumber = 1;
            }
            name = context.getString(R.string.marker_name_format, nextMarkerNumber + 1);
        }

        if (lastStoredTrackPointWithLocation == null) {
            Log.i(TAG, "Could not create a marker as trackPoint is unknown.");
            return null;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = context.getString(R.string.marker_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";

        // Insert marker
        Marker marker = new Marker(name, description, category, icon, trackId, getTrackStatistics(), lastStoredTrackPointWithLocation, photoUrl);
        Uri uri = contentProviderUtils.insertMarker(marker);
        return new Marker.Id(ContentUris.parseId(uri));
    }

    /**
     * @return TrackPoint was stored?
     */
    boolean onNewTrackPoint(@NonNull TrackPoint trackPoint) {
        //Storing trackPoint

        // Always insert the first segment location
        if (lastStoredTrackPoint == null) {
            insertTrackPoint(trackPoint);
            return true;
        }

        if (trackPoint.hasLocation() && lastStoredTrackPointWithLocation == null) {
            insertTrackPoint(trackPoint);
            return true;
        }

        if (!trackPoint.hasLocation() && !trackPoint.hasSensorDistance()) {
            Log.d(TAG, "Ignoring TrackPoint as it has no distance.");
            return false;
        }

        Distance distanceToLastStoredTrackPoint;
        if (trackPoint.hasLocation() && !lastStoredTrackPoint.hasLocation()) {
            distanceToLastStoredTrackPoint = trackPoint.distanceToPreviousFromLocation(lastStoredTrackPointWithLocation);
        } else {
            distanceToLastStoredTrackPoint = trackPoint.distanceToPrevious(lastStoredTrackPoint);
        }

        if (distanceToLastStoredTrackPoint.greaterThan(maxRecordingDistance)) {
            trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
            insertTrackPoint(trackPoint);
            return true;
        }

        if (distanceToLastStoredTrackPoint.greaterOrEqualThan(recordingDistanceInterval)
                && trackPoint.isMoving()) {
            insertTrackPoint(trackPoint);
            return true;
        }

        if (trackPoint.isMoving() != lastStoredTrackPoint.isMoving()) {
            // Moving from non-moving to moving or vice versa; required to compute moving time correctly.
            insertTrackPoint(trackPoint);
            return true;
        }

        Log.d(TAG, "Not recording TrackPoint");
        lastTrackPoint = trackPoint;

        return false;
    }

    TrackStatistics getTrackStatistics() {
        return trackStatisticsUpdater.getTrackStatistics();
    }

    private void insertTrackPoint(@NonNull TrackPoint trackPoint) {
        if (lastTrackPoint != null) {
            if (lastStoredTrackPoint != null && lastTrackPoint.getTime().equals(lastStoredTrackPoint.getTime())) {
                // Do not insert if inserted already
                Log.w(TAG, "Ignore insertTrackPoint. trackPoint time same as last valid trackId point time.");
            } else {
                insertTrackPointHelper(lastTrackPoint);
                // Remove the sensorDistance from trackPoint that is already going  be stored with lastTrackPoint.
                trackPoint.minusCumulativeSensorData(lastTrackPoint);
            }
            lastTrackPoint = null;
        }

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
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.recording_distance_interval_key, key)) {
            recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval();
        }
        if (PreferencesUtils.isKey(R.string.max_recording_distance_key, key)) {
            maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance();
        }
    }
}
