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

import android.content.Context;

/**
 * A factory of {@link SensorManager}.
 *
 * @author Sandor Dornbush
 */
public class SensorManagerFactory {

  private static SensorManager systemSensorManager = null;
  private static SensorManager sensorManagerTemporary = null;

  private SensorManagerFactory() {}

  /**
   * Gets the system sensor manager.
   *
   * @param context the context
   */
  public static SensorManager getSystemSensorManager(Context context) {
    releaseSensorManagerTemporary();
    releaseSystemSensorManager();
    systemSensorManager = getSensorManager(context);
    if (systemSensorManager != null) {
      systemSensorManager.startSensor();
    }
    return systemSensorManager;
  }

  /**
   * Releases the system sensor manager.
   */
  public static void releaseSystemSensorManager() {
    if (systemSensorManager != null) {
      systemSensorManager.stopSensor();
    }
    systemSensorManager = null;
  }

  /**
   * Gets the temp sensor manager.
   *
   * @param context
   */
  public static SensorManager getSensorManagerTemporary(Context context) {
    releaseSensorManagerTemporary();
    if (systemSensorManager != null) {
      return null;
    }
    sensorManagerTemporary = getSensorManager(context);
    if (sensorManagerTemporary != null) {
      sensorManagerTemporary.startSensor();
    }
    return sensorManagerTemporary;
  }

  /**
   * Releases the temp sensor manager.
   */
  public static void releaseSensorManagerTemporary() {
    if (sensorManagerTemporary != null) {
      sensorManagerTemporary.stopSensor();
    }
    sensorManagerTemporary = null;
  }

  /**
   * Gets the sensor manager.
   *
   * @param context the context
   */
  private static SensorManager getSensorManager(Context context) {
    //TODO Implement Bluetooth LE Sensor manager

    return null;
   }
}
