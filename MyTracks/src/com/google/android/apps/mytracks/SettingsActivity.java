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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.io.backup.BackupActivityHelper;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.sensors.ant.AntUtils;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.BluetoothDeviceUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An activity that let's the user see and edit the settings.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class SettingsActivity extends PreferenceActivity {

  // Value when the task frequency is off.
  private static final String TASK_FREQUENCY_OFF = "0";
  
  // Value when the recording interval is 'Adapt battery life'.
  private static final String RECORDING_INTERVAL_ADAPT_BATTERY_LIFE = "-2";
  
  // Value when the recording interval is 'Adapt accuracy'.
  private static final String RECORDING_INTERVAL_ADAPT_ACCURACY = "-1";
  
  // Value for the recommended recording interval.
  private static final String RECORDING_INTERVAL_RECOMMENDED = "0";

  // Value when the auto resume timeout is never.
  private static final String AUTO_RESUME_TIMEOUT_NEVER = "0";
  
  // Value when the auto resume timeout is always.
  private static final String AUTO_RESUME_TIMEOUT_ALWAYS = "-1";
  
  // Value for the recommended recording distance.
  private static final String RECORDING_DISTANCE_RECOMMENDED = "5";

  // Value for the recommended track distance.  
  private static final String TRACK_DISTANCE_RECOMMENDED = "200";

  // Value for the recommended GPS accuracy.
  private static final String GPS_ACCURACY_RECOMMENDED = "200";
  
  // Value when the GPS accuracy is for excellent GPS signal.
  private static final String GPS_ACCURACY_EXCELLENT = "10";
  
  // Value when the GPS accuracy is for poor GPS signal.
  private static final String GPS_ACCURACY_POOR = "5000";

  private BackupPreferencesListener backupListener;
  private SharedPreferences preferences;
  
  /** Called when the activity is first created. */
  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // The volume we want to control is the Text-To-Speech volume
    ApiFeatures apiFeatures = ApiFeatures.getInstance();
    int volumeStream =
        new StatusAnnouncerFactory(apiFeatures).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // Tell it where to read/write preferences
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(Constants.SETTINGS_NAME);
    preferenceManager.setSharedPreferencesMode(0);

    // Set up automatic preferences backup
    backupListener = apiFeatures.getApiAdapter().getBackupPreferencesListener(this);
    preferences = preferenceManager.getSharedPreferences();
    preferences.registerOnSharedPreferenceChangeListener(backupListener);

    // Load the preferences to be displayed
    addPreferencesFromResource(R.xml.preferences);

    // Disable voice announcement if not available
    if (!apiFeatures.hasTextToSpeech()) {
      IntegerListPreference announcementFrequency =
          (IntegerListPreference) findPreference(
              getString(R.string.announcement_frequency_key));
      announcementFrequency.setEnabled(false);
      announcementFrequency.setValue(TASK_FREQUENCY_OFF);
      announcementFrequency.setSummary(R.string.settings_recording_voice_not_available);
    }
    
    setRecordingIntervalOptions();
    setAutoResumeTimeoutOptions();

    // Hook up switching of displayed list entries between metric and imperial
    // units
    CheckBoxPreference metricUnitsPreference =
        (CheckBoxPreference) findPreference(
            getString(R.string.metric_units_key));
    metricUnitsPreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference,
              Object newValue) {
            boolean isMetric = (Boolean) newValue;
            updateDisplayOptions(isMetric);
            return true;
          }
        });
    updateDisplayOptions(metricUnitsPreference.isChecked());

    customizeSensorOptionsPreferences();
    customizeTrackColorModePreferences();
    
    // Hook up action for resetting all settings
    Preference resetPreference = findPreference(getString(R.string.reset_key));
    resetPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference arg0) {
        onResetPreferencesClick();
        return true;
      }
    });
    
    // Add a confirmation dialog for the 'Allow access' preference.
    final CheckBoxPreference allowAccessPreference = (CheckBoxPreference) findPreference(
        getString(R.string.allow_access_key));
    allowAccessPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
          AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
              .setCancelable(true)
              .setTitle(getString(R.string.settings_sharing_allow_access))
              .setMessage(getString(R.string.settings_sharing_allow_access_confirm_message))
              .setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int button) {
                  allowAccessPreference.setChecked(true);
                }
              })
              .setNegativeButton(android.R.string.cancel, null)
              .create();
          dialog.show();
          return false;
        } else {
          return true;
        }
      }
    });
  }
  
  /**
   * Sets the display options for the 'Time between points' option.
   */
  private void setRecordingIntervalOptions() {
    String[] values = getResources().getStringArray(R.array.recording_interval_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(RECORDING_INTERVAL_ADAPT_BATTERY_LIFE)) {
        options[i] = getString(R.string.value_adapt_battery_life);
      } else if (values[i].equals(RECORDING_INTERVAL_ADAPT_ACCURACY)) {
        options[i] = getString(R.string.value_adapt_accuracy);
      } else if (values[i].equals(RECORDING_INTERVAL_RECOMMENDED)) {
        options[i] = getString(R.string.value_smallest_recommended);
      } else {
        int value = Integer.parseInt(values[i]);
        String format;
        if (value < 60) {
          format = getString(R.string.value_integer_second);
        } else {
          value = value / 60;
          format = getString(R.string.value_integer_minute);
        }
        options[i] = String.format(format, value);
      }
    }
    ListPreference list = (ListPreference) findPreference(
        getString(R.string.min_recording_interval_key));
    list.setEntries(options);
  }

  /**
   * Sets the display options for the 'Auto-resume timeout' option.
   */
  private void setAutoResumeTimeoutOptions() {
    String[] values = getResources().getStringArray(R.array.recording_auto_resume_timeout_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(AUTO_RESUME_TIMEOUT_NEVER)) {
        options[i] = getString(R.string.value_never);
      } else if (values[i].equals(AUTO_RESUME_TIMEOUT_ALWAYS)) {
        options[i] = getString(R.string.value_always);
      } else {
        int value = Integer.parseInt(values[i]);
        String format = getString(R.string.value_integer_minute);
        options[i] = String.format(format, value);
      }
    }
    ListPreference list = (ListPreference) findPreference(
        getString(R.string.auto_resume_track_timeout_key));
    list.setEntries(options);
  }

  private void customizeSensorOptionsPreferences() {
    ListPreference sensorTypePreference =
        (ListPreference) findPreference(getString(R.string.sensor_type_key));
    sensorTypePreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference,
              Object newValue) {
            updateSensorSettings((String) newValue);
            return true;
          }
        });
    updateSensorSettings(sensorTypePreference.getValue());

    if (!AntUtils.hasAntSupport(this)) {
      // The sensor options screen has a few ANT-specific options which we
      // need to remove.  First, we need to remove the ANT sensor types.
      // Second, we need to remove the ANT unpairing options.

      Set<Integer> toRemove = new HashSet<Integer>();

      String[] antValues = getResources().getStringArray(R.array.sensor_type_ant_values);
      for (String antValue : antValues) {
        toRemove.add(sensorTypePreference.findIndexOfValue(antValue));
      }

      CharSequence[] entries = sensorTypePreference.getEntries();
      CharSequence[] entryValues = sensorTypePreference.getEntryValues();

      CharSequence[] filteredEntries = new CharSequence[entries.length - toRemove.size()];
      CharSequence[] filteredEntryValues = new CharSequence[filteredEntries.length];
      for (int i = 0, last = 0; i < entries.length; i++) {
        if (!toRemove.contains(i)) {
          filteredEntries[last] = entries[i];
          filteredEntryValues[last++] = entryValues[i];
        }
      }

      sensorTypePreference.setEntries(filteredEntries);
      sensorTypePreference.setEntryValues(filteredEntryValues);

      PreferenceScreen sensorOptionsScreen =
          (PreferenceScreen) findPreference(getString(R.string.sensor_options_key));
      sensorOptionsScreen.removePreference(findPreference(getString(R.string.ant_options_key)));
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
    
    configureBluetoothPreferences();
    Preference backupNowPreference =
        findPreference(getString(R.string.backup_to_sd_key));
    Preference restoreNowPreference =
        findPreference(getString(R.string.restore_from_sd_key));
    Preference resetPreference = findPreference(getString(R.string.reset_key));

    // If recording, disable backup/restore/reset
    // (we don't want to get to inconsistent states)
    boolean recording =
        preferences.getLong(getString(R.string.recording_track_key), -1) != -1;
    backupNowPreference.setEnabled(!recording);
    restoreNowPreference.setEnabled(!recording);
    resetPreference.setEnabled(!recording);
    backupNowPreference.setSummary(
        recording ? R.string.settings_not_while_recording
                  : R.string.settings_backup_now_summary);
    restoreNowPreference.setSummary(
        recording ? R.string.settings_not_while_recording
                  : R.string.settings_backup_restore_summary);
    resetPreference.setSummary(
        recording ? R.string.settings_not_while_recording
                  : R.string.settings_reset_summary);

    // Add actions to the backup preferences
    backupNowPreference.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            BackupActivityHelper backupHelper =
                new BackupActivityHelper(SettingsActivity.this);
            backupHelper.writeBackup();
            return true;
          }
        });
    restoreNowPreference.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            BackupActivityHelper backupHelper =
                new BackupActivityHelper(SettingsActivity.this);
            backupHelper.restoreBackup();
            return true;
          }
        });
  }

  @Override
  protected void onDestroy() {
    getPreferenceManager().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(backupListener);

    super.onPause();
  }

  private void updateSensorSettings(String sensorType) {
    boolean usesBluetooth =
        getString(R.string.sensor_type_value_zephyr).equals(sensorType)
        || getString(R.string.sensor_type_value_polar).equals(sensorType);
    findPreference(
        getString(R.string.bluetooth_sensor_key)).setEnabled(usesBluetooth);
    findPreference(
        getString(R.string.bluetooth_pairing_key)).setEnabled(usesBluetooth);

    // Update the ANT+ sensors.
    // TODO: Only enable on phones that have ANT+.
    Preference antHrm = findPreference(getString(R.string.ant_heart_rate_sensor_id_key));
    Preference antSrm = findPreference(getString(R.string.ant_srm_bridge_sensor_id_key));
    if (antHrm != null && antSrm != null) {
      antHrm
          .setEnabled(getString(R.string.sensor_type_value_ant).equals(sensorType));
      antSrm
          .setEnabled(getString(R.string.sensor_type_value_srm_ant_bridge).equals(sensorType));
    }
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
  
  /**
   * Updates display options that depends on the preferred distance units, metric or imperial.
   *
   * @param isMetric true to use metric units, false to use imperial
   */
  private void updateDisplayOptions(boolean isMetric) {
    setTaskOptions(isMetric, R.string.announcement_frequency_key);
    setTaskOptions(isMetric, R.string.split_frequency_key);
    setRecordingDistanceOptions(isMetric, R.string.min_recording_distance_key);
    setTrackDistanceOptions(isMetric, R.string.max_recording_distance_key);
    setGpsAccuracyOptions(isMetric, R.string.min_required_accuracy_key);
  }

  /**
   * Sets the display options for a periodic task.
   */
  private void setTaskOptions(boolean isMetric, int listId) {
    String[] values = getResources().getStringArray(R.array.recording_task_frequency_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(TASK_FREQUENCY_OFF)) {
        options[i] = getString(R.string.value_off);
      } else if (values[i].startsWith("-")) {
        int value = Integer.parseInt(values[i].substring(1));
        int stringId = isMetric ? R.string.value_integer_kilometer : R.string.value_integer_mile;
        String format = getString(stringId);
        options[i] = String.format(format, value);
      } else {
        int value = Integer.parseInt(values[i]);
        String format = getString(R.string.value_integer_minute);
        options[i] = String.format(format, value);
      }
    }

    ListPreference list = (ListPreference) findPreference(getString(listId));
    list.setEntries(options);
  }
  
  /**
   * Sets the display options for 'Distance between points' option.
   */
  private void setRecordingDistanceOptions(boolean isMetric, int listId) {
    String[] values = getResources().getStringArray(R.array.recording_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (!isMetric) {
        value = (int) (value * UnitConversions.M_TO_FT);
      }
      String format;
      if (values[i].equals(RECORDING_DISTANCE_RECOMMENDED)) {
        int stringId = isMetric ? R.string.value_integer_meter_recommended
            : R.string.value_integer_feet_recommended;
        format = getString(stringId);
      } else {
        int stringId = isMetric ? R.string.value_integer_meter : R.string.value_integer_feet;
        format = getString(stringId);
      }
      options[i] = String.format(format, value);
    }

    ListPreference list = (ListPreference) findPreference(getString(listId));
    list.setEntries(options);
  }
  
  /**
   * Sets the display options for 'Distance between Tracks'.
   */
  private void setTrackDistanceOptions(boolean isMetric, int listId) {
    String[] values = getResources().getStringArray(R.array.recording_track_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      String format;
      if (isMetric) {
        int stringId = values[i].equals(TRACK_DISTANCE_RECOMMENDED) 
            ? R.string.value_integer_meter_recommended : R.string.value_integer_meter;
        format = getString(stringId);
        options[i] = String.format(format, value);
      } else {
        value = (int) (value * UnitConversions.M_TO_FT);
        if (value < 2000) {
          int stringId = values[i].equals(TRACK_DISTANCE_RECOMMENDED) 
              ? R.string.value_integer_feet_recommended : R.string.value_integer_feet;
          format = getString(stringId);
          options[i] = String.format(format, value);
        } else {
          double mile = value / UnitConversions.MI_TO_FEET;
          format = getString(R.string.value_float_mile);
          options[i] = String.format(format, mile);
        }
      }
    }

    ListPreference list = (ListPreference) findPreference(getString(listId));
    list.setEntries(options);
  }
  
  /**
   * Sets the display options for 'GPS accuracy'.
   */
  private void setGpsAccuracyOptions(boolean isMetric, int listId) {
    String[] values = getResources().getStringArray(R.array.recording_gps_accuracy_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      String format;
      if (isMetric) {
        if (values[i].equals(GPS_ACCURACY_RECOMMENDED)) {
          format = getString(R.string.value_integer_meter_recommended);
        } else if (values[i].equals(GPS_ACCURACY_EXCELLENT)) {
          format = getString(R.string.value_integer_meter_excellent_gps);
        } else if (values[i].equals(GPS_ACCURACY_POOR)) {
          format = getString(R.string.value_integer_meter_poor_gps);
        } else {
          format = getString(R.string.value_integer_meter);
        }
        options[i] = String.format(format, value);
      } else {
        value = (int) (value * UnitConversions.M_TO_FT);
        if (value < 2000) {
          if (values[i].equals(GPS_ACCURACY_RECOMMENDED)) {
            format = getString(R.string.value_integer_feet_recommended);
          } else if (values[i].equals(GPS_ACCURACY_EXCELLENT)) {
            format = getString(R.string.value_integer_feet_excellent_gps);
          } else {
            format = getString(R.string.value_integer_feet);
          }
          options[i] = String.format(format, value);
        } else {
          double mile = value / UnitConversions.MI_TO_FEET;
          if (values[i].equals(GPS_ACCURACY_POOR)) {
            format = getString(R.string.value_float_mile_poor_gps);
          } else {
            format = getString(R.string.value_float_mile);
          }
          options[i] = String.format(format, mile);    
        }
      }
    }
    ListPreference list = (ListPreference) findPreference(getString(listId));
    list.setEntries(options);
  }

  /**
   * Configures preference actions related to bluetooth.
   */
  private void configureBluetoothPreferences() {
    if (BluetoothDeviceUtils.isBluetoothMethodSupported()) {
      // Populate the list of bluetooth devices
      populateBluetoothDeviceList();

      // Make the pair devices preference go to the system preferences
      findPreference(getString(R.string.bluetooth_pairing_key))
          .setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
              Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
              startActivity(settingsIntent);
              return false;
            }
          });
    }
  }

  /**
   * Populates the list preference with all available bluetooth devices.
   */
  private void populateBluetoothDeviceList() {
    // Build the list of entries and their values
    List<String> entries = new ArrayList<String>();
    List<String> entryValues = new ArrayList<String>();

    // The actual devices
    BluetoothDeviceUtils.getInstance().populateDeviceLists(entries, entryValues);

    CharSequence[] entriesArray = entries.toArray(new CharSequence[entries.size()]);
    CharSequence[] entryValuesArray = entryValues.toArray(new CharSequence[entryValues.size()]);
    ListPreference devicesPreference =
        (ListPreference) findPreference(getString(R.string.bluetooth_sensor_key));
    devicesPreference.setEntryValues(entryValuesArray);
    devicesPreference.setEntries(entriesArray);
  }

  /** Callback for when user asks to reset all settings. */
  private void onResetPreferencesClick() {
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setCancelable(true)
        .setTitle(R.string.settings_reset)
        .setMessage(R.string.settings_reset_dialog_message)
        .setPositiveButton(android.R.string.ok,
            new OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int button) {
                onResetPreferencesConfirmed();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
    dialog.show();
  }

  /** Callback for when user confirms resetting all settings. */
  private void onResetPreferencesConfirmed() {
    // Change preferences in a separate thread.
    new Thread() {
      @Override
      public void run() {
        Log.i(TAG, "Resetting all settings");

        // Actually wipe preferences (and save synchronously).
        preferences.edit().clear().commit();

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
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
    CheckBoxPreference metricUnitsPreference = (CheckBoxPreference) findPreference(
        getString(R.string.metric_units_key));
    if(metricUnitsPreference.isChecked()) {
      return;
    }
    // Convert miles/h to km/h
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    String metricspeed = prefs.getString(getString(id), null);
    int englishspeed;
    try {
      englishspeed = (int) (Double.parseDouble(metricspeed) * UnitConversions.KMH_TO_MPH);
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
    CheckBoxPreference metricUnitsPreference = (CheckBoxPreference) findPreference(
        getString(R.string.metric_units_key));
    String metricspeed;
    if(!metricUnitsPreference.isChecked()) {
      // Convert miles/h to km/h
      try {
        metricspeed = String.valueOf(
            (int) (Double.parseDouble(newValue) * UnitConversions.MPH_TO_KMH) + 1);
      } catch (NumberFormatException e) {
        metricspeed = "0";
      }
    } else {
      metricspeed = newValue;
    }
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    prefs.edit().putString(getString(id), metricspeed).commit();
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
