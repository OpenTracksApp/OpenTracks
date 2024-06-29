package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Position;

public class AggregatorGPS extends Aggregator<Position, Position> {


    public AggregatorGPS(String sensorAddress) {
        super(sensorAddress);
    }

    @Override
    protected void computeValue(Raw<Position> current) {
        value = current.value();
    }

    @Override
    public void reset() {
        value = null;
    }

    @NonNull
    @Override
    protected Position getNoneValue() {
        return Position.empty();
    }
}
