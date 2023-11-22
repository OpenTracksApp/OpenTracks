package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;

public class BluetoothHandlerRunningSpeedAndCadenceTest {

    @Test
    public void parseRunningSpeedAndCadence_with_distance() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerRunningSpeedAndCadence.RUNNING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{2, 0, 5, 80, (byte) 0xFF, (byte) 0xFF, 0, 1});

        // when
        BluetoothHandlerRunningSpeedAndCadence.Data sensor = BluetoothHandlerRunningSpeedAndCadence.parseRunningSpeedAndCadence("sensorName", characteristic);

        // then
        assertEquals(Speed.of(5), sensor.speed());
        assertEquals(Cadence.of(80), sensor.cadence());
        assertEquals(Distance.of(6553.5 + 1677721.6), sensor.totalDistance());
    }
}