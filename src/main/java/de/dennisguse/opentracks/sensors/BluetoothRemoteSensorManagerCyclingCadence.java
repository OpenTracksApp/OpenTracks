package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;

public class BluetoothRemoteSensorManagerCyclingCadence extends BluetoothConnectionManager<Cadence> {

    BluetoothRemoteSensorManagerCyclingCadence(SensorDataObserver observer) {
        super(BluetoothUtils.CYCLING_SPEED_CADENCE, observer);
    }

    @Override
    protected SensorDataCyclingCadence createEmptySensorData(String address) {
        return new SensorDataCyclingCadence(address);
    }

    @Override
    protected SensorDataCyclingCadence parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return null;
        }

        if (cadenceAndSpeed.getCadence() != null) {
            return cadenceAndSpeed.getCadence();
        }

        return null;
    }
}
