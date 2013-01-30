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

import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import android.content.Context;
import android.widget.CheckBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

/**
 * Utilites for Google Drive sync endtoend test.
 * 
 * @author Youtao Liu
 */
public class SyncTestUtils {

  public static final String KML_FILE_POSTFIX = ".kml";
  public static final long MAX_TIME_TO_WAIT_SYNC = 50000;

  /**
   * Gets drive object of Google Drive.
   * 
   * @param context the context of application
   * @return drive drive object of Google Drive

   */
  public static Drive getGoogleDrive(Context context) {
    String googleAccount = PreferencesUtils.getString(context, R.string.google_account_key,
        PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    GoogleAccountCredential credential = SyncUtils.getCredential(context, googleAccount);
    return SyncUtils.getDriveService(credential);
  }

  /**
   * Queries files from Google Drive.
   * 
   * @param context the context of application
   * @param drive drive object of Google Drive
   * @return KML file on Google drive
   * @throws IOException
   */
  public static List<File> updateDriveFiles(Context context, Drive drive) throws IOException {
    String folderId = SyncUtils.getMyTracksFolder(context, drive);
    return drive.files().list()
        .setQ(String.format(Locale.US, SyncUtils.GET_KML_FILES_QUERY, folderId)).execute()
        .getItems();
  }

  /**
   * Finds whether a file is existed by the name of track.
   * 
   * @param trackName name of track
   * @param drive drive object of Google Drive
   * @return the file be found
   * @throws IOException
   */
  public static File getFile(String trackName, Drive drive) throws IOException {
    List<File> files = updateDriveFiles(EndToEndTestUtils.activityMytracks.getApplicationContext(),
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
   * @param drive drive object of Google Drive
   * @throws IOException
   */
  public static void removeKMLFiles(Drive drive) throws IOException {
    List<File> files = SyncTestUtils.updateDriveFiles(
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
   * @param drive drive object of Google Drive
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
      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
          .getString(R.string.generic_ok));

      EndToEndTestUtils.SOLO.goBack();
      EndToEndTestUtils.SOLO.goBack();
    }
  }

  /**
   * Checks the files number on Google Drive
   * 
   * @param number number of files on Google Drive
   * @param drive drive object of Google Drive
   * @throws IOException
   */
  public static void checkFilesNumber(int number, Drive drive) throws IOException {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      List<File> files = updateDriveFiles(
          EndToEndTestUtils.activityMytracks.getApplicationContext(), drive);
      if (files.size() == number) {
        return;
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
   * @param drive drive object of Google Drive
   * @return true means the actual result is same as expectation
   * @throws IOException
   */
  public static boolean checkFile(String trackName, boolean shouldExist, Drive drive)
      throws IOException {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      if ((getFile(trackName, drive) != null) == shouldExist) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the content of a file on Google Drive.
   * 
   * @param file file to read
   * @param drive drive object of Google Drive
   * @return the string content of the file
   * @throws IOException
   */
  public static String getContentOfFile(File file, Drive drive) throws IOException {
    HttpResponse resp = drive.getRequestFactory()
        .buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
    InputStream response = resp.getContent();
    BufferedReader br = new BufferedReader(new InputStreamReader(response));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    String fileContent = sb.toString();
    br.close();
    return fileContent;
  }
}
