/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.io.drive;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.sendtogoogle.AbstractSendAsyncTask;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import android.accounts.Account;
import android.content.Context;

import java.io.IOException;

/**
 * AsyncTask to send a track to Google Drive.
 * 
 * @author Jimmy Shih
 */
public class SendDriveAsyncTask extends AbstractSendAsyncTask {

  private final long trackId;
  private final Account account;
  private final String[] acl;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;

  public SendDriveAsyncTask(SendDriveActivity activity, long trackId, Account account, String acl) {
    super(activity);
    this.trackId = trackId;
    this.account = account;
    if (acl != null) {
      this.acl = acl.split(",");
    } else {
      this.acl = null;
    }

    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  protected void closeConnection() {}

  @Override
  protected void saveResult() {}

  @Override
  protected boolean performTask() {
    try {
      GoogleAccountCredential credential = SendToGoogleUtils.getGoogleAccountCredential(
          context, account.name, SendToGoogleUtils.DRIVE_SCOPE);
      if (credential == null) {
        return false;
      }
      Drive drive = SyncUtils.getDriveService(credential);
      File folder = SyncUtils.getMyTracksFolder(context, drive);
      if (folder == null) {
        return false;
      }
      String folderId = folder.getId();
      if (folderId == null) {
        return false;
      }

      Track track = myTracksProviderUtils.getTrack(trackId);
      String driveId = track.getDriveId();
      if (driveId != null && !driveId.equals("")) {
        File driveFile = drive.files().get(driveId).execute();
        if (SyncUtils.isValid(driveFile, folderId) && SyncUtils.updateDriveFile(
            drive, driveFile, context, myTracksProviderUtils, track, false)) {
          addPermission(drive, driveId);
          return true;
        }
        SyncUtils.updateTrackWithDriveFileInfo(myTracksProviderUtils, track, null);
      }

      String id = SyncUtils.insertDriveFile(
          drive, folderId, context, myTracksProviderUtils, track, false);
      if (id == null) {
        return false;
      }
      addPermission(drive, id);
      return true;
    } catch (UserRecoverableAuthException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
      return false;
    } catch (GoogleAuthException e) {
      return retryTask();
    } catch (UserRecoverableAuthIOException e) {
      SendToGoogleUtils.sendNotification(
          context, account.name, e.getIntent(), SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
      return false;
    } catch (IOException e) {
      return retryTask();
    }
  }

  @Override
  protected void invalidateToken() {}

  /**
   * Adds permission.
   * 
   * @param drive the drive
   * @param driveId the drive id
   */
  private void addPermission(Drive drive, String driveId) throws IOException {
    if (acl != null) {
      for (String email : acl) {
        email = email.trim();
        if (!email.equals("")) {
          Permission permission = new Permission();
          permission.setValue(email);
          permission.setType("user");
          permission.setRole("reader");
          drive.permissions().insert(driveId, permission).execute();
        }
      }
    }
  }
}
