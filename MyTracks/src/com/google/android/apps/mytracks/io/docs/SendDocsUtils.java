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
package com.google.android.apps.mytracks.io.docs;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.gdata.docs.DocumentsClient;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient.WorksheetEntry;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ResourceUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilities for sending a track to Google Docs.
 *
 * @author Sandor Dornbush
 * @author Matthew Simmons
 */
public class SendDocsUtils {
  private static final String GET_SPREADSHEET_BY_TITLE_URI =
      "https://docs.google.com/feeds/documents/private/full?"
      + "category=mine,spreadsheet&title=%s&title-exact=true";
  private static final String CREATE_SPREADSHEET_URI =
    "https://docs.google.com/feeds/documents/private/full";
  private static final String GET_WORKSHEETS_URI =
    "https://spreadsheets.google.com/feeds/worksheets/%s/private/full";
  private static final String GET_WORKSHEET_URI =
    "https://spreadsheets.google.com/feeds/list/%s/%s/private/full";

  private static final String SPREADSHEET_ID_PREFIX =
      "https://docs.google.com/feeds/documents/private/full/spreadsheet%3A";

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String ATOM_FEED_MIME_TYPE = "application/atom+xml";
  private static final String OPENDOCUMENT_SPREADSHEET_MIME_TYPE =
      "application/x-vnd.oasis.opendocument.spreadsheet";

  private static final String AUTHORIZATION = "Authorization";
  private static final String AUTHORIZATION_PREFIX = "GoogleLogin auth=";

  private static final String SLUG = "Slug";

