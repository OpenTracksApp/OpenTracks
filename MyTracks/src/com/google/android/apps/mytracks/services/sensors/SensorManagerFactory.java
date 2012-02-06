/*
 * Copyright 2009 Google Inc.
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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.sensors.ant.AntDirectSensorManager;
import com.google.android.apps.mytracks.services.sensors.ant.AntSrmBridgeSensorManager;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A factory of SensorManagers.
 *
 * @author Sandor Dornbush
 */
public class SensorManagerFactory {

  private String activeSensorType;
  private SensorManager activeSensorManager;
  private int refCount;

  private static SensorManagerFactory instance = new SensorManagerFactory();

  private SensorManagerFactory() {
  }

  /**
   * Get the factory instance. 
   */
  public static SensorManagerFactory getInstance() {
    return instance;
  }

  /**
   * Get and start a new sensor manager.
   * @param context Context to fetch system preferences.
   * @return The sensor manager that corresponds to the sensor type setting.
   */
  public SensorManager getSensorManager(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs == null) {
      return null;
    }

    context = context.getApplicationContext();

    String sensor = prefs.getString(context.getString(R.string.sensor_type_key), null);
    Log.i(Constants.TAG, "Creating sensor of type: " + sensor);

    if (sensor == null) {
      reset();
      return null;
    }
    if (sensor.equals(activeSensorType)) {
      Log.i(Constants.TAG, "Returning existing sensor manager.");
      refCount++;
      return activeSensorManager;
    }
    reset();

    if (sensor.equals(context.getString(R.string.sensor_type_value_ant))) {
      activeSensorManager = new AntDirectSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.sensor_type_value_srm_ant_bridge))) {
      activeSensorManager = new AntSrmBridgeSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.sensor_type_value_zephyr))) {
      activeSensorManager = new ZephyrSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.sensor_type_value_polar))) {
      activeSensorManager = new PolarSensorManager(context);
    } else  {
      Log.w(Constants.TAG, "Unable to find sensor type: " + sensor);
      return null;
    }
    activeSensorType = sensor;
    refCount = 1;
    activeSensorManager.onStartTrack();
    return activeSensorManager;
  }

  /**
   * Finish using a sensor manager.
   */
  public void releaseSensorManager(SensorManager sensorManager) {
    Log.i(Constants.TAG, "releaseSensorManager: " + activeSensorType + " " + refCount);
    if (sensorManager != activeSensorManager) {
      Log.e(Constants.TAG, "invalid parameter to releaseSensorManager");
    }
    if (--refCount > 0) {
      return;
    }
    reset();
  }

  private void reset() {
    activeSensorType = null;
    if (activeSensorManager != null) {
      activeSensorManager.shutdown();
    }
    activeSensorManager = null;
    refCount = 0;
  }
}
