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

import com.google.android.apps.mytracks.services.sensors.ant.AntUtils;
import com.google.android.apps.mytracks.util.BluetoothDeviceUtils;
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
import java.util.Arrays;
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

    ListPreference sensorTypeListPreference = (ListPreference) findPreference(
        getString(R.string.sensor_type_key));
    List<String> sensorTypeEntries = Arrays.asList(getResources().getStringArray(
        AntUtils.hasAntSupport(this) ? R.array.sensor_type_all_options
            : R.array.sensor_type_bluetooth_options));
    List<String> sensorTypeEntryValues = Arrays.asList(getResources().getStringArray(
        AntUtils.hasAntSupport(this) ? R.array.sensor_type_all_values
            : R.array.sensor_type_bluetooth_values));
    sensorTypeListPreference.setEntries(sensorTypeEntries.toArray(
        new CharSequence[sensorTypeEntries.size()]));
    sensorTypeListPreference.setEntryValues(
        sensorTypeEntryValues.toArray(new CharSequence[sensorTypeEntryValues.size()]));
    sensorTypeListPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateUiBySensorType((String) newValue);
        return true;
      }
    });

    updateUiBySensorType(sensorTypeListPreference.getValue());

    findPreference(getString(R.string.settings_sensor_bluetooth_pairing_key))
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
          public boolean onPreferenceClick(Preference preference) {
            Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(settingsIntent);
            return true;
          }
        });

    if (!AntUtils.hasAntSupport(this)) {
      PreferenceScreen rootPreferenceScreen = (PreferenceScreen) findPreference(
          getString(R.string.settings_sensor_root_key));
      rootPreferenceScreen.removePreference(
          findPreference(getString(R.string.settings_sensor_ant_key)));
    }
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

    Preference antHeartRateSensorId = findPreference(
        getString(R.string.ant_heart_rate_sensor_id_key));
    if (antHeartRateSensorId != null) {
      antHeartRateSensorId.setEnabled(getString(R.string.sensor_type_value_ant).equals(sensorType));
    }

    Preference antSrmBridgetSensorId = findPreference(
        getString(R.string.ant_srm_bridge_sensor_id_key));
    if (antSrmBridgetSensorId != null) {
      antSrmBridgetSensorId.setEnabled(
          getString(R.string.sensor_type_value_srm_ant_bridge).equals(sensorType));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateBluetoothSensorListPreference();
  }

  /**
   * Updates the bluetooth sensor list preference.
   */
  private void updateBluetoothSensorListPreference() {
    @SuppressWarnings("deprecation")
    ListPreference bluetoothSensorListPreference = (ListPreference) findPreference(
        getString(R.string.bluetooth_sensor_key));
    List<String> entries = new ArrayList<String>();
    List<String> entryValues = new ArrayList<String>();
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (bluetoothAdapter != null) {
      BluetoothDeviceUtils.populateDeviceLists(bluetoothAdapter, entries, entryValues);
    }
    bluetoothSensorListPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
    bluetoothSensorListPreference.setEntryValues(entryValues.toArray(
        new CharSequence[entryValues.size()]));
    if (entries.size() == 1) {
      bluetoothSensorListPreference.setValueIndex(0);
    }
  }
}
