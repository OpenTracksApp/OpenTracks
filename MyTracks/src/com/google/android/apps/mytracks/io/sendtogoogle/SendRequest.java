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

  private boolean driveSync = false; // to enable Drive sync 
  private boolean driveSyncConfirm = false;
  private boolean driveShare = false; // to enable Drive share
  private String driveShareEmails = null; // for driveShare, emails to share
  private boolean driveSharePublic = false; // for driveShare, share as public

  private Account account = null;

  private boolean driveSuccess = false;
  private boolean mapsSuccess = false;
  private boolean spreadsheetsSuccess = false;
  private boolean fusionTablesSuccess = false;
  private String shareUrl = null;

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

  public boolean isDriveSync() {
    return driveSync;
  }

  public void setDriveSync(boolean driveSync) {
    this.driveSync = driveSync;
  }

  public boolean isDriveSyncConfirm() {
    return driveSyncConfirm;
  }

  public void setDriveSyncConfirm(boolean driveSyncConfirm) {
    this.driveSyncConfirm = driveSyncConfirm;
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

  public boolean isDriveSharePublic() {
    return driveSharePublic;
  }

  public void setDriveSharePublic(boolean driveSharePublic) {
    this.driveSharePublic = driveSharePublic;
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

  public String getShareUrl() {
    return shareUrl;
  }

  public void setShareUrl(String shareUrl) {
    this.shareUrl = shareUrl;
  }

  private SendRequest(Parcel in) {
    trackId = in.readLong();
    sendDrive = in.readByte() == 1;
    sendMaps = in.readByte() == 1;
    sendFusionTables = in.readByte() == 1;
    sendSpreadsheets = in.readByte() == 1;
    driveSync = in.readByte() == 1;
    driveSyncConfirm = in.readByte() == 1;
    driveShare = in.readByte() == 1;
    driveShareEmails = in.readString();
    driveSharePublic = in.readByte() == 1;
    account = in.readParcelable(null);
    driveSuccess = in.readByte() == 1;
    mapsSuccess = in.readByte() == 1;
    fusionTablesSuccess = in.readByte() == 1;
    spreadsheetsSuccess = in.readByte() == 1;
    shareUrl = in.readString();
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
    out.writeByte((byte) (driveSync ? 1 : 0));
    out.writeByte((byte) (driveSyncConfirm ? 1 : 0));
    out.writeByte((byte) (driveShare ? 1 : 0));
    out.writeString(driveShareEmails);
    out.writeByte((byte) (driveSharePublic ? 1 : 0));
    out.writeParcelable(account, 0);
    out.writeByte((byte) (driveSuccess ? 1 : 0));
    out.writeByte((byte) (mapsSuccess ? 1 : 0));
    out.writeByte((byte) (fusionTablesSuccess ? 1 : 0));
    out.writeByte((byte) (spreadsheetsSuccess ? 1 : 0));
    out.writeString(shareUrl);
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
