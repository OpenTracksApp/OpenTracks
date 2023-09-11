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

package de.dennisguse.opentracks.data.tables;

import android.net.Uri;
import android.provider.BaseColumns;

import de.dennisguse.opentracks.data.ContentProviderUtils;

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
    // See {@link TrackPoint.Type}
    String TYPE = "type";

    String LONGITUDE = "longitude";
    String LATITUDE = "latitude";

    String TIME = "time";
    String ALTITUDE = "elevation";
    String HORIZONTAL_ACCURACY = "accuracy";
    String VERTICAL_ACCURACY = "accuracy_vertical";
    String SPEED = "speed";
    String BEARING = "bearing";
    String SENSOR_HEARTRATE = "sensor_heartrate";
    String SENSOR_CADENCE = "sensor_cadence";
    String SENSOR_DISTANCE = "sensor_distance"; //DISTANCE from previous TrackPoint
    String SENSOR_POWER = "sensor_power";
    String ALTITUDE_GAIN = "elevation_gain";
    String ALTITUDE_LOSS = "elevation_loss";

    // Alias for sensor statistics
    String ALIAS_AVG_HR = "avg_hr";
    String ALIAS_MAX_HR = "max_hr";
    String ALIAS_AVG_CADENCE = "avg_cadence";
    String ALIAS_MAX_CADENCE = "max_cadence";
    String ALIAS_AVG_POWER = "avg_power";
    String ALIAS_MAX_POWER = "max_power";

    String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TRACKID + " INTEGER NOT NULL, "
            + LONGITUDE + " INTEGER, "
            + LATITUDE + " INTEGER, "
            + TIME + " INTEGER, "
            + ALTITUDE + " FLOAT, "
            + HORIZONTAL_ACCURACY + " FLOAT, "
            + SPEED + " FLOAT, "
            + BEARING + " FLOAT, "
            + SENSOR_HEARTRATE + " FLOAT, "
            + SENSOR_CADENCE + " FLOAT, "
            + SENSOR_POWER + " FLOAT, "
            + ALTITUDE_GAIN + " FLOAT, "
            + ALTITUDE_LOSS + " FLOAT, "
            + TYPE + " TEXT CHECK(type IN (-2, -1, 0, 1, 3)), "
            + SENSOR_DISTANCE + " FLOAT, "
            + VERTICAL_ACCURACY + " FLOAT, "
            + "FOREIGN KEY (" + TRACKID + ") REFERENCES " + TracksColumns.TABLE_NAME + "(" + TracksColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
            + ")";

    String CREATE_TABLE_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + TRACKID + "_index ON " + TABLE_NAME + "(" + TRACKID + ")";
}
