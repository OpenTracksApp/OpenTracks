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
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import de.dennisguse.opentracks.content.sensor.SensorDataCycling;

/**
 * Utilities for dealing with bluetooth devices.
 *
 * @author Rodrigo Damazio
 */
public class BluetoothUtils {

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = new UUID(0x290200001000L, 0x800000805f9b34fbL);

    public static final UUID HEART_RATE_SERVICE_UUID = new UUID(0x180D00001000L, 0x800000805f9b34fbL);
    public static final UUID HEART_RATE_MEASUREMENT_CHAR_UUID = new UUID(0x2A3700001000L, 0x800000805f9b34fbL);

    public static final UUID CYCLING_SPEED_CADENCE_SERVICE_UUID = new UUID(0x181600001000L, 0x800000805f9b34fbL);
    public static final UUID CYCLING_SPPED_CADENCE_MEASUREMENT_CHAR_UUID = new UUID(0x2A5B00001000L, 0x800000805f9b34fbL);

    private BluetoothUtils() {
    }

    /**
     * If called from UI: use a background thread to get the default Bluetooth adapter.
     * TODO Check if this is necessary.
     */
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
                    mutex.wait(UnitConversions.ONE_SECOND_MS);
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

    public static boolean hasBluetooth(final String TAG) {
        return BluetoothUtils.getDefaultBluetoothAdapter(TAG) != null;
    }

    public static Integer parseHeartRate(BluetoothGattCharacteristic characteristic) {
        //DOCUMENTATION https://www.bluetooth.com/specifications/gatt/characteristics/
        byte[] raw = characteristic.getValue();
        if (raw.length == 0) {
            return null;
        }

        boolean formatUINT16 = ((raw[0] & 0x1) == 1);
        if (formatUINT16 && raw.length >= 3) {
            return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
        }
        if (!formatUINT16 && raw.length >= 2) {
            return characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        }

        return null;
    }

    /**
     * Documentation: https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=261449
     */
    public static SensorDataCycling.CadenceAndSpeed parseCyclingCrankAndWheel(String address, String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasCrank = (flags & 0x01) > 0;
        boolean hasWheel = (flags & 0x02) > 0;

        SensorDataCycling.Cadence cadence = null;
        int index = 1;
        if (hasCrank && valueLength - index >= 6) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index);
            index += 4;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            index += 2;
            cadence = new SensorDataCycling.Cadence(address, sensorName, crankCount, crankTime);
        }

        SensorDataCycling.Speed speed = null;
        if (hasWheel && valueLength - index >= 4) {
            int wheelCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;
            int wheelTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            speed = new SensorDataCycling.Speed(address, sensorName, wheelCount, wheelTime);
        }

        return new SensorDataCycling.CadenceAndSpeed(address, sensorName, cadence, speed);
    }
}
