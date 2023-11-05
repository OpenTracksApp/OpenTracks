package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionManagerCyclingPower  implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID CYCLING_POWER = new ServiceMeasurementUUID(
            new UUID(0x181800001000L, 0x800000805f9b34fbL),
            new UUID(0x2A6300001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(CYCLING_POWER);
    }

    @Override
    public SensorDataCyclingPower createEmptySensorData(String address) {
        return new SensorDataCyclingPower(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, @NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingPower.Data cyclingPower = BluetoothUtils.parseCyclingPower(address, sensorName, characteristic);

        if (cyclingPower != null) {
            observer.onChange(cyclingPower.power());
        }
    }
}
