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
import com.google.android.apps.mytracks.services.TrackRecordingServiceBinder;
import com.google.android.apps.mytracks.services.sensors.SensorUtils;
import com.google.android.maps.mytracks.R;
import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An activity that displays information about sensors.
 *
 * @author Sandor Dornbush
 */
public class SensorStateActivity extends Activity {

  private static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("HH:mm:ss");
  private static final long REFRESH_PERIOD_MS = 250;

  private final StatsUtilities utils;
  private TrackRecordingServiceBinder serviceBinder;

  /**
   * This timer periodically invokes the refresh timer task.
   */
  private Timer timer;

  private final Runnable stateUpdater = new Runnable() {
    public void run() {
     updateState();
    }
  };

  /**
   * A task which will update the U/I.
   */
  private class RefreshTask extends TimerTask {
    @Override
    public void run() {
      runOnUiThread(stateUpdater);
    }
  };

  public SensorStateActivity() {
    utils = new StatsUtilities(this);
    Log.w(TAG, "SensorStateActivity()");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.w(TAG, "SensorStateActivity.onCreate");

    setContentView(R.layout.sensor_state);

    serviceBinder = TrackRecordingServiceBinder.getInstance(this);
    serviceBinder.bindService(stateUpdater);
    updateState();
  }

  @Override
  protected void onResume() {
    super.onResume();
    timer = new Timer();
    timer.schedule(new RefreshTask(), REFRESH_PERIOD_MS, REFRESH_PERIOD_MS);
  }

  @Override
  protected void onPause() {
    super.onPause();
    timer.cancel();
    timer.purge();
    timer = null;
  }

  @Override
  protected void onDestroy() {
    serviceBinder.unbindService();
  }

  protected void updateState() {
    ITrackRecordingService service = serviceBinder.getServiceIfBound();
    if (service == null) {
      Log.d(Constants.TAG, "Could not get track recording service.");
      updateSensorState(Sensor.SensorState.NONE);
      updateSensorData(null);
      return;
    }
    Sensor.SensorDataSet sds = null;
    try {
      byte[] buff = service.getSensorData();
      if (buff != null) {
        sds = Sensor.SensorDataSet.parseFrom(buff);
        updateSensorData(sds);
      }
    } catch (RemoteException e) {
      Log.e(Constants.TAG, "Could not read sensor data.", e);
    } catch (InvalidProtocolBufferException e) {
      Log.e(Constants.TAG, "Could not read sensor data.", e);
    }
    updateSensorData(sds);

    try {
      int i = service.getSensorState();
      updateSensorState(Sensor.SensorState.valueOf(i));
    } catch (RemoteException e) {
      Log.e(Constants.TAG, "Could not read sensor state.", e);
      updateSensorState(Sensor.SensorState.NONE);
    }
  }

  private void updateSensorState(Sensor.SensorState state) {
    TextView sensorTime =
        ((TextView) findViewById(R.id.sensor_state_register));
    sensorTime.setText(SensorUtils.getStateAsString(state, this));
  }

  protected void updateSensorData(Sensor.SensorDataSet sds) {
    if (sds == null) {
      utils.setUnknown(R.id.sensor_data_time_register);
      utils.setUnknown(R.id.cadence_state_register);
      utils.setUnknown(R.id.power_state_register);
      utils.setUnknown(R.id.heart_rate_register);
      return;
    }

    TextView sensorTime =
        ((TextView) findViewById(R.id.sensor_data_time_register));
    sensorTime.setText(
        TIMESTAMP_FORMAT.format(new Date(sds.getCreationTime())));

    if (sds.hasPower() && sds.getPower().hasValue()
        && sds.getPower().getState() == Sensor.SensorState.SENDING) {
      utils.setText(R.id.power_state_register, 
    		  Integer.toString(sds.getPower().getValue()));
    } else {
      utils.setText(R.id.power_state_register,
          SensorUtils.getStateAsString(
              sds.hasPower()
                  ? sds.getPower().getState()
                  : Sensor.SensorState.NONE,
              this));
    }

    if (sds.hasCadence() && sds.getCadence().hasValue()
        && sds.getCadence().getState() == Sensor.SensorState.SENDING) {
      utils.setText(R.id.cadence_state_register,
          Integer.toString(sds.getCadence().getValue()));
    } else {
      utils.setText(R.id.cadence_state_register,
          SensorUtils.getStateAsString(
              sds.hasCadence()
                  ? sds.getCadence().getState()
                  : Sensor.SensorState.NONE,
              this));
    }

    if (sds.hasHeartRate() && sds.getHeartRate().hasValue()
        && sds.getHeartRate().getState() == Sensor.SensorState.SENDING) {
      utils.setText(R.id.heart_rate_register,
    		  Integer.toString(sds.getHeartRate().getValue()));
    } else {
	    utils.setText(R.id.heart_rate_register,
	        SensorUtils.getStateAsString(
	            sds.hasHeartRate()
	                ? sds.getHeartRate().getState()
	                : Sensor.SensorState.NONE,
	            this));
    }
  }
}
