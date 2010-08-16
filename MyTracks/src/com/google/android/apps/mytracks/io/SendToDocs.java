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
package com.google.android.apps.mytracks.io;

import com.google.android.apps.mymaps.MyMapsConstants;
import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.QueryFunction;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ResourceUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.docs.DocumentsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient.WorksheetEntry;
import com.google.wireless.gdata.maps.xml.XmlMapsGDataParserFactory;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A helper class used to transmit tracks statistics to Google Docs/Trix.
 *
 * @author Sandor Dornbush
 */
public class SendToDocs {
  private static final NumberFormat LARGE_UNIT_FORMAT =
      new DecimalFormat("#,###,###.00");
  private static final NumberFormat SMALL_UNIT_FORMAT =
      new DecimalFormat("###,###");
  private static final String DOCS_FEED_URL =
      "http://docs.google.com/feeds/documents/private/full";
  private static final String DOCS_MY_SPREADSHEETS_FEED_URL =
      "http://docs.google.com/feeds/documents/private/full?"
          + "category=mine,spreadsheet";
  private static final String DOCS_SPREADSHEET_URL =
      "http://docs.google.com/feeds/documents/private/full/spreadsheet%3A";
  private static final String DOCS_SPREADSHEET_URL_FORMAT =
       "http://spreadsheets.google.com/feeds/list/%s/%s/private/full";
  protected static final String DOCS_WORKSHEETS_URL_FORMAT =
      "http://spreadsheets.google.com/feeds/worksheets/%s/private/full";
  private static final String CONTENT_TYPE_PARAM = "Content-Type";
  private static final String ATOM_FEED_MIME_TYPE = "application/atom+xml";
  private static final String OPENDOCUMENT_SPREADSHEET_MIME_TYPE =
      "application/x-vnd.oasis.opendocument.spreadsheet";

  private final Activity activity;
  private final AuthManager wiseAuth;
  private final AuthManager writelyAuth;
  private final long trackId;
  private final boolean metricUnits;
  private final HandlerThread handlerThread;
  private final Handler handler;

  private boolean createdNewSpreadSheet = false;

  private String spreadSheetId = null;
  private String workSheetId = null;

  private boolean success = true;
  private String statusMessage = "";
  private Runnable onCompletion = null;

