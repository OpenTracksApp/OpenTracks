package de.dennisguse.opentracks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataRunning;

public class BluetoothUtilsTest {

    @Test
    public void parseHeartRate_uint8() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEART_RATE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x02, 0x3C});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        assertEquals(60, heartRate);
    }

    @Test
    public void parseHeartRate_uint16() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEART_RATE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x01, 0x01, 0x01});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        assertEquals(257, heartRate);
    }

    @Test
    public void parseCyclingSpeedCadence_crankOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
        assertNull(sensor.getDistanceSpeed());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertNull(sensor.getCadence());
        assertEquals(225, sensor.getDistanceSpeed().getWheelRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
        assertEquals(225, sensor.getDistanceSpeed().getWheelRevolutionsCount());
    }

    @Test
    public void parseCyclingPower_power() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_POWER_MEASUREMENT_CHAR_UUID, 0, 0);
        characteristic.setValue(new byte[]{0, 0, 40, 0});

        // when
        int power_w = BluetoothUtils.parseCyclingPower(characteristic);

        // then
        assertEquals(40, power_w);
    }

    @Test
    public void parseRunningSpeedAndCadence_with_distance() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_MEASUREMENT_CHAR_UUID, 0, 0);
        characteristic.setValue(new byte[]{2, 0, 5, 80, 12, 1});

        // when
        SensorDataRunning sensor = BluetoothUtils.parseRunningSpeedAndCadence("address", "sensorName", characteristic);

        // then
        assertEquals(Speed.of(5), sensor.getSpeed());
        assertEquals(80, sensor.getCadence(), 0.01);
        assertEquals(Distance.of(26.8), sensor.getTotalDistance());
    }
}