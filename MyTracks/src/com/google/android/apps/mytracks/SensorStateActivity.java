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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.sensors.SensorManager;
import com.google.android.apps.mytracks.services.sensors.SensorManagerFactory;
import com.google.android.apps.mytracks.services.sensors.SensorUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;
import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * An activity that displays information about sensors.
 *
 * @author Sandor Dornbush
 */
public class SensorStateActivity extends Activity {

  private static final long REFRESH_PERIOD_MS = 250;

  private final StatsUtilities utils;

  /**
   * This timer periodically invokes the refresh timer task.
   */
  private Timer timer;

  private final Runnable stateUpdater = new Runnable() {
    public void run() {
      if (isVisible)    // only update when UI is visible
        updateState();
    }
  };

  /**
   * Connection to the recording service.
   */
  private TrackRecordingServiceConnection serviceConnection;

  /**
   * A task which will update the U/I.
   */
  private class RefreshTask extends TimerTask {
    @Override
    public void run() {
      runOnUiThread(stateUpdater);
    }
  };


  /**
   * A temporary sensor manager, when none is available.
   */
  private SensorManager tempSensorManager = null;

  /**
   * A state flag set to true when the activity is active/visible,
   * i.e. after resume, and before pause
   *
   * Used to avoid updating after the pause event, because sometimes an update
   * event occurs even after the timer is cancelled. In this case,
   * it could cause the {@link #tempSensorManager} to be recreated, after it
   * is destroyed at the pause event.
   */
  private boolean isVisible = false;

  public SensorStateActivity() {
    utils = new StatsUtilities(this);
    Log.w(TAG, "SensorStateActivity()");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.w(TAG, "SensorStateActivity.onCreate");

    setContentView(R.layout.sensor_state);

    serviceConnection = new TrackRecordingServiceConnection(this, stateUpdater);
    serviceConnection.bindIfRunning();
    updateState();
  }

  @Override
  protected void onResume() {
    super.onResume();

    isVisible = true;

    serviceConnection.bindIfRunning();

    timer = new Timer();
    timer.schedule(new RefreshTask(), REFRESH_PERIOD_MS, REFRESH_PERIOD_MS);
  }

  @Override
  protected void onPause() {
    isVisible = false;

    timer.cancel();
    timer.purge();
    timer = null;
    stopTempSensorManager();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    serviceConnection.unbind();
    super.onDestroy();
  }

  private void updateState() {
    Log.d(TAG, "Updating SensorStateActivity");

    ITrackRecordingService service = serviceConnection.getServiceIfBound();

    //  Check if service is available, and recording.
    boolean isRecording = false;
    if (service != null) {
      try {
        isRecording = service.isRecording();
      } catch (RemoteException e) {
        Log.e(TAG, "Unable to determine if service is recording.", e);
      }
    }

    //  If either service isn't available, or not recording.
    if (!isRecording) {
      updateFromTempSensorManager();
    } else {
      updateFromSysSensorManager();
    }
  }

  private void updateFromTempSensorManager() {
    //  Use variables to hold the sensor state and data set.
    Sensor.SensorState currentState = null;
    Sensor.SensorDataSet currentDataSet = null;

    // If no temp sensor manager is present, create one, and start it.
    if (tempSensorManager == null) {
      tempSensorManager = SensorManagerFactory.getInstance().getSensorManager(this);
    }

    //  If a temp sensor manager is available, use states from temp sensor
    //  manager.
    if (tempSensorManager != null) {
      currentState = tempSensorManager.getSensorState();
      currentDataSet = tempSensorManager.getSensorDataSet();
    }

    // Update the sensor state, and sensor data, using the variables.
    updateSensorStateAndData(currentState, currentDataSet);
  }

  private void updateFromSysSensorManager() {
    //  Use variables to hold the sensor state and data set.
    Sensor.SensorState currentState = null;
    Sensor.SensorDataSet currentDataSet = null;

    ITrackRecordingService service = serviceConnection.getServiceIfBound();

    //  If a temp sensor manager is present, shut it down,
    //  probably recording just started.
    stopTempSensorManager();

    // Get sensor details from the service.
    if (service == null) {
      Log.d(TAG, "Could not get track recording service.");
    } else {
      try {
        byte[] buff = service.getSensorData();
        if (buff != null) {
          currentDataSet = Sensor.SensorDataSet.parseFrom(buff);
        }
      } catch (RemoteException e) {
        Log.e(TAG, "Could not read sensor data.", e);
      } catch (InvalidProtocolBufferException e) {
        Log.e(TAG, "Could not read sensor data.", e);
      }

      try {
        currentState = Sensor.SensorState.valueOf(service.getSensorState());
      } catch (RemoteException e) {
        Log.e(TAG, "Could not read sensor state.", e);
        currentState = Sensor.SensorState.NONE;
      }
    }

    // Update the sensor state, and sensor data, using the variables.
    updateSensorStateAndData(currentState, currentDataSet);
  }

