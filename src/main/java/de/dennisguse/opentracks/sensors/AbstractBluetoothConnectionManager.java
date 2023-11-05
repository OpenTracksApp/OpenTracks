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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorData;

/**
 * Manages connection to a Bluetooth LE sensor and subscribes for onChange-notifications.
 */
@SuppressLint("MissingPermission")
public abstract class AbstractBluetoothConnectionManager<DataType> {

    private static final String TAG = AbstractBluetoothConnectionManager.class.getSimpleName();

    private final SensorManager.SensorDataChangedObserver observer;

    private final List<ServiceMeasurementUUID> serviceMeasurementUUIDs;
    private BluetoothGatt bluetoothGatt;

    private final BluetoothGattCallback connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING ->
                        Log.i(TAG, "Connecting to sensor: " + gatt.getDevice());
                case BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to sensor: " + gatt.getDevice() + "; discovering services.");
                    gatt.discoverServices();
                }
                case BluetoothProfile.STATE_DISCONNECTING ->
                        Log.i(TAG, "Disconnecting from sensor: " + gatt.getDevice());
                case BluetoothProfile.STATE_DISCONNECTED -> {
                    //This is also triggered, if no connection was established (ca. 30s)
                    Log.i(TAG, "Disconnected from sensor: " + gatt.getDevice() + "; trying to reconnect");
                    if (gatt.connect()) {
                        Log.e(TAG, "Could not trigger reconnect for sensor: " + gatt.getDevice());
                    }
                    clearData();
                }
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattService gattService = null;
            ServiceMeasurementUUID serviceMeasurement = null;
            for (ServiceMeasurementUUID s : serviceMeasurementUUIDs) {
                gattService = gatt.getService(s.serviceUUID());
                if (gattService != null) {
                    serviceMeasurement = s;
                    break;
                }
            }

            if (gattService == null) {
                Log.e(TAG, "Could not get gattService for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceMeasurement);
                return;
            }

            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(serviceMeasurement.measurementUUID());
            if (characteristic == null) {
                Log.e(TAG, "Could not get BluetoothCharacteristic for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceMeasurement.serviceUUID() + " characteristicUUID=" + serviceMeasurement.measurementUUID());
                return;
            }
            gatt.setCharacteristicNotification(characteristic, true);

            // Register for updates.
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor == null) {
                Log.e(TAG, "CLIENT_CHARACTERISTIC_CONFIG_UUID characteristic not available; cannot request notifications for changed data.");
                return;
            }

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            UUID serviceUUID = characteristic.getService().getUuid();
            Log.d(TAG, "Received data from " + gatt.getDevice().getAddress() + " with service " + serviceUUID + " and characteristics " + characteristic.getUuid());
            Optional<ServiceMeasurementUUID> serviceMeasurementUUID = serviceMeasurementUUIDs.stream()
                    .filter(s -> s.serviceUUID().equals(characteristic.getService().getUuid())).findFirst();
            if (serviceMeasurementUUID.isEmpty()) {
                Log.e(TAG, "Unknown service UUID; not supported?");
                return;
            }

            SensorData<DataType> sensorData = parsePayload(serviceMeasurementUUID.get(), gatt.getDevice().getName(), gatt.getDevice().getAddress(), characteristic);
            if (sensorData != null) {
                Log.d(TAG, "Decoded data from " + gatt.getDevice().getAddress() + ": " + sensorData);
                observer.onChange(sensorData);
            }
        }
    };

    AbstractBluetoothConnectionManager(List<ServiceMeasurementUUID> serviceUUUID, SensorManager.SensorDataChangedObserver observer) {
        this.serviceMeasurementUUIDs = serviceUUUID;
        this.observer = observer;
    }

    synchronized void connect(Context context, Handler handler, @NonNull BluetoothDevice device) {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connected; ignoring.");
            return;
        }

        Log.d(TAG, "Connecting to: " + device);

        bluetoothGatt = device.connectGatt(context, false, connectCallback, BluetoothDevice.TRANSPORT_AUTO, 0, handler);

        SensorData<?> sensorData = createEmptySensorData(bluetoothGatt.getDevice().getAddress());
        observer.onChange(sensorData);
    }

    private synchronized void clearData() {
        observer.onDisconnect(createEmptySensorData(bluetoothGatt.getDevice().getAddress()));
    }

    synchronized void disconnect() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "Cannot disconnect if not connected.");
            return;
        }
        bluetoothGatt.close();
        clearData();
        bluetoothGatt = null;
    }

    synchronized boolean isSameBluetoothDevice(String address) {
        if (bluetoothGatt == null) {
            return false;
        }

        return address.equals(bluetoothGatt.getDevice().getAddress());
    }

    protected abstract SensorData<DataType> createEmptySensorData(String address);

    /**
     * @return null if data could not be parsed.
     */
    protected abstract SensorData<DataType> parsePayload(@NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, @NonNull BluetoothGattCharacteristic characteristic);
}
