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
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_send_google), true);
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
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_edit), true);

    String newTrackName = "T E S T E D I T T R A C K";
    String newType = "T Y P E " + newTrackName;
    String newDesc = "D E S C " + newTrackName;

    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    sendKeys(KeyEvent.KEYCODE_DEL);
    sendKeys(newTrackName);
    sendKeys(KeyEvent.KEYCODE_TAB);
    sendKeys(newType);
    sendKeys(KeyEvent.KEYCODE_TAB);
    sendKeys(newDesc);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));
    instrumentation.waitForIdleSync();
    // Go back to track list.
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    // For all characters will be change to lower case during sendKeys() method, add to lower() method here.
    assertTrue(EndToEndTestUtils.SOLO.searchText(newTrackName.toLowerCase().replaceAll(" ", "")));
    assertTrue(EndToEndTestUtils.SOLO.searchText(newDesc.toLowerCase().replaceAll(" ", "")));
  }

  /**
   * Creates one track with a two locations, a way point and a statistics point.
   */
  public void testCreateTrackWithMarkers() {
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    // Send Gps before send marker.
    EndToEndTestUtils.sendGps(2);
    if (EndToEndTestUtils.hasActionBar) {
      // Check the title is Recording.
      assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks
          .getString(R.string.icon_recording)));
    }
    createWaypoint();
    EndToEndTestUtils.sendGps(2);
    // Back to tracks list.
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.stopRecording(false);
    EndToEndTestUtils.trackName = EndToEndTestUtils.TRACK_NAME_PREFIX + System.currentTimeMillis();
    EndToEndTestUtils.SOLO.enterText(0, EndToEndTestUtils.trackName);
    if(!EndToEndTestUtils.isEmulator) {
      // Close soft keyboard.
      EndToEndTestUtils.SOLO.goBack();
    }
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_save));

    instrumentation.waitForIdleSync();
    // Check the new track
    assertTrue(EndToEndTestUtils.SOLO.waitForText(EndToEndTestUtils.trackName, 1, 5000, true,
        false));
  }

  /**
   * Tests the display of track start time. Use relative time (x mins ago) for
   * the start time if it is within the past week. But only show the start time
   * if it is different from the track name.
   */
  public void testTrackStartTime() {
    // Delete all track first.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true);
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

    // Test should show relative time for createSimpleTrack would save a track
    // name that is different with the start time.
    EndToEndTestUtils.createSimpleTrack(2);
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(RELATIVE_STARTTIME_POSTFIX, 1, 5000));
  }

  /**
   * Creates a way point and a split maker during track recording.
   */
  private void createWaypoint() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_markers), true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.marker_list_empty_message));
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_insert_marker), true);
    // Rotate when show insert page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.enterText(0, WAYPOINT_NAME);
    if(!EndToEndTestUtils.isEmulator) {
      // Close soft keyboard.
      EndToEndTestUtils.SOLO.goBack();
    }
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_add));
    assertTrue(EndToEndTestUtils.SOLO.searchText(WAYPOINT_NAME));

    EndToEndTestUtils.SOLO.goBack();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
