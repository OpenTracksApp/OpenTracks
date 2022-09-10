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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorData;

/**
 * Manages connection to a Bluetooth LE sensor and subscribes for onChange-notifications.
 * Also parses the transferred data into {@link SensorDataObserver}.
 */
public abstract class BluetoothConnectionManager<DataType> {

    private static final String TAG = BluetoothConnectionManager.class.getSimpleName();

    private final SensorDataObserver observer;

    private final List<ServiceMeasurementUUID> serviceMeasurementUUIDs;
    private BluetoothGatt bluetoothGatt;

    private final BluetoothGattCallback connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "Connecting to sensor: " + gatt.getDevice());
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "Connected to sensor: " + gatt.getDevice());

                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "Disconnecting from sensor: " + gatt.getDevice());
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "Disconnected from sensor: " + gatt.getDevice());

                    clearData();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattService gattService = null;
            ServiceMeasurementUUID serviceMeasurement = null;
            for (ServiceMeasurementUUID s : serviceMeasurementUUIDs) {
                gattService = gatt.getService(s.getServiceUUID());
                if (gattService != null) {
                    serviceMeasurement = s;
                    break;
                }
            }

            if (gattService == null) {
                Log.e(TAG, "Could not get gattService for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceMeasurement);
                return;
            }

            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(serviceMeasurement.getMeasurementUUID());
            if (characteristic == null) {
                Log.e(TAG, "Could not get BluetoothCharacteristic for address=" + gatt.getDevice().getAddress() + " serviceUUID=" + serviceMeasurement.getServiceUUID() + " characteristicUUID=" + serviceMeasurement.getMeasurementUUID());
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
                    .filter(s -> s.getServiceUUID().equals(characteristic.getService().getUuid())).findFirst();
            if (serviceMeasurementUUID.isEmpty()) {
                Log.e(TAG, "Unknown service UUID; not supported?");
                return;
            }

            SensorData<DataType> sensorData = parsePayload(serviceMeasurementUUID.get(), gatt.getDevice().getName(), gatt.getDevice().getAddress(), characteristic);
            if (sensorData != null) {
                Log.d(TAG, "Decoded data from " + gatt.getDevice().getAddress() + ": " + sensorData);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    observer.onChanged(sensorData);
                } else {
                    //TODO This might lead to NPEs in case of race conditions due to shutdown.
                    observer.getHandler().post(() -> observer.onChanged(sensorData));
                }
            }
        }
    };

    BluetoothConnectionManager(ServiceMeasurementUUID serviceUUUID, SensorDataObserver observer) {
        this.serviceMeasurementUUIDs = List.of(serviceUUUID);
        this.observer = observer;
    }

    BluetoothConnectionManager(List<ServiceMeasurementUUID> serviceUUUID, SensorDataObserver observer) {
        this.serviceMeasurementUUIDs = serviceUUUID;
        this.observer = observer;
    }

    synchronized void connect(Context context, @NonNull BluetoothDevice device) {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connected; ignoring.");
            return;
        }

        Log.d(TAG, "Connecting to: " + device);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothGatt = device.connectGatt(context, false, connectCallback, BluetoothDevice.TRANSPORT_AUTO, 0, this.observer.getHandler());
        } else {
            bluetoothGatt = device.connectGatt(context, false, connectCallback);
        }
        SensorData<?> sensorData = createEmptySensorData(bluetoothGatt.getDevice().getAddress());
        observer.onChanged(sensorData);
    }

    private synchronized void clearData() {
        observer.onDisconnecting(createEmptySensorData(bluetoothGatt.getDevice().getAddress()));
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

    interface SensorDataObserver {

        void onChanged(SensorData<?> sensorData);

        void onDisconnecting(SensorData<?> sensorData);

        @NonNull
        Handler getHandler();
    }
}
