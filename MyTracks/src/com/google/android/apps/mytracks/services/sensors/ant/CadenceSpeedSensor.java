/*
 * Copyright 2012 Google Inc.
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
 * Ant+ combined cadence and speed sensor.
 *
 * @author Laszlo Molnar
 */

public class CadenceSpeedSensor extends AntSensorBase {
  /*
   * These constants are defined by the ANT+ bike speed and cadence sensor spec.
   */
  public static final byte CADENCE_SPEED_DEVICE_TYPE = 121;
  public static final short CADENCE_SPEED_CHANNEL_PERIOD = 8086;

  SensorEventCounter dataProcessor = new SensorEventCounter();

  CadenceSpeedSensor(short devNum) {
    super(devNum, CADENCE_SPEED_DEVICE_TYPE, "speed&cadence sensor", CADENCE_SPEED_CHANNEL_PERIOD);
  }

  /**
   * Decode an ANT+ cadence&speed sensor message.
   * @param antMessage The byte array received from the sensor.
   */
  @Override
  public void handleBroadcastData(byte[] antMessage, AntSensorDataCollector c) {
    int sensorTime = ((int) antMessage[1] & 0xFF) + ((int) antMessage[2] & 0xFF) * 256;
    int crankRevs = ((int) antMessage[3] & 0xFF) + ((int) antMessage[4] & 0xFF) * 256;
    c.setCadence(dataProcessor.getEventsPerMinute(crankRevs, sensorTime));
  }
};
