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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.Sensor;

/**
 * An implementation of a Sensor MessageParser for Zephyr.
 *
 * @author Sandor Dornbush
 */
public class ZephyrMessageParser implements MessageParser {

  public static final int ZEPHYR_HXM_BYTE_STX = 0;
  public static final int ZEPHYR_HXM_BYTE_CRC = 58;
  public static final int ZEPHYR_HXM_BYTE_ETX = 59;
	
  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < buffer.length; i++) {
      sb.append(String.format("%02X", buffer[i]));
    }
    Log.w(Constants.TAG, "Got zephyr data: " + sb);
    // Heart Rate
    Sensor.SensorData.Builder heartrate = Sensor.SensorData.newBuilder()
        .setValue(buffer[12])
        .setState(Sensor.SensorState.SENDING);
    // Changes Nico Laum (Power and Cadence)
    Sensor.SensorData.Builder power = Sensor.SensorData.newBuilder()
	    .setValue(buffer[11])
	    .setState(Sensor.SensorState.SENDING);
    Sensor.SensorData.Builder cadence = Sensor.SensorData.newBuilder()
	    .setValue(SensorUtils.unsignedShortToIntLittleEndian(buffer, 56) / 16)
	    .setState(Sensor.SensorState.SENDING);
    
    Sensor.SensorDataSet sds =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis())
      .setPower(power)
      .setHeartRate(heartrate)
      .setCadence(cadence)
      .build();
    
    return sds;
  }

  @Override
  public boolean isValid(byte[] buffer) {
    // Check STX (Start of Text), ETX (End of Text) and CRC Checksum
    return buffer.length > ZEPHYR_HXM_BYTE_ETX
        && buffer[ZEPHYR_HXM_BYTE_STX] == 0x02
        && buffer[ZEPHYR_HXM_BYTE_ETX] == 0x03
        && SensorUtils.getCrc8(buffer, 3, 55) == buffer[ZEPHYR_HXM_BYTE_CRC];
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
