package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

public class BluetoothHandlerCyclingPowerTest {

    @Test
    public void parseCyclingPower_power() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerCyclingPower.CYCLING_POWER.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0, 0, 40, 0});

        // when
        BluetoothHandlerManagerCyclingPower.Data powerCadence = BluetoothHandlerManagerCyclingPower.parseCyclingPower(characteristic);

        // then
        assertEquals(40, powerCadence.power().getW(), 0.01);
    }

    @Test
    public void parseCyclingPower_power_with_cadence() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerCyclingPower.CYCLING_POWER.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x2C, 0x00, 0x00, 0x00, (byte) 0x9F, 0x00, 0x0C, 0x00, (byte) 0xE5, 0x42});

        // when
        BluetoothHandlerManagerCyclingPower.Data powerCadence = BluetoothHandlerManagerCyclingPower.parseCyclingPower(characteristic);

        // then
        assertEquals(0, powerCadence.power().getW(), 0.01);

        assertEquals(12, powerCadence.crank().crankRevolutionsCount());
        assertEquals(17125, powerCadence.crank().crankRevolutionsTime());
    }

}