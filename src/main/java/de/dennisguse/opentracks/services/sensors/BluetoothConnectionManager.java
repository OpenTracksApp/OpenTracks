/*
 * Copyright (C) 2010 Google Inc.
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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.content.sensor.SensorState;

/**
 * Manages connection to Bluetooth LE heart rate monitor.
 * TODO: shutdown connection.
 */
public class BluetoothConnectionManager {

    // Message types sent to handler
    static final int MESSAGE_CONNECTING = 1;
    static final int MESSAGE_CONNECTED = 2;
    static final int MESSAGE_READ = 3;
    static final int MESSAGE_DISCONNECTED = 4;

    private static final UUID HEART_RATE_SERVICE_UUID = new UUID(0x180D00001000L, 0x800000805f9b34fbL);
    private static final UUID HEART_RATE_MEASUREMENT_CHAR_UUID = new UUID(0x2A3700001000L, 0x800000805f9b34fbL);
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = new UUID(0x290200001000L, 0x800000805f9b34fbL);

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    private final Context context;
    private final Handler handler;

    private SensorState sensorState;

    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;

    private BluetoothGattCallback connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "Connecting to sensor: " + gatt.getDevice());
                    setState(SensorState.CONNECTING);

                    handler.obtainMessage(MESSAGE_CONNECTING, gatt.getDevice().getName()).sendToTarget();
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "Connected to sensor: " + gatt.getDevice());
                    setState(SensorState.CONNECTED);

                    gatt.discoverServices();

                    handler.obtainMessage(MESSAGE_CONNECTED, gatt.getDevice().getName()).sendToTarget();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "Disconnecting from sensor: " + gatt.getDevice());
                    setState(SensorState.DISCONNECTING);

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "Disconnected from sensor: " + gatt.getDevice());
                    setState(SensorState.DISCONNECTED);

                    handler.obtainMessage(MESSAGE_DISCONNECTED, gatt.getDevice().getName()).sendToTarget();
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattCharacteristic characteristic = gatt
                    .getService(HEART_RATE_SERVICE_UUID)
                    .getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID);

            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            int heartRate = BluetoothLEUtils.parseHeartRate(characteristic);

            Log.d(TAG, "Received heart beat rate: " + heartRate);
            SensorDataSet sensorDataSet = new SensorDataSet(heartRate, gatt.getDevice().getName(), gatt.getDevice().getAddress());
            handler.obtainMessage(MESSAGE_READ, sensorDataSet).sendToTarget();
        }
    };

    /**
     * Constructor.
     *
     * @param handler a handler for sending messages back to the UI activity
     */
    BluetoothConnectionManager(@NonNull Context context, @NonNull BluetoothDevice bluetoothDevice, @NonNull Handler handler) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.handler = handler;
        this.sensorState = SensorState.NONE;
    }

    public synchronized void connect() {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connected; ignoring.");
        }

        Log.d(TAG, "Connecting to: " + bluetoothDevice);

        setState(SensorState.CONNECTING);
        bluetoothGatt = bluetoothDevice.connectGatt(this.context, true, this.connectCallback);
    }

    public synchronized void disconnect() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "Cannot disconnect if not connected.");
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public synchronized boolean isSameBluetoothDevice(BluetoothDevice bluetoothDevice) {
        return this.bluetoothDevice.equals(bluetoothDevice);
    }

    synchronized SensorState getSensorState() {
        return sensorState;
    }

    private synchronized void setState(SensorState sensorState) {
        this.sensorState = sensorState;
    }
}
