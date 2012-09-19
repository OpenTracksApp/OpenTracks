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

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
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

  @TargetApi(8)
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
   * menus in these views. In MapFragment, the menu should contain satellite/map
   * mode. In ChartFragment and StatsFragment, the menu should not contain
   * satellite/map mode.
   */
  public void testSwitchViewsAndMenusOfView() {
    EndToEndTestUtils.createTrackIfEmpty(3, true);
    
    EndToEndTestUtils.SOLO.scrollUp();
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName, 1, true);

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.rotateAllActivities();

    assertTrue(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings),
        false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_satellite_mode), false));
    assertFalse(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_map_mode),
        false));

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings),
        false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_satellite_mode), false));
    assertFalse(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_map_mode),
        false));

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_map_tab));
    EndToEndTestUtils.rotateAllActivities();
    assertTrue(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings),
        false));
    assertTrue(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_satellite_mode), false)
        || EndToEndTestUtils
            .findMenuItem(activityMyTracks.getString(R.string.menu_map_mode), false));
  }

  /**
   * Tests the switch between satellite mode and map mode.
   */
  public void testSatelliteAndMapView() {
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    // Check current mode.
    boolean isMapMode = true;
    // If can not find switch menu in top menu, click More menu.
    if (!EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_satellite_mode),
        false)) {
      isMapMode = false;
    }
    EndToEndTestUtils.showMenuItem();
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
    assertTrue(EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_help), false));

    // Switch back.
    EndToEndTestUtils.showMenuItem();
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
        0, EndToEndTestUtils.SHORT_WAIT_TIME);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
