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

  private SensorManagerFactory() {
  }

  /**
   * Get a new sensor manager.
   * @param context Context to fetch system preferences.
   * @return The sensor manager that corresponds to the sensor type setting.
   */
  public static SensorManager getSensorManager(Context context) {
    SharedPreferences prefs =
        context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    if (prefs == null) {
      return null;
    }

    context = context.getApplicationContext();

    String sensor = prefs.getString(context.getString(R.string.sensor_type_key), null);
    Log.i(Constants.TAG, "Creating sensor of type: " + sensor);

    if (sensor == null) {
      return null;
    } else if (sensor.equals(context.getString(R.string.ant_sensor_type))) {
      return new AntDirectSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.srm_ant_bridge_sensor_type))) {
      return new AntSrmBridgeSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.zephyr_sensor_type))) {
      return new ZephyrSensorManager(context);
    } else if (sensor.equals(context.getString(R.string.polar_sensor_type))) {
      return new PolarSensorManager(context);
    } else  {
      Log.w(Constants.TAG, "Unable to find sensor type: " + sensor);
      return null;
    }
  }
}
