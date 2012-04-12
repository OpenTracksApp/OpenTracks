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
 * Ant+ cadence sensor.
 *
 * @author Laszlo Molnar
 */

public class CadenceSensor extends AntSensorBase {
  /*
   * These constants are defined by the ANT+ bike speed and cadence sensor spec.
   */
  public static final byte CADENCE_DEVICE_TYPE = 122;
  public static final short CADENCE_CHANNEL_PERIOD = 8102;

  SensorEventCounter dataProcessor = new SensorEventCounter();

  CadenceSensor(short devNum) {
    super(devNum, CADENCE_DEVICE_TYPE, "cadence sensor", CADENCE_CHANNEL_PERIOD);
  }

  /**
   * Decode an ANT+ cadence sensor message.
   * @param antMessage The byte array received from the cadence sensor.
   */
  @Override
  public void handleBroadcastData(byte[] antMessage, AntSensorDataCollector c) {
    int sensorTime = ((int) antMessage[5] & 0xFF) + ((int) antMessage[6] & 0xFF) * 256;
    int crankRevs = ((int) antMessage[7] & 0xFF) + ((int) antMessage[8] & 0xFF) * 256;
    c.setCadence(dataProcessor.getEventsPerMinute(crankRevs, sensorTime));
  }
};
