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

import com.google.android.apps.mytracks.ChartView;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.GoogleUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Tests the setting of MyTracks.
 * 
 * @author Youtao Liu
 */
public class SettingTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  public SettingTest() {
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
   * Changes some settings and then reset them. This test case tests three
   * points:
   * <ul>
   * <li>Resets all settings.</li>
   * <li>Changes the setting of Display.</li>
   * <li>Changes the setting of Sharing.</li>
   * </ul>
   */
  public void testChangeAndReset() {
    // Reset all settings.
    EndToEndTestUtils.resetAllSettings(activityMyTracks, true);
    // Change a setting of Map.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    instrumentation.waitForIdleSync();

    // Rotate on a sub setting page.
    EndToEndTestUtils.rotateCurrentActivity();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title));
    ArrayList<CheckBox> displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class);
    boolean useMetric = displayCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();

    // Change a setting of sharing.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_sharing_allow_access), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME));
    ArrayList<CheckBox> sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class);
    boolean newMapsPublic = sharingCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_sharing_allow_access_confirm_message), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME));
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_yes));
    instrumentation.waitForIdleSync();
    assertEquals(!newMapsPublic, EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class).get(0)
        .isChecked());
    EndToEndTestUtils.SOLO.goBack();

    // Reset all settings.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_reset));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset_summary));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset_done), 1,
        EndToEndTestUtils.SHORT_WAIT_TIME);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();

    // Check settings.
    // Add following scroll up for a bug of Robotium.
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.track_detail_stats_tab),
        1, EndToEndTestUtils.NORMAL_WAIT_TIME);
    EndToEndTestUtils.SOLO.scrollUp();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_stats_units_title), 1,
        EndToEndTestUtils.LONG_WAIT_TIME));
    displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class);
    Log.i(EndToEndTestUtils.LOG_TAG, "useMetric:" + useMetric);
    assertEquals(useMetric, displayCheckBoxs.get(0).isChecked());

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_sharing_allow_access));
    sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class);
    assertEquals(newMapsPublic, sharingCheckBoxs.get(0).isChecked());
  }

  /**
   * Tests the change of preferred units.
   */
  public void testChangePreferredUnits() {
    EndToEndTestUtils.createTrackIfEmpty(5, false);

    // Change it and verify it.
    ChangePreferredUnits();

    // Change it back and verify it.
    ChangePreferredUnits();
  }

  /**
   * Tests the change of stats settings during recording on chart view.
   */
  public void testChangeStatsSettings_underRecording_chart() {
    EndToEndTestUtils.startRecording();

    // Test just change preferred units when display CHART tab.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    EndToEndTestUtils.sendGps(3);
    ChangeStatsSettings(true, false, false, false);
    EndToEndTestUtils.sendGps(3, 3);
    EndToEndTestUtils.stopRecording(true);

  }

  /**
   * Tests the change of stats settings during recording on stats tab.
   */
  public void testChangeStatsSettings_underRecording_stats() {
    EndToEndTestUtils.startRecording();
    // Test change preferred units and preferred rate when display STATS tab.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    EndToEndTestUtils.sendGps(3);
    ChangeStatsSettings(true, true, false, false);
    EndToEndTestUtils.sendGps(3, 3);
    EndToEndTestUtils.stopRecording(true);

  }

  /**
   * Tests displaying elevation, grade and latitude and longitude information in
   * Stats tab.
   */
  public void testChangeStatsSettings_showExtraInfos() {
    EndToEndTestUtils.resetAllSettings(activityMyTracks, false);
    ChangeStatsSettings(false, false, true, true);
    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.stats_elevation), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME, true));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_grade)));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.stats_latitude)));

    EndToEndTestUtils.stopRecording(true);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.stats_elevation), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME, true));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.stats_grade)));
    assertFalse(EndToEndTestUtils.SOLO.searchText(activityMyTracks
        .getString(R.string.stats_latitude)));
  }

  /**
   * Changes settings of preferred units and preferred rate.
   * 
   * @param changeUnits is change the preferred unit
   * @param changeRate is change the preferred rate
   * @param changeGradeElevation is change the grade and elevation
   * @param changeLatLong is change the latitude and longitude
   */
  private void ChangeStatsSettings(boolean changeUnits, boolean changeRate,
      boolean changeGradeElevation, boolean changeLatLong) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title)));

    // Change preferred units.
    if (changeUnits) {
      boolean isImperial = EndToEndTestUtils.findTextViewInView(
          activityMyTracks.getString(R.string.settings_stats_units_imperial),
          EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0)) != null;

      // Change preferred units.
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_units_title));
      if (isImperial) {
        EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.unit_kilometer));
      } else {
        EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.unit_feet));
      }
    }
    if (changeRate) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_rate_title));
    }
    if (changeGradeElevation) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_grade_elevation));
    }
    if (changeLatLong) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_coordinate));
    }
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Changes PreferredUnits and verifies the change.
   */
  private void ChangePreferredUnits() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title)));

    boolean isImperial = EndToEndTestUtils.findTextViewInView(activityMyTracks
        .getString(R.string.settings_stats_units_imperial),
        EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0)) != null;

    // Change preferred units.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_stats_units_title));
    if (isImperial) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.unit_kilometer));
    } else {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.unit_feet));
    }
    isImperial = !isImperial;
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_chart_tab));
    instrumentation.waitForIdleSync();
    ChartView chartView = EndToEndTestUtils.getChartView();
    assertEquals(!isImperial, chartView.isMetricUnits());
  }

  /**
   * Tests the backup.
   */
  public void testBackup() {
    // Delete all backup at first.
    EndToEndTestUtils.deleteExportedFiles(EndToEndTestUtils.BACKUPS);
    EndToEndTestUtils.createTrackIfEmpty(3, true);

    // Write to SD card.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_reset));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_now));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_backup_now_success), 0,
        EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
    instrumentation.waitForIdleSync();

    // Delete all tracks.
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.deleteAllTracks();
    instrumentation.waitForIdleSync();

    // Read from SD card.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_reset));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_now));
    EndToEndTestUtils.SOLO
        .clickOnText(activityMyTracks.getString(R.string.settings_backup_restore));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);

    // Now there should be 2 backups.
    instrumentation.waitForIdleSync();
    assertEquals(2, EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount());

    // Click the first one.
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class)
        .get(0).getChildAt(0));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_backup_restore_success), 0,
        EndToEndTestUtils.SUPER_LONG_WAIT_TIME));

    // Check restore track.
    instrumentation.waitForIdleSync();
    boolean isContainTrack = EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackName);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_reset));
    EndToEndTestUtils.SOLO
        .clickOnText(activityMyTracks.getString(R.string.settings_backup_restore));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_yes), true,
        true);
    instrumentation.waitForIdleSync();

    // Click the second one.
    EndToEndTestUtils.SOLO.clickOnView(EndToEndTestUtils.SOLO.getCurrentViews(ListView.class)
        .get(0).getChildAt(1));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_backup_restore_success), 0,
        EndToEndTestUtils.SUPER_LONG_WAIT_TIME));

    // For the older backup contains tracks and the newer backup is empty. The
    // search result of twice must be different.
    instrumentation.waitForIdleSync();
    assertTrue(isContainTrack != EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackName));
  }

  /**
   * Tests the setting of recording. This test changes the default name and
   * activity of tracks, and the creates a new track to check it.
   */
  public void testRecording() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_recording));

    // Changes the setting of recording name.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_recording_track_name_title));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_recording_track_name_number_option));

    // Changes the setting of default activity.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_recording_default_activity_title));
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.activityType);
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();

    EndToEndTestUtils.startRecording();
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.stopRecording(false);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.searchText(activityMyTracks.getString(
        R.string.track_name_format).split(" ")[0]));
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.activityType));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_save));
  }

  /**
   * Tests the setting of fixed track color settings.
   */
  public void testTrackColorSettings_fixed() {
    Context context = activityMyTracks.getApplicationContext();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.menu_map));
    String errorString = "error";

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_fixed));
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_dynamic_value));
    assertEquals(activityMyTracks.getString(R.string.settings_map_track_color_mode_fixed_value),
        PreferencesUtils.getString(context, R.string.track_color_mode_key, errorString));
  }

  /**
   * Tests the setting of dynamic track color settings.
   */
  public void testTrackColorSettings_dynamic() {
    Context context = activityMyTracks.getApplicationContext();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.menu_map));
    String errorString = "error";

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_dynamic));
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_dynamic_value));
    assertEquals(activityMyTracks.getString(R.string.settings_map_track_color_mode_dynamic_value),
        PreferencesUtils.getString(context, R.string.track_color_mode_key, errorString));
  }

  /**
   * Tests the setting of single track color settings.
   */
  public void testTrackColorSettings_single() {
    Context context = activityMyTracks.getApplicationContext();
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.menu_map));
    String errorString = "error";
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode));
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_single));
    EndToEndTestUtils.waitTextToDisappear(activityMyTracks
        .getString(R.string.settings_map_track_color_mode_dynamic_value));
    assertEquals(activityMyTracks.getString(R.string.settings_map_track_color_mode_single_value),
        PreferencesUtils.getString(context, R.string.track_color_mode_key, errorString));
  }

  /**
   * Tests the setting and menu item of sync.
   */
  public void testSyncNowMenu() {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_google));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_google_account_title));

    // Whether test account is bound.
    if (EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_1, 1,
        EndToEndTestUtils.TINY_WAIT_TIME)) {
      EndToEndTestUtils.SOLO.clickOnText(GoogleUtils.ACCOUNT_NAME_1);
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.menu_sync_drive));
      assertTrue(EndToEndTestUtils.findTextView(
          activityMyTracks.getString(R.string.menu_sync_drive)).isEnabled());
    } else {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.value_none));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.menu_sync_drive));
      assertFalse(EndToEndTestUtils.findTextView(
          activityMyTracks.getString(R.string.menu_sync_drive)).isEnabled());
      return;
    }

    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.menu_sync_drive));

    boolean isSyncChecked = false;
    if (EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.sync_drive_confirm_message).split("%")[0], 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)) {
      isSyncChecked = true;
    } else {
      isSyncChecked = false;
    }
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_yes));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    assertEquals(isSyncChecked,
        EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_refresh), false));
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
