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

package de.dennisguse.opentracks.content.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.android.ContentResolverWrapper;
import de.dennisguse.opentracks.android.IContentResolver;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * {@link ContentProviderUtils} implementation.
 * Allows to use {@link ContentResolver} and {@link android.content.ContentProvider} interchangeably via {@link IContentResolver}.
 *
 * @author Leif Hendrik Wilden
 */
public class ContentProviderUtils {

    private static final String TAG = ContentProviderUtils.class.getSimpleName();
    private static final int MAX_LATITUDE = 90000000;

    // The authority (the first part of the URI) for the app's content provider.
    static final String AUTHORITY_PACKAGE = BuildConfig.APPLICATION_ID + ".content";

    // The base URI for the app's content provider.
    public static final String CONTENT_BASE_URI = "content://" + AUTHORITY_PACKAGE;

    // Maximum number of waypoints that will be loaded at one time.
    public static final int MAX_LOADED_WAYPOINTS_POINTS = 10000;
    private static final String ID_SEPARATOR = ",";

    private final IContentResolver contentResolver;
    private int defaultCursorBatchSize = 2000;

    public ContentProviderUtils(Context context) {
        this(context.getContentResolver());
    }

    public ContentProviderUtils(ContentResolver contentResolver) {
        this.contentResolver = new ContentResolverWrapper(contentResolver);
    }

