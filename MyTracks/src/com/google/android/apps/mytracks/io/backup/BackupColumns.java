/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.io.backup;

import static com.google.android.apps.mytracks.content.ContentTypeIds.*;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointsColumns;

public class BackupColumns {
  /** Columns that go into the backup. */
  public static final String[] POINTS_BACKUP_COLUMNS =
      { TrackPointsColumns._ID, TrackPointsColumns.TRACKID, TrackPointsColumns.LATITUDE,
        TrackPointsColumns.LONGITUDE, TrackPointsColumns.ALTITUDE, TrackPointsColumns.BEARING,
        TrackPointsColumns.TIME, TrackPointsColumns.ACCURACY, TrackPointsColumns.SPEED,
        TrackPointsColumns.SENSOR };
  public static final byte[] POINTS_BACKUP_COLUMN_TYPES =
      { LONG_TYPE_ID, LONG_TYPE_ID, INT_TYPE_ID, INT_TYPE_ID, FLOAT_TYPE_ID,
        FLOAT_TYPE_ID, LONG_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID, BLOB_TYPE_ID };

  public static final String[] TRACKS_BACKUP_COLUMNS = {
      TracksColumns._ID, TracksColumns.NAME, TracksColumns.DESCRIPTION, TracksColumns.CATEGORY,
      TracksColumns.STARTID, TracksColumns.STOPID, TracksColumns.STARTTIME, TracksColumns.STOPTIME,
      TracksColumns.NUMPOINTS, TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME,
      TracksColumns.MOVINGTIME, TracksColumns.AVGSPEED, TracksColumns.AVGMOVINGSPEED,
      TracksColumns.MAXSPEED, TracksColumns.MINELEVATION, TracksColumns.MAXELEVATION,
      TracksColumns.ELEVATIONGAIN, TracksColumns.MINGRADE, TracksColumns.MAXGRADE,
      TracksColumns.MINLAT, TracksColumns.MAXLAT, TracksColumns.MINLON, TracksColumns.MAXLON,
      TracksColumns.MAPID, TracksColumns.TABLEID};
  public static final byte[] TRACKS_BACKUP_COLUMN_TYPES = {
      LONG_TYPE_ID, STRING_TYPE_ID, STRING_TYPE_ID, STRING_TYPE_ID,
      LONG_TYPE_ID, LONG_TYPE_ID, LONG_TYPE_ID, LONG_TYPE_ID, INT_TYPE_ID,
      FLOAT_TYPE_ID, LONG_TYPE_ID, LONG_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID,
      FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID,
      FLOAT_TYPE_ID, INT_TYPE_ID, INT_TYPE_ID, INT_TYPE_ID, INT_TYPE_ID, STRING_TYPE_ID,
      STRING_TYPE_ID};

  public static final String[] WAYPOINTS_BACKUP_COLUMNS = {
      WaypointsColumns._ID, WaypointsColumns.TRACKID, WaypointsColumns.NAME,
      WaypointsColumns.DESCRIPTION, WaypointsColumns.CATEGORY, WaypointsColumns.ICON,
      WaypointsColumns.TYPE, WaypointsColumns.LENGTH, WaypointsColumns.DURATION,
      WaypointsColumns.STARTTIME, WaypointsColumns.STARTID, WaypointsColumns.STOPID,
      WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE, WaypointsColumns.ALTITUDE,
      WaypointsColumns.BEARING, WaypointsColumns.TIME, WaypointsColumns.ACCURACY,
      WaypointsColumns.SPEED, WaypointsColumns.TOTALDISTANCE, WaypointsColumns.TOTALTIME,
      WaypointsColumns.MOVINGTIME, WaypointsColumns.AVGSPEED, WaypointsColumns.AVGMOVINGSPEED,
      WaypointsColumns.MAXSPEED, WaypointsColumns.MINELEVATION, WaypointsColumns.MAXELEVATION,
      WaypointsColumns.ELEVATIONGAIN, WaypointsColumns.MINGRADE, WaypointsColumns.MAXGRADE };
  public static final byte[] WAYPOINTS_BACKUP_COLUMN_TYPES = {
      LONG_TYPE_ID, LONG_TYPE_ID, STRING_TYPE_ID, STRING_TYPE_ID,
      STRING_TYPE_ID, STRING_TYPE_ID, INT_TYPE_ID, FLOAT_TYPE_ID, LONG_TYPE_ID,
      LONG_TYPE_ID, LONG_TYPE_ID, LONG_TYPE_ID, INT_TYPE_ID, INT_TYPE_ID,
      FLOAT_TYPE_ID, FLOAT_TYPE_ID, LONG_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID,
      FLOAT_TYPE_ID, LONG_TYPE_ID, LONG_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID,
      FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID, FLOAT_TYPE_ID,
      FLOAT_TYPE_ID };
}
