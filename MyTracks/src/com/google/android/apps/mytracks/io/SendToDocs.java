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

import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.docs.DocsHelper;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.docs.DocumentsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.maps.xml.XmlMapsGDataParserFactory;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;

/**
 * A helper class used to transmit tracks statistics to Google Docs/Trix.
 *
 * @author Sandor Dornbush
 */
public class SendToDocs {
  /** The GData service name for Google Spreadsheets (aka Trix) */
  public static final String GDATA_SERVICE_NAME_TRIX = "wise";
  
  /** The GData service name for the Google Docs Document List */
  public static final String GDATA_SERVICE_NAME_DOCLIST = "writely";
  
  private final Activity activity;
  private final AuthManager trixAuth;
  private final AuthManager docListAuth;
  private final long trackId;
  private final boolean metricUnits;
  private final HandlerThread handlerThread;
  private final Handler handler;

  private boolean createdNewSpreadSheet = false;

  private boolean success = true;
  private String statusMessage = "";
  private Runnable onCompletion = null;

  public SendToDocs(Activity activity, AuthManager trixAuth,
      AuthManager docListAuth, long trackId) {
    this.activity = activity;
    this.trixAuth = trixAuth;
    this.docListAuth = docListAuth;
    this.trackId = trackId;

    SharedPreferences preferences = activity.getSharedPreferences(
        MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      metricUnits =
          preferences.getBoolean(activity.getString(R.string.metric_units_key),
              true);
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
    GDataWrapper docListWrapper = new GDataWrapper();
    docListWrapper.setAuthManager(docListAuth);
    docListWrapper.setRetryOnAuthFailure(true);
    
    GDataWrapper trixWrapper = new GDataWrapper();
    trixWrapper.setAuthManager(trixAuth);
    trixWrapper.setRetryOnAuthFailure(true);

    DocsHelper docsHelper = new DocsHelper();

    GDataClient androidClient = null;
    try {
      androidClient = GDataClientFactory.getGDataClient(activity);
      SpreadsheetsClient gdataClient = new SpreadsheetsClient(androidClient,
          new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
      trixWrapper.setClient(gdataClient);
      Log.d(MyTracksConstants.TAG,
          "GData connection prepared: " + this.docListAuth);
      String sheetTitle = "My Tracks";

      if (track.getCategory() != null && !track.getCategory().equals("")) {
        sheetTitle += "-" + track.getCategory();
      }

      DocumentsClient docsGdataClient = new DocumentsClient(androidClient,
          new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
      docListWrapper.setClient(docsGdataClient);

      // First try to find the spreadsheet:
      String spreadsheetId = null;
      try {
        spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper, 
            sheetTitle);
      } catch (IOException e) {
        Log.i(MyTracksConstants.TAG, "Spreadsheet lookup failed.", e);
        return false;
      }

      if (spreadsheetId == null) {
        MyTracks.getInstance().setProgressValue(65);
        // Waiting a few seconds and trying again. Maybe the server just had a
        // hickup (unfortunately that happens quite a lot...).
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          Log.e(MyTracksConstants.TAG, "Sleep interrupted", e);
        }
        
        try {
          spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper, 
              sheetTitle);
        } catch (IOException e) {
          Log.i(MyTracksConstants.TAG, "2nd spreadsheet lookup failed.", e);
          return false;
        }
      }
      
      // We were unable to find an existing spreadsheet, so create a new one.
      MyTracks.getInstance().setProgressValue(70);
      if (spreadsheetId == null) {
        Log.i(MyTracksConstants.TAG, "Creating new spreadsheet: " + sheetTitle);

        try {
          spreadsheetId = docsHelper.createSpreadsheet(activity, docListWrapper,
              sheetTitle);
        } catch (IOException e) {
          Log.i(MyTracksConstants.TAG, "Failed to create new spreadsheet "
              + sheetTitle, e);
          return false;
        }
        MyTracks.getInstance().setProgressValue(80);
        createdNewSpreadSheet = true;

        if (spreadsheetId == null) {
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

          try {
            spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper, 
                sheetTitle);
          } catch (IOException e) {
            Log.i(MyTracksConstants.TAG, "Failed create-failed lookup", e);
            return false;
          }

          if (spreadsheetId == null) {
            MyTracks.getInstance().setProgressValue(87);
            // Re-try
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              Log.e(MyTracksConstants.TAG, "Sleep interrupted", e);
            }
            
            try {
              spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper, 
                  sheetTitle);
            } catch (IOException e) {
              Log.i(MyTracksConstants.TAG, "Failed create-failed relookup", e);
              return false;
            }
          }
          if (spreadsheetId == null) {
            Log.i(MyTracksConstants.TAG,
                "Creating new spreadsheet really failed.");
            return false;
          }
        }
      }

      String worksheetId = null;
      try {
        worksheetId = docsHelper.getWorksheetId(trixWrapper, spreadsheetId);
        if (worksheetId == null) {
          throw new IOException("Worksheet ID lookup returned empty");
        }
      } catch (IOException e) {
        Log.i(MyTracksConstants.TAG, "Looking up worksheet id failed.", e);
        return false;
      }

      MyTracks.getInstance().setProgressValue(90);

      docsHelper.addTrackRow(activity, trixAuth, spreadsheetId, worksheetId, 
          track, metricUnits);
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

}
