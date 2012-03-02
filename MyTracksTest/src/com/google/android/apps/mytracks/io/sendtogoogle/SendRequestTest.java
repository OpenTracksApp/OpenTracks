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
package com.google.android.apps.mytracks.io.sendtogoogle;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;

/**
 * Tests the {@link SendRequest}.
 * 
 * @author Youtao Liu
 */
public class SendRequestTest extends AndroidTestCase {
  private SendRequest sendRequest;

  final static private String ACCOUNTNAME = "testAccount1";
  final static private String ACCOUNTYPE = "testType1";
  final static private String MAPID = "mapId1";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sendRequest = new SendRequest(1, true, true, true);
  }

  /**
   * Tests the method {@link SendRequest#getTrackId()}. The value should be set
   * to 1 when it is initialed in setup method.
   */
  public void testGetTrackId() {
    assertEquals(1, sendRequest.getTrackId());
  }

  /**
   * Tests the method {@link SendRequest#isShowMaps()}. The value should be set
   * to true when it is initialed in setup method.
   */
  public void testIsShowMaps() {
    assertEquals(true, sendRequest.isShowMaps());
  }

  /**
   * Tests the method {@link SendRequest#isShowFusionTables()}. The value should
   * be set to true when it is initialed in setup method.
   */
  public void testIsShowFusionTables() {
    assertEquals(true, sendRequest.isShowFusionTables());
  }

  /**
   * Tests the method {@link SendRequest#isShowDocs()}. The value should be set
   * to true when it is initialed in setup method.
   */
  public void testIsShowDocs() {
    assertEquals(true, sendRequest.isShowDocs());
  }

  public void testIsShowAll() {
    assertEquals(true, sendRequest.isShowAll());
    sendRequest = new SendRequest(1, false, true, true);
    assertEquals(false, sendRequest.isShowAll());
    sendRequest = new SendRequest(1, true, false, true);
    assertEquals(false, sendRequest.isShowAll());
    sendRequest = new SendRequest(1, true, true, false);
    assertEquals(false, sendRequest.isShowAll());
    sendRequest = new SendRequest(1, false, true, false);
    assertEquals(false, sendRequest.isShowAll());
    sendRequest = new SendRequest(1, false, false, false);
    assertEquals(false, sendRequest.isShowAll());
  }

  public void testIsSendMaps() {
    assertEquals(false, sendRequest.isSendMaps());
    sendRequest.setSendMaps(true);
    assertEquals(true, sendRequest.isSendMaps());
  }

  public void testIsSendFusionTables() {
    assertEquals(false, sendRequest.isSendFusionTables());
    sendRequest.setSendFusionTables(true);
    assertEquals(true, sendRequest.isSendFusionTables());
  }

  /**
   * Tests the method {@link SendRequest#isSendDocs()}. The value should be set
   * to false which is its default value when it is initialed in setup method.
   */
  public void testIsSendDocs() {
    assertEquals(false, sendRequest.isSendDocs());
    sendRequest.setSendDocs(true);
    assertEquals(true, sendRequest.isSendDocs());
  }

  /**
   * Tests the method {@link SendRequest#isNewMap()}. The value should be set to
   * false which is its default value when it is initialed in setup method.
   */
  public void testIsNewMap() {
    assertEquals(false, sendRequest.isNewMap());
    sendRequest.setNewMap(true);
    assertEquals(true, sendRequest.isNewMap());
  }

  /**
   * Tests the method {@link SendRequest#getAccount()}. The value should be set
   * to null which is its default value when it is initialed in setup method.
   */
  public void testGetAccount() {
    assertEquals(null, sendRequest.getAccount());
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    sendRequest.setAccount(account);
    assertEquals(account, sendRequest.getAccount());
  }

  /**
   * Tests the method {@link SendRequest#getMapId()}. The value should be set to
   * null which is its default value when it is initialed in setup method.
   */
  public void testGetMapId() {
    assertEquals(null, sendRequest.getMapId());
    sendRequest.setMapId("1");
    assertEquals("1", "1");
  }

  /**
   * Tests the method {@link SendRequest#isMapsSuccess()}. The value should be
   * set to false which is its default value when it is initialed in setup
   * method.
   */
  public void testIsMapsSuccess() {
    assertEquals(false, sendRequest.isMapsSuccess());
    sendRequest.setMapsSuccess(true);
    assertEquals(true, sendRequest.isMapsSuccess());
  }

  /**
   * Tests the method {@link SendRequest#isFusionTablesSuccess()}. The value
   * should be set to false which is its default value when it is initialed in
   * setup method.
   */
  public void testIsFusionTablesSuccess() {
    assertEquals(false, sendRequest.isFusionTablesSuccess());
    sendRequest.setFusionTablesSuccess(true);
    assertEquals(true, sendRequest.isFusionTablesSuccess());
  }

  /**
   * Tests the method {@link SendRequest#isDocsSuccess()}. The value should be
   * set to false which is its default value when it is initialed in setup
   * method.
   */
  public void testIsDocsSuccess() {
    assertEquals(false, sendRequest.isDocsSuccess());
    sendRequest.setDocsSuccess(true);
    assertEquals(true, sendRequest.isDocsSuccess());
  }

  /**
   * Tests {@link SendRequest#SendRequest(Parcel in)} when all values are true.
   */
  public void testCreateFromParcel_true() {
    Parcel sourceParcel = Parcel.obtain();
    sourceParcel.setDataPosition(0);
    sourceParcel.writeLong(2);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    sourceParcel.writeParcelable(account, 0);
    sourceParcel.writeString(MAPID);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.writeByte((byte) 1);
    sourceParcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(sourceParcel);
    assertEquals(2, sendRequest.getTrackId());
    assertEquals(true, sendRequest.isShowMaps());
    assertEquals(true, sendRequest.isShowFusionTables());
    assertEquals(true, sendRequest.isShowDocs());
    assertEquals(true, sendRequest.isSendMaps());
    assertEquals(true, sendRequest.isSendFusionTables());
    assertEquals(true, sendRequest.isSendDocs());
    assertEquals(true, sendRequest.isNewMap());
    assertEquals(account, sendRequest.getAccount());
    assertEquals(MAPID, sendRequest.getMapId());
    assertEquals(true, sendRequest.isMapsSuccess());
    assertEquals(true, sendRequest.isFusionTablesSuccess());
    assertEquals(true, sendRequest.isDocsSuccess());
  }

  /**
   * Tests {@link SendRequest#SendRequest(Parcel in)} when all values are false.
   */
  public void testCreateFromParcel_false() {
    Parcel sourceParcel = Parcel.obtain();
    sourceParcel.setDataPosition(0);
    sourceParcel.writeLong(4);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    sourceParcel.writeParcelable(account, 0);
    sourceParcel.writeString(MAPID);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.writeByte((byte) 0);
    sourceParcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(sourceParcel);
    assertEquals(4, sendRequest.getTrackId());
    assertEquals(false, sendRequest.isShowMaps());
    assertEquals(false, sendRequest.isShowFusionTables());
    assertEquals(false, sendRequest.isShowDocs());
    assertEquals(false, sendRequest.isSendMaps());
    assertEquals(false, sendRequest.isSendFusionTables());
    assertEquals(false, sendRequest.isSendDocs());
    assertEquals(false, sendRequest.isNewMap());
    assertEquals(account, sendRequest.getAccount());
    assertEquals(MAPID, sendRequest.getMapId());
    assertEquals(false, sendRequest.isMapsSuccess());
    assertEquals(false, sendRequest.isFusionTablesSuccess());
    assertEquals(false, sendRequest.isDocsSuccess());
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel in)} when all input values
   * are true or affirmative.
   */
  public void testWriteToParcel_allTrue() {
    sendRequest = new SendRequest(1, false, false, false);
    Parcel parcelWrite1st = Parcel.obtain();
    parcelWrite1st.setDataPosition(0);
    sendRequest.writeToParcel(parcelWrite1st, 1);
    parcelWrite1st.setDataPosition(0);
    long trackId = parcelWrite1st.readLong();
    boolean showMaps = parcelWrite1st.readByte() == 1;
    boolean showFusionTables = parcelWrite1st.readByte() == 1;
    boolean showDocs = parcelWrite1st.readByte() == 1;
    boolean sendMaps = parcelWrite1st.readByte() == 1;
    boolean sendFusionTables = parcelWrite1st.readByte() == 1;
    boolean sendDocs = parcelWrite1st.readByte() == 1;
    boolean newMap = parcelWrite1st.readByte() == 1;
    Parcelable account = parcelWrite1st.readParcelable(null);
    String mapId = parcelWrite1st.readString();
    boolean mapsSuccess = parcelWrite1st.readByte() == 1;
    boolean fusionTablesSuccess = parcelWrite1st.readByte() == 1;
    boolean docsSuccess = parcelWrite1st.readByte() == 1;
    assertEquals(1, trackId);
    assertEquals(false, showMaps);
    assertEquals(false, showFusionTables);
    assertEquals(false, showDocs);
    assertEquals(false, sendMaps);
    assertEquals(false, sendFusionTables);
    assertEquals(false, sendDocs);
    assertEquals(false, newMap);
    assertEquals(null, account);
    assertEquals(null, mapId);
    assertEquals(false, mapsSuccess);
    assertEquals(false, fusionTablesSuccess);
    assertEquals(false, docsSuccess);
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel in)} when all input values
   * are false or negative.
   */
  public void testWriteToParcel_allFalse() {
    sendRequest = new SendRequest(4, true, true, true);
    sendRequest.setSendMaps(true);
    sendRequest.setSendFusionTables(true);
    sendRequest.setSendDocs(true);
    sendRequest.setNewMap(true);
    Account accountNew = new Account(ACCOUNTNAME + "2", ACCOUNTYPE + "2");
    sendRequest.setAccount(accountNew);
    sendRequest.setMapId(MAPID);
    sendRequest.setMapsSuccess(true);
    sendRequest.setFusionTablesSuccess(true);
    sendRequest.setDocsSuccess(true);
    Parcel parcelWrite2nd = Parcel.obtain();
    parcelWrite2nd.setDataPosition(0);
    sendRequest.writeToParcel(parcelWrite2nd, 1);
    parcelWrite2nd.setDataPosition(0);
    long trackId = parcelWrite2nd.readLong();
    boolean showMaps = parcelWrite2nd.readByte() == 1;
    boolean showFusionTables = parcelWrite2nd.readByte() == 1;
    boolean showDocs = parcelWrite2nd.readByte() == 1;
    boolean sendMaps = parcelWrite2nd.readByte() == 1;
    boolean sendFusionTables = parcelWrite2nd.readByte() == 1;
    boolean sendDocs = parcelWrite2nd.readByte() == 1;
    boolean newMap = parcelWrite2nd.readByte() == 1;
    Parcelable account = parcelWrite2nd.readParcelable(null);
    String mapId = parcelWrite2nd.readString();
    boolean mapsSuccess = parcelWrite2nd.readByte() == 1;
    boolean fusionTablesSuccess = parcelWrite2nd.readByte() == 1;
    boolean docsSuccess = parcelWrite2nd.readByte() == 1;
    assertEquals(4, trackId);
    assertEquals(true, showMaps);
    assertEquals(true, showFusionTables);
    assertEquals(true, showDocs);
    assertEquals(true, sendMaps);
    assertEquals(true, sendFusionTables);
    assertEquals(true, sendDocs);
    assertEquals(true, newMap);
    assertEquals(accountNew, account);
    assertEquals(MAPID, mapId);
    assertEquals(true, mapsSuccess);
    assertEquals(true, fusionTablesSuccess);
    assertEquals(true, docsSuccess);
  }

}
