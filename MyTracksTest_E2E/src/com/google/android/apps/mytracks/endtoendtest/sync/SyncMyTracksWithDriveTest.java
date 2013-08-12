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
import com.google.android.apps.mytracks.endtoendtest.RunConfiguration;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import java.io.IOException;

/**
 * Tests making changes on My Tracks when syncing My Tracks with Google Drive.
 * 
 * @author Youtao Liu
 */
public class SyncMyTracksWithDriveTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Drive drive;
  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public SyncMyTracksWithDriveTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    SyncTestUtils.setUpForSyncTest(instrumentation, trackListActivity);
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    drive = SyncTestUtils
        .getGoogleDrive(EndToEndTestUtils.activityMytracks.getApplicationContext());
  }

  /**
   * Deletes all tracks on Google Drive and checks in MyTracks.
   * 
   * @throws IOException
   */
  public void testDeleteAllTracksOnDrive() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.createTrackIfEmpty(0, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);

    SyncTestUtils.removeKMLFiles(drive);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkTracksNumber(0);
  }

  /**
   * Deletes one file on Google Drive and checks it in MyTracks.
   * 
   * @throws IOException
   */
  public void testDeleteOneFileOnDrive() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);

    // Remove one track from Google Drive
    File file = SyncTestUtils.getFile(EndToEndTestUtils.trackName, drive);
    SyncTestUtils.removeFile(file, drive);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);
    SyncTestUtils.checkTracksNumber(1);
  }

  /**
   * Tests deleting and creating MyTracks folder on Google Dive by MyTracks.
   * 
   * @throws IOException
   */
  public void testCreateMyTracksOnDrive() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    instrumentation.waitForIdleSync();
    SyncTestUtils.checkFilesNumber(drive);
    File folder = SyncUtils.getMyTracksFolder(trackListActivity.getApplicationContext(), drive);
    assertNotNull(folder);

    SyncTestUtils.removeFile(folder, drive);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);
  }
}
