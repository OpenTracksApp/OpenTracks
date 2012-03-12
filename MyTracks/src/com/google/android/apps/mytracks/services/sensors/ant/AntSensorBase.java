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
 * Base class for ANT+ sensors.
 *
 * @author Laszlo Molnar
 */

public abstract class AntSensorBase {
  /*
   * These constants are defined by the ANT+ spec.
   */
  public static final byte NETWORK_NUMBER = 1;
  public static final byte RF_FREQUENCY = 57;

  private short deviceNumber;
  private final byte deviceType;
  private final short channelPeriod;

  AntSensorBase(short deviceNumber, byte deviceType,
                String deviceTypeString, short channelPeriod) {
    this.deviceNumber = deviceNumber;
    this.deviceType = deviceType;
    this.channelPeriod = channelPeriod;

    Log.i(TAG, "Will pair with " + deviceTypeString + " device: " + ((int) deviceNumber & 0xFFFF));
  }

  public abstract void handleBroadcastData(byte[] antMessage, AntSensorDataCollector c);

  public void setDeviceNumber(short dn) {
    deviceNumber = dn;
  }

  public short getDeviceNumber() {
    return deviceNumber;
  }

  public byte getNetworkNumber() {
    return NETWORK_NUMBER;
  }

  public byte getFrequency() {
    return RF_FREQUENCY;
  }

  public byte getDeviceType() {
    return deviceType;
  }

  public short getChannelPeriod() {
    return channelPeriod;
  }
};
