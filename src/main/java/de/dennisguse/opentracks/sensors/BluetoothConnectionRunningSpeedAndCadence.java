package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionRunningSpeedAndCadence implements SensorHandlerInterface {


    public static final ServiceMeasurementUUID RUNNING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181400001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5300001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(RUNNING_SPEED_CADENCE);
    }

    @Override
    public SensorDataRunning createEmptySensorData(String address) {
        return new SensorDataRunning(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, @NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        observer.onChange(BluetoothUtils.parseRunningSpeedAndCadence(address, sensorName, characteristic));
    }
}
