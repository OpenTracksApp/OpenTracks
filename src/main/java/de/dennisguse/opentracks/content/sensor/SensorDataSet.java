package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPointSensorDataSet;

/**
 *
 */
public final class SensorDataSet {

    private SensorDataHeartRate heartRate;

    private SensorDataCycling.Cadence cyclingCadence;

    private SensorDataCycling.Speed cyclingSpeed;

    public SensorDataSet() {
    }

    @VisibleForTesting
    public SensorDataSet(SensorDataHeartRate heartRate, SensorDataCycling.Cadence cyclingCadence, SensorDataCycling.Speed cyclingSpeed) {
        this.heartRate = heartRate;
        this.cyclingCadence = cyclingCadence;
        this.cyclingSpeed = cyclingSpeed;
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
        if (data == null) {
            return;
        }

        if (data instanceof SensorDataHeartRate) {
            this.heartRate = (SensorDataHeartRate) data;
            return;
        }

        if (data instanceof SensorDataCycling.Cadence) {
            this.cyclingCadence = (SensorDataCycling.Cadence) data;
            return;
        }
        if (data instanceof SensorDataCycling.Speed) {
            this.cyclingSpeed = (SensorDataCycling.Speed) data;
            return;
        }
        if (data instanceof SensorDataCycling.CadenceAndSpeed) {
            set(((SensorDataCycling.CadenceAndSpeed) data).getCadence());
            set(((SensorDataCycling.CadenceAndSpeed) data).getSpeed());
        }

        throw new UnsupportedOperationException();
    }

    public void clear() {
        this.heartRate = null;
        this.cyclingCadence = null;
        this.cyclingSpeed = null;
    }

    public TrackPointSensorDataSet createTrackPointSensorDataSet() {
        TrackPointSensorDataSet sensorDataSet = new TrackPointSensorDataSet();
        if (heartRate != null) {
            sensorDataSet.setHeartRate_bpm(heartRate.getHeartRate_bpm());
        }

        if (cyclingCadence != null && cyclingCadence.hasCadence_rpm()) {
            sensorDataSet.setCyclingCadence(cyclingCadence.getCadence_rpm());
        }

        if (cyclingSpeed != null && cyclingSpeed.hasSpeed()) {
            sensorDataSet.setCyclingCadence(cyclingSpeed.getSpeed_ms());
        }

        return sensorDataSet;
    }

    @NonNull
    @Override
    public String toString() {
        return (getHeartRate() != null ? "" + getHeartRate() : "")
                + (getCyclingCadence() != null ? " " + getCyclingCadence() : "")
                + (getCyclingSpeed() != null ? " " + getCyclingSpeed() : "");
    }
}
