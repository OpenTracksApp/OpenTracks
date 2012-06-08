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
    sendRequest = new SendRequest(1);
  }

  /**
   * Tests the method {@link SendRequest#getTrackId()}. The value should be set
   * to 1 when it is initialed in setup method.
   */
  public void testGetTrackId() {
    assertEquals(1, sendRequest.getTrackId());
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
   * Tests SendRequest.CREATOR.createFromParcel when all values are true.
   */
  public void testCreateFromParcel_true() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(2);
    parcel.writeString("");
    parcel.writeString("");
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeString(MAPID);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(2, sendRequest.getTrackId());
    assertEquals("", sendRequest.getSharingAppPackageName());
    assertEquals("", sendRequest.getSharingAppClassName());
    assertTrue(sendRequest.isSendMaps());
    assertTrue(sendRequest.isSendFusionTables());
    assertTrue(sendRequest.isSendDocs());
    assertTrue(sendRequest.isNewMap());
    assertEquals(account, sendRequest.getAccount());
    assertEquals(MAPID, sendRequest.getMapId());
    assertTrue(sendRequest.isMapsSuccess());
    assertTrue(sendRequest.isFusionTablesSuccess());
    assertTrue(sendRequest.isDocsSuccess());
  }

  /**
   * Tests SendRequest.CREATOR.createFromParcel when all values are false.
   */
  public void testCreateFromParcel_false() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(4);
    parcel.writeString(null);
    parcel.writeString(null);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeString(MAPID);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(4, sendRequest.getTrackId());
    assertNull(sendRequest.getSharingAppPackageName());
    assertNull(sendRequest.getSharingAppClassName());
    assertFalse(sendRequest.isSendMaps());
    assertFalse(sendRequest.isSendFusionTables());
    assertFalse(sendRequest.isSendDocs());
    assertFalse(sendRequest.isNewMap());
    assertEquals(account, sendRequest.getAccount());
    assertEquals(MAPID, sendRequest.getMapId());
    assertFalse(sendRequest.isMapsSuccess());
    assertFalse(sendRequest.isFusionTablesSuccess());
    assertFalse(sendRequest.isDocsSuccess());
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel, int)} with default values.
   */
  public void testWriteToParcel_default() {
    sendRequest = new SendRequest(1);
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    sendRequest.writeToParcel(parcel, 1);
    parcel.setDataPosition(0);
    long trackId = parcel.readLong();
    String sharingAppPackageName = parcel.readString();
    String sharingAppClassName = parcel.readString();
    boolean sendMaps = parcel.readByte() == 1;
    boolean sendFusionTables = parcel.readByte() == 1;
    boolean sendDocs = parcel.readByte() == 1;
    boolean newMap = parcel.readByte() == 1;
    Parcelable account = parcel.readParcelable(null);
    String mapId = parcel.readString();
    boolean mapsSuccess = parcel.readByte() == 1;
    boolean fusionTablesSuccess = parcel.readByte() == 1;
    boolean docsSuccess = parcel.readByte() == 1;
    assertEquals(1, trackId);
    assertNull(sharingAppPackageName);
    assertNull(sharingAppClassName);
    assertFalse(sendMaps);
    assertFalse(sendFusionTables);
    assertFalse(sendDocs);
    assertFalse(newMap);
    assertNull(account);
    assertNull(mapId);
    assertFalse(mapsSuccess);
    assertFalse(fusionTablesSuccess);
    assertFalse(docsSuccess);
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel, int)}.
   */
  public void testWriteToParcel() {
    sendRequest = new SendRequest(4);
    sendRequest.setSharingAppPackageName("package");
    sendRequest.setSharingAppClassName("class"); 
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
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    sendRequest.writeToParcel(parcel, 1);
    parcel.setDataPosition(0);
    long trackId = parcel.readLong();
    String sharingAppPackageName = parcel.readString();
    String sharingAppClassName = parcel.readString();
    boolean sendMaps = parcel.readByte() == 1;
    boolean sendFusionTables = parcel.readByte() == 1;
    boolean sendDocs = parcel.readByte() == 1;
    boolean newMap = parcel.readByte() == 1;
    Parcelable account = parcel.readParcelable(null);
    String mapId = parcel.readString();
    boolean mapsSuccess = parcel.readByte() == 1;
    boolean fusionTablesSuccess = parcel.readByte() == 1;
    boolean docsSuccess = parcel.readByte() == 1;
    assertEquals(4, trackId);
    assertEquals("package", sharingAppPackageName);
    assertEquals("class", sharingAppClassName);
    assertTrue(sendMaps);
    assertTrue(sendFusionTables);
    assertTrue(sendDocs);
    assertTrue(newMap);
    assertEquals(accountNew, account);
    assertEquals(MAPID, mapId);
    assertTrue(mapsSuccess);
    assertTrue(fusionTablesSuccess);
    assertTrue(docsSuccess);
  }
}
