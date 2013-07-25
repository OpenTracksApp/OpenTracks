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
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.widget.ListView;

/**
 * Tests some menu items of MyTracks.
 * 
 * @author Youtao Liu
 */
public class MenuItemsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public MenuItemsTest() {
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
   * Tests following items in More menu.
   * <ul>
   * <li>Tests the aggregated statistics activity.</li>
   * <li>Tests the Sensor state activity.</li>
   * <li>Tests the help menu.</li>
   * </ul>
   */
  public void testSomeMenuItems() {
    // Menu in TrackListActivity.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_aggregated_statistics),
        true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_distance));
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    // Menu in TrackDetailActivity.
    // When there is no sensor connected this menu will be hidden.
    if (EndToEndTestUtils
        .findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true)) {
      EndToEndTestUtils.SOLO.waitForText(activityMyTracks
          .getString(R.string.sensor_state_last_sensor_time));
    }

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_help), true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.help_about), true, true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
  }

  /**
   * Tests search menu item. Checks the display and hide of record controller
   * during search.
   */
  public void testSearch() {
    EndToEndTestUtils.createSimpleTrack(0, true);
    assertTrue(isControllerShown());
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_search), true);
    assertFalse(isControllerShown());
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.trackName);
    sendKeys(KeyEvent.KEYCODE_ENTER);
    instrumentation.waitForIdleSync();
    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).size());
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(isControllerShown());
  }

  /**
   * Gets the status whether is controller is shown.
   * 
   * @return true mean the controller is display and false mean it is disappear
   */
  private boolean isControllerShown() {
    return activityMyTracks.findViewById(R.id.track_controler_container).isShown();
  }

  /**
   * Checks the voice frequency and split frequency menus during recording. When
   * recording, they should be in both the menu and the recording settings. When
   * not recording, they should only be in the recording settings.
   */
  public void testFrequencyMenu() {
    EndToEndTestUtils.startRecording();

    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_voice_frequency), false));
    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_split_frequency), false));

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.stopRecording(true);

    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_voice_frequency), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_split_frequency), false));

    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
  }

  /**
   * Tests starting and stopping GPS.
   */
  public void testGPSMenu() {
    boolean GPSStatus = EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_gps), false);

    // Following starting/stopping or stopping/starting GPS.
    EndToEndTestUtils.findMenuItem(GPSStatus ? activityMyTracks.getString(R.string.menu_stop_gps)
        : activityMyTracks.getString(R.string.menu_start_gps), true);
    GPSStatus = !GPSStatus;
    EndToEndTestUtils.waitTextToDisappear(GPSStatus ? activityMyTracks
        .getString(R.string.menu_start_gps) : activityMyTracks.getString(R.string.menu_stop_gps));
    assertEquals(GPSStatus,
        EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_stop_gps), false));

    EndToEndTestUtils.findMenuItem(GPSStatus ? activityMyTracks.getString(R.string.menu_stop_gps)
        : activityMyTracks.getString(R.string.menu_start_gps), true);
    GPSStatus = !GPSStatus;
    EndToEndTestUtils.waitTextToDisappear(GPSStatus ? activityMyTracks
        .getString(R.string.menu_start_gps) : activityMyTracks.getString(R.string.menu_stop_gps));
    assertEquals(GPSStatus,
        EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_stop_gps), false));
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
