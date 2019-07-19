/*
 * Copyright 2010 Google Inc.
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

package com.google.android.apps.mytracks;

import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.apps.mytracks.content.sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.sensor.SensorState;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.services.sensors.SensorManagerFactory;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

/**
 * An activity that displays information about sensors.
 * 
 * @author Sandor Dornbush
 */
public class SensorStateActivity extends AbstractActivity {

  private static final String TAG = SensorStateActivity.class.getName();
  
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private Handler handler;
  private SensorManager tempSensorManager;

  private final Runnable updateUiRunnable = new Runnable() {
      @Override
    public void run() {
      ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

      // Check if service is available and recording
      boolean isRecording = false;
      if (trackRecordingService != null) {
        isRecording = trackRecordingService.isRecording();
      }

      if (!isRecording) {
        updateFromTempSensorManager();
      } else {
        stopTempSensorManager();
        updateFromSystemSensorManager();
      }
      handler.postDelayed(this, UnitConversions.ONE_SECOND);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);
    handler = new Handler();
  }

  @Override
  protected void onStart() {
    super.onStart();
    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
  }

  @Override
  protected void onResume() {
    super.onResume();
    handler.post(updateUiRunnable);
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(updateUiRunnable);
    stopTempSensorManager();
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.sensor_state;
  }

  /**
   * Stops the temp sensor manager.
   */
  private void stopTempSensorManager() {
    if (tempSensorManager != null) {
      SensorManagerFactory.releaseSensorManagerTemporary();
      tempSensorManager = null;
    }
  }

  /**
   * Updates from a temp sensor manager.
   */
  private void updateFromTempSensorManager() {
    SensorState sensorState = SensorState.NONE;
    SensorDataSet sensorDataSet = null;

    if (tempSensorManager == null) {
      tempSensorManager = SensorManagerFactory.getSensorManagerTemporary(this);
    }

    if (tempSensorManager != null) {
      sensorState = tempSensorManager.getSensorState();
      sensorDataSet = tempSensorManager.getSensorDataSet();
    }

    updateSensorStateAndDataSet(sensorState, sensorDataSet);
  }

  /**
   * Updates from the system sensor manager.
   */
  private void updateFromSystemSensorManager() {
    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
    if (trackRecordingService == null) {
      Log.d(TAG, "Cannot get the track recording service.");
      updateSensorStateAndDataSet(SensorState.NONE, null);
    } else {
      updateSensorStateAndDataSet(trackRecordingService.getSensorState(), trackRecordingService.getSensorData());
    }
  }

  /**
   * Updates the sensor state and data set.
   * 
   * @param sensorState sensor state
   * @param sensorDataSet sensor data set
   */
  private void updateSensorStateAndDataSet(SensorState sensorState, SensorDataSet sensorDataSet) {
    ((TextView) findViewById(R.id.sensor_state)).setText(SensorState.getStateAsString(sensorState, this));

    String lastSensorTime = sensorDataSet == null ? getString(R.string.value_unknown) : getLastSensorTime(sensorDataSet);
    String heartRate = sensorDataSet == null ? getString(R.string.value_unknown) : getHeartRate(sensorDataSet);
    String cadence = sensorDataSet == null ? getString(R.string.value_unknown) : getCadence(sensorDataSet);
    String power = sensorDataSet == null ? getString(R.string.value_unknown) : getPower(sensorDataSet);
    String battery = sensorDataSet == null ? getString(R.string.value_unknown) : getBattery(sensorDataSet);

    ((TextView) findViewById(R.id.sensor_state_last_sensor_time)).setText(lastSensorTime);
    ((TextView) findViewById(R.id.sensor_state_heart_rate)).setText(heartRate);
    ((TextView) findViewById(R.id.sensor_state_cadence)).setText(cadence);
    ((TextView) findViewById(R.id.sensor_state_power)).setText(power);
    ((TextView) findViewById(R.id.sensor_state_battery)).setText(battery);
  }

  /**
   * Gets the last sensor time.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getLastSensorTime(SensorDataSet sensorDataSet) {
    return DateFormat.format("h:mm:ss aa", sensorDataSet.getCreationTime()).toString();
  }

  /**
   * Gets the heart rate.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getHeartRate(SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasHeartRate()) {
      value = getString(R.string.sensor_state_heart_rate_value, (int)sensorDataSet.getHeartRate());
    } else {
      value = SensorState.getStateAsString(SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the cadence.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getCadence(SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasCadence()) {
      value = getString(R.string.sensor_state_cadence_value, (int)sensorDataSet.getCadence());
    } else {
      value = SensorState.getStateAsString(SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the power.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getPower(SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasPower()) {
      value = getString(R.string.sensor_state_power_value, (int)sensorDataSet.getPower());
    } else {
      value = SensorState.getStateAsString(SensorState.NONE,this);
    }
    return value;
  }

  /**
   * Gets the battery.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getBattery(SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasBatteryLevel()) {
      value = getString(R.string.value_integer_percent, sensorDataSet.getBatteryLevel());
    } else {
      value = SensorState.getStateAsString(SensorState.NONE, this);
    }
    return value;
  }
}