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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines the URI for the tracks provider and the available column names
 * and content types.
 *
 * @author Leif Hendrik Wilden
 */
public interface WaypointsColumns extends BaseColumns {

  public static final Uri CONTENT_URI =
      Uri.parse("content://com.google.android.maps.mytracks/waypoints");
  public static final String CONTENT_TYPE =
      "vnd.android.cursor.dir/vnd.google.waypoint";
  public static final String CONTENT_ITEMTYPE =
      "vnd.android.cursor.item/vnd.google.waypoint";
  public static final String DEFAULT_SORT_ORDER = "_id";

  /* All columns */
  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String CATEGORY = "category";
  public static final String ICON = "icon";
  public static final String TRACKID = "trackid";
  public static final String TYPE = "type";
  public static final String LENGTH = "length";
  public static final String DURATION = "duration";
  public static final String STARTTIME = "starttime";
  public static final String STARTID = "startid";
  public static final String STOPID = "stopid";

  public static final String LATITUDE = "latitude";
  public static final String LONGITUDE = "longitude";
  public static final String ALTITUDE = "elevation";
  public static final String BEARING = "bearing";
  public static final String TIME = "time";
  public static final String ACCURACY = "accuracy";
  public static final String SPEED = "speed";

  public static final String TOTALDISTANCE = "totaldistance";
  public static final String TOTALTIME = "totaltime";
  public static final String MOVINGTIME = "movingtime";
  public static final String AVGSPEED = "avgspeed";
  public static final String AVGMOVINGSPEED = "avgmovingspeed";
  public static final String MAXSPEED = "maxspeed";
  public static final String MINELEVATION = "minelevation";
  public static final String MAXELEVATION = "maxelevation";
  public static final String ELEVATIONGAIN = "elevationgain";
  public static final String MINGRADE = "mingrade";
  public static final String MAXGRADE = "maxgrade";
}
