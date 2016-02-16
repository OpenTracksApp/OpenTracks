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
import android.widget.CheckBox;

import junit.framework.Assert;

/**
 * Tests the settings.
 * 
 * @author Youtao Liu
 */
public class SettingsTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final String GMAIL_COM = "gmail.com";
  private static final String NEW_ACTIVITY_TYPE = "NewActivityType";

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public SettingsTest() {
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
   * Tests changing preferred units to imperial.
   */
  public void testPreferredUnits_imperial() {
    testPreferredUnits(R.string.unit_mile);
  }

  /**
   * Tests changing preferred units to metric.
   */
  public void testPreferredUnits_metric() {
    testPreferredUnits(R.string.unit_kilometer);
  }

  /**
   * Tests changing preferred units while recording and viewing the chart tab.
   */
  public void testPreferredUnitsRecording_chart() {
    testPreferredUnitsRecording(R.string.track_detail_chart_tab);
  }

  /**
   * Tests changing preferred units while recording and viewing the stats tab.
   */
  public void testPreferredUnitsRecording_stats() {
    testPreferredUnitsRecording(R.string.track_detail_stats_tab);
  }

  /**
   * Tests changing preferred units while recording and viewing the map tab.
   */
  public void testPreferredUnitsRecording_map() {
    testPreferredUnitsRecording(R.string.track_detail_map_tab);
  }

  /**
   * Tests grade/elevation and latitude/longitude..
   */
  public void testGradeElevationAndLatitudeLongitude() {
    // Ennable grade/elevation and latitude/longitude
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.settings_stats_units_title)));
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_stats_grade_elevation));
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_stats_coordinate));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    // Start recording and check stats tab
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.stats_elevation), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME, true));
    assertTrue(
        EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.stats_grade)));
    assertTrue(
        EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.stats_latitude)));
    assertTrue(
        EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.stats_longitude)));

    // Stop recording and check stats tab
    EndToEndTestUtils.stopRecording(true);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.stats_elevation), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME, true));
    assertTrue(
        EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.stats_grade)));
    assertNull(
        EndToEndTestUtils.findTextView(trackListActivity.getString(R.string.stats_latitude)));
    assertNull(
        EndToEndTestUtils.findTextView(trackListActivity.getString(R.string.stats_longitude)));
  }

  /**
   * Tests default track name and default activity type.
   */
  public void testDefaultTrackNameAndDefaultActivityType() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_recording));

    // Change the default track name to number
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_recording_track_name_title));
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_recording_track_name_number_option));

    // Change the default activity type to NEW_ACTIVITY_TYPE
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_recording_default_activity_title));
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, NEW_ACTIVITY_TYPE);
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.stopRecording(false);
    assertTrue(EndToEndTestUtils.SOLO.searchText(
        trackListActivity.getString(R.string.track_name_format).split(" ")[0]));
    assertTrue(EndToEndTestUtils.SOLO.searchText(NEW_ACTIVITY_TYPE));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.generic_save));
  }

  /**
   * Tests track color setting for fixed thresholds.
   */
  public void testTrackColor_fixed() {
    testTrackColor(R.string.settings_map_track_color_mode_fixed,
        R.string.settings_map_track_color_mode_fixed_summary);
  }

  /**
   * Tests track color setting for dynamic thresholds.
   */
  public void testTrackColorSettings_dynamic() {
    testTrackColor(R.string.settings_map_track_color_mode_dynamic,
        R.string.settings_map_track_color_mode_dynamic_summary);
  }

  /**
   * Tests track color setting for single color.
   */
  public void testTrackColorSettings_single() {
    testTrackColor(R.string.settings_map_track_color_mode_single,
        R.string.settings_map_track_color_mode_single);
  }

  /**
   * Tests allow access.
   */
  public void testAllowAccess() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_advanced));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.settings_sharing_allow_access), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME));
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.settings_sharing_allow_access_confirm_message), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME));
    EndToEndTestUtils.SOLO.clickOnButton(trackListActivity.getString(R.string.generic_yes));
    instrumentation.waitForIdleSync();
    assertEquals(true, EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class).get(0).isChecked());
    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Tests sync.
   */
  public void testSync() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.menu_sync_drive));

    // Whether there is a gmail.com account
    if (!EndToEndTestUtils.SOLO.waitForText(GMAIL_COM, 1, EndToEndTestUtils.TINY_WAIT_TIME)) {
      return;
    }
    // Click the first gmail.com account
    EndToEndTestUtils.SOLO.clickOnText(GMAIL_COM, 0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.getButtonOnScreen(
        trackListActivity.getString(R.string.generic_ok), true, true);

    EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.sync_drive_confirm_message).split("%")[0], 1,
        EndToEndTestUtils.SHORT_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.generic_no));
    EndToEndTestUtils.SOLO.goBack();
    assertEquals(false,
        EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_sync_now), false));
  }

  /**
   * Tests preferred units.
   * 
   * @param unitId the unit id
   */
  private void testPreferredUnits(int unitId) {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    Assert.assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.settings_stats_units_title)));

    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_stats_units_title));
    instrumentation.waitForIdleSync();
    String unit = trackListActivity.getString(unitId);
    EndToEndTestUtils.SOLO.clickOnText(unit);
    EndToEndTestUtils.SOLO.searchText(unit);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Tests preferred units while recording.
   * 
   * @param tabId the tab id
   */
  private void testPreferredUnitsRecording(int tabId) {
    testPreferredUnits(R.string.unit_kilometer);

    EndToEndTestUtils.startRecording();

    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(tabId));
    EndToEndTestUtils.sendGps(3);
    testPreferredUnits(R.string.unit_mile);
    EndToEndTestUtils.sendGps(3, 3);
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Tests track color setting.
   * 
   * @param modeId the mode id
   * @param summaryId the summary id
   */
  private void testTrackColor(int modeId, int summaryId) {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.menu_map));

    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_map_track_color_mode));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(modeId));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(summaryId));
  }
}
