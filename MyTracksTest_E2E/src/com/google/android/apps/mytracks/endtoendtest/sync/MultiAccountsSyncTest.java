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
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.Drive;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

/**
 * Tests the situation when user use multiple account in MyTracks.
 * 
 * @author Youtao Liu
 */
public class MultiAccountsSyncTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Drive drive;
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
    SyncTestUtils.setUpForSyncTest(instrumentation, trackListActivity);
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    drive = SyncTestUtils
        .getGoogleDrive(EndToEndTestUtils.activityMytracks.getApplicationContext());
  }

  /**
   * Tests sync tracks with Google Drive of two accounts with following
   * sequence:
   * <ul>
   * <li>1. Enable sync with account1.</li>
   * <li>2. Create a track.</li>
   * <li>3. Enable sync with account2.</li>
   * <li>4. Create a track.</li>
   * <li>5. Sync and check</li>
   * <li>6. Enable sync with account1.</li>
   * <li>7. Sync and check</li>
   * <li>8. Enable sync with account2.</li>
   * <li>9. Check</li>
   * </ul>
   */
  public void testSyncTracksWithMultiAccounts() throws Exception {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    // Create tracks with first track.
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Create tracks with second track.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Sync with Google Drive and then check it of the second account.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
    SyncTestUtils.checkFilesNumber(drive);

    // Check Google Drive of the first account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
    SyncTestUtils.checkFilesNumber(drive);

    // Back to account1 change check that files are still existed.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
    SyncTestUtils.checkFilesNumber(drive);
  }

  /**
   * Tests the behavior of MyTracks about delete operation and switch accounts
   * with following sequence.:
   * <ul>
   * <li>1. Create 3 tracks.</li>
   * <li>2. Delete 1 track.</li>
   * <li>3. Sync with account2(account2 should have 2 files).</li>
   * <li>4. Delete 1 track (account2 should have 1 file).</li>
   * <li>5. Sync with account 1 (account 1 should have 1 file, account2 should
   * also have 1 file).</li>
   * </ul>
   */
  public void testDeleteTracksWithMultiAccounts() throws Exception {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(0, true);

    // Delete one track.
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class)
        .get(0).getChildAt(0));
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnMenuItem(EndToEndTestUtils.activityMytracks
        .getString(R.string.menu_delete));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_yes));

    // Switch account and delete another track.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class)
        .get(0).getChildAt(0));
    EndToEndTestUtils.SOLO.clickOnMenuItem(EndToEndTestUtils.activityMytracks
        .getString(R.string.menu_delete));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_yes));

    // Check Google Drive of the first account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
    SyncTestUtils.checkFilesNumber(drive);

    // Check Google Drive of the second account.
    SyncTestUtils.enableSync(GoogleUtils.ACCOUNT_NAME_2);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    drive = SyncTestUtils.getGoogleDrive(trackListActivity.getApplicationContext());
    SyncTestUtils.checkFilesNumber(drive);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    EndToEndTestUtils.SOLO.finishOpenedActivities();
  }
}
