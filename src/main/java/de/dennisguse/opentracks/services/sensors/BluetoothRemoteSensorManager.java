/*
 * Copyright 2010 Google Inc.
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

package de.dennisguse.opentracks.services.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Bluetooth LE sensor manager.
 * Should only be instantiated once!
 *
 * @author Sandor Dornbush
 */
public class BluetoothRemoteSensorManager implements BluetoothConnectionManager.EventCallback {

    public static final long MAX_SENSOR_DATE_SET_AGE_MS = 5000;

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    private static final BluetoothAdapter bluetoothAdapter = BluetoothUtils.getDefaultBluetoothAdapter(TAG);

    private final Context context;

    private final SharedPreferences sharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (bluetoothConnectionManager != null && PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_heart_rate_key, key)) {
                if (PreferencesUtils.isBluetoothHeartRateSensorAddressDefault(context)) {
                    stop();
                    return;
                }
                String address = PreferencesUtils.getBluetoothHeartRateSensorAddress(context);
                if (bluetoothConnectionManager.isSameBluetoothDevice(address)) {
                    return;
                }

                disconnect();
                startCurrentSensor();
            }
        }
    };

    private SensorDataSet sensorDataSet = null;
    private BluetoothConnectionManager bluetoothConnectionManager;

    /**
     * @param context the context
     */
    public BluetoothRemoteSensorManager(Context context) {
        this.context = context;
        sharedPreferences = PreferencesUtils.getSharedPreferences(context);
    }

    public void start() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        startCurrentSensor();
    }

    public void stop() {
        disconnect();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }


    public boolean isEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public SensorDataSet getSensorDataSet() {
        return sensorDataSet;
    }

    public boolean isSensorDataSetValid() {
        SensorDataSet sensorDataSet = getSensorDataSet();
        if (sensorDataSet == null) {
            return false;
        }
        return sensorDataSet.isRecent(MAX_SENSOR_DATE_SET_AGE_MS);
    }

    private void startCurrentSensor() {
        if (!isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled.");
            return;
        }

        if (PreferencesUtils.isBluetoothHeartRateSensorAddressDefault(context)) {
            Log.w(TAG, "No bluetooth address.");
            return;
        }
        String address = PreferencesUtils.getBluetoothHeartRateSensorAddress(context);
        Log.i(TAG, "Connecting to bluetooth address: " + address);

        BluetoothDevice device;
        try {
            device = bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to get remote device for: " + address, e);

            String toastMessage = context.getString(R.string.sensor_not_known, address);
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();

            return;
        }

        disconnect();

        bluetoothConnectionManager = new BluetoothConnectionManager(context, device, this);
        bluetoothConnectionManager.connect();
    }

    private void disconnect() {
        if (bluetoothConnectionManager != null) {
            bluetoothConnectionManager.disconnect();
            bluetoothConnectionManager = null;
        }
    }

    @Override
    public void connecting(String sensorName) {
    }

    @Override
    public void connected(String sensorName) {
    }

    @Override
    public void onSensorDataReceived(SensorDataSet sensorDataSet) {
        this.sensorDataSet = sensorDataSet;
    }

    @Override
    public void disconnected(String sensorName) {
    }
}
