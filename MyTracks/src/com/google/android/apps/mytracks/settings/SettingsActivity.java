/*
 * Copyright 2008 Google Inc.
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
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity for accessing settings.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class SettingsActivity extends AbstractSettingsActivity {

  private static final String TAG = SettingsActivity.class.getSimpleName();
  private static final int DIALOG_CONFIRM_RESET_ID = 0;

  private ListPreference googleAccountListPreference;
  private Preference resetPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.settings);

    Preference mapPreference = findPreference(getString(R.string.settings_map_key));
    mapPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, MapSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference chartPreference = findPreference(getString(R.string.settings_chart_key));
    chartPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, ChartSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference statsPreference = findPreference(getString(R.string.settings_stats_key));
    statsPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, StatsSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference recordingPreference = findPreference(getString(R.string.settings_recording_key));
    recordingPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(
            SettingsActivity.this, RecordingSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference sharingPreference = findPreference(getString(R.string.settings_sharing_key));
    sharingPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, SharingSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference sensorPreference = findPreference(getString(R.string.settings_sensor_key));
    sensorPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, SensorSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    Preference backupPreference = findPreference(getString(R.string.settings_backup_key));
    backupPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = IntentUtils.newIntent(SettingsActivity.this, BackupSettingsActivity.class);
        startActivity(intent);
        return true;
      }
    });

    googleAccountListPreference = (ListPreference) findPreference(getString(R.string.google_account_key));
    List<String> entries = new ArrayList<String>();
    Account[] accounts = AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE);
    for (Account account : accounts) {
      entries.add(account.name);
    }
    googleAccountListPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
    googleAccountListPreference.setEntryValues(entries.toArray(
        new CharSequence[entries.size()]));
    if (entries.size() == 1) {
      googleAccountListPreference.setValueIndex(0);
    }
    String googleAccount = PreferencesUtils.getString(this, R.string.google_account_key,
        PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    updateSwitchAccountSummary(googleAccount);
    googleAccountListPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateSwitchAccountSummary((String) newValue);
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
  protected void onResume() {
    super.onResume();
    boolean isRecording = PreferencesUtils.getLong(this, R.string.recording_track_id_key)
        != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    resetPreference.setEnabled(!isRecording);
    resetPreference.setSummary(isRecording ? R.string.settings_not_while_recording
        : R.string.settings_reset_summary);
  }

  /**
   * Updates the switch account summary.
   * 
   * @param value the value
   */
  private void updateSwitchAccountSummary(String value) {
    googleAccountListPreference.setSummary(
        PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT.equals(value) ? getString(R.string.value_unknown)
            : value);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CONFIRM_RESET_ID) {
      return null;
    }
    return DialogUtils.createConfirmationDialog(
        this, R.string.settings_reset_confirm_message, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int button) {
            onResetPreferencesConfirmed();
          }
        });
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
        SharedPreferences sharedPreferences = getSharedPreferences(
            Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
        // Actually wipe preferences and save synchronously
        sharedPreferences.edit().clear().commit();

        // Give UI feedback in the UI thread
        runOnUiThread(new Runnable() {
            @Override
          public void run() {
            Toast.makeText(SettingsActivity.this, R.string.settings_reset_done, Toast.LENGTH_SHORT)
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
