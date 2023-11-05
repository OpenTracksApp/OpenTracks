package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadenceAndDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionManagerCyclingDistanceSpeed  implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID CYCLING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181600001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5B00001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(CYCLING_SPEED_CADENCE);
    }

    @Override
    public SensorDataCyclingDistanceSpeed createEmptySensorData(String address) {
        return new SensorDataCyclingDistanceSpeed(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingCadenceAndDistanceSpeed cadenceAndSpeed = parseCyclingCrankAndWheel(address, sensorName, characteristic);
        if (cadenceAndSpeed == null) {
            return;
        }

        if (cadenceAndSpeed.getDistanceSpeed() != null) {
            observer.onChange(cadenceAndSpeed.getDistanceSpeed());
        }
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static SensorDataCyclingCadenceAndDistanceSpeed parseCyclingCrankAndWheel(String address, String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.csc_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasWheel = (flags & 0x01) > 0;
        boolean hasCrank = (flags & 0x02) > 0;

        int index = 1;
        SensorDataCyclingDistanceSpeed speed = null;
        if (hasWheel && valueLength - index >= 6) {
            int wheelTotalRevolutionCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index);
            index += 4;
            int wheelTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            speed = new SensorDataCyclingDistanceSpeed(address, sensorName, wheelTotalRevolutionCount, wheelTime);
            index += 2;
        }

        SensorDataCyclingCadence cadence = null;
        if (hasCrank && valueLength - index >= 4) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s
            cadence = new SensorDataCyclingCadence(address, sensorName, crankCount, crankTime);
        }

        return new SensorDataCyclingCadenceAndDistanceSpeed(address, sensorName, cadence, speed);
    }
}
