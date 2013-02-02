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
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import java.io.IOException;

/**
 * AsyncTask to send a track to Google Drive.
 * 
 * @author Jimmy Shih
 */
public class SendDriveAsyncTask extends AbstractSendAsyncTask {

  private static final String TAG = SendDriveAsyncTask.class.getSimpleName();

  private final long trackId;
  private final Account account;
  private final Context context;
  private final String[] acl;
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
    GoogleAccountCredential credential = SyncUtils.getCredential(context, account.name);
    if (credential == null) {
      return false;
    }

    Drive drive = SyncUtils.getDriveService(credential);
    String folderId = SyncUtils.getMyTracksFolder(context, drive);
    if (folderId == null) {
      return false;
    }

    try {
      Track track = myTracksProviderUtils.getTrack(trackId);
      String driveId = track.getDriveId();
      if (driveId != null && !driveId.equals("")) {
        File driveFile = drive.files().get(driveId).execute();
        if (SyncUtils.isValid(driveFile, folderId)
            && SyncUtils.updateDriveFile(context, myTracksProviderUtils, drive, driveFile, track)) {
          addPermission(drive, driveId);
          return true;
        }
        track.setDriveId("");
        track.setModifiedTime(-1L);
        myTracksProviderUtils.updateTrack(track);
      }

      String id = SyncUtils.addDriveFile(context, myTracksProviderUtils, drive, folderId, track);
      if (id == null) {
        return false;
      }
      addPermission(drive, id);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
      return false;
    }
  }

  @Override
  protected void invalidateToken() {}

  /**
   * Adds permision.
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
