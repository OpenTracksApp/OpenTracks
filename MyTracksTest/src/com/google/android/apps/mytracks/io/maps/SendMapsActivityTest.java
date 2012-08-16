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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.docs.SendDocsActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;

import android.test.AndroidTestCase;

/**
 * Tests the {@link SendMapsActivity}.
 * 
 * @author Youtao Liu
 */
public class SendMapsActivityTest extends AndroidTestCase {

  private SendMapsActivity sendMapsActivity;
  private SendRequest sendRequest;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sendRequest = new SendRequest(1L);
    sendMapsActivity = new SendMapsActivity();
  }

  /**
   * Tests the method
   * {@link SendMapsActivity#getNextClass(SendRequest, boolean)}. Sets the flags
   * of "sendFusionTables","sendDocs" and "cancel" to true, true and false.
   */
  public void testGetNextClass_notCancelSendFusionTables() {
    sendRequest.setSendFusionTables(true);
    sendRequest.setSendDocs(true);
    Class<?> next = sendMapsActivity.getNextClass(sendRequest, false);
    assertEquals(SendFusionTablesActivity.class, next);
  }

  /**
   * Tests the method
   * {@link SendMapsActivity#getNextClass(SendRequest, boolean)}. Sets the flags
   * of "sendFusionTables","sendDocs" and "cancel" to false, true and false.
  */
  public void testGetNextClass_notCancelSendDocs() {
    sendRequest.setSendFusionTables(false);
    sendRequest.setSendDocs(true);
    Class<?> next = sendMapsActivity.getNextClass(sendRequest, false);
    assertEquals(SendDocsActivity.class, next);
  }
  
  /**
   * Tests the method
   * {@link SendMapsActivity#getNextClass(SendRequest, boolean)}. Sets the flags
   * of "sendFusionTables","sendDocs" and "cancel" to false, false and false.
   */
  public void testGetNextClass_notCancelNotSend() {
    sendRequest.setSendFusionTables(false);
    sendRequest.setSendDocs(false);
    Class<?> next = sendMapsActivity.getNextClass(sendRequest, false);
    assertEquals(UploadResultActivity.class, next);
  }

  /**
   * Tests the method
   * {@link SendMapsActivity#getNextClass(SendRequest, boolean)}. Sets the flags
   * of "sendFusionTables","sendDocs" and "cancel" to true, true and true.
   */
  public void testGetNextClass_cancelSendDocs() {
    sendRequest.setSendFusionTables(true);
    sendRequest.setSendDocs(true);
    Class<?> next = sendMapsActivity.getNextClass(sendRequest, true);
    assertEquals(UploadResultActivity.class, next);
  }

}