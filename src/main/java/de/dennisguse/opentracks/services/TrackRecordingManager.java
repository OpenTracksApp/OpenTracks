package de.dennisguse.opentracks.services;

import android.content.ContentUris;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import de.dennisguse.opentracks.settings.PreferencesUtils;
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

    private boolean currentSegmentHasTrackPoint;
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

        String category = PreferencesUtils.getDefaultActivity();
        track.setCategory(category);
        track.setIcon(TrackIconUtils.getIconValue(context, category));
        track.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(track);

        currentSegmentHasTrackPoint = false;

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
        currentSegmentHasTrackPoint = false;
    }

    void pause(TrackPointCreator trackPointCreator) {
        if (lastTrackPoint != null) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);
        }
        insertTrackPoint(trackId, trackPointCreator.createSegmentEnd());
        currentSegmentHasTrackPoint = false;
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
        currentSegmentHasTrackPoint = false;
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

    boolean onNewTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        //TODO Figure out how to avoid loading the lastValidTrackPoint from the database
        TrackPoint lastValidTrackPoint = getLastValidTrackPointInCurrentSegment(trackId);


        //Storing trackPoint

        // Always insert the first segment location
        if (!currentSegmentHasTrackPoint) {
            insertTrackPoint(trackId, trackPoint);
            currentSegmentHasTrackPoint = true;
            lastTrackPoint = null;
            return true;
        }

        Distance distanceToLastTrackLocation = trackPoint.distanceToPrevious(lastValidTrackPoint);
        if (distanceToLastTrackLocation != null) {
            if (distanceToLastTrackLocation.greaterThan(maxRecordingDistance)) {
                insertTrackPointIfNewer(trackId, lastTrackPoint);

                trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
                insertTrackPoint(trackId, trackPoint);

                isIdle = false;
                lastTrackPoint = null;

                return true;
            }

            if (trackPoint.hasSensorData() || distanceToLastTrackLocation.greaterOrEqualThan(recordingDistanceInterval)) {
                insertTrackPointIfNewer(trackId, lastTrackPoint);

                insertTrackPoint(trackId, trackPoint);

                isIdle = false;
                lastTrackPoint = null;

                return true;
            }
        }

        if (!isIdle && !trackPoint.isMoving()) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);

            insertTrackPoint(trackId, trackPoint);

            isIdle = true;
            lastTrackPoint = null;

            return true;
        }

        if (isIdle && trackPoint.isMoving()) {
            insertTrackPointIfNewer(trackId, lastTrackPoint);

            insertTrackPoint(trackId, trackPoint);

            isIdle = false;
            lastTrackPoint = null;

            return true;
        }

        Log.d(TAG, "Not recording TrackPoint, idle");
        lastTrackPoint = trackPoint;
        return false;
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
        if (!currentSegmentHasTrackPoint) {
            return null;
        }
        return contentProviderUtils.getLastValidTrackPoint(trackId);
    }

    /**
     * Inserts a trackPoint if this trackPoint is different than lastValidTrackPoint.
     */
    private void insertTrackPointIfNewer(@NonNull Track.Id trackId, @Nullable TrackPoint trackPoint) {
        if (trackPoint == null) return;

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
            lastValidTrackPoint = trackPoint;
        } catch (SQLiteException e) {
            /*
             * Insert failed, most likely because of SqlLite error code 5 (SQLite_BUSY).
             * This is expected to happen extremely rarely (if our listener gets invoked twice at about the same time).
             */
            Log.w(TAG, "SQLiteException", e);
        }
    }

    public void onSharedPreferenceChanged(String key) {
        if (PreferencesUtils.isKey(R.string.recording_distance_interval_key, key)) {
            recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval();
        }
        if (PreferencesUtils.isKey(R.string.max_recording_distance_key, key)) {
            maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance();
        }
    }
}
