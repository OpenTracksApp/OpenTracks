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
package de.dennisguse.opentracks.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utilities for dealing with bluetooth devices.
 *
 * @author Rodrigo Damazio
 */
public class BluetoothUtils {

    private BluetoothUtils() {
    }

    public static BluetoothAdapter getDefaultBluetoothAdapter(final String TAG) {
        // If from the main application thread, return directly
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            return BluetoothAdapter.getDefaultAdapter();
        }

        // Get the default adapter from the main application thread
        final ArrayList<BluetoothAdapter> adapters = new ArrayList<>(1);
        final Object mutex = new Object();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapters.add(BluetoothAdapter.getDefaultAdapter());
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        while (adapters.isEmpty()) {
            synchronized (mutex) {
                try {
                    mutex.wait(UnitConversions.ONE_SECOND);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for default bluetooth adapter", e);
                }
            }
        }

        if (adapters.get(0) == null) {
            Log.w(TAG, "No bluetooth adapter found.");
        }
        return adapters.get(0);
    }

    /**
     * Populates the device names and the device addresses with all the suitable
     * bluetooth devices.
     *
     * @param bluetoothAdapter the bluetooth adapter
     * @param deviceNames      list of device names
     * @param deviceAddresses  list of device addresses
     */
    public static void populateDeviceLists(BluetoothAdapter bluetoothAdapter, List<String> deviceNames, List<String> deviceAddresses) {
        // Ensure the bluetooth adapter is not in discovery mode.
        bluetoothAdapter.cancelDiscovery();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            BluetoothClass bluetoothClass = device.getBluetoothClass();
            if (bluetoothClass != null && (bluetoothClass.getDeviceClass() & BluetoothClass.Device.HEALTH_PULSE_RATE) != 0) {
                deviceAddresses.add(device.getAddress());
                deviceNames.add(device.getName());
            }
        }
    }

    public static int parseHeartRate(BluetoothGattCharacteristic characteristic) {
        //DOCUMENTATION https://www.bluetooth.com/specifications/gatt/characteristics/
        byte[] raw = characteristic.getValue();
        int index = ((raw[0] & 0x1) == 1) ? 2 : 1;
        int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;
        return characteristic.getIntValue(format, index);
    }

}
