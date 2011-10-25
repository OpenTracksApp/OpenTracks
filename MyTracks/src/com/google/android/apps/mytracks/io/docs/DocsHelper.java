/*
 * Copyright 2010 Google Inc.
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
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.QueryFunction;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ResourceUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient.WorksheetEntry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class contains helper methods for interacting with Google Docs and
 * Google Spreadsheets.
 *
 * @author Sandor Dornbush
 * @author Matthew Simmons
 */
public class DocsHelper {
  private static final String DOCS_FEED_URL =
    "https://docs.google.com/feeds/documents/private/full";
  private static final String DOCS_SPREADSHEET_URL =
      "https://docs.google.com/feeds/documents/private/full/spreadsheet%3A";
  private static final String DOCS_WORKSHEETS_URL_FORMAT =
    "https://spreadsheets.google.com/feeds/worksheets/%s/private/full";
  private static final String DOCS_MY_SPREADSHEETS_FEED_URL =
    "https://docs.google.com/feeds/documents/private/full?category=mine,spreadsheet";
  private static final String DOCS_SPREADSHEET_URL_FORMAT =
    "https://spreadsheets.google.com/feeds/list/%s/%s/private/full";

  private static final String CONTENT_TYPE_PARAM = "Content-Type";
  private static final String OPENDOCUMENT_SPREADSHEET_MIME_TYPE =
      "application/x-vnd.oasis.opendocument.spreadsheet";
  private static final String ATOM_FEED_MIME_TYPE = "application/atom+xml";

