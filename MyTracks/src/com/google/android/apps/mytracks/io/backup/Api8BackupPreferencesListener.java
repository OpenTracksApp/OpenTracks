/*
 * Copyright 2011 Google Inc.
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

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Implementation of {@link BackupPreferencesListener} that calls the
 * {@link BackupManager}. <br>
 * For API Level 8 or higher.
 *
 * @author Jimmy Shih
 */
@TargetApi(8)
public class Api8BackupPreferencesListener implements BackupPreferencesListener {

  private final BackupManager backupManager;

  public Api8BackupPreferencesListener(Context context) {
    this.backupManager = new BackupManager(context);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    backupManager.dataChanged();
  }
}
