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

  private final static String ACCOUNTNAME = "testAccount1";
  private final static String ACCOUNTYPE = "testType1";
  private final static String DRIVE_SHARE_EMAILS = "foo@foo.com";
  private final static String SHARE_URL = "url@url.com";

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
   * Tests the method {@link SendRequest#isSendSpreadsheets()}. The value should
   * be set to false which is its default value when it is initialed in setup
   * method.
   */
  public void testIsSendSpreadsheets() {
    assertEquals(false, sendRequest.isSendSpreadsheets());
    sendRequest.setSendSpreadsheets(true);
    assertEquals(true, sendRequest.isSendSpreadsheets());
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
   * Tests the method {@link SendRequest#isSpreadsheetsSuccess()}. The value
   * should be set to false which is its default value when it is initialed in
   * setup method.
   */
  public void testIsSpreadsheetsSuccess() {
    assertEquals(false, sendRequest.isSpreadsheetsSuccess());
    sendRequest.setSpreadsheetsSuccess(true);
    assertEquals(true, sendRequest.isSpreadsheetsSuccess());
  }

  /**
   * Tests SendRequest.CREATOR.createFromParcel when all values are true.
   */
  public void testCreateFromParcel_true() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(2);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeString(DRIVE_SHARE_EMAILS);
    parcel.writeByte((byte) 1);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeString(SHARE_URL);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(2, sendRequest.getTrackId());
    assertTrue(sendRequest.isSendDrive());
    assertTrue(sendRequest.isSendMaps());
    assertTrue(sendRequest.isSendFusionTables());
    assertTrue(sendRequest.isSendSpreadsheets());
    assertTrue(sendRequest.isDriveSync());
    assertTrue(sendRequest.isDriveSyncConfirm());
    assertTrue(sendRequest.isDriveShare());
    assertEquals(DRIVE_SHARE_EMAILS, sendRequest.getDriveShareEmails());
    assertTrue(sendRequest.isDriveSharePublic());
    assertEquals(account, sendRequest.getAccount());
    assertTrue(sendRequest.isDriveSuccess());
    assertTrue(sendRequest.isMapsSuccess());
    assertTrue(sendRequest.isFusionTablesSuccess());
    assertTrue(sendRequest.isSpreadsheetsSuccess());
    assertEquals(SHARE_URL, sendRequest.getShareUrl());
  }

  /**
   * Tests SendRequest.CREATOR.createFromParcel when all values are false.
   */
  public void testCreateFromParcel_false() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(4);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeString(null);
    parcel.writeByte((byte) 0);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeString(null);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(4, sendRequest.getTrackId());
    assertFalse(sendRequest.isSendDrive());
    assertFalse(sendRequest.isSendMaps());
    assertFalse(sendRequest.isSendFusionTables());
    assertFalse(sendRequest.isSendSpreadsheets());
    assertFalse(sendRequest.isDriveSync());
    assertFalse(sendRequest.isDriveSyncConfirm());
    assertFalse(sendRequest.isDriveShare());
    assertNull(sendRequest.getDriveShareEmails());
    assertFalse(sendRequest.isDriveSharePublic());
    assertEquals(account, sendRequest.getAccount());
    assertFalse(sendRequest.isDriveSuccess());
    assertFalse(sendRequest.isMapsSuccess());
    assertFalse(sendRequest.isFusionTablesSuccess());
    assertFalse(sendRequest.isSpreadsheetsSuccess());
    assertNull(sendRequest.getShareUrl());
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
    boolean sendDrive = parcel.readByte() == 1;
    boolean sendMaps = parcel.readByte() == 1;
    boolean sendFusionTables = parcel.readByte() == 1;
    boolean sendSpreadsheets = parcel.readByte() == 1;
    boolean driveSync = parcel.readByte() == 1;
    boolean driveSyncConfirm = parcel.readByte() == 1;
    boolean driveShare = parcel.readByte() == 1;
    String dirveShareEmails = parcel.readString();
    boolean driveSharePublic = parcel.readByte() == 1;
    Parcelable account = parcel.readParcelable(null);
    boolean driveSuccess = parcel.readByte() == 1;
    boolean mapsSuccess = parcel.readByte() == 1;
    boolean fusionTablesSuccess = parcel.readByte() == 1;
    boolean spreadsheetsSuccess = parcel.readByte() == 1;
    String shareUrl = parcel.readString();
    assertEquals(1, trackId);
    assertFalse(sendDrive);
    assertFalse(sendMaps);
    assertFalse(sendFusionTables);
    assertFalse(sendSpreadsheets);
    assertFalse(driveSync);
    assertFalse(driveSyncConfirm);
    assertFalse(driveShare);
    assertNull(dirveShareEmails);
    assertFalse(driveSharePublic);
    assertNull(account);
    assertFalse(driveSuccess);
    assertFalse(mapsSuccess);
    assertFalse(fusionTablesSuccess);
    assertFalse(spreadsheetsSuccess);
    assertNull(shareUrl);
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel, int)}.
   */
  public void testWriteToParcel() {
    sendRequest = new SendRequest(4);
    sendRequest.setSendDrive(true);
    sendRequest.setSendMaps(true);
    sendRequest.setSendFusionTables(true);
    sendRequest.setSendSpreadsheets(true);
    sendRequest.setDriveSync(true);
    sendRequest.setDriveSyncConfirm(true);
    sendRequest.setDriveShare(true);
    sendRequest.setDriveShareEmails(DRIVE_SHARE_EMAILS);
    sendRequest.setDriveSharePublic(true);
    Account accountNew = new Account(ACCOUNTNAME + "2", ACCOUNTYPE + "2");
    sendRequest.setAccount(accountNew);
    sendRequest.setMapsSuccess(true);
    sendRequest.setDriveSuccess(true);
    sendRequest.setFusionTablesSuccess(true);
    sendRequest.setSpreadsheetsSuccess(true);
    sendRequest.setShareUrl(SHARE_URL);
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    sendRequest.writeToParcel(parcel, 1);
    parcel.setDataPosition(0);
    long trackId = parcel.readLong();
    boolean sendDrive = parcel.readByte() == 1;
    boolean sendMaps = parcel.readByte() == 1;
    boolean sendFusionTables = parcel.readByte() == 1;
    boolean sendSpreadsheets = parcel.readByte() == 1;
    boolean driveSync = parcel.readByte() == 1;
    boolean driveSyncConfirm = parcel.readByte() == 1;
    boolean driveShare = parcel.readByte() == 1;
    String driveShareEmails = parcel.readString();
    boolean driveSharePublic = parcel.readByte() == 1;
    Parcelable account = parcel.readParcelable(null);
    boolean driveSuccess = parcel.readByte() == 1;
    boolean mapsSuccess = parcel.readByte() == 1;
    boolean fusionTablesSuccess = parcel.readByte() == 1;
    boolean spreadsheetsSuccess = parcel.readByte() == 1;
    String shareUrl = parcel.readString();
    assertEquals(4, trackId);
    assertTrue(sendDrive);
    assertTrue(sendMaps);
    assertTrue(sendFusionTables);
    assertTrue(sendSpreadsheets);
    assertTrue(driveSync);
    assertTrue(driveSyncConfirm);
    assertTrue(driveShare);
    assertEquals(DRIVE_SHARE_EMAILS, driveShareEmails);
    assertTrue(driveSharePublic);
    assertEquals(accountNew, account);
    assertTrue(driveSuccess);
    assertTrue(mapsSuccess);
    assertTrue(fusionTablesSuccess);
    assertTrue(spreadsheetsSuccess);
    assertEquals(SHARE_URL, shareUrl);
  }
}
