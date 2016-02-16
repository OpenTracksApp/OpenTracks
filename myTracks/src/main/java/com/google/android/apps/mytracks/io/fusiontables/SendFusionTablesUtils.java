/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.io.fusiontables;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.common.annotations.VisibleForTesting;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Utilities for sending a track to Google Fusion Tables.
 * 
 * @author Jimmy Shih
 */
public class SendFusionTablesUtils {

  private static final String MAP_URL = "https://www.google.com/fusiontables/embedviz?"
      + "viz=MAP&q=select+col0,+col1,+col2,+col3+from+%s+&h=false&lat=%f&lng=%f&z=%d&t=1&l=col2";
  private static final String TAG = SendFusionTablesUtils.class.getSimpleName();

  private SendFusionTablesUtils() {}

  /**
   * Gets the url to visualize a fusion table on a map.
   *
   * @param track the track
   * @param tableId the table id
   * @return the url.
   */
  public static String getMapUrl(Track track, String tableId) {
    if (track == null || track.getTripStatistics() == null) {
      Log.e(TAG, "Invalid track");
      return null;
    }
    double latE6;
    double lonE6;
    int z;
    if (track.getNumberOfPoints() < 2) {
      // Use Google's latitude and longitude
      latE6 = 37.423 * 1.E6;
      lonE6 = -122.084 * 1.E6;
      z = 2;
    } else {
      TripStatistics stats = track.getTripStatistics();
      latE6 = stats.getBottom() + (stats.getTop() - stats.getBottom()) / 2;
      lonE6 = stats.getLeft() + (stats.getRight() - stats.getLeft()) / 2;
      z = 15;
    }
    // We explicitly format with Locale.US because we need the latitude and
    // longitude to be formatted in a locale-independent manner. Specifically,
    // we need the decimal separator to be a period rather than a comma.
    return String.format(Locale.US, MAP_URL, tableId, latE6 / 1.E6, lonE6 / 1.E6, z);
  }

  /**
   * Formats an array of values as a SQL VALUES like
   * ('value1','value2',...,'value_n'). Escapes single quotes with two single
   * quotes.
   *
   * @param values an array of values to format
   * @return the formated SQL VALUES.
   */
  public static String formatSqlValues(String... values) {
    StringBuilder builder = new StringBuilder("(");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append('\'');
      builder.append(escapeSqlString(values[i]));
      builder.append('\'');
    }
    builder.append(")");
    return builder.toString();
  }

  /**
   * Escapes a SQL string. Escapes single quotes with two single quotes.
   *
   * @param string the string
   * @return the escaped string.
   */
  @VisibleForTesting
  static String escapeSqlString(String string) {
    return string.replaceAll("'", "''");
  }

  /**
   * Gets a KML Point value representing a location.
   *
   * @param location the location
   * @return the KML Point value.
   */
  public static String getKmlPoint(Location location) {
    StringBuilder builder = new StringBuilder("<Point><coordinates>");
    if (location != null) {
      appendLocation(location, builder);
    }
    builder.append("</coordinates></Point>");
    return builder.toString();
  }

  /**
   * Gets a KML LineString value representing an array of locations.
   *
   * @param locations the locations.
   * @return the KML LineString value.
   */
  public static String getKmlLineString(ArrayList<Location> locations) {
    StringBuilder builder = new StringBuilder("<LineString><coordinates>");
    if (locations != null) {
      for (int i = 0; i < locations.size(); i++) {
        if (i != 0) {
          builder.append(' ');
        }
        appendLocation(locations.get(i), builder);
      }
    }
    builder.append("</coordinates></LineString>");
    return builder.toString();
  }

  /**
   * Appends a location to a string builder using "longitude,latitude[,altitude]" format.
   *
   * @param location the location
   * @param builder the string builder
   */
  @VisibleForTesting
  static void appendLocation(Location location, StringBuilder builder) {
    builder.append(location.getLongitude()).append(",").append(location.getLatitude());
    if (location.hasAltitude()) {
      builder.append(",");
      builder.append(location.getAltitude());
    }
  }
}
