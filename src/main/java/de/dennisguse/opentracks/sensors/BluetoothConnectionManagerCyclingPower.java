package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingCadence;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataCyclingPower;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionManagerCyclingPower  implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID CYCLING_POWER = new ServiceMeasurementUUID(
            new UUID(0x181800001000L, 0x800000805f9b34fbL),
            new UUID(0x2A6300001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(CYCLING_POWER);
    }

    @Override
    public SensorDataCyclingPower createEmptySensorData(String address) {
        return new SensorDataCyclingPower(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, @NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        SensorDataCyclingPower.Data cyclingPower = parseCyclingPower(address, sensorName, characteristic);

        if (cyclingPower != null) {
            observer.onChange(cyclingPower.power());
        }
    }


    @VisibleForTesting
    public static SensorDataCyclingPower.Data parseCyclingPower(String address, String sensorName, BluetoothGattCharacteristic characteristic) {
        // DOCUMENTATION https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Characteristics/org.bluetooth.characteristic.cycling_power_measurement.xml
        int valueLength = characteristic.getValue().length;
        if (valueLength == 0) {
            return null;
        }

        int index = 0;
        int flags1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index++);
        int flags2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, index++);
        boolean hasPedalPowerBalance = (flags1 & 0x01) > 0;
        boolean hasAccumulatedTorque = (flags1 & 0x04) > 0;
        boolean hasWheel = (flags1 & 16) > 0;
        boolean hasCrank = (flags1 & 32) > 0;

        Integer instantaneousPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, index);
        index += 2;

        if (hasPedalPowerBalance) {
            index += 1;
        }
        if (hasAccumulatedTorque) {
            index += 2;
        }
        if (hasWheel) {
            index += 2 + 2;
        }

        SensorDataCyclingCadence cadence = null;
        if (hasCrank && valueLength - index >= 4) {
            long crankCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
            index += 2;

            int crankTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index); // 1/1024s

            cadence = new SensorDataCyclingCadence(address, sensorName, crankCount, crankTime);
        }


        return new SensorDataCyclingPower.Data(new SensorDataCyclingPower(sensorName, address, Power.of(instantaneousPower)), cadence);
    }

}
