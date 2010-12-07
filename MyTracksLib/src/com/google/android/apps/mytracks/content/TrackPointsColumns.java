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
 * Defines the URI for the track points provider and the available column names
 * and content types.
 *
 * @author Leif Hendrik Wilden
 */
public interface TrackPointsColumns extends BaseColumns {
  public static final Uri CONTENT_URI =
      Uri.parse("content://com.google.android.maps.mytracks/trackpoints");
  public static final String CONTENT_TYPE =
      "vnd.android.cursor.dir/vnd.google.trackpoint";
  public static final String CONTENT_ITEMTYPE =
      "vnd.android.cursor.item/vnd.google.trackpoint";
  public static final String DEFAULT_SORT_ORDER = "_id";

  /* All columns */
  public static final String TRACKID = "trackid";
  public static final String LATITUDE = "latitude";
  public static final String LONGITUDE = "longitude";
  public static final String ALTITUDE = "elevation";
  public static final String BEARING = "bearing";
  public static final String TIME = "time";
  public static final String ACCURACY = "accuracy";
  public static final String SPEED = "speed";
  public static final String SENSOR = "sensor";
}
