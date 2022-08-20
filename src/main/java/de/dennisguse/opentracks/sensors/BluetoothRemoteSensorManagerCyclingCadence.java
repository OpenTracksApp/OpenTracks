package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCycling;

public class BluetoothRemoteSensorManagerCyclingCadence extends BluetoothConnectionManager<Cadence> {

    BluetoothRemoteSensorManagerCyclingCadence(SensorDataObserver observer) {
        super(BluetoothUtils.CYCLING_SPEED_CADENCE, observer);
    }

    @Override
    protected SensorDataCycling.CyclingCadence createEmptySensorData(String address) {
        return new SensorDataCycling.CyclingCadence(address);
    }

    @Override
    protected SensorDataCycling.CyclingCadence parsePayload(String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCycling.CadenceAndSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return null;
        }

        if (cadenceAndSpeed.getCadence() != null) {
            return cadenceAndSpeed.getCadence();
        }

        return null;
    }
}
