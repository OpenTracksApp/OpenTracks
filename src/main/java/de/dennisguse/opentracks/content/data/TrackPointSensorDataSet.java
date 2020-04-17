package de.dennisguse.opentracks.content.data;

import androidx.annotation.NonNull;

//TODO Rename.
public final class TrackPointSensorDataSet {
    private long time_ms;

    private Float heartRate_bpm = null;
    private Float cyclingCadence = null;
    private Float cyclingSpeed = null;
    private Float power = null;

    public TrackPointSensorDataSet() {
        this(System.currentTimeMillis());
    }

    public TrackPointSensorDataSet(long time_ms) {
        this.time_ms = time_ms;
    }

    public boolean hasHeartRate() {
        return heartRate_bpm != null && heartRate_bpm > 0;
    }

    public float getHeartRate_bpm() {
        return heartRate_bpm;
    }

    public void setHeartRate_bpm(Float heartRate_bpm) {
        this.heartRate_bpm = heartRate_bpm;
    }

    public boolean hasCyclingCadence() {
        return cyclingCadence != null;
    }

    public Float getCyclingCadence_rpm() {
        return cyclingCadence;
    }

    public void setCyclingCadence(Float cyclingCadence) {
        this.cyclingCadence = cyclingCadence;
    }

    public void setCyclingCadence(Integer cyclingCadence) {
        this.cyclingCadence = cyclingCadence != null ? cyclingCadence.floatValue() : null;
    }

    public boolean hasCyclingSpeed() {
        return cyclingSpeed != null;
    }

    public Float getCyclingSpeed() {
        return cyclingSpeed;
    }

    public void setCyclingSpeed(Float cyclingSpeed) {
        this.cyclingSpeed = cyclingSpeed;
    }

    public boolean hasPower() {
        return power != null;
    }

    public Float getPower() {
        return power;
    }

    public void setPower(Float power) {
        this.power = power;
    }

    public long getTime() {
        return time_ms;
    }

    public void setTime(long time_ms) {
        this.time_ms = time_ms;
    }

    /**
     * Is the data recent considering the current time.
     *
     * @param maxAge the maximal age in milliseconds.
     */
    public boolean isRecent(long maxAge) {
        return time_ms + maxAge > System.currentTimeMillis();
    }

    @NonNull
    @Override
    public String toString() {
        return "time=" + getTime()
                + (hasHeartRate() ? " heart=" + getHeartRate_bpm() : "")
                + (hasCyclingCadence() ? " cyclingCad=" + getCyclingCadence_rpm() : "")
                + (hasCyclingSpeed() ? " cyclingSpeed=" + getCyclingSpeed() : "");
    }
}
