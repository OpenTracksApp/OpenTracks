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

package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

/**
 * A provider that handles recorded (GPS) tracks and their track points.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksProvider extends ContentProvider {

  private static final String DATABASE_NAME = "mytracks.db";
  private static final int DATABASE_VERSION = 20;
  private static final int TRACKPOINTS = 1;
  private static final int TRACKPOINTS_ID = 2;
  private static final int TRACKS = 3;
  private static final int TRACKS_ID = 4;
  private static final int WAYPOINTS = 5;
  private static final int WAYPOINTS_ID = 6;
  private static final String TAG = MyTracksProvider.class.getSimpleName();

  /**
   * Helper which creates or upgrades the database if necessary.
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(TrackPointsColumns.CREATE_TABLE);
      db.execSQL(TracksColumns.CREATE_TABLE);
      db.execSQL(WaypointsColumns.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
      if (oldVersion < 17) {
        // Delete the old data
        Log.w(TAG, "Delete all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TrackPointsColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TracksColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WaypointsColumns.TABLE_NAME);
        onCreate(db);
      } else {
        // Incremental updates go here. For each DB version, add a corresponding if clause.

        // Track points sensor column
        if (oldVersion <= 17) {
          Log.w(TAG, "Upgrade DB: Adding sensor column.");
          db.execSQL("ALTER TABLE " + TrackPointsColumns.TABLE_NAME + " ADD "
              + TrackPointsColumns.SENSOR + " BLOB");
        }
        // Tracks table id column
        if (oldVersion <= 18) {
          Log.w(TAG, "Upgrade DB: Adding tableid column.");
          db.execSQL("ALTER TABLE " + TracksColumns.TABLE_NAME + " ADD " + TracksColumns.TABLEID
              + " STRING");
        }
        // Tracks table icon column
        if (oldVersion <= 19) {
          Log.w(TAG, "Upgrade DB: Adding icon column.");
          db.execSQL(
              "ALTER TABLE " + TracksColumns.TABLE_NAME + " ADD " + TracksColumns.ICON + " STRING");
        }
      }
    }
  }

  private final UriMatcher urlMatcher;

  private SQLiteDatabase db;

  public MyTracksProvider() {
    urlMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY,
        "trackpoints", TRACKPOINTS);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY,
        "trackpoints/#", TRACKPOINTS_ID);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY, "tracks", TRACKS);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY, "tracks/#", TRACKS_ID);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY, "waypoints", WAYPOINTS);
    urlMatcher.addURI(MyTracksProviderUtils.AUTHORITY,
        "waypoints/#", WAYPOINTS_ID);
  }

  private boolean canAccess() {
    if (Binder.getCallingPid() == Process.myPid()) {
      return true;
    } else {
      return PreferencesUtils.getBoolean(
          getContext(), R.string.allow_access_key, PreferencesUtils.ALLOW_ACCESS_DEFAULT);
    }
  }

  @Override
  public boolean onCreate() {
    if (!canAccess()) {
      return false;
    }
    DatabaseHelper dbHelper = new DatabaseHelper(getContext());
    try {
      db = dbHelper.getWritableDatabase();
    } catch (SQLiteException e) {
      Log.e(TAG, "Unable to open database for writing", e);
    }
    return db != null;
  }

  @Override
  public int delete(Uri url, String where, String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
    }
    String table;
    boolean shouldVacuum = false;
    switch (urlMatcher.match(url)) {
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

    Log.w(MyTracksProvider.TAG, "provider delete in " + table + "!");
    int count = db.delete(table, where, selectionArgs);
    getContext().getContentResolver().notifyChange(url, null, true);

    if (shouldVacuum) {
      // If a potentially large amount of data was deleted, we want to reclaim its space.
      Log.i(TAG, "Vacuuming the database");
      db.execSQL("VACUUM");
    }

    return count;
  }

  @Override
  public String getType(Uri url) {
    if (!canAccess()) {
      return null;
    }
    switch (urlMatcher.match(url)) {
      case TRACKPOINTS:
        return TrackPointsColumns.CONTENT_TYPE;
      case TRACKPOINTS_ID:
        return TrackPointsColumns.CONTENT_ITEMTYPE;
      case TRACKS:
        return TracksColumns.CONTENT_TYPE;
      case TRACKS_ID:
        return TracksColumns.CONTENT_ITEMTYPE;
      case WAYPOINTS:
        return WaypointsColumns.CONTENT_TYPE;
      case WAYPOINTS_ID:
        return WaypointsColumns.CONTENT_ITEMTYPE;
      default:
        throw new IllegalArgumentException("Unknown URL " + url);
    }
  }

  @Override
  public Uri insert(Uri url, ContentValues initialValues) {
    if (!canAccess()) {
      return null;
    }
    Log.d(MyTracksProvider.TAG, "MyTracksProvider.insert");
    ContentValues values;
    if (initialValues != null) {
      values = initialValues;
    } else {
      values = new ContentValues();
    }

    int urlMatchType = urlMatcher.match(url);
    return insertType(url, urlMatchType, values);
  }

  private Uri insertType(Uri url, int urlMatchType, ContentValues values) {
    switch (urlMatchType) {
      case TRACKPOINTS:
        return insertTrackPoint(url, values);
      case TRACKS:
        return insertTrack(url, values);
      case WAYPOINTS:
        return insertWaypoint(url, values);
      default:
        throw new IllegalArgumentException("Unknown URL " + url);
    }
  }


  @Override
  public int bulkInsert(Uri url, ContentValues[] valuesBulk) {
    if (!canAccess()) {
      return 0;
    }
    Log.d(MyTracksProvider.TAG, "MyTracksProvider.bulkInsert");
    int numInserted = 0;
    try {
      // Use a transaction in order to make the insertions run as a single batch
      db.beginTransaction();

      int urlMatch = urlMatcher.match(url);
      for (numInserted = 0; numInserted < valuesBulk.length; numInserted++) {
        ContentValues values = valuesBulk[numInserted];
        if (values == null) { values = new ContentValues(); }

        insertType(url, urlMatch, values);
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    return numInserted;
  }

  private Uri insertTrackPoint(Uri url, ContentValues values) {
    boolean hasLat = values.containsKey(TrackPointsColumns.LATITUDE);
    boolean hasLong = values.containsKey(TrackPointsColumns.LONGITUDE);
    boolean hasTime = values.containsKey(TrackPointsColumns.TIME);
    if (!hasLat || !hasLong || !hasTime) {
      throw new IllegalArgumentException(
          "Latitude, longitude, and time values are required.");
    }
    long rowId = db.insert(TrackPointsColumns.TABLE_NAME, TrackPointsColumns._ID, values);
    if (rowId >= 0) {
      Uri uri = ContentUris.appendId(
          TrackPointsColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLiteException("Failed to insert row into " + url);
  }

  private Uri insertTrack(Uri url, ContentValues values) {
    boolean hasStartTime = values.containsKey(TracksColumns.STARTTIME);
    boolean hasStartId = values.containsKey(TracksColumns.STARTID);
    if (!hasStartTime || !hasStartId) {
      throw new IllegalArgumentException(
          "Both start time and start id values are required.");
    }
    long rowId = db.insert(TracksColumns.TABLE_NAME, TracksColumns._ID, values);
    if (rowId > 0) {
      Uri uri = ContentUris.appendId(
          TracksColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLException("Failed to insert row into " + url);
  }

  private Uri insertWaypoint(Uri url, ContentValues values) {
    long rowId = db.insert(WaypointsColumns.TABLE_NAME, WaypointsColumns._ID, values);
    if (rowId > 0) {
      Uri uri = ContentUris.appendId(
          WaypointsColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLException("Failed to insert row into " + url);
  }

  @Override
  public Cursor query(
      Uri url, String[] projection, String selection, String[] selectionArgs,
      String sort) {
    if (!canAccess()) {
      return null;
    }
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    int match = urlMatcher.match(url);
    String sortOrder = null;
    if (match == TRACKPOINTS) {
      qb.setTables(TrackPointsColumns.TABLE_NAME);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = TrackPointsColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == TRACKPOINTS_ID) {
      qb.setTables(TrackPointsColumns.TABLE_NAME);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else if (match == TRACKS) {
      qb.setTables(TracksColumns.TABLE_NAME);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = TracksColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == TRACKS_ID) {
      qb.setTables(TracksColumns.TABLE_NAME);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else if (match == WAYPOINTS) {
      qb.setTables(WaypointsColumns.TABLE_NAME);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = WaypointsColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == WAYPOINTS_ID) {
      qb.setTables(WaypointsColumns.TABLE_NAME);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else {
      throw new IllegalArgumentException("Unknown URL " + url);
    }
    Log.i(Constants.TAG, "Build query: "
        + qb.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, null));
    
    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
        sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), url);
    return c;
  }

  @Override
  public int update(Uri url, ContentValues values, String where,
      String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
    }
    int count;
    int match = urlMatcher.match(url);
    if (match == TRACKPOINTS) {
      count = db.update(TrackPointsColumns.TABLE_NAME, values, where, selectionArgs);
    } else if (match == TRACKPOINTS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(TrackPointsColumns.TABLE_NAME, values, "_id=" + segment
          + (!TextUtils.isEmpty(where)
              ? " AND (" + where + ')'
              : ""),
          selectionArgs);
    } else if (match == TRACKS) {
      count = db.update(TracksColumns.TABLE_NAME, values, where, selectionArgs);
    } else if (match == TRACKS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(TracksColumns.TABLE_NAME, values, "_id=" + segment
          + (!TextUtils.isEmpty(where)
              ? " AND (" + where + ')'
              : ""),
          selectionArgs);
    } else if (match == WAYPOINTS) {
      count = db.update(WaypointsColumns.TABLE_NAME, values, where, selectionArgs);
    } else if (match == WAYPOINTS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(WaypointsColumns.TABLE_NAME, values, "_id=" + segment
          + (!TextUtils.isEmpty(where)
              ? " AND (" + where + ')'
              : ""),
          selectionArgs);
    } else {
      throw new IllegalArgumentException("Unknown URL " + url);
    }
    getContext().getContentResolver().notifyChange(url, null, true);
    return count;
  }

}
