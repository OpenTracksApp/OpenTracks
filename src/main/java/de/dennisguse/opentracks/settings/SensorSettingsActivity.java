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

package de.dennisguse.opentracks.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;

import de.dennisguse.opentracks.util.BluetoothDeviceUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity for accessing sensor settings.
 *
 * @author Jimmy Shih
 */
public class SensorSettingsActivity extends AbstractSettingsActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings_sensors);

        findPreference(getString(R.string.settings_sensor_bluetooth_pairing_key))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(settingsIntent);
                        return true;
                    }
                });
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
    private void configBluetoothSensor() {
        ListPreference preference = (ListPreference) findPreference(getString(R.string.bluetooth_sensor_key));
        String value = PreferencesUtils.getString(this, R.string.bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);
        List<String> devicesNameList = new ArrayList<>();
        List<String> devicesAddressList = new ArrayList<>();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            BluetoothDeviceUtils.populateDeviceLists(bluetoothAdapter, devicesNameList, devicesAddressList);
        }

        // Was the previously configured device unpaired? Then forget it.
        if (!devicesAddressList.contains(value)) {
            value = PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT;
            PreferencesUtils.setString(this, R.string.bluetooth_sensor_key, value);
        }

        devicesNameList.add(0, getString(R.string.value_none));
        devicesAddressList.add(0, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);

        String[] values = devicesAddressList.toArray(new String[0]);
        preference.setEntryValues(values);

        String[] options = devicesNameList.toArray(new String[0]);
        preference.setEntries(options);
    }
}
