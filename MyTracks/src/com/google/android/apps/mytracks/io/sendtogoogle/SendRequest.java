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
 * Send request states for sending a track to Google Maps, Google Fusion Tables,
 * and Google Docs.
 *
 * @author Jimmy Shih
 */
public class SendRequest implements Parcelable {

  public static final String SEND_REQUEST_KEY = "sendRequest";

  private long trackId = -1L;
  private String sharingAppPackageName = null;
  private String sharingAppClassName = null;
  private boolean sendMaps = false;
  private boolean sendFusionTables = false;
  private boolean sendDocs = false;
  private boolean newMap = false;
  private Account account = null;
  private String mapId = null;
  private boolean mapsSuccess = false;
  private boolean docsSuccess = false;
  private boolean fusionTablesSuccess = false;

  /**
   * Creates a new send request.
   *
   * @param trackId the track id
   */
  public SendRequest(long trackId) {
    this.trackId = trackId;
  }

  /**
   * Get the track id.
   */
  public long getTrackId() {
    return trackId;
  }

  /**
   * Gets the sharing app package name.
   */
  public String getSharingAppPackageName() {
    return sharingAppPackageName;
  }

  /**
   * Sets the sharing app package name.
   * 
   * @param sharingAppPackageName the sharing app package name
   */
  public void setSharingAppPackageName(String sharingAppPackageName) {
    this.sharingAppPackageName = sharingAppPackageName;
  }

  /**
   * Gets the sharing app class name.
   */
  public String getSharingAppClassName() {
    return sharingAppClassName;
  }

  /**
   * Sets the sharing app class name.
   * 
   * @param sharingAppClassName the sharing app class name
   */
  public void setSharingAppClassName(String sharingAppClassName) {
    this.sharingAppClassName = sharingAppClassName;
  }

  /**
   * True if the user has selected the send to Google Maps option.
   */
  public boolean isSendMaps() {
    return sendMaps;
  }

  /**
   * Sets the send to Google Maps option.
   *
   * @param sendMaps true if the user has selected the send to Google Maps
   *          option
   */
  public void setSendMaps(boolean sendMaps) {
    this.sendMaps = sendMaps;
  }

  /**
   * True if the user has selected the send to Google Fusion Tables option.
   */
  public boolean isSendFusionTables() {
    return sendFusionTables;
  }

  /**
   * Sets the send to Google Fusion Tables option.
   *
   * @param sendFusionTables true if the user has selected the send to Google
   *          Fusion Tables option
   */
  public void setSendFusionTables(boolean sendFusionTables) {
    this.sendFusionTables = sendFusionTables;
  }

  /**
   * True if the user has selected the send to Google Docs option.
   */
  public boolean isSendDocs() {
    return sendDocs;
  }

  /**
   * Sets the send to Google Docs option.
   *
   * @param sendDocs true if the user has selected the send to Google Docs
   *          option
   */
  public void setSendDocs(boolean sendDocs) {
    this.sendDocs = sendDocs;
  }

  /**
   * True if the user has selected to create a new Google Maps.
   */
  public boolean isNewMap() {
    return newMap;
  }

  /**
   * Sets the new map option.
   *
   * @param newMap true if the user has selected to create a new Google Maps.
   */
  public void setNewMap(boolean newMap) {
    this.newMap = newMap;
  }

  /**
   * Gets the account.
   */
  public Account getAccount() {
    return account;
  }

  /**
   * Sets the account.
   *
   * @param account the account
   */
  public void setAccount(Account account) {
    this.account = account;
  }

  /**
   * Gets the selected map id if the user has selected to send a track to an
   * existing Google Maps.
   */
  public String getMapId() {
    return mapId;
  }

  /**
   * Sets the map id.
   *
   * @param mapId the map id
   */
  public void setMapId(String mapId) {
    this.mapId = mapId;
  }

  /**
   * True if sending to Google Maps is success.
   */
  public boolean isMapsSuccess() {
    return mapsSuccess;
  }

  /**
   * Sets the Google Maps result.
   *
   * @param mapsSuccess true if sending to Google Maps is success
   */
  public void setMapsSuccess(boolean mapsSuccess) {
    this.mapsSuccess = mapsSuccess;
  }

  /**
   * True if sending to Google Fusion Tables is success.
   */
  public boolean isFusionTablesSuccess() {
    return fusionTablesSuccess;
  }

  /**
   * Sets the Google Fusion Tables result.
   *
   * @param fusionTablesSuccess true if sending to Google Fusion Tables is
   *          success
   */
  public void setFusionTablesSuccess(boolean fusionTablesSuccess) {
    this.fusionTablesSuccess = fusionTablesSuccess;
  }

  /**
   * True if sending to Google Docs is success.
   */
  public boolean isDocsSuccess() {
    return docsSuccess;
  }

  /**
   * Sets the Google Docs result.
   *
   * @param docsSuccess true if sending to Google Docs is success
   */
  public void setDocsSuccess(boolean docsSuccess) {
    this.docsSuccess = docsSuccess;
  }

  private SendRequest(Parcel in) {
    trackId = in.readLong();
    sharingAppPackageName = in.readString();
    sharingAppClassName = in.readString();
    sendMaps = in.readByte() == 1;
    sendFusionTables = in.readByte() == 1;
    sendDocs = in.readByte() == 1;
    newMap = in.readByte() == 1;
    account = in.readParcelable(null);
    mapId = in.readString();
    mapsSuccess = in.readByte() == 1;
    fusionTablesSuccess = in.readByte() == 1;
    docsSuccess = in.readByte() == 1;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(trackId);
    out.writeString(sharingAppPackageName);
    out.writeString(sharingAppClassName);
    out.writeByte((byte) (sendMaps ? 1 : 0));
    out.writeByte((byte) (sendFusionTables ? 1 : 0));
    out.writeByte((byte) (sendDocs ? 1 : 0));
    out.writeByte((byte) (newMap ? 1 : 0));
    out.writeParcelable(account, 0);
    out.writeString(mapId);
    out.writeByte((byte) (mapsSuccess ? 1 : 0));
    out.writeByte((byte) (fusionTablesSuccess ? 1 : 0));
    out.writeByte((byte) (docsSuccess ? 1 : 0));
  }

  public static final Parcelable.Creator<SendRequest> CREATOR = new Parcelable.Creator<
      SendRequest>() {
    public SendRequest createFromParcel(Parcel in) {
      return new SendRequest(in);
    }

    public SendRequest[] newArray(int size) {
      return new SendRequest[size];
    }
  };
}
