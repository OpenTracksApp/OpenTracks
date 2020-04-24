package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 *
 */
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

    public void fillTrackPoint(TrackPoint trackPoint) {
        if (heartRate != null) {
            trackPoint.setHeartRate_bpm(heartRate.getHeartRate_bpm());
        }

        if (cyclingCadence != null && cyclingCadence.hasCadence_rpm()) {
            trackPoint.setCyclingCadence_rpm(cyclingCadence.getCadence_rpm());
        }

        if (cyclingSpeed != null && cyclingSpeed.hasSpeed()) {
            trackPoint.setSpeed(cyclingSpeed.getSpeed_ms());
        }
    }

    @NonNull
    @Override
    public String toString() {
        return (getHeartRate() != null ? "" + getHeartRate() : "")
                + (getCyclingCadence() != null ? " " + getCyclingCadence() : "")
                + (getCyclingSpeed() != null ? " " + getCyclingSpeed() : "");
    }
}
