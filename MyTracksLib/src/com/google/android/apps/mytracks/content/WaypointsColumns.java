/*
 * Copyright 2009 Google Inc.
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

import static com.google.android.apps.mytracks.content.ContentTypeIds.FLOAT_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.INT_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.LONG_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.STRING_TYPE_ID;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for waypoints table.
 * 
 * @author Leif Hendrik Wilden
 */
public interface WaypointsColumns extends BaseColumns {

  public static final String TABLE_NAME = "waypoints";
  public static final Uri CONTENT_URI = Uri.parse(
      "content://com.google.android.maps.mytracks/waypoints");
  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.waypoint";
  public static final String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.waypoint";
  public static final String DEFAULT_SORT_ORDER = "_id";

  // Columns
  public static final String NAME = "name"; // waypoint name
  public static final String DESCRIPTION = "description"; // waypoint description
  public static final String CATEGORY = "category"; // waypoint category
  public static final String ICON = "icon"; // waypoint icon
  public static final String TRACKID = "trackid"; // track id
  public static final String TYPE = "type"; // type
  public static final String LENGTH = "length"; // length of the track (without smoothing) 
  public static final String DURATION = "duration"; // total duration of the track (not from last waypoint)
  public static final String STARTTIME = "starttime"; // start time of the trip statistics 
  public static final String STARTID = "startid"; // start track point id
  public static final String STOPID = "stopid"; // stop track point id

  public static final String LONGITUDE = "longitude"; // longitude
  public static final String LATITUDE = "latitude"; // latitude
  public static final String TIME = "time"; // time
  public static final String ALTITUDE = "elevation"; // altitude
  public static final String ACCURACY = "accuracy"; // accuracy
  public static final String SPEED = "speed"; // speed  
  public static final String BEARING = "bearing"; // bearing

  public static final String TOTALDISTANCE = "totaldistance"; // total distance
  public static final String TOTALTIME = "totaltime"; // total time
  public static final String MOVINGTIME = "movingtime"; // moving time
  public static final String AVGSPEED = "avgspeed"; // average speed
  public static final String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
  public static final String MAXSPEED = "maxspeed"; // max speed
  public static final String MINELEVATION = "minelevation"; // min elevation
  public static final String MAXELEVATION = "maxelevation"; // max elevation
  public static final String ELEVATIONGAIN = "elevationgain"; // elevation gain
  public static final String MINGRADE = "mingrade"; // min grade
  public static final String MAXGRADE = "maxgrade"; // max grade
  
  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" 
      + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + NAME + " STRING, "
      + DESCRIPTION + " STRING, "
      + CATEGORY + " STRING, "
      + ICON + " STRING, "
      + TRACKID + " INTEGER, "
      + TYPE + " INTEGER, "
      + LENGTH + " FLOAT, "
      + DURATION + " INTEGER, "
      + STARTTIME + " INTEGER, "
      + STARTID + " INTEGER, "
      + STOPID + " INTEGER, "
      + LONGITUDE + " INTEGER, "
      + LATITUDE + " INTEGER, "
      + TIME + " INTEGER, "
      + ALTITUDE + " FLOAT, "
      + ACCURACY + " FLOAT, "
      + SPEED + " FLOAT, "
      + BEARING + " FLOAT, "
      + TOTALDISTANCE + " FLOAT, "
      + TOTALTIME + " INTEGER, "
      + MOVINGTIME + " INTEGER, "
      + AVGSPEED + " FLOAT, "
      + AVGMOVINGSPEED + " FLOAT, "
      + MAXSPEED + " FLOAT, "
      + MINELEVATION + " FLOAT, "
      + MAXELEVATION + " FLOAT, "
      + ELEVATIONGAIN + " FLOAT, "
      + MINGRADE + " FLOAT, "
      + MAXGRADE + " FLOAT" 
      + ");";
  
  public static final String[] COLUMNS = {
      _ID,
      NAME,
      DESCRIPTION,
      CATEGORY,
      ICON,
      TRACKID,
      TYPE,
      LENGTH,
      DURATION,
      STARTTIME,
      STARTID,
      STOPID,
      LONGITUDE,
      LATITUDE,
      TIME,
      ALTITUDE,
      ACCURACY,
      SPEED,
      BEARING,
      TOTALDISTANCE,
      TOTALTIME,
      MOVINGTIME,
      AVGSPEED,
      AVGMOVINGSPEED,
      MAXSPEED,
      MINELEVATION,
      MAXELEVATION,
      ELEVATIONGAIN,
      MINGRADE,
      MAXGRADE
    };
    
  public static final byte[] COLUMN_TYPES = {
      LONG_TYPE_ID, // id
      STRING_TYPE_ID, // name
      STRING_TYPE_ID, // description
      STRING_TYPE_ID, // category
      STRING_TYPE_ID, // icon
      LONG_TYPE_ID, // track id
      INT_TYPE_ID, // type
      FLOAT_TYPE_ID, // length
      LONG_TYPE_ID, // duration
      LONG_TYPE_ID, // start time
      LONG_TYPE_ID, // start id
      LONG_TYPE_ID, // stop id
      INT_TYPE_ID, // longitude
      INT_TYPE_ID, // latitude
      LONG_TYPE_ID, // time
      FLOAT_TYPE_ID, // altitude
      FLOAT_TYPE_ID, // accuracy
      FLOAT_TYPE_ID, // speed
      FLOAT_TYPE_ID, // bearing
      FLOAT_TYPE_ID, // total distance
      LONG_TYPE_ID, // total time
      LONG_TYPE_ID, // moving time
      FLOAT_TYPE_ID, // average speed
      FLOAT_TYPE_ID, // average moving speed
      FLOAT_TYPE_ID, // max speed
      FLOAT_TYPE_ID, // min elevation
      FLOAT_TYPE_ID, // max elevation
      FLOAT_TYPE_ID, // elevation gain
      FLOAT_TYPE_ID, // min grade
      FLOAT_TYPE_ID // max grade
    };
}
