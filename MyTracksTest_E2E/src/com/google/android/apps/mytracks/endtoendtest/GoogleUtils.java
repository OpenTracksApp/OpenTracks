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
package com.google.android.apps.mytracks.endtoendtest;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.maps.MapFeatureEntry;
import com.google.android.apps.mytracks.io.gdata.maps.MapsClient;
import com.google.android.apps.mytracks.io.gdata.maps.MapsConstants;
import com.google.android.apps.mytracks.io.gdata.maps.MapsGDataConverter;
import com.google.android.apps.mytracks.io.gdata.maps.MapsMapMetadata;
import com.google.android.apps.mytracks.io.gdata.maps.XmlMapsGDataParserFactory;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.spreadsheets.SendSpreadsheetsAsyncTask;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Table;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.wireless.gdata.parser.GDataParser;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Provides utilities to access Google Maps, Google Documents, Google Fusion
 * Tables.
 * 
 * @author Youtao Liu
 */
public class GoogleUtils {

  public static final String DOCUMENT_NAME_PREFIX = "My Tracks";
  // This is the preferred account.
  public static final String ACCOUNT_NAME_1 = "mytrackstest@gmail.com";
  public static final String ACCOUNT_NAME_2 = "mytrackstest2@gmail.com";
  public static final String SPREADSHEET_NAME = DOCUMENT_NAME_PREFIX + "-"
      + EndToEndTestUtils.activityType;
  private static final String WORK_SHEET_NAME = "Log";
  private static final String TRANCK_NAME_COLUMN = "Name";

  private static final String TEST_TRACKS_QUERY = "'root' in parents and title contains '"
      + EndToEndTestUtils.TRACK_NAME_PREFIX + "' and trashed = false";

  /**
   * Gets the account to access Google Services.
   * 
   * @param context context used to get account
   * @return the first account which is bound with current device
   */
  private static Account getAccount(Context context) {
    return AccountManager.get(context).getAccountsByType(Constants.ACCOUNT_TYPE)[0];
  }

  /**
   * Gets Google maps of a user.
   * 
   * @param context used to get maps
   * @param mapsClient the client to access Google Maps
   * @return true means set successfully
   */
  private static ArrayList<MapsMapMetadata> getMaps(Context context, MapsClient mapsClient) {
    String authToken = null;
    ArrayList<String> mapIds = new ArrayList<String>();
    ArrayList<MapsMapMetadata> mapData = new ArrayList<MapsMapMetadata>();

    try {
      authToken = AccountManager.get(context).blockingGetAuthToken(getAccount(context),
          MapsConstants.SERVICE_NAME, false);
    } catch (Exception e) {
      Log.d(EndToEndTestUtils.LOG_TAG, "Unable to get auth token", e);
      return mapData;
    }

    GDataParser gDataParser = null;
    try {
      gDataParser = mapsClient.getParserForFeed(MapFeatureEntry.class, MapsClient.getMapsFeed(),
          authToken);
      gDataParser.init();
      while (gDataParser.hasMoreData()) {
        MapFeatureEntry entry = (MapFeatureEntry) gDataParser.readNextEntry(null);
        mapIds.add(MapsGDataConverter.getMapidForEntry(entry));
        mapData.add(MapsGDataConverter.getMapMetadataForEntry(entry));
      }
    } catch (Exception e) {
      Log.d(EndToEndTestUtils.LOG_TAG, "Unable to get maps", e);
    } finally {
      if (gDataParser != null) {
        gDataParser.close();
      }
    }

    return mapData;
  }

