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
package com.google.android.apps.mytracks.endtoendtest.others;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Tests Connect to Sensors.
 * 
 * @author Youtao Liu
 */
public class SensorTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  /**
   * Set to false as default.
   */
  public static boolean testSensor = false;

  public SensorTest() {
    super(TrackListActivity.class);
  }

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;
  public static final String DISABLE_MESSAGE = "This test is disabled"; 

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    if (!testSensor) {
      return;
    }
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Tests connecting to a Zephyr Bluetooth sensor. Before this test, a Zephyr
   * sensor must has been paired with the device.
   */
  public void testConnectZephyrBluetoothSensor() {
    if (!testSensor) {
      Log.i(EndToEndTestUtils.LOG_TAG, DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sensor));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sensor_type));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sensor_type_zephyr));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sensor_bluetooth_sensor));
    assertTrue(getPairedSensorsNumber() > 0);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(checkSensorsStatus());
  }

  /**
   * Tests connecting to a Polar Bluetooth sensor. Before this test, a Polar
   * sensor must has been paired with the device.
   */
  public void testConnectPolarBluetoothSensor() {
    if (!testSensor) {
      Log.i(EndToEndTestUtils.LOG_TAG, DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sensor));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.settings_sensor_type));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sensor_type_polar));
    EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
        .getString(R.string.settings_sensor_bluetooth_sensor));
    assertTrue(getPairedSensorsNumber() > 0);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    assertTrue(checkSensorsStatus());
  }

  /**
   * Get the number of paired sensors.
   * 
   * @return the number of paired sensors
   */
  private int getPairedSensorsNumber() {
    ArrayList<ListView> allListViews = EndToEndTestUtils.SOLO.getCurrentListViews();
    int number = allListViews.get(0).getCount();
    return number;
  }

  /**
   * Checks whether the sensor is connected with MyTracks.
   * 
   * @return true means the sensor is connected with MyTracks
   */
  private boolean checkSensorsStatus() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(0, false);
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_sensor_state), true);
    return EndToEndTestUtils.SOLO.waitForText(
        activityMyTracks.getString(R.string.sensor_state_connected), 1,
        EndToEndTestUtils.NORMAL_WAIT_TIME);
  }
  
  @Override
  protected void tearDown() throws Exception {
    if (!testSensor) {
      return;
    }
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}