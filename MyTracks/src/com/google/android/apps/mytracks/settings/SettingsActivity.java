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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Toast;

/**
 * An activity for accessing settings.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class SettingsActivity extends AbstractSettingsActivity {

  private static final int DIALOG_CONFIRM_RESET_ID = 0;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.preferences);

    customizeTrackColorModePreferences();
    
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
    
    // Hook up action for resetting all settings
    Preference resetPreference = findPreference(getString(R.string.reset_key));
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
    switch (id) {
      case DIALOG_CONFIRM_RESET_ID:
        return DialogUtils.createConfirmationDialog(
            this, R.string.settings_reset_confirm_message, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int button) {
                onResetPreferencesConfirmed();
              }
            });
      default:
        return null;
    }
  }

  private void customizeTrackColorModePreferences() {
    ListPreference trackColorModePreference =
        (ListPreference) findPreference(getString(R.string.track_color_mode_key));
    trackColorModePreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference,
              Object newValue) {
            updateTrackColorModeSettings((String) newValue);
            return true;
          }
        });
    updateTrackColorModeSettings(trackColorModePreference.getValue());
    
    setTrackColorModePreferenceListeners();
    
    PreferenceCategory speedOptionsCategory = (PreferenceCategory) findPreference(
        getString(R.string.track_color_mode_fixed_speed_options_key));

    speedOptionsCategory.removePreference(
        findPreference(getString(R.string.track_color_mode_fixed_speed_slow_key)));
    speedOptionsCategory.removePreference(
        findPreference(getString(R.string.track_color_mode_fixed_speed_medium_key)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Preference resetPreference = findPreference(getString(R.string.reset_key));
    boolean recording = PreferencesUtils.getLong(this, R.string.recording_track_id_key) != -1;
    resetPreference.setEnabled(!recording);
    resetPreference.setSummary(
        recording ? R.string.settings_not_while_recording
                  : R.string.settings_reset_summary);
  }

  private void updateTrackColorModeSettings(String trackColorMode) {
    boolean usesFixedSpeed =
        trackColorMode.equals(getString(R.string.display_track_color_value_fixed));
    boolean usesDynamicSpeed =
        trackColorMode.equals(getString(R.string.display_track_color_value_dynamic));

    findPreference(getString(R.string.track_color_mode_fixed_speed_slow_display_key))
        .setEnabled(usesFixedSpeed);
    findPreference(getString(R.string.track_color_mode_fixed_speed_medium_display_key))
        .setEnabled(usesFixedSpeed);
    findPreference(getString(R.string.track_color_mode_dynamic_speed_variation_key))
        .setEnabled(usesDynamicSpeed);
  }

  /** Callback for when user confirms resetting all settings. */
  private void onResetPreferencesConfirmed() {
    // Change preferences in a separate thread.
    new Thread() {
      @Override
      public void run() {
        Log.i(TAG, "Resetting all settings");

        SharedPreferences sharedPreferences = getSharedPreferences(
            Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
        // Actually wipe preferences (and save synchronously).
        sharedPreferences.edit().clear().commit();

        // Give UI feedback in the UI thread.
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // Give feedback to the user.
            Toast.makeText(
                SettingsActivity.this,
                R.string.settings_reset_done,
                Toast.LENGTH_SHORT).show();

            // Restart the settings activity so all changes are loaded.
            Intent intent = getIntent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
          }
        });
      }
    }.start();
  }
  
  /** 
   * Set the given edit text preference text.
   * If the units are not metric convert the value before displaying.  
   */
  private void viewTrackColorModeSettings(EditTextPreference preference, int id) {
    if (PreferencesUtils.getBoolean(this, R.string.metric_units_key, true)) {
      return;
    }
    // Convert miles/h to km/h
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    String metricspeed = prefs.getString(getString(id), null);
    int englishspeed;
    try {
      englishspeed = (int) (Double.parseDouble(metricspeed) * UnitConversions.KM_TO_MI);
    } catch (NumberFormatException e) {
      englishspeed = 0;
    }
    preference.getEditText().setText(String.valueOf(englishspeed));
  }
  
  /** 
   * Saves the given edit text preference value.
   * If the units are not metric convert the value before saving.  
   */
  private void validateTrackColorModeSettings(String newValue, int id) {
    String metricspeed;
    if (PreferencesUtils.getBoolean(this, R.string.metric_units_key, true)) {
      metricspeed = newValue;
    } else {
      // Convert miles/h to km/h
      try {
        metricspeed = String.valueOf(
            (int) (Double.parseDouble(newValue) * UnitConversions.MI_TO_KM));
      } catch (NumberFormatException e) {
        metricspeed = "0";
      }
    }
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    Editor editor = prefs.edit();
    editor.putString(getString(id), metricspeed);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }
  
  /** 
   * Sets the TrackColorMode preference listeners.
   */
  private void setTrackColorModePreferenceListeners() {
    setTrackColorModePreferenceListener(R.string.track_color_mode_fixed_speed_slow_display_key,
        R.string.track_color_mode_fixed_speed_slow_key);
    setTrackColorModePreferenceListener(R.string.track_color_mode_fixed_speed_medium_display_key,
        R.string.track_color_mode_fixed_speed_medium_key);
  }
  
  /** 
   * Sets a TrackColorMode preference listener.
   */
  private void setTrackColorModePreferenceListener(int displayKey, final int metricKey) {
    EditTextPreference trackColorModePreference =
        (EditTextPreference) findPreference(getString(displayKey));
    trackColorModePreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference,
              Object newValue) {
            validateTrackColorModeSettings((String) newValue, metricKey);
            return true;
          }
        });
    trackColorModePreference.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            viewTrackColorModeSettings((EditTextPreference) preference, metricKey);
            return true;
          }
        });
  }
}
