package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

public class SensorDataHeartRate extends SensorData {

    private final float heartRate_bpm;

    public SensorDataHeartRate(String name, String address, float heartRate_bpm) {
        super(name, address);
        this.heartRate_bpm = heartRate_bpm;
    }

    public boolean hasHeartRate_bpm() {
        return !Float.isNaN(heartRate_bpm);
    }

    public float getHeartRate_bpm() {
        return heartRate_bpm;
    }

    @NonNull
    @Override
    public String toString() {
        return "heart=" + heartRate_bpm;
    }
}
