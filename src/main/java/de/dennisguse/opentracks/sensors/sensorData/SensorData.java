package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;

import de.dennisguse.opentracks.sensors.BluetoothRemoteSensorManager;

public abstract class SensorData<T> {

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

    @NonNull
    protected abstract T getNoneValue();

    public T getValue() {
        if (!hasValue()) {
            return null;
        }
        if (isRecent()) {
            return value;
        }
        return getNoneValue();
    }

    /**
     * Reset long term aggregated values (more than derived from previous SensorData). e.g. overall distance.
     */
    public void reset() {
    }

    /**
     * Is the data recent considering the current time.
     */
    private boolean isRecent() {
        return Instant.now()
                .isBefore(time.plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE));
    }

    @NonNull
    @Override
    public String toString() {
        return "sensorAddress='" + sensorAddress;
    }
}
