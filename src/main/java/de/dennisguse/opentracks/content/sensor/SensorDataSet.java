package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

public final class SensorDataSet {

    private SensorDataHeartRate heartRate;

    private SensorDataCycling.Cadence cyclingCadence;

    private SensorDataCycling.DistanceSpeed cyclingDistanceSpeed;

    private SensorDataCyclingPower cyclingPower;

    public SensorDataSet() {
    }

    public SensorDataHeartRate getHeartRate() {
        return heartRate;
    }

    public SensorDataCycling.Cadence getCyclingCadence() {
        return cyclingCadence;
    }

    public SensorDataCycling.DistanceSpeed getCyclingDistanceSpeed() {
        return cyclingDistanceSpeed;
    }

    public SensorDataCyclingPower getCyclingPower() {
        return cyclingPower;
    }

    public void set(SensorData<?> data) {
        set(data, data);
    }

    public void remove(SensorData<?> type) {
        set(type, null);
    }

    public void clear() {
        this.heartRate = null;
        this.cyclingCadence = null;
        this.cyclingDistanceSpeed = null;
        this.cyclingPower = null;
    }

    public void fillTrackPoint(TrackPoint trackPoint) {
        if (heartRate != null) {
            trackPoint.setHeartRate_bpm(heartRate.getValue());
        }

        if (cyclingCadence != null && cyclingCadence.hasValue()) {
            trackPoint.setCyclingCadence_rpm(cyclingCadence.getValue());
        }

        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasValue()) {
            trackPoint.setSensorDistance(cyclingDistanceSpeed.getValue().distance_overall_m);
            trackPoint.setSpeed(cyclingDistanceSpeed.getValue().speed_mps);
        }

        if (cyclingPower != null && cyclingPower.hasValue()) {
            trackPoint.setPower(cyclingPower.getValue());
        }
    }

    public void reset() {
        if (heartRate != null) heartRate.reset();
        if (cyclingCadence != null) cyclingCadence.reset();
        if (cyclingDistanceSpeed != null) cyclingDistanceSpeed.reset();
        if (cyclingPower != null) cyclingPower.reset();
    }

    @NonNull
    @Override
    public String toString() {
        return (getHeartRate() != null ? "" + getHeartRate() : "")
                + (getCyclingCadence() != null ? " " + getCyclingCadence() : "")
                + (getCyclingDistanceSpeed() != null ? " " + getCyclingDistanceSpeed() : "")
                + (getCyclingPower() != null ? " " + getCyclingPower() : "");
    }

    private void set(@NonNull SensorData<?> type, SensorData<?> data) {
        if (type instanceof SensorDataHeartRate) {
            this.heartRate = (SensorDataHeartRate) data;
            return;
        }

        if (type instanceof SensorDataCycling.Cadence) {
            this.cyclingCadence = (SensorDataCycling.Cadence) data;
            return;
        }
        if (type instanceof SensorDataCycling.DistanceSpeed) {
            this.cyclingDistanceSpeed = (SensorDataCycling.DistanceSpeed) data;
            return;
        }

        if (type instanceof SensorDataCyclingPower) {
            this.cyclingPower = (SensorDataCyclingPower) data;
            return;
        }

        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }
}
