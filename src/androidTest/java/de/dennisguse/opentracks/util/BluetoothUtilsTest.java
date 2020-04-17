package de.dennisguse.opentracks.util;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Assert;
import org.junit.Test;

import de.dennisguse.opentracks.content.sensor.SensorDataCycling;

public class BluetoothUtilsTest {

    @Test
    public void parseHeartRate_uint8() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEART_RATE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x02, 0x3C});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        Assert.assertEquals(60, heartRate);
    }

    @Test
    public void parseHeartRate_uint16() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEART_RATE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x01, 0x01, 0x01});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        Assert.assertEquals(257, heartRate);
    }

    @Test
    public void parseCyclingSpeedCadence_crankOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        Assert.assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
        Assert.assertNull(sensor.getSpeed());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        Assert.assertNull(sensor.getCadence());
        Assert.assertEquals(225, sensor.getSpeed().getWheelRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        Assert.assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
        Assert.assertEquals(225, sensor.getSpeed().getWheelRevolutionsCount());
    }
}