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
   * Changes some settings and then reset them.
   */
  public void testSetting() {
    sendKeys(KeyEvent.KEYCODE_MENU);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true, false);
    instrumentation.waitForIdleSync();
    // Rotate on the settings page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset));
    // Reset all settings at first.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));
    // Change a setting of display.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_display));
    EndToEndTestUtils.SOLO
        .waitForText(activityMyTracks.getString(R.string.settings_display_metric));
    // Rotate on a sub setting page.
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO
        .waitForText(activityMyTracks.getString(R.string.settings_display_metric));
    ArrayList<CheckBox> displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    boolean useMetric = displayCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    EndToEndTestUtils.SOLO.goBack();
    // Change a setting of sharing.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_sharing_allow_access));
    ArrayList<CheckBox> sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    boolean newMapsPublic = sharingCheckBoxs.get(0).isChecked();
    EndToEndTestUtils.SOLO.clickOnCheckBox(0);
    // Rotate on a sub setting page when the value of a checkbox is changed.
    instrumentation.waitForIdleSync();
    EndToEndTestUtils.rotateAllActivities();
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_sharing_allow_access));
    assertEquals(!newMapsPublic, sharingCheckBoxs.get(0).isChecked());
    EndToEndTestUtils.SOLO.goBack();

    // Reset all settings.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    EndToEndTestUtils.SOLO.clickOnButton(activityMyTracks.getString(R.string.generic_ok));

    // Check settings.
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_display));
    EndToEndTestUtils.SOLO
        .waitForText(activityMyTracks.getString(R.string.settings_display_metric));
    displayCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    assertEquals(useMetric, displayCheckBoxs.get(0).isChecked());

    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sharing));
    EndToEndTestUtils.SOLO.waitForText(activityMyTracks
        .getString(R.string.settings_sharing_allow_access));
    sharingCheckBoxs = EndToEndTestUtils.SOLO.getCurrentCheckBoxes();
    assertEquals(newMapsPublic, sharingCheckBoxs.get(0).isChecked());
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}
