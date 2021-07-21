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

import androidx.annotation.NonNull;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorData;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataRunning;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

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
public class BluetoothRemoteSensorManager implements BluetoothConnectionManager.SensorDataObserver {

    private static final String TAG = BluetoothRemoteSensorManager.class.getSimpleName();

    public static final Duration MAX_SENSOR_DATE_SET_AGE_MS = Duration.ofSeconds(5);

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    private boolean started = false;

    private final SharedPreferences sharedPreferences;
    private Distance preferenceWheelCircumference;

    private final BluetoothConnectionManager.HeartRate heartRate = new BluetoothConnectionManager.HeartRate(this);
    private final BluetoothConnectionManager.CyclingCadence cyclingCadence = new BluetoothConnectionManager.CyclingCadence(this);
    private final BluetoothConnectionManager.CyclingDistanceSpeed cyclingSpeed = new BluetoothConnectionManager.CyclingDistanceSpeed(this);
    private final BluetoothConnectionManager.CyclingPower cyclingPower = new BluetoothConnectionManager.CyclingPower(this);
    private final BluetoothConnectionManager.RunningSpeedAndCadence runningSpeedAndCadence = new BluetoothConnectionManager.RunningSpeedAndCadence(this);

    private final SensorDataSet sensorDataSet = new SensorDataSet();

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!started) return;

            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_heart_rate_key, key)) {
                String address = PreferencesUtils.getBluetoothHeartRateSensorAddress(sharedPreferences, context);
                connect(heartRate, address);
            }

            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_cycling_cadence_key, key)) {
                String address = PreferencesUtils.getBluetoothCyclingCadenceSensorAddress(sharedPreferences, context);
                connect(cyclingCadence, address);
            }

            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_cycling_speed_key, key)) {
                String address = PreferencesUtils.getBluetoothCyclingSpeedSensorAddress(sharedPreferences, context);

                connect(cyclingSpeed, address);
            }
            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key, key)) {
                preferenceWheelCircumference = PreferencesUtils.getWheelCircumference(sharedPreferences, context);
            }

            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_cycling_power_key, key)) {
                String address = PreferencesUtils.getBluetoothCyclingPowerSensorAddress(sharedPreferences, context);

                connect(cyclingPower, address);
            }


            if (PreferencesUtils.isKey(context, R.string.settings_sensor_bluetooth_running_speed_and_cadence_key, key)) {
                String address = PreferencesUtils.getBluetoothRunningSpeedAndCadenceAddress(sharedPreferences, context);

                connect(runningSpeedAndCadence, address);
            }
        }
    };

    public BluetoothRemoteSensorManager(Context context) {
        this.context = context;
        sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        bluetoothAdapter = BluetoothUtils.getAdapter(context);
    }

    public void start() {
        started = true;
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        //Trigger connection startup
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);
    }

    public synchronized void stop() {
        heartRate.disconnect();
        cyclingCadence.disconnect();
        cyclingSpeed.disconnect();
        cyclingPower.disconnect();
        runningSpeedAndCadence.disconnect();

        sensorDataSet.clear();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        started = false;
    }

    public boolean isEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private synchronized void connect(BluetoothConnectionManager<?> connectionManager, String address) {
        if (!isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled.");
            return;
        }

        if (PreferencesUtils.isBluetoothSensorAddressNone(context, address)) {
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

        Log.i(TAG, "Connecting to bluetooth address: " + address);
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connectionManager.connect(context, device);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to get remote device for: " + address, e);
        }
    }

    public SensorDataSet fill(@NonNull TrackPoint trackPoint) {
        sensorDataSet.fillTrackPoint(trackPoint);
        return new SensorDataSet(sensorDataSet);
    }

    public void reset() {
        sensorDataSet.reset();
    }

    @Override
    public synchronized void onChanged(SensorData<?> sensorData) {
        if (sensorData instanceof SensorDataCycling.Cadence) {
            SensorDataCycling.Cadence previous = sensorDataSet.getCyclingCadence();
            Log.d(TAG, "Previous: " + previous + "; current: " + sensorData);

            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cadence data repeated.");
                return;
            }
            ((SensorDataCycling.Cadence) sensorData).compute(previous);
        }
        if (sensorData instanceof SensorDataCycling.DistanceSpeed) {
            SensorDataCycling.DistanceSpeed previous = sensorDataSet.getCyclingDistanceSpeed();
            Log.d(TAG, "Previous: " + previous + "; Current" + sensorData);
            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cycling speed data repeated.");
                return;
            }
            ((SensorDataCycling.DistanceSpeed) sensorData).compute(previous, preferenceWheelCircumference);
        }
        if (sensorData instanceof SensorDataRunning) {
            SensorDataRunning previous = sensorDataSet.getRunningDistanceSpeedCadence();
            Log.d(TAG, "Previous: " + previous + "; Current" + sensorData);
            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: running speed data repeated.");
                return;
            }
            ((SensorDataRunning) sensorData).compute(previous);
        }

        sensorDataSet.set(sensorData);
    }

    @Override
    public void onDisconnecting(SensorData<?> sensorData) {
        sensorDataSet.remove(sensorData);
    }
}
