package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRate extends Aggregator<HeartRate, HeartRate> {

    public AggregatorHeartRate(String name, String address) {
        super(name, address);
    }

    @Override
    protected void computeValue(Raw<HeartRate> current) {
        this.aggregatedValue = current.value();
    }

    @Override
    protected void resetImmediate() {
        aggregatedValue = getNoneValue();
    }

    @Override
    public void resetAggregated() {
    }

    @NonNull
    @Override
    protected HeartRate getNoneValue() {
        return HeartRate.of(0);
    }
}
