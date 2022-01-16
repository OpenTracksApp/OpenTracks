package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Pair;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;

public final class SensorDataSet {

    private SensorDataHeartRate heartRate;

    private SensorDataCycling.CyclingCadence cyclingCadence;

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

    public Pair<HeartRate, String> getHeartRate() {
        if (heartRate != null) {
            return new Pair<>(heartRate.getValue(), heartRate.getSensorNameOrAddress());
        }

        return null;
    }

    public Pair<Cadence, String> getCadence() {
        if (cyclingCadence != null) {
            return new Pair<>(cyclingCadence.getValue(), cyclingCadence.getSensorNameOrAddress());
        }

        if (runningDistanceSpeedCadence != null) {
            return new Pair<>(runningDistanceSpeedCadence.getCadence(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public Pair<Speed, String> getSpeed() {
        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasValue() && cyclingDistanceSpeed.getValue().getSpeed() != null) {
            return new Pair<>(cyclingDistanceSpeed.getValue().getSpeed(), cyclingDistanceSpeed.getSensorNameOrAddress());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue() && runningDistanceSpeedCadence.getValue().getSpeed() != null) {
            return new Pair<>(runningDistanceSpeedCadence.getSpeed(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public SensorDataCycling.CyclingCadence getCyclingCadence() {
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
        if (getHeartRate() != null) {
            trackPoint.setHeartRate(getHeartRate().first);
        }

        if (getCadence() != null) {
            trackPoint.setCadence(getCadence().first);
        }

        if (getSpeed() != null) {
            trackPoint.setSpeed(getSpeed().first);
        }

        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasValue()) {
            trackPoint.setSensorDistance(cyclingDistanceSpeed.getValue().getDistanceOverall());
        }

        if (cyclingPower != null && cyclingPower.hasValue()) {
            trackPoint.setPower(cyclingPower.getValue());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue()) {
            trackPoint.setSensorDistance(runningDistanceSpeedCadence.getValue().getDistance());
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
        return (heartRate != null ? "" + heartRate : "")
                + (cyclingCadence != null ? " " + cyclingCadence : "")
                + (cyclingDistanceSpeed != null ? " " + cyclingDistanceSpeed : "")
                + (cyclingPower != null ? " " + cyclingPower : "")
                + (runningDistanceSpeedCadence != null ? " " + runningDistanceSpeedCadence : "");
    }

    private void set(@NonNull SensorData<?> type, SensorData<?> data) {
        if (type instanceof SensorDataHeartRate) {
            this.heartRate = (SensorDataHeartRate) data;
            return;
        }

        if (type instanceof SensorDataCycling.CyclingCadence) {
            this.cyclingCadence = (SensorDataCycling.CyclingCadence) data;
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
