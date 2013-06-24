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

package com.google.android.apps.mytracks.io.spreadsheets;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendAsyncTask;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * AsyncTask to send a track to Google Spreadsheet.
 * 
 * @author Jimmy Shih
 */
public class SendSpreadsheetsAsyncTask extends AbstractSendAsyncTask {

  private static final String TAG = SendSpreadsheetsAsyncTask.class.getSimpleName();
  private static final String
      GOOGLE_SPREADSHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
  private static final String
      OPENDOCUMENT_SPREADSHEET_MIME_TYPE = "application/x-vnd.oasis.opendocument.spreadsheet";
  @VisibleForTesting
  public static final String GET_SPREADSHEET_QUERY =
      "'root' in parents and title = '%s' and mimeType = '" + GOOGLE_SPREADSHEET_MIME_TYPE
      + "' and trashed = false";
  @VisibleForTesting
  public static final String
      GET_WORKSHEETS_URI = "https://spreadsheets.google.com/feeds/worksheets/%s/private/full";

  private static final int PROGRESS_GET_SPREADSHEET_ID = 0;
  private static final int PROGRESS_GET_WORKSHEET_URL = 35;
  private static final int PROGRESS_ADD_TRACK_INFO = 70;
  private static final int PROGRESS_COMPLETE = 100;

  private final long trackId;
  private final Account account;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;

