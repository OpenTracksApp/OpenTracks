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
import android.text.TextUtils;
import android.util.Log;

/**
 * A provider that handles recorded (GPS) tracks and their track points.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksProvider extends ContentProvider {

  private static final String DATABASE_NAME = "mytracks.db";
  private static final int DATABASE_VERSION = 17;
  private static final int TRACKPOINTS = 1;
  private static final int TRACKPOINTS_ID = 2;
  private static final int TRACKS = 3;
  private static final int TRACKS_ID = 4;
  private static final int WAYPOINTS = 5;
  private static final int WAYPOINTS_ID = 6;
  private static final String TRACKPOINTS_TABLE = "trackpoints";
  private static final String TRACKS_TABLE = "tracks";
  private static final String WAYPOINTS_TABLE = "waypoints";
  public static final String TAG = "MyTracksProvider";

  /**
   * Helper which creates or upgrades the database if necessary.
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TRACKPOINTS_TABLE + " ("
          + TrackPointsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + TrackPointsColumns.TRACKID + " INTEGER, "
          + TrackPointsColumns.LONGITUDE + " INTEGER, "
          + TrackPointsColumns.LATITUDE + " INTEGER, "
          + TrackPointsColumns.TIME + " INTEGER, "
          + TrackPointsColumns.ALTITUDE + " FLOAT, "
          + TrackPointsColumns.ACCURACY + " FLOAT, "
          + TrackPointsColumns.SPEED + " FLOAT, "
          + TrackPointsColumns.BEARING + " FLOAT);");
      db.execSQL("CREATE TABLE " + TRACKS_TABLE + " ("
          + TracksColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + TracksColumns.NAME + " STRING, "
          + TracksColumns.DESCRIPTION + " STRING, "
          + TracksColumns.CATEGORY + " STRING, "
          + TracksColumns.STARTID + " INTEGER, "
          + TracksColumns.STOPID + " INTEGER, "
          + TracksColumns.STARTTIME + " INTEGER, "
          + TracksColumns.STOPTIME + " INTEGER, "
          + TracksColumns.NUMPOINTS + " INTEGER, "
          + TracksColumns.TOTALDISTANCE + " FLOAT, "
          + TracksColumns.TOTALTIME + " INTEGER, "
          + TracksColumns.MOVINGTIME + " INTEGER, "
          + TracksColumns.MINLAT + " INTEGER, "
          + TracksColumns.MAXLAT + " INTEGER, "
          + TracksColumns.MINLON + " INTEGER, "
          + TracksColumns.MAXLON + " INTEGER, "
          + TracksColumns.AVGSPEED + " FLOAT, "
          + TracksColumns.AVGMOVINGSPEED + " FLOAT, "
          + TracksColumns.MAXSPEED + " FLOAT, "
          + TracksColumns.MINELEVATION + " FLOAT, "
          + TracksColumns.MAXELEVATION + " FLOAT, "
          + TracksColumns.ELEVATIONGAIN + " FLOAT, "
          + TracksColumns.MINGRADE + " FLOAT, "
          + TracksColumns.MAXGRADE + " FLOAT, "
          + TracksColumns.MAPID + " STRING);");
      db.execSQL("CREATE TABLE " + WAYPOINTS_TABLE + " ("
          + WaypointsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + WaypointsColumns.NAME + " STRING, "
          + WaypointsColumns.DESCRIPTION + " STRING, "
          + WaypointsColumns.CATEGORY + " STRING, "
          + WaypointsColumns.ICON + " STRING, "
          + WaypointsColumns.TRACKID + " INTEGER, "
          + WaypointsColumns.TYPE + " INTEGER, "
          + WaypointsColumns.LENGTH + " FLOAT, "
          + WaypointsColumns.DURATION + " INTEGER, "
          + WaypointsColumns.STARTTIME + " INTEGER, "
          + WaypointsColumns.STARTID + " INTEGER, "
          + WaypointsColumns.STOPID + " INTEGER, "
          + WaypointsColumns.LONGITUDE + " INTEGER, "
          + WaypointsColumns.LATITUDE + " INTEGER, "
          + WaypointsColumns.TIME + " INTEGER, "
          + WaypointsColumns.ALTITUDE + " FLOAT, "
          + WaypointsColumns.ACCURACY + " FLOAT, "
          + WaypointsColumns.SPEED + " FLOAT, "
          + WaypointsColumns.BEARING + " FLOAT, "
          + WaypointsColumns.TOTALDISTANCE + " FLOAT, "
          + WaypointsColumns.TOTALTIME + " INTEGER, "
          + WaypointsColumns.MOVINGTIME + " INTEGER, "
          + WaypointsColumns.AVGSPEED + " FLOAT, "
          + WaypointsColumns.AVGMOVINGSPEED + " FLOAT, "
          + WaypointsColumns.MAXSPEED + " FLOAT, "
          + WaypointsColumns.MINELEVATION + " FLOAT, "
          + WaypointsColumns.MAXELEVATION + " FLOAT, "
          + WaypointsColumns.ELEVATIONGAIN + " FLOAT, "
          + WaypointsColumns.MINGRADE + " FLOAT, "
          + WaypointsColumns.MAXGRADE + " FLOAT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
          + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + TRACKPOINTS_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + TRACKS_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + WAYPOINTS_TABLE);
      onCreate(db);
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

  @Override
  public boolean onCreate() {
    DatabaseHelper dbHelper = new DatabaseHelper(getContext());
    db = dbHelper.getWritableDatabase();
    return db != null;
  }

  @Override
  public int delete(Uri url, String where, String[] selectionArgs) {
    if (urlMatcher.match(url) == TRACKPOINTS) {
      Log.w(MyTracksProvider.TAG, "provider trackpoints delete!");
      int count = db.delete(TRACKPOINTS_TABLE, where, selectionArgs);
      getContext().getContentResolver().notifyChange(url, null, true);
      return count;
    } else if (urlMatcher.match(url) == TRACKS) {
      Log.w(MyTracksProvider.TAG, "provider track delete!");
      int count = db.delete(TRACKS_TABLE, where, selectionArgs);
      getContext().getContentResolver().notifyChange(url, null, true);
      return count;
    } else if (urlMatcher.match(url) == WAYPOINTS) {
      Log.w(MyTracksProvider.TAG, "provider waypoint delete!");
      int count = db.delete(WAYPOINTS_TABLE, where, selectionArgs);
      getContext().getContentResolver().notifyChange(url, null, true);
      return count;
    } else {
      throw new IllegalArgumentException("Unknown URL " + url);
    }
  }

  @Override
  public String getType(Uri url) {
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
    Log.d(MyTracksProvider.TAG, "MyTracksProvider.insert");
    ContentValues values;
    if (initialValues != null) {
      values = initialValues;
    } else {
      values = new ContentValues();
    }
    if (urlMatcher.match(url) == TRACKPOINTS) {
      return insertTrackPoint(url, values);
    } else if (urlMatcher.match(url) == TRACKS) {
      return insertTrack(url, values);
    } else if (urlMatcher.match(url) == WAYPOINTS) {
      return insertWaypoint(url, values);
    } else {
      throw new IllegalArgumentException("Unknown URL " + url);
    }
  }

  private Uri insertTrackPoint(Uri url, ContentValues values) {
    Log.d(MyTracksProvider.TAG, "MyTracksProvider.insertTrackPoint");
    boolean hasLat = values.containsKey(TrackPointsColumns.LATITUDE);
    boolean hasLong = values.containsKey(TrackPointsColumns.LONGITUDE);
    boolean hasTime = values.containsKey(TrackPointsColumns.TIME);
    if (!hasLat || !hasLong || !hasTime) {
      throw new IllegalArgumentException(
          "Latitude, longitude, and time values are required.");
    }
    long rowId = db.insert(TRACKPOINTS_TABLE, TrackPointsColumns._ID, values);
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
    long rowId = db.insert(TRACKS_TABLE, TracksColumns._ID, values);
    if (rowId > 0) {
      Uri uri = ContentUris.appendId(
          TracksColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLException("Failed to insert row into " + url);
  }

  private Uri insertWaypoint(Uri url, ContentValues values) {
    long rowId = db.insert(WAYPOINTS_TABLE, WaypointsColumns._ID, values);
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
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    int match = urlMatcher.match(url);
    String sortOrder = null;
    if (match == TRACKPOINTS) {
      qb.setTables(TRACKPOINTS_TABLE);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = TrackPointsColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == TRACKPOINTS_ID) {
      qb.setTables(TRACKPOINTS_TABLE);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else if (match == TRACKS) {
      qb.setTables(TRACKS_TABLE);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = TracksColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == TRACKS_ID) {
      qb.setTables(TRACKS_TABLE);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else if (match == WAYPOINTS) {
      qb.setTables(WAYPOINTS_TABLE);
      if (sort != null) {
        sortOrder = sort;
      } else {
        sortOrder = WaypointsColumns.DEFAULT_SORT_ORDER;
      }
    } else if (match == WAYPOINTS_ID) {
      qb.setTables(WAYPOINTS_TABLE);
      qb.appendWhere("_id=" + url.getPathSegments().get(1));
    } else {
      throw new IllegalArgumentException("Unknown URL " + url);
    }

    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
        sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), url);
    return c;
  }

  @Override
  public int update(Uri url, ContentValues values, String where,
      String[] selectionArgs) {
    int count;
    int match = urlMatcher.match(url);
    if (match == TRACKPOINTS) {
      count = db.update(TRACKPOINTS_TABLE, values, where, selectionArgs);
    } else if (match == TRACKPOINTS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(TRACKPOINTS_TABLE, values, "_id=" + segment
          + (!TextUtils.isEmpty(where)
              ? " AND (" + where + ')'
              : ""),
          selectionArgs);
    } else if (match == TRACKS) {
      count = db.update(TRACKS_TABLE, values, where, selectionArgs);
    } else if (match == TRACKS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(TRACKS_TABLE, values, "_id=" + segment
          + (!TextUtils.isEmpty(where)
              ? " AND (" + where + ')'
              : ""),
          selectionArgs);
    } else if (match == WAYPOINTS) {
      count = db.update(WAYPOINTS_TABLE, values, where, selectionArgs);
    } else if (match == WAYPOINTS_ID) {
      String segment = url.getPathSegments().get(1);
      count = db.update(WAYPOINTS_TABLE, values, "_id=" + segment
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
