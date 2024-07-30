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

package de.dennisguse.opentracks.data.tables;

import android.net.Uri;
import android.provider.BaseColumns;

import de.dennisguse.opentracks.data.ContentProviderUtils;

/**
 * Constants for markers table.
 *
 * @author Leif Hendrik Wilden
 */
public interface MarkerColumns extends BaseColumns {

    String TABLE_NAME = "markers";
    Uri CONTENT_URI = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME);
    Uri CONTENT_URI_BY_TRACKID = Uri.parse(ContentProviderUtils.CONTENT_BASE_URI + "/" + TABLE_NAME + "/trackid");
    String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.de.dennisguse.waypoint";
    String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.de.dennisguse.waypoint";
    String DEFAULT_SORT_ORDER = _ID;

    // Columns
    String NAME = "name"; // marker name
    String DESCRIPTION = "description"; // marker description
    String CATEGORY = "category"; // marker category
    String ICON = "icon"; // marker icon
    String TRACKID = "trackid"; // track id
    String LONGITUDE = "longitude"; // longitude
    String LATITUDE = "latitude"; // latitude
    String TIME = "time"; // time
    String ALTITUDE = "elevation"; // altitude //TODO RENAME column
    String ACCURACY = "accuracy"; // accuracy
    String BEARING = "bearing"; // bearing

    String PHOTOURL = "photoUrl"; // url for the photo

    String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NAME + " TEXT, "
            + DESCRIPTION + " TEXT, "
            + CATEGORY + " TEXT, "
            + ICON + " TEXT, "
            + TRACKID + " INTEGER NOT NULL, "
            + LONGITUDE + " INTEGER, "
            + LATITUDE + " INTEGER, "
            + TIME + " INTEGER, "
            + ALTITUDE + " FLOAT, "
            + ACCURACY + " FLOAT, "
            + BEARING + " FLOAT, "
            + PHOTOURL + " TEXT, "
            + "FOREIGN KEY (" + TRACKID + ") REFERENCES " + TracksColumns.TABLE_NAME + "(" + TracksColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
            + ")";

    String CREATE_TABLE_INDEX = "CREATE INDEX " + TABLE_NAME + "_" + TRACKID + "_index ON " + TABLE_NAME + "(" + TRACKID + ")";
}
