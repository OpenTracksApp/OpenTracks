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
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

/**
 * An activity for advanced settings.
 * 
 * @author Jimmy Shih
 */
public class AdvancedSettingsActivity extends AbstractSettingsActivity {

  private static final String TAG = AdvancedSettingsActivity.class.getSimpleName();

  private static final int DIALOG_CONFIRM_ALLOW_ACCESS_ID = 0;
  private static final int DIALOG_CONFIRM_RESET_ID = 1;

  private CheckBoxPreference allowAccessCheckBoxPreference;
  private Preference resetPreference;

  private SharedPreferences sharedPreferences;
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
              AdvancedSettingsActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                AdvancedSettingsActivity.this, R.string.recording_track_id_key);
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
    addPreferencesFromResource(R.xml.advanced_settings);

    ListPreference preference = (ListPreference) findPreference(getString(R.string.photo_size_key));
    int value = PreferencesUtils.getInt(
        this, R.string.photo_size_key, PreferencesUtils.PHOTO_SIZE_DEFAULT);
    String[] values = getResources().getStringArray(R.array.photo_size_values);
    String[] options = new String[values.length];
    String[] summary = new String[values.length];
    setPhotoSizeSummaryAndOptions(summary, options, values);
    configureListPreference(preference, summary, options, values, String.valueOf(value), null);

    allowAccessCheckBoxPreference = (CheckBoxPreference) findPreference(
        getString(R.string.allow_access_key));
    allowAccessCheckBoxPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        if ((Boolean) newValue) {
          showDialog(DIALOG_CONFIRM_ALLOW_ACCESS_ID);
          return false;
        } else {
          return true;
        }
      }
    });

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

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

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_CONFIRM_ALLOW_ACCESS_ID:
        return DialogUtils.createConfirmationDialog(this,
            R.string.settings_sharing_allow_access_confirm_title,
            getString(R.string.settings_sharing_allow_access_confirm_message),
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface dialog, int button) {
                allowAccessCheckBoxPreference.setChecked(true);
              }
            });
      case DIALOG_CONFIRM_RESET_ID:
        return DialogUtils.createConfirmationDialog(this, R.string.settings_reset_confirm_title,
            getString(R.string.settings_reset_confirm_message),
            new DialogInterface.OnClickListener() {
                @Override
              public void onClick(DialogInterface d, int button) {
                onResetPreferencesConfirmed();
              }
            });
      default:
        return null;
    }
  }

  private void setPhotoSizeSummaryAndOptions(String[] summary, String[] options, String[] values) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (value == -1) {
        options[i] = getString(R.string.settings_advanced_photo_size_original);
        summary[i] = getString(R.string.settings_advanced_photo_size_original);
      } else if (value < 1024) {
        options[i] = getString(R.string.value_integer_kilobyte, value);
        summary[i] = getString(R.string.settings_advanced_photo_size_summary, options[i]);
      } else {
        int megabyte = value / 1024;
        options[i] = getString(R.string.value_integer_megabyte, megabyte);
        summary[i] = getString(R.string.settings_advanced_photo_size_summary, options[i]);
      }
    }
  }

  /**
   * Updates the UI based on the recording state.
   */
  private void updateUi() {
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    resetPreference.setEnabled(!isRecording);
    resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
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

        SyncUtils.disableSync(AdvancedSettingsActivity.this);

        // Give UI feedback in the UI thread
        runOnUiThread(new Runnable() {
            @Override
          public void run() {
            Toast.makeText(
                AdvancedSettingsActivity.this, R.string.settings_reset_done, Toast.LENGTH_SHORT)
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
