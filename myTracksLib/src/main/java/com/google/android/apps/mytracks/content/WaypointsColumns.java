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

  String TABLE_NAME = "waypoints";
  Uri CONTENT_URI = Uri.parse(
      "content://com.google.android.maps.mytracks/waypoints");
  String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.waypoint";
  String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.waypoint";
  String DEFAULT_SORT_ORDER = "_id";

  // Columns
  String NAME = "name"; // waypoint name
  String DESCRIPTION = "description"; // waypoint description
  String CATEGORY = "category"; // waypoint category
  String ICON = "icon"; // waypoint icon
  String TRACKID = "trackid"; // track id
  String TYPE = "type"; // type
  String LENGTH = "length"; // length of the track (without smoothing)
  String DURATION = "duration"; // total duration of the track (not from last waypoint)
  String STARTTIME = "starttime"; // start time of the trip statistics
  String STARTID = "startid"; // start track point id
  String STOPID = "stopid"; // stop track point id

  String LONGITUDE = "longitude"; // longitude
  String LATITUDE = "latitude"; // latitude
  String TIME = "time"; // time
  String ALTITUDE = "elevation"; // altitude
  String ACCURACY = "accuracy"; // accuracy
  String SPEED = "speed"; // speed
  String BEARING = "bearing"; // bearing

  String TOTALDISTANCE = "totaldistance"; // total distance
  String TOTALTIME = "totaltime"; // total time
  String MOVINGTIME = "movingtime"; // moving time
  String AVGSPEED = "avgspeed"; // average speed
  String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
  String MAXSPEED = "maxspeed"; // max speed
  String MINELEVATION = "minelevation"; // min elevation
  String MAXELEVATION = "maxelevation"; // max elevation
  String ELEVATIONGAIN = "elevationgain"; // elevation gain
  String MINGRADE = "mingrade"; // min grade
  String MAXGRADE = "maxgrade"; // max grade
  String CALORIE = "calorie"; // calorie
  
  String PHOTOURL = "photoUrl"; // url for the photo
  
  String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
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
      + MAXGRADE + " FLOAT, "
      + CALORIE + " FLOAT, "  
      + PHOTOURL + " STRING"
      + ");";
  
  String[] COLUMNS = {
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
      MAXGRADE,
      CALORIE,
      PHOTOURL
    };
    
  byte[] COLUMN_TYPES = {
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
      FLOAT_TYPE_ID, // max grade
      FLOAT_TYPE_ID, // calorie
      STRING_TYPE_ID // photo url
    };
}
