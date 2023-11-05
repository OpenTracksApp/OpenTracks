package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;

public class BluetoothConnectionManagerCyclingPowerTest {

    @Test
    public void parseCyclingPower_power() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothConnectionManagerCyclingPower.CYCLING_POWER.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0, 0, 40, 0});

        // when
        SensorDataCyclingPower.Data powerCadence = BluetoothConnectionManagerCyclingPower.parseCyclingPower("", "", characteristic);

        // then
        assertEquals(40, powerCadence.power().getValue().getW(), 0.01);
    }

    @Test
    public void parseCyclingPower_power_with_cadence() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothConnectionManagerCyclingPower.CYCLING_POWER.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x2C, 0x00, 0x00, 0x00, (byte) 0x9F, 0x00, 0x0C, 0x00, (byte) 0xE5, 0x42});

        // when
        SensorDataCyclingPower.Data powerCadence = BluetoothConnectionManagerCyclingPower.parseCyclingPower("", "", characteristic);

        // then
        assertEquals(0, powerCadence.power().getValue().getW(), 0.01);

        assertEquals(12, powerCadence.cadence().getCrankRevolutionsCount());
        assertEquals(17125, powerCadence.cadence().getCrankRevolutionsTime());
    }

}