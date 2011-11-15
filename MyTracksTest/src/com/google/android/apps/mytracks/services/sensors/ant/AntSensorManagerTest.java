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

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

public class AntSensorManagerTest extends AndroidTestCase {
  private class TestAntSensorManager extends AntSensorManager {

    public TestAntSensorManager(Context context) {
      super(context);
    }

    public byte messageId;
    public byte[] messageData;

    @Override
    protected void setupAntSensorChannels() {}

    @SuppressWarnings("deprecation")
    @Override
    public void handleMessage(byte[] rawMessage) {
      super.handleMessage(rawMessage);
    }

    @SuppressWarnings("hiding")
    @Override
    public boolean handleMessage(byte messageId, byte[] messageData) {
      this.messageId = messageId;
      this.messageData = messageData;
      return true;
    }
  }

  private TestAntSensorManager sensorManager;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sensorManager = new TestAntSensorManager(getContext());
  }  

  public void testSimple() {
    byte[] rawMessage = {
        0x03,              // length
        0x12,              // message id
        0x11, 0x22, 0x33,  // body
    };

    byte[] expectedBody = { 0x11, 0x22, 0x33 };

    sensorManager.handleMessage(rawMessage);
    assertEquals((byte) 0x12, sensorManager.messageId);
    MoreAsserts.assertEquals(expectedBody, sensorManager.messageData);
  }

  public void testTooShort() {
    byte[] rawMessage = {
        0x53,  // length
        0x12   // message id
    };

    sensorManager.handleMessage(rawMessage);
    assertEquals(0, sensorManager.messageId);
    assertNull(sensorManager.messageData);
  }

  public void testLengthWrong() {
    byte[] rawMessage = {
        0x53,  // length
        0x12,  // message id
        0x34,  // body
    };

    sensorManager.handleMessage(rawMessage);
    assertEquals(0, sensorManager.messageId);
    assertNull(sensorManager.messageData);
  }
}