  // Google Docs can only parse numbers in the English locale.
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);
  private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(
      Locale.ENGLISH);

  static {
    NUMBER_FORMAT.setMaximumFractionDigits(2);
    NUMBER_FORMAT.setMinimumFractionDigits(2);
  }

  private static final String TAG = SendDocsUtils.class.getSimpleName();

  private SendDocsUtils() {}

  /**
   * Gets the spreadsheet id of a spreadsheet. Returns null if the spreadsheet
   * doesn't exist.
   *
   * @param title the title of the spreadsheet
   * @param documentsClient the documents client
   * @param authToken the auth token
   * @return spreadsheet id or null if it doesn't exist.
   */
  public static String getSpreadsheetId(
      String title, DocumentsClient documentsClient, String authToken)
      throws IOException, ParseException, HttpException {
    GDataParser gDataParser = null;
    try {
      String uri = String.format(GET_SPREADSHEET_BY_TITLE_URI, URLEncoder.encode(title));
      gDataParser = documentsClient.getParserForFeed(Entry.class, uri, authToken);
      gDataParser.init();

      while (gDataParser.hasMoreData()) {
        Entry entry = gDataParser.readNextEntry(null);
        String entryTitle = entry.getTitle();
        if (entryTitle.equals(title)) {
          return getEntryId(entry);
        }
      }
      return null;
    } finally {
      if (gDataParser != null) {
        gDataParser.close();
      }
    }
  }

  /**
   * Gets the id from an entry. Returns null if not available.
   * 
   * @param entry the entry
   */
  @VisibleForTesting
  static String getEntryId(Entry entry) {
    String entryId = entry.getId();
    if (entryId.startsWith(SPREADSHEET_ID_PREFIX)) {
      return entryId.substring(SPREADSHEET_ID_PREFIX.length());
    }
    return null;
  }

  /**
   * Creates a new spreadsheet with the given title. Returns the spreadsheet ID
   * if successful. Returns null otherwise. Note that it is possible that a new
   * spreadsheet is created, but the returned ID is null.
   *
   * @param title the title
   * @param authToken the auth token
   * @param context the context
   */
  public static String createSpreadsheet(String title, String authToken, Context context)
      throws IOException {
    URL url = new URL(CREATE_SPREADSHEET_URI);
    URLConnection conn = url.openConnection();
    conn.addRequestProperty(CONTENT_TYPE, OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
    conn.addRequestProperty(SLUG, title);
    conn.addRequestProperty(AUTHORIZATION, AUTHORIZATION_PREFIX + authToken);
    conn.setDoOutput(true);
    OutputStream outputStream = conn.getOutputStream();
    ResourceUtils.readBinaryFileToOutputStream(
        context, R.raw.mytracks_empty_spreadsheet, outputStream);

    // Get the response
    BufferedReader bufferedReader = null;
    StringBuilder resultBuilder = new StringBuilder();
    try {
      bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        resultBuilder.append(line);
      }
    } catch (FileNotFoundException e) {
      // The GData API sometimes throws an error, even though creation of
      // the document succeeded. In that case let's just return. The caller
      // then needs to check if the doc actually exists.
      Log.d(TAG, "Unable to read result after creating a spreadsheet", e);
      return null;
    } finally {
      outputStream.close();
      if (bufferedReader != null) {
        bufferedReader.close();
      }
    }
    return getNewSpreadsheetId(resultBuilder.toString());
  }

  /**
   * Gets the spreadsheet id from a create spreadsheet result.
   *
   * @param result the create spreadsheet result
   */
  @VisibleForTesting
  static String getNewSpreadsheetId(String result) {
    int idTagIndex = result.indexOf("<id>");
    if (idTagIndex == -1) {
      return null;
    }

    int idTagCloseIndex = result.indexOf("</id>", idTagIndex);
    if (idTagCloseIndex == -1) {
      return null;
    }

    int idStringStart = result.indexOf(SPREADSHEET_ID_PREFIX, idTagIndex);
    if (idStringStart == -1) {
      return null;
    }

    return result.substring(idStringStart + SPREADSHEET_ID_PREFIX.length(), idTagCloseIndex);
  }

  /**
   * Gets the first worksheet ID of a spreadsheet. Returns null if not
   * available.
   *
   * @param spreadsheetId the spreadsheet ID
   * @param spreadsheetClient the spreadsheet client
   * @param authToken the auth token
   */
  public static String getWorksheetId(
      String spreadsheetId, SpreadsheetsClient spreadsheetClient, String authToken)
      throws IOException, AuthenticationException, ParseException {
    GDataParser gDataParser = null;
    try {
      String uri = String.format(GET_WORKSHEETS_URI, spreadsheetId);
      gDataParser = spreadsheetClient.getParserForWorksheetsFeed(uri, authToken);
      gDataParser.init();
      if (!gDataParser.hasMoreData()) {
        Log.d(TAG, "No worksheet");
        return null;
      }

      // Get the first worksheet
      WorksheetEntry worksheetEntry =
          (WorksheetEntry) gDataParser.readNextEntry(new WorksheetEntry());
      return getWorksheetEntryId(worksheetEntry);
    } finally {
      if (gDataParser != null) {
        gDataParser.close();
      }
    }
  }

  /**
   * Gets the worksheet id from a worksheet entry. Returns null if not available.
   * 
   * @param entry the worksheet entry
   */
  @VisibleForTesting
  static String getWorksheetEntryId(WorksheetEntry entry) {
    String id = entry.getId();
    int lastSlash = id.lastIndexOf('/');
    if (lastSlash == -1) {
      Log.d(TAG, "No id");
      return null;
    }
    return id.substring(lastSlash + 1);
  }

  /**
   * Adds a track's info as a row in a worksheet.
   *
   * @param track the track
   * @param spreadsheetId the spreadsheet ID
   * @param worksheetId the worksheet ID
   * @param authToken the auth token
   * @param context the context
   */
  public static void addTrackInfo(
      Track track, String spreadsheetId, String worksheetId, String authToken, Context context)
      throws IOException {
    String worksheetUri = String.format(GET_WORKSHEET_URI, spreadsheetId, worksheetId);
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    boolean metricUnits = prefs.getBoolean(context.getString(R.string.metric_units_key), true);

    addRow(worksheetUri, getRowContent(track, metricUnits, context), authToken);
  }

  /**
   * Gets the row content containing the track's info.
   *
   * @param track the track
   * @param metricUnits true to use metric
   * @param context the context
   */
  @VisibleForTesting
  static String getRowContent(Track track, boolean metricUnits, Context context) {
    TripStatistics stats = track.getStatistics();

    String distanceUnit = context.getString(
        metricUnits ? R.string.unit_kilometer : R.string.unit_mile);
    String speedUnit = context.getString(
        metricUnits ? R.string.unit_kilometer_per_hour : R.string.unit_mile_per_hour);
    String elevationUnit = context.getString(
        metricUnits ? R.string.unit_meter : R.string.unit_feet);

    StringBuilder builder = new StringBuilder().append("<entry xmlns='http://www.w3.org/2005/Atom' "
        + "xmlns:gsx='http://schemas.google.com/spreadsheets/2006/extended'>");
    appendTag(builder, "name", track.getName());
    appendTag(builder, "description", track.getDescription());
    appendTag(builder, "date", StringUtils.formatDateTime(context, stats.getStartTime()));
    appendTag(builder, "totaltime", StringUtils.formatElapsedTimeWithHour(stats.getTotalTime()));
    appendTag(builder, "movingtime", StringUtils.formatElapsedTimeWithHour(stats.getMovingTime()));
    appendTag(builder, "distance", getDistance(stats.getTotalDistance(), metricUnits));
    appendTag(builder, "distanceunit", distanceUnit);
    appendTag(builder, "averagespeed", getSpeed(stats.getAverageSpeed(), metricUnits));
    appendTag(builder, "averagemovingspeed", getSpeed(stats.getAverageMovingSpeed(), metricUnits));
    appendTag(builder, "maxspeed", getSpeed(stats.getMaxSpeed(), metricUnits));
    appendTag(builder, "speedunit", speedUnit);
    appendTag(builder, "elevationgain", getElevation(stats.getTotalElevationGain(), metricUnits));
    appendTag(builder, "minelevation", getElevation(stats.getMinElevation(), metricUnits));
    appendTag(builder, "maxelevation", getElevation(stats.getMaxElevation(), metricUnits));
    appendTag(builder, "elevationunit", elevationUnit);

    if (track.getMapId().length() > 0) {
      appendTag(builder, "map",
          String.format("%s?msa=0&msid=%s", Constants.MAPSHOP_BASE_URL, track.getMapId()));
    }

    builder.append("</entry>");
    return builder.toString();
  }

  /**
   * Appends a name-value pair as a gsx tag to a string builder.
   *
   * @param stringBuilder the string builder
   * @param name the name
   * @param value the value
   */
  @VisibleForTesting
  static void appendTag(StringBuilder stringBuilder, String name, String value) {
    stringBuilder
        .append("<gsx:")
        .append(name)
        .append(">")
        .append(StringUtils.formatCData(value))
        .append("</gsx:")
        .append(name)
        .append(">");
  }

  /**
   * Gets the distance. Performs unit conversion and formatting.
   *
   * @param distanceInMeter the distance in meters
   * @param metricUnits true to use metric
   */
  @VisibleForTesting
  static final String getDistance(double distanceInMeter, boolean metricUnits) {
    double distanceInKilometer = distanceInMeter * UnitConversions.M_TO_KM;
    double distance = metricUnits ? distanceInKilometer
        : distanceInKilometer * UnitConversions.KM_TO_MI;
    return NUMBER_FORMAT.format(distance);
  }

  /**
   * Gets the speed. Performs unit conversion and formatting.
   *
   * @param speedInMeterPerSecond the speed in meters per second
   * @param metricUnits true to use metric
   */
  @VisibleForTesting
  static final String getSpeed(double speedInMeterPerSecond, boolean metricUnits) {
    double speedInKilometerPerHour = speedInMeterPerSecond * UnitConversions.MS_TO_KMH;
    double speed = metricUnits ? speedInKilometerPerHour
        : speedInKilometerPerHour * UnitConversions.KM_TO_MI;
    return NUMBER_FORMAT.format(speed);
  }

  /**
   * Gets the elevation. Performs unit conversion and formatting.
   *
   * @param elevationInMeter the elevation value in meters
   * @param metricUnits true to use metric
   */
  @VisibleForTesting
  static final String getElevation(double elevationInMeter, boolean metricUnits) {
    double elevation = metricUnits ? elevationInMeter : elevationInMeter * UnitConversions.M_TO_FT;
    return INTEGER_FORMAT.format(elevation);
  }

  /**
   * Adds a row to a Google Spreadsheet worksheet.
   *
   * @param worksheetUri the worksheet URI
   * @param rowContent the row content
   * @param authToken the auth token
   */
  private static final void addRow(String worksheetUri, String rowContent, String authToken)
      throws IOException {
    URL url = new URL(worksheetUri);
    URLConnection conn = url.openConnection();
    conn.addRequestProperty(CONTENT_TYPE, ATOM_FEED_MIME_TYPE);
    conn.addRequestProperty(AUTHORIZATION, AUTHORIZATION_PREFIX + authToken);
    conn.setDoOutput(true);
    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
    writer.write(rowContent);
    writer.flush();

    // Get the response
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    while ((reader.readLine()) != null) {
      // Just read till the end
    }
    writer.close();
    reader.close();
  }
}
