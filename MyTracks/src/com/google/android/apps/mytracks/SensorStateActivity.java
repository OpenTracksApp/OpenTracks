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

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.services.sensors.SensorManagerFactory;
import com.google.android.apps.mytracks.services.sensors.SensorUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;
import com.google.protobuf.InvalidProtocolBufferException;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

/**
 * An activity that displays information about sensors.
 * 
 * @author Sandor Dornbush
 */
public class SensorStateActivity extends AbstractMyTracksActivity {

  private static final String TAG = SensorStateActivity.class.getName();
  private static final long ONE_SECOND = 1000;

  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private Handler handler;
  private SensorManager tempSensorManager;

  private final Runnable updateUiRunnable = new Runnable() {
      @Override
    public void run() {
      ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
          .getServiceIfBound();

      // Check if service is available and recording
      boolean isRecording = false;
      if (trackRecordingService != null) {
        try {
          isRecording = trackRecordingService.isRecording();
        } catch (RemoteException e) {
          Log.e(TAG, "Unable to determine if the track recording service is recording.", e);
        }
      }

      if (!isRecording) {
        updateFromTempSensorManager();
      } else {
        stopTempSensorManager();
        updateFromSystemSensorManager();
      }
      handler.postDelayed(this, ONE_SECOND);
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
      SensorManagerFactory.releaseTempSensorManager();
      tempSensorManager = null;
    }
  }

  /**
   * Updates from a temp sensor manager.
   */
  private void updateFromTempSensorManager() {
    Sensor.SensorState sensorState = Sensor.SensorState.NONE;
    Sensor.SensorDataSet sensorDataSet = null;

    if (tempSensorManager == null) {
      tempSensorManager = SensorManagerFactory.getTempSensorManager(this);
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
    Sensor.SensorState sensorState = Sensor.SensorState.NONE;
    Sensor.SensorDataSet sensorDataSet = null;

    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
        .getServiceIfBound();

    // Get sensor details from the service.
    if (trackRecordingService == null) {
      Log.d(TAG, "Cannot get teh track recording service.");
    } else {
      try {
        sensorState = Sensor.SensorState.valueOf(trackRecordingService.getSensorState());
      } catch (RemoteException e) {
        Log.e(TAG, "Cannote read sensor state.", e);
        sensorState = Sensor.SensorState.NONE;
      }

      try {
        byte[] buff = trackRecordingService.getSensorData();
        if (buff != null) {
          sensorDataSet = Sensor.SensorDataSet.parseFrom(buff);
        }
      } catch (RemoteException e) {
        Log.e(TAG, "Cannot read sensor data set.", e);
      } catch (InvalidProtocolBufferException e) {
        Log.e(TAG, "Cannot read sensor data set.", e);
      }
    }

    updateSensorStateAndDataSet(sensorState, sensorDataSet);
  }

  /**
   * Updates the sensor state and data set.
   * 
   * @param sensorState sensor state
   * @param sensorDataSet sensor data set
   */
  private void updateSensorStateAndDataSet(
      Sensor.SensorState sensorState, Sensor.SensorDataSet sensorDataSet) {
    ((TextView) findViewById(R.id.sensor_state)).setText(
        SensorUtils.getStateAsString(sensorState, this));

    String lastSensorTime = sensorDataSet == null ? getString(R.string.value_unknown)
        : getLastSensorTime(sensorDataSet);
    String heartRate = sensorDataSet == null ? getString(R.string.value_unknown)
        : getHeartRate(sensorDataSet);
    String cadence = sensorDataSet == null ? getString(R.string.value_unknown)
        : getCadence(sensorDataSet);
    String power = sensorDataSet == null ? getString(R.string.value_unknown)
        : getPower(sensorDataSet);
    String battery = sensorDataSet == null ? getString(R.string.value_unknown)
        : getBattery(sensorDataSet);

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
  private String getLastSensorTime(Sensor.SensorDataSet sensorDataSet) {
    return DateFormat.format("h:mm:ss aa", sensorDataSet.getCreationTime()).toString();
  }

  /**
   * Gets the heart rate.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getHeartRate(Sensor.SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasHeartRate() && sensorDataSet.getHeartRate().hasValue()
        && sensorDataSet.getHeartRate().getState() == Sensor.SensorState.SENDING) {
      value = getString(
          R.string.sensor_state_heart_rate_value, sensorDataSet.getHeartRate().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sensorDataSet.hasHeartRate() ? sensorDataSet.getHeartRate().getState()
              : Sensor.SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the cadence.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getCadence(Sensor.SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasCadence() && sensorDataSet.getCadence().hasValue()
        && sensorDataSet.getCadence().getState() == Sensor.SensorState.SENDING) {
      value = getString(R.string.sensor_state_cadence_value, sensorDataSet.getCadence().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sensorDataSet.hasCadence() ? sensorDataSet.getCadence().getState()
              : Sensor.SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the power.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getPower(Sensor.SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasPower() && sensorDataSet.getPower().hasValue()
        && sensorDataSet.getPower().getState() == Sensor.SensorState.SENDING) {
      value = getString(R.string.sensor_state_power_value, sensorDataSet.getPower().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sensorDataSet.hasPower() ? sensorDataSet.getPower().getState() : Sensor.SensorState.NONE,
          this);
    }
    return value;
  }

  /**
   * Gets the battery.
   * 
   * @param sensorDataSet sensor data set
   */
  private String getBattery(Sensor.SensorDataSet sensorDataSet) {
    String value;
    if (sensorDataSet.hasBatteryLevel() && sensorDataSet.getBatteryLevel().hasValue()
        && sensorDataSet.getBatteryLevel().getState() == Sensor.SensorState.SENDING) {
      value = getString(R.string.value_integer_percent, sensorDataSet.getBatteryLevel().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sensorDataSet.hasBatteryLevel() ? sensorDataSet.getBatteryLevel().getState()
              : Sensor.SensorState.NONE, this);
    }
    return value;
  }
}