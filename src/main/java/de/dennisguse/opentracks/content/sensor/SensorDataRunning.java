package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;

/**
 * Provides cadence in rpm and speed in milliseconds from Bluetooth LE Running Speed and Cadence sensors.
 */
public final class SensorDataRunning extends SensorData<SensorDataRunning.Data> {

    private static final String TAG = SensorDataRunning.class.getSimpleName();

    private final Speed speed;

    private final Float cadence;

    private final Distance totalDistance;

    public SensorDataRunning(String sensorAddress) {
        super(sensorAddress);
        this.speed = null;
        this.cadence = null;
        this.totalDistance = null;
    }

    public SensorDataRunning(String sensorAddress, String sensorName, Speed speed, Float cadence, Distance totalDistance) {
        super(sensorAddress, sensorName);
        this.speed = speed;
        this.cadence = cadence;
        this.totalDistance = totalDistance;
    }

    private boolean hasTotalDistance() {
        return totalDistance != null;
    }


    public Float getCadence() {
        return cadence;
    }

    @VisibleForTesting
    public Speed getSpeed() {
        return speed;
    }

    @VisibleForTesting
    public Distance getTotalDistance() {
        return totalDistance;
    }

    public void compute(SensorDataRunning previous) {
        Distance overallDistance = null;
        if (hasTotalDistance() && previous != null && previous.hasTotalDistance()) {
            overallDistance = this.totalDistance.minus(previous.totalDistance);
            if (previous.hasValue() && previous.getValue().getDistance() != null) {
                overallDistance = overallDistance.plus(previous.getValue().getDistance());
            }
        }

        value = new Data(speed, cadence, overallDistance);
    }

    @Override
    public void reset() {
        if (value != null) {
            value = new Data(value.speed, value.cadence, Distance.of(0));
        }
    }

    public static class Data {
        private final Speed speed;
        private final Float cadence;

        @Nullable
        private final Distance distance;

        public Data(Speed speed, Float cadence, @Nullable Distance distance) {
            this.speed = speed;
            this.cadence = cadence;
            this.distance = distance;
        }

        public Speed getSpeed() {
            return speed;
        }

        public Float getCadence() {
            return cadence;
        }

        @Nullable
        public Distance getDistance() {
            return distance;
        }

        @NonNull
        @Override
        public String toString() {
            return "Data{" +
                    "speed=" + speed +
                    ", cadence=" + cadence +
                    ", distance=" + distance +
                    '}';
        }
    }
}

