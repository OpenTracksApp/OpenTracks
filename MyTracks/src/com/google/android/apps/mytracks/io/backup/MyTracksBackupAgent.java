/*
 * Copyright 2010 Google Inc.
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

import com.google.android.apps.mytracks.Constants;

import android.annotation.TargetApi;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

/**
 * Backup agent used to backup and restore all preferences.
 * We use a regular {@link BackupAgent} instead of the convenient helpers in
 * order to be future-proof (assuming we'll want to back up tracks later).
 * 
 * @author Rodrigo Damazio
 */
@TargetApi(8)
public class MyTracksBackupAgent extends BackupAgent {
  private static final String PREFERENCES_ENTITY = "prefs";

  @Override
  public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
      ParcelFileDescriptor newState) throws IOException {
    Log.i(Constants.TAG, "Performing backup");
    SharedPreferences preferences = this.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    backupPreferences(data, preferences);
    Log.i(Constants.TAG, "Backup complete");
  }

  private void backupPreferences(BackupDataOutput data,
      SharedPreferences preferences) throws IOException {
    PreferenceBackupHelper preferenceDumper = createPreferenceBackupHelper();
    byte[] dumpedContents = preferenceDumper.exportPreferences(preferences);
    data.writeEntityHeader(PREFERENCES_ENTITY, dumpedContents.length);
    data.writeEntityData(dumpedContents, dumpedContents.length);
  }

  protected PreferenceBackupHelper createPreferenceBackupHelper() {
    return new PreferenceBackupHelper(this);
  }

  @Override
  public void onRestore(BackupDataInput data, int appVersionCode,
      ParcelFileDescriptor newState) throws IOException {
    Log.i(Constants.TAG, "Restoring from backup");
    while (data.readNextHeader()) {
      String key = data.getKey();
      Log.d(Constants.TAG, "Restoring entity " + key);
      if (key.equals(PREFERENCES_ENTITY)) {
        restorePreferences(data);
      } else {
        Log.e(Constants.TAG, "Found unknown backup entity: " + key);
        data.skipEntityData();
      }
    }
    Log.i(Constants.TAG, "Done restoring from backup");
  }

  /**
   * Restores all preferences from the backup.
   * 
   * @param data the backup data to read from
   * @throws IOException if there are any errors while reading
   */
  private void restorePreferences(BackupDataInput data) throws IOException {
    int dataSize = data.getDataSize();
    byte[] dataBuffer = new byte[dataSize];
    int read = data.readEntityData(dataBuffer, 0, dataSize);
    if (read != dataSize) {
      throw new IOException("Failed to read all the preferences data");
    }

    SharedPreferences preferences = this.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    PreferenceBackupHelper importer = createPreferenceBackupHelper();
    importer.importPreferences(dataBuffer, preferences);
  }
}