  /**
   * Searches a map in user's Google Maps.
   * 
   * @param title the title of map
   * @param activity activity to get context
   * @param isDelete whether delete the map of this track in the Google Maps
   * @return true means find the map
   */
  private static boolean searchMapByTitle(String title, Activity activity, boolean isDelete) {
    Context context = activity.getApplicationContext();
    MapsClient mapsClient = new MapsClient(GDataClientFactory.getGDataClient(context),
        new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
    ArrayList<MapsMapMetadata> mapData = getMaps(context, mapsClient);
    for (MapsMapMetadata oneData : mapData) {
      if (oneData.getDescription().indexOf(DOCUMENT_NAME_PREFIX) > -1
          && oneData.getTitle().equals(title)) {
        if (isDelete) {
          try {
            mapsClient.deleteEntry(oneData.getGDataEditUri(), AccountManager.get(context)
                .blockingGetAuthToken(getAccount(context), MapsConstants.SERVICE_NAME, false));
            return true;
          } catch (Exception e) {
            Log.d(EndToEndTestUtils.LOG_TAG, "Unable to drop map", e);
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Searches a map in user's Google Maps.
   * 
   * @param title the title of map
   * @param activity activity to get context
   * @return true means find the map
   */
  public static boolean searchMap(String title, Activity activity) {
    return searchMapByTitle(title, activity, false);
  }

  /**
   * Searches a map in user's Google Maps and then delete it.
   * 
   * @param title the title of map
   * @param activity activity to get context
   * @return true means find the map and delete it successfully
   */
  public static boolean deleteMap(String title, Activity activity) {
    return searchMapByTitle(title, activity, true);
  }

  /**
   * Removes old tracks created by MyTracks test.
   * 
   * @param activity
   * @param accountName
   */
  public static void deleteTestTracksOnGoogleDrive(Activity activity, String accountName) {
    try {
      GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
          activity.getApplicationContext(), accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (driveCredential == null) {
        return;
      }

      Drive drive = SyncUtils.getDriveService(driveCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list()
          .setQ(TEST_TRACKS_QUERY);
      List<File> files = list.execute().getItems();
      for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
        File file = (File) iterator.next();
        drive.files().delete(file.getId()).execute();
      }
    } catch (Exception e) {
      Log.e(EndToEndTestUtils.LOG_TAG, "Delete test tracks failed.");
    }
  }

  /**
   * Removes old tracks on Google Maps created by MyTracks test.
   * 
   * @param activity
   */
  public static void deleteTracksOnGoogleMaps(Activity activity) {
    Context context = activity.getApplicationContext();
    MapsClient mapsClient = new MapsClient(GDataClientFactory.getGDataClient(context),
        new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
    ArrayList<MapsMapMetadata> mapData = getMaps(context, mapsClient);
    for (MapsMapMetadata oneData : mapData) {

      try {
        mapsClient.deleteEntry(oneData.getGDataEditUri(), AccountManager.get(context)
            .blockingGetAuthToken(getAccount(context), MapsConstants.SERVICE_NAME, false));
      } catch (Exception e) {
        Log.d(EndToEndTestUtils.LOG_TAG, "Unable to drop map", e);
      }
    }
  }

  /**
   * Delete spreadsheet which name is title.
   * 
   * @param title the name of spreadsheet
   * @param activity to get context
   * @param accountName the name of Google account
   * @return true means delete successfully
   */
  public static boolean deleteSpreadsheetByTitle(String title, Activity activity, String accountName) {
    try {
      GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
          activity.getApplicationContext(), accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (driveCredential == null) {
        return false;
      }

      Drive drive = SyncUtils.getDriveService(driveCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list()
          .setQ(String.format(Locale.US, SendSpreadsheetsAsyncTask.GET_SPREADSHEET_QUERY, title));
      List<File> files = list.execute().getItems();
      for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
        File file = (File) iterator.next();
        drive.files().delete(file.getId()).execute();
      }
      return true;
    } catch (Exception e) {
      Log.e(EndToEndTestUtils.LOG_TAG, "Search spreadsheet failed.");
    }
    return false;
  }

  /**
   * Searches docs in user's Google Documents.
   * 
   * @param title the title of doc
   * @param activity to get context
   * @param accountName the name of Google account
   * @return the file list of the document, null means can not find the
   *         spreadsheets
   */
  public static List<File> searchAllSpreadsheetByTitle(String title, Activity activity,
      String accountName) {
    try {
      GoogleAccountCredential driveCredential = SendToGoogleUtils.getGoogleAccountCredential(
          activity.getApplicationContext(), accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (driveCredential == null) {
        return null;
      }

      Drive drive = SyncUtils.getDriveService(driveCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list()
          .setQ(String.format(Locale.US, SendSpreadsheetsAsyncTask.GET_SPREADSHEET_QUERY, title));
      FileList result = list.execute();
      return result.getItems();
    } catch (Exception e) {
      Log.e(EndToEndTestUtils.LOG_TAG, "Search spreadsheet failed.");
    }
    return null;
  }

  /**
   * Searches a track title in a spreadsheet.
   * 
   * @param trackName the track name to search
   * @param activity to get context
   * @param spreadsheetTitle the title of spreadsheet
   * @param isDelete whether delete the information of this track in the
   *          document
   * @param accountName the name of Google account
   * @return true means find the track name in the spreadsheet
   */
  private static boolean searchTrackTitleInSpreadsheet(String trackName, Activity activity,
      String spreadsheetTitle, boolean isDelete, String accountName) {
    try {
      // Get spreadsheet Id.
      String spreadsheetId = searchAllSpreadsheetByTitle(spreadsheetTitle, activity, accountName)
          .get(0).getId();

      // Get spreadsheet service.
      SpreadsheetService spreadsheetService = new SpreadsheetService(spreadsheetTitle);
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
      credential.setAccessToken(SendToGoogleUtils.getToken(activity.getApplicationContext(),
          accountName, SendToGoogleUtils.SPREADSHEETS_SCOPE));
      spreadsheetService.setOAuth2Credentials(credential);

      // Get work sheet.
      WorksheetFeed feed = spreadsheetService.getFeed(
          new URL(String.format(Locale.US, SendSpreadsheetsAsyncTask.GET_WORKSHEETS_URI,
              spreadsheetId)), WorksheetFeed.class);
      List<WorksheetEntry> data = feed.getEntries();
      for (Iterator<WorksheetEntry> iterator = data.iterator(); iterator.hasNext();) {
        WorksheetEntry worksheetEntry = (WorksheetEntry) iterator.next();
        String title = worksheetEntry.getTitle().getPlainText();
        if (title.equals(WORK_SHEET_NAME)) {
          URL listFeedUrl = worksheetEntry.getListFeedUrl();
          List<ListEntry> listFeed = spreadsheetService.getFeed(listFeedUrl, ListFeed.class)
              .getEntries();
          for (Iterator<ListEntry> iterator2 = listFeed.iterator(); iterator2.hasNext();) {
            ListEntry listEntry = (ListEntry) iterator2.next();
            String name = listEntry.getCustomElements().getValue(TRANCK_NAME_COLUMN);
            if (name.equals(trackName)) {
              if (isDelete) {
                listEntry.delete();
              }
              return true;
            }
          }
        }
      }
    } catch (Exception e) {
      Log.e(EndToEndTestUtils.LOG_TAG, "Search spreadsheet failed.");
    }
    return false;
  }

  /**
   * Searches and deletes a track in spreadsheet.
   * 
   * @param title the track name to search
   * @param activity to get context
   * @return true means find and delete successfully
   */
  public static boolean deleteTrackInSpreadSheet(String title, Activity activity, String account) {
    return searchTrackTitleInSpreadsheet(title, activity, GoogleUtils.SPREADSHEET_NAME, true,
        account);
  }

  /**
   * Searches a fusion table in user's Google tables.
   * 
   * @param tableName name of fusion table to search
   * @param context android context
   * @param accountName name of account
   * @param isDelete whether delete this track
   * @return true means find and delete fusion table
   */
  public static boolean searchFusionTableByTitle(String tableName, Context context,
      String accountName, boolean isDelete) {
    try {
      GoogleAccountCredential credential = SendToGoogleUtils.getGoogleAccountCredential(context,
          accountName, SendToGoogleUtils.FUSION_TABLES_SCOPE);
      if (credential == null) {
        return false;
      }
      Fusiontables fusiontables = new Fusiontables.Builder(AndroidHttp.newCompatibleTransport(),
          new GsonFactory(), credential).build();
      List<Table> tables = fusiontables.table().list().execute().getItems();
      for (Iterator<Table> iterator = tables.iterator(); iterator.hasNext();) {
        Table table = (Table) iterator.next();
        String title = table.getName();
        if (title.equals(tableName)) {
          if (isDelete) {
            fusiontables.table().delete(table.getTableId()).execute();
          }
          return true;
        }
      }

    } catch (Exception e) {
      Log.d(EndToEndTestUtils.LOG_TAG, "Failed when operate fusion table.", e);
    }
    return false;
  }

  /**
   * Checks whether the status of account is right to use.
   * 
   * @return true means the status of account is good for sending
   */
  public static boolean isAccountAvailable() {
    // Check whether no account is binded with this device.
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.activityMytracks.getString(R.string.send_google_no_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      EndToEndTestUtils.getButtonOnScreen(
          EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), true, true);
      return false;
    }

    // Check whether need to choose account.
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.activityMytracks.getString(R.string.send_google_choose_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      EndToEndTestUtils.SOLO.clickOnText(GoogleUtils.ACCOUNT_NAME_1);
      EndToEndTestUtils.getButtonOnScreen(
          EndToEndTestUtils.activityMytracks.getString(R.string.generic_ok), false, true);
    }

    // Check whether no account permission.
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.activityMytracks.getString(R.string.send_google_no_account_permission),
        1, EndToEndTestUtils.SHORT_WAIT_TIME)) {
      return false;
    }

    return true;
  }

}