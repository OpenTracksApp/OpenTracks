package de.dennisguse.opentracks.stats;

public class SensorStatistics {
    private Float maxHr;
    private Float avgHr;
    private Float maxCadence;
    private Float avgCadence;
    private Float avgPower;

    public SensorStatistics(Float maxHr, Float avgHr, Float maxCadence, Float avgCadence, Float avgPower) {
        this.maxHr = maxHr;
        this.avgHr = avgHr;
        this.maxCadence = maxCadence;
        this.avgCadence = avgCadence;
        this.avgPower = avgPower;
    }

    public boolean hasHeartRate() {
        return avgHr != null && maxHr != null;
    }

    public float getMaxHeartRate() {
        return maxHr;
    }

    public float getAvgHeartRate() {
        return avgHr;
    }

    public boolean hasCadence() {
        return avgCadence != null && maxCadence != null;
    }

    public float getMaxCadence() {
        return maxCadence;
    }

    public float getAvgCadence() {
        return avgCadence;
    }

    public boolean hasPower() {
        return avgPower != null;
    }

    public float getAvgPower() {
        return avgPower;
    }
}
