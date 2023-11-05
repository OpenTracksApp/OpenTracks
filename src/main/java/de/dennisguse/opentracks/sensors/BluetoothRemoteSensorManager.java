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

package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.PermissionRequester;

/**
 * Bluetooth LE sensor manager: manages connections to Bluetooth LE sensors.
 * <p>
 * Note: should only be instantiated once.
 * <p>
 * TODO: listen for Bluetooth enabled/disabled events.
 * <p>
 * TODO: In case, a cycling (Cadence and Speed) sensor reports both values, testing is required.
 * We establish two GATT separate GATT connections (as if two different sensors were used).
 * However, it is not clear if this is allowed.
 * Even if this works, it is not clear what happens if a user (while recording) changes one of the sensors in the settings as this will trigger a disconnect of one GATT.
 *
 * @author Sandor Dornbush
 */
public class BluetoothRemoteSensorManager implements SensorConnector, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = BluetoothRemoteSensorManager.class.getSimpleName();

    public static final Duration MAX_SENSOR_DATE_SET_AGE = Duration.ofSeconds(5);

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final Handler handler;
    private boolean started = false;

    private final BluetoothConnectionManager heartRate;
    private final BluetoothConnectionManager cyclingCadence;
    private final BluetoothConnectionManager cyclingSpeed;
    private final BluetoothConnectionManager cyclingPower;
    private final BluetoothConnectionManager runningSpeedAndCadence;

    public BluetoothRemoteSensorManager(@NonNull Context context, @NonNull Handler handler, @Nullable SensorManager.SensorDataChangedObserver observer) {
        this.context = context;
        this.handler = handler;
        bluetoothAdapter = BluetoothUtils.getAdapter(context);

        this.heartRate = new BluetoothConnectionManager(observer, new BluetoothHandlerManagerHeartRate());
        this.cyclingCadence = new BluetoothConnectionManager(observer, new BluetoothHandlerCyclingDistanceSpeed());
        this.cyclingSpeed = new BluetoothConnectionManager(observer, new BluetoothHandlerCyclingDistanceSpeed());
        this.cyclingPower = new BluetoothConnectionManager(observer, new BluetoothHandlerManagerCyclingPower());
        this.runningSpeedAndCadence = new BluetoothConnectionManager(observer, new BluetoothHandlerRunningSpeedAndCadence());

    }

    @Override
    public void start(Context context, Handler handler) {
        started = true;

        // Triggers connection startup
        onSharedPreferenceChanged(null, null);
    }

    @Override
    public synchronized void stop(Context context) {
        heartRate.disconnect();
        cyclingCadence.disconnect();
        cyclingSpeed.disconnect();
        cyclingPower.disconnect();
        runningSpeedAndCadence.disconnect();

        started = false;
    }

    public boolean isEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private synchronized void connect(BluetoothConnectionManager connectionManager, String address) {
        if (!isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled.");
            return;
        }

        if (PreferencesUtils.isBluetoothSensorAddressNone(address)) {
            Log.w(TAG, "No Bluetooth address.");
            connectionManager.disconnect();
            return;
        }

        // Check if there is an ongoing connection; if yes, check if the address changed.
        if (connectionManager.isSameBluetoothDevice(address)) {
            return;
        } else {
            connectionManager.disconnect();
        }
        if (!PermissionRequester.BLUETOOTH.hasPermission(context)) {
            Log.w(TAG, "BLUETOOTH_SCAN and/or BLUETOOTH_CONNECT not granted; not connecting.");
        }

        Log.i(TAG, "Connecting to bluetooth address: " + address);
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connectionManager.connect(context, handler, device);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to get remote device for: " + address, e);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences unused, @Nullable String key) {
        if (!started) return;

        if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_heart_rate_key, key)) {
            String address = PreferencesUtils.getBluetoothHeartRateSensorAddress();
            connect(heartRate, address);
        }

        if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_cycling_cadence_key, key)) {
            String address = PreferencesUtils.getBluetoothCyclingCadenceSensorAddress();
            connect(cyclingCadence, address);
        }

        if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_cycling_speed_key, key)) {
            String address = PreferencesUtils.getBluetoothCyclingSpeedSensorAddress();

            connect(cyclingSpeed, address);
        }

        if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_cycling_power_key, key)) {
            String address = PreferencesUtils.getBluetoothCyclingPowerSensorAddress();

            connect(cyclingPower, address);
        }

        if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_running_speed_and_cadence_key, key)) {
            String address = PreferencesUtils.getBluetoothRunningSpeedAndCadenceAddress();

            connect(runningSpeedAndCadence, address);
        }
    }
}
