/*
 * Copyright 2013 Google Inc.
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
package com.google.android.apps.mytracks.endtoendtest.sync;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import android.widget.CheckBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

/**
 * Utilites for Google Drive sync endtoend test.
 * 
 * @author Youtao Liu
 */
public class SyncTestUtils {

  public static boolean isCheckedRunSyncTest = false;
  public static boolean runSyncTest = false;
  public static final String KML_FILE_POSTFIX = ".kml";
  public static final long MAX_TIME_TO_WAIT_SYNC = 50000;

  /**
   * Sets up sync tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param trackListActivity the startup activity
   * @return a Google Drive object
   */
  public static Drive setUpForSyncTest(Instrumentation instrumentation,
      TrackListActivity trackListActivity) throws IOException, GoogleAuthException {
    if (runSyncTest || !isCheckedRunSyncTest) {
      EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
    }
    if (!isCheckedRunSyncTest) {
      runSyncTest = canRunSyncTest();
      isCheckedRunSyncTest = true;
    }
    if (runSyncTest) {
      EndToEndTestUtils.deleteAllTracks();
      SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
      Drive drive1 = SyncTestUtils.getGoogleDrive(EndToEndTestUtils.activityMytracks
          .getApplicationContext());
      removeKMLFiles(drive1);
      SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
      Drive drive2 = SyncTestUtils.getGoogleDrive(EndToEndTestUtils.activityMytracks
          .getApplicationContext());
      removeKMLFiles(drive2);
      return drive2;
    }
    return null;
  }

