package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataHeartRate;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothConnectionManagerHeartRate implements SensorHandlerInterface {

    public static final ServiceMeasurementUUID HEARTRATE = new ServiceMeasurementUUID(
            new UUID(0x180D00001000L, 0x800000805f9b34fbL),
            new UUID(0x2A3700001000L, 0x800000805f9b34fbL)
    );

    // Used for device discovery in preferences
    public static final List<ServiceMeasurementUUID> HEART_RATE_SUPPORTING_DEVICES = List.of(
            HEARTRATE,
            //Devices that support HEART_RATE_SERVICE_UUID, but do not announce HEART_RATE_SERVICE_UUID in there BLE announcement messages (during device discovery).
            new ServiceMeasurementUUID(
                    UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"), //Miband3
                    HEARTRATE.measurementUUID()
            )
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return HEART_RATE_SUPPORTING_DEVICES;
    }

    @Override
    public SensorDataHeartRate createEmptySensorData(String address) {
        return new SensorDataHeartRate(address);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, @NonNull ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        HeartRate heartRate = BluetoothUtils.parseHeartRate(characteristic);

        if (heartRate != null) {
            observer.onChange(new SensorDataHeartRate(address, sensorName, heartRate));
        }
    }
}
