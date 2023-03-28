package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataRunning;

public class BluetoothConnectionRunningSpeedAndCadence extends AbstractBluetoothConnectionManager<SensorDataRunning.Data> {

    BluetoothConnectionRunningSpeedAndCadence(@NonNull SensorDataObserver observer) {
        super(BluetoothUtils.RUNNING_SPEED_CADENCE, observer);
    }

    @Override
    protected SensorDataRunning createEmptySensorData(String address) {
        return new SensorDataRunning(address);
    }

    @Override
    protected SensorDataRunning parsePayload(@NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        return BluetoothUtils.parseRunningSpeedAndCadence(address, sensorName, characteristic);
    }
}
