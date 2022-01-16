package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.HeartRate;

public class SensorDataHeartRate extends SensorData<HeartRate> {

    public SensorDataHeartRate(String address) {
        super(address);
    }

    public SensorDataHeartRate(String name, String address, @NonNull HeartRate heartRate) {
        super(name, address);
        this.value = heartRate;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " heart=" + value;
    }

    @NonNull
    @Override
    protected HeartRate getNoneValue() {
        return HeartRate.of(0);
    }
}
