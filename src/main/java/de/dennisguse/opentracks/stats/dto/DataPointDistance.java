package de.dennisguse.opentracks.stats.dto;

public class DataPointDistance {
    float time;
    double distance;

    public DataPointDistance(float x, double y) {
        this.time = x;
        this.distance = y;
    }

    public float getTime() {
        return time;
    }

    public double getDistance() {
        return distance;
    }
}
