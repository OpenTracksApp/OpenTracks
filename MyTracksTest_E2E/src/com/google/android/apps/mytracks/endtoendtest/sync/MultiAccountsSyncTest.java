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
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.Drive;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import java.io.IOException;

/**
 * Tests the situation when user use multiple account in MyTracks.
 * 
 * @author Youtao Liu
 */
public class MultiAccountsSyncTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  public static Drive drive;
  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public MultiAccountsSyncTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
    drive = SyncTestUtils.setUpForSyncTest(GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.deleteAllTracks();
  }

  /**
   * Tests sync tracks with Google Drive of two accounts.
   * 
   * @throws IOException
   */
  public void testSyncTracksWithMultiAccounts() throws IOException {
    // Create tracks with first track.
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Create tracks with second track.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Sync with Google Drive and then check it of the second account.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    drive = SyncTestUtils.setUpForSyncTest(GoogleUtils.ACCOUNT_NAME_2);
    SyncTestUtils.checkFilesNumber(drive);

    // Check Google Drive of the first account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    drive = SyncTestUtils.setUpForSyncTest(GoogleUtils.ACCOUNT_NAME_1);
    SyncTestUtils.checkFilesNumber(drive);
  }

  /**
   * Creates three tracks and the deletes one in one account, and then deletes
   * another one in another account. Keeps one tracks, then sync with two
   * accounts.
   * 
   * @throws IOException
   */
  public void testDeleteTracksWithMultiAccounts() throws IOException {
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Delete one track.
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentListViews().get(0)
        .getChildAt(0));
    EndToEndTestUtils.SOLO.clickOnMenuItem(EndToEndTestUtils.activityMytracks
        .getString(R.string.menu_delete));

    // Switch account and delete another track.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentListViews().get(0)
        .getChildAt(0));
    EndToEndTestUtils.SOLO.clickOnMenuItem(EndToEndTestUtils.activityMytracks
        .getString(R.string.menu_delete));

    // Check Google Drive of the first account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    drive = SyncTestUtils.setUpForSyncTest(GoogleUtils.ACCOUNT_NAME_1);
    SyncTestUtils.checkFilesNumber(drive);

    // Check Google Drive of the second account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_sync_now), true);
    drive = SyncTestUtils.setUpForSyncTest(GoogleUtils.ACCOUNT_NAME_2);
    SyncTestUtils.checkFilesNumber(drive);

  }

}
