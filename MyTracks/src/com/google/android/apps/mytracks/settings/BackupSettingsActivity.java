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
import com.google.android.apps.mytracks.io.backup.BackupActivity;
import com.google.android.apps.mytracks.io.backup.RestoreChooserActivity;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

/**
 * An activity for accessing the backup settings.
 * 
 * @author Jimmy Shih
 */
public class BackupSettingsActivity extends AbstractSettingsActivity {

  private static final int DIALOG_CONFIRM_RESTORE_NOW_ID = 0;

  Preference backupNowPreference;
  Preference restoreNowPreference;

  /*
   * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
   * Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          // Note that key can be null
          if (PreferencesUtils.getKey(BackupSettingsActivity.this, R.string.recording_track_id_key)
              .equals(key)) {
            updateUi();
          }
        }
      };

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    SharedPreferences sharedPreferences = getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    addPreferencesFromResource(R.xml.backup_settings);
    backupNowPreference = findPreference(getString(R.string.backup_to_sd_key));
    backupNowPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(BackupSettingsActivity.this, BackupActivity.class);
        startActivity(intent);
        return true;
      }
    });
    restoreNowPreference = findPreference(getString(R.string.restore_from_sd_key));
    restoreNowPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        showDialog(DIALOG_CONFIRM_RESTORE_NOW_ID);
        return true;
      }
    });
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CONFIRM_RESTORE_NOW_ID) {
      return null;
    }
    return DialogUtils.createConfirmationDialog(this,
        R.string.settings_backup_restore_confirm_message, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent intent = IntentUtils.newIntent(
                BackupSettingsActivity.this, RestoreChooserActivity.class);
            startActivity(intent);
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateUi();
  }

  /**
   * Updates the UI based on the recording state.
   */
  private void updateUi() {
    boolean isRecording = PreferencesUtils.getLong(this, R.string.recording_track_id_key)
        != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    backupNowPreference.setEnabled(!isRecording);
    restoreNowPreference.setEnabled(!isRecording);
    backupNowPreference.setSummary(isRecording ? R.string.settings_not_while_recording
        : R.string.settings_backup_now_summary);
    restoreNowPreference.setSummary(isRecording ? R.string.settings_not_while_recording
        : R.string.settings_backup_restore_summary);
  }
}
