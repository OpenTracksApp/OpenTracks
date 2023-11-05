package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionManagerCyclingDistanceSpeed  implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID CYCLING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181600001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5B00001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(CYCLING_SPEED_CADENCE);
    }

    @Override
    public SensorDataCyclingDistanceSpeed createEmptySensorData(String address) {
        return new SensorDataCyclingDistanceSpeed(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return;
        }

        if (cadenceAndSpeed.getDistanceSpeed() != null) {
            observer.onChange(cadenceAndSpeed.getDistanceSpeed());
        }
    }
}
