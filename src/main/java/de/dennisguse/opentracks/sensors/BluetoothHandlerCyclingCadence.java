package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.util.Pair;

import java.util.List;

import de.dennisguse.opentracks.sensors.sensorData.AggregatorCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothHandlerCyclingCadence implements SensorHandlerInterface {
    private static final String TAG = BluetoothHandlerCyclingCadence.class.getSimpleName();

    public static final List<ServiceMeasurementUUID> CYCLING_CADENCE = List.of(
            BluetoothHandlerManagerCyclingPower.CYCLING_POWER,
            BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return CYCLING_CADENCE;
    }

    @Override
    public AggregatorCyclingCadence createEmptySensorData(String address, String name) {
        return new AggregatorCyclingCadence(address, name);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        if (serviceMeasurementUUID.equals(BluetoothHandlerManagerCyclingPower.CYCLING_POWER)) {
            BluetoothHandlerManagerCyclingPower.Data data = BluetoothHandlerManagerCyclingPower.parseCyclingPower(characteristic);
            if (data != null && data.crank() != null) {
                observer.onChange(new Raw<>(observer.getNow(), data.crank()));
            }
            return;
        }

        if (serviceMeasurementUUID.equals(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE)) {
            Pair<BluetoothHandlerCyclingDistanceSpeed.WheelData, CrankData> data = BluetoothHandlerCyclingDistanceSpeed.parseCyclingCrankAndWheel(address, sensorName, characteristic);

            if (data != null && data.second != null) {
                observer.onChange(new Raw<>(observer.getNow(), data.second));
            }
            return;
        }

        Log.e(TAG, "Don't know how to decode this payload.");
    }

    public record CrankData(
            long crankRevolutionsCount, // UINT32
            int crankRevolutionsTime // UINT16; 1/1024s
     ) {}
}
