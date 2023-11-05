package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.List;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothHandlerCyclingCadence implements SensorHandlerInterface {
    private static final String TAG = BluetoothHandlerCyclingCadence.class.getSimpleName();

    public static final List<ServiceMeasurementUUID> CYCLING_CADENCE = List.of(
            BluetoothConnectionManagerCyclingPower.CYCLING_POWER,
            BluetoothConnectionManagerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return CYCLING_CADENCE;
    }

    @Override
    public SensorDataCyclingCadence createEmptySensorData(String address) {
        return new SensorDataCyclingCadence(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {

        //TODO Implement to ServiceMeasurement.parse()?
        if (serviceMeasurementUUID.equals(BluetoothConnectionManagerCyclingPower.CYCLING_POWER)) {
            SensorDataCyclingPower.Data data = BluetoothUtils.parseCyclingPower(address, sensorName, characteristic);
            if (data!= null) {
                observer.onChange(data.cadence());
            }
        } else if (serviceMeasurementUUID.equals(BluetoothConnectionManagerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE)) {
            SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = BluetoothUtils.parseCyclingCrankAndWheel(address, sensorName, characteristic);
            if (cadenceAndSpeed == null) {
                return;
            }

            if (cadenceAndSpeed.getCadence() != null) {
                observer.onChange(cadenceAndSpeed.getCadence());
            }
        }

        Log.e(TAG, "Don't know how to decode this payload.");
    }
}
