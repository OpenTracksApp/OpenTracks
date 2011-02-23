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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.apps.mytracks.io.backup.BackupActivityHelper;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.services.sensors.ant.AntUtils;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.BluetoothDeviceUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

/**
 * An activity that let's the user see and edit the settings.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MyTracksSettings extends PreferenceActivity {

  public static final String SETTINGS_NAME = "MyTracksSettings";

  /*
   * Default values - keep in sync with those in preferences.xml.
   */
  public static final int DEFAULT_AUTO_RESUME_TRACK_TIMEOUT = 10;  // In min.
  public static final int DEFAULT_ANNOUNCEMENT_FREQUENCY = -1;
  public static final int DEFAULT_MAX_RECORDING_DISTANCE = 200;
  public static final int DEFAULT_MIN_RECORDING_DISTANCE = 5;
  public static final int DEFAULT_MIN_RECORDING_INTERVAL = 0;
  public static final int DEFAULT_MIN_REQUIRED_ACCURACY = 200;
  public static final int DEFAULT_SPLIT_FREQUENCY = 0;

  private BackupPreferencesListener backupListener;

  private SharedPreferences preferences;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // The volume we want to control is the Text-To-Speech volume
    ApiFeatures apiFeatures = ApiFeatures.getInstance();
    int volumeStream =
        new StatusAnnouncerFactory(apiFeatures).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // Tell it where to read/write preferences
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(SETTINGS_NAME);
    preferenceManager.setSharedPreferencesMode(0);

    // Set up automatic preferences backup
    backupListener = BackupPreferencesListener.create(this, apiFeatures);
    preferences = preferenceManager.getSharedPreferences();
    preferences.registerOnSharedPreferenceChangeListener(backupListener);

    // Load the preferences to be displayed
    addPreferencesFromResource(R.xml.preferences);

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
            updatePreferenceUnits(isMetric);
            return true;
          }
        });
    updatePreferenceUnits(metricUnitsPreference.isChecked());

    customizeSensorTypePreferences();

    // Disable TTS announcement preference if not available
    if (!apiFeatures.hasTextToSpeech()) {
      IntegerListPreference announcementFrequency =
          (IntegerListPreference) findPreference(
              getString(R.string.announcement_frequency_key));
      announcementFrequency.setEnabled(false);
      announcementFrequency.setValue("-1");
      announcementFrequency.setSummary(
          R.string.settings_not_available_summary);
    }
  }

  private void customizeSensorTypePreferences() {
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

      String[] antValues = getResources().getStringArray(R.array.ant_sensor_type_values);
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

  @Override
  protected void onResume() {
    super.onResume();

    configureBluetoothPreferences();
    Preference backupNowPreference =
        findPreference(getString(R.string.backup_to_sd_key));
    Preference restoreNowPreference =
        findPreference(getString(R.string.restore_from_sd_key));

    // If recording, disable backup/restore
    // (we don't want to get to inconsistent states)
    boolean recording =
        preferences.getLong(getString(R.string.recording_track_key), -1) != -1;
    backupNowPreference.setEnabled(!recording);
    restoreNowPreference.setEnabled(!recording);
    backupNowPreference.setSummary(
        recording ? R.string.settings_no_backup_while_recording
                  : R.string.settings_backup_to_sd_summary);
    restoreNowPreference.setSummary(
        recording ? R.string.settings_no_backup_while_recording
                  : R.string.settings_restore_from_sd_summary);

    // Add actions to the backup preferences
    backupNowPreference.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            BackupActivityHelper backupHelper =
                new BackupActivityHelper(MyTracksSettings.this);
            backupHelper.writeBackup();
            return true;
          }
        });
    restoreNowPreference.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            BackupActivityHelper backupHelper =
                new BackupActivityHelper(MyTracksSettings.this);
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
        getString(R.string.zephyr_sensor_type).equals(sensorType);
    findPreference(
        getString(R.string.bluetooth_sensor_key)).setEnabled(usesBluetooth);
    findPreference(
        getString(R.string.bluetooth_pairing_key)).setEnabled(usesBluetooth);

    // Update the ANT+ sensors.
    // TODO: Only enable on phones that have ANT+.
    findPreference(getString(R.string.ant_heart_rate_sensor_id_key))
        .setEnabled(getString(R.string.ant_sensor_type).equals(sensorType));
    findPreference(getString(R.string.ant_srm_bridge_sensor_id_key))
        .setEnabled(getString(R.string.srm_ant_bridge_sensor_type).equals(sensorType));
  }

  /**
   * Updates all the preferences which give options with distance units to use
   * the proper unit the user has selected.
   *
   * @param isMetric true to use metric units, false to use imperial
   */
  private void updatePreferenceUnits(boolean isMetric) {
    final ListPreference minRecordingDistance =
        (ListPreference) findPreference(
            getString(R.string.min_recording_distance_key));
    final ListPreference maxRecordingDistance =
        (ListPreference) findPreference(
            getString(R.string.max_recording_distance_key));
    final ListPreference minRequiredAccuracy =
        (ListPreference) findPreference(
            getString(R.string.min_required_accuracy_key));
    final ListPreference splitFrequency =
        (ListPreference) findPreference(
            getString(R.string.split_frequency_key));

    minRecordingDistance.setEntries(isMetric
        ? R.array.min_recording_distance_options
        : R.array.min_recording_distance_options_ft);
    maxRecordingDistance.setEntries(isMetric
        ? R.array.max_recording_distance_options
        : R.array.max_recording_distance_options_ft);
    minRequiredAccuracy.setEntries(isMetric
        ? R.array.min_required_accuracy_options
        : R.array.min_required_accuracy_options_ft);
    splitFrequency.setEntries(isMetric
        ? R.array.split_frequency_options
        : R.array.split_frequency_options_ft);
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
}
