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
import android.view.KeyEvent;

import java.io.IOException;

/**
 * Tests making changes on Google Drive when syncing My Tracks with Google
 * Drive.
 * 
 * @author Youtao Liu
 */
public class SyncDriveWithMyTracksTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Drive drive;
  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public SyncDriveWithMyTracksTest() {
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
   * Deletes all tracks in MyTracks and checks in Google Drive.
   * 
   * @throws IOException
   */
  public void testDeleteAllTracksInMyTracks() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);
  }

  /**
   * Deletes one track in MyTracks and checks it in Google Drive.
   * 
   * @throws IOException
   */
  public void testDeleteOneTracksInMyTracks() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.createTrackIfEmpty(2, false);
    EndToEndTestUtils.SOLO.clickOnMenuItem(EndToEndTestUtils.activityMytracks
        .getString(R.string.menu_delete));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_yes));
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFile(EndToEndTestUtils.trackName, false, drive);
  }

  /**
   * Creates one empty track and one non-empty track in MyTracks and then check
   * them in Google Drive.
   * 
   * @throws IOException
   */
  public void testCreateTracksInMyTracks() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.createSimpleTrack(0, true);
    EndToEndTestUtils.createSimpleTrack(3, true);
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFilesNumber(drive);
  }

  /**
   * Edits a tracks in MyTracks and checks it on Google Drive after sync.
   * 
   * @throws IOException
   */
  public void testEditTrackInMyTracks() throws IOException {
    if (!RunConfiguration.getInstance().runSyncTest) {
      return;
    }
    EndToEndTestUtils.createTrackIfEmpty(3, true);

    // Sync this track.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);
    SyncTestUtils.checkFile(EndToEndTestUtils.trackName, true, drive);
    String oldTrack = SyncTestUtils.getContentOfFile(
        SyncTestUtils.getFile(EndToEndTestUtils.trackName, drive), drive);

    String oldTrackName = EndToEndTestUtils.trackName;

    // Edit this track.
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName);
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_edit), true);
    String newTrackName = EndToEndTestUtils.TRACK_NAME_PREFIX + "_new" + System.currentTimeMillis();
    String newType = EndToEndTestUtils.activityType + newTrackName;
    String newDesc = "desc" + newTrackName;
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.generic_save));
    sendKeys(KeyEvent.KEYCODE_DEL);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, newTrackName);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(1, newType);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(2, newDesc);
    EndToEndTestUtils.SOLO.clickOnButton(trackListActivity.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Sync again.
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_refresh), true);

    // Check.
    SyncTestUtils.checkFile(oldTrackName, false, drive);
    SyncTestUtils.checkFile(newTrackName, true, drive);
    String newTrack = SyncTestUtils.getContentOfFile(SyncTestUtils.getFile(newTrackName, drive),
        drive);
    assertNotSame(oldTrack, newTrack);
    assertTrue(newTrack.indexOf(newTrackName) > 0);
    assertTrue(newTrack.indexOf(newType) > 0);
    assertTrue(newTrack.indexOf(newDesc) > 0);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    EndToEndTestUtils.SOLO.finishOpenedActivities();
  }
}
