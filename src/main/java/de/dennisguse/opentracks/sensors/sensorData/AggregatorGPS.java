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
    protected void resetImmediate() {
        aggregatedValue = Position.empty();
    }

    @Override
    public void resetAggregated() {
        /*
         * GPS data is not an aggregated value, but for now we want to ensure to only save the data once.
         * The data is too large to save it more often than needed (i.e., duplicated values).
         * TODO: this behavior can be changed if TrackRecordingManager.insertTrackPoint() would strip GPS data if it was already saved. This would simplify TrackPointCreator.createCurrentTrackPoint()
         */
        aggregatedValue = Position.empty();
    }

    @NonNull
    @Override
    protected Position getNoneValue() {
        return Position.empty();
    }
}
