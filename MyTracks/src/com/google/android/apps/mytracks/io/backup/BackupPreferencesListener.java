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

import com.google.android.apps.mytracks.util.ApiFeatures;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * Shared preferences listener which notifies the backup system about new data
 * being available for backup.
 * This class is API-version-safe and will provide a dummy implementation if
 * the device doesn't support backup services.
 *
 * @author Rodrigo Damazio
 */
public abstract class BackupPreferencesListener
    implements OnSharedPreferenceChangeListener {

  /**
   * Real implementation of the listener, which calls the {@link BackupManager}.
   */
  private static class BackupPreferencesListenerImpl
      extends BackupPreferencesListener {
    private final BackupManager backupManager;

    public BackupPreferencesListenerImpl(Context context) {
      this.backupManager = new BackupManager(context);
    }

    @Override
    public void onSharedPreferenceChanged(
        SharedPreferences sharedPreferences, String key) {
      backupManager.dataChanged();
    }
  }

  /**
   * Dummy implementation of the listener which does nothing.
   */
  private static class DummyBackupPreferencesListener
      extends BackupPreferencesListener {
    @Override
    public void onSharedPreferenceChanged(
        SharedPreferences sharedPreferences, String key) {
      // Do nothing
    }
  }

  /**
   * Creates and returns a proper instance of the listener for this device.
   */
  public static BackupPreferencesListener create(
      Context context, ApiFeatures apiFeatures) {
    if (apiFeatures.hasBackup()) {
      return new BackupPreferencesListenerImpl(context);
    } else {
      return new DummyBackupPreferencesListener();
    }
  }
}