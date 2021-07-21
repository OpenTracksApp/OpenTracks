package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

public final class SensorDataSet {

    private SensorDataHeartRate heartRate;

    private SensorDataCycling.Cadence cyclingCadence;

    private SensorDataCycling.DistanceSpeed cyclingDistanceSpeed;

    private SensorDataCyclingPower cyclingPower;

    private SensorDataRunning runningDistanceSpeedCadence;

    public SensorDataSet() {
    }

    public SensorDataSet(SensorDataSet toCopy) {
        this.heartRate = toCopy.heartRate;
        this.cyclingCadence = toCopy.cyclingCadence;
        this.cyclingDistanceSpeed = toCopy.cyclingDistanceSpeed;
        this.cyclingPower = toCopy.cyclingPower;
        this.runningDistanceSpeedCadence = toCopy.runningDistanceSpeedCadence;
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

    public SensorDataRunning getRunningDistanceSpeedCadence() {
        return runningDistanceSpeedCadence;
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
        this.runningDistanceSpeedCadence = null;
    }

    public void fillTrackPoint(TrackPoint trackPoint) {
        if (heartRate != null) {
            trackPoint.setHeartRate_bpm(heartRate.getValue());
        }

        if (cyclingCadence != null && cyclingCadence.hasValue()) {
            trackPoint.setCyclingCadence_rpm(cyclingCadence.getValue());
        }

        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasValue()) {
            trackPoint.setSensorDistance(cyclingDistanceSpeed.getValue().getDistanceOverall());
            trackPoint.setSpeed(cyclingDistanceSpeed.getValue().getSpeed());
        }

        if (cyclingPower != null && cyclingPower.hasValue()) {
            trackPoint.setPower(cyclingPower.getValue());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue()) {
            trackPoint.setSensorDistance(runningDistanceSpeedCadence.getValue().getDistance());
            trackPoint.setSpeed(runningDistanceSpeedCadence.getValue().getSpeed());
            trackPoint.setCyclingCadence_rpm(runningDistanceSpeedCadence.getValue().getCadence());
        }
    }

    public void reset() {
        if (heartRate != null) heartRate.reset();
        if (cyclingCadence != null) cyclingCadence.reset();
        if (cyclingDistanceSpeed != null) cyclingDistanceSpeed.reset();
        if (cyclingPower != null) cyclingPower.reset();

        if (runningDistanceSpeedCadence != null) runningDistanceSpeedCadence.reset();
    }

    @NonNull
    @Override
    public String toString() {
        return (getHeartRate() != null ? "" + getHeartRate() : "")
                + (getCyclingCadence() != null ? " " + getCyclingCadence() : "")
                + (getCyclingDistanceSpeed() != null ? " " + getCyclingDistanceSpeed() : "")
                + (getCyclingPower() != null ? " " + getCyclingPower() : "")
                + (getRunningDistanceSpeedCadence() != null ? " " + getRunningDistanceSpeedCadence() : "");
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

        if (type instanceof SensorDataRunning) {
            this.runningDistanceSpeedCadence = (SensorDataRunning) data;
            return;
        }

        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }
}
