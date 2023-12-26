package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;

/**
 * Provides cadence in rpm and speed in milliseconds from Bluetooth LE Running Speed and Cadence sensors.
 */
public final class AggregatorRunning extends Aggregator<BluetoothHandlerRunningSpeedAndCadence.Data, AggregatorRunning.Data> {

    private static final String TAG = AggregatorRunning.class.getSimpleName();

    public AggregatorRunning(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
    }

    @Override
    public void computeValue(Raw<BluetoothHandlerRunningSpeedAndCadence.Data> current) {
        if (previous != null) {

            Distance distance = null;
            if (previous.value().totalDistance() != null && current.value().totalDistance() != null) {
                distance = current.value().totalDistance().minus(previous.value().totalDistance());
                if (value != null) {
                    distance = distance.plus(value.distance);
                }
            }

            value = new Data(current.value().speed(), current.value().cadence(), distance);
        }
    }

    @Override
    public void reset() {
        if (value != null) {
            value = new Data(value.speed, value.cadence, Distance.of(0));
        }
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

    public record Data(Speed speed, Cadence cadence, @NonNull Distance distance) {
    }
}

