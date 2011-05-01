/*
 * Copyright 2011 Google Inc.
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
 * An implementation of a SensorData parser for Polar Wearlink Bluetooth HRM.
 *
 * @author John R. Gerthoffer
 */
public class PolarMessageParser implements MessageParser {
  // Last Heart Rate value storage (in case of corrupt buffer)
  private int lastHeartRate = 0;                   
  
  @Override
  public Sensor.SensorDataSet parseBuffer(byte[] buffer) {
    StringBuilder sb = new StringBuilder();
    int heartRate = 0;
    
    // Due to a memory inconsistency issue, the packet does not always start at buffer[0].
    // While it works stepping thru in debug mode, it is not consistent when running real-time.
    // Note: After changing the BluetoothConnectionManager to pass a copy of 'buffer', 
    //       the problem was resolved!  Passing a buffer copy in threaded comm's is typical in my experience.  

    // Minimum length Polar packets is 8, so stop search 8 bytes before buffer ends.
    heartRate = lastHeartRate;                          // Default to use last value. (If our buffer is corrupted).
    for (int i = 0; i < buffer.length - 8; i++) {
      boolean bHdrOK = ((buffer[i] & 0xFF) == 0xFE); 
      boolean bChkOK = ((buffer[i+2] & 0xFF) == (0xFF - (buffer[i+1] & 0xFF))); 
      boolean bSeqOK = ((buffer[i+3] & 0xFF) < 16); 
      boolean bStatusOK = ((buffer[i+4] & 0xFF) > 128); // I've seen 0xF1 or 0xE1. 

      if (bHdrOK && bChkOK && bSeqOK && bStatusOK) {
        heartRate = buffer[i + 5] & 0xFF;
        lastHeartRate = heartRate;                      // Remember good value for next time.
        break;						                    // Let's go store our data.  
      }
    }

    // Heart Rate
    Sensor.SensorData.Builder b = Sensor.SensorData.newBuilder()
      .setValue(heartRate)
      .setState(Sensor.SensorState.SENDING);

    Sensor.SensorDataSet sds =
      Sensor.SensorDataSet.newBuilder()
      .setCreationTime(System.currentTimeMillis())
      .setHeartRate(b)
      .build();
    
    return sds;
  }

  /**
   * Applies packet validation rule to buffer
   * Parsing rule for a good Polar HRM packet;
   *   boolean goodHdr = ((buffer[0] & 0xFF) == 0xFE);
   *   boolean goodChk = ((buffer[2] & 0xFF) == (0xFF - (buffer[1] & 0xFF)));
   *   goodPacket = goodHdr && goodChk;
   *     
   * @param an array of bytes to parse
   * @return whether buffer has a valid packet starting at index zero
   */
  @Override
  public boolean isValid(byte[] buffer) {
	/**
	 *  Polar Bluetooth Wearlink packet example;
	 *   Hdr Len Chk Seq Status HeartRate RRInterval_16-bits
     *    FE  08  F7  06   F1      48          03 64
     *   where Hdr always = 0xFE, Chk = 0xFF - Len
     *               
     *  Additional packet examples;
     *    FE 08 F7 06 F1 48 03 64           
     *    FE 0A F5 06 F1 48 03 64 03 70
	 */
    boolean goodHdr = ((buffer[0] & 0xFF) == 0xFE);
    boolean goodChk = ((buffer[2] & 0xFF) == (0xFF - (buffer[1] & 0xFF)));
    return goodHdr && goodChk;    
  }

  /**
   * Polar uses variable packet sizes; 8, 10, 12, 14 and rarely 16.
   * The most frequent are 8 and 10.
   * We will wait for 16 bytes.
   * This way, we are assured of getting one good one.
   * 
   * @return the size of buffer needed to parse a good packet
   */
  @Override
  public int getFrameSize() {
	return 16;									
  }

  /**
   * Searches buffer for the beginning of a valid packet.
   * Parsing rule for a good Polar HRM packet;
   *   boolean goodHdr = ((buffer[0] & 0xFF) == 0xFE);
   *   boolean goodChk = ((buffer[2] & 0xFF) == (0xFF - (buffer[1] & 0xFF)));
   *   goodPacket = goodHdr && goodChk;
   *     
   * @param an array of bytes to parse
   */
  @Override
  public int findNextAlignment(byte[] buffer) {
    // Minimum length Polar packets is 8, so stop search 8 bytes before buffer ends.
    for (int i = 0; i < buffer.length - 8; i++) {
      if (((buffer[i] & 0xFF) == 0xFE) && ((buffer[i+2] & 0xFF) == (0xFF - (buffer[i+1] & 0xFF)))) {
	    return i;
      }
    }
    return -1;
  }
}
