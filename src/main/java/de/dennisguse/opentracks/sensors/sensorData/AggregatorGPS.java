package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Position;

public class AggregatorGPS extends Aggregator<Position, Position> {


    public AggregatorGPS(String sensorAddress) {
        super(sensorAddress);
    }

    @Override
    protected void computeValue(Raw<Position> current) {
        aggregatedValue = current.value();
    }

    @Override
    public void reset() {
    }

    @NonNull
    @Override
    protected Position getNoneValue() {
        return Position.empty();
    }
}
