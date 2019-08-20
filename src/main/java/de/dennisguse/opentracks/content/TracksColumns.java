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

package de.dennisguse.opentracks.content;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for the tracks table.
 *
 * @author Leif Hendrik Wilden
 */
public interface TracksColumns extends BaseColumns {

    String TABLE_NAME = "tracks";

    /**
     * Tracks provider uri.
     */
    Uri CONTENT_URI = Uri.parse("content://" + ContentProviderUtils.AUTHORITY + "/tracks");

    /**
     * Track content type.
     */
    String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.de.dennisguse.track";

    /**
     * Track id content type.
     */
    String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.de.dennisguse.track";

    /**
     * Tracks table default sort order.
     */
    String DEFAULT_SORT_ORDER = "_id";

    // Columns
    String NAME = "name"; // track name
    String DESCRIPTION = "description"; // track description
    String CATEGORY = "category"; // track activity type
    String STARTID = "startid"; // first track point id
    String STOPID = "stopid"; // last track point id
    String STARTTIME = "starttime"; // track start time
    String STOPTIME = "stoptime"; // track stop time
    String NUMPOINTS = "numpoints"; // number of track points
    String TOTALDISTANCE = "totaldistance"; // total distance
    String TOTALTIME = "totaltime"; // total time
    String MOVINGTIME = "movingtime"; // moving time
    String MINLAT = "minlat"; // minimum latitude
    String MAXLAT = "maxlat"; // maximum latitude
    String MINLON = "minlon"; // minimum longitude
    String MAXLON = "maxlon"; // maximum longitude
    String AVGSPEED = "avgspeed"; // average speed

    // average moving speed
    String AVGMOVINGSPEED = "avgmovingspeed";
    String MAXSPEED = "maxspeed"; // maximum speed
    String MINELEVATION = "minelevation"; // minimum elevation
    String MAXELEVATION = "maxelevation"; // maximum elevation
    String ELEVATIONGAIN = "elevationgain"; // elevation gain
    String MINGRADE = "mingrade"; // minimum grade
    String MAXGRADE = "maxgrade"; // maximum grade
    String ICON = "icon"; // track activity type icon

    String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" // table
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // id
            + NAME + " TEXT, " // name
            + DESCRIPTION + " TEXT, " // description
            + CATEGORY + " TEXT, " // category
            + STARTID + " INTEGER, " // start id
            + STOPID + " INTEGER, " // stop id
            + STARTTIME + " INTEGER, " // start time
            + STOPTIME + " INTEGER, " // stop time
            + NUMPOINTS + " INTEGER, " // num points
            + TOTALDISTANCE + " FLOAT, " // total distance
            + TOTALTIME + " INTEGER, " // total time
            + MOVINGTIME + " INTEGER, " // moving time
            + MINLAT + " INTEGER, " // min latitude
            + MAXLAT + " INTEGER, " // max latitude
            + MINLON + " INTEGER, " // min longitude
            + MAXLON + " INTEGER, " // max longitude
            + AVGSPEED + " FLOAT, " // average speed
            + AVGMOVINGSPEED + " FLOAT, " // average moving speed
            + MAXSPEED + " FLOAT, " // max speed
            + MINELEVATION + " FLOAT, " // min elevation
            + MAXELEVATION + " FLOAT, " // max elevation
            + ELEVATIONGAIN + " FLOAT, " // elevation gain
            + MINGRADE + " FLOAT, " // min grade
            + MAXGRADE + " FLOAT, " // max grade
            + ICON + " TEXT);"; // icon
}