  /**
   * Runs sync tests when both test accounts are bound with the devices.
   * 
   * @return true means can run sync tests in this device
   */
  public static boolean canRunSyncTest() {
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google_account_title));
    boolean canRunSyncE2ETest = EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_1, 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)
        && EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_2, 1,
            EndToEndTestUtils.TINY_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_cancel));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    return canRunSyncE2ETest;
  }

  /**
   * Gets drive object of Google Drive.
   * 
   * @param context the context of application
   * @return a Google Drive object
   */
  public static Drive getGoogleDrive(Context context) throws IOException, GoogleAuthException {
    String googleAccount = PreferencesUtils.getString(
        context, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    GoogleAccountCredential credential = SyncUtils.getGoogleAccountCredential(
        context, googleAccount);
    return SyncUtils.getDriveService(credential);
  }

  /**
   * Gets KML files from Google Drive of current account.
   * 
   * @param context the context of application
   * @param drive a Google Drive object
   * @return KML file on Google drive
   * @throws IOException
   */
  public static List<File> getDriveFiles(Context context, Drive drive) throws IOException {
    File folder = SyncTestUtils.getMyTracksFolder(context, drive);
    if (folder == null) {
      return new ArrayList<File>();
    }
    String folderId = folder.getId();
    return drive.files().list()
        .setQ(String.format(Locale.US, SyncUtils.MY_TRACKS_FOLDER_FILES_QUERY, folderId)).execute()
        .getItems();
  }

  /**
   * Finds whether a file is existed by the name of track.
   * 
   * @param trackName name of track
   * @param drive a Google Drive object
   * @return the file be found
   * @throws IOException
   */
  public static File getFile(String trackName, Drive drive) throws IOException {
    List<File> files = getDriveFiles(EndToEndTestUtils.activityMytracks.getApplicationContext(),
        drive);
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      String title = file.getTitle();
      if (title.equals(trackName + KML_FILE_POSTFIX)) {
        return file;
      }
    }
    return null;
  }

  /**
   * Removes all KML files on Google Drive.
   * 
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void removeKMLFiles(Drive drive) throws IOException {
    List<File> files = SyncTestUtils.getDriveFiles(
        EndToEndTestUtils.activityMytracks.getApplicationContext(), drive);
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      removeFile(file, drive);
    }
  }

  /**
   * Removes one file on Google Drive.
   * 
   * @param file the file to remove
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void removeFile(File file, Drive drive) throws IOException {
    drive.files().trash(file.getId()).execute();
  }

  /**
   * Enables sync feature in MyTracks settings.
   * 
   * @param accountName the account which is used to sync with
   */
  public static void enableSync(String accountName) {
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google_account_title));

    // Whether test account is bound.
    if (EndToEndTestUtils.SOLO.waitForText(accountName, 1, EndToEndTestUtils.TINY_WAIT_TIME)) {
      EndToEndTestUtils.SOLO.clickOnText(accountName);
      EndToEndTestUtils.instrumentation.waitForIdleSync();
      if (EndToEndTestUtils.SOLO.waitForText(
          EndToEndTestUtils.activityMytracks.getString(
              R.string.settings_google_account_confirm_message).split("%")[0], 1,
          EndToEndTestUtils.SHORT_WAIT_TIME)) {
        EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
            .getString(R.string.generic_ok));
      }
    } else {
      Assert.fail();
      return;
    }

    EndToEndTestUtils.instrumentation.waitForIdleSync();
    CheckBox syncCheckBox = EndToEndTestUtils.SOLO.getCurrentCheckBoxes().get(0);
    if (!syncCheckBox.isChecked()) {
      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
          .getString(R.string.settings_google_drive_sync_title));

      EndToEndTestUtils.SOLO.waitForText(
          EndToEndTestUtils.activityMytracks.getString(
              R.string.settings_google_drive_sync_confirm_message).split("%")[0], 1,
          EndToEndTestUtils.SHORT_WAIT_TIME);
      Assert.assertTrue(EndToEndTestUtils.SOLO.searchText(accountName, true));
      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
          .getString(R.string.generic_ok));
    }
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Checks the files number on Google Drive
   * 
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void checkFilesNumber(Drive drive) throws IOException {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      try {
        int trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();
        List<File> files = getDriveFiles(
            EndToEndTestUtils.activityMytracks.getApplicationContext(), drive);
        if (files.size() == trackNumber) {
          return;
        }
      } catch (GoogleJsonResponseException e) {
        EndToEndTestUtils.sleep(EndToEndTestUtils.SHORT_WAIT_TIME);
      }
    }
    Assert.fail();
  }

  /**
   * Checks the files number on Google Drive
   * 
   * @param number number of files on Google Drive
   */
  public static void checkTracksNumber(int number) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      int trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();
      if (trackNumber == number) {
        return;
      }
    }
    Assert.fail();
  }

  /**
   * Checks one file on Google Drive
   * 
   * @param trackName the name of track
   * @param shouldExist true means this track should be existed
   * @param drive a Google Drive object
   * @return true means the actual result is same as expectation
   * @throws IOException
   */
  public static boolean checkFile(String trackName, boolean shouldExist, Drive drive)
      throws IOException {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      boolean exist = getFile(trackName, drive) != null;
      if (exist == shouldExist) {
        return true;
      }
    }
    Assert.fail();
    return false;
  }

  /**
   * Gets the content of a file on Google Drive.
   * 
   * @param file file to read
   * @param drive a Google Drive object
   * @return the string content of the file
   * @throws IOException
   */
  public static String getContentOfFile(File file, Drive drive) throws IOException {
    HttpResponse resp = drive.getRequestFactory()
        .buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
    BufferedReader br = new BufferedReader(new InputStreamReader(resp.getContent()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    String fileContent = sb.toString();
    br.close();
    return fileContent;
  }

  /**
   * Gets the MyTracks folder on Google Drive.
   * 
   * @param context context of application
   * @param drive a Google Drive object
   * @return the MyTracks folder on Google Drive
   */
  public static File getMyTracksFolder(Context context, Drive drive) {
    try {
      String folderName = context.getString(R.string.my_tracks_app_name);
      com.google.api.services.drive.Drive.Files.List list = drive.files().list()
          .setQ(String.format(Locale.US, SyncUtils.MY_TRACKS_FOLDER_QUERY, folderName));
      FileList result = list.execute();
      for (File file : result.getItems()) {
        if (file.getTitle().equals(folderName)) {
          return file;
        }
      }
    } catch (IOException e) {
      Log.e(EndToEndTestUtils.LOG_TAG, "IOException", e);
    }
    return null;
  }
}
