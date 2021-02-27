package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;

import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;

public class SensorData<T> {

    protected T value;

    private final String sensorAddress;
    private final String sensorName;

    private final Instant time;

    SensorData(String sensorAddress) {
        this(sensorAddress, null);
    }

    SensorData(String sensorAddress, String sensorName) {
        this(sensorAddress, sensorName, Instant.now());
    }

    @VisibleForTesting
    SensorData(String sensorAddress, String sensorName, Instant time) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.time = time;
    }

    @NonNull
    public String getSensorAddress() {
        return sensorAddress;
    }

    @Nullable
    public String getSensorName() {
        return sensorName;
    }

    public String getSensorNameOrAddress() {
        return sensorName != null ? sensorName : sensorAddress;
    }

    public boolean hasValue() {
        return value != null;
    }

    public T getValue() {
        return value;
    }

    /**
     * Is the data recent considering the current time.
     */
    public boolean isRecent() {
        return Instant.now()
                .isBefore(time.plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS));
    }
}
