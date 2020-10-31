package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

public final class SensorDataSet {

    private SensorDataHeartRate heartRate;

    private SensorDataCycling.Cadence cyclingCadence;

    private SensorDataCycling.Speed cyclingSpeed;

    public SensorDataSet() {
    }

    public SensorDataHeartRate getHeartRate() {
        return heartRate;
    }

    public SensorDataCycling.Cadence getCyclingCadence() {
        return cyclingCadence;
    }

    public SensorDataCycling.Speed getCyclingSpeed() {
        return cyclingSpeed;
    }

    public void set(SensorData data) {
        set(data, data);
    }

    public void remove(SensorData type) {
        set(type, null);
    }

    public void clear() {
        this.heartRate = null;
        this.cyclingCadence = null;
        this.cyclingSpeed = null;
    }

    public void fillTrackPoint(TrackPoint trackPoint) {
        if (heartRate != null) {
            trackPoint.setHeartRate_bpm(heartRate.getHeartRate_bpm());
        }

        if (cyclingCadence != null && cyclingCadence.hasCadence_rpm()) {
            trackPoint.setCyclingCadence_rpm(cyclingCadence.getCadence_rpm());
        }

        if (cyclingSpeed != null && cyclingSpeed.hasSpeed_mps()) {
            trackPoint.setSpeed(cyclingSpeed.getSpeed_mps());
        }
    }

    @NonNull
    @Override
    public String toString() {
        return (getHeartRate() != null ? "" + getHeartRate() : "")
                + (getCyclingCadence() != null ? " " + getCyclingCadence() : "")
                + (getCyclingSpeed() != null ? " " + getCyclingSpeed() : "");
    }

    private void set(SensorData type, SensorData data) {
        if (type instanceof SensorDataHeartRate) {
            this.heartRate = (SensorDataHeartRate) data;
            return;
        }

        if (type instanceof SensorDataCycling.Cadence) {
            this.cyclingCadence = (SensorDataCycling.Cadence) data;
            return;
        }
        if (type instanceof SensorDataCycling.Speed) {
            this.cyclingSpeed = (SensorDataCycling.Speed) data;
            return;
        }

        throw new UnsupportedOperationException();
    }
}
