package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import java.time.Instant;

import de.dennisguse.opentracks.sensors.BluetoothRemoteSensorManager;

public abstract class Aggregator<Input, Output> {

    protected Raw<Input> previous;

    protected Output value;

    private final String sensorAddress;
    private final String sensorName;

    Aggregator(String sensorAddress) {
        this(sensorAddress, null);
    }

    Aggregator(String sensorAddress, String sensorName) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
    }

    public String getSensorNameOrAddress() {
        return sensorName != null ? sensorName : sensorAddress;
    }

    public final void add(Raw<Input> current) {
        computeValue(current);
        previous = current;
    }

    protected abstract void computeValue(Raw<Input> current);

    public boolean hasValue() {
        return value != null;
    }

    @NonNull
    protected abstract Output getNoneValue();

    public Output getValue(Instant now) {
        if (!hasValue()) {
            return null; //TODO Check if this is a good idea!
        }
        if (isRecent(now)) {
            return value;
        }
        return getNoneValue();
    }

    /**
     * Reset long term aggregated values (more than derived from previous SensorData). e.g. overall distance.
     */
    public void reset() {}

    /**
     * Is the data recent considering the current time.
     */
    private boolean isRecent(Instant now) {
        if (previous == null) {
            return false;
        }

        return now
                .isBefore(previous.time().plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE));
    }

    @NonNull
    @Override
    public String toString() {
        return "sensorAddress=" + sensorAddress + " data=" + value;
    }
}
