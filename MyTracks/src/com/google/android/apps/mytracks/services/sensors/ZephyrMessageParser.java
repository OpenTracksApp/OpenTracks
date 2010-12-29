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

import android.util.Log;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.content.Sensor;

/**
 * An implementation of a SensorData parser for Zephyr HRM.
 *
 * @author Sandor Dornbush
 */
public class ZephyrMessageParser implements MessageParser {

  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < buffer.length; i++) {
      sb.append(String.format("%02X", buffer[i]));
    }
    Log.w(MyTracksConstants.TAG, "Got zephyr data: " + sb);
    // The provided units are 1/16 strides per minute.
    // int cadence = SensorUtils.unsignedShortToInt(buffer, 56);
    // Heart Rate
    Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
      .setValue(buffer[12] & 0xFF)
      .setState(Sensor.SensorState.SENDING);
      // Cadence
      //.setCadence(cadence / 16)
      //.build();
    Sensor.SensorDataSet sds =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis())
      .setHeartRate(b)
      .build();
    
    return sds;
  }

  @Override
  public boolean isValid(byte[] buffer) {
    // TODO crc etc.
    return buffer[0] == 0x02 && buffer[59] == 0x03;
  }

  @Override
  public int getFrameSize() {
    return 60;
  }

  @Override
  public int findNextAlignment(byte[] buffer) {
    // TODO test or understand this code.
    for (int i = 0; i < buffer.length - 1; i++) {
      if (buffer[i] == 0x03 && buffer[i+1] == 0x02) {
        return i;
      }
    }
    return -1;
  }
}
