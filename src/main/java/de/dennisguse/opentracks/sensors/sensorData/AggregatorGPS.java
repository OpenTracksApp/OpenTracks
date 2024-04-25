package de.dennisguse.opentracks.sensors.sensorData;

import android.location.Location;

import androidx.annotation.NonNull;

import java.util.Optional;

public class AggregatorGPS extends Aggregator<Location, Optional<Location>> {


    public AggregatorGPS(String sensorAddress) {
        super(sensorAddress);
    }

    @Override
    protected void computeValue(Raw<Location> current) {
        value = Optional.of(current.value());
    }

    @Override
    public void reset() {
        value = null;
    }

    @NonNull
    @Override
    protected Optional<Location> getNoneValue() {
        return Optional.empty();
    }
}
