package de.dennisguse.opentracks.sensors.sensorData;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;

import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;

public interface SensorHandlerInterface {

    List<ServiceMeasurementUUID> getServices();

    SensorData<?, ?> createEmptySensorData(String address);

    void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic);
}
