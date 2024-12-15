package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.AggregatorCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothHandlerCyclingDistanceSpeed implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID CYCLING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181600001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5B00001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(CYCLING_SPEED_CADENCE);
    }

    @Override
    public AggregatorCyclingDistanceSpeed createEmptySensorData(String address, String name) {
        return new AggregatorCyclingDistanceSpeed(address, name);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        Pair<WheelData, BluetoothHandlerCyclingCadence.CrankData> data = parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (data.first != null) {
            observer.onChange(new Raw<>(observer.getNow(), data.first));
        }
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static Pair<WheelData, BluetoothHandlerCyclingCadence.CrankData> parseCyclingCrankAndWheel(String address, String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.csc_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasWheel = (flags & 0x01) > 0;
        boolean hasCrank = (flags & 0x02) > 0;

        int index = 1;
        WheelData wheelData = null;
        if (hasWheel && valueLength - index >= 6) {
            long wheelTotalRevolutionCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index);
            index += 4;
            int wheelTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            wheelData = new WheelData(wheelTotalRevolutionCount, wheelTime);
            index += 2;
        }

        BluetoothHandlerCyclingCadence.CrankData crankData = null;
        if (hasCrank && valueLength - index >= 4) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            crankData = new BluetoothHandlerCyclingCadence.CrankData(crankCount, crankTime);
        }

        return new Pair<>(wheelData, crankData);
    }

    public record WheelData(

            long wheelRevolutionsCount, // UINT32

            int wheelRevolutionsTime // UINT16; 1/1024s
    ) {}
}
