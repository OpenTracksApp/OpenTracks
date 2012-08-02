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

package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Sensor.SensorState;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manage the connection to a sensor.
 * 
 * @author Sandor Dornbush
 */
public abstract class SensorManager {

  private static final String TAG = SensorManager.class.getSimpleName();
  private static final long MAX_SENSOR_DATE_SET_AGE = 5000;
  private static final long MAX_SENSOR_STATE_AGE = 20000;
  private static final int RETRY_PERIOD = 20000;

  private SensorState sensorState = SensorState.NONE;
  private long sensorStateTimestamp = System.currentTimeMillis();

  /**
   * A time task to check sensor connection.
   */
  private TimerTask checkSensorConnectionTimeTask = new TimerTask() {
      @Override
    public void run() {
      switch (getSensorState()) {
        case CONNECTING:
          if (System.currentTimeMillis() - sensorStateTimestamp > MAX_SENSOR_STATE_AGE) {
            Log.i(TAG, "Retry setUpChannel");
            setUpChannel();
          }
          break;
        case NONE:
        case DISCONNECTED:
          setUpChannel();
          break;
        default:
          break;
      }
    }
  };

  private final Timer timer = new Timer();

  /**
   * Returns true if the sensor is enabled.
   */
  public abstract boolean isEnabled();

  /**
   * Sets up the sensor channel.
   */
  protected abstract void setUpChannel();

  /**
   * Tears down the sensor channel.
   */
  protected abstract void tearDownChannel();

  /**
   * Gets the sensor data set.
   */
  public abstract SensorDataSet getSensorDataSet();

  /**
   * Starts the sensor.
   */
  public void startSensor() {
    setUpChannel();
    timer.schedule(checkSensorConnectionTimeTask, RETRY_PERIOD, RETRY_PERIOD);
  }

  /**
   * Stops the sensor.
   */
  public void stopSensor() {
    timer.cancel();
    tearDownChannel();
  }

  /**
   * Sets the sensor state.
   * 
   * @param sensorState the sensor state
   */
  public void setSensorState(SensorState sensorState) {
    sensorStateTimestamp = System.currentTimeMillis();
    this.sensorState = sensorState;
  }

  /**
   * Gets the sensor state.
   */
  public SensorState getSensorState() {
    return sensorState;
  }

  /**
   * Returns true if the sensor data set is valid.
   */
  public boolean isSensorDataSetValid() {
    SensorDataSet sensorDataSet = getSensorDataSet();
    if (sensorDataSet == null) {
      return false;
    }
    return (System.currentTimeMillis() - sensorDataSet.getCreationTime()) < MAX_SENSOR_DATE_SET_AGE;
  }
}
