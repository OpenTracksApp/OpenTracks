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
    Uri CONTENT_URI = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME);
    String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.de.dennisguse.trackpoint";
    String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.de.dennisguse.trackpoint";
    String DEFAULT_SORT_ORDER = "_id";

    // Columns
    String TRACKID = "trackid";
    String LONGITUDE = "longitude";
    String LATITUDE = "latitude";
    String TIME = "time";
    String ALTITUDE = "elevation";
    String ACCURACY = "accuracy";
    String SPEED = "speed";
    String BEARING = "bearing";
    String SENSOR_HEARTRATE = "sensor_heartrate";
    String SENSOR_CADENCE = "sensor_cadence";
    String SENSOR_POWER = "sensor_power";

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
            + SENSOR_HEARTRATE + " FLOAT, "
            + SENSOR_CADENCE + " FLOAT, "
            + SENSOR_POWER + " FLOAT);";
}
