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
import android.widget.EditText;

import java.util.ArrayList;

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
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_total_distance));
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(1, false);
    instrumentation.waitForIdleSync();
    // Menu in TrackDetailActivity.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.sensor_state_last_sensor_time));

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_help), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.help_about));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_ok));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_ok));
  }
  
  /**
   * Tests search menu item.
   */
  public void testSearch() {
    EndToEndTestUtils.createSimpleTrack(1);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.getButtonOnScreen(activityMyTracks
        .getString(R.string.menu_search)));
    ArrayList<EditText> editTexts = EndToEndTestUtils.SOLO.getCurrentEditTexts();
    EndToEndTestUtils.SOLO.enterText(editTexts.get(0), EndToEndTestUtils.trackName);
    sendKeys(KeyEvent.KEYCODE_ENTER);
    instrumentation.waitForIdleSync();
    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentListViews().size());
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
