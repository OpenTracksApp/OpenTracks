package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataHeartRate;

public class BluetoothRemoteSensorManagerHeartRate extends BluetoothConnectionManager<HeartRate> {

    BluetoothRemoteSensorManagerHeartRate(@NonNull SensorDataObserver observer) {
        super(BluetoothUtils.HEARTRATE, observer);
    }

    @Override
    protected SensorDataHeartRate createEmptySensorData(String address) {
        return new SensorDataHeartRate(address);
    }

    @Override
    protected SensorDataHeartRate parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        Integer heartRate = BluetoothUtils.parseHeartRate(characteristic);

        return heartRate != null ? new SensorDataHeartRate(address, sensorName, HeartRate.of(heartRate)) : null;
    }
}
