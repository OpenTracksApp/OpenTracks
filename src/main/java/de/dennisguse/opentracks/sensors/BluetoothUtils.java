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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import de.dennisguse.opentracks.data.models.BatteryLevel;

/**
 * Utilities for dealing with bluetooth devices.
 *
 * @author Rodrigo Damazio
 */
public class BluetoothUtils {

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = new UUID(0x290200001000L, 0x800000805f9b34fbL);

    public static final ServiceMeasurementUUID BATTERY = new ServiceMeasurementUUID(
            new UUID(0x180F00001000L, 0x800000805f9b34fbL),
            new UUID(0x2A1900001000L, 0x800000805f9b34fbL)
    );


    private static final String TAG = BluetoothUtils.class.getSimpleName();

    public static BluetoothAdapter getAdapter(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.i(TAG, "BluetoothManager not available.");
            return null;
        } else {
            return bluetoothManager.getAdapter();
        }
    }

    public static boolean hasBluetooth(Context context) {
        return BluetoothUtils.getAdapter(context) != null;
    }

    public static BatteryLevel parseBatteryLevel(BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION org.bluetooth.characteristic.battery_level.xml
        byte[] raw = characteristic.getValue();
        if (raw.length == 0) {
            return null;
        }

        final int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        return BatteryLevel.of(batteryLevel);
    }
}
