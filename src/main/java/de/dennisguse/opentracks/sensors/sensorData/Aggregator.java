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

    public boolean hasAggregatedValue() {
        return aggregatedValue != null;
    }

    @NonNull
    protected abstract Output getNoneValue();

    @NonNull
    public Output getAggregatedValue(Instant now) {
        if (!hasAggregatedValue()) {
            return getNoneValue();
        }
        //TODO This should only affect measured data (like heartrate), but not aggregated values.
        //Remove current measurements, but provide aggregates?
        if (isRecent(now)) {
            return aggregatedValue;
        }
        return getNoneValue();
    }

    @NonNull
    public Pair<Output, String> getAggregatedValueWithSensorName(Instant now) {
        return new Pair<>(getAggregatedValue(now), getSensorNameOrAddress());
    }

    /**
     * Reset long term aggregated values (more than derived from previous SensorData). e.g. overall distance.
     */
    public abstract void reset();

    /**
     * Is the data recent considering the current time.
     */
    private boolean isRecent(Instant now) {
        if (previous == null) {
            return false;
        }

        return now
                .isBefore(previous.time().plus(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE)); //TODO Per Sensor!
    }

    @NonNull
    @Override
    public String toString() {
        return "sensorAddress=" + sensorAddress + " data=" + aggregatedValue;
    }
}
