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
import com.google.api.services.drive.model.PermissionList;

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
  private final boolean isPublic;
  private final Context context;
  private final MyTracksProviderUtils myTracksProviderUtils;

  public SendDriveAsyncTask(
      SendDriveActivity activity, long trackId, Account account, String acl, boolean isPublic) {
    super(activity);
    this.trackId = trackId;
    this.account = account;
    if (acl != null) {
      this.acl = acl.split(",");
    } else {
      this.acl = null;
    }
    this.isPublic = isPublic;
    
    context = activity.getApplicationContext();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
  }

  @Override
  protected void closeConnection() {}

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
          addPermission(drive, driveFile);
          return true;
        }
        SyncUtils.updateTrackWithDriveFileInfo(myTracksProviderUtils, track, null);
      }

      File file = SyncUtils.insertDriveFile(
          drive, folderId, context, myTracksProviderUtils, track, false);
      if (file == null) {
        return false;
      }
      addPermission(drive, file);
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
   * @param file the drive file
   */
  private void addPermission(Drive drive, File file) throws IOException {
    if (isPublic) {
      boolean hasPublic = false;
      PermissionList permissionList = drive.permissions().list(file.getId()).execute();
      for (Permission permission : permissionList.getItems()) {
        String role = permission.getRole();
        if (role.equals("reader") || role.equals("writer")) {
          if (permission.getType().equals("anyone")) {
            hasPublic = true;
            break;
          }
        }
      }
      if (!hasPublic) {
        Permission permission = new Permission();
        permission.setRole("reader");
        permission.setType("anyone");
        permission.setValue("");
        drive.permissions().insert(file.getId(), permission).execute();
      }
      shareUrl = file.getAlternateLink();
    }

    if (acl != null) {
      for (String email : acl) {
        email = email.trim();
        if (!email.equals("")) {
          Permission permission = new Permission();
          permission.setRole("reader");
          permission.setType("user");
          permission.setValue(email);
          drive.permissions().insert(file.getId(), permission).execute();
        }
      }
    }
  }
}
