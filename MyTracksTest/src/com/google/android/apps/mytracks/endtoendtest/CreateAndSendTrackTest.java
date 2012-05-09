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
  private static final String STATISTICS_NAME = "testStatistics";
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
    // Create a track at first.
    EndToEndTestUtils.createSimpleTrack(1);
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
    EndToEndTestUtils.createSimpleTrack(0);
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

    assertTrue(EndToEndTestUtils.SOLO.searchText(newTrackName));
    assertTrue(EndToEndTestUtils.SOLO.searchText(newDesc));
  }

  /**
   * Creates one track with a two locations, a way point and a statistics point.
   */
  public void testCreateTrackWithMarkers() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true,
        false);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();

    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.sendGps(2);
    createWaypointAndStatistics();
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
   * Creates a way point and a statistics during track recording.
   */
  private void createWaypointAndStatistics() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_markers), true,
        true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_insert_marker), true,
        true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.marker_waypoint));
    // Rotate when show insert page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_more_options));
    EndToEndTestUtils.SOLO.enterText(0, WAYPOINT_NAME);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_add));
    assertTrue(EndToEndTestUtils.SOLO.searchText(WAYPOINT_NAME));


    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_insert_marker), true,
        true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.marker_statistics));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_more_options));
    // Rotate when show option page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.enterText(0, STATISTICS_NAME);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_add));
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(STATISTICS_NAME));

    EndToEndTestUtils.SOLO.goBack();
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
