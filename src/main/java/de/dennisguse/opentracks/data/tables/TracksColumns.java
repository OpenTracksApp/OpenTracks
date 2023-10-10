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
 * Constants for the tracks table.
 *
 * @author Leif Hendrik Wilden
 */
public interface TracksColumns extends BaseColumns {

    String TABLE_NAME = "tracks";
    Uri CONTENT_URI = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME);
    Uri CONTENT_URI_SENSOR_STATS = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME + "/sensorstats");
    String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.de.dennisguse.track";
    String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.de.dennisguse.track";
    String DEFAULT_SORT_ORDER = _ID;

    // Columns
    String UUID = "uuid"; // identifier to make tracks globally unique (prevent re-import)
    String NAME = "name"; // track name
    String DESCRIPTION = "description"; // track description
    /** see {@link de.dennisguse.opentracks.data.models.ActivityType}.id */
    String ACTIVITY_TYPE = "activity_type";
    String ACTIVITY_TYPE_LOCALIZED = "category"; // track activity type
    @Deprecated
    String ICON = "icon"; // track activity type icon
    String STARTTIME = "starttime"; // track start time
    String STARTTIME_OFFSET = "starttime_offset"; // in plus/minus in seconds
    String STOPTIME = "stoptime"; // track stop time
    String MARKER_COUNT = "markerCount"; // the numbers of markers (virtual column)
    @Deprecated
    String NUMPOINTS = "numpoints"; // number of track points //TODO UNUSED
    String TOTALDISTANCE = "totaldistance"; // total distance
    String TOTALTIME = "totaltime"; // total time
    String MOVINGTIME = "movingtime"; // moving time

    String AVGSPEED = "avgspeed"; // average speed
    String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
    String MAXSPEED = "maxspeed"; // maximum speed
    String MIN_ALTITUDE = "minelevation"; // minimum altitude //TODO RENAME column
    String MAX_ALTITUDE = "maxelevation"; // maximum altitude //TODO RENAME column
    String ALTITUDE_GAIN = "elevationgain"; // altitude gain //TODO RENAME column
    String ALTITUDE_LOSS = "elevationloss"; // altitude loss //TODO RENAME column

    String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NAME + " TEXT, "
            + DESCRIPTION + " TEXT, "
            + ACTIVITY_TYPE_LOCALIZED + " TEXT, "
            + STARTTIME + " INTEGER, "
            + STOPTIME + " INTEGER, "
            + NUMPOINTS + " INTEGER, "
            + TOTALDISTANCE + " FLOAT, "
            + TOTALTIME + " INTEGER, "
            + MOVINGTIME + " INTEGER, "
            + AVGSPEED + " FLOAT, "
            + AVGMOVINGSPEED + " FLOAT, "
            + MAXSPEED + " FLOAT, "
            + MIN_ALTITUDE + " FLOAT, "
            + MAX_ALTITUDE + " FLOAT, "
            + ALTITUDE_GAIN + " FLOAT, "
            + ICON + " TEXT, "
            + UUID + " BLOB, "
            + ALTITUDE_LOSS + " FLOAT, "
            + STARTTIME_OFFSET + " INTEGER, "
            + ACTIVITY_TYPE + " TEXT)";

    String CREATE_TABLE_INDEX = "CREATE UNIQUE INDEX " + TABLE_NAME + "_" + UUID + "_index ON " + TABLE_NAME + "(" + UUID + ")";

}
