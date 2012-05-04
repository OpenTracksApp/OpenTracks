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
import com.google.android.apps.mytracks.fragments.MapFragment;
import com.google.android.maps.MapView;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Tests switching views and the menu list of each view.
 * 
 * @author Youtao Liu
 */
public class ViewsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public ViewsTest() {
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
   * Switches view from {@link MapFragment} to @ ChartFragment} , then changes
   * to @ StatsFragment} . Finally back to {@link MapFragment}. And check some
   * menus in these views.
   */
  public void testSwitchViewsAndMenusOfView() {
    EndToEndTestUtils.SOLO.sendKey(KeyEvent.KEYCODE_MENU);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_delete_all), true,
        false);
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    instrumentation.waitForIdleSync();

    EndToEndTestUtils.createSimpleTrack(3);

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.TRACK_NAME);

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.rotateAllActivities();
    sendKeys(KeyEvent.KEYCODE_MENU);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_chart_settings), true,
        false);
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks
        .getString(R.string.chart_settings_by_distance)));
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    sendKeys(KeyEvent.KEYCODE_MENU);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_satellite_mode), false,
        false);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true,
        false);
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks
        .getString(R.string.sensor_state_last_sensor_time)));
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_map_tab));
    sendKeys(KeyEvent.KEYCODE_MENU);
    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_my_location), false, false));
  }

  /**
   * Tests the switch between satellite mode and map mode.
   */
  public void testSatelliteAndMapView() {
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    // Check current mode.
    boolean isMapMode = true;
    sendKeys(KeyEvent.KEYCODE_MENU);
    // If can not find switch menu in top menu, click More menu.
    if (!EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_satellite_mode),
        false, false)) {
      isMapMode = false;
    }
    // Switch to satellite mode if it's map mode now..
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(isMapMode ? R.string.menu_satellite_mode : R.string.menu_map_mode));

    isMapMode = !isMapMode;

    ArrayList<View> allViews = EndToEndTestUtils.SOLO.getViews();
    for (View view : allViews) {
      if (view instanceof MapView) {
        assertEquals(isMapMode, !((MapView) view).isSatellite());
      }
    }
    instrumentation.waitForIdleSync();

    EndToEndTestUtils.SOLO.sendKey(KeyEvent.KEYCODE_MENU);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_help), false, false);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();

    // Switch back.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(isMapMode ? R.string.menu_satellite_mode : R.string.menu_map_mode));
    isMapMode = !isMapMode;
    allViews = EndToEndTestUtils.SOLO.getViews();
    for (View view : allViews) {
      if (view instanceof MapView) {
        assertEquals(isMapMode, !((MapView) view).isSatellite());
      }
    }

    // Wait for the TrackDetailActivity, or the stop button will can not be
    // found in next step.
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.track_detail_chart_tab),
        0, 1000);
    EndToEndTestUtils.stopRecording(true);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