  public SendSpreadsheetsAsyncTask(SendSpreadsheetsActivity activity, long trackId, Account account) {
    super(activity);
    this.trackId = trackId;
    this.account = account;

    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  protected void closeConnection() {}

  @Override
  protected boolean performTask() {
    try {
      SpreadsheetService spreadsheetService = new SpreadsheetService(
          "MyTracks-" + SystemUtils.getMyTracksVersion(context));
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());

      credential.setAccessToken(
          SendToGoogleUtils.getToken(context, account.name, SendToGoogleUtils.SPREADSHEETS_SCOPE));
      spreadsheetService.setOAuth2Credentials(credential);

      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track == null) {
        Log.d(TAG, "No track for " + trackId);
        return false;
      }

      String title = context.getString(R.string.my_tracks_app_name);
      if (track.getCategory() != null && !track.getCategory().equals("")) {
        title += "-" + track.getCategory();
      }

      publishProgress(PROGRESS_GET_SPREADSHEET_ID);
      String spreadsheetId = getSpreadSheetId(title);
      if (spreadsheetId == null) {
        Log.d(TAG, "Unable to get the spreadsheet ID for " + title);
        return false;
      }

      publishProgress(PROGRESS_GET_WORKSHEET_URL);
      URL worksheetUrl = getWorksheetUrl(spreadsheetService, spreadsheetId);
      if (worksheetUrl == null) {
        Log.d(TAG, "Unable to get the worksheet url for " + spreadsheetId);
        return false;
      }

      publishProgress(PROGRESS_ADD_TRACK_INFO);
      if (!addTrackInfo(spreadsheetService, worksheetUrl, track)) {
        Log.d(TAG, "Unable to add track info");
        return false;
      }

      publishProgress(PROGRESS_COMPLETE);
      return true;
    } catch (UserRecoverableAuthException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEETS_NOTIFICATION_ID);
      return false;
    } catch (GoogleAuthException e) {
      Log.e(TAG, "GoogleaAuthException", e);
      return retryTask();
    } catch (UserRecoverableAuthIOException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.SPREADSHEETS_NOTIFICATION_ID);
      return false;
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
      return retryTask();
    } catch (ServiceException e) {
      Log.e(TAG, "ServiceException", e);
      return retryTask();
    }
  }

  @Override
  protected void invalidateToken() {}

  /**
   * Gets the spreadsheet id.
   * 
   * @param fileName the file name
   */
  private String getSpreadSheetId(String fileName) throws IOException, GoogleAuthException {
    if (isCancelled()) {
      return null;
    }
    GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
        context, account.name, SendToGoogleUtils.DRIVE_SCOPE);
    if (driveCredential == null) {
      return null;
    }
    Drive drive = SyncUtils.getDriveService(driveCredential);
    com.google.api.services.drive.Drive.Files.List list = drive.files()
        .list().setQ(String.format(Locale.US, GET_SPREADSHEET_QUERY, fileName));
    FileList result = list.execute();
    for (File file : result.getItems()) {
      if (file.getSharedWithMeDate() == null) {
        return file.getId();
      }
    }
    if (isCancelled()) {
      return null;
    }
    InputStream inputStream = null;
    try {
      inputStream = context.getResources().openRawResource(R.raw.mytracks_empty_spreadsheet);
      byte[] b = new byte[inputStream.available()];
      inputStream.read(b);
      ByteArrayContent fileContent = new ByteArrayContent(OPENDOCUMENT_SPREADSHEET_MIME_TYPE, b);

      File file = new File();
      file.setTitle(fileName);
      file.setMimeType(OPENDOCUMENT_SPREADSHEET_MIME_TYPE);
      return drive.files().insert(file, fileContent).setConvert(true).execute().getId();
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  /**
   * Gets the worksheet url.
   * 
   * @param spreadsheetService the spreadsheet service
   * @param spreadsheetId the spreadsheet id
   */
  private URL getWorksheetUrl(SpreadsheetService spreadsheetService, String spreadsheetId)
      throws IOException, ServiceException {
    if (isCancelled()) {
      return null;
    }
    URL url = new URL(String.format(Locale.US, GET_WORKSHEETS_URI, spreadsheetId));
    WorksheetFeed feed = spreadsheetService.getFeed(url, WorksheetFeed.class);
    List<WorksheetEntry> worksheets = feed.getEntries();

    if (worksheets.size() > 0) {
      return worksheets.get(0).getListFeedUrl();
    }
    return null;
  }

  /**
   * Adds track info to a worksheet.
   * 
   * @param spreadsheetService the spreadsheet service
   * @param worksheetUrl the worksheet url
   * @param track the track
   * @return true if completes.
   */
  private boolean addTrackInfo(SpreadsheetService spreadsheetService, URL worksheetUrl, Track track)
      throws IOException, ServiceException {
    if (isCancelled()) {
      return false;
    }
    TripStatistics tripStatistics = track.getTripStatistics();
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);
    String distanceUnit = context.getString(
        metricUnits ? R.string.unit_kilometer : R.string.unit_mile);
    String speedUnit = context.getString(
        metricUnits ? R.string.unit_kilometer_per_hour : R.string.unit_mile_per_hour);
    String elevationUnit = context.getString(
        metricUnits ? R.string.unit_meter : R.string.unit_feet);
    ListEntry row = new ListEntry();

    row.getCustomElements().setValueLocal("name", track.getName());
    row.getCustomElements().setValueLocal("description", track.getDescription());
    row.getCustomElements()
        .setValueLocal("date", StringUtils.formatDateTime(context, tripStatistics.getStartTime()));
    row.getCustomElements().setValueLocal(
        "totaltime", StringUtils.formatElapsedTimeWithHour(tripStatistics.getTotalTime()));
    row.getCustomElements().setValueLocal(
        "movingtime", StringUtils.formatElapsedTimeWithHour(tripStatistics.getMovingTime()));
    row.getCustomElements().setValueLocal(
        "distance", SendSpreadsheetsUtils.getDistance(tripStatistics.getTotalDistance(), metricUnits));
    row.getCustomElements().setValueLocal("distanceunit", distanceUnit);
    row.getCustomElements().setValueLocal(
        "averagespeed", SendSpreadsheetsUtils.getSpeed(tripStatistics.getAverageSpeed(), metricUnits));
    row.getCustomElements().setValueLocal("averagemovingspeed",
        SendSpreadsheetsUtils.getSpeed(tripStatistics.getAverageMovingSpeed(), metricUnits));
    row.getCustomElements().setValueLocal(
        "maxspeed", SendSpreadsheetsUtils.getSpeed(tripStatistics.getMaxSpeed(), metricUnits));
    row.getCustomElements().setValueLocal("speedunit", speedUnit);
    row.getCustomElements().setValueLocal("elevationgain",
        SendSpreadsheetsUtils.getElevation(tripStatistics.getTotalElevationGain(), metricUnits));
    row.getCustomElements().setValueLocal(
        "minelevation", SendSpreadsheetsUtils.getElevation(tripStatistics.getMinElevation(), metricUnits));
    row.getCustomElements().setValueLocal(
        "maxelevation", SendSpreadsheetsUtils.getElevation(tripStatistics.getMaxElevation(), metricUnits));
    row.getCustomElements().setValueLocal("elevationunit", elevationUnit);
    
    ListEntry result = spreadsheetService.insert(worksheetUrl, row);
    return result != null;
  }
}
