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

import android.test.AndroidTestCase;

public class AntChannelIdMessageTest extends AndroidTestCase {
  public void testParse() {
    byte[] rawMessage = {
        0,  // channel number
        0x34, 0x12,  // device number
        (byte) 0xaa,  // device type id
        (byte) 0xbb,  // transmission type
    };

    AntChannelIdMessage message = new AntChannelIdMessage(rawMessage);
    assertEquals(0, message.getChannelNumber());
    assertEquals(0x1234, message.getDeviceNumber());
    assertEquals((byte) 0xaa, message.getDeviceTypeId());
    assertEquals((byte) 0xbb, message.getTransmissionType());
  }
}
