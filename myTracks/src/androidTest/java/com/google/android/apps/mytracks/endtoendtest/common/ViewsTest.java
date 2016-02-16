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

import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.fragments.ChartFragment;
import com.google.android.apps.mytracks.fragments.MyTracksMapFragment;
import com.google.android.apps.mytracks.fragments.StatsFragment;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.Display;
import android.view.View;

import junit.framework.Assert;

/**
 * Tests switching views and the menu list of each view.
 * 
 * @author Youtao Liu
 */
public class ViewsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public ViewsTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Switches tab from {@link MyTracksMapFragment} to {@link ChartFragment},
   * then to {@link StatsFragment}, and then to {@link MyTracksMapFragment}. In
   * each tab, checks menu items like settings and map layer.
   */
  public void testSwitchTabAndViewMenu() {
    EndToEndTestUtils.createTrackIfEmpty(3, true);

    EndToEndTestUtils.SOLO.scrollUp();
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.trackName, 1, true);

    // Switch to chart view
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.rotateCurrentActivity();
    assertTrue(
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_map_layer), false));

    // Switch to stats view
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    assertTrue(
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_map_layer), false));

    // Switch to map view
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.track_detail_map_tab));
    EndToEndTestUtils.rotateCurrentActivity();
    assertTrue(
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), false));
    assertTrue(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_map_layer), false));
  }

  /**
   * Test selecting map layer.
   */
  public void testMapLayer() {
    EndToEndTestUtils.createTrackIfEmpty(1, false);

    selectMapLayer(R.string.menu_terrain);
    selectMapLayer(R.string.menu_satellite_with_streets);    
    selectMapLayer(R.string.menu_satellite);
    selectMapLayer(R.string.menu_map);
  }
  
  /**
   * Tests the track controller position in landscape view and portrait view on
   * {@link TrackListActivity} and {@link TrackDetailActivity}.
   */
  public void testTrackControllerPostion() {
    checkTrackControllerPosition();

    EndToEndTestUtils.rotateCurrentActivity();
    instrumentation.waitForIdleSync();
    checkTrackControllerPosition();

    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    checkTrackControllerPosition();

    EndToEndTestUtils.rotateCurrentActivity();
    instrumentation.waitForIdleSync();
    checkTrackControllerPosition();

    EndToEndTestUtils.stopRecording(true);
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    checkTrackControllerPosition();

    EndToEndTestUtils.rotateCurrentActivity();
    instrumentation.waitForIdleSync();
    checkTrackControllerPosition();
  }

  /**
   * Selects a map layer
   * 
   * @param id the map layer id
   */
  private void selectMapLayer(int id) {

    // Make sure can see the map tab
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.track_detail_map_tab));
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_map_layer), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(id));
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
  }

  /**
   * Checks the position of track controller in landscape view and portrait
   * view.
   */
  @SuppressWarnings("deprecation")
  private void checkTrackControllerPosition() {
    View controller = EndToEndTestUtils.SOLO.getCurrentActivity()
        .findViewById(R.id.track_controler_container);
    Display display = EndToEndTestUtils.SOLO.getCurrentActivity()
        .getWindowManager().getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();

    if (width > height) {
      // In landscape view
      Assert.assertTrue(controller.getWidth() < controller.getHeight());
      Assert.assertTrue(controller.getTop() < height / 2);
      Assert.assertTrue(controller.getRight() < width / 2);
    } else {
      // In portrait view
      Assert.assertTrue(controller.getWidth() > controller.getHeight());
      Assert.assertTrue(controller.getTop() > height / 2);
      Assert.assertTrue(controller.getRight() > width / 2);
    }
  }
}