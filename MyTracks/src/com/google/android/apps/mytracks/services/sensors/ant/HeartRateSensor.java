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

import static com.google.android.apps.mytracks.Constants.TAG;

import android.util.Log;

/**
 * Ant+ heart reate monitor sensor.
 *
 * @author Laszlo Molnar
 */

public class HeartRateSensor extends AntSensorBase {
  /*
   * These constants are defined by the ANT+ heart rate monitor spec.
   */
  public static final byte HEART_RATE_DEVICE_TYPE = 120;
  public static final short HEART_RATE_CHANNEL_PERIOD = 8070;

  HeartRateSensor(short devNum) {
    super(devNum, HEART_RATE_DEVICE_TYPE, "heart rate monitor", HEART_RATE_CHANNEL_PERIOD);
  }

  /**
   * Decode an ANT+ heart rate monitor message.
   * @param antMessage The byte array received from the heart rate monitor.
   */
  @Override
  public void handleBroadcastData(byte[] antMessage, AntSensorDataCollector c) {
    int bpm = (int) antMessage[8] & 0xFF;
    Log.d(TAG, "now:" + System.currentTimeMillis() + " heart rate=" + bpm);
    c.setHeartRate(bpm);
  }
};
