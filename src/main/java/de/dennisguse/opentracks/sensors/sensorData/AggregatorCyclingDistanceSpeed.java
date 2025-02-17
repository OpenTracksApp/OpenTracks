package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.UintUtils;

public class AggregatorCyclingDistanceSpeed extends Aggregator<BluetoothHandlerCyclingDistanceSpeed.WheelData, AggregatorCyclingDistanceSpeed.Data> {

    private final String TAG = AggregatorCyclingDistanceSpeed.class.getSimpleName();

    private Distance wheelCircumference;

    public AggregatorCyclingDistanceSpeed(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
    }

    @Override
    protected void computeValue(Raw<BluetoothHandlerCyclingDistanceSpeed.WheelData> current) {
        if (previous == null) {
            return;
        }

        float timeDiff_ms = UintUtils.diff(current.value().wheelRevolutionsTime(), previous.value().wheelRevolutionsTime(), UintUtils.UINT16_MAX) / 1024f * 1000;
        Duration timeDiff = Duration.ofMillis((long) timeDiff_ms);

        if (timeDiff.isZero()) {
            return;
        }
        if (timeDiff.isNegative()) {
            Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
            aggregatedValue = null;
            return;
        }

        if (current.value().wheelRevolutionsCount() < previous.value().wheelRevolutionsCount()) {
            Log.e(TAG, "Wheel revolutions count difference is invalid: cannot compute speed.");
            return;
        }
        long wheelDiff = UintUtils.diff(current.value().wheelRevolutionsCount(), previous.value().wheelRevolutionsCount(), UintUtils.UINT32_MAX);

        Distance distance = wheelCircumference.multipliedBy(wheelDiff);
        Distance distanceOverall = distance;
        if (aggregatedValue != null) {
            distanceOverall = distance.plus(aggregatedValue.distanceOverall);
        }
        Speed speed_mps = Speed.of(distance, timeDiff);
        aggregatedValue = new Data(distance, distanceOverall, speed_mps);
    }

    @Override
    protected void resetImmediate() {
        aggregatedValue = new Data(Distance.of(0), aggregatedValue.distanceOverall, Speed.zero());
    }

    @Override
    public void resetAggregated() {
        if (aggregatedValue != null) {
            aggregatedValue = new Data(aggregatedValue.distance, Distance.of(0), aggregatedValue.speed);
        }
    }

    @NonNull
    @Override
    protected Data getNoneValue() {
        return new Data(Distance.of(0), Distance.of(0), Speed.zero());
    }

    public void setWheelCircumference(Distance wheelCircumference) {
        this.wheelCircumference = wheelCircumference;
    }

    public record Data(
            Distance distance, //Only used for debugging
            Distance distanceOverall,
            Speed speed) {
    }
}
