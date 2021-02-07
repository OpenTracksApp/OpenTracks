package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;

import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;

public class SensorData {

    private final String sensorAddress;
    private final String sensorName;

    private final Instant timestamp_ms;

    SensorData(String sensorAddress) {
        this(sensorAddress, null);
    }

    SensorData(String sensorAddress, String sensorName) {
        this(sensorAddress, sensorName, Instant.now());
    }

    @VisibleForTesting
    SensorData(String sensorAddress, String sensorName, Instant timestamp_ms) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.timestamp_ms = timestamp_ms;
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

    public boolean isRecent() {
        return Instant.now().isBefore(timestamp_ms.plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS));
    }
}
