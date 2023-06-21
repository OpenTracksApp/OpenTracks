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

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.data.models.BatteryLevel;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;

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

    public static final ServiceMeasurementUUID HEARTRATE = new ServiceMeasurementUUID(
            new UUID(0x180D00001000L, 0x800000805f9b34fbL),
            new UUID(0x2A3700001000L, 0x800000805f9b34fbL)
    );

    // Used for device discovery in preferences
    public static final List<ServiceMeasurementUUID> HEART_RATE_SUPPORTING_DEVICES = List.of(
            HEARTRATE,
            //Devices that support HEART_RATE_SERVICE_UUID, but do not announce HEART_RATE_SERVICE_UUID in there BLE announcement messages (during device discovery).
            new ServiceMeasurementUUID(
                    UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"), //Miband3
                    HEARTRATE.measurementUUID()
            )
    );

    private static final UUID ENVIRONMENTAL_SENSING_SERVICE = new UUID(0x181A00001000L, 0x800000805f9b34fbL);
    public static final ServiceMeasurementUUID BAROMETRIC_PRESSURE = new ServiceMeasurementUUID(
            ENVIRONMENTAL_SENSING_SERVICE,
            new UUID(0x2A6D00001000L, 0x800000805f9b34fbL)
    );

    public static final ServiceMeasurementUUID CYCLING_POWER = new ServiceMeasurementUUID(
            new UUID(0x181800001000L, 0x800000805f9b34fbL),
            new UUID(0x2A6300001000L, 0x800000805f9b34fbL)
    );

    public static final ServiceMeasurementUUID CYCLING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181600001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5B00001000L, 0x800000805f9b34fbL)
    );

    public static final List<ServiceMeasurementUUID> CYCLING_CADENCE = List.of(
            CYCLING_POWER,
            CYCLING_SPEED_CADENCE
    );

    public static final ServiceMeasurementUUID RUNNING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181400001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5300001000L, 0x800000805f9b34fbL)
    );

    private static final String TAG = BluetoothUtils.class.getSimpleName();

    private BluetoothUtils() {
    }

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

    public static HeartRate parseHeartRate(BluetoothGattCharacteristic characteristic) {
        //DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.heart_rate_measurement.xml
        byte[] raw = characteristic.getValue();
        if (raw.length == 0) {
            return null;
        }

        boolean formatUINT16 = ((raw[0] & 0x1) == 1);
        if (formatUINT16 && raw.length >= 3) {
            return HeartRate.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1));
        }
        if (!formatUINT16 && raw.length >= 2) {
            return HeartRate.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
        }

        return null;
    }

    public static AtmosphericPressure parseEnvironmentalSensing(BluetoothGattCharacteristic characteristic) {
        byte[] raw = characteristic.getValue();

        if (raw.length < 4) {
            return null;
        }

        Integer pressure = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        return AtmosphericPressure.ofPA(pressure / 10f);
    }

    public static SensorDataCyclingPower.Data parseCyclingPower(String address, String sensorName, BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.cycling_power_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int index = 0;
        int flags1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index++);
        int flags2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index++);
        boolean hasPedalPowerBalance = (flags1 & 0x01) > 0;
        boolean hasAccumulatedTorque = (flags1 & 0x04) > 0;
        boolean hasWheel = (flags1 & 16) > 0;
        boolean hasCrank = (flags1 & 32) > 0;

        Integer instantaneousPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, index);
        index += 2;

        if (hasPedalPowerBalance) {
            index += 1;
        }
        if (hasAccumulatedTorque) {
            index += 2;
        }
        if (hasWheel) {
            index += 2 + 2;
        }

        SensorDataCyclingCadence cadence = null;
        if (hasCrank && valueLength - index >= 4) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s

            cadence = new SensorDataCyclingCadence(address, sensorName, crankCount, crankTime);
        }


        return new SensorDataCyclingPower.Data(new SensorDataCyclingPower(sensorName, address, Power.of(instantaneousPower)), cadence);
    }

    public static SensorDataCyclingCadenceAndDistanceSpeed parseCyclingCrankAndWheel(String address, String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.csc_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasWheel = (flags & 0x01) > 0;
        boolean hasCrank = (flags & 0x02) > 0;

        int index = 1;
        SensorDataCyclingDistanceSpeed speed = null;
        if (hasWheel && valueLength - index >= 6) {
            int wheelTotalRevolutionCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index);
            index += 4;
            int wheelTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            speed = new SensorDataCyclingDistanceSpeed(address, sensorName, wheelTotalRevolutionCount, wheelTime);
            index += 2;
        }

        SensorDataCyclingCadence cadence = null;
        if (hasCrank && valueLength - index >= 4) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            cadence = new SensorDataCyclingCadence(address, sensorName, crankCount, crankTime);
        }

        return new SensorDataCyclingCadenceAndDistanceSpeed(address, sensorName, cadence, speed);
    }

    public static SensorDataRunning parseRunningSpeedAndCadence(String address, String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.rsc_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasStrideLength = (flags & 0x01) > 0;
        boolean hasTotalDistance = (flags & 0x02) > 0;
        boolean hasStatus = (flags & 0x03) > 0; // walking vs running

        Speed speed = null;
        Cadence cadence = null;
        Distance totalDistance = null;

        int index = 1;
        if (valueLength - index >= 2) {
            speed = Speed.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index) / 256f);
        }

        index = 3;
        if (valueLength - index >= 1) {
            cadence = Cadence.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index));

            // Hacky workaround as the Wahoo Tickr X provides cadence in SPM (steps per minute) in violation to the standard.
            if (sensorName != null && sensorName.startsWith("TICKR X")) {
                cadence = Cadence.of(cadence.getRPM() / 2);
            }
        }

        index = 4;
        if (hasStrideLength && valueLength - index >= 2) {
            Distance strideDistance = Distance.ofCM(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index));
            index += 2;
        }

        if (hasTotalDistance && valueLength - index >= 4) {
            totalDistance = Distance.ofDM(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index));
        }

        return new SensorDataRunning(address, sensorName, speed, cadence, totalDistance);
    }
}
