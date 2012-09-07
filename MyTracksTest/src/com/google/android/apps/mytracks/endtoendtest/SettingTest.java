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

import com.google.android.apps.mytracks.ChartView;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckBox;

import java.util.ArrayList;

/**
 * Tests the setting of MyTracks.
 * 
 * @author Youtao Liu
 */
public class SettingTest extends ActivityInstrumentationTestCase2<TrackListActivity> {
  
  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @TargetApi(8)
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
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title));
    ArrayList<CheckBox> displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    boolean useMetric = displayCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.goBack();
    // Change a setting of sharing.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_sharing_allow_access), 1, EndToEndTestUtils.NORMAL_WAIT_TIME));
    ArrayList<CheckBox> sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    boolean newMapsPublic = sharingCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.settings_sharing_allow_access), 1, EndToEndTestUtils.NORMAL_WAIT_TIME));
    assertEquals(!newMapsPublic, EndToEndTestUtils.SOLO.getCurrentCheckBoxes().get(0).isChecked());
    EndToEndTestUtils.SOLO.goBack();

    // Reset all settings.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    EndToEndTestUtils.getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);

    // Check settings.
    // Add following scroll up for a bug of Robotium.
    EndToEndTestUtils.SOLO.scrollUp();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title), 1, EndToEndTestUtils.LONG_WAIT_TIME));
    displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    assertEquals(useMetric, displayCheckBoxs.get(0).isChecked());

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_sharing_allow_access));
    sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
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
    ChangeStatsSettings(true, false, false, false, false);
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
    ChangeStatsSettings(true, true, false, false, false);
    EndToEndTestUtils.sendGps(3, 3);
    EndToEndTestUtils.stopRecording(true);

  }
  
  /**
   * Tests displaying elevation, grade and latitude and longitude information in Stats tab.
   */
  public void testChangeStatsSettings_showExtraInfos() {
    EndToEndTestUtils.createTrackIfEmpty(5, false);
    EndToEndTestUtils.resetAllSettings(activityMyTracks, true);
    ChangeStatsSettings(false, false, true, true, true);
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.stats_min_elevation), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME, true));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.stats_min_grade)));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.stats_latitude)));
  }
  
  /**
   * Changes settings of preferred units and preferred rate.
   * 
   * @param changeUnits is change the preferred unit
   * @param changeRate is change the preferred rate
   * @param changeElevation is change the elevation
   * @param changeGrade is change the grade
   * @param changeLatLong is change the latitude and longitude
   */
  private void ChangeStatsSettings(boolean changeUnits, boolean changeRate,
      boolean changeElevation, boolean changeGrade, boolean changeLatLong) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_stats_tab));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_stats_units_title)));

    // Change preferred units.
    if (changeUnits) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_units_title));
    }
    if (changeRate) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_rate_title));
    }
    if (changeElevation) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.stats_elevation));
    }
    if (changeGrade) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.settings_stats_grade));
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
        .getString(R.string.settings_stats_units_imperial), EndToEndTestUtils.SOLO
        .getCurrentListViews().get(0)) != null;

    // Change preferred units.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_stats_units_title));
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
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_now));
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.sd_card_save_success), 0,
        EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
    instrumentation.waitForIdleSync();

    // Delete all tracks.
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.deleteAllTracks();
    instrumentation.waitForIdleSync();

    // Read from SD card.
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup));
    EndToEndTestUtils.SOLO
        .clickOnText(activityMyTracks.getString(R.string.settings_backup_restore));
    EndToEndTestUtils
        .getButtonOnScreen(activityMyTracks.getString(R.string.generic_ok), true, true);
    assertTrue(EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.sd_card_import_success), 0,
        EndToEndTestUtils.SUPER_LONG_WAIT_TIME));
    // Check restore track.
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.trackName));
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
        .getString(R.string.settings_recording_track_name));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_recording_track_name_number_option));
    // Changes the setting of default activity.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_recording_default_activity));
    EndToEndTestUtils.enterTextAvoidSoftKeyBoard(0, EndToEndTestUtils.DEFAULTACTIVITY);
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
    assertTrue(EndToEndTestUtils.SOLO.searchText(EndToEndTestUtils.DEFAULTACTIVITY));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.generic_save));
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
