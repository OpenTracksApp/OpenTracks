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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for the tracks table.
 * 
 * @author Leif Hendrik Wilden
 */
public interface TracksColumns extends BaseColumns {

  public static final String TABLE_NAME = "tracks";

  /**
   * Tracks provider uri.
   */
  public static final Uri CONTENT_URI = Uri.parse(
      "content://com.google.android.maps.mytracks/tracks");

  /**
   * Track content type.
   */
  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.track";

  /**
   * Track id content type.
   */
  public static final String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.track";

  /**
   * Tracks table default sort order.
   */
  public static final String DEFAULT_SORT_ORDER = "_id";

  // Columns
  public static final String NAME = "name"; // track name
  public static final String DESCRIPTION = "description"; // track description
  public static final String CATEGORY = "category"; // track activity type
  public static final String STARTID = "startid"; // first track point id
  public static final String STOPID = "stopid"; // last track point id
  public static final String STARTTIME = "starttime"; // track start time
  public static final String STOPTIME = "stoptime"; // track stop time
  public static final String NUMPOINTS = "numpoints"; // number of track points
  public static final String TOTALDISTANCE = "totaldistance"; // total distance
  public static final String TOTALTIME = "totaltime"; // total time
  public static final String MOVINGTIME = "movingtime"; // moving time
  public static final String MINLAT = "minlat"; // minimum latitude
  public static final String MAXLAT = "maxlat"; // maximum latitude
  public static final String MINLON = "minlon"; // minimum longitude
  public static final String MAXLON = "maxlon"; // maximum longitude
  public static final String AVGSPEED = "avgspeed"; // average speed
  public static final String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
  public static final String MAXSPEED = "maxspeed"; // maximum speed
  public static final String MINELEVATION = "minelevation"; // minimum elevation
  public static final String MAXELEVATION = "maxelevation"; // maximum elevation
  public static final String ELEVATIONGAIN = "elevationgain"; // elevation gain
  public static final String MINGRADE = "mingrade"; // minimum grade
  public static final String MAXGRADE = "maxgrade"; // maximum grade
  public static final String MAPID = "mapid"; // Google Maps id
  public static final String TABLEID = "tableid"; // Google Fusion Tables id
  public static final String ICON = "icon"; // track activity type icon
  
  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" 
      + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
      + NAME + " STRING, " 
      + DESCRIPTION + " STRING, "
      + CATEGORY + " STRING, " 
      + STARTID + " INTEGER, " 
      + STOPID + " INTEGER, " 
      + STARTTIME + " INTEGER, " 
      + STOPTIME + " INTEGER, " 
      + NUMPOINTS + " INTEGER, " 
      + TOTALDISTANCE + " FLOAT, " 
      + TOTALTIME + " INTEGER, " 
      + MOVINGTIME + " INTEGER, " 
      + MINLAT + " INTEGER, "
      + MAXLAT + " INTEGER, " 
      + MINLON + " INTEGER, " 
      + MAXLON + " INTEGER, " 
      + AVGSPEED + " FLOAT, " 
      + AVGMOVINGSPEED + " FLOAT, " 
      + MAXSPEED + " FLOAT, " 
      + MINELEVATION + " FLOAT, "
      + MAXELEVATION + " FLOAT, " 
      + ELEVATIONGAIN + " FLOAT, " 
      + MINGRADE + " FLOAT, " 
      + MAXGRADE + " FLOAT, " 
      + MAPID + " STRING, " 
      + TABLEID + " STRING, " 
      + ICON + " STRING" 
      + ");";

  public static final String[] COLUMNS = {
      _ID,
      NAME,
      DESCRIPTION,
      CATEGORY,
      STARTID,
      STOPID,
      STARTTIME,
      STOPTIME,
      NUMPOINTS,
      TOTALDISTANCE,
      TOTALTIME,
      MOVINGTIME,
      MINLAT,
      MAXLAT,
      MINLON,
      MAXLON,
      AVGSPEED,
      AVGMOVINGSPEED,
      MAXSPEED,
      MINELEVATION,
      MAXELEVATION,
      ELEVATIONGAIN,
      MINGRADE,
      MAXGRADE,
      MAPID,
      TABLEID,
      ICON
  };

  public static final byte[] COLUMN_TYPES = {
      ContentTypeIds.LONG_TYPE_ID, // id
      ContentTypeIds.STRING_TYPE_ID, // name
      ContentTypeIds.STRING_TYPE_ID, // description
      ContentTypeIds.STRING_TYPE_ID, // category
      ContentTypeIds.LONG_TYPE_ID, // start id
      ContentTypeIds.LONG_TYPE_ID, // stop id
      ContentTypeIds.LONG_TYPE_ID, // start time
      ContentTypeIds.LONG_TYPE_ID, // stop time
      ContentTypeIds.INT_TYPE_ID, // num points
      ContentTypeIds.FLOAT_TYPE_ID, // total distance
      ContentTypeIds.LONG_TYPE_ID, // total time
      ContentTypeIds.LONG_TYPE_ID, // moving time
      ContentTypeIds.INT_TYPE_ID, // min latitude
      ContentTypeIds.INT_TYPE_ID, // max latitude
      ContentTypeIds.INT_TYPE_ID, // min longitude
      ContentTypeIds.INT_TYPE_ID, // max longitude
      ContentTypeIds.FLOAT_TYPE_ID, // average speed
      ContentTypeIds.FLOAT_TYPE_ID, // average moving speed
      ContentTypeIds.FLOAT_TYPE_ID, // max speed
      ContentTypeIds.FLOAT_TYPE_ID, // min elevation
      ContentTypeIds.FLOAT_TYPE_ID, // max elevation
      ContentTypeIds.FLOAT_TYPE_ID, // elevation gain
      ContentTypeIds.FLOAT_TYPE_ID, // min grade
      ContentTypeIds.FLOAT_TYPE_ID, // max grade
      ContentTypeIds.STRING_TYPE_ID, // map id
      ContentTypeIds.STRING_TYPE_ID, // table id
      ContentTypeIds.STRING_TYPE_ID // icon
    };
}
