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

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Tests some menu items of MyTracks.
 * 
 * @author Youtao Liu
 */
public class MenuItemsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  private final static String SHARE_ITEM_PARENT_VIEW_NAME = "RecycleListView";

  @TargetApi(8)
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
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_total_distance));
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
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.help_about), true, true);
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
  }
  
  /**
   * Tests search menu item.
   */
  public void testSearch() {
    EndToEndTestUtils.createSimpleTrack(1);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_search), true);
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.trackName);
    sendKeys(KeyEvent.KEYCODE_ENTER);
    instrumentation.waitForIdleSync();
    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentListViews().size());
  }
  
  /**
   * Tests the share menu item. This test to check whether crash will happen during the share.
   */
  public void testShareActivity() {
    // Try all share items.
    for (int i = 0;; i++) {
      EndToEndTestUtils.createTrackIfEmpty(0, false);
      View oneItemView = findShareItem(i);
      if (oneItemView == null) {
        break;
      }
      EndToEndTestUtils.SOLO.clickOnView(oneItemView);
      EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true,
          true);
      if(!GoogleUtils.isAccountAvailable()) {
        break;
      }
      // Waiting the send is finish.
      while (EndToEndTestUtils.SOLO.waitForText(
          activityMyTracks.getString(R.string.generic_progress_title), 1,
          EndToEndTestUtils.SHORT_WAIT_TIME)) {}
      
      // Check whether data is correct on Google Map and then delete it.
      assertTrue(GoogleUtils.deleteMap(EndToEndTestUtils.trackName, activityMyTracks));

      // Display the MyTracks activity for the share item may startup other
      // applications.
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setClass(activityMyTracks.getApplicationContext(), TrackListActivity.class);
      activityMyTracks.getApplicationContext().startActivity(intent);
      EndToEndTestUtils.sleep(EndToEndTestUtils.NORMAL_WAIT_TIME);
    }
  }

  /**
   * Gets the view to click the share item by item index.
   * 
   * @param index of a share item
   * @return null when no such item
   */
  private View findShareItem(int index) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_share), true);
    ArrayList<View> views = EndToEndTestUtils.SOLO.getViews();
    int i = 0;
    for (View view : views) {
      String name = view.getParent().getClass().getName();
      // Each share item is a child of a "RecycleListView"
      if (name.indexOf(SHARE_ITEM_PARENT_VIEW_NAME) > 0) {
        if (index == i) {
          return view;
        }
        i++;
      }
    }
    return null;
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
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.stopRecording(true);
    
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_voice_frequency), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_split_frequency), false));
    
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(R.string.menu_split_frequency), 1, true, true));
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
