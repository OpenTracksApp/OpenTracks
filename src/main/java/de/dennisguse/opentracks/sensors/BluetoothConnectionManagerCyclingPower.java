package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import java.util.List;

import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;

public class BluetoothConnectionManagerCyclingPower extends AbstractBluetoothConnectionManager<Power> {

    BluetoothConnectionManagerCyclingPower(@NonNull SensorManager.SensorDataChangedObserver observer) {
        super(List.of(BluetoothUtils.CYCLING_POWER), observer);
    }

    @Override
    protected SensorDataCyclingPower createEmptySensorData(String address) {
        return new SensorDataCyclingPower(address);
    }

    @Override
    protected SensorDataCyclingPower parsePayload(@NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingPower.Data cyclingPower = BluetoothUtils.parseCyclingPower(address, sensorName, characteristic);

        return cyclingPower != null ? cyclingPower.power() : null;
    }
}
