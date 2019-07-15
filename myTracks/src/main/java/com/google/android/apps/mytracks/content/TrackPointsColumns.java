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

  String TABLE_NAME = "trackpoints";
  Uri CONTENT_URI = Uri.parse(
      "content://com.google.android.maps.mytracks/trackpoints");
  String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.trackpoint";
  String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.trackpoint";
  String DEFAULT_SORT_ORDER = "_id";

  // Columns
  String TRACKID = "trackid"; // track id
  String LONGITUDE = "longitude"; // longitude
  String LATITUDE = "latitude"; // latitude
  String TIME = "time"; // time
  String ALTITUDE = "elevation"; // altitude
  String ACCURACY = "accuracy"; // accuracy
  String SPEED = "speed"; // speed
  String BEARING = "bearing"; // bearing
  String SENSOR = "sensor"; // sensor

  String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
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

  String[] COLUMNS = {
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

   byte[] COLUMN_TYPES = {
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
