package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;

public class BluetoothRemoteSensorManagerCyclingPower extends BluetoothConnectionManager<Power> {

    BluetoothRemoteSensorManagerCyclingPower(@NonNull SensorDataObserver observer) {
        super(BluetoothUtils.CYCLING_POWER, observer);
    }

    @Override
    protected SensorDataCyclingPower createEmptySensorData(String address) {
        return new SensorDataCyclingPower(address);
    }

    @Override
    protected SensorDataCyclingPower parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingPower.Data cyclingPower = BluetoothUtils.parseCyclingPower(characteristic);

        return cyclingPower != null ? new SensorDataCyclingPower(address, sensorName, cyclingPower.getPower()) : null;
    }
}
