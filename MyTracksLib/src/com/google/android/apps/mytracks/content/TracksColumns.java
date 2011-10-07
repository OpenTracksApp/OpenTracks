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
 * Defines the URI for the tracks provider and the available column names
 * and content types.
 *
 * @author Leif Hendrik Wilden
 */
public interface TracksColumns extends BaseColumns {

  // MyTracks content provider URI for the tracks table.
  public static final Uri CONTENT_URI = Uri.parse(
      "content://" + MyTracksProviderUtils.AUTHORITY + "/tracks");

  // Database content provider URI for the tracks table.
  public static final Uri DATABASE_CONTENT_URI = Uri.parse(
      "content://" + MyTracksProviderUtils.DATABASE_AUTHORITY + "/tracks");
  public static final String CONTENT_TYPE =
      "vnd.android.cursor.dir/vnd.google.track";
  public static final String CONTENT_ITEMTYPE =
      "vnd.android.cursor.item/vnd.google.track";
  public static final String DEFAULT_SORT_ORDER = "_id";

  /* All columns */
  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String CATEGORY = "category";
  public static final String STARTID = "startid";
  public static final String STOPID = "stopid";
  public static final String STARTTIME = "starttime";
  public static final String STOPTIME = "stoptime";
  public static final String NUMPOINTS = "numpoints";
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
  public static final String MINLAT = "minlat";
  public static final String MAXLAT = "maxlat";
  public static final String MINLON = "minlon";
  public static final String MAXLON = "maxlon";
  public static final String MAPID = "mapid";
  public static final String TABLEID = "tableid";
}
