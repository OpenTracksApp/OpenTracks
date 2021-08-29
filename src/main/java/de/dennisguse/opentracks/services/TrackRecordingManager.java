package de.dennisguse.opentracks.services;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackNameUtils;

class TrackRecordingManager {

    private static final String TAG = TrackRecordingManager.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final Context context;

    private Distance recordingDistanceInterval;
    private Distance maxRecordingDistance;

    private Track.Id trackId;
    private TrackStatisticsUpdater trackStatisticsUpdater;

    private TrackPoint lastTrackPoint;
    private TrackPoint lastValidTrackPoint;
    private boolean isIdle;

    TrackRecordingManager(Context context) {
        this.context = context;
        contentProviderUtils = new ContentProviderUtils(context);
    }

    Track.Id start(TrackPoint segmentStartTrackPoint) {
        // Create new track
        Track track = new Track();
        trackId = contentProviderUtils.insertTrack(track);
        track.setId(trackId);

        trackStatisticsUpdater = new TrackStatisticsUpdater();

        insertTrackPoint(trackId, segmentStartTrackPoint);

        //TODO Pass TrackPoint
        track.setName(TrackNameUtils.getTrackName(context, trackId, segmentStartTrackPoint.getTime()));

        String category = PreferencesUtils.getDefaultActivity(PreferencesUtils.getSharedPreferences(context), context); //TODO Re-use sharedpreferences
        track.setCategory(category);
        track.setIcon(TrackIconUtils.getIconValue(context, category));
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(track);

        return trackId;
    }

    //TODO Handle non-existing trackId? Start a new track or exception?
    void resume(@NonNull Track.Id resumeTrackId, @NonNull TrackPoint segmentStartTrackPoint) {
        trackId = resumeTrackId;
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "Ignore resumeTrack. Track " + trackId.getId() + " does not exists.");
            return;
        }

        trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics());
        insertTrackPoint(trackId, segmentStartTrackPoint);
    }

    void pause(TrackPointCreator trackPointCreator) {
        if (lastTrackPoint != null) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);
        }
        insertTrackPoint(trackId, trackPointCreator.createSegmentEnd());
    }

    void end(TrackPointCreator trackPointCreator) {
        if (lastTrackPoint != null) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);
        }

        TrackPoint segmentEnd = trackPointCreator.createSegmentEnd();
        insertTrackPoint(trackId, segmentEnd);

        trackId = null;
        trackStatisticsUpdater = null;
        lastTrackPoint = null;
        lastValidTrackPoint = null;
        isIdle = false;
    }

    Pair<Track, Pair<TrackPoint, SensorDataSet>> get(TrackPointCreator trackPointCreator) {
        if (trackPointCreator == null) {
            return null;
        }
        TrackStatisticsUpdater tmpTrackStatisticsUpdater = getTrackStatisticsUpdater();
        Pair<TrackPoint, SensorDataSet> current = trackPointCreator.createCurrentTrackPoint(lastTrackPoint);

        tmpTrackStatisticsUpdater.addTrackPoint(current.first, recordingDistanceInterval);

        Track track = getTrack(); //Get copy
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

        TrackPoint trackPoint = getLastValidTrackPointInCurrentSegment(trackId);
        if (trackPoint == null) {
            Log.i(TAG, "Could not create a marker as trackPoint is unknown.");
            return null;
        }

        category = category != null ? category : "";
        description = description != null ? description : "";
        String icon = context.getString(R.string.marker_icon_url);
        photoUrl = photoUrl != null ? photoUrl : "";


        // Insert marker
        Marker marker = new Marker(name, description, category, icon, trackId, getTrackStatistics(), trackPoint, photoUrl);
        Uri uri = contentProviderUtils.insertMarker(marker);
        return new Marker.Id(ContentUris.parseId(uri));
    }

    void onNewTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        //TODO Figure out how to avoid loading the lastValidTrackPoint from the database
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(trackId);

        //Storing trackPoint

        // Always insert the first segment location
        if (!currentSegmentHasTrackPoint()) {
            insertTrackPoint(trackId, trackPoint);
            lastTrackPoint = trackPoint;
            return;
        }

        Distance distanceToLastTrackLocation = trackPoint.distanceToPrevious(lastValidTrackPoint);
        if (distanceToLastTrackLocation != null) {
            if (distanceToLastTrackLocation.greaterThan(maxRecordingDistance)) {
                insertTrackPointIfNewer(trackId, lastTrackPoint);

                trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
                insertTrackPoint(trackId, trackPoint);

                isIdle = false;
                lastTrackPoint = trackPoint;
                return;
            }

            if (trackPoint.hasSensorData() || distanceToLastTrackLocation.greaterOrEqualThan(recordingDistanceInterval)) {
                insertTrackPointIfNewer(trackId, lastTrackPoint);

                insertTrackPoint(trackId, trackPoint);

                isIdle = false;

                lastTrackPoint = trackPoint;
                return;
            }
        }

        if (!isIdle && !trackPoint.isMoving()) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);

            insertTrackPoint(trackId, trackPoint);

            isIdle = true;

            lastTrackPoint = trackPoint;
            return;
        }

        if (isIdle && trackPoint.isMoving()) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);

            insertTrackPoint(trackId, trackPoint);

            isIdle = false;

            lastTrackPoint = trackPoint;
            return;
        }

        Log.d(TAG, "Not recording TrackPoint, idle");
        lastTrackPoint = trackPoint;
    }

    Track getTrack() {
        return contentProviderUtils.getTrack(trackId);
    }

    //Functionality that uses this method should happen here.
    @Deprecated
    TrackStatisticsUpdater getTrackStatisticsUpdater() {
        return new TrackStatisticsUpdater(trackStatisticsUpdater);
    }

    TrackStatistics getTrackStatistics() {
        return trackStatisticsUpdater.getTrackStatistics();
    }

    /**
     * Gets the last valid track point in the current segment.
     *
     * @param trackId the track id
     * @return the location or null
     */
    @Deprecated
    //Use lastValidTrackPoint
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
     * Inserts a trackPoint if this trackPoint is different than lastValidTrackPoint.
     */
    private void insertTrackPointIfNewer(@NonNull Track.Id trackId, @NonNull TrackPoint trackPoint) {
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(trackId);
        if (lastValidTrackPoint != null && trackPoint.getTime().equals(lastValidTrackPoint.getTime())) {
            // Do not insert if inserted already
            Log.w(TAG, "Ignore insertTrackPoint. trackPoint time same as last valid trackId point time.");
            return;
        }

        insertTrackPoint(trackId, trackPoint);
    }

    private void insertTrackPoint(@NonNull Track.Id trackId, @NonNull TrackPoint trackPoint) {
        try {
            contentProviderUtils.insertTrackPoint(trackPoint, trackId);
            trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);

            contentProviderUtils.updateTrackStatistics(trackId, trackStatisticsUpdater.getTrackStatistics());
        } catch (SQLiteException e) {
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
    }

    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(context, R.string.recording_distance_interval_key, key)) {
            recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(sharedPreferences, context);
        }
        if (PreferencesUtils.isKey(context, R.string.max_recording_distance_key, key)) {
            maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance(sharedPreferences, context);
        }
    }
}
