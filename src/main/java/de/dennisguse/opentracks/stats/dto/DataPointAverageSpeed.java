package de.dennisguse.opentracks.stats.dto;

public class DataPointAverageSpeed {
    float time;
    double averageSpeed;

    public DataPointAverageSpeed(float x, double y) {
        this.time = x;
        this.averageSpeed = y;
    }

    public float getTime() {
        return time;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }
}
