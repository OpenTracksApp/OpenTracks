/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.android.apps.mytracks.services.sensors.ant;

/**
 * A enum which stores static data about ANT sensors.
 *
 * @author Matthew Simmons
 */
public enum AntSensor {
  SENSOR_HEART_RATE (Constants.ANT_DEVICE_TYPE_HRM),
  SENSOR_CADENCE (Constants.ANT_DEVICE_TYPE_CADENCE),
  SENSOR_SPEED (Constants.ANT_DEVICE_TYPE_SPEED),
  SENSOR_POWER (Constants.ANT_DEVICE_TYPE_POWER);

  private static class Constants {
    public static byte ANT_DEVICE_TYPE_POWER = 11;
    public static byte ANT_DEVICE_TYPE_HRM = 120;
    public static byte ANT_DEVICE_TYPE_CADENCE = 122;
    public static byte ANT_DEVICE_TYPE_SPEED = 123;
  };

  private final byte deviceType;

  private AntSensor(byte deviceType) {
    this.deviceType = deviceType;
  }

  public byte getDeviceType() {
    return deviceType;
  }
}
