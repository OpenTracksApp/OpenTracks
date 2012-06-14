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

import static com.google.android.apps.mytracks.content.ContentTypeIds.BLOB_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.FLOAT_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.INT_TYPE_ID;
import static com.google.android.apps.mytracks.content.ContentTypeIds.LONG_TYPE_ID;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for the track points table.
 *
 * @author Leif Hendrik Wilden
 */
public interface TrackPointsColumns extends BaseColumns {

  public static final String TABLE_NAME = "trackpoints";
  public static final Uri CONTENT_URI = Uri.parse(
      "content://com.google.android.maps.mytracks/trackpoints");
  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.trackpoint";
  public static final String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.trackpoint";
  public static final String DEFAULT_SORT_ORDER = "_id";

  // Columns
  public static final String TRACKID = "trackid"; // track id
  public static final String LONGITUDE = "longitude"; // longitude
  public static final String LATITUDE = "latitude"; // latitude
  public static final String TIME = "time"; // time
  public static final String ALTITUDE = "elevation"; // altitude
  public static final String ACCURACY = "accuracy"; // accuracy
  public static final String SPEED = "speed"; // speed
  public static final String BEARING = "bearing"; // bearing
  public static final String SENSOR = "sensor"; // sensor

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
      + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + TRACKID + " INTEGER, "
      + LONGITUDE + " INTEGER, "
      + LATITUDE + " INTEGER, "
      + TIME + " INTEGER, "
      + ALTITUDE + " FLOAT, "
      + ACCURACY + " FLOAT, "
      + SPEED + " FLOAT, "
      + BEARING + " FLOAT, "
      + SENSOR + " BLOB" 
      + ");";

  public static final String[] COLUMNS = {
      _ID,
      TRACKID,
      LONGITUDE,
      LATITUDE,
      TIME,
      ALTITUDE,
      ACCURACY,
      SPEED,
      BEARING,
      SENSOR
   };

   public static final byte[] COLUMN_TYPES = {
       LONG_TYPE_ID, // id
       LONG_TYPE_ID, // track id
       INT_TYPE_ID, // longitude
       INT_TYPE_ID, // latitude
       LONG_TYPE_ID, // time
       FLOAT_TYPE_ID, // altitude
       FLOAT_TYPE_ID, // accuracy
       FLOAT_TYPE_ID, // speed
       FLOAT_TYPE_ID, // bearing
       BLOB_TYPE_ID // sensor
   };
}
