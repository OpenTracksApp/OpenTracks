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

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntMesg;

import android.test.AndroidTestCase;

public class AntChannelResponseMessageTest extends AndroidTestCase {
  public void testParse() {
    byte[] rawMessage = {
        0,
        AntMesg.MESG_EVENT_ID,
        AntDefine.EVENT_RX_SEARCH_TIMEOUT
    };
    AntChannelResponseMessage message = new AntChannelResponseMessage(rawMessage);

    assertEquals(0, message.getChannelNumber());
    assertEquals(AntMesg.MESG_EVENT_ID, message.getMessageId());
    assertEquals(AntDefine.EVENT_RX_SEARCH_TIMEOUT, message.getMessageCode());
  }
}
