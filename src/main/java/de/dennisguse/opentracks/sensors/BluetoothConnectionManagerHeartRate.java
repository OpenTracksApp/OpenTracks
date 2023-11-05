package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import java.util.List;

import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataHeartRate;

public class BluetoothConnectionManagerHeartRate extends AbstractBluetoothConnectionManager<HeartRate> {

    BluetoothConnectionManagerHeartRate(@NonNull SensorManager.SensorDataChangedObserver observer) {
        super(List.of(BluetoothUtils.HEARTRATE), observer);
    }

    @Override
    protected SensorDataHeartRate createEmptySensorData(String address) {
        return new SensorDataHeartRate(address);
    }

    @Override
    protected SensorDataHeartRate parsePayload(@NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        HeartRate heartRate = BluetoothUtils.parseHeartRate(characteristic);

        return heartRate != null ? new SensorDataHeartRate(address, sensorName, heartRate) : null;
    }
}
