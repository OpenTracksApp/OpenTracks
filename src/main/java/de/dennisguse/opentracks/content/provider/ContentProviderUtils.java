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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.UUIDUtils;

/**
 * {@link ContentProviderUtils} implementation.
 *
 * @author Leif Hendrik Wilden
 */
public class ContentProviderUtils {

    private static final String TAG = ContentProviderUtils.class.getSimpleName();
    private static final int MAX_LATITUDE = 90000000;

    // The authority (the first part of the URI) for the app's content provider.
    @VisibleForTesting
    public static final String AUTHORITY_PACKAGE = BuildConfig.APPLICATION_ID + ".content";

    // The base URI for the app's content provider.
    public static final String CONTENT_BASE_URI = "content://" + AUTHORITY_PACKAGE;

    // Maximum number of markers that will be loaded at one time.
    public static final int MAX_LOADED_MARKERS = 10000;
    private static final String ID_SEPARATOR = ",";

    private final ContentResolver contentResolver;
    private int defaultCursorBatchSize = 2000;

    public ContentProviderUtils(Context context) {
        contentResolver = context.getContentResolver();
    }

    @VisibleForTesting
    public ContentProviderUtils(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    /**
     * Clears a track: removes markers and trackPoints.
     * Only keeps the track id.
     *
     * @param trackId the track id
     */
    @Deprecated //TODO Figure out why we need this.
    public void clearTrack(Track.Id trackId) {
        deleteTrackPointsAndMarkers(trackId);
        Track track = new Track();
        track.setId(trackId);
        updateTrack(track);
    }

    /**
     * Creates a {@link Track} from a cursor.
     *
     * @param cursor the cursor pointing to the track
     */
    public static Track createTrack(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
        int uuidIndex = cursor.getColumnIndexOrThrow(TracksColumns.UUID);
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
        int elevationLossIndex = cursor.getColumnIndexOrThrow(TracksColumns.ELEVATIONLOSS);
        int iconIndex = cursor.getColumnIndexOrThrow(TracksColumns.ICON);

        Track track = new Track();
        TrackStatistics trackStatistics = track.getTrackStatistics();
        if (!cursor.isNull(idIndex)) {
            track.setId(new Track.Id(cursor.getLong(idIndex)));
        }
        if (!cursor.isNull(uuidIndex)) {
            track.setUuid(UUIDUtils.fromBytes(cursor.getBlob(uuidIndex)));
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
        if (!cursor.isNull(elevationLossIndex)) {
            trackStatistics.setTotalElevationLoss(cursor.getFloat(elevationLossIndex));
        }
        if (!cursor.isNull(iconIndex)) {
            track.setIcon(cursor.getString(iconIndex));
        }
        return track;
    }

    @VisibleForTesting
    public void deleteAllTracks(Context context) {
        contentResolver.delete(TrackPointsColumns.CONTENT_URI_BY_ID, null, null);
        contentResolver.delete(MarkerColumns.CONTENT_URI, null, null);
        // Delete tracks last since it triggers a database vaccum call
        contentResolver.delete(TracksColumns.CONTENT_URI, null, null);

        File dir = FileUtils.getPhotoDir(context);
        FileUtils.deleteDirectoryRecurse(dir);
    }

    public void deleteTracks(Context context, @NonNull List<Track.Id> trackIds) {
        // Delete track folder resources.
        for (Track.Id trackId : trackIds) {
            FileUtils.deleteDirectoryRecurse(FileUtils.getPhotoDir(context, trackId));
        }

        // Delete track last since it triggers a database vacuum call
        String whereClause = String.format(TracksColumns._ID + " IN (%s)", TextUtils.join(",", Collections.nCopies(trackIds.size(), "?")));
        contentResolver.delete(TracksColumns.CONTENT_URI, whereClause, trackIds.stream().map(id->Long.toString(id.getId())).toArray(String[]::new));
    }

    public void deleteTrack(Context context, @NonNull Track.Id trackId) {
        // Delete track folder resources.
        FileUtils.deleteDirectoryRecurse(FileUtils.getPhotoDir(context, trackId));

        // Delete track last since it triggers a database vacuum call
        contentResolver.delete(TracksColumns.CONTENT_URI, TracksColumns._ID + "=?", new String[]{Long.toString(trackId.getId())});
    }

    private void deleteTrackPointsAndMarkers(@NonNull Track.Id trackId) {
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};

        contentResolver.delete(TrackPointsColumns.CONTENT_URI_BY_ID, TrackPointsColumns.TRACKID + "=?", selectionArgs);
        contentResolver.delete(MarkerColumns.CONTENT_URI, MarkerColumns.TRACKID + "=?", selectionArgs);
    }

    public List<Track> getTracks() {
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

    /**
     * @param trackId the track id.
     */
    public Track getTrack(Track.Id trackId) {
        if (trackId == null) {
            return null;
        }
        try (Cursor cursor = getTrackCursor(TracksColumns._ID + "=?", new String[]{Long.toString(trackId.getId())}, null)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    /**
     * @param trackUUID the track uuid.
     */
    public Track getTrack(@NonNull UUID trackUUID) {
        String trackUUIDsearch = UUIDUtils.toHex(trackUUID);
        try (Cursor cursor = getTrackCursor("hex(" + TracksColumns.UUID + ")=?", new String[]{trackUUIDsearch}, null)) {
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
        contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(track), TracksColumns._ID + "=?", new String[]{Long.toString(track.getId().getId())});
    }

    private ContentValues createContentValues(Track track) {
        ContentValues values = new ContentValues();
        TrackStatistics trackStatistics = track.getTrackStatistics();

        // Value < 0 indicates no id is available
        if (track.getId() != null) {
            values.put(TracksColumns._ID, track.getId().getId());
        }
        values.put(TracksColumns.UUID, UUIDUtils.toBytes(track.getUuid()));
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
        values.put(TracksColumns.ELEVATIONLOSS, trackStatistics.getTotalElevationLoss());
        values.put(TracksColumns.ICON, track.getIcon());

        return values;
    }

    public Marker createMarker(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(MarkerColumns._ID);
        int nameIndex = cursor.getColumnIndexOrThrow(MarkerColumns.NAME);
        int descriptionIndex = cursor.getColumnIndexOrThrow(MarkerColumns.DESCRIPTION);
        int categoryIndex = cursor.getColumnIndexOrThrow(MarkerColumns.CATEGORY);
        int iconIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ICON);
        int trackIdIndex = cursor.getColumnIndexOrThrow(MarkerColumns.TRACKID);
        int lengthIndex = cursor.getColumnIndexOrThrow(MarkerColumns.LENGTH);
        int durationIndex = cursor.getColumnIndexOrThrow(MarkerColumns.DURATION);
        int longitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.LONGITUDE);
        int latitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.LATITUDE);
        int timeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.TIME);
        int altitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ALTITUDE);
        int accuracyIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ACCURACY);
        int bearingIndex = cursor.getColumnIndexOrThrow(MarkerColumns.BEARING);
        int photoUrlIndex = cursor.getColumnIndexOrThrow(MarkerColumns.PHOTOURL);

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

        Track.Id trackId = new Track.Id(cursor.getLong(trackIdIndex));
        Marker marker = new Marker(trackId, location);

        if (!cursor.isNull(idIndex)) {
            marker.setId(new Marker.Id(cursor.getLong(idIndex)));
        }
        if (!cursor.isNull(nameIndex)) {
            marker.setName(cursor.getString(nameIndex));
        }
        if (!cursor.isNull(descriptionIndex)) {
            marker.setDescription(cursor.getString(descriptionIndex));
        }
        if (!cursor.isNull(categoryIndex)) {
            marker.setCategory(cursor.getString(categoryIndex));
        }
        if (!cursor.isNull(iconIndex)) {
            marker.setIcon(cursor.getString(iconIndex));
        }
        if (!cursor.isNull(lengthIndex)) {
            marker.setLength(cursor.getFloat(lengthIndex));
        }
        if (!cursor.isNull(durationIndex)) {
            marker.setDuration(cursor.getLong(durationIndex));
        }

        if (!cursor.isNull(photoUrlIndex)) {
            marker.setPhotoUrl(cursor.getString(photoUrlIndex));
        }
        return marker;
    }

    public void deleteMarker(Context context, Marker.Id markerId) {
        final Marker marker = getMarker(markerId);
        deleteMarkerPhoto(context, marker);
        contentResolver.delete(MarkerColumns.CONTENT_URI, MarkerColumns._ID + "=?", new String[]{Long.toString(markerId.getId())});
    }

    /**
     * @return -1 if not able to get the next marker number.
     */
    public int getNextMarkerNumber(@NonNull Track.Id trackId) {
        String[] projection = {MarkerColumns._ID};
        String selection = MarkerColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};
        try (Cursor cursor = getMarkerCursor(projection, selection, selectionArgs, MarkerColumns._ID, -1)) {
            if (cursor != null) {
                return cursor.getCount();
            }
        }
        return -1;
    }

    public Marker getMarker(@NonNull Marker.Id markerId) {
        try (Cursor cursor = getMarkerCursor(null, MarkerColumns._ID + "=?", new String[]{Long.toString(markerId.getId())}, MarkerColumns._ID, 1)) {
            if (cursor != null && cursor.moveToFirst()) {
                return createMarker(cursor);
            }
        }
        return null;
    }

    /**
     * The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId     the track id
     * @param minMarkerId the minimum marker id. null to ignore
     * @param maxCount    the maximum number of markers to return. -1 for no limit
     */
    public Cursor getMarkerCursor(@NonNull Track.Id trackId, @Nullable Marker.Id minMarkerId, int maxCount) {
        String selection;
        String[] selectionArgs;
        if (minMarkerId != null) {
            selection = MarkerColumns.TRACKID + "=? AND " + MarkerColumns._ID + ">=?";
            selectionArgs = new String[]{Long.toString(trackId.getId()), Long.toString(minMarkerId.getId())};
        } else {
            selection = MarkerColumns.TRACKID + "=?";
            selectionArgs = new String[]{Long.toString(trackId.getId())};
        }
        return getMarkerCursor(null, selection, selectionArgs, MarkerColumns._ID, maxCount);
    }

    @Deprecated //TODO Move to test package
    @VisibleForTesting
    public List<Marker> getMarkers(Track.Id trackId) {
        ArrayList<Marker> markers = new ArrayList<>();
        try (Cursor cursor = getMarkerCursor(trackId, null, -1)) {
            if (cursor.moveToFirst()) {
                do {
                    markers.add(createMarker(cursor));
                } while (cursor.moveToNext());
            }
        }
        return markers;
    }

    @Deprecated //TODO TracksColumns.MARKER_COUNT while querying for tracks
    public int getMarkerCount(Track.Id trackId) {
        String[] projection = new String[]{"count(*) AS count"};
        String selection = MarkerColumns.TRACKID + "=?";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};
        try (Cursor cursor = contentResolver.query(MarkerColumns.CONTENT_URI, projection, selection, selectionArgs, MarkerColumns._ID)) {
            if (cursor == null) {
                return 0;
            }
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    /**
     * @return the content provider URI of the inserted marker.
     */
    public Uri insertMarker(@NonNull Marker marker) {
        marker.setId(null);
        return contentResolver.insert(MarkerColumns.CONTENT_URI, createContentValues(marker));
    }

    private void deleteMarkerPhoto(Context context, Marker marker) {
        if (marker != null && marker.hasPhoto()) {
            Uri uri = marker.getPhotoURI();
            File file = FileUtils.buildInternalPhotoFile(context, marker.getTrackId(), uri);
            if (file.exists()) {
                File parent = file.getParentFile();
                file.delete();
                if (parent.listFiles().length == 0) {
                    parent.delete();
                }
            }
        }
    }

    /**
     * @param updateMarker the marker with updated data.
     * @return true if successful.
     */
    public boolean updateMarker(Context context, Marker updateMarker) {
        Marker savedMarker = getMarker(updateMarker.getId());
        if (!updateMarker.hasPhoto()) {
            deleteMarkerPhoto(context, savedMarker);
        }
        int rows = contentResolver.update(MarkerColumns.CONTENT_URI, createContentValues(updateMarker), MarkerColumns._ID + "=?", new String[]{Long.toString(updateMarker.getId().getId())});
        return rows == 1;
    }

    ContentValues createContentValues(@NonNull Marker marker) {
        ContentValues values = new ContentValues();

        if (marker.getId() != null) {
            values.put(MarkerColumns._ID, marker.getId().getId());
        }
        values.put(MarkerColumns.NAME, marker.getName());
        values.put(MarkerColumns.DESCRIPTION, marker.getDescription());
        values.put(MarkerColumns.CATEGORY, marker.getCategory());
        values.put(MarkerColumns.ICON, marker.getIcon());
        values.put(MarkerColumns.TRACKID, marker.getTrackId().getId());
        values.put(MarkerColumns.LENGTH, marker.getLength());
        values.put(MarkerColumns.DURATION, marker.getDuration());

        Location location = marker.getLocation();
        values.put(MarkerColumns.LONGITUDE, (int) (location.getLongitude() * 1E6));
        values.put(MarkerColumns.LATITUDE, (int) (location.getLatitude() * 1E6));
        values.put(MarkerColumns.TIME, location.getTime());
        if (location.hasAltitude()) {
            values.put(MarkerColumns.ALTITUDE, location.getAltitude());
        }
        if (location.hasAccuracy()) {
            values.put(MarkerColumns.ACCURACY, location.getAccuracy());
        }
        if (location.hasBearing()) {
            values.put(MarkerColumns.BEARING, location.getBearing());
        }

        values.put(MarkerColumns.PHOTOURL, marker.getPhotoUrl());
        return values;
    }

    /**
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection args
     * @param sortOrder     the sort order
     * @param maxCount      the maximum number of markers
     */
    private Cursor getMarkerCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder, int maxCount) {
        if (sortOrder == null) {
            sortOrder = MarkerColumns._ID;
        }
        if (maxCount >= 0) {
            sortOrder += " LIMIT " + maxCount;
        }
        return contentResolver.query(MarkerColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
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
            trackPoint.setId(new TrackPoint.Id(cursor.getInt(indexes.idIndex)));
        }

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

        if (!cursor.isNull(indexes.elevationGainIndex)) {
            trackPoint.setElevationGain(cursor.getFloat(indexes.elevationGainIndex));
        }
        if (!cursor.isNull(indexes.elevationLossIndex)) {
            trackPoint.setElevationLoss(cursor.getFloat(indexes.elevationLossIndex));
        }

        return trackPoint;
    }

    //TODO Only used for file import; might be better to replace it.
    public int bulkInsertTrackPoint(List<TrackPoint> trackPoints, Track.Id trackId) {
        ContentValues[] values = new ContentValues[trackPoints.size()];
        for (int i = 0; i < trackPoints.size(); i++) {
            values[i] = createContentValues(trackPoints.get(i), trackId);
        }
        return contentResolver.bulkInsert(TrackPointsColumns.CONTENT_URI_BY_ID, values);
    }

    /**
     * Gets the first location id for a track.
     * Returns null if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public Track.Id getFirstTrackPointId(Track.Id trackId) {
        String selection = TrackPointsColumns._ID + "=(SELECT MIN(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new Track.Id(cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID)));
            }
        }
        return null;
    }

    /**
     * Gets the last location id for a track.
     * Returns -1L if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public TrackPoint.Id getLastTrackPointId(@NonNull Track.Id trackId) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new TrackPoint.Id(cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID)));
            }
        }
        return null;
    }

    /**
     * Gets the trackPoint id for a location.
     *
     * @param trackId  the track id
     * @param location the location
     * @return trackPoint id if the location is in the track. -1L otherwise.
     */
    public TrackPoint.Id getTrackPointId(Track.Id trackId, Location location) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns.TIME + "=?)";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId()), Long.toString(location.getTime())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new TrackPoint.Id(cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID)));
            }
        }
        return null;
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
     * @param startTrackPointId the starting trackPoint id. `null` to ignore
     * @param maxLocations      maximum number of locations to return. `null` for no limit
     */
    public Cursor getTrackPointCursor(Track.Id trackId, TrackPoint.Id startTrackPointId, Integer maxLocations) {
        String selection;
        String[] selectionArgs;
        if (startTrackPointId != null) {
            selection = TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns._ID + ">=?";
            selectionArgs = new String[]{Long.toString(trackId.getId()), Long.toString(startTrackPointId.getId())};
        } else {
            selection = TrackPointsColumns.TRACKID + "=?";
            selectionArgs = new String[]{Long.toString(trackId.getId())};
        }

        String sortOrder = TrackPointsColumns.DEFAULT_SORT_ORDER;
        if (maxLocations != null) {
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
    public TrackPoint getLastValidTrackPoint(Track.Id trackId) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + TrackPointsColumns.TRACKID + "=? AND " + TrackPointsColumns.LATITUDE + "<=" + MAX_LATITUDE + ")";
        String[] selectionArgs = new String[]{Long.toString(trackId.getId())};
        return findTrackPointBy(selection, selectionArgs);
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     * @param trackId    the track id
     * @return the content provider URI of the inserted trackPoint
     */
    public Uri insertTrackPoint(TrackPoint trackPoint, Track.Id trackId) {
        return contentResolver.insert(TrackPointsColumns.CONTENT_URI_BY_ID, createContentValues(trackPoint, trackId));
    }

    /**
     * Creates the {@link ContentValues} for a {@link TrackPoint}.
     *
     * @param trackPoint the trackPointstats_pace_km#87
     * @param trackId    the track id
     */
    private ContentValues createContentValues(TrackPoint trackPoint, Track.Id trackId) {
        ContentValues values = new ContentValues();
        values.put(TrackPointsColumns.TRACKID, trackId.getId());
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

        if (trackPoint.hasElevationGain()) {
            values.put(TrackPointsColumns.ELEVATION_GAIN, trackPoint.getElevationGain());
        }
        if (trackPoint.hasElevationLoss()) {
            values.put(TrackPointsColumns.ELEVATION_LOSS, trackPoint.getElevationLoss());
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
     * @param startTrackPointId the starting trackPoint id. `null` to ignore
     */
    public TrackPointIterator getTrackPointLocationIterator(final Track.Id trackId, final TrackPoint.Id startTrackPointId) {
        return new TrackPointIterator(this, trackId, startTrackPointId);
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
    public List<TrackPoint> getTrackPoints(Track.Id trackId) {
        List<TrackPoint> trackPoints = null;

        try (Cursor trackPointCursor = getTrackPointCursor(trackId, null, null)) {
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


    public static String formatIdListForUri(Track.Id... trackIds) {
        long[] ids = new long[trackIds.length];
        for (int i = 0; i < trackIds.length; i++) {
            ids[i] = trackIds[i].getId();
        }

        return formatIdListForUri(ids);
    }

    /**
     * Formats an array of IDs as comma separated string value
     *
     * @param ids array with IDs
     * @return comma separated list of ids
     */
    private static String formatIdListForUri(long[] ids) {
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
