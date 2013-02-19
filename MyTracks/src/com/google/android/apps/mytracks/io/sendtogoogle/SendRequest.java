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

/**
 * Send request states for sending a track to Google Drive, Google Maps, Google
 * Fusion Tables, and Google Spreadsheets.
 * 
 * @author Jimmy Shih
 */
public class SendRequest implements Parcelable {

  public static final String SEND_REQUEST_KEY = "sendRequest";

  private long trackId = -1L;
  private boolean sendDrive = false;
  private boolean sendMaps = false;
  private boolean sendFusionTables = false;
  private boolean sendSpreadsheets = false;

  private boolean driveShare = false;
  private String driveShareEmails = null;

  private boolean mapsShare = false;
  private String mapsSharePackageName = null;
  private String mapsShareClassName = null;
  private boolean mapsExistingMap = false;
  private String mapsExistingMapId = null;

  private Account account = null;

  private boolean driveSuccess = false;
  private boolean mapsSuccess = false;
  private boolean spreadsheetsSuccess = false;
  private boolean fusionTablesSuccess = false;

  /**
   * Creates a new send request.
   * 
   * @param trackId the track id
   */
  public SendRequest(long trackId) {
    this.trackId = trackId;
  }

  public long getTrackId() {
    return trackId;
  }

  public boolean isSendDrive() {
    return sendDrive;
  }

  public void setSendDrive(boolean sendDrive) {
    this.sendDrive = sendDrive;
  }

  public boolean isSendMaps() {
    return sendMaps;
  }

  public void setSendMaps(boolean sendMaps) {
    this.sendMaps = sendMaps;
  }

  public boolean isSendFusionTables() {
    return sendFusionTables;
  }

  public void setSendFusionTables(boolean sendFusionTables) {
    this.sendFusionTables = sendFusionTables;
  }

  public boolean isSendSpreadsheets() {
    return sendSpreadsheets;
  }

  public void setSendSpreadsheets(boolean sendSpreadsheets) {
    this.sendSpreadsheets = sendSpreadsheets;
  }

  public boolean isDriveShare() {
    return driveShare;
  }

  public void setDriveShare(boolean driveShare) {
    this.driveShare = driveShare;
  }

  public String getDriveShareEmails() {
    return driveShareEmails;
  }

  public void setDriveShareEmails(String driveShareEmails) {
    this.driveShareEmails = driveShareEmails;
  }

  public boolean isMapsShare() {
    return mapsShare;
  }

  public void setMapsShare(boolean mapsShare) {
    this.mapsShare = mapsShare;
  }

  public String getMapsSharePackageName() {
    return mapsSharePackageName;
  }

  public void setMapsSharePackageName(String mapsSharePackageName) {
    this.mapsSharePackageName = mapsSharePackageName;
  }

  public String getMapsShareClassName() {
    return mapsShareClassName;
  }

  public void setMapsShareClassName(String mapsShareClassName) {
    this.mapsShareClassName = mapsShareClassName;
  }

  public boolean isMapsExistingMap() {
    return mapsExistingMap;
  }

  public void setMapsExistingMap(boolean mapsExistingMap) {
    this.mapsExistingMap = mapsExistingMap;
  }

  public String getMapsExistingMapId() {
    return mapsExistingMapId;
  }

  public void setMapsExistingMapId(String mapsExistingMapId) {
    this.mapsExistingMapId = mapsExistingMapId;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public boolean isDriveSuccess() {
    return driveSuccess;
  }

  public void setDriveSuccess(boolean driveSuccess) {
    this.driveSuccess = driveSuccess;
  }

  public boolean isMapsSuccess() {
    return mapsSuccess;
  }

  public void setMapsSuccess(boolean mapsSuccess) {
    this.mapsSuccess = mapsSuccess;
  }

  public boolean isFusionTablesSuccess() {
    return fusionTablesSuccess;
  }

  public void setFusionTablesSuccess(boolean fusionTablesSuccess) {
    this.fusionTablesSuccess = fusionTablesSuccess;
  }

  public boolean isSpreadsheetsSuccess() {
    return spreadsheetsSuccess;
  }

  public void setSpreadsheetsSuccess(boolean spreadsheetsSuccess) {
    this.spreadsheetsSuccess = spreadsheetsSuccess;
  }

  private SendRequest(Parcel in) {
    trackId = in.readLong();
    sendDrive = in.readByte() == 1;
    sendMaps = in.readByte() == 1;
    sendFusionTables = in.readByte() == 1;
    sendSpreadsheets = in.readByte() == 1;
    driveShare = in.readByte() == 1;
    driveShareEmails = in.readString();
    mapsShare = in.readByte() == 1;
    mapsSharePackageName = in.readString();
    mapsShareClassName = in.readString();
    mapsExistingMap = in.readByte() == 1;
    mapsExistingMapId = in.readString();
    account = in.readParcelable(null);
    driveSuccess = in.readByte() == 1;
    mapsSuccess = in.readByte() == 1;
    fusionTablesSuccess = in.readByte() == 1;
    spreadsheetsSuccess = in.readByte() == 1;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(trackId);
    out.writeByte((byte) (sendDrive ? 1 : 0));
    out.writeByte((byte) (sendMaps ? 1 : 0));
    out.writeByte((byte) (sendFusionTables ? 1 : 0));
    out.writeByte((byte) (sendSpreadsheets ? 1 : 0));
    out.writeByte((byte) (driveShare ? 1 : 0));
    out.writeString(driveShareEmails);
    out.writeByte((byte) (mapsShare ? 1 : 0));
    out.writeString(mapsSharePackageName);
    out.writeString(mapsShareClassName);
    out.writeByte((byte) (mapsExistingMap ? 1 : 0));
    out.writeString(mapsExistingMapId);
    out.writeParcelable(account, 0);
    out.writeByte((byte) (driveSuccess ? 1 : 0));
    out.writeByte((byte) (mapsSuccess ? 1 : 0));
    out.writeByte((byte) (fusionTablesSuccess ? 1 : 0));
    out.writeByte((byte) (spreadsheetsSuccess ? 1 : 0));
  }

  public static final Parcelable.Creator<SendRequest>
      CREATOR = new Parcelable.Creator<SendRequest>() {
        public SendRequest createFromParcel(Parcel in) {
          return new SendRequest(in);
        }

        public SendRequest[] newArray(int size) {
          return new SendRequest[size];
        }
      };
}
