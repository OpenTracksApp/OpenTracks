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

import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;

import java.util.Arrays;

/**
 * An implementation of a Sensor MessageParser for Zephyr.
 *
 * @author Sandor Dornbush
 * @author Dominik Rttsches
 */
public class ZephyrMessageParser implements MessageParser {

  public static final int ZEPHYR_HXM_BYTE_STX = 0;
  public static final int ZEPHYR_HXM_BYTE_CRC = 58;
  public static final int ZEPHYR_HXM_BYTE_ETX = 59;
  
  private static final byte[] CADENCE_BUG_FW_ID = {0x1A, 0x00, 0x31, 0x65, 0x50, 0x00, 0x31, 0x62};
  
  private StrideReadings strideReadings;
  
  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    Sensor.SensorDataSet.Builder sds =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis());

    Sensor.SensorData.Builder heartrate = Sensor.SensorData.newBuilder()
      .setValue(buffer[12] & 0xFF)
      .setState(Sensor.SensorState.SENDING);
    sds.setHeartRate(heartrate);
    
    Sensor.SensorData.Builder batteryLevel = Sensor.SensorData.newBuilder()
      .setValue(buffer[11])
      .setState(Sensor.SensorState.SENDING);
    sds.setBatteryLevel(batteryLevel);
    
    setCadence(sds, buffer);
    
    return sds.build();
  }

  private void setCadence(Sensor.SensorDataSet.Builder sds, byte[] buffer) {
    // Device Firmware ID, Firmware Version, Hardware ID, Hardware Version
    // 0x1A00316550003162 produces erroneous values for Cadence and needs
    // a workaround based on the stride counter.
    // Firmware values range from field 3 to 10 (inclusive) of the byte buffer.
    byte[] hardwareFirmwareId = ApiAdapterFactory.getApiAdapter().copyByteArray(buffer, 3, 11);
    Sensor.SensorData.Builder cadence = Sensor.SensorData.newBuilder();

    if (Arrays.equals(hardwareFirmwareId, CADENCE_BUG_FW_ID)) {
      if (strideReadings == null) {
        strideReadings = new StrideReadings();
      }
      strideReadings.updateStrideReading(buffer[54] & 0xFF);
      
      if (strideReadings.getCadence() != StrideReadings.CADENCE_NOT_AVAILABLE) {
        cadence.setValue(strideReadings.getCadence()).setState(Sensor.SensorState.SENDING);
      }
    } else {
      cadence
        .setValue(SensorUtils.unsignedShortToIntLittleEndian(buffer, 56) / 16)
        .setState(Sensor.SensorState.SENDING);
    }
    sds.setCadence(cadence);
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
