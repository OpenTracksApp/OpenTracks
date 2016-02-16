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
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.android.maps.mytracks.R;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
import android.content.Context;
import android.util.Log;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Utilities for accessing Google Drive, Google Maps, Google Fusion Tables, and
 * Google Spreadsheets.
 * 
 * @author Youtao Liu
 */
public class GoogleUtils {

  public static final String ACCOUNT_1 = "mytrackstest@gmail.com";
  public static final String ACCOUNT_2 = "mytrackstest2@gmail.com";

  private static final String TAG = GoogleUtils.class.getSimpleName();
  private static final String MY_TRACKS_PREFIX = "My Tracks";
  private static final String SPREADSHEETS_NAME = MY_TRACKS_PREFIX + "-"
      + EndToEndTestUtils.activityType;
  private static final String SPREADSHEETS_WORKSHEET_NAME = "Log";
  private static final String SPREADSHEETS_TRANCK_NAME_COLUMN = "Name";
  private static final String DRIVE_TEST_FILES_QUERY = "'root' in parents and title contains '"
      + EndToEndTestUtils.TRACK_NAME_PREFIX + "' and trashed = false";

  /**
   * Deletes Google Drive test files.
   * 
   * @param context the context
   * @param accountName the account name
   */
  public static void deleteDriveTestFiles(Context context, String accountName) {
    try {
      GoogleAccountCredential googleAccountCredential = SendToGoogleUtils
          .getGoogleAccountCredential(context, accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (googleAccountCredential == null) {
        return;
      }

      Drive drive = SyncUtils.getDriveService(googleAccountCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files()
          .list().setQ(DRIVE_TEST_FILES_QUERY);
      List<File> files = list.execute().getItems();
      Iterator<File> iterator = files.iterator();
      while (iterator.hasNext()) {
        File file = (File) iterator.next();
        drive.files().delete(file.getId()).execute();
      }
    } catch (Exception e) {
      Log.e(TAG, "Unable to delete Google Drive test files.", e);
    }
  }

  /**
   * Deletes Google Maps of the first Google account.
   * 
   * @param context the context
   * @param accountName the account name
   * @param mapTitle the map title
   * @return true if deletion is success.
   */
  public static boolean deleteMaps(Context context, String accountName, String mapTitle) {
    Account account = getGoogleAccount(context, accountName);
    if (account == null) {
      return false;
    }
    MapsClient mapsClient = new MapsClient(GDataClientFactory.getGDataClient(context),
        new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
    ArrayList<MapsMapMetadata> mapMetadata = getMaps(context, account, mapsClient);
    for (MapsMapMetadata data : mapMetadata) {
      if (data.getDescription().indexOf(MY_TRACKS_PREFIX) > -1
          && data.getTitle().equals(mapTitle)) {
        try {
          mapsClient.deleteEntry(data.getGDataEditUri(), AccountManager.get(context)
              .blockingGetAuthToken(account, MapsConstants.SERVICE_NAME, false));
          return true;
        } catch (Exception e) {
          Log.e(TAG, "Unable to delete maps.", e);
          return false;
        }
      }
    }
    return false;
  }

  /**
   * Deletes all Google Maps of the first Google account
   * 
   * @param context the context
   * @param accountName the account name
   */
  public static void deleteAllMaps(Context context, String accountName) {
    Account account = getGoogleAccount(context, accountName);
    if (account == null) {
      Log.e(TAG, "Unable to get account.");
      return;
    }
    MapsClient mapsClient = new MapsClient(GDataClientFactory.getGDataClient(context),
        new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));
    ArrayList<MapsMapMetadata> mapMetadata = getMaps(context, account, mapsClient);
    for (MapsMapMetadata oneData : mapMetadata) {
      try {
        mapsClient.deleteEntry(oneData.getGDataEditUri(), AccountManager.get(context)
            .blockingGetAuthToken(account, MapsConstants.SERVICE_NAME, false));
      } catch (Exception e) {
        Log.e(TAG, "Unable to delete maps.", e);
      }
    }
  }

  /**
   * Deletes Google Fusion Tables.
   * 
   * @param context the context
   * @param accountName the account name
   * @param tableName the table name
   * @return true if deletion is success.
   */
  public static boolean deleteFusionTables(Context context, String accountName, String tableName) {
    try {
      GoogleAccountCredential googleAccountCredential = SendToGoogleUtils
          .getGoogleAccountCredential(context, accountName, SendToGoogleUtils.FUSION_TABLES_SCOPE);
      if (googleAccountCredential == null) {
        return false;
      }
      Fusiontables fusiontables = new Fusiontables.Builder(
          AndroidHttp.newCompatibleTransport(), new GsonFactory(), googleAccountCredential).build();
      List<Table> tables = fusiontables.table().list().execute().getItems();
      Iterator<Table> iterator = tables.iterator();
      while (iterator.hasNext()) {
        Table table = (Table) iterator.next();
        String name = table.getName();
        if (name != null && name.equals(tableName)) {
          fusiontables.table().delete(table.getTableId()).execute();
          return true;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Unable to search fusion tables.", e);
    }
    return false;
  }

  /**
   * Searches Google Spreadsheets.
   * 
   * @param context the context
   * @param accountName the account name
   * @return the list of spreadsheets matching the title. Null if unable to
   *         search.
   */
  public static List<File> searchSpreadsheets(Context context, String accountName) {
    try {
      GoogleAccountCredential googleAccountCredential = SendToGoogleUtils
          .getGoogleAccountCredential(context, accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (googleAccountCredential == null) {
        return null;
      }

      Drive drive = SyncUtils.getDriveService(googleAccountCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list().setQ(String.format(
          Locale.US, SendSpreadsheetsAsyncTask.GET_SPREADSHEET_QUERY, SPREADSHEETS_NAME));
      return list.execute().getItems();
    } catch (Exception e) {
      Log.e(TAG, "Unable to search spreadsheets.", e);
    }
    return null;
  }

  /**
   * Deletes Google Spreadsheets.
   * 
   * @param context the context
   * @param accountName the account name
   * @return true if deletion is success.
   */
  public static boolean deleteSpreadsheets(Context context, String accountName) {
    try {
      GoogleAccountCredential googleAccountCredential = SendToGoogleUtils
          .getGoogleAccountCredential(context, accountName, SendToGoogleUtils.DRIVE_SCOPE);
      if (googleAccountCredential == null) {
        return false;
      }

      Drive drive = SyncUtils.getDriveService(googleAccountCredential);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list().setQ(String.format(
          Locale.US, SendSpreadsheetsAsyncTask.GET_SPREADSHEET_QUERY, SPREADSHEETS_NAME));
      List<File> files = list.execute().getItems();
      Iterator<File> iterator = files.iterator();
      while (iterator.hasNext()) {
        File file = (File) iterator.next();
        drive.files().delete(file.getId()).execute();
      }
      return true;
    } catch (Exception e) {
      Log.e(TAG, "Unable to delete spreadsheets.", e);
    }
    return false;
  }

  /**
   * Deletes Google Spreadsheets row.
   * 
   * @param context the context
   * @param accountName the account name
   * @param trackName the track name
   * @return true if deletion is success.
   */
  public static boolean deleteSpreadsheetsRow(
      Context context, String accountName, String trackName) {
    try {
      // Get spreadsheet Id
      List<File> files = searchSpreadsheets(context, accountName);
      if (files == null || files.size() == 0) {
        return false;
      }
      String spreadsheetId = files.get(0).getId();

      // Get spreadsheet service
      SpreadsheetService spreadsheetService = new SpreadsheetService(
          "MyTracks-" + SystemUtils.getMyTracksVersion(context));
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
      credential.setAccessToken(
          SendToGoogleUtils.getToken(context, accountName, SendToGoogleUtils.SPREADSHEETS_SCOPE));
      spreadsheetService.setOAuth2Credentials(credential);

      // Get work sheet
      WorksheetFeed worksheetFeed = spreadsheetService.getFeed(new URL(
          String.format(Locale.US, SendSpreadsheetsAsyncTask.GET_WORKSHEETS_URI, spreadsheetId)),
          WorksheetFeed.class);
      Iterator<WorksheetEntry> worksheetEntryIterator = worksheetFeed.getEntries().iterator();
      while (worksheetEntryIterator.hasNext()) {
        WorksheetEntry worksheetEntry = (WorksheetEntry) worksheetEntryIterator.next();
        String worksheetTitle = worksheetEntry.getTitle().getPlainText();
        if (worksheetTitle.equals(SPREADSHEETS_WORKSHEET_NAME)) {
          URL url = worksheetEntry.getListFeedUrl();
          Iterator<ListEntry> listEntryIterator = spreadsheetService.getFeed(url, ListFeed.class)
              .getEntries().iterator();
          while (listEntryIterator.hasNext()) {
            ListEntry listEntry = (ListEntry) listEntryIterator.next();
            String name = listEntry.getCustomElements().getValue(SPREADSHEETS_TRANCK_NAME_COLUMN);
            if (name.equals(trackName)) {
              listEntry.delete();
              return true;
            }
          }
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Unable to delete spreadsheets row.", e);
    }
    return false;
  }

  /**
   * Returns true if an account is available.
   */
  public static boolean isAccountAvailable() {
    // Check the no account dialog
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.trackListActivity.getString(R.string.send_google_no_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      EndToEndTestUtils.getButtonOnScreen(
          EndToEndTestUtils.trackListActivity.getString(R.string.generic_ok), false, true);
      return false;
    }

    // Check the choose account dialog
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.trackListActivity.getString(R.string.send_google_choose_account_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      EndToEndTestUtils.SOLO.clickOnText(ACCOUNT_1);
      EndToEndTestUtils.getButtonOnScreen(
          EndToEndTestUtils.trackListActivity.getString(R.string.generic_ok), false, true);
    }

    // Check the no account permission dialog
    if (EndToEndTestUtils.SOLO.waitForText(
        EndToEndTestUtils.trackListActivity.getString(R.string.send_google_no_account_permission), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      return false;
    }

    return true;
  }

  /**
   * Gets the google account.
   * 
   * @param context the context
   * @param accountName the account name
   */
  private static Account getGoogleAccount(Context context, String accountName) {
    Account[] accounts = AccountManager.get(context).getAccountsByType(Constants.ACCOUNT_TYPE);
    for (Account account : accounts) {
      if (account.name.equals(accountName)) {
        return account;
      }
    }
    return null;
  }

  /**
   * Gets Google Maps.
   * 
   * @param context the context
   * @param account the account
   * @param mapsClient the map client
   * @return a list of maps.
   */
  private static ArrayList<MapsMapMetadata> getMaps(
      Context context, Account account, MapsClient mapsClient) {
    String authToken = null;
    ArrayList<String> mapIds = new ArrayList<String>();
    ArrayList<MapsMapMetadata> mapMetadata = new ArrayList<MapsMapMetadata>();

    try {
      authToken = AccountManager.get(context)
          .blockingGetAuthToken(account, MapsConstants.SERVICE_NAME, false);
    } catch (Exception e) {
      Log.e(TAG, "Unable to get auth token.", e);
      return mapMetadata;
    }

    GDataParser gDataParser = null;
    try {
      gDataParser = mapsClient.getParserForFeed(
          MapFeatureEntry.class, MapsClient.getMapsFeed(), authToken);
      gDataParser.init();
      while (gDataParser.hasMoreData()) {
        MapFeatureEntry entry = (MapFeatureEntry) gDataParser.readNextEntry(null);
        mapIds.add(MapsGDataConverter.getMapidForEntry(entry));
        mapMetadata.add(MapsGDataConverter.getMapMetadataForEntry(entry));
      }
    } catch (Exception e) {
      Log.e(TAG, "Unable to get maps.", e);
    } finally {
      if (gDataParser != null) {
        gDataParser.close();
      }
    }
    return mapMetadata;
  }
}