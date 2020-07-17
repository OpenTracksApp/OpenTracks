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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.WaypointsColumns;

/**
 * A {@link ContentProvider} that handles access to track points, tracks, and waypoints tables.
 *
 * @author Leif Hendrik Wilden
 */
public abstract class CustomContentProvider extends ContentProvider {

    private static final String TAG = CustomContentProvider.class.getSimpleName();

    private static final String SQL_LIST_DELIMITER = ",";

    private final UriMatcher uriMatcher;

    private SQLiteDatabase db;

    public CustomContentProvider() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_ID.getPath(), UrlType.TRACKPOINTS.ordinal());
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_ID.getPath() + "/#", UrlType.TRACKPOINTS_BY_ID.ordinal());
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_TRACKID.getPath() + "/*", UrlType.TRACKPOINTS_BY_TRACKID.ordinal());

        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TracksColumns.CONTENT_URI.getPath(), UrlType.TRACKS.ordinal());
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TracksColumns.CONTENT_URI.getPath() + "/*", UrlType.TRACKS_BY_ID.ordinal());

        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, WaypointsColumns.CONTENT_URI.getPath(), UrlType.WAYPOINTS.ordinal());
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, WaypointsColumns.CONTENT_URI.getPath() + "/#", UrlType.WAYPOINTS_BY_ID.ordinal());
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, WaypointsColumns.CONTENT_URI_BY_TRACKID.getPath() + "/*", UrlType.WAYPOINTS_BY_TRACKID.ordinal());
    }

    @Override
    public boolean onCreate() {
        return onCreate(getContext());
    }

    /**
     * Helper method to make onCreate is testable.
     *
     * @param context context to creates database
     * @return true means run successfully
     */
    @VisibleForTesting
    boolean onCreate(Context context) {
        CustomSQLiteOpenHelper databaseHelper = new CustomSQLiteOpenHelper(context);
        try {
            db = databaseHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(TAG, "Unable to open database for writing.", e);
        }
        return db != null;
    }

    @Override
    public int delete(@NonNull Uri url, String where, String[] selectionArgs) {
        String table;
        boolean shouldVacuum = false;
        switch (getUrlType(url)) {
            case TRACKPOINTS:
                table = TrackPointsColumns.TABLE_NAME;
                break;
            case TRACKS:
                table = TracksColumns.TABLE_NAME;
                shouldVacuum = true;
                break;
            case WAYPOINTS:
                table = WaypointsColumns.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        Log.w(TAG, "Deleting table " + table);
        int count;
        try {
            db.beginTransaction();
            count = db.delete(table, where, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(url, null, false);

        if (shouldVacuum) {
            // If a potentially large amount of data was deleted, reclaim its space.
            Log.i(TAG, "Vacuuming the database.");
            db.execSQL("VACUUM");
        }
        return count;
    }

    @Override
    public String getType(@NonNull Uri url) {
        switch (getUrlType(url)) {
            case TRACKPOINTS:
                return TrackPointsColumns.CONTENT_TYPE;
            case TRACKPOINTS_BY_ID:
            case TRACKPOINTS_BY_TRACKID:
                return TrackPointsColumns.CONTENT_ITEMTYPE;
            case TRACKS:
                return TracksColumns.CONTENT_TYPE;
            case TRACKS_BY_ID:
                return TracksColumns.CONTENT_ITEMTYPE;
            case WAYPOINTS:
                return WaypointsColumns.CONTENT_TYPE;
            case WAYPOINTS_BY_ID:
            case WAYPOINTS_BY_TRACKID:
                return WaypointsColumns.CONTENT_ITEMTYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(@NonNull Uri url, ContentValues initialValues) {
        if (initialValues == null) {
            initialValues = new ContentValues();
        }
        Uri result;
        try {
            db.beginTransaction();
            result = insertContentValues(url, getUrlType(url), initialValues);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(url, null, false);
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri url, @NonNull ContentValues[] valuesBulk) {
        int numInserted;
        try {
            // Use a transaction in order to make the insertions run as a single batch
            db.beginTransaction();

            UrlType urlType = getUrlType(url);
            for (numInserted = 0; numInserted < valuesBulk.length; numInserted++) {
                ContentValues contentValues = valuesBulk[numInserted];
                if (contentValues == null) {
                    contentValues = new ContentValues();
                }
                insertContentValues(url, urlType, contentValues);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(url, null, false);
        return numInserted;
    }

    @Override
    public Cursor query(@NonNull Uri url, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String sortOrder = null;
        switch (getUrlType(url)) {
            case TRACKPOINTS:
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                sortOrder = sort != null ? sort : TrackPointsColumns.DEFAULT_SORT_ORDER;
                break;
            case TRACKPOINTS_BY_ID:
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(TrackPointsColumns._ID + "=" + ContentUris.parseId(url));
                break;
            case TRACKPOINTS_BY_TRACKID:
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(TrackPointsColumns.TRACKID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
                break;
            case TRACKS:
                queryBuilder.setTables(TracksColumns.TABLE_NAME);
                sortOrder = sort != null ? sort : TracksColumns.DEFAULT_SORT_ORDER;
                break;
            case TRACKS_BY_ID:
                queryBuilder.setTables(TracksColumns.TABLE_NAME);
                queryBuilder.appendWhere(TracksColumns._ID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
                break;
            case WAYPOINTS:
                queryBuilder.setTables(WaypointsColumns.TABLE_NAME);
                sortOrder = sort != null ? sort : WaypointsColumns.DEFAULT_SORT_ORDER;
                break;
            case WAYPOINTS_BY_ID:
                queryBuilder.setTables(WaypointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(WaypointsColumns._ID + "=" + ContentUris.parseId(url));
                break;
            case WAYPOINTS_BY_TRACKID:
                queryBuilder.setTables(WaypointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(WaypointsColumns.TRACKID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
                break;
            default:
                throw new IllegalArgumentException("Unknown url " + url);
        }
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), url);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri url, ContentValues values, String where, String[] selectionArgs) {
        // TODO Use SQLiteQueryBuilder
        String table;
        String whereClause;
        switch (getUrlType(url)) {
            case TRACKPOINTS:
                table = TrackPointsColumns.TABLE_NAME;
                whereClause = where;
                break;
            case TRACKPOINTS_BY_ID:
                table = TrackPointsColumns.TABLE_NAME;
                whereClause = TrackPointsColumns._ID + "=" + ContentUris.parseId(url);
                if (!TextUtils.isEmpty(where)) {
                    whereClause += " AND (" + where + ")";
                }
                break;
            case TRACKS:
                table = TracksColumns.TABLE_NAME;
                whereClause = where;
                break;
            case TRACKS_BY_ID:
                table = TracksColumns.TABLE_NAME;
                whereClause = TracksColumns._ID + "=" + ContentUris.parseId(url);
                if (!TextUtils.isEmpty(where)) {
                    whereClause += " AND (" + where + ")";
                }
                break;
            case WAYPOINTS:
                table = WaypointsColumns.TABLE_NAME;
                whereClause = where;
                break;
            case WAYPOINTS_BY_ID:
                table = WaypointsColumns.TABLE_NAME;
                whereClause = WaypointsColumns._ID + "=" + ContentUris.parseId(url);
                if (!TextUtils.isEmpty(where)) {
                    whereClause += " AND (" + where + ")";
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown url " + url);
        }
        int count;
        try {
            db.beginTransaction();
            count = db.update(table, values, whereClause, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(url, null, false);
        return count;
    }

    /**
     * Gets the {@link UrlType} for a url.
     *
     * @param url the url
     */
    @NonNull
    private UrlType getUrlType(Uri url) {
        UrlType[] urlTypes = UrlType.values();
        int matchIndex = uriMatcher.match(url);
        if (0 <= matchIndex && matchIndex < urlTypes.length) {
            return urlTypes[matchIndex];
        }

        throw new IllegalArgumentException("Unknown URL " + url);
    }

    /**
     * Inserts a content based on the url type.
     *
     * @param url           the content url
     * @param urlType       the url type
     * @param contentValues the content values
     */
    private Uri insertContentValues(Uri url, UrlType urlType, ContentValues contentValues) {
        switch (urlType) {
            case TRACKPOINTS:
                return insertTrackPoint(url, contentValues);
            case TRACKS:
                return insertTrack(url, contentValues);
            case WAYPOINTS:
                return insertWaypoint(url, contentValues);
            default:
                throw new IllegalArgumentException("Unknown url " + url);
        }
    }

    /**
     * Inserts a track point.
     *
     * @param url    the content url
     * @param values the content values
     */
    private Uri insertTrackPoint(Uri url, ContentValues values) {
        boolean hasLatitude = values.containsKey(TrackPointsColumns.LATITUDE);
        boolean hasLongitude = values.containsKey(TrackPointsColumns.LONGITUDE);
        boolean hasTime = values.containsKey(TrackPointsColumns.TIME);
        if (!hasLatitude || !hasLongitude || !hasTime) {
            throw new IllegalArgumentException("Latitude, longitude, and time values are required.");
        }
        long rowId = db.insert(TrackPointsColumns.TABLE_NAME, TrackPointsColumns._ID, values);
        if (rowId >= 0) {
            return ContentUris.appendId(TrackPointsColumns.CONTENT_URI_BY_ID.buildUpon(), rowId).build();
        }
        throw new SQLiteException("Failed to insert a track point " + url);
    }

    /**
     * Inserts a track.
     *
     * @param url           the content url
     * @param contentValues the content values
     */
    private Uri insertTrack(Uri url, ContentValues contentValues) {
        boolean hasStartTime = contentValues.containsKey(TracksColumns.STARTTIME);
        if (!hasStartTime) {
            throw new IllegalArgumentException("Both start time and start id values are required.");
        }
        long rowId = db.insert(TracksColumns.TABLE_NAME, TracksColumns._ID, contentValues);
        if (rowId >= 0) {
            return ContentUris.appendId(TracksColumns.CONTENT_URI.buildUpon(), rowId).build();
        }
        throw new SQLException("Failed to insert a track " + url);
    }

    /**
     * Inserts a waypoint.
     *
     * @param url           the content url
     * @param contentValues the content values
     */
    private Uri insertWaypoint(Uri url, ContentValues contentValues) {
        long rowId = db.insert(WaypointsColumns.TABLE_NAME, WaypointsColumns._ID, contentValues);
        if (rowId >= 0) {
            return ContentUris.appendId(WaypointsColumns.CONTENT_URI.buildUpon(), rowId).build();
        }
        throw new SQLException("Failed to insert a waypoint " + url);
    }

    /**
     * Types of url.
     *
     * @author Jimmy Shih
     */
    @VisibleForTesting
    enum UrlType {
        TRACKPOINTS,
        TRACKPOINTS_BY_ID,
        TRACKPOINTS_BY_TRACKID,
        TRACKS,
        TRACKS_BY_ID,
        WAYPOINTS,
        WAYPOINTS_BY_ID,
        WAYPOINTS_BY_TRACKID
    }
}
