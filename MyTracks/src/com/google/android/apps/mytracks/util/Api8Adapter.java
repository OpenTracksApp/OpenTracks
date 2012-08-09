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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.io.backup.Api8BackupPreferencesListener;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.tasks.Api8AnnouncementPeriodicTask;
import com.google.android.apps.mytracks.services.tasks.PeriodicTask;

import android.content.Context;

/**
 * API level 8 specific implementation of the {@link ApiAdapter}.
 *
 * @author Jimmy Shih
 */
public class Api8Adapter extends Api7Adapter {
  
  @Override
  public PeriodicTask getAnnouncementPeriodicTask(Context context) {
    return new Api8AnnouncementPeriodicTask(context);
  }
  
  @Override
  public BackupPreferencesListener getBackupPreferencesListener(Context context) {
    return new Api8BackupPreferencesListener(context);
  }
}
