package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;

public class SensorData {

    private final String sensorAddress;
    private final String sensorName;

    private final long timestamp_ms;

    SensorData(String sensorAddress) {
        this(sensorAddress, null, System.currentTimeMillis());
    }

    SensorData(String sensorAddress, String sensorName) {
        this(sensorAddress, sensorName, System.currentTimeMillis());
    }

    @VisibleForTesting
    SensorData(String sensorAddress, String sensorName, long timestamp_ms) {
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

    /**
     * Is the data recent considering the current time.
     */
    public boolean isRecent() {
        return timestamp_ms + BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS > System.currentTimeMillis();
    }
}
