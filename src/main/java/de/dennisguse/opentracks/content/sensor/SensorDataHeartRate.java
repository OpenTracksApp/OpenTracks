package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

public class SensorDataHeartRate extends SensorData<Float> {

    public SensorDataHeartRate(String address) {
        super(address);
    }

    public SensorDataHeartRate(String name, String address, float heartRate_bpm) {
        super(name, address);
        this.value = heartRate_bpm;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " heart=" + value;
    }
}
