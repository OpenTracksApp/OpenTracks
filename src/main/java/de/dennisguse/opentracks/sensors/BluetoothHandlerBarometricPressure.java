package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.sensorData.Aggregator;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

public class BluetoothHandlerBarometricPressure implements SensorHandlerInterface {
    private static final UUID ENVIRONMENTAL_SENSING_SERVICE = new UUID(0x181A00001000L, 0x800000805f9b34fbL);
    public static final ServiceMeasurementUUID BAROMETRIC_PRESSURE = new ServiceMeasurementUUID(
            ENVIRONMENTAL_SENSING_SERVICE,
            new UUID(0x2A6D00001000L, 0x800000805f9b34fbL)
    );

    @Override
    public List<ServiceMeasurementUUID> getServices() {
        return List.of(BAROMETRIC_PRESSURE);
    }

    @Override
    public Aggregator<?, ?> createEmptySensorData(String address, String name) {
        return new AggregatorBarometer(address, name);
    }

    @Override
    public void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic) {
        AtmosphericPressure value = parseEnvironmentalSensing(characteristic);
        if (value == null) return;

        observer.onChange(new Raw<>(observer.getNow(), value));
    }

    /**
     * Decoding:
     * org.bluetooth.service.environmental_sensing.xml
     * org.bluetooth.characteristic.pressure.xml
     */
    public static AtmosphericPressure parseEnvironmentalSensing(BluetoothGattCharacteristic characteristic) {
        byte[] raw = characteristic.getValue();

        if (raw.length < 4) {
            return null;
        }

        Integer pressure = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        return AtmosphericPressure.ofPA(pressure / 10f);
    }

}
