package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingCadence;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public final class SensorDataSet {

    private static final String TAG = SensorDataSet.class.getSimpleName();

    @VisibleForTesting
    public AggregatorHeartRate heartRate;

    @VisibleForTesting
    public AggregatorCyclingCadence cyclingCadence;

    @VisibleForTesting
    public AggregatorCyclingDistanceSpeed cyclingDistanceSpeed;

    @VisibleForTesting
    public AggregatorCyclingPower cyclingPower;

    @VisibleForTesting
    public AggregatorRunning runningDistanceSpeedCadence;

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

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue() && runningDistanceSpeedCadence.value.cadence() != null) {
            return new Pair<>(runningDistanceSpeedCadence.value.cadence(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public Pair<Speed, String> getSpeed() {
        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasValue() && cyclingDistanceSpeed.getValue().speed() != null) {
            return new Pair<>(cyclingDistanceSpeed.getValue().speed(), cyclingDistanceSpeed.getSensorNameOrAddress());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasValue() && runningDistanceSpeedCadence.getValue().speed() != null) {
            return new Pair<>(runningDistanceSpeedCadence.value.speed(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public AggregatorCyclingPower getCyclingPower() {
        return cyclingPower;
    }

    public void add(@NonNull Aggregator<?, ?> data) {
        set(data, data);
    }

    public void update(@NonNull Raw<?> data) {
        Record value = data.value();

        if (value instanceof HeartRate) {
            this.heartRate.add((Raw<HeartRate>) data);
            return;
        }

        if (value instanceof BluetoothHandlerCyclingCadence.CrankData) {
            this.cyclingCadence.add((Raw<BluetoothHandlerCyclingCadence.CrankData>) data);
            return;
        }
        if (value instanceof BluetoothHandlerCyclingDistanceSpeed.WheelData ) {
            this.cyclingDistanceSpeed.setWheelCircumference(PreferencesUtils.getWheelCircumference()); //TODO Fetch once and then listen for changes.
            this.cyclingDistanceSpeed.add((Raw<BluetoothHandlerCyclingDistanceSpeed.WheelData>) data);
            return;
        }
        if (value instanceof BluetoothHandlerRunningSpeedAndCadence.Data) {
            this.runningDistanceSpeedCadence.add((Raw<BluetoothHandlerRunningSpeedAndCadence.Data>) data);

            return;
        }
        if (value instanceof BluetoothHandlerManagerCyclingPower.Data) {
            this.cyclingPower.add((Raw<Power>) data);
            return;
        }

        throw new UnsupportedOperationException(data.getClass().getCanonicalName());
    }

    public void remove(@NonNull Aggregator<?, ?> type) {
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
            trackPoint.setSensorDistance(cyclingDistanceSpeed.getValue().distanceOverall());
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

    private void set(@NonNull Aggregator<?, ?> type, @Nullable Aggregator<?, ?> sensorData) {
        if (type instanceof AggregatorHeartRate hr) {
            heartRate = hr;
            return;
        }
        if (type instanceof AggregatorCyclingCadence cc) {
            cyclingCadence = cc;
            return;
        }
        if (type instanceof AggregatorCyclingDistanceSpeed ds) {
            cyclingDistanceSpeed = ds;
            return;
        }
        if (type instanceof AggregatorCyclingPower cp) {
            cyclingPower = cp;
            return;
        }
        if (type instanceof AggregatorRunning rr) {
            runningDistanceSpeedCadence = rr;
            return;
        }

        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }
}
