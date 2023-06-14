package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public final class SensorDataSet {

    private static final String TAG = SensorDataSet.class.getSimpleName();

    private SensorDataHeartRate heartRate;

    private SensorDataCyclingCadence cyclingCadence;

    private SensorDataCyclingDistanceSpeed cyclingDistanceSpeed;

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

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue() && runningDistanceSpeedCadence.getValue().speed() != null) {
            return new Pair<>(runningDistanceSpeedCadence.getSpeed(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public SensorDataCyclingCadence getCyclingCadence() {
        return cyclingCadence;
    }

    public SensorDataCyclingDistanceSpeed getCyclingDistanceSpeed() {
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
            trackPoint.setSensorDistance(runningDistanceSpeedCadence.getValue().distance());
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

    private void set(@NonNull SensorData<?> type, @Nullable SensorData<?> sensorData) {
        if (type instanceof SensorDataHeartRate) {
            this.heartRate = (SensorDataHeartRate) sensorData;
            return;
        }

        if (type instanceof SensorDataCyclingCadence) {
            SensorDataCyclingCadence previous = getCyclingCadence();
            Log.d(TAG, "Previous: " + previous + "; current: " + sensorData);

            if (sensorData != null && sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cadence data repeated.");
                return;
            }

            this.cyclingCadence = (SensorDataCyclingCadence) sensorData;
            this.cyclingCadence.compute(previous);
            return;
        }

        if (type instanceof SensorDataCyclingDistanceSpeed) {
            SensorDataCyclingDistanceSpeed previous = getCyclingDistanceSpeed();
            Log.d(TAG, "Previous: " + previous + "; Current" + sensorData);
            if (sensorData != null && sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: cycling speed data repeated.");
                return;
            }
            Distance preferenceWheelCircumference = PreferencesUtils.getWheelCircumference(); //TODO Fetch once and then listen for changes.

            this.cyclingDistanceSpeed = (SensorDataCyclingDistanceSpeed) sensorData;
            this.cyclingDistanceSpeed.compute(previous, preferenceWheelCircumference);
            return;
        }

        if (type instanceof SensorDataCyclingPower) {
            this.cyclingPower = (SensorDataCyclingPower) sensorData;
            return;
        }

        if (type instanceof SensorDataRunning) {
            SensorDataRunning previous = getRunningDistanceSpeedCadence();
            Log.d(TAG, "Previous: " + previous + "; Current" + sensorData);
            if (sensorData != null && sensorData.equals(previous)) {
                Log.d(TAG, "onChanged: running speed data repeated.");
                return;
            }

            this.runningDistanceSpeedCadence = (SensorDataRunning) sensorData;
            this.runningDistanceSpeedCadence.compute(previous);
            return;
        }

        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }
}
