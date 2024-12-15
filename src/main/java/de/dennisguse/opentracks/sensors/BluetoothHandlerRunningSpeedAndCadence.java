package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorRunning;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothHandlerRunningSpeedAndCadence implements SensorHandlerInterface {


    public static final ServiceMeasurementUUID RUNNING_SPEED_CADENCE = new ServiceMeasurementUUID(
            new UUID(0x181400001000L, 0x800000805f9b34fbL),
            new UUID(0x2A5300001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(RUNNING_SPEED_CADENCE);
    }

    @Override
    public AggregatorRunning createEmptySensorData(String address, String name) {
        return new AggregatorRunning(address, name);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, @NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        Data data = parseRunningSpeedAndCadence(sensorName, characteristic);
        observer.onChange(new Raw<>(observer.getNow(), data));
    }

    @VisibleForTesting
    public static Data parseRunningSpeedAndCadence(String sensorName, @NonNull BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.rsc_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int flags = characteristic.getValue()[0];
        boolean hasStrideLength = (flags & 0x01) > 0;
        boolean hasTotalDistance = (flags & 0x02) > 0;
        boolean hasStatus = (flags & 0x03) > 0; // walking vs running

        Speed speed = null;
        Cadence cadence = null;
        Distance totalDistance = null;

        int index = 1;
        if (valueLength - index >= 2) {
            speed = Speed.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index) / 256f);
        }

        index = 3;
        if (valueLength - index >= 1) {
            cadence = Cadence.of(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index));

            // Hacky workaround as the Wahoo Tickr X provides cadence in SPM (steps per minute) in violation to the standard.
            if (sensorName != null && sensorName.startsWith("TICKR X")) {
                cadence = Cadence.of(cadence.getRPM() / 2);
            }
        }

        index = 4;
        if (hasStrideLength && valueLength - index >= 2) {
            Distance strideDistance = Distance.ofCM(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index));
            index += 2;
        }

        if (hasTotalDistance && valueLength - index >= 4) {
            totalDistance = Distance.ofDM(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index));
        }

        return new Data(speed, cadence, totalDistance);
    }

    public record Data(Speed speed, Cadence cadence, Distance totalDistance) {}
}
