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
 * This class decodes and encapsulates an ANT Channel Response / Event message.
 * (ANT Message ID 0x40, Protocol & Usage guide v4.2 section 9.5.6.1)
 *
 * @author Matthew Simmons
 */
public class AntChannelResponseMessage extends AntMessage {
  private byte channelNumber;
  private byte messageId;
  private byte messageCode;

  public AntChannelResponseMessage(byte[] messageData) {
    channelNumber = messageData[0];
    messageId = messageData[1];
    messageCode = messageData[2];
  }

  /** Returns the channel number */
  public byte getChannelNumber() {
    return channelNumber;
  }

  /** Returns the ID of the message being responded to */
  public byte getMessageId() {
    return messageId;
  }

  /** Returns the code for a specific response or event */
  public byte getMessageCode() {
    return messageCode;
  }
}
