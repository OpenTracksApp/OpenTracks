package de.dennisguse.opentracks.sensors.sensorData;

import android.location.Location;

import androidx.annotation.NonNull;

public class AggregatorGPS extends Aggregator<Location, Location> {

    public AggregatorGPS(String sensorAddress) {
        super(sensorAddress);
    }

    @Override
    protected void computeValue(Raw<Location> current) {
        value = current.value();
    }

    @Override
    public void reset() {
        value = null;
    }

    @NonNull
    @Override
    protected Location getNoneValue() {
        return new Location("none");
    }
}
