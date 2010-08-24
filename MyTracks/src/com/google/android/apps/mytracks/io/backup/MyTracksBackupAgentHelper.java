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

import com.google.android.apps.mytracks.MyTracksSettings;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Backup agent helper for mytracks.
 *
 * TODO: If/when possible, back up the database as well
 *       (see past revisions in rdamazio-mytracks-staging1 for the code)
 *
 * @author Rodrigo Damazio
 */
public class MyTracksBackupAgentHelper extends BackupAgentHelper {
  @Override
  public void onCreate() {
    super.onCreate();

    addHelper("prefs",
        new SharedPreferencesBackupHelper(
            this, MyTracksSettings.SETTINGS_NAME));
  }
}
