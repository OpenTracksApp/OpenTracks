package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingDistanceSpeed;

public class BluetoothRemoteSensorManagerCyclingDistanceSpeed extends BluetoothConnectionManager<SensorDataCyclingDistanceSpeed.Data> {

    BluetoothRemoteSensorManagerCyclingDistanceSpeed(SensorDataObserver observer) {
        super(BluetoothUtils.CYCLING_SPEED_CADENCE, observer);
    }

    @Override
    protected SensorDataCyclingDistanceSpeed createEmptySensorData(String address) {
        return new SensorDataCyclingDistanceSpeed(address);
    }

    @Override
    protected SensorDataCyclingDistanceSpeed parsePayload(ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return null;
        }

        if (cadenceAndSpeed.getDistanceSpeed() != null) {
            return cadenceAndSpeed.getDistanceSpeed();
        }

        return null;
    }
}
