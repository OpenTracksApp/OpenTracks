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

package de.dennisguse.opentracks.content.data;

import android.net.Uri;
import android.provider.BaseColumns;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * Constants for the track points table.
 *
 * @author Leif Hendrik Wilden
 */
public interface TrackPointsColumns extends BaseColumns {

    String TABLE_NAME = "trackpoints";
    Uri CONTENT_URI_BY_ID = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME);
    Uri CONTENT_URI_BY_TRACKID = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME + "/trackid");
    String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.de.dennisguse.trackpoint";
    String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.de.dennisguse.trackpoint";
    String DEFAULT_SORT_ORDER = _ID;

    // Columns
    String TRACKID = "trackid";

    String LONGITUDE = "longitude";
    String LATITUDE = "latitude";
    @Deprecated
    double PAUSE_LATITUDE = 100.0;
    @Deprecated
    double RESUME_LATITUDE = 200.0;

    String TIME = "time";
    String ALTITUDE = "elevation";
    String ACCURACY = "accuracy";
    String SPEED = "speed";
    String BEARING = "bearing";
    String SENSOR_HEARTRATE = "sensor_heartrate";
    String SENSOR_CADENCE = "sensor_cadence";
    String SENSOR_POWER = "sensor_power";
    String ELEVATION_GAIN = "elevation_gain";
    String ELEVATION_LOSS = "elevation_loss";

    String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TRACKID + " INTEGER NOT NULL, "
            + LONGITUDE + " INTEGER, "
            + LATITUDE + " INTEGER, "
            + TIME + " INTEGER, "
            + ALTITUDE + " FLOAT, "
            + ACCURACY + " FLOAT, "
            + SPEED + " FLOAT, "
            + BEARING + " FLOAT, "
            + SENSOR_HEARTRATE + " FLOAT, "
            + SENSOR_CADENCE + " FLOAT, "
            + SENSOR_POWER + " FLOAT, "
            + ELEVATION_GAIN + " FLOAT, "
            + ELEVATION_LOSS + " FLOAT, "
            + "FOREIGN KEY (" + TRACKID + ") REFERENCES " + TracksColumns.TABLE_NAME + "(" + TracksColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
            + ")";

    String CREATE_TABLE_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + TRACKID + "_index ON " + TABLE_NAME + "(" + TRACKID + ")";
}
