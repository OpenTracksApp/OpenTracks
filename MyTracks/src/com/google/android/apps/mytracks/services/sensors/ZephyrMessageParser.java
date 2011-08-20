/*
 * Copyright 2010 Google Inc.
 * Copyright 2011 Dominik Ršttsches <d.roettsches@gmail.com>
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
import com.google.android.apps.mytracks.content.Sensor;

import android.util.Log;

import java.util.LinkedList;

/**
 * An implementation of a Sensor MessageParser for Zephyr.
 *
 * @author Sandor Dornbush
 */
public class ZephyrMessageParser implements MessageParser {

  public static final int ZEPHYR_HXM_BYTE_STX = 0;
  public static final int ZEPHYR_HXM_BYTE_CRC = 58;
  public static final int ZEPHYR_HXM_BYTE_ETX = 59;
  
  private StrideReadings strideReadings;
  
  public class StrideReadings {
    class StrideReading {
      public int timeMs;
      public int numStrides;
      
      StrideReading(int newTimeMs, int newNumStrides) {
        timeMs = newTimeMs;
        numStrides = newNumStrides;
      }
    }
    
    private LinkedList<StrideReading> strideReadingsHistory;
    private static final int NUM_READINGS_FOR_AVERAGE = 10;
    private static final int MIN_READINGS_FOR_AVERAGE = 5;
    public static final int CADENCE_NOT_AVAILABLE = -1;
    
    public StrideReadings() {
      strideReadingsHistory = new LinkedList<StrideReading>();
    }

    public void updateStrideReading(int timeInMs, int numStrides) {
      // HRM/HxM docs say, transmission frequency is 1 Hz, 
      // let's keep last NUM_READINGS_FOR_AVERAGE readings.
      // TODO: Calibrate this using a reliable footpod / cadence sensor, 
      // otherwise perhaps use heartbeat timestamp for calculation. 
      strideReadingsHistory.addFirst(new StrideReading(timeInMs, numStrides));
      while(strideReadingsHistory.size() > NUM_READINGS_FOR_AVERAGE) {
        strideReadingsHistory.removeLast();
      }
    }
    
    public int getCadence() {
      if(strideReadingsHistory.size() < MIN_READINGS_FOR_AVERAGE) {
        // Bail out if we cannot really get a meaningful average yet.
        return CADENCE_NOT_AVAILABLE;
      }
      // compute assuming 1 stride reading/second
      int timeSinceOldestReadingSecs = strideReadingsHistory.size() - 1; 
      int stridesThen = strideReadingsHistory.getLast().numStrides;
      int stridesNow = strideReadingsHistory.getFirst().numStrides;
      // Contrary to documentation stride value seems to roll over every 127 strides.
      return Math.round( (float)((stridesNow - stridesThen) % 127) / 
          timeSinceOldestReadingSecs * 60);
    }
  }
  
  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < buffer.length; i++) {
      sb.append(String.format("%02X", buffer[i]));
    }
    Log.w(Constants.TAG, "Got zephyr data: " + sb);

    // Device Firmware ID, Firmware Version, Hardware ID, Hardware Version
    // 0x1A00316550003162 produces erroneous values for Cadence and needs
    // a workaround based on the stride counter.
    String hardwareFirmwareId = sb.substring(6, 22);
    boolean needsWorkaround = hardwareFirmwareId.equals("1A00316550003162");
    Log.w(Constants.TAG, "FW & HW Ids & Version " + hardwareFirmwareId + " needs workaround: " + needsWorkaround);

    Sensor.SensorData.Builder heartrate = Sensor.SensorData.newBuilder()
      .setValue(buffer[12] & 0xFF)
      .setState(Sensor.SensorState.SENDING);
    
    Sensor.SensorData.Builder batteryLevel = Sensor.SensorData.newBuilder()
      .setValue(buffer[11])
      .setState(Sensor.SensorState.SENDING);
    
    Sensor.SensorData.Builder cadence = Sensor.SensorData.newBuilder();

    if(!needsWorkaround) {
      cadence = cadence
        .setValue(SensorUtils.unsignedShortToIntLittleEndian(buffer, 56) / 16)
        .setState(Sensor.SensorState.SENDING);
    } else {
      if(strideReadings == null) {
        strideReadings = new StrideReadings();
      }
  
      strideReadings.updateStrideReading(
          SensorUtils.unsignedShortToIntLittleEndian(buffer, 14), 
          buffer[54] & 0xFF);
      
      if(strideReadings.getCadence() != StrideReadings.CADENCE_NOT_AVAILABLE) {
        cadence = cadence.setValue(strideReadings.getCadence())
          .setState(Sensor.SensorState.SENDING);  
      } else {
        cadence = cadence.setValue(0).setState(Sensor.SensorState.NONE);
      }
    }
      
    Sensor.SensorDataSet sds =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis())
      .setBatteryLevel(batteryLevel)
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
