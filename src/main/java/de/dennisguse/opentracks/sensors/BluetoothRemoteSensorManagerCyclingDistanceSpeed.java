package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCycling;

public class BluetoothRemoteSensorManagerCyclingDistanceSpeed extends BluetoothConnectionManager<SensorDataCycling.DistanceSpeed.Data> {

    BluetoothRemoteSensorManagerCyclingDistanceSpeed(SensorDataObserver observer) {
        super(BluetoothUtils.CYCLING_SPEED_CADENCE, observer);
    }

    @Override
    protected SensorDataCycling.DistanceSpeed createEmptySensorData(String address) {
        return new SensorDataCycling.DistanceSpeed(address);
    }

    @Override
    protected SensorDataCycling.DistanceSpeed parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCycling.CadenceAndSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return null;
        }

        if (cadenceAndSpeed.getDistanceSpeed() != null) {
            return cadenceAndSpeed.getDistanceSpeed();
        }

        return null;
    }
}
