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

package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * AsyncTask to backup data to the SD card.
 *
 * @author Jimmy Shih
 */
public class BackupAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = BackupAsyncTask.class.getSimpleName();

  private BackupActivity backupActivity;
  private final ExternalFileBackup externalFileBackup;

  // true if the AsyncTask result is success
  private boolean success;

  // true if the AsyncTask has completed
  private boolean completed;

  // message id to return to the activity
  private int messageId;

  /**
   * Creates an AsyncTask.
   *
   * @param backupActivity the activity currently associated with this
   *          AsyncTask
   */
  public BackupAsyncTask(BackupActivity backupActivity) {
    this.backupActivity = backupActivity;
    this.externalFileBackup = new ExternalFileBackup(backupActivity);
    success = false;
    completed = false;
    messageId = R.string.settings_backup_now_error;
  }

  /**
   * Sets the current {@link BackupActivity} associated with this AyncTask.
   *
   * @param activity the current {@link BackupActivity}, can be null
   */
  public void setActivity(BackupActivity activity) {
    this.backupActivity = activity;
    if (completed && backupActivity != null) {
      backupActivity.onAsyncTaskCompleted(success, messageId);
    }
  }

  @Override
  protected void onPreExecute() {
    if (backupActivity != null) {
      backupActivity.showProgressDialog();
    }
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    if (!FileUtils.isExternalStorageWriteable()) {
      messageId = R.string.external_storage_not_writable;
      return false;
    }

    if (!externalFileBackup.isBackupsDirectoryAvailable(true)) {
      messageId = R.string.external_storage_not_writable;
      return false;
    }

    try {
      externalFileBackup.writeToDefaultFile();
      messageId = R.string.settings_backup_now_success;
      return true;
    } catch (IOException e) {
      Log.d(TAG, "IO exception", e);
      return false;
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (backupActivity != null) {
      backupActivity.onAsyncTaskCompleted(success, messageId);
    }
  }
}
