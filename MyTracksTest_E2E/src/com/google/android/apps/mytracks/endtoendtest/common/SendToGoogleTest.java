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
package com.google.android.apps.mytracks.endtoendtest.common;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.apps.mytracks.endtoendtest.sync.SyncTestUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.services.drive.model.File;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the logic of send track to Google.
 * 
 * @author Youtao Liu
 */
public class SendToGoogleTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public SendToGoogleTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();

    GoogleUtils.deleteTracksOnGoogleMaps(activityMyTracks);
    GoogleUtils.deleteTestTracksOnGoogleDrive(activityMyTracks, GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Checks all services and send to google.
   * 
   * @throws GoogleAuthException
   * @throws IOException
   */
  public void testCreateAndSendTrack_send() throws IOException, GoogleAuthException {
    // Create a new track.
    EndToEndTestUtils.createSimpleTrack(1, false);
    instrumentation.waitForIdleSync();
    checkSendTrackToGoogle();
  }

  /**
   * Checks all services and send to google.
   * 
   * @throws GoogleAuthException
   * @throws IOException
   */
  public void testCreateAndSendTrack_sendPausedTrack() throws IOException, GoogleAuthException {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createTrackWithPause(3);
    instrumentation.waitForIdleSync();
    checkSendTrackToGoogle();
  }

  /**
   * Sends two tracks with same activity. Two tracks should be in the same
   * spreadsheet.
   */
  public void testSendTwoTracksWithSameActivity() {
    String testActivity = "(TestActivity)";
    EndToEndTestUtils.activityType = testActivity;
    GoogleUtils.deleteSpreadsheetByTitle("My Tracks-" + EndToEndTestUtils.activityType,
        EndToEndTestUtils.activityMytracks, GoogleUtils.ACCOUNT_NAME_1);
    EndToEndTestUtils.createSimpleTrack(1, false);
    boolean result = sendToGoogle();
    result = result && sendToGoogle();

    // Result is true mean has account bound with this device and send
    // successful.
    if (result) {
      List<File> fileList = GoogleUtils.searchAllSpreadsheetByTitle(
          GoogleUtils.DOCUMENT_NAME_PREFIX + "-" + EndToEndTestUtils.activityType,
          activityMyTracks, GoogleUtils.ACCOUNT_NAME_1);
      assertEquals(1, fileList.size());
    }
  }

  /**
   * Checks the process of sending track to google.
   * 
   * @throws GoogleAuthException
   * @throws IOException
   */
  private void checkSendTrackToGoogle() throws IOException, GoogleAuthException {
    if (!sendToGoogle()) {
      return;
    }
    // Check whether all data is correct on Google Drive, Maps, Documents, and
    // Spreadsheet.
    assertTrue(SyncTestUtils.checkFile(EndToEndTestUtils.trackName, true,
        SyncTestUtils.getGoogleDrive(activityMyTracks.getApplicationContext())));
    assertTrue(GoogleUtils.deleteMap(EndToEndTestUtils.trackName, activityMyTracks));
    assertTrue(GoogleUtils.searchFusionTableByTitle(EndToEndTestUtils.trackName, activityMyTracks,
        GoogleUtils.ACCOUNT_NAME_1, true));
    assertTrue(GoogleUtils.deleteTrackInSpreadSheet(EndToEndTestUtils.trackName, activityMyTracks,
        GoogleUtils.ACCOUNT_NAME_1));
  }

  /**
   * Sends a track to Google.
   * 
   * @return true means send successfully
   */
  private boolean sendToGoogle() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_export), true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.export_title));
    instrumentation.waitForIdleSync();
    ArrayList<CheckBox> checkBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    for (int i = 0; i < checkBoxs.size(); i++) {
      if (!checkBoxs.get(i).isChecked()) {
        EndToEndTestUtils.SOLO.clickOnView(checkBoxs.get(i));
        instrumentation.waitForIdleSync();
      }
    }

    if (checkBoxs.size() < 4) {
      EndToEndTestUtils.SOLO.scrollDown();
      checkBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();

      // Choose all Google service.
      for (int i = 0; i < checkBoxs.size(); i++) {
        if (!checkBoxs.get(i).isChecked()) {
          EndToEndTestUtils.SOLO.clickOnCheckBox(i);
        }
      }
    }
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.menu_export),
        true, true);

    instrumentation.waitForIdleSync();
    if (!GoogleUtils.isAccountAvailable()) {
      return false;
    }

    // Following check the process of "Send to Google".
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.generic_progress_title)));

    // Waiting the send is finish. Should double check it for the progress
    // dialog may disappear and display again while switch to next send(There
    // are four sends currently, there are send to drive, maps, fusion table and
    // spreadsheet).
    while (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.generic_progress_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)
        || EndToEndTestUtils.SOLO.waitForText(
            activityMyTracks.getString(R.string.generic_progress_title), 1,
            EndToEndTestUtils.SHORT_WAIT_TIME)) {}

    // Check whether the result dialog is display.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_share_url)));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    return true;
  }

  /**
   * Sends a large track to Google.
   * 
   * @throws GoogleAuthException
   * @throws IOException
   */
  public void testSendLargeTrackToGoogle() throws IOException, GoogleAuthException {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createSimpleTrack(200, false);
    instrumentation.waitForIdleSync();
    checkSendTrackToGoogle();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.activityType = EndToEndTestUtils.DEFAULTACTIVITYTYPE;
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}