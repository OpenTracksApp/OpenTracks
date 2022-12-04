package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;

public class BluetoothUtilsTest {

    @Test
    public void parseHeartRate_uint8() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEARTRATE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, 0x3C});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        assertEquals(60, heartRate);
    }

    @Test
    public void parseHeartRate_uint16() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.HEARTRATE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, 0x01, 0x01});

        // when
        int heartRate = BluetoothUtils.parseHeartRate(characteristic);

        // then
        assertEquals(257, heartRate);
    }

    @Test
    public void parsePressure_Pa() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.PRESSURE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{(byte) 0xB2, (byte) 0x48, (byte) 0x0F, (byte) 0x00});


        // when
        int pressure = BluetoothUtils.parsePressure(characteristic);

        // then
        assertEquals(1001650, pressure);
    }

    @Test
    public void parseCyclingSpeedCadence_crankOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertNull(sensor.getDistanceSpeed());
        assertEquals(200, sensor.getCadence().getCrankRevolutionsCount());
    }

    @Test
    public void parseCyclingSpeedCadence_wheelOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xFF, (byte) 0xFF, 0, 1, 0x45, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(65535 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertNull(sensor.getCadence());
    }

    @Test
    public void parseCyclingSpeedCadence_crankWheel() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_SPEED_CADENCE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x03, (byte) 0xC8, 0x00, 0x00, 0x01, 0x06, (byte) 0x99, (byte) 0xE1, 0x00, 0x45, (byte) 0x99});

        // when
        SensorDataCyclingCadenceAndDistanceSpeed sensor = BluetoothUtils.parseCyclingCrankAndWheel("address", "sensorName", characteristic);

        // then
        assertEquals(200 + 16777216, sensor.getDistanceSpeed().getWheelRevolutionsCount());
        assertEquals(225, sensor.getCadence().getCrankRevolutionsCount());
    }

    @Test
    public void parseCyclingPower_power() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_POWER.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0, 0, 40, 0});

        // when
        SensorDataCyclingPower.Data powerCadence = BluetoothUtils.parseCyclingPower("", "", characteristic);

        // then
        assertEquals(40, powerCadence.getPower().getValue().getW(), 0.01);
    }

    @Test
    public void parseCyclingPower_power_with_cadence() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.CYCLING_POWER.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x2C, 0x00, 0x00, 0x00, (byte) 0x9F, 0x00, 0x0C, 0x00, (byte) 0xE5, 0x42});

        // when
        SensorDataCyclingPower.Data powerCadence = BluetoothUtils.parseCyclingPower("", "", characteristic);

        // then
        assertEquals(0, powerCadence.getPower().getValue().getW(), 0.01);

        assertEquals(12, powerCadence.getCadence().getCrankRevolutionsCount());
        assertEquals(17125, powerCadence.getCadence().getCrankRevolutionsTime());
    }

    @Test
    public void parseRunningSpeedAndCadence_with_distance() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothUtils.RUNNING_SPEED_CADENCE.getServiceUUID(), 0, 0);
        characteristic.setValue(new byte[]{2, 0, 5, 80, (byte) 0xFF, (byte) 0xFF, 0, 1});

        // when
        SensorDataRunning sensor = BluetoothUtils.parseRunningSpeedAndCadence("address", "sensorName", characteristic);

        // then
        assertEquals(Speed.of(5), sensor.getSpeed());
        assertEquals(Cadence.of(80), sensor.getCadence());
        assertEquals(Distance.of(6553.5 + 1677721.6), sensor.getTotalDistance());
    }
}