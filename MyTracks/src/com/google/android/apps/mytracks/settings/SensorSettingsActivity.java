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

import com.dsi.ant.AntInterface;
import com.google.android.apps.mytracks.services.sensors.ant.AntSensorManager;
import com.google.android.apps.mytracks.util.BluetoothDeviceUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity for accessing sensor settings.
 * 
 * @author Jimmy Shih
 */
public class SensorSettingsActivity extends AbstractSettingsActivity {

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.sensor_settings);

    boolean hasAntSupport = AntInterface.hasAntSupport(this);

    configSensorType(hasAntSupport);

    findPreference(getString(R.string.settings_sensor_bluetooth_pairing_key))
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
          public boolean onPreferenceClick(Preference preference) {
            Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(settingsIntent);
            return true;
          }
        });

    if (!hasAntSupport) {
      PreferenceScreen rootPreferenceScreen = (PreferenceScreen) findPreference(
          getString(R.string.settings_sensor_root_key));
      rootPreferenceScreen.removePreference(
          findPreference(getString(R.string.settings_sensor_ant_key)));
    }
  }

  @SuppressWarnings("deprecation")
  private void configSensorType(boolean hasAntSupport) {
    ListPreference preference = (ListPreference) findPreference(
        getString(R.string.sensor_type_key));
    String value = PreferencesUtils.getString(
        this, R.string.sensor_type_key, PreferencesUtils.SENSOR_TYPE_DEFAULT);
    String[] options = getResources().getStringArray(
        hasAntSupport ? R.array.sensor_type_all_options : R.array.sensor_type_bluetooth_options);
    String[] values = getResources().getStringArray(
        hasAntSupport ? R.array.sensor_type_all_values : R.array.sensor_type_bluetooth_values);

    if (!hasAntSupport && value.equals(R.string.sensor_type_value_ant)) {
      value = PreferencesUtils.SENSOR_TYPE_DEFAULT;
      PreferencesUtils.setString(this, R.string.sensor_type_key, value);
    }

    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        updateUiBySensorType((String) newValue);
        return true;
      }
    };
    configureListPreference(preference, options, options, values, value, listener);
  }

  /**
   * Updates the UI based on the sensor type.
   * 
   * @param sensorType the sensor type
   */
  @SuppressWarnings("deprecation")
  private void updateUiBySensorType(String sensorType) {
    boolean isBluetooth = getString(R.string.sensor_type_value_polar).equals(sensorType)
        || getString(R.string.sensor_type_value_zephyr).equals(sensorType);
    findPreference(getString(R.string.settings_sensor_bluetooth_key)).setEnabled(isBluetooth);

    boolean isAnt = getString(R.string.sensor_type_value_ant).equals(sensorType);
    updateAntSensor(R.string.settings_sensor_ant_reset_heart_rate_monitor_key,
        R.string.ant_heart_rate_monitor_id_key, isAnt);
    updateAntSensor(R.string.settings_sensor_ant_reset_speed_distance_monitor_key,
        R.string.ant_speed_distance_monitor_id_key, isAnt);
    updateAntSensor(R.string.settings_sensor_ant_reset_bike_cadence_sensor_key,
        R.string.ant_bike_cadence_sensor_id_key, isAnt);
    updateAntSensor(R.string.settings_sensor_ant_reset_combined_bike_sensor_key,
        R.string.ant_combined_bike_sensor_id_key, isAnt);
  }

  /**
   * Updates an ant sensor.
   * 
   * @param preferenceKey the preference key
   * @param valueKey the value key
   * @param enabled true if enabled
   */
  @SuppressWarnings("deprecation")
  private void updateAntSensor(int preferenceKey, final int valueKey, boolean enabled) {
    Preference preference = findPreference(getString(preferenceKey));
    if (preference != null) {
      preference.setEnabled(enabled);
      int deviceId = PreferencesUtils.getInt(this, valueKey, AntSensorManager.WILDCARD);
      if (deviceId == AntSensorManager.WILDCARD) {
        preference.setSummary(R.string.settings_sensor_ant_not_connected);
      } else {
        preference.setSummary(getString(R.string.settings_sensor_ant_paired, deviceId));
      }
      preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
          @Override
        public boolean onPreferenceClick(Preference pref) {
          PreferencesUtils.setInt(SensorSettingsActivity.this, valueKey, AntSensorManager.WILDCARD);
          pref.setSummary(R.string.settings_sensor_ant_not_connected);
          return true;
        }
      });
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Update each time in case the list of bluetooth sensors has changed
    configBluetoothSensor();
  }

  /**
   * Configures the bluetooth sensor.
   */
  @SuppressWarnings("deprecation")
  private void configBluetoothSensor() {
    ListPreference preference = (ListPreference) findPreference(
        getString(R.string.bluetooth_sensor_key));
    String value = PreferencesUtils.getString(
        this, R.string.bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);
    List<String> optionsList = new ArrayList<String>();
    List<String> valuesList = new ArrayList<String>();
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter != null) {
      BluetoothDeviceUtils.populateDeviceLists(bluetoothAdapter, optionsList, valuesList);
    }
    String[] options = optionsList.toArray(new String[optionsList.size()]);
    String[] values = valuesList.toArray(new String[valuesList.size()]);

    if (valuesList.size() == 1) {
      if (!valuesList.get(0).equals(value)) {
        value = valuesList.get(0);
        PreferencesUtils.setString(this, R.string.bluetooth_sensor_key, value);
      }
    } else {
      if (!valuesList.contains(value)) {
        value = PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT;
        PreferencesUtils.setString(this, R.string.bluetooth_sensor_key, value);
      }
    }
    configureListPreference(preference, options, options, values, value, null);
  }
}
