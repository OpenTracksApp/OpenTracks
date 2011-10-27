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

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;

/**
 * Manage the connection to a sensor.
 *
 * @author Sandor Dornbush
 */
public abstract class SensorManager {

  /**
   * The maximum age where the data is considered valid.
   */
  public static final long MAX_AGE = 5000;

  /**
   * Time to wait after a time out to retry.
   */
  public static final int RETRY_PERIOD = 30000;

  private Sensor.SensorState sensorState = Sensor.SensorState.NONE;

  private long sensorStateTimestamp = 0;

  /**
   * A task to run periodically to check to see if connection was lost.
   */
  private TimerTask checkSensorManager = new TimerTask() {
    @Override
    public void run() {
      Log.i(Constants.TAG,
          "SensorManager state: " + getSensorState());
      switch (getSensorState()) {
        case CONNECTING:
          long age = System.currentTimeMillis() - getSensorStateTimestamp();
          if (age > 2 * RETRY_PERIOD) {
            Log.i(Constants.TAG, "Retrying connecting SensorManager.");
            setupChannel();
          }
          break;
        case DISCONNECTED:
          Log.i(Constants.TAG,
              "Re-registering disconnected SensoManager.");
          setupChannel();
          break;
      }
    }
  };

  /**
   * This timer invokes periodically the checkLocationListener timer task.
   */
  private final Timer timer = new Timer();

  /**
   * Is the sensor that this manages enabled.
   * @return true if the sensor is enabled
   */
  public abstract boolean isEnabled();

  /**
   * This is called when my tracks starts recording a new track.
   * This is the place to open connections to the sensor.
   */
  public void onStartTrack() {
    setupChannel();
    timer.schedule(checkSensorManager, RETRY_PERIOD, RETRY_PERIOD);
  }

  /**
   * This method is used to set up any necessary connections to underlying
   * sensor hardware.
   */
  protected abstract void setupChannel();

  public void shutdown() {
    timer.cancel();
    onDestroy();
  }

  /**
   * This is called when my tracks stops recording.
   * This is the place to shutdown any open connections.
   */
  public abstract void onDestroy();

  /**
   * Return the last sensor reading.
   * @return The last reading from the sensor.
   */
  public abstract Sensor.SensorDataSet getSensorDataSet();

  public void setSensorState(Sensor.SensorState sensorState) {
    this.sensorState = sensorState;
  }

  /**
   * Return the current sensor state.
   * @return The current sensor state.
   */
  public Sensor.SensorState getSensorState() {
    return sensorState;
  }

  public long getSensorStateTimestamp() {
    return sensorStateTimestamp;
  }

  /**
   * @return True if the data is recent enough to be considered valid.
   */
  public boolean isDataValid() {
    return (System.currentTimeMillis() - getSensorDataSet().getCreationTime()) < MAX_AGE;
  }

}
