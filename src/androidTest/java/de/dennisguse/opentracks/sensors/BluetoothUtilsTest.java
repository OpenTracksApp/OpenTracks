package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCycling;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;

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
        characteristic.setValue(new byte[]{0x02, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertNull(sensor.getDistanceSpeed());
        assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xFF, (byte) 0xFF, 0, 1, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(65535 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertNull(sensor.getCadence());
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID, 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x01, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCycling.CadenceAndSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertEquals(225, sensor.getCadence().getCrankRevolutionsCount());
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
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.RUNNING_RUNNING_SPEED_CADENCE_CHAR_UUID, 0, 0);
        characteristic.setValue(new byte[]{2, 0, 5, 80, (byte) 0xFF, (byte) 0xFF, 0, 1});

        // when
        SensorDataRunning sensor = BluetoothUtils.parseRunningSpeedAndCadence("address", "sensorName", characteristic);

        // then
        assertEquals(Speed.of(5), sensor.getSpeed());
        assertEquals(Cadence.of(80), sensor.getCadence());
        assertEquals(Distance.of(6553.5 + 1677721.6), sensor.getTotalDistance());
    }
}