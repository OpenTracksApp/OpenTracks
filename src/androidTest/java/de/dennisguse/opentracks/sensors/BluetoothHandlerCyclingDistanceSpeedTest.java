package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Pair;

import org.junit.Test;

public class BluetoothHandlerCyclingDistanceSpeedTest {
    @Test
    public void parseCyclingSpeedCadence_crankOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        Pair<BluetoothHandlerCyclingDistanceSpeed.WheelData, BluetoothHandlerCyclingCadence.CrankData> sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertNull(sensor.first);
        assertEquals(200, sensor.second.crankRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xFF, (byte) 0xFF, 0, 1, 0x45, (byte) 0x99});

        // when
        Pair<BluetoothHandlerCyclingDistanceSpeed.WheelData, BluetoothHandlerCyclingCadence.CrankData> sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(65535 + 16777216, sensor.first.wheelRevolutionsCount());
        assertNull(sensor.second);
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x01, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        Pair<BluetoothHandlerCyclingDistanceSpeed.WheelData, BluetoothHandlerCyclingCadence.CrankData> sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200 + 16777216, sensor.first.wheelRevolutionsCount());
        assertEquals(225, sensor.second.crankRevolutionsCount());
    }

}