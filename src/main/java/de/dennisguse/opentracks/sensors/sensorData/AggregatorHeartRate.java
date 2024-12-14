package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRate extends Aggregator<HeartRate, HeartRate> {

    public AggregatorHeartRate(String name, String address) {
        super(name, address);
    }

    @Override
    protected void computeValue(Raw<HeartRate> current) {
        this.value = current.value();
    }

    @Override
    public void reset() {
        // We don't need to reset the heart rate as this value is valid for a certain amount of time: and it is not an aggregate.
    }

    @NonNull
    @Override
    protected HeartRate getNoneValue() {
        return HeartRate.of(0);
    }
}
