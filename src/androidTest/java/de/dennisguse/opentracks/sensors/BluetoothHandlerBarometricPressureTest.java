package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;

public class BluetoothHandlerBarometricPressureTest {

    @Test
    public void parseEnvironmentalSensing_Pa() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{(byte) 0xB2, (byte) 0x48, (byte) 0x0F, (byte) 0x00});

        // when
        AtmosphericPressure pressure = BluetoothHandlerBarometricPressure.parseEnvironmentalSensing(characteristic);

        // then
        assertEquals(AtmosphericPressure.ofHPA(1001.65f), pressure);
    }
}