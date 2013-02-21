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
package com.google.android.apps.mytracks.io.fusiontables;

import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;
import com.google.android.apps.mytracks.io.spreadsheets.SendSpreadsheetsActivity;

import android.test.AndroidTestCase;

/**
 * Tests the {@link SendFusionTablesActivity}.
 * 
 * @author Youtao Liu
 */
public class SendFusionTablesActivityTest extends AndroidTestCase {

  private SendFusionTablesActivity sendFusionTablesActivity;
  private SendRequest sendRequest;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sendRequest = new SendRequest(1L);
    sendFusionTablesActivity = new SendFusionTablesActivity();
  }

  /**
   * Tests the method
   * {@link SendFusionTablesActivity#getNextClass(SendRequest, boolean)}. Sets
   * the flags of "sendSpreadsheets" and "cancel" to false and false.
   */
  public void testGetNextClass_notCancelNotSendSpreadsheets() {
    sendRequest.setSendSpreadsheets(false);
    Class<?> next = sendFusionTablesActivity.getNextClass(sendRequest, false);
    assertEquals(UploadResultActivity.class, next);
  }

  /**
   * Tests the method
   * {@link SendFusionTablesActivity#getNextClass(SendRequest, boolean)}. Sets
   * the flags of "sendSpreadsheets" and "cancel" to true and false.
   */
  public void testGetNextClass_notCancelSendSpreadsheets() {
    sendRequest.setSendSpreadsheets(true);
    Class<?> next = sendFusionTablesActivity.getNextClass(sendRequest, false);
    assertEquals(SendSpreadsheetsActivity.class, next);
  }

  /**
   * Tests the method
   * {@link SendFusionTablesActivity#getNextClass(SendRequest,boolean)}. Sets
   * the flags of "sendSpreadsheets" and "cancel" to true and true.
   */
  public void testGetNextClass_cancelSendSpreadsheets() {
    sendRequest.setSendSpreadsheets(true);
    Class<?> next = sendFusionTablesActivity.getNextClass(sendRequest, true);
    assertEquals(UploadResultActivity.class, next);
  }

  /**
   * Tests the method
   * {@link SendFusionTablesActivity#getNextClass(SendRequest,boolean)}. Sets
   * the flags of "sendSpreadsheets" and "cancel" to false and true.
   */
  public void testGetNextClass_cancelNotSendSpreadsheets() {
    sendRequest.setSendSpreadsheets(false);
    Class<?> next = sendFusionTablesActivity.getNextClass(sendRequest, true);
    assertEquals(UploadResultActivity.class, next);
  }

}
