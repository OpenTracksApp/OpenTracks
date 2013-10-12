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
import com.google.android.apps.mytracks.endtoendtest.RunConfiguration;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Tests connecting to sensors.
 * 
 * @author Youtao Liu
 */
public class SensorTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  public SensorTest() {
    super(TrackListActivity.class);
  }

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;
  private boolean runTest = false;
  private static final String ZEPHYR_NAME = "HXM";
  private static final String POLAR_NAME = "Polar";

  @Override
  protected void setUp() throws Exception {
    runTest = RunConfiguration.runSensorTest;
    super.setUp();
    if (!runTest) {
      return;
    }
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
  }

  /**
   * Tests connecting to a Zephyr Bluetooth sensor while not recording. Before this test, a Zephyr
   * sensor must be paired with the device.
   */
  public void testConnectZephyrBluetoothSensor_notRecording() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    bluetoothSensorTest(R.string.settings_sensor_type_zephyr, ZEPHYR_NAME, false);
  }
  
  /**
   * Tests connecting to a Zephyr Bluetooth sensor while under recording. Before this test, a Zephyr
   * sensor must be paired with the device.
   */
  public void testConnectZephyrBluetoothSensor_underRecording() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    bluetoothSensorTest(R.string.settings_sensor_type_zephyr, ZEPHYR_NAME, true);
  }

  /**
   * Tests connecting to a Polar Bluetooth sensor while not recording. Before this test, a Polar
   * sensor must be paired with the device.
   */
  public void testConnectPolarBluetoothSensor_notRecording() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    bluetoothSensorTest(R.string.settings_sensor_type_polar, POLAR_NAME, false);
  }
  
  /**
   * Tests connecting to a Polar Bluetooth sensor while under recording. Before this test, a Polar
   * sensor must be paired with the device.
   */
  public void testConnectPolarBluetoothSensor_underRecording() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    bluetoothSensorTest(R.string.settings_sensor_type_polar, POLAR_NAME, true);
  }

  /**
   * Set the paired sensor.
   * 
   * @param sensorTypeStringId the string id of paired sensors type
   * @param nameString part of the sensor name string which can distinguish
   *          different Bluetooth sensors
   * @param isUnderRecording true means test under recording
   */
  private void bluetoothSensorTest(int sensorTypeStringId, String nameString,
      boolean isUnderRecording) {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_sensor));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_sensor_type));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(sensorTypeStringId));
    checkPairedSensorsNumber(nameString);
    // Set the paired sensor.
    EndToEndTestUtils.SOLO.clickOnText(nameString);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    if (isUnderRecording) {
      assertTrue(checkSensorsStatus_underRecording());
    } else {
      assertTrue(checkSensorsStatus_notRecording());
    }
  }

  /**
   * Checks the number of paired sensors.
   */
  private void checkPairedSensorsNumber(String nameString) {
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity
        .getString(R.string.settings_sensor_bluetooth_sensor));
    instrumentation.waitForIdleSync();
    ArrayList<ListView> allListViews = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class);
    int number = allListViews.get(0).getCount();
    if (number > 0
        && EndToEndTestUtils.SOLO.waitForText(nameString, 1, EndToEndTestUtils.SHORT_WAIT_TIME)) {
      return;
    }

    Log.i(EndToEndTestUtils.LOG_TAG, "No sensor is paried");
    fail("No bluetooth sensor is paired. Please pair at Settings->Bluetooth before running the test.");
  }

  /**
   * Tests connecting to a cadence ANT+ sensor.
   */
  public void testConnectANTSensor_Cadence() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    useANTSeonsor();
    assertTrue(checkSensorsStatus_notRecording());
    checkANTSensorsStatus(R.id.sensor_state_cadence);
  }

  /**
   * Tests connecting to cadence and heart rate ANT+ sensors at the same time.
   */
  public void testConnectTwoANTSensors() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    useANTSeonsor();
    assertTrue(checkSensorsStatus_notRecording());
    checkANTSensorsStatus(R.id.sensor_state_cadence);
    checkANTSensorsStatus(R.id.sensor_state_heart_rate);
  }

  /**
   * Sets the setting to use ANT+ sensor
   */
  private void useANTSeonsor() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_sensor));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.settings_sensor_type));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity
        .getString(R.string.settings_sensor_type_ant));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }

  /**
   * Checks whether the ANT+ sensor is connected with MyTracks.
   * 
   * @param viewID the id to check which is not equal with None
   */
  private void checkANTSensorsStatus(int viewID) {
    TextView sensorValueText = ((TextView) EndToEndTestUtils.SOLO.getCurrentActivity()
        .findViewById(viewID));
    assertNotNull(sensorValueText);
    String realValue = sensorValueText.getText().toString();
    String noneValue = trackListActivity.getString(R.string.value_none);
    assertNotSame(realValue, noneValue);
  }
  
  /**
   * Checks whether the sensor is connected with MyTracks during recording.
   * 
   * @return true means the sensor is connected with MyTracks
   */
  private boolean checkSensorsStatus_underRecording() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_sensor_state), true);
    boolean result = EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.sensor_state_connected), 1,
        EndToEndTestUtils.LONG_WAIT_TIME);
    EndToEndTestUtils.stopRecording(true);
    return result;
  }

  /**
   * Checks whether the sensor is connected with MyTracks while not under recording.
   * 
   * @return true means the sensor is connected with MyTracks
   */
  private boolean checkSensorsStatus_notRecording() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    EndToEndTestUtils.createTrackIfEmpty(0, false);
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_sensor_state), true);
    return EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.sensor_state_connected), 1,
        EndToEndTestUtils.LONG_WAIT_TIME);
  }

  @Override
  protected void tearDown() throws Exception {
    if (!runTest) {
      return;
    }
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

}