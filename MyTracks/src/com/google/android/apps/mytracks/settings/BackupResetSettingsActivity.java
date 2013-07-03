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
import com.google.android.apps.mytracks.io.sync.SyncUtils;
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
import android.util.Log;
import android.widget.Toast;

/**
 * An activity for accessing the backup and reset settings.
 * 
 * @author Jimmy Shih
 */
public class BackupResetSettingsActivity extends AbstractSettingsActivity {

  private static final String TAG = BackupResetSettingsActivity.class.getSimpleName();
  private static final int DIALOG_CONFIRM_RESTORE_ID = 0;
  private static final int DIALOG_CONFIRM_RESET_ID = 1;

  private SharedPreferences sharedPreferences;
  private Preference backupPreference;
  private Preference restoreNowPreference;
  private Preference resetPreference;

  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

  /*
   * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
   * Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (key != null && key.equals(PreferencesUtils.getKey(
              BackupResetSettingsActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                BackupResetSettingsActivity.this, R.string.recording_track_id_key);
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                updateUi();
              }
            });
          }
        }
      };

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.backup_reset_settings);

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    backupPreference = findPreference(getString(R.string.settings_backup_key));
    backupPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(
            BackupResetSettingsActivity.this, BackupActivity.class);
        startActivity(intent);
        return true;
      }
    });
    restoreNowPreference = findPreference(getString(R.string.settings_restore_key));
    restoreNowPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        showDialog(DIALOG_CONFIRM_RESTORE_ID);
        return true;
      }
    });

    resetPreference = findPreference(getString(R.string.settings_reset_key));
    resetPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference arg0) {
        showDialog(DIALOG_CONFIRM_RESET_ID);
        return true;
      }
    });
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    Dialog dialog;
    switch (id) {
      case DIALOG_CONFIRM_RESTORE_ID:
        dialog = DialogUtils.createConfirmationDialog(this,
            R.string.settings_backup_restore_confirm_message,
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int which) {
                Intent intent = IntentUtils.newIntent(
                    BackupResetSettingsActivity.this, RestoreChooserActivity.class);
                startActivity(intent);
              }
            });
        break;
      case DIALOG_CONFIRM_RESET_ID:
        dialog = DialogUtils.createConfirmationDialog(
            this, R.string.settings_reset_confirm_message, new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                onResetPreferencesConfirmed();
              }
            });
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  @Override
  protected void onResume() {
    super.onResume();
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
    updateUi();
  }

  @Override
  protected void onPause() {
    super.onPause();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  /**
   * Updates the UI based on the recording state.
   */
  private void updateUi() {
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    backupPreference.setEnabled(!isRecording);
    backupPreference.setSummary(
        isRecording ? R.string.settings_not_while_recording : R.string.settings_backup_now_summary);

    restoreNowPreference.setEnabled(!isRecording);
    restoreNowPreference.setSummary(isRecording ? R.string.settings_not_while_recording
        : R.string.settings_backup_restore_summary);

    resetPreference.setEnabled(!isRecording);
    resetPreference.setSummary(
        isRecording ? R.string.settings_not_while_recording : R.string.settings_reset_summary);
  }

  /**
   * Callback when the user confirms resetting all settings.
   */
  private void onResetPreferencesConfirmed() {
    // Change preferences in a separate thread
    new Thread() {
        @Override
      public void run() {
        Log.i(TAG, "Resetting all settings");

        // Actually wipe preferences and save synchronously
        sharedPreferences.edit().clear().commit();

        SyncUtils.disableSync(BackupResetSettingsActivity.this);
        SyncUtils.clearSyncState(BackupResetSettingsActivity.this);
        
        // Give UI feedback in the UI thread
        runOnUiThread(new Runnable() {
            @Override
          public void run() {
            Toast.makeText(
                BackupResetSettingsActivity.this, R.string.settings_reset_done, Toast.LENGTH_SHORT)
                .show();
            // Restart the settings activity so all changes are loaded
            Intent intent = getIntent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
          }
        });
      }
    }.start();
  }
}
