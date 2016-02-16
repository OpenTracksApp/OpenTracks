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
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.widget.ListView;

import java.util.Locale;

/**
 * Tests menu items.
 * 
 * @author Youtao Liu
 */
public class MenuItemsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public MenuItemsTest() {
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
   * Tests following More menu items.
   * <ul>
   * <li>Aggregated statistics in {@link TrackListActivity}</li>
   * <li>Help & feedback in {@link TrackListActivity}</li>
   * <li>Help & feedback in {@link TrackDetailActivity}</li>
   * <li>Export in {@link TrackDetailActivity}</li>
   * </ul>
   */
  public void testMoreMenuItems() {
    // Aggregated statistics in TrackListActivity
    EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_aggregated_statistics), true);
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.stats_distance));
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();

    // Help & feedback in TrackListActivity
    checkHelpFeedbackPage();

    EndToEndTestUtils.createSimpleTrack(1, false);
    instrumentation.waitForIdleSync();

    // Help & feedback in TrackDetailActivity
    checkHelpFeedbackPage();

    // Export in TrackDetailActivity
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_export), true);
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.export_title));
  }

  /**
   * Tests the search menu. When search is displayed, the track controller
   * should be hidden.
   */
  public void testSearchMenu() {
    EndToEndTestUtils.deleteAllTracks();
    EndToEndTestUtils.createSimpleTrack(0, true);
    assertTrue(isTrackControllerShown());

    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_search), true);
    assertFalse(isTrackControllerShown());

    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.trackName);
    sendKeys(KeyEvent.KEYCODE_ENTER);
    instrumentation.waitForIdleSync();
    assertEquals(1, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());
    // TODO: not sure why the following fails. It passes in debug mode.
    // assertFalse(isTrackControllerShown());

    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
    assertTrue(isTrackControllerShown());
  }

  /**
   * Checks the voice frequency and the split frequency menu. When recording,
   * they should be in both the {@link TrackDetailActivity} menu and the
   * recording settings. When not recording, they should only be in the
   * recording settings.
   */
  public void testFrequencyMenu() {
    EndToEndTestUtils.startRecording();

    assertTrue(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_voice_frequency), false));
    assertTrue(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_split_frequency), false));

    checkFrequencyInSettings();

    EndToEndTestUtils.stopRecording(true);

    assertFalse(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_voice_frequency), false));
    assertFalse(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_split_frequency), false));

    checkFrequencyInSettings();
  }

  /**
   * Tests start and stop gps.
   */
  public void testGpsMenu() {
    // Start gps
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_start_gps), true);
    EndToEndTestUtils.waitTextToDisappear(trackListActivity.getString(R.string.menu_start_gps));
    assertTrue(
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_stop_gps), false));

    // Stop gps
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_stop_gps), true);
    EndToEndTestUtils.waitTextToDisappear(trackListActivity.getString(R.string.menu_stop_gps));
    assertTrue(EndToEndTestUtils.findMenuItem(
        trackListActivity.getString(R.string.menu_start_gps), false));
  }

  /**
   * Checks the help & feedback page.
   */
  private void checkHelpFeedbackPage() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_help_feedback), true);
    assertNotNull(EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.menu_feedback).toUpperCase(Locale.getDefault()), true,
        false));
    EndToEndTestUtils.SOLO.goBack();
    instrumentation.waitForIdleSync();
  }

  /**
   * Returns true if the track controller is shown.
   */
  private boolean isTrackControllerShown() {
    return trackListActivity.findViewById(R.id.track_controler_container).isShown();
  }

  /**
   * Checks that the frequency options exists in settings.
   */
  private void checkFrequencyInSettings() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_recording));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        trackListActivity.getString(R.string.menu_voice_frequency), 1, true, true));
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        trackListActivity.getString(R.string.menu_split_frequency), 1, true, true));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }
}
