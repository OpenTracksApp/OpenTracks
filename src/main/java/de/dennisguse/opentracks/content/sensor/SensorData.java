package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;

public class SensorData {

    private String sensorAddress;
    private String sensorName;

    private long timestamp_ms;

    SensorData(String sensorAddress, String sensorName) {
        this(sensorAddress, sensorName, System.currentTimeMillis());
    }

    @VisibleForTesting
    SensorData(String sensorAddress, String sensorName, long timestamp_ms) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.timestamp_ms = timestamp_ms;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }

    public String getSensorName() {
        return sensorName;
    }

    /**
     * Is the data recent considering the current time.
     */
    public boolean isRecent() {
        return timestamp_ms + BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS > System.currentTimeMillis();
    }
}
