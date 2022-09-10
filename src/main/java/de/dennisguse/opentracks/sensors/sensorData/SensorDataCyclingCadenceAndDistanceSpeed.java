package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

public class SensorDataCyclingCadenceAndDistanceSpeed extends SensorData<Pair<SensorDataCyclingCadence, SensorDataCyclingDistanceSpeed>> {

    public SensorDataCyclingCadenceAndDistanceSpeed(String sensorAddress, String sensorName, @Nullable SensorDataCyclingCadence cadence, @Nullable SensorDataCyclingDistanceSpeed distanceSpeed) {
        super(sensorAddress, sensorName);
        this.value = new Pair<>(cadence, distanceSpeed);
    }

    public SensorDataCyclingCadence getCadence() {
        return this.value != null ? this.value.first : null;
    }

    public SensorDataCyclingDistanceSpeed getDistanceSpeed() {
        return this.value != null ? this.value.second : null;
    }

    @NonNull
    @Override
    protected Pair<SensorDataCyclingCadence, SensorDataCyclingDistanceSpeed> getNoneValue() {
        return new Pair<>(null, null);
    }
}
