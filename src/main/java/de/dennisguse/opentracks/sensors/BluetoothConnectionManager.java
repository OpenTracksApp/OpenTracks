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

package de.dennisguse.opentracks.sensors;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Optional;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.driver.Driver;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

/**
 * Manages connection to a Bluetooth LE sensor and subscribes for onChange-notifications.
 */
@SuppressLint("MissingPermission")
public class BluetoothConnectionManager implements Driver {

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    private final SensorManager.SensorDataChangedObserver observer;

    private final SensorHandlerInterface sensorHandler;

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothGattCallback connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING ->
                        Log.i(TAG, gatt.getDevice() + ": connecting to sensor");
                case BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, gatt.getDevice() + ": connected to sensor; discovering services");
                    gatt.discoverServices();
                }
                case BluetoothProfile.STATE_DISCONNECTING ->
                        Log.i(TAG, gatt.getDevice() + ": disconnecting from sensor: ");
                case BluetoothProfile.STATE_DISCONNECTED -> {
                    //This is also triggered, if no connection was established (ca. 30s)
                    Log.i(TAG, gatt.getDevice() + ": disconnected from sensor: trying to reconnect");
                    if (gatt.connect()) {
                        Log.e(TAG, gatt.getDevice() + ": could not trigger reconnect for sensor");
                    }
                    clearData();
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattService gattService = null;
            ServiceMeasurementUUID serviceMeasurement = null;
            for (ServiceMeasurementUUID s : sensorHandler.getServices()) {
                gattService = gatt.getService(s.serviceUUID());
                if (gattService != null) {
                    serviceMeasurement = s;
                    break;
                }
            }

            if (gattService == null) {
                Log.e(TAG, gatt.getDevice() + ": could not get gattService for serviceUUID=" + serviceMeasurement);
                return;
            }

            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(serviceMeasurement.measurementUUID());
            if (characteristic == null) {
                Log.e(TAG, gatt.getDevice() + ": could not get BluetoothCharacteristic for serviceUUID=" + serviceMeasurement.serviceUUID() + " characteristicUUID=" + serviceMeasurement.measurementUUID());
                return;
            }
            gatt.setCharacteristicNotification(characteristic, true);

            // Register for updates.
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor == null) {
                Log.e(TAG, "CLIENT_CHARACTERISTIC_CONFIG_UUID characteristic not available; cannot request notifications for changed data.");
                return;
            }

            if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                Log.e(TAG, "CLIENT_CHARACTERISTIC_CONFIG_UUID could not be set to ENABLE_NOTIFICATION_VALUE");
            }
            if (!gatt.writeDescriptor(descriptor)) {
                Log.e(TAG, "CLIENT_CHARACTERISTIC_CONFIG_UUID descriptor could not be written");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            UUID serviceUUID = characteristic.getService().getUuid();
            BluetoothDevice device = gatt.getDevice();
            Log.d(TAG, device + ": Received data with service " + serviceUUID + " and characteristics " + characteristic.getUuid());
            Optional<ServiceMeasurementUUID> serviceMeasurementUUID = sensorHandler.getServices()
                    .stream()
                    .filter(s -> s.serviceUUID().equals(characteristic.getService().getUuid()))
                    .findFirst();
            if (serviceMeasurementUUID.isEmpty()) {
                Log.e(TAG, device + ": Unknown service UUID; not supported?");
                return;
            }

            sensorHandler.handlePayload(observer, serviceMeasurementUUID.get(), gatt.getDevice().getName(), gatt.getDevice().getAddress(), characteristic);
        }
    };

    BluetoothConnectionManager(BluetoothAdapter bluetoothAdapter, SensorManager.SensorDataChangedObserver observer, SensorHandlerInterface sensorHandler) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.observer = observer;
        this.sensorHandler = sensorHandler;
    }

    @Override
    public synchronized void connect(Context context, Handler handler, @NonNull String address) {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled.");
            return;
        }

        if (SensorType.NONE.getPreferenceValue().equals(address)) {
            Log.w(TAG, "NONE: going to disconnect");
            if (isConnected()) {
                disconnect();
                observer.onRemove(sensorHandler.createEmptySensorData(null, null));
            }
            return;
        }

        if (isConnected()) {
            Log.w(TAG, "Already connected; ignoring.");
            return;
        }

        if (isSameBluetoothDevice(address)) {
            return;
        } else {
            disconnect();
        }

        BluetoothDevice device;
        try {
            device = bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, address + ": Unable to get remote device for", e);
            return;
        }

        Log.d(TAG, device + ": trying to connect");

        bluetoothGatt = device.connectGatt(context, false, connectCallback, BluetoothDevice.TRANSPORT_AUTO, 0, handler);

        observer.onConnect(sensorHandler.createEmptySensorData(device.getAddress(), device.getName()));
    }

    private synchronized void clearData() {
        observer.onDisconnect(sensorHandler.createEmptySensorData(bluetoothGatt.getDevice().getAddress(), bluetoothGatt.getDevice().getName()));
    }

    @Override
    public synchronized void disconnect() {
        if (!isConnected()) {
            Log.w(TAG, "Not connected; no need to re-connect.");
            return;
        }
        Log.i(TAG, bluetoothGatt.getDevice() + ": start disconnect");
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        clearData();
        Log.i(TAG, bluetoothGatt.getDevice() + ": disconnect finished");
        bluetoothGatt = null;
    }

    private synchronized boolean isSameBluetoothDevice(String address) {
        if (bluetoothGatt == null) {
            return false;
        }

        return address.equals(bluetoothGatt.getDevice().getAddress());
    }


    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @Override
    public boolean isConnected() {
        return bluetoothGatt != null;
    }
}