  public SendToDocs(Activity activity, AuthManager wiseAuth,
      AuthManager writelyAuth, long trackId) {
    this.activity = activity;
    this.wiseAuth = wiseAuth;
    this.writelyAuth = writelyAuth;
    this.trackId = trackId;

    SharedPreferences preferences = activity.getSharedPreferences(
        MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      metricUnits = preferences.getBoolean(MyTracksSettings.METRIC_UNITS, true);
    } else {
      metricUnits = true;
    }

    Log.d(MyTracksConstants.TAG,
        "Sending to Google Docs: trackId = " + trackId);
    handlerThread = new HandlerThread("SendToGoogleDocs");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  public void run() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        doUpload();
      }
    });
  }

  private void doUpload() {
    statusMessage = activity.getString(R.string.error_sending_to_mymap);
    success = false;

    try {
      if (trackId == -1) {
        Log.w(MyTracksConstants.TAG, "Cannot get track id.");
        return;
      }

      // Get the track from the provider:
      Track track =
          MyTracksProviderUtils.Factory.get(activity).getTrack(trackId);
      if (track == null) {
        Log.w(MyTracksConstants.TAG, "Cannot get track.");
        return;
      }

      // Transmit track stats via GData feed:
      // -------------------------------

      Log.d(MyTracksConstants.TAG, "SendToDocs: Uploading to spreadsheet");
      success = uploadToDocs(track);
      if (success) {
        if (createdNewSpreadSheet) {
          statusMessage = activity.getString(
              R.string.status_tracks_have_been_uploaded_to_new_doc);
        } else {
          statusMessage = activity.getString(
              R.string.status_tracks_have_been_uploaded_to_docs);
        }
      } else {
        statusMessage = activity.getString(R.string.error_sending_to_docs);
      }
      Log.d(MyTracksConstants.TAG, "SendToDocs: Done.");
    } finally {
      if (onCompletion != null) {
        activity.runOnUiThread(onCompletion);
      }
    }
  }

  public boolean wasSuccess() {
    return success;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setOnCompletion(Runnable onCompletion) {
    this.onCompletion = onCompletion;
  }

  /**
   * Uploads the statistics about a track to Google Docs using the docs GData
   * feed.
   *
   * @param track the track
   */
  private boolean uploadToDocs(Track track) {
    GDataWrapper wiseWrapper = new GDataWrapper();
    wiseWrapper.setAuthManager(wiseAuth);
    wiseWrapper.setRetryOnAuthFailure(true);

    GDataWrapper writelyWrapper = new GDataWrapper();
    writelyWrapper.setAuthManager(writelyAuth);
    writelyWrapper.setRetryOnAuthFailure(true);

    GDataClient androidClient = null;
    try {
      androidClient = GDataClientFactory.getGDataClient(activity);
      SpreadsheetsClient gdataClient = new SpreadsheetsClient(androidClient,
          new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
      wiseWrapper.setClient(androidClient, gdataClient);
      Log.d(MyTracksConstants.TAG,
          "GData connection prepared: " + this.writelyAuth);
      String sheetTitle = "My Tracks";

      if (track.getCategory() != null && !track.getCategory().equals("")) {
        sheetTitle += "-" + track.getCategory();
      }

      DocumentsClient docsGdataClient = new DocumentsClient(androidClient,
          new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
      writelyWrapper.setClient(androidClient, docsGdataClient);

      // First try to find the spreadsheet:
      if (!getSpreadsheetId(writelyWrapper, sheetTitle)) {
        Log.i(MyTracksConstants.TAG, "Spreadsheet lookup failed.");
        return false;
      }

      if (spreadSheetId == null) {
        MyTracks.getInstance().setProgressValue(65);
        // Waiting a few seconds and trying again. Maybe the server just had a
        // hickup (unfortunately that happens quite a lot...).
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          Log.e(MyTracksConstants.TAG, "Sleep interrupted", e);
        }
        if (!getSpreadsheetId(writelyWrapper, sheetTitle)) {
          Log.i(MyTracksConstants.TAG, "2nd spreadsheet lookup failed.");
          return false;
        }
      }

      MyTracks.getInstance().setProgressValue(70);
      if (spreadSheetId == null) {
        Log.i(MyTracksConstants.TAG, "Creating new spreadsheet: " + sheetTitle);

        if (!createSpreadSheet(writelyWrapper, sheetTitle)) {
          return false;
        }
        MyTracks.getInstance().setProgressValue(80);

        if (spreadSheetId == null) {
          MyTracks.getInstance().setProgressValue(85);
          // The previous creation might have succeeded even though GData
          // reported an error. Seems to be a know bug,
          // see http://code.google.com/p/gdata-issues/issues/detail?id=929
          // Try to find the created spreadsheet:
          Log.w(MyTracksConstants.TAG,
              "Create might have failed. Trying to find created document.");
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            Log.e(MyTracksConstants.TAG, "Sleep interrupted", e);
          }
          if (!getSpreadsheetId(writelyWrapper, sheetTitle)) {
            return false;
          }
          if (spreadSheetId == null) {
            MyTracks.getInstance().setProgressValue(87);
            // Re-try
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              Log.e(MyTracksConstants.TAG, "Sleep interrupted", e);
            }
            if (!getSpreadsheetId(writelyWrapper, sheetTitle)) {
              return false;
            }
          }
          if (spreadSheetId == null) {
            Log.i(MyTracksConstants.TAG,
                "Creating new spreadsheet really failed.");
            return false;
          }
        }
      }

      if (!getWorkSheetId(wiseWrapper)) {
        Log.i(MyTracksConstants.TAG, "Looking up worksheet id failed.");
        return false;
      }

      MyTracks.getInstance().setProgressValue(90);

      insertRowNet(track, spreadSheetId, workSheetId);
      Log.i(MyTracksConstants.TAG, "Done uploading to docs.");
    } catch (IOException e) {
      Log.e(MyTracksConstants.TAG, "Unable to upload docs.", e);
      return false;
    } finally {
      if (androidClient != null) {
        androidClient.close();
      }
    }
    return true;
  }

  private boolean getSpreadsheetId(GDataWrapper wrapper, final String title) {
    spreadSheetId = null;
    return wrapper.runQuery(new QueryFunction() {
      @Override
      public void query(GDataServiceClient client)
          throws IOException, ParseException, HttpException {
        GDataParser listParser;
        listParser = client.getParserForFeed(Entry.class,
            DOCS_MY_SPREADSHEETS_FEED_URL, writelyAuth.getAuthToken());
        listParser.init();
        while (listParser.hasMoreData()) {
          Entry entry = listParser.readNextEntry(null);
          String entryTitle = entry.getTitle();
          Log.i(MyTracksConstants.TAG, "Found docs entry: " + entryTitle);
          if (entryTitle.equals(title)) {
            String entryId = entry.getId();
            int lastSlash = entryId.lastIndexOf('/');
            spreadSheetId = entryId.substring(lastSlash + 15);
            break;
          }
        }
      }
    });
  }

  private boolean getWorkSheetId(GDataWrapper wrapper) {
    workSheetId = null;
    return wrapper.runQuery(new QueryFunction() {
      @Override
      public void query(GDataServiceClient client)
          throws AuthenticationException, IOException, ParseException {
        String uri = String.format(DOCS_WORKSHEETS_URL_FORMAT, spreadSheetId);
        GDataParser sheetParser =
            ((SpreadsheetsClient) client).getParserForWorksheetsFeed(uri,
                wiseAuth.getAuthToken());
        sheetParser.init();
        if (!sheetParser.hasMoreData()) {
          Log.i(MyTracksConstants.TAG, "Found no worksheets");
          return; // failure
        }

        // just grab the first.
        WorksheetEntry worksheetEntry =
            (WorksheetEntry) sheetParser.readNextEntry(new WorksheetEntry());

        int lastSlash = worksheetEntry.getId().lastIndexOf('/');
        workSheetId = worksheetEntry.getId().substring(lastSlash + 1);
      }
    });
  }

  /**
   * Creates a new MyTracks spreadsheet with the given name.
   */
  private boolean createSpreadSheet(GDataWrapper writelyWrapper,
      final String name) {
    spreadSheetId = null;

    return writelyWrapper.runQuery(new QueryFunction() {
      @Override
      public void query(GDataServiceClient client) throws IOException {
        // Construct data
        // Send data
        URL url = new URL(DOCS_FEED_URL);
        URLConnection conn = url.openConnection();
        conn.addRequestProperty(CONTENT_TYPE_PARAM,
            OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
        conn.addRequestProperty("Slug", name);
        conn.addRequestProperty("Authorization",
            "GoogleLogin auth=" + writelyAuth.getAuthToken());
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        ResourceUtils.readBinaryFileToOutputStream(
            activity, R.raw.mytracks_empty_spreadsheet, os);

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
        spreadSheetId = result.substring(
            idStringStart + DOCS_SPREADSHEET_URL.length(), idTagCloseIndex);
        Log.i(MyTracksConstants.TAG, "Created new spreadsheet: " + spreadSheetId);
      }});
  }

  private void insertRowNet(Track track, String spreadsheetId,
      String worksheetId) throws IOException {
    String worksheetUri = String.format(DOCS_SPREADSHEET_URL_FORMAT,
        spreadsheetId, worksheetId);
    TripStatistics stats = track.getStatistics();

    /* Prepare the Post-Text we are going to send. */
    StringBuilder sb = new StringBuilder();
    sb.append("<entry xmlns='http://www.w3.org/2005/Atom' ");
    sb.append("xmlns:gsx='http://schemas.google.com/spreadsheets/"
        + "2006/extended'>");
    appendTag("name", track.getName(), sb);
    appendTag("description", track.getDescription(), sb);
    appendTag("date", String.format("%tc", stats.getStartTime()), sb);
    appendTag("totaltime", StringUtils.formatTimeAlwaysShowingHours(
        stats.getTotalTime()), sb);
    appendTag("movingtime", StringUtils.formatTimeAlwaysShowingHours(
        stats.getMovingTime()), sb);
    appendLargeUnitsTag("distance", stats.getTotalDistance() / 1000, sb);
    appendTag("distanceunit",
        metricUnits
            ? activity.getString(R.string.kilometer)
            : activity.getString(R.string.mile),
        sb);
    appendLargeUnitsTag("averagespeed", stats.getAverageSpeed() * 3.6, sb);
    appendLargeUnitsTag("averagemovingspeed",
        stats.getAverageMovingSpeed() * 3.6, sb);
    appendLargeUnitsTag("maxspeed", stats.getMaxSpeed() * 3.6, sb);
    appendTag("speedunit",
        metricUnits
            ? activity.getString(R.string.kilometer_per_hour)
            : activity.getString(R.string.mile_per_hour),
        sb);
    appendSmallUnitsTag("elevationgain", stats.getTotalElevationGain(), sb);
    appendSmallUnitsTag("minelevation", stats.getMinElevation(), sb);
    appendSmallUnitsTag("maxelevation", stats.getMaxElevation(), sb);
    appendTag("elevationunit",
        metricUnits
        ? activity.getString(R.string.meter)
        : activity.getString(R.string.feet),
        sb);
    if (track.getMapId().length() > 0) {
      appendTag("map", MyMapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid="
          + track.getMapId(), sb);
    }
    sb.append("</entry>");
    Log.i(MyTracksConstants.TAG,
        "Inserting at: " + spreadsheetId + " => " + worksheetUri);

    String postText = sb.toString();
    Log.i(MyTracksConstants.TAG, postText);

    // Send data
    // No need for a wrapper because we know that the authorization was good
    // enough to get this far
    URL url = new URL(worksheetUri);
    URLConnection conn = url.openConnection();
    conn.addRequestProperty(CONTENT_TYPE_PARAM, ATOM_FEED_MIME_TYPE);
    conn.addRequestProperty("Authorization",
        "GoogleLogin auth=" + wiseAuth.getAuthToken());
    conn.setDoOutput(true);
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(postText);
    wr.flush();

    // Get the response
    BufferedReader rd =
        new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    while ((line = rd.readLine()) != null) {
      // Process line...
      Log.i(MyTracksConstants.TAG, "r: " + line);
    }
    wr.close();
    rd.close();

    Log.i(MyTracksConstants.TAG, "Post finished.");
  }

  private void appendTag(String name, String value, StringBuilder sb) {
    sb.append("<gsx:");
    sb.append(name);
    sb.append('>');
    sb.append(StringUtils.stringAsCData(value));
    sb.append("</gsx:");
    sb.append(name);
    sb.append('>');
  }

  private void appendLargeUnitsTag(String name, double d, StringBuilder sb) {
    double value = metricUnits ? d : (d * UnitConversions.KM_TO_MI);
    appendTag(name, LARGE_UNIT_FORMAT.format(value), sb);
  }

  private void appendSmallUnitsTag(String name, double d, StringBuilder sb) {
    double value = metricUnits ? d : (d * UnitConversions.M_TO_FT);
    appendTag(name, SMALL_UNIT_FORMAT.format(value), sb);
  }
}
