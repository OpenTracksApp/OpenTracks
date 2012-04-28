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
package com.google.android.apps.mytracks.services.sensors.ant;

/**
 * This class decodes and encapsulates an ANT Channel ID message.
 * (ANT Message ID 0x51, Protocol & Usage guide v4.2 section 9.5.7.2)
 *
 * @author Matthew Simmons
 */
public class AntChannelIdMessage extends AntMessage {
  private byte channelNumber;
  private short deviceNumber;
  private byte deviceTypeId;
  private byte transmissionType;

  public AntChannelIdMessage(byte[] messageData) {
    channelNumber = messageData[0];
    deviceNumber = decodeShort(messageData[1], messageData[2]);
    deviceTypeId = messageData[3];
    transmissionType = messageData[4];
  }

  /** Returns the channel number */
  public byte getChannelNumber() {
    return channelNumber;
  }

  /** Returns the device number */
  public short getDeviceNumber() {
    return deviceNumber;
  }

  /** Returns the device type */
  public byte getDeviceTypeId() {
    return deviceTypeId;
  }

  /** Returns the transmission type */
  public byte getTransmissionType() {
    return transmissionType;
  }
}
