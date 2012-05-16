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

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.widget.CheckBox;

import java.util.ArrayList;

/**
 * Tests creating a track with markers, and editing and sending send a track.
 * 
 * @author Youtao Liu
 */
public class CreateAndSendTrackTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final String WAYPOINT_NAME = "testWaypoint";
  private static final String RELATIVE_STARTTIME_POSTFIX = "mins ago";
  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public CreateAndSendTrackTest() {
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
   * Creates a track with markers and the setting to send to google.
   */
  public void testCreateAndSendTrack() {
    if (EndToEndTestUtils.isTrackListEmpty(true)) {
      // Create a simple track.
      EndToEndTestUtils.createSimpleTrack(1);
      assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.TRACK_NAME));
    }
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_send_google), true,
        false);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.send_google_title));
    EndToEndTestUtils.rotateCurrentActivity();
    instrumentation.waitForIdleSync();

    ArrayList<CheckBox> checkBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    for (int i = 0; i < checkBoxs.size(); i++) {
      if (checkBoxs.get(i).isChecked()) {
        EndToEndTestUtils.SOLO.clickOnCheckBox(i);
      }
    }

    if (checkBoxs.size() < 3) {
      EndToEndTestUtils.SOLO.scrollDown();
      checkBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
      for (int i = 0; i < checkBoxs.size(); i++) {
        if (checkBoxs.get(i).isChecked()) {
          EndToEndTestUtils.SOLO.clickOnCheckBox(i);
        }
      }
    }

    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.send_google_send_now));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.send_google_no_service_selected)));
    // Stop here for do not really send.
  }

  /**
   * Tests editing a track.
   */
  public void testEditTrack() {
    if (EndToEndTestUtils.isTrackListEmpty(true)) {
      // Create a simple track.
      EndToEndTestUtils.createSimpleTrack(0);
    }
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_edit), true, false);

    String newTrackName = EndToEndTestUtils.TRACK_NAME_PREFIX + "_new" + System.currentTimeMillis();
    String newType = "type" + newTrackName;
    String newDesc = "desc" + newTrackName;

    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    sendKeys(KeyEvent.KEYCODE_DEL);
    EndToEndTestUtils.SOLO.enterText(0, newTrackName);
    sendKeys(KeyEvent.KEYCODE_TAB);
    EndToEndTestUtils.SOLO.enterText(1, newType);
    sendKeys(KeyEvent.KEYCODE_TAB);
    EndToEndTestUtils.SOLO.enterText(2, newDesc);

    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();
    // Go back to track list.
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(newTrackName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(newDesc));
  }

  /**
   * Creates one track with a two locations, a way point and a statistics point.
   */
  public void testCreateTrackWithMarkers() {
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    // Send Gps before send marker.
    EndToEndTestUtils.sendGps(2);
    if (EndToEndTestUtils.HAS_ACTIONBAR) {
      // Check the title is Recording.
      assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks
          .getString(R.string.icon_recording)));
    }
    createWaypointAndSplit();
    EndToEndTestUtils.sendGps(2);
    // Back to tracks list.
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.stopRecording(false);
    EndToEndTestUtils.TRACK_NAME = EndToEndTestUtils.TRACK_NAME_PREFIX + System.currentTimeMillis();
    EndToEndTestUtils.SOLO.enterText(0, EndToEndTestUtils.TRACK_NAME);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));

    instrumentation.waitForIdleSync();
    // Check the new track
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.TRACK_NAME, 1, 5000, true,
        false));
  }

  /**
   * Tests the display of track start time. Use relative time (x mins ago) for
   * the start time if it is within the past week. But only show the start time
   * if it is different from the track name.
   */
  public void testTrackStartTime() {
    // Delete all track first.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true,
        false);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();
    // Test should not show relative time.
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.stopRecording(false);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    assertFalse(EndToEndTestUtils.SOLO.waitForText(RELATIVE_STARTTIME_POSTFIX, 1, 500));

    // Test should show relative time.
    EndToEndTestUtils.createSimpleTrack(2);
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(RELATIVE_STARTTIME_POSTFIX, 1, 500));

  }

  /**
   * Creates a way point and a split maker during track recording.
   */
  private void createWaypointAndSplit() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_markers), true, true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.marker_list_empty_message));
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_insert_marker), true,
        true);
    // Rotate when show insert page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.enterText(0, WAYPOINT_NAME);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_add));
    assertTrue(EndToEndTestUtils.SOLO.searchText(WAYPOINT_NAME));

    // TODO Add test when Split maker feature is finished.
    EndToEndTestUtils.SOLO.goBack();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
