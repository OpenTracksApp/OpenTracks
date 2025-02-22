package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.time.Instant;

import de.dennisguse.opentracks.sensors.BluetoothRemoteSensorManager;

public abstract class Aggregator<Input, Output> {

    protected Raw<Input> previous;

    protected Output aggregatedValue;

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

    /**
     * @return did we process data from a sensor.
     * NOTE: for some sensors this may require more than one measurement.
     */
    public boolean hasReceivedData() {
        return aggregatedValue != null;
    }

    @NonNull
    protected abstract Output getNoneValue();

    @NonNull
    public Output getAggregatedValue(Instant now) {
        if (!hasReceivedData()) {
            return getNoneValue();
        }
        if (isOutdated(now)) {
            resetImmediate();
        }

        return aggregatedValue;
    }

    @NonNull
    public Pair<Output, String> getAggregatedValueWithSensorName(Instant now) {
        return new Pair<>(getAggregatedValue(now), getSensorNameOrAddress());
    }

    /**
     * Reset short-term (i.e., non-aggregated) values that were directly derived from sensor data.
     */
    protected abstract void resetImmediate();

    /**
     * Reset long-term (i.e., aggregated) values (more than derived from previous SensorData) like overall distance.
     */
    public abstract void resetAggregated();

    /**
     * Is the data recent considering the current time.
     */
    private boolean isOutdated(Instant now) {
        if (previous == null) {
            return true;
        }

        return now
                .isAfter(
                        previous.time()
                                .plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE)
                );
    }

    @NonNull
    @Override
    public String toString() {
        return "sensorAddress=" + sensorAddress + " data=" + aggregatedValue;
    }
}
