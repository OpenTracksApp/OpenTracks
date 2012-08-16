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

package com.google.android.apps.mytracks.settings;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;

/**
 * An abstract activity for all the settings activities.
 * 
 * @author Jimmy Shih
 */
public class AbstractSettingsActivity extends PreferenceActivity {

  private BackupPreferencesListener backupPreferencesListener;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);

    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(Constants.SETTINGS_NAME);
    preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);

    // Set up automatic preferences backup
    backupPreferencesListener = ApiAdapterFactory.getApiAdapter()
        .getBackupPreferencesListener(this);
    preferenceManager.getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(backupPreferencesListener);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() != android.R.id.home) {
      return super.onOptionsItemSelected(item);
    }
    finish();
    return true;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void onDestroy() {
    super.onDestroy();
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(backupPreferencesListener);
  }
}
