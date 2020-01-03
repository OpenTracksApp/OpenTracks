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

package de.dennisguse.opentracks.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

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
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.stats.TripStatistics;
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

    /**
     * The authority (the first part of the URI) for the app's content provider.
     */
    static String AUTHORITY_PACKAGE = BuildConfig.APPLICATION_ID + ".content";

    /**
     * The base URI for the app's content provider.
     */
    public static String CONTENT_BASE_URI = "content://" + AUTHORITY_PACKAGE;

    /**
     * Maximum number of waypoints that will be loaded at one time.
     */
    public static int MAX_LOADED_WAYPOINTS_POINTS = 10000;

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
     * Clears a track: removes waypoints and trackpoints.
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
        int startIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTID);
        int stopIdIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPID);
        int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int stopTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPTIME);
        int numPointsIndex = cursor.getColumnIndexOrThrow(TracksColumns.NUMPOINTS);
        int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int movingTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MOVINGTIME);
        int maxSpeedIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXSPEED);
        int minElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINELEVATION);
        int maxElevationIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXELEVATION);
        int elevationGainIndex = cursor.getColumnIndexOrThrow(TracksColumns.ELEVATIONGAIN);
        int minGradeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MINGRADE);
        int maxGradeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXGRADE);
        int iconIndex = cursor.getColumnIndexOrThrow(TracksColumns.ICON);

        Track track = new Track();
        TripStatistics tripStatistics = track.getTripStatistics();
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
            tripStatistics.setStartTime(cursor.getLong(startTimeIndex));
        }
        if (!cursor.isNull(stopTimeIndex)) {
            tripStatistics.setStopTime(cursor.getLong(stopTimeIndex));
        }
        if (!cursor.isNull(numPointsIndex)) {
            track.setNumberOfPoints(cursor.getInt(numPointsIndex));
        }
        if (!cursor.isNull(totalDistanceIndex)) {
            tripStatistics.setTotalDistance(cursor.getFloat(totalDistanceIndex));
        }
        if (!cursor.isNull(totalTimeIndex)) {
            tripStatistics.setTotalTime(cursor.getLong(totalTimeIndex));
        }
        if (!cursor.isNull(movingTimeIndex)) {
            tripStatistics.setMovingTime(cursor.getLong(movingTimeIndex));
        }
        if (!cursor.isNull(maxSpeedIndex)) {
            tripStatistics.setMaxSpeed(cursor.getFloat(maxSpeedIndex));
        }
        if (!cursor.isNull(minElevationIndex)) {
            tripStatistics.setMinElevation(cursor.getFloat(minElevationIndex));
        }
        if (!cursor.isNull(maxElevationIndex)) {
            tripStatistics.setMaxElevation(cursor.getFloat(maxElevationIndex));
        }
        if (!cursor.isNull(elevationGainIndex)) {
            tripStatistics.setTotalElevationGain(cursor.getFloat(elevationGainIndex));
        }
        if (!cursor.isNull(minGradeIndex)) {
            tripStatistics.setMinGrade(cursor.getFloat(minGradeIndex));
        }
        if (!cursor.isNull(maxGradeIndex)) {
            tripStatistics.setMaxGrade(cursor.getFloat(maxGradeIndex));
        }
        if (!cursor.isNull(iconIndex)) {
            track.setIcon(cursor.getString(iconIndex));
        }
        return track;
    }

    /**
     * Deletes all tracks (including waypoints and track points).
     */
    public void deleteAllTracks(Context context) {
        contentResolver.delete(TrackPointsColumns.CONTENT_URI, null, null);
        contentResolver.delete(WaypointsColumns.CONTENT_URI, null, null);
        // Delete tracks last since it triggers a database vaccum call
        contentResolver.delete(TracksColumns.CONTENT_URI, null, null);

        File dir = FileUtils.getPhotoDir(context);
        deleteDirectoryRecurse(context, dir);
    }

    /**
     * Deletes a track.
     *
     * @param trackId the track id
     */
    public void deleteTrack(Context context, long trackId) {
        deleteTrackPointsAndWaypoints(context, trackId);

        // Delete track last since it triggers a database vaccum call
        contentResolver.delete(TracksColumns.CONTENT_URI, TracksColumns._ID + "=?",
                new String[]{Long.toString(trackId)});
    }

    /**
     * Deletes track points and waypoints of a track.
     * Assumes {@link TracksColumns#NUMPOINTS} will be updated by the caller.
     *
     * @param trackId the track id
     */
    private void deleteTrackPointsAndWaypoints(Context context, long trackId) {
        String where = TrackPointsColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        contentResolver.delete(TrackPointsColumns.CONTENT_URI, where, selectionArgs);

        contentResolver.delete(WaypointsColumns.CONTENT_URI, WaypointsColumns.TRACKID + "=?",
                new String[]{Long.toString(trackId)});
        deleteDirectoryRecurse(context, FileUtils.getPhotoDir(context, trackId));
    }

    /**
     * Delete the directory recursively.
     *
     * @param dir the directory
     */
    private void deleteDirectoryRecurse(Context context, File dir) {
        if (dir.exists() && dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                deleteDirectoryRecurse(context, child);
            }
            dir.delete();
        }
    }

    /**
     * Gets all the tracks.
     * If no track exists, an empty list is returned.
     * NOTE: the returned tracks do not have any track points attached.
     */
    public List<Track> getAllTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        try (Cursor cursor = getTrackCursor(null, null, null, TracksColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                tracks.ensureCapacity(cursor.getCount());
                do {
                    tracks.add(createTrack(cursor));
                } while (cursor.moveToNext());
            }
        }
        return tracks;
    }

    /**
     * Gets the last track or null.
     */
    public Track getLastTrack() {
        try (Cursor cursor = getTrackCursor(null, null, null, TracksColumns.STARTTIME + " DESC")) {
            // Using the same order as shown in the track list
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a track by a track id or null
     * Note that the returned track doesn't have any track points attached.
     *
     * @param trackId the track id.
     */
    public Track getTrack(long trackId) {
        if (trackId < 0) {
            return null;
        }
        try (Cursor cursor = getTrackCursor(null, TracksColumns._ID + "=?", new String[]{Long.toString(trackId)}, TracksColumns._ID)) {
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
        return getTrackCursor(null, selection, selectionArgs, sortOrder);
    }

    /**
     * Inserts a track.
     * NOTE: This doesn't insert any track points.
     *
     * @param track the track
     * @return the content provider URI of the inserted track.
     */
    public Uri insertTrack(Track track) {
        return contentResolver.insert(TracksColumns.CONTENT_URI, createContentValues(track));
    }

    /**
     * Updates a track.
     * NOTE: This doesn't update any track points.
     *
     * @param track the track
     */
    public void updateTrack(Track track) {
        contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(track),
                TracksColumns._ID + "=?", new String[]{Long.toString(track.getId())});
    }

    private ContentValues createContentValues(Track track) {
        ContentValues values = new ContentValues();
        TripStatistics tripStatistics = track.getTripStatistics();

        // Value < 0 indicates no id is available
        if (track.getId() >= 0) {
            values.put(TracksColumns._ID, track.getId());
        }
        values.put(TracksColumns.NAME, track.getName());
        values.put(TracksColumns.DESCRIPTION, track.getDescription());
        values.put(TracksColumns.CATEGORY, track.getCategory());
        values.put(TracksColumns.STARTTIME, tripStatistics.getStartTime());
        values.put(TracksColumns.STOPTIME, tripStatistics.getStopTime());
        values.put(TracksColumns.NUMPOINTS, track.getNumberOfPoints());
        values.put(TracksColumns.TOTALDISTANCE, tripStatistics.getTotalDistance());
        values.put(TracksColumns.TOTALTIME, tripStatistics.getTotalTime());
        values.put(TracksColumns.MOVINGTIME, tripStatistics.getMovingTime());
        values.put(TracksColumns.AVGSPEED, tripStatistics.getAverageSpeed());
        values.put(TracksColumns.AVGMOVINGSPEED, tripStatistics.getAverageMovingSpeed());
        values.put(TracksColumns.MAXSPEED, tripStatistics.getMaxSpeed());
        values.put(TracksColumns.MINELEVATION, tripStatistics.getMinElevation());
        values.put(TracksColumns.MAXELEVATION, tripStatistics.getMaxElevation());
        values.put(TracksColumns.ELEVATIONGAIN, tripStatistics.getTotalElevationGain());
        values.put(TracksColumns.MINGRADE, tripStatistics.getMinGrade());
        values.put(TracksColumns.MAXGRADE, tripStatistics.getMaxGrade());
        values.put(TracksColumns.ICON, track.getIcon());

        return values;
    }

    /**
     * Gets a track cursor.
     *
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection arguments
     * @param sortOrder     the sort oder
     */
    private Cursor getTrackCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TracksColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
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
        int speedIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.SPEED);
        int bearingIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.BEARING);
        int photoUrlIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.PHOTOURL);

        Waypoint waypoint = new Waypoint();

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
        if (!cursor.isNull(speedIndex)) {
            location.setSpeed(cursor.getFloat(speedIndex));
        }
        if (!cursor.isNull(bearingIndex)) {
            location.setBearing(cursor.getFloat(bearingIndex));
        }
        waypoint.setLocation(location);

        if (!cursor.isNull(photoUrlIndex)) {
            waypoint.setPhotoUrl(cursor.getString(photoUrlIndex));
        }
        return waypoint;
    }

    /**
     * Deletes a waypoint.
     * If deleting a statistics waypoint, this will also correct the next statistics waypoint after the deleted one to reflect the  deletion.
     * The generator is used to update the next statistics waypoint.
     *
     * @param waypointId the waypoint id
     */

    public void deleteWaypoint(Context context, long waypointId) {
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
     * Gets the last waypoint for a type. Returns null if it doesn't exist.
     *
     * @param trackId the track id
     */
    public Waypoint getLastWaypoint(long trackId) {
        if (trackId < 0) {
            return null;
        }
        String selection = WaypointsColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = getWaypointCursor(null, selection, selectionArgs, WaypointsColumns._ID + " DESC", 1)) {

            if (cursor != null && cursor.moveToFirst()) {
                return createWaypoint(cursor);
            }
        }
        return null;
    }

    /**
     * Gets the next waypoint number for a type.
     * Returns -1 if not able to get the next waypoint number.
     *
     * @param trackId the track id
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

    /**
     * Gets a waypoint from a waypoint id.
     * Returns null if not found.
     *
     * @param waypointId the waypoint id
     */
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
     * @param maxWaypoints  the maximum number of waypoints to return. -1 for no
     *                      limit
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
        Cursor cursor = contentResolver.query(WaypointsColumns.CONTENT_URI, projection, selection, selectionArgs, WaypointsColumns._ID);

        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
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
        if (location != null) {
            values.put(WaypointsColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
            values.put(WaypointsColumns.LATITUDE, (int) (location.getLatitude() * 1E6));
            values.put(WaypointsColumns.TIME, location.getTime());
            if (location.hasAltitude()) {
                values.put(WaypointsColumns.ALTITUDE, location.getAltitude());
            }
            if (location.hasAccuracy()) {
                values.put(WaypointsColumns.ACCURACY, location.getAccuracy());
            }
            if (location.hasSpeed()) {
                values.put(WaypointsColumns.SPEED, location.getSpeed());
            }
            if (location.hasBearing()) {
                values.put(WaypointsColumns.BEARING, location.getBearing());
            }
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
     * Inserts multiple track points.
     *
     * @param locations an array of locations
     * @param length    the number of locations (from the beginning of the array) to
     *                  insert, or -1 for all of them
     * @param trackId   the track id
     * @return the number of points inserted
     */
    public int bulkInsertTrackPoint(Location[] locations, int length, long trackId) {
        if (length == -1) {
            length = locations.length;
        }
        ContentValues[] values = new ContentValues[length];
        for (int i = 0; i < length; i++) {
            values[i] = createContentValues(locations[i], trackId);
        }
        return contentResolver.bulkInsert(TrackPointsColumns.CONTENT_URI, values);
    }

    /**
     * Creates a location object from a cursor.
     *
     * @param cursor the cursor pointing to the location
     */
    public Location createTrackPoint(Cursor cursor) {
        Location location = new TrackPoint("");
        fillTrackPoint(cursor, new CachedTrackPointsIndexes(cursor), location);
        return location;
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
        String selection = TrackPointsColumns._ID + "=(select min(" + TrackPointsColumns._ID
                + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID
                + "=?)";
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
        String selection = TrackPointsColumns._ID + "=(select max(" + TrackPointsColumns._ID
                + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID
                + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
            }
        }
        return -1L;
    }

    /**
     * Gets the track point id of a location.
     *
     * @param trackId  the track id
     * @param location the location
     * @return track point id if the location is in the track. -1L otherwise.
     */
    public long getTrackPointId(long trackId, Location location) {
        if (trackId < 0) {
            return -1L;
        }
        String selection = TrackPointsColumns._ID + "=(select max(" + TrackPointsColumns._ID
                + ") from " + TrackPointsColumns.TABLE_NAME
                + " WHERE " + TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns.TIME + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId), Long.toString(location.getTime())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID));
            }
        }
        return -1L;
    }

    /**
     * Gets the last valid location for a track.
     * Returns null if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public Location getLastValidTrackPoint(long trackId) {
        if (trackId < 0) {
            return null;
        }
        String selection = TrackPointsColumns._ID + "=(select max(" + TrackPointsColumns._ID + ") from "
                + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND "
                + TrackPointsColumns.LATITUDE + "<=" + MAX_LATITUDE + ")";
        String[] selectionArgs = new String[]{Long.toString(trackId)};
        return findTrackPointBy(selection, selectionArgs);
    }

    /**
     * Creates a location cursor. The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting track point id. -1L to ignore
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
            selection = TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns._ID + comparison
                    + "?";
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
     * Fills a track point from a cursor.
     *
     * @param cursor   the cursor pointing to a location.
     * @param indexes  the cached track points indexes
     * @param location the track point
     */
    static void fillTrackPoint(Cursor cursor, CachedTrackPointsIndexes indexes, Location location) {
        location.reset();

        if (!cursor.isNull(indexes.longitudeIndex)) {
            location.setLongitude(((double) cursor.getInt(indexes.longitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(indexes.latitudeIndex)) {
            location.setLatitude(((double) cursor.getInt(indexes.latitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(indexes.timeIndex)) {
            location.setTime(cursor.getLong(indexes.timeIndex));
        }
        if (!cursor.isNull(indexes.altitudeIndex)) {
            location.setAltitude(cursor.getFloat(indexes.altitudeIndex));
        }
        if (!cursor.isNull(indexes.accuracyIndex)) {
            location.setAccuracy(cursor.getFloat(indexes.accuracyIndex));
        }
        if (!cursor.isNull(indexes.speedIndex)) {
            location.setSpeed(cursor.getFloat(indexes.speedIndex));
        }
        if (!cursor.isNull(indexes.bearingIndex)) {
            location.setBearing(cursor.getFloat(indexes.bearingIndex));
        }
        if (location instanceof TrackPoint) {
            TrackPoint sensorDataSetLocation = (TrackPoint) location;

            float heartRate = cursor.isNull(indexes.sensorHeartRateIndex) ? SensorDataSet.DATA_UNAVAILABLE : cursor.getFloat(indexes.sensorHeartRateIndex);
            float cadence = cursor.isNull(indexes.sensorCadenceIndex) ? SensorDataSet.DATA_UNAVAILABLE : cursor.getFloat(indexes.sensorCadenceIndex);
            float power = cursor.isNull(indexes.sensorPowerIndex) ? SensorDataSet.DATA_UNAVAILABLE : cursor.getFloat(indexes.sensorPowerIndex);

            sensorDataSetLocation.setSensorDataSet(new SensorDataSet(heartRate, cadence, power, SensorDataSet.DATA_UNAVAILABLE, location.getTime()));
        }
    }

    /**
     * Inserts a track point.
     *
     * @param location the location
     * @param trackId  the track id
     * @return the content provider URI of the inserted track point
     */
    public Uri insertTrackPoint(Location location, long trackId) {
        return contentResolver.insert(TrackPointsColumns.CONTENT_URI, createContentValues(location, trackId));
    }

    /**
     * Creates the {@link ContentValues} for a {@link Location}.
     *
     * @param location the location
     * @param trackId  the track id
     */
    private ContentValues createContentValues(Location location, long trackId) {
        ContentValues values = new ContentValues();
        values.put(TrackPointsColumns.TRACKID, trackId);
        values.put(TrackPointsColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
        values.put(TrackPointsColumns.LATITUDE, (int) (location.getLatitude() * 1E6));

        values.put(TrackPointsColumns.TIME, location.getTime());
        if (location.hasAltitude()) {
            values.put(TrackPointsColumns.ALTITUDE, location.getAltitude());
        }
        if (location.hasAccuracy()) {
            values.put(TrackPointsColumns.ACCURACY, location.getAccuracy());
        }
        if (location.hasSpeed()) {
            values.put(TrackPointsColumns.SPEED, location.getSpeed());
        }
        if (location.hasBearing()) {
            values.put(TrackPointsColumns.BEARING, location.getBearing());
        }

        //SensorData
        if (location instanceof TrackPoint) {
            TrackPoint sensorDataSetLocation = (TrackPoint) location;
            SensorDataSet sensorDataSet = sensorDataSetLocation.getSensorDataSet();
            if (sensorDataSet != null && sensorDataSet.hasHeartRate()) {
                values.put(TrackPointsColumns.SENSOR_HEARTRATE, sensorDataSetLocation.getSensorDataSet().getHeartRate());
            }
            if (sensorDataSet != null && sensorDataSet.hasCadence()) {
                values.put(TrackPointsColumns.SENSOR_CADENCE, sensorDataSetLocation.getSensorDataSet().getCadence());
            }
            if (sensorDataSet != null && sensorDataSet.hasPower()) {
                values.put(TrackPointsColumns.SENSOR_POWER, sensorDataSetLocation.getSensorDataSet().getPower());
            }
        }
        return values;
    }

    /**
     * Creates a new read-only iterator over a given track's points.
     * It provides a lightweight way of iterating over long tracks without failing due to the underlying cursor limitations.
     * Since it's a read-only iterator, {@link Iterator#remove()} always throws {@link UnsupportedOperationException}.
     * Each call to {@link LocationIterator#next()} may advance to the next DB record, and if so, the iterator calls {@link LocationFactory#createLocation()} and populates it with information retrieved from the record.
     * When done with iteration, {@link LocationIterator#close()} must be called.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting track point id. -1L to ignore
     * @param descending        true to sort the result in descending order (latest location first)
     * @param locationFactory   the location factory
     */
    public LocationIterator getTrackPointLocationIterator(final long trackId, final long startTrackPointId, final boolean descending, final LocationFactory locationFactory) {
        if (locationFactory == null) {
            throw new IllegalArgumentException("locationFactory is null");
        }
        return new LocationIterator(this, trackId, startTrackPointId, descending, locationFactory);
    }

    private Location findTrackPointBy(String selection, String[] selectionArgs) {
        try (Cursor cursor = getTrackPointCursor(null, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrackPoint(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a track point cursor.
     *
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection arguments
     * @param sortOrder     the sort order
     */
    private Cursor getTrackPointCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TrackPointsColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
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
     * A cache of track points indexes.
     */
    static class CachedTrackPointsIndexes {
        final int idIndex;
        final int longitudeIndex;
        final int latitudeIndex;
        final int timeIndex;
        final int altitudeIndex;
        final int accuracyIndex;
        final int speedIndex;
        final int bearingIndex;
        final int sensorHeartRateIndex;
        final int sensorCadenceIndex;
        final int sensorPowerIndex;

        CachedTrackPointsIndexes(Cursor cursor) {
            idIndex = cursor.getColumnIndex(TrackPointsColumns._ID);
            longitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE);
            latitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE);
            timeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.TIME);
            altitudeIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALTITUDE);
            accuracyIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.ACCURACY);
            speedIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SPEED);
            bearingIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.BEARING);
            sensorHeartRateIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_HEARTRATE);
            sensorCadenceIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_CADENCE);
            sensorPowerIndex = cursor.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_POWER);
        }
    }
}
