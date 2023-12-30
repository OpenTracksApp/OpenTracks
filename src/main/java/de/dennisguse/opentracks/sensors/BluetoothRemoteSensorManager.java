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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Bluetooth LE sensor manager: manages connections to Bluetooth LE sensors.
 * <p>
 * Note: should only be instantiated once.
 * TODO: listen for Bluetooth enabled/disabled events.
 * @author Sandor Dornbush
 */
public class BluetoothRemoteSensorManager implements SensorConnector, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = BluetoothRemoteSensorManager.class.getSimpleName();

    public static final Duration MAX_SENSOR_DATE_SET_AGE = Duration.ofSeconds(5);

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

        BluetoothAdapter bluetoothAdapter = BluetoothUtils.getAdapter(context);
        this.heartRate = new BluetoothConnectionManager(bluetoothAdapter, observer, new BluetoothHandlerManagerHeartRate());
        this.cyclingCadence = new BluetoothConnectionManager(bluetoothAdapter, observer, new BluetoothHandlerCyclingCadence());
        this.cyclingSpeed = new BluetoothConnectionManager(bluetoothAdapter, observer, new BluetoothHandlerCyclingDistanceSpeed());
        this.cyclingPower = new BluetoothConnectionManager(bluetoothAdapter, observer, new BluetoothHandlerManagerCyclingPower());
        this.runningSpeedAndCadence = new BluetoothConnectionManager(bluetoothAdapter, observer, new BluetoothHandlerRunningSpeedAndCadence());
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

    private synchronized void connect(BluetoothConnectionManager connectionManager, String address) {
        connectionManager.connect(context, handler, address);
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