  /**
   * Stops the temporary sensor manager, if one exists.
   */
  private void stopTempSensorManager() {
    if (tempSensorManager != null) {
      SensorManagerFactory.getInstance().releaseSensorManager(tempSensorManager);
      tempSensorManager = null;
    }
  }

  private void updateSensorStateAndData(Sensor.SensorState state, Sensor.SensorDataSet dataSet) {
    updateSensorState(state == null ? Sensor.SensorState.NONE : state);
    updateSensorData(dataSet);
  }

  private void updateSensorState(Sensor.SensorState state) {
    ((TextView) findViewById(R.id.sensor_state)).setText(SensorUtils.getStateAsString(state, this));
  }

  private void updateSensorData(Sensor.SensorDataSet sds) {
    if (sds == null) {
      utils.setUnknown(R.id.sensor_state_last_sensor_time);
      utils.setUnknown(R.id.sensor_state_power);
      utils.setUnknown(R.id.sensor_state_cadence);
      utils.setUnknown(R.id.sensor_state_battery);
    } else {
      ((TextView) findViewById(R.id.sensor_state_last_sensor_time)).setText(getLastSensorTime(sds));
      ((TextView) findViewById(R.id.sensor_state_power)).setText(getPower(sds));
      ((TextView) findViewById(R.id.sensor_state_cadence)).setText(getCadence(sds));
      ((TextView) findViewById(R.id.sensor_state_heart_rate)).setText(getHeartRate(sds));
      ((TextView) findViewById(R.id.sensor_state_battery)).setText(getBattery(sds));
    }
  }

  /**
   * Gets the last sensor time.
   *
   * @param sds sensor data set
   */
  private String getLastSensorTime(Sensor.SensorDataSet sds) {
    return StringUtils.formatTime(this, sds.getCreationTime());
  }

  /**
   * Gets the power.
   *
   * @param sds sensor data set
   */
  private String getPower(Sensor.SensorDataSet sds) {
    String value;
    if (sds.hasPower() && sds.getPower().hasValue()
        && sds.getPower().getState() == Sensor.SensorState.SENDING) {
      String format = getString(R.string.sensor_state_power_value);
      value = String.format(format, sds.getPower().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sds.hasPower() ? sds.getPower().getState() : Sensor.SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the cadence.
   *
   * @param sds sensor data set
   */
  private String getCadence(Sensor.SensorDataSet sds) {
    String value;
    if (sds.hasCadence() && sds.getCadence().hasValue()
        && sds.getCadence().getState() == Sensor.SensorState.SENDING) {
      String format = getString(R.string.sensor_state_cadence_value);
      value = String.format(format, sds.getCadence().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sds.hasCadence() ? sds.getCadence().getState() : Sensor.SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the heart rate.
   *
   * @param sds sensor data set
   */
  private String getHeartRate(Sensor.SensorDataSet sds) {
    String value;
    if (sds.hasHeartRate() && sds.getHeartRate().hasValue()
        && sds.getHeartRate().getState() == Sensor.SensorState.SENDING) {
      String format = getString(R.string.sensor_state_heart_rate_value);
      value = String.format(format, sds.getHeartRate().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sds.hasHeartRate() ? sds.getHeartRate().getState() : Sensor.SensorState.NONE, this);
    }
    return value;
  }

  /**
   * Gets the battery.
   *
   * @param sds sensor data set
   */
  private String getBattery(Sensor.SensorDataSet sds) {
    String value;
    if (sds.hasBatteryLevel() && sds.getBatteryLevel().hasValue()
        && sds.getBatteryLevel().getState() == Sensor.SensorState.SENDING) {
      String format = getString(R.string.sensor_state_battery_value);
      value = String.format(format, sds.getBatteryLevel().getValue());
    } else {
      value = SensorUtils.getStateAsString(
          sds.hasBatteryLevel() ? sds.getBatteryLevel().getState() : Sensor.SensorState.NONE, this);
    }
    return value;
  }
}