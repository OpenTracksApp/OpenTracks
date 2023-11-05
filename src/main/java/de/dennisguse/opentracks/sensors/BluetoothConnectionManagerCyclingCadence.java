package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;

public class BluetoothConnectionManagerCyclingCadence extends AbstractBluetoothConnectionManager<Cadence> {

    private static final String TAG = BluetoothConnectionManagerCyclingCadence.class.getSimpleName();

    BluetoothConnectionManagerCyclingCadence(SensorManager.SensorDataChangedObserver observer) {
        super(BluetoothUtils.CYCLING_CADENCE, observer);
    }

    @Override
    protected SensorDataCyclingCadence createEmptySensorData(String address) {
        return new SensorDataCyclingCadence(address);
    }

    @Override
    protected SensorDataCyclingCadence parsePayload(ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {

        //TODO Implement to ServiceMeasurement.parse()?
        if (serviceMeasurementUUID.equals(BluetoothUtils.CYCLING_POWER)) {
            SensorDataCyclingPower.Data data = BluetoothUtils.parseCyclingPower(address, sensorName, characteristic);
            if (data!= null) {
                return data.cadence();
            }
        } else if (serviceMeasurementUUID.equals(BluetoothUtils.CYCLING_SPEED_CADENCE)) {
            SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
            if (cadenceAndSpeed == null) {
                return null;
            }

            if (cadenceAndSpeed.getCadence() != null) {
                return cadenceAndSpeed.getCadence();
            }
        }

        Log.e(TAG, "Don't know how to decode this payload.");
        return null;
    }
}
