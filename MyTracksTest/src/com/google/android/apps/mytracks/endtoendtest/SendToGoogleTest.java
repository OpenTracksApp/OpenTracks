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

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.data.Entry;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckBox;

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

  @TargetApi(15)
  public SendToGoogleTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Check all services and send to google.
   */
  public void testCreateAndSendTrack_send() {
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    checkSendTrackToGoogle();
  }

  /**
   * Check all services and send to google.
   */
  public void testCreateAndSendTrack_sendPausedTrack() {
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
    EndToEndTestUtils.createSimpleTrack(1, false);
    sendToGoogle();
    sendToGoogle();

    List<Entry> spreadsheetEntry = GoogleUtils.searchAllSpreadsheetByTitle(GoogleUtils.DOCUMENT_NAME_PREFIX + "-"
        + EndToEndTestUtils.activityType, activityMyTracks);
    assertEquals(1, spreadsheetEntry.size());
  }

  /**
   * Checks the process of sending track to google.
   */
  private void checkSendTrackToGoogle() {
    if (!sendToGoogle()) {
      return;
    }
    // Check whether all data is correct on Google Map, Documents, and
    // Spreadsheet.
    assertTrue(GoogleUtils.deleteMap(EndToEndTestUtils.trackName, activityMyTracks));
    assertTrue(GoogleUtils.searchFusionTableByTitle(EndToEndTestUtils.TRACK_NAME_PREFIX,
        activityMyTracks));
    assertTrue(GoogleUtils.deleteTrackInSpreadSheet(EndToEndTestUtils.trackName, activityMyTracks));
    assertTrue(GoogleUtils.dropFusionTables(EndToEndTestUtils.trackName, activityMyTracks));
  }

  /**
   * Sends a track to Google.
   * 
   * @return true means send successfully
   */
  private boolean sendToGoogle() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_send_google), true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.send_google_title));
    ArrayList<CheckBox> checkBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    for (int i = 0; i < checkBoxs.size(); i++) {
      if (!checkBoxs.get(i).isChecked()) {
        EndToEndTestUtils.SOLO.clickOnCheckBox(i);
      }
    }

    if (checkBoxs.size() < 3) {
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
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.send_google_send_now),
        true, true);

    if (!GoogleUtils.isAccountAvailable()) {
      return false;
    }

    // Following check the process of "Send to Google".
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.generic_progress_title)));
    // Waiting the send is finish.
    while (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.generic_progress_title), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {}

    // Check whether the result dialog is display.
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.share_track_share_url)));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    return true;
  }
  
  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.activityType = EndToEndTestUtils.DEFAULTACTIVITYTYPE;
    super.tearDown();
  }

}