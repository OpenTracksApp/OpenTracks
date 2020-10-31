package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

public class SensorDataHeartRate extends SensorData {

    private final Float heartRate_bpm;

    public SensorDataHeartRate(String address) {
        super(address);
        heartRate_bpm = null;
    }

    public SensorDataHeartRate(String name, String address, float heartRate_bpm) {
        super(name, address);
        this.heartRate_bpm = heartRate_bpm;
    }

    public boolean hasHeartRate_bpm() {
        return heartRate_bpm != null;
    }

    public Float getHeartRate_bpm() {
        return heartRate_bpm;
    }

    @NonNull
    @Override
    public String toString() {
        return "heart=" + heartRate_bpm;
    }
}