  /**
   * Creates a new MyTracks spreadsheet with the given name.
   *
   * @param context The context associated with this request.
   * @param docListWrapper The GData handle for the Document List service.
   * @param name The name for the newly-created spreadsheet.
   * @return The spreadsheet ID, if one is created.  {@code null} will be
   *     returned if a GData error didn't occur, but no spreadsheet ID was
   *     returned.
   */
  public String createSpreadsheet(final Context context,
      final GDataWrapper<GDataServiceClient> docListWrapper, final String name) throws IOException {
    final AtomicReference<String> idSaver = new AtomicReference<String>();

    boolean success = docListWrapper.runQuery(new QueryFunction<GDataServiceClient>() {
      @Override
      public void query(GDataServiceClient client) throws IOException,
          GDataWrapper.AuthenticationException {
        // Construct and send request
        URL url = new URL(DOCS_FEED_URL);
        URLConnection conn = url.openConnection();
        conn.addRequestProperty(CONTENT_TYPE_PARAM,
            OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
        conn.addRequestProperty("Slug", name);
        conn.addRequestProperty("Authorization",
            "GoogleLogin auth=" +
            docListWrapper.getAuthManager().getAuthToken());
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        ResourceUtils.readBinaryFileToOutputStream(
            context, R.raw.mytracks_empty_spreadsheet, os);

        // Get the response
        // TODO: The following is a horrible ugly hack.
        //       Hopefully we can retire it when there is a proper gdata api.
        BufferedReader rd = null;
        String line;
        StringBuilder resultBuilder = new StringBuilder();
        try {
          rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          while ((line = rd.readLine()) != null) {
            resultBuilder.append(line);
          }
          os.close();
          rd.close();
        } catch (FileNotFoundException e) {
          // The GData API sometimes throws an error, even though creation of
          // the document succeeded. In that case let's just return. The caller
          // then needs to check if the doc actually exists.
          return;
        } finally {
          os.close();
          if (rd != null) {
            rd.close();
          }
        }

        String result = resultBuilder.toString();
        // Try to find the id.
        int idTagIndex = result.indexOf("<id>");
        if (idTagIndex == -1) {
          return;
        }
        int idTagCloseIndex = result.indexOf("</id>", idTagIndex);
        if (idTagCloseIndex == -1) {
          return;
        }
        int idStringStart = result.indexOf(DOCS_SPREADSHEET_URL, idTagIndex);
        if (idStringStart == -1) {
          return;
        }

        String id = result.substring(
            idStringStart + DOCS_SPREADSHEET_URL.length(), idTagCloseIndex);
        Log.i(Constants.TAG, "Created new spreadsheet: " + id);
        idSaver.set(id);
      }});

    if (!success) {
      throw newIOException(docListWrapper,
          "Failed to create new spreadsheet.");
    }

    return idSaver.get();
  }

  /**
   * Retrieve the ID of a spreadsheet with the given name.
   *
   * @param docListWrapper The GData handle for the Document List service.
   * @param title The name of the spreadsheet whose ID is to be retrieved.
   * @return The spreadsheet ID, if it can be retrieved.  {@code null} will
   *     be returned if no spreadsheet exists by the given name.
   * @throws IOException If an error occurs during the GData request.
   */
  public String requestSpreadsheetId(final GDataWrapper<GDataServiceClient> docListWrapper,
      final String title) throws IOException {
    final AtomicReference<String> idSaver = new AtomicReference<String>();

    boolean result = docListWrapper.runQuery(new QueryFunction<GDataServiceClient>() {
      @Override
      public void query(GDataServiceClient client)
          throws IOException, GDataWrapper.ParseException, GDataWrapper.HttpException,
          GDataWrapper.AuthenticationException {
        GDataParser listParser;
        try {
          listParser = client.getParserForFeed(Entry.class,
              DOCS_MY_SPREADSHEETS_FEED_URL,
              docListWrapper.getAuthManager().getAuthToken());
          listParser.init();

          while (listParser.hasMoreData()) {
            Entry entry = listParser.readNextEntry(null);
            String entryTitle = entry.getTitle();
            Log.i(Constants.TAG, "Found docs entry: " + entryTitle);
            if (entryTitle.equals(title)) {
              String entryId = entry.getId();
              int lastSlash = entryId.lastIndexOf('/');
              idSaver.set(entryId.substring(lastSlash + 15));
              break;
            }
          }
        } catch (ParseException e) {
          throw new GDataWrapper.ParseException(e);
        } catch (HttpException e) {
          throw new GDataWrapper.HttpException(e.getStatusCode(), e.getMessage());
        }
      }
    });

    if (!result) {
      throw newIOException(docListWrapper,
          "Failed to retrieve spreadsheet list.");
    }

    return idSaver.get();
  }

  /**
   * Retrieve the ID of the first worksheet in the named spreadsheet.
   *
   * @param trixWrapper The GData handle for the spreadsheet service.
   * @param spreadsheetId The GData ID for the given spreadsheet.
   * @return The worksheet ID, if it can be retrieved.  {@code null} will be
   *     returned if the GData request returns without error, but without an
   *     ID.
   * @throws IOException If an error occurs during the GData request.
   */
  public String getWorksheetId(final GDataWrapper<GDataServiceClient> trixWrapper,
      final String spreadsheetId) throws IOException {
    final AtomicReference<String> idSaver = new AtomicReference<String>();

    boolean result = trixWrapper.runQuery(new QueryFunction<GDataServiceClient>() {
      @Override
      public void query(GDataServiceClient client)
          throws GDataWrapper.AuthenticationException, IOException, GDataWrapper.ParseException, GDataWrapper.HttpException {
        String uri = String.format(DOCS_WORKSHEETS_URL_FORMAT, spreadsheetId);
        GDataParser sheetParser;
        try {
          sheetParser = ((SpreadsheetsClient) client).getParserForWorksheetsFeed(uri,
              trixWrapper.getAuthManager().getAuthToken());
          sheetParser.init();
          if (!sheetParser.hasMoreData()) {
            Log.i(Constants.TAG, "Found no worksheets");
            return;
          }

          // Grab the first worksheet.
          WorksheetEntry worksheetEntry =
              (WorksheetEntry) sheetParser.readNextEntry(new WorksheetEntry());

          int lastSlash = worksheetEntry.getId().lastIndexOf('/');
          idSaver.set(worksheetEntry.getId().substring(lastSlash + 1));
        } catch (ParseException e) {
          throw new GDataWrapper.ParseException(e);
        } catch (AuthenticationException e) {
          throw new GDataWrapper.AuthenticationException(e);
        }
      }
    });

    if (!result) {
      throw newIOException(trixWrapper, "Failed to retrieve worksheet ID.");
    }

    return idSaver.get();
  }

  /**
   * Add a row to a worksheet containing the stats for a given track.
   *
   * @param context The context associated with this request.
   * @param trixAuth The GData authorization for the spreadsheet service.
   * @param spreadsheetId The spreadsheet to be modified.
   * @param worksheetId The worksheet to be modified.
   * @param track The track whose stats are to be written.
   * @param metricUnits True if metric units are to be used.  If false,
   *     imperial units will be used.
   * @throws IOException If an error occurs while updating the worksheet.
   */
  public void addTrackRow(Context context, AuthManager trixAuth,
      String spreadsheetId, String worksheetId, Track track,
      boolean metricUnits) throws IOException {
    String worksheetUri = String.format(DOCS_SPREADSHEET_URL_FORMAT,
        spreadsheetId, worksheetId);
    TripStatistics stats = track.getStatistics();

    String distanceUnit = context.getString(metricUnits ?
        R.string.kilometer : R.string.mile);
    String speedUnit = context.getString(metricUnits ?
        R.string.kilometer_per_hour : R.string.mile_per_hour);
    String elevationUnit = context.getString(metricUnits ?
        R.string.meter : R.string.feet);

    // Prepare the Post-Text we are going to send.
    DocsTagBuilder tagBuilder = new DocsTagBuilder(metricUnits)
        .append("name", track.getName())
        .append("description", track.getDescription())
        .append("date", getDisplayDate(context, stats.getStartTime()))
        .append("totaltime", StringUtils.formatTimeAlwaysShowingHours(
            stats.getTotalTime()))
        .append("movingtime", StringUtils.formatTimeAlwaysShowingHours(
            stats.getMovingTime()))
        .appendLargeUnits("distance", stats.getTotalDistance() / 1000)
        .append("distanceunit", distanceUnit)
        .appendLargeUnits("averagespeed", stats.getAverageSpeed() * 3.6)
        .appendLargeUnits("averagemovingspeed",
            stats.getAverageMovingSpeed() * 3.6)
        .appendLargeUnits("maxspeed", stats.getMaxSpeed() * 3.6)
        .append("speedunit", speedUnit)
        .appendSmallUnits("elevationgain", stats.getTotalElevationGain())
        .appendSmallUnits("minelevation", stats.getMinElevation())
        .appendSmallUnits("maxelevation", stats.getMaxElevation())
        .append("elevationunit", elevationUnit);

    if (track.getMapId().length() > 0) {
      tagBuilder.append("map", String.format("%s?msa=0&msid=%s",
          Constants.MAPSHOP_BASE_URL, track.getMapId()));
    }

    String postText = new StringBuilder()
        .append("<entry xmlns='http://www.w3.org/2005/Atom' "
            + "xmlns:gsx='http://schemas.google.com/spreadsheets/"
            + "2006/extended'>")
        .append(tagBuilder.build())
        .append("</entry>")
        .toString();

    Log.i(Constants.TAG,
        "Inserting at: " + spreadsheetId + " => " + worksheetUri);

    Log.i(Constants.TAG, postText);

    writeRowData(trixAuth, worksheetUri, postText);

    Log.i(Constants.TAG, "Post finished.");
  }

  /**
   * Gets the display string for a time based on the phone's setting.
   *
   * @param context the context to obtain the phone's setting.
   * @param time the time
   * @return the display string of the time
   */
  protected String getDisplayDate(Context context, long time) {
    java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
    java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
    Date startTime = new Date(time);
    String dateString = dateFormat.format(startTime) + " " + timeFormat.format(startTime);
    return dateString;
  }

  /**
   * Writes spreadsheet row data to the indicated worksheet.
   *
   * @param trixAuth The GData authorization for the spreadsheet service.
   * @param worksheetUri The URI of the worksheet to be altered.
   * @param postText The XML tags describing the change to be made.
   * @throws IOException Thrown if an error occurs during the write.
   */
  protected void writeRowData(AuthManager trixAuth, String worksheetUri,
      String postText) throws IOException {
    // No need for a wrapper because we know that the authorization was good
    // enough to get this far.
    URL url = new URL(worksheetUri);
    URLConnection conn = url.openConnection();
    conn.addRequestProperty(CONTENT_TYPE_PARAM, ATOM_FEED_MIME_TYPE);
    conn.addRequestProperty("Authorization",
        "GoogleLogin auth=" + trixAuth.getAuthToken());
    conn.setDoOutput(true);
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(postText);
    wr.flush();

    // Get the response.
    // TODO: Should we parse the response, rather than simply throwing it away?
    BufferedReader rd =
        new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    while ((line = rd.readLine()) != null) {
      // Process line.
      Log.i(Constants.TAG, "r: " + line);
    }
    wr.close();
    rd.close();
  }

  private static IOException newIOException(GDataWrapper<GDataServiceClient> wrapper,
      String message) {
    return new IOException(String.format("%s: %d: %s", message,
        wrapper.getErrorType(), wrapper.getErrorMessage()));
  }
}
