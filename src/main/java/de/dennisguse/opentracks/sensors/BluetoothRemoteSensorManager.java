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

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorData;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
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
public class BluetoothRemoteSensorManager implements BluetoothConnectionManager.SensorDataObserver {

    private static final String TAG = BluetoothRemoteSensorManager.class.getSimpleName();

    public static final Duration MAX_SENSOR_DATE_SET_AGE = Duration.ofSeconds(5);

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final Handler handler;
    private boolean started = false;
    private static final String PREVIOUS = "Previous: ";
    private Distance preferenceWheelCircumference;

    private final BluetoothRemoteSensorManagerHeartRate heartRate = new BluetoothRemoteSensorManagerHeartRate(this);
    private final BluetoothRemoteSensorManagerCyclingCadence cyclingCadence = new BluetoothRemoteSensorManagerCyclingCadence(this);
    private final BluetoothRemoteSensorManagerCyclingDistanceSpeed cyclingSpeed = new BluetoothRemoteSensorManagerCyclingDistanceSpeed(this);
    private final BluetoothRemoteSensorManagerCyclingPower cyclingPower = new BluetoothRemoteSensorManagerCyclingPower(this);
    private final BluetoothRemoteSensorManagerRunningSpeedAndCadence runningSpeedAndCadence = new BluetoothRemoteSensorManagerRunningSpeedAndCadence(this);

    private final SensorDataSet sensorDataSet = new SensorDataSet();

    private final SensorDataSetChangeObserver observer;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
            if (PreferencesUtils.isKey(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key, key)) {
                preferenceWheelCircumference = PreferencesUtils.getWheelCircumference();
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
    };

    public BluetoothRemoteSensorManager(@NonNull Context context, @NonNull Handler handler, @NonNull SensorDataSetChangeObserver observer) {
        this.context = context;
        this.handler = handler;
        this.observer = observer;
        bluetoothAdapter = BluetoothUtils.getAdapter(context);
    }

    public void start() {
        started = true;

        //Registering triggers connection startup
        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public synchronized void stop() {
        heartRate.disconnect();
        cyclingCadence.disconnect();
        cyclingSpeed.disconnect();
        cyclingPower.disconnect();
        runningSpeedAndCadence.disconnect();

        sensorDataSet.clear();

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
        if (sensorData instanceof SensorDataCyclingCadence) {
            SensorDataCyclingCadence previous = sensorDataSet.getCyclingCadence();
            Log.d(TAG, PREVIOUS + previous + "; current: " + sensorData);

            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cadence data repeated.");
                return;
            }
            ((SensorDataCyclingCadence) sensorData).compute(previous);
        }
        if (sensorData instanceof SensorDataCyclingDistanceSpeed) {
            SensorDataCyclingDistanceSpeed previous = sensorDataSet.getCyclingDistanceSpeed();
            Log.d(TAG, PREVIOUS + previous + "; Current" + sensorData);
            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cycling speed data repeated.");
                return;
            }
            ((SensorDataCyclingDistanceSpeed) sensorData).compute(previous, preferenceWheelCircumference);
        }
        if (sensorData instanceof SensorDataRunning) {
            SensorDataRunning previous = sensorDataSet.getRunningDistanceSpeedCadence();
            Log.d(TAG, PREVIOUS + previous + "; Current" + sensorData);
            if (sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: running speed data repeated.");
                return;
            }
            ((SensorDataRunning) sensorData).compute(previous);
        }

        sensorDataSet.set(sensorData);
        observer.onChange(new SensorDataSet(sensorDataSet));
    }

    @Override
    public void onDisconnecting(SensorData<?> sensorData) {
        sensorDataSet.remove(sensorData);
    }

    @NonNull
    @Override
    public Handler getHandler() {
        return handler;
    }

    public interface SensorDataSetChangeObserver {
        void onChange(SensorDataSet sensorDataSet);
    }
}
