package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;

/**
 * Provides cadence in rpm and speed in milliseconds from Bluetooth LE Running Speed and Cadence sensors.
 */
public final class SensorDataRunning extends SensorData<SensorDataRunning.Data> {

    private static final String TAG = SensorDataRunning.class.getSimpleName();

    private final Speed speed;

    private final Cadence cadence;

    private final Distance totalDistance;

    public SensorDataRunning(String sensorAddress) {
        super(sensorAddress);
        this.speed = null;
        this.cadence = null;
        this.totalDistance = null;
    }

    public SensorDataRunning(String sensorAddress, String sensorName, Speed speed, Cadence cadence, Distance totalDistance) {
        super(sensorAddress, sensorName);
        this.speed = speed;
        this.cadence = cadence;
        this.totalDistance = totalDistance;
    }

    private boolean hasTotalDistance() {
        return totalDistance != null;
    }


    public Cadence getCadence() {
        return cadence;
    }

    public Speed getSpeed() {
        return speed;
    }

    @VisibleForTesting
    public Distance getTotalDistance() {
        return totalDistance;
    }

    @NonNull
    @Override
    protected Data getNoneValue() {
        if (value != null) {
            return new Data(Speed.zero(), Cadence.of(0f), value.distance);
        } else {
            return new Data(Speed.zero(), Cadence.of(0f), Distance.of(0));
        }
    }

    public void compute(SensorDataRunning previous) {
        if (speed != null && hasTotalDistance()) {
            Distance overallDistance = null;
            if (previous != null && previous.hasTotalDistance()) {
                overallDistance = this.totalDistance.minus(previous.totalDistance);
                if (previous.hasValue() && previous.getValue().distance() != null) {
                    overallDistance = overallDistance.plus(previous.getValue().distance());
                }
            }

            value = new Data(speed, cadence, overallDistance);
        }
    }

    @Override
    public void reset() {
        if (value != null) {
            value = new Data(value.speed, value.cadence, Distance.of(0));
        }
    }

    public record Data(Speed speed, Cadence cadence, @Nullable Distance distance) {}
}

