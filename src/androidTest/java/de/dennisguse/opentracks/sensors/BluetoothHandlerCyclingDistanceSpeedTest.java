package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;

public class BluetoothHandlerCyclingDistanceSpeedTest {
    @Test
    public void parseCyclingSpeedCadence_crankOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertNull(sensor.getDistanceSpeed());
        assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xFF, (byte) 0xFF, 0, 1, 0x45, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(65535 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertNull(sensor.getCadence());
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x01, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertEquals(225, sensor.getCadence().getCrankRevolutionsCount());
    }

}