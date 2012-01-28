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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.docs.DocumentsClient;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient;
import com.google.android.apps.mytracks.io.gdata.docs.XmlDocsGDataParserFactory;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * AsyncTask to send a track to Google Docs.
 *
 * @author jshih@google.com (Jimmy Shih)
 */
public class SendDocsAsyncTask extends AsyncTask<Void, Integer, Boolean> {
  private static final int PROGRESS_GET_SPREADSHEET_ID = 0;
  private static final int PROGRESS_CREATE_SPREADSHEET = 25;
  private static final int PROGRESS_GET_WORKSHEET_ID = 50;
  private static final int PROGRESS_ADD_TRACK_INFO = 75;
  private static final int PROGRESS_COMPLETE = 100;

  private static final String TAG = SendDocsAsyncTask.class.getSimpleName();

  private SendDocsActivity activity;
  private final Account account;
  private final long trackId;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;
  private final GDataClient gDataClient;
  private final DocumentsClient documentsClient;
  private final SpreadsheetsClient spreadsheetsClient;
  
  /**
   * True if can retry sending to Google Docs.
   */
  private boolean canRetry;

  /**
   * True if the AsyncTask has completed.
   */
  private boolean completed;

  /**
   * True if the result is success.
   */
  private boolean success;
  
  // The following variables are for per upload states
  private String documentsAuthToken;
  private String spreadsheetsAuthToken;
  private String spreadsheetId;
  private String worksheetId;

  public SendDocsAsyncTask(SendDocsActivity activity, Account account, long trackId) {
    this.activity = activity;
    this.account = account;
    this.trackId = trackId;

    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    gDataClient = GDataClientFactory.getGDataClient(context);
    documentsClient = new DocumentsClient(
        gDataClient, new XmlDocsGDataParserFactory(new AndroidXmlParserFactory()));
    spreadsheetsClient = new SpreadsheetsClient(
        gDataClient, new XmlDocsGDataParserFactory(new AndroidXmlParserFactory()));
      
    canRetry = true;
    completed = false;
    success = false;
  }

  /**
   * Sets the activity associated with this AyncTask.
   *
   * @param activity the activity.
   */
  public void setActivity(SendDocsActivity activity) {
    this.activity = activity;
    if (completed && activity != null) {
      activity.onAsyncTaskCompleted(success);
    }
  }

  @Override
  protected void onPreExecute() {
    activity.showProgressDialog();
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    return doUpload();
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    if (activity != null) {
      activity.setProgressDialogValue(values[0]);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    closeClient();
    success = result;
    completed = true;
    if (activity != null) {
      activity.onAsyncTaskCompleted(success);
    }
  }

  @Override
  protected void onCancelled() {
    closeClient();
  }

  private void closeClient() {
    if (gDataClient != null) {
      gDataClient.close();
    } 
  }

  /**
   * Uploads a track to Google Docs.
   *
   * @return true if success.
   */
  private boolean doUpload() {
    // Reset the per upload states
    documentsAuthToken = null;
    spreadsheetsAuthToken = null;
    spreadsheetId = null;
    worksheetId = null;

    try {
      documentsAuthToken = AccountManager.get(context).blockingGetAuthToken(
          account, documentsClient.getServiceName(), false);
      spreadsheetsAuthToken = AccountManager.get(context).blockingGetAuthToken(
          account, spreadsheetsClient.getServiceName(), false);
    } catch (OperationCanceledException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (AuthenticatorException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    }

    Track track = myTracksProviderUtils.getTrack(trackId);
    if (track == null) {
      Log.d(TAG, "Track is null");
      return false;
    }

    String title = context.getString(R.string.my_tracks_app_name);
    if (track.getCategory() != null && !track.getCategory().equals("")) {
      title += "-" + track.getCategory();
    }

    // Get the spreadsheet ID
    publishProgress(PROGRESS_GET_SPREADSHEET_ID);
    if (!fetchSpreadSheetId(title, false)) {
      return retryUpload(); 
    }

    // Create a new spreadsheet if necessary
    publishProgress(PROGRESS_CREATE_SPREADSHEET);
    if (spreadsheetId == null) {
      if (!createSpreadSheet(title)) {
        Log.d(TAG, "Unable to create a new spreadsheet");
        return false;
      }

      // The previous creation might have succeeded even though GData
      // reported an error. Seems to be a know bug.
      // See http://code.google.com/p/gdata-issues/issues/detail?id=929
      // Try to find the created spreadsheet.
      if (spreadsheetId == null) {
        if (!fetchSpreadSheetId(title, true)) {
          Log.d(TAG, "Unable to check if the new spreadsheet is created");
          return false;
        }

        if (spreadsheetId == null) {
          Log.d(TAG, "Unable to create a new spreadsheet");
          return false;
        }
      }
    }

    // Get the worksheet ID
    publishProgress(PROGRESS_GET_WORKSHEET_ID);
    if (!fetchWorksheetId()) {
      return retryUpload();
    }
    if (worksheetId == null) {
      Log.d(TAG, "Unable to get a worksheet ID");
      return false;
    }

    // Add the track info
    publishProgress(PROGRESS_ADD_TRACK_INFO);
    if (!addTrackInfo(track)) {
      Log.d(TAG, "Unable to add track info");
      return false;
    }

    publishProgress(PROGRESS_COMPLETE);
    return true;
  }

  /**
   * Fetches the spreadsheet id. Sets the instance variable
   * {@link SendDocsAsyncTask#spreadsheetId}.
   *
   * @param title the spreadsheet title
   * @param waitFirst wait before checking
   * @return true if completes.
   */
  private boolean fetchSpreadSheetId(String title, boolean waitFirst) {
    if (isCancelled()) {
      return false;
    }

    if (waitFirst) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        Log.d(TAG, "Unable to wait", e);
        return false;
      }
    }

    try {
      spreadsheetId = SendDocsUtils.getSpreadsheetId(title, documentsClient, documentsAuthToken);
    } catch (ParseException e) {
      Log.d(TAG, "Unable to fetch spreadsheet ID", e);
      return false;
    } catch (HttpException e) {
      Log.d(TAG, "Unable to fetch spreadsheet ID", e);
      return false;
    } catch (IOException e) {
      Log.d(TAG, "Unable to fetch spreadsheet ID", e);
      return false;
    }

    if (spreadsheetId == null) {
      // Waiting a few seconds and trying again. Maybe the server just had a
      // hickup (unfortunately that happens quite a lot...).
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        Log.d(TAG, "Unable to wait", e);
        return false;
      }

      try {
        spreadsheetId = SendDocsUtils.getSpreadsheetId(title, documentsClient, documentsAuthToken);
      } catch (ParseException e) {
        Log.d(TAG, "Unable to fetch spreadsheet ID", e);
        return false;
      } catch (HttpException e) {
        Log.d(TAG, "Unable to fetch spreadsheet ID", e);
        return false;
      } catch (IOException e) {
        Log.d(TAG, "Unable to fetch spreadsheet ID", e);
        return false;
      }
    }
    return true;
  }

  /**
   * Creates a spreadsheet. If successful, sets the instance variable
   * {@link SendDocsAsyncTask#spreadsheetId}.
   *
   * @param spreadsheetTitle the spreadsheet title
   * @return true if completes.
   */
  private boolean createSpreadSheet(String spreadsheetTitle) {
    if (isCancelled()) {
      return false;
    }
    try {
      spreadsheetId = SendDocsUtils.createSpreadsheet(spreadsheetTitle, documentsAuthToken, context);
    } catch (IOException e) {
      Log.d(TAG, "Unable to create spreadsheet", e);
      return false;
    }
    return true;
  }

  /**
   * Fetches the worksheet ID. Sets the instance variable
   * {@link SendDocsAsyncTask#worksheetId}.
   *
   * @return true if completes.
   */
  private boolean fetchWorksheetId() {
    if (isCancelled()) {
      return false;
    }
    try {
      worksheetId = SendDocsUtils.getWorksheetId(
          spreadsheetId, spreadsheetsClient, spreadsheetsAuthToken);
    } catch (IOException e) {
      Log.d(TAG, "Unable to fetch worksheet ID", e);
      return false;
    } catch (AuthenticationException e) {
      Log.d(TAG, "Unable to fetch worksheet ID", e);
      return false;
    } catch (ParseException e) {
      Log.d(TAG, "Unable to fetch worksheet ID", e);
      return false;
    }
    return true;
  }

  /**
   * Adds track info to a worksheet.
   * 
   * @param track the track
   * @return true if completes.
   */
  private boolean addTrackInfo(Track track) {
    if (isCancelled()) {
      return false;
    }
    try {
      SendDocsUtils.addTrackInfo(track, spreadsheetId, worksheetId, spreadsheetsAuthToken, context);
    } catch (IOException e) {
      Log.d(TAG, "Unable to add track info", e);
      return false;
    }
    return true;
  }

  /**
   * Retries upload. Invalidates the authToken. If can retry, invokes
   * {@link SendDocsAsyncTask#doUpload()}. Returns false if cannot retry.
   */
  private boolean retryUpload() {
    if (isCancelled()) {
      return false;
    }

    AccountManager.get(context).invalidateAuthToken(
        documentsClient.getServiceName(), documentsAuthToken);
    AccountManager.get(context).invalidateAuthToken(
        spreadsheetsClient.getServiceName(), spreadsheetsAuthToken);
    if (canRetry) {
      canRetry = false;
      return doUpload();
    }
    return false;
  }
}