    public ContentProviderUtils(IContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    /**
     * Clears a track: removes waypoints and trackPoints.
     * Only keeps the track id.
     *
     * @param trackId the track id
     */
    public void clearTrack(Context context, long trackId) {
        deleteTrackPointsAndWaypoints(context, trackId);
        Track track = new Track();
        track.setId(trackId);
        updateTrack(track);
    }

    /**
     * Creates a {@link Track} from a cursor.
     *
     * @param cursor the cursor pointing to the track
     */
    public Track createTrack(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
        int nameIndex = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
        int descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
        int categoryIndex = cursor.getColumnIndexOrThrow(TracksColumns.CATEGORY);
        int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int stopTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPTIME);
        int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int movingTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MOVINGTIME);
        int maxSpeedIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXSPEED);
        int minElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINELEVATION);
        int maxElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXELEVATION);
        int elevationGainIndex = cursor.getColumnIndexOrThrow(TracksColumns.ELEVATIONGAIN);
        int iconIndex = cursor.getColumnIndexOrThrow(TracksColumns.ICON);

        Track track = new Track();
        TrackStatistics trackStatistics = track.getTrackStatistics();
        if (!cursor.isNull(idIndex)) {
            track.setId(cursor.getLong(idIndex));
        }
        if (!cursor.isNull(nameIndex)) {
            track.setName(cursor.getString(nameIndex));
        }
        if (!cursor.isNull(descriptionIndex)) {
            track.setDescription(cursor.getString(descriptionIndex));
        }
        if (!cursor.isNull(categoryIndex)) {
            track.setCategory(cursor.getString(categoryIndex));
        }
        if (!cursor.isNull(startTimeIndex)) {
            trackStatistics.setStartTime_ms(cursor.getLong(startTimeIndex));
        }
        if (!cursor.isNull(stopTimeIndex)) {
            trackStatistics.setStopTime_ms(cursor.getLong(stopTimeIndex));
        }
        if (!cursor.isNull(totalDistanceIndex)) {
            trackStatistics.setTotalDistance(cursor.getFloat(totalDistanceIndex));
        }
        if (!cursor.isNull(totalTimeIndex)) {
            trackStatistics.setTotalTime(cursor.getLong(totalTimeIndex));
        }
        if (!cursor.isNull(movingTimeIndex)) {
            trackStatistics.setMovingTime(cursor.getLong(movingTimeIndex));
        }
        if (!cursor.isNull(maxSpeedIndex)) {
            trackStatistics.setMaxSpeed(cursor.getFloat(maxSpeedIndex));
        }
        if (!cursor.isNull(minElevationIndex)) {
            trackStatistics.setMinElevation(cursor.getFloat(minElevationIndex));
        }
        if (!cursor.isNull(maxElevationIndex)) {
            trackStatistics.setMaxElevation(cursor.getFloat(maxElevationIndex));
        }
        if (!cursor.isNull(elevationGainIndex)) {
            trackStatistics.setTotalElevationGain(cursor.getFloat(elevationGainIndex));
        }
        if (!cursor.isNull(iconIndex)) {
            track.setIcon(cursor.getString(iconIndex));
        }
        return track;
    }

    /**
     * Deletes all tracks (including waypoints and trackPoints).
     */
    public void deleteAllTracks(Context context) {
        contentResolver.delete(TrackPointsColumns.CONTENT_URI_BY_ID, null, null);
        contentResolver.delete(WaypointsColumns.CONTENT_URI, null, null);
        // Delete tracks last since it triggers a database vaccum call
        contentResolver.delete(TracksColumns.CONTENT_URI, null, null);

        File dir = FileUtils.getPhotoDir(context);
        deleteDirectoryRecurse(dir);
    }

    /**
     * Deletes a track.
     *
     * @param trackId the track id
     */
    public void deleteTrack(Context context, long trackId) {
        deleteTrackPointsAndWaypoints(context, trackId);

        // Delete track last since it triggers a database vacuum call
        contentResolver.delete(TracksColumns.CONTENT_URI, TracksColumns._ID + "=?", new String[]{Long.toString(trackId)});
    }

    /**
     * Deletes trackPoints and waypoints of a track.
     *
     * @param trackId the track id
     */
    private void deleteTrackPointsAndWaypoints(Context context, long trackId) {
        String where = TrackPointsColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        contentResolver.delete(TrackPointsColumns.CONTENT_URI_BY_ID, where, selectionArgs);

        contentResolver.delete(WaypointsColumns.CONTENT_URI, WaypointsColumns.TRACKID + "=?", new String[]{Long.toString(trackId)});
        deleteDirectoryRecurse(FileUtils.getPhotoDir(context, trackId));
    }

    /**
     * Delete the directory recursively.
     *
     * @param dir the directory
     */
    private void deleteDirectoryRecurse(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                deleteDirectoryRecurse(child);
            }
            dir.delete();
        }
    }

    /**
     * Gets all the tracks.
     *
     * @return the tracks do not have any trackPoints attached.
     */
    @VisibleForTesting
    public List<Track> getAllTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        try (Cursor cursor = getTrackCursor(null, null, TracksColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                tracks.ensureCapacity(cursor.getCount());
                do {
                    tracks.add(createTrack(cursor));
                } while (cursor.moveToNext());
            }
        }
        return tracks;
    }

    public Track getLastTrack() {
        try (Cursor cursor = getTrackCursor(null, null, TracksColumns.STARTTIME + " DESC")) {
            // Using the same order as shown in the track list
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    /**
     * @param trackId the track id.
     * @return the track doesn't have any trackPoints attached
     */
    public Track getTrack(long trackId) {
        if (trackId < 0) {
            return null;
        }
        try (Cursor cursor = getTrackCursor(TracksColumns._ID + "=?", new String[]{Long.toString(trackId)}, TracksColumns._ID)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a track cursor.
     * The caller owns the returned cursor and is responsible for closing it.
     *
     * @param selection     the selection. Can be null
     * @param selectionArgs the selection arguments. Can be null
     * @param sortOrder     the sort order. Can be null
     */
    public Cursor getTrackCursor(String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TracksColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);
    }

    /**
     * Inserts a track.
     * NOTE: This doesn't insert any trackPoints.
     *
     * @param track the track
     * @return the content provider URI of the inserted track.
     */
    public Uri insertTrack(Track track) {
        return contentResolver.insert(TracksColumns.CONTENT_URI, createContentValues(track));
    }

    /**
     * Updates a track.
     * NOTE: This doesn't update any trackPoints.
     *
     * @param track the track
     */
    public void updateTrack(Track track) {
        contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(track), TracksColumns._ID + "=?", new String[]{Long.toString(track.getId())});
    }

    private ContentValues createContentValues(Track track) {
        ContentValues values = new ContentValues();
        TrackStatistics trackStatistics = track.getTrackStatistics();

        // Value < 0 indicates no id is available
        if (track.getId() >= 0) {
            values.put(TracksColumns._ID, track.getId());
        }
        values.put(TracksColumns.NAME, track.getName());
        values.put(TracksColumns.DESCRIPTION, track.getDescription());
        values.put(TracksColumns.CATEGORY, track.getCategory());
        values.put(TracksColumns.STARTTIME, trackStatistics.getStartTime_ms());
        values.put(TracksColumns.STOPTIME, trackStatistics.getStopTime_ms());
        values.put(TracksColumns.TOTALDISTANCE, trackStatistics.getTotalDistance());
        values.put(TracksColumns.TOTALTIME, trackStatistics.getTotalTime());
        values.put(TracksColumns.MOVINGTIME, trackStatistics.getMovingTime());
        values.put(TracksColumns.AVGSPEED, trackStatistics.getAverageSpeed());
        values.put(TracksColumns.AVGMOVINGSPEED, trackStatistics.getAverageMovingSpeed());
        values.put(TracksColumns.MAXSPEED, trackStatistics.getMaxSpeed());
        values.put(TracksColumns.MINELEVATION, trackStatistics.getMinElevation());
        values.put(TracksColumns.MAXELEVATION, trackStatistics.getMaxElevation());
        values.put(TracksColumns.ELEVATIONGAIN, trackStatistics.getTotalElevationGain());
        values.put(TracksColumns.ICON, track.getIcon());

        return values;
    }


    /**
     * Creates a waypoint from a cursor.
     *
     * @param cursor the cursor pointing to the waypoint
     */
    public Waypoint createWaypoint(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(WaypointsColumns._ID);
        int nameIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.NAME);
        int descriptionIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.DESCRIPTION);
        int categoryIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.CATEGORY);
        int iconIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ICON);
        int trackIdIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TRACKID);
        int lengthIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LENGTH);
        int durationIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.DURATION);
        int longitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LONGITUDE);
        int latitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.LATITUDE);
        int timeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
        int altitudeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ALTITUDE);
        int accuracyIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.ACCURACY);
        int bearingIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.BEARING);
        int photoUrlIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.PHOTOURL);

        Location location = new Location("");
        if (!cursor.isNull(longitudeIndex) && !cursor.isNull(latitudeIndex)) {
            location.setLongitude(((double) cursor.getInt(longitudeIndex)) / 1E6);
            location.setLatitude(((double) cursor.getInt(latitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(timeIndex)) {
            location.setTime(cursor.getLong(timeIndex));
        }
        if (!cursor.isNull(altitudeIndex)) {
            location.setAltitude(cursor.getFloat(altitudeIndex));
        }
        if (!cursor.isNull(accuracyIndex)) {
            location.setAccuracy(cursor.getFloat(accuracyIndex));
        }
        if (!cursor.isNull(bearingIndex)) {
            location.setBearing(cursor.getFloat(bearingIndex));
        }

        Waypoint waypoint = new Waypoint(location);

        if (!cursor.isNull(idIndex)) {
            waypoint.setId(cursor.getLong(idIndex));
        }
        if (!cursor.isNull(nameIndex)) {
            waypoint.setName(cursor.getString(nameIndex));
        }
        if (!cursor.isNull(descriptionIndex)) {
            waypoint.setDescription(cursor.getString(descriptionIndex));
        }
        if (!cursor.isNull(categoryIndex)) {
            waypoint.setCategory(cursor.getString(categoryIndex));
        }
        if (!cursor.isNull(iconIndex)) {
            waypoint.setIcon(cursor.getString(iconIndex));
        }
        if (!cursor.isNull(trackIdIndex)) {
            waypoint.setTrackId(cursor.getLong(trackIdIndex));
        }
        if (!cursor.isNull(lengthIndex)) {
            waypoint.setLength(cursor.getFloat(lengthIndex));
        }
        if (!cursor.isNull(durationIndex)) {
            waypoint.setDuration(cursor.getLong(durationIndex));
        }

        if (!cursor.isNull(photoUrlIndex)) {
            waypoint.setPhotoUrl(cursor.getString(photoUrlIndex));
        }
        return waypoint;
    }

    public void deleteWaypoint(long waypointId) {
        final Waypoint waypoint = getWaypoint(waypointId);
        if (waypoint != null && waypoint.hasPhoto()) {
            Uri uri = waypoint.getPhotoURI();
            File file = new File(uri.getPath());
            if (file.exists()) {
                File parent = file.getParentFile();
                file.delete();
                if (parent.listFiles().length == 0) {
                    parent.delete();
                }
            }
        }
        contentResolver.delete(WaypointsColumns.CONTENT_URI, WaypointsColumns._ID + "=?", new String[]{Long.toString(waypointId)});
    }

    /**
     * Gets the next waypoint number.
     *
     * @param trackId the track id
     * @return -1 if not able to get the next waypoint number.
     */
    public int getNextWaypointNumber(long trackId) {
        if (trackId < 0) {
            return -1;
        }
        String[] projection = {WaypointsColumns._ID};
        String selection = WaypointsColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = getWaypointCursor(projection, selection, selectionArgs, WaypointsColumns._ID, -1)) {
            if (cursor != null) {
                return cursor.getCount();
            }
        }
        return -1;
    }

    public Waypoint getWaypoint(long waypointId) {
        if (waypointId < 0) {
            return null;
        }
        try (Cursor cursor = getWaypointCursor(null, WaypointsColumns._ID + "=?",
                new String[]{Long.toString(waypointId)}, WaypointsColumns._ID, 1)) {
            if (cursor != null && cursor.moveToFirst()) {
                return createWaypoint(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a waypoint cursor.
     * he caller owns the returned cursor and is responsible for closing it.
     *
     * @param selection     the selection. Can be null
     * @param selectionArgs the selection arguments. Can be null
     * @param sortOrder     the sort order. Can be null
     * @param maxWaypoints  the maximum number of waypoints to return. -1 for no limit
     */
    public Cursor getWaypointCursor(String selection, String[] selectionArgs, String sortOrder, int maxWaypoints) {
        return getWaypointCursor(null, selection, selectionArgs, sortOrder, maxWaypoints);
    }

    /**
     * Gets a waypoint cursor for a track.
     * The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId       the track id
     * @param minWaypointId the minimum waypoint id. -1L to ignore
     * @param maxWaypoints  the maximum number of waypoints to return. -1 for no limit
     */
    public Cursor getWaypointCursor(long trackId, long minWaypointId, int maxWaypoints) {
        if (trackId < 0) {
            return null;
        }

        String selection;
        String[] selectionArgs;
        if (minWaypointId >= 0) {
            selection = WaypointsColumns.TRACKID + "=? AND " + WaypointsColumns._ID + ">=?";
            selectionArgs = new String[]{Long.toString(trackId), Long.toString(minWaypointId)};
        } else {
            selection = WaypointsColumns.TRACKID + "=?";
            selectionArgs = new String[]{Long.toString(trackId)};
        }
        return getWaypointCursor(null, selection, selectionArgs, WaypointsColumns._ID, maxWaypoints);
    }

    @VisibleForTesting
    public List<Waypoint> getWaypoints(long trackId) {
        ArrayList<Waypoint> waypoints = new ArrayList<>();
        try (Cursor cursor = getWaypointCursor(trackId, -1L, -1)) {
            if (cursor.moveToFirst()) {
                do {
                    waypoints.add(createWaypoint(cursor));
                } while (cursor.moveToNext());
            }
        }
        return waypoints;
    }

    /**
     * Gets the number of waypoints for a track.
     *
     * @param trackId the track id
     */
    public int getWaypointCount(long trackId) {
        if (trackId < 0) {
            return 0;
        }

        String[] projection = new String[]{"count(*) AS count"};
        String selection = WaypointsColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = contentResolver.query(WaypointsColumns.CONTENT_URI, projection, selection, selectionArgs, WaypointsColumns._ID)) {
            if (cursor == null) {
                return 0;
            }
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    /**
     * Inserts a waypoint.
     *
     * @param waypoint the waypoint
     * @return the content provider URI of the inserted waypoint.
     */
    public Uri insertWaypoint(Waypoint waypoint) {
        waypoint.setId(-1L);
        return contentResolver.insert(WaypointsColumns.CONTENT_URI, createContentValues(waypoint));
    }

    /**
     * Updates a waypoint.
     * Returns true if successful.
     *
     * @param waypoint the waypoint
     */
    public boolean updateWaypoint(Waypoint waypoint) {
        int rows = contentResolver.update(WaypointsColumns.CONTENT_URI, createContentValues(waypoint), WaypointsColumns._ID + "=?", new String[]{Long.toString(waypoint.getId())});
        return rows == 1;
    }

    ContentValues createContentValues(Waypoint waypoint) {
        ContentValues values = new ContentValues();

        // Value < 0 indicates no id is available
        if (waypoint.getId() >= 0) {
            values.put(WaypointsColumns._ID, waypoint.getId());
        }
        values.put(WaypointsColumns.NAME, waypoint.getName());
        values.put(WaypointsColumns.DESCRIPTION, waypoint.getDescription());
        values.put(WaypointsColumns.CATEGORY, waypoint.getCategory());
        values.put(WaypointsColumns.ICON, waypoint.getIcon());
        values.put(WaypointsColumns.TRACKID, waypoint.getTrackId());
        values.put(WaypointsColumns.LENGTH, waypoint.getLength());
        values.put(WaypointsColumns.DURATION, waypoint.getDuration());

        Location location = waypoint.getLocation();
        values.put(WaypointsColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
        values.put(WaypointsColumns.LATITUDE, (int) (location.getLatitude() * 1E6));
        values.put(WaypointsColumns.TIME, location.getTime());
        if (location.hasAltitude()) {
            values.put(WaypointsColumns.ALTITUDE, location.getAltitude());
        }
        if (location.hasAccuracy()) {
            values.put(WaypointsColumns.ACCURACY, location.getAccuracy());
        }
        if (location.hasBearing()) {
            values.put(WaypointsColumns.BEARING, location.getBearing());
        }

        values.put(WaypointsColumns.PHOTOURL, waypoint.getPhotoUrl());
        return values;
    }

    /**
     * Gets a waypoint cursor.
     *
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection args
     * @param sortOrder     the sort order
     * @param maxWaypoints  the maximum number of waypoints
     */
    private Cursor getWaypointCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder, int maxWaypoints) {
        if (sortOrder == null) {
            sortOrder = WaypointsColumns._ID;
        }
        if (maxWaypoints >= 0) {
            sortOrder += " LIMIT " + maxWaypoints;
        }
        return contentResolver.query(WaypointsColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Fills a {@link TrackPoint} from a cursor.
     *
     * @param cursor  the cursor pointing to a trackPoint.
     * @param indexes the cached trackPoints indexes
     */
    static TrackPoint fillTrackPoint(Cursor cursor, CachedTrackPointsIndexes indexes) {
        TrackPoint trackPoint = new TrackPoint();

        if (!cursor.isNull(indexes.longitudeIndex)) {
            trackPoint.setLongitude(((double) cursor.getInt(indexes.longitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(indexes.latitudeIndex)) {
            trackPoint.setLatitude(((double) cursor.getInt(indexes.latitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(indexes.timeIndex)) {
            trackPoint.setTime(cursor.getLong(indexes.timeIndex));
        }
        if (!cursor.isNull(indexes.altitudeIndex)) {
            trackPoint.setAltitude(cursor.getFloat(indexes.altitudeIndex));
        }
        if (!cursor.isNull(indexes.accuracyIndex)) {
            trackPoint.setAccuracy(cursor.getFloat(indexes.accuracyIndex));
        }
        if (!cursor.isNull(indexes.speedIndex)) {
            trackPoint.setSpeed(cursor.getFloat(indexes.speedIndex));
        }
        if (!cursor.isNull(indexes.bearingIndex)) {
            trackPoint.setBearing(cursor.getFloat(indexes.bearingIndex));
        }

        if (!cursor.isNull(indexes.sensorHeartRateIndex)) {
            trackPoint.setHeartRate_bpm(cursor.getFloat(indexes.sensorHeartRateIndex));
        }
        if (!cursor.isNull(indexes.sensorCadenceIndex)) {
            trackPoint.setCyclingCadence_rpm(cursor.getFloat(indexes.sensorCadenceIndex));
        }
        if (!cursor.isNull(indexes.sensorPowerIndex)) {
            trackPoint.setPower(cursor.getFloat(indexes.sensorPowerIndex));
        }

        return trackPoint;
    }

    /**
     * Inserts multiple trackPoints.
     *
     * @param trackPoints an array of trackPoints
     * @param trackId     the trackPoints id
     * @return the number of trackPoints inserted
     */
    //TODO Only used for testing and file import; might be better to replace it.
    public int bulkInsertTrackPoint(TrackPoint[] trackPoints, long trackId) {
        ContentValues[] values = new ContentValues[trackPoints.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = createContentValues(trackPoints[i], trackId);
        }
        return contentResolver.bulkInsert(TrackPointsColumns.CONTENT_URI_BY_ID, values);
    }

    /**
     * Gets the first location id for a track.
     * Returns -1L if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public long getFirstTrackPointId(long trackId) {
        if (trackId < 0) {
            return -1L;
        }
        String selection = TrackPointsColumns._ID + "=(SELECT MIN(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
            }
        }
        return -1L;
    }

    /**
     * Gets the last location id for a track.
     * Returns -1L if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public long getLastTrackPointId(long trackId) {
        if (trackId < 0) {
            return -1L;
        }
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
            }
        }
        return -1L;
    }

    /**
     * Gets the trackPoint id for a location.
     *
     * @param trackId  the track id
     * @param location the location
     * @return trackPoint id if the location is in the track. -1L otherwise.
     */
    public long getTrackPointId(long trackId, Location location) {
        if (trackId < 0) {
            return -1L;
        }
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns.TIME + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId), Long.toString(location.getTime())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
            }
        }
        return -1L;
    }

    /**
     * Creates a {@link TrackPoint} object from a cursor.
     *
     * @param cursor the cursor pointing to the location
     */
    public TrackPoint createTrackPoint(Cursor cursor) {
        return fillTrackPoint(cursor, new CachedTrackPointsIndexes(cursor));
    }

    /**
     * Creates a location cursor. The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting trackPoint id. -1L to ignore
     * @param maxLocations      maximum number of locations to return. -1 for no limit
     * @param descending        true to sort the result in descending order (latest location first)
     */
    public Cursor getTrackPointCursor(long trackId, long startTrackPointId, int maxLocations, boolean descending) {
        if (trackId < 0) {
            return null;
        }

        String selection;
        String[] selectionArgs;
        if (startTrackPointId >= 0) {
            String comparison = descending ? "<=" : ">=";
            selection = TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns._ID + comparison + "?";
            selectionArgs = new String[]{Long.toString(trackId), Long.toString(startTrackPointId)};
        } else {
            selection = TrackPointsColumns.TRACKID + "=?";
            selectionArgs = new String[]{Long.toString(trackId)};
        }

        String sortOrder = TrackPointsColumns._ID;
        if (descending) {
            sortOrder += " DESC";
        }
        if (maxLocations >= 0) {
            sortOrder += " LIMIT " + maxLocations;
        }
        return getTrackPointCursor(null, selection, selectionArgs, sortOrder);
    }

    /**
     * Gets the last valid location for a track.
     * Returns null if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public TrackPoint getLastValidTrackPoint(long trackId) {
        if (trackId < 0) {
            return null;
        }
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns.LATITUDE + "<=" + MAX_LATITUDE + ")";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        return findTrackPointBy(selection, selectionArgs);
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     * @param trackId    the track id
     * @return the content provider URI of the inserted trackPoint
     */
    public Uri insertTrackPoint(TrackPoint trackPoint, long trackId) {
        return contentResolver.insert(TrackPointsColumns.CONTENT_URI_BY_ID, createContentValues(trackPoint, trackId));
    }

    /**
     * Creates the {@link ContentValues} for a {@link TrackPoint}.
     *
     * @param trackPoint the trackPoint
     * @param trackId    the track id
     */
    private ContentValues createContentValues(TrackPoint trackPoint, long trackId) {
        ContentValues values = new ContentValues();
        values.put(TrackPointsColumns.TRACKID, trackId);
        values.put(TrackPointsColumns.LONGITUDE, (int) (trackPoint.getLongitude() * 1E6));
        values.put(TrackPointsColumns.LATITUDE, (int) (trackPoint.getLatitude() * 1E6));

        values.put(TrackPointsColumns.TIME, trackPoint.getTime());
        if (trackPoint.hasAltitude()) {
            values.put(TrackPointsColumns.ALTITUDE, trackPoint.getAltitude());
        }
        if (trackPoint.hasAccuracy()) {
            values.put(TrackPointsColumns.ACCURACY, trackPoint.getAccuracy());
        }
        if (trackPoint.hasSpeed()) {
            values.put(TrackPointsColumns.SPEED, trackPoint.getSpeed());
        }
        if (trackPoint.hasBearing()) {
            values.put(TrackPointsColumns.BEARING, trackPoint.getBearing());
        }

        if (trackPoint.hasHeartRate()) {
            values.put(TrackPointsColumns.SENSOR_HEARTRATE, trackPoint.getHeartRate_bpm());
        }
        if (trackPoint.hasCyclingCadence()) {
            values.put(TrackPointsColumns.SENSOR_CADENCE, trackPoint.getCyclingCadence_rpm());
        }
        if (trackPoint.hasPower()) {
            values.put(TrackPointsColumns.SENSOR_POWER, trackPoint.getPower());
        }
        return values;
    }

    /**
     * Creates a new read-only iterator over a given track's points.
     * It provides a lightweight way of iterating over long tracks without failing due to the underlying cursor limitations.
     * Since it's a read-only iterator, {@link Iterator#remove()} always throws {@link UnsupportedOperationException}.
     * Each call to {@link TrackPointIterator#next()} may advance to the next DB record.
     * When done with iteration, {@link TrackPointIterator#close()} must be called.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting trackPoint id. -1L to ignore
     * @param descending        true to sort the result in descending order (latest location first)
     */
    public TrackPointIterator getTrackPointLocationIterator(final long trackId, final long startTrackPointId, final boolean descending) {
        return new TrackPointIterator(this, trackId, startTrackPointId, descending);
    }

    private TrackPoint findTrackPointBy(String selection, String[] selectionArgs) {
        try (Cursor cursor = getTrackPointCursor(null, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrackPoint(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a trackPoint cursor.
     *
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection arguments
     * @param sortOrder     the sort order
     */
    private Cursor getTrackPointCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, projection, selection, selectionArgs, sortOrder);
    }

    @VisibleForTesting
    public List<TrackPoint> getTrackPoints(long trackId) {
        List<TrackPoint> trackPoints = null;

        try (Cursor trackPointCursor = getTrackPointCursor(trackId, -1L, -1, false)) {
            if (trackPointCursor != null) {
                trackPointCursor.moveToFirst();
                trackPoints = new ArrayList<>(trackPointCursor.getCount());
                for (int i = 0; i < trackPointCursor.getCount(); i++) {
                    trackPoints.add(createTrackPoint(trackPointCursor));
                    trackPointCursor.moveToNext();
                }
            }
        }

        return trackPoints;
    }

    int getDefaultCursorBatchSize() {
        return defaultCursorBatchSize;
    }

    /**
     * Sets the default cursor batch size. For testing purpose.
     *
     * @param defaultCursorBatchSize the default cursor batch size
     */
    @VisibleForTesting
    void setDefaultCursorBatchSize(int defaultCursorBatchSize) {
        this.defaultCursorBatchSize = defaultCursorBatchSize;
    }

    /**
     * Formats an array of IDs as comma separated string value
     *
     * @param ids array with IDs
     * @return comma separated list of ids
     */
    public static String formatIdListForUri(long[] ids) {
        StringBuilder idsPathSegment = new StringBuilder();
        for (long id : ids) {
            if (idsPathSegment.length() > 0) {
                idsPathSegment.append(ID_SEPARATOR);
            }
            idsPathSegment.append(id);
        }
        return idsPathSegment.toString();
    }

    public static String[] parseTrackIdsFromUri(Uri url) {
        return TextUtils.split(url.getLastPathSegment(), ID_SEPARATOR);
    }

}
