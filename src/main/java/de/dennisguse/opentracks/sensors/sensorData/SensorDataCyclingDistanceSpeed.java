package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.UintUtils;

public class SensorDataCyclingDistanceSpeed extends SensorData<BluetoothHandlerCyclingDistanceSpeed.WheelData, SensorDataCyclingDistanceSpeed.Data> {

    private final String TAG = SensorDataCyclingDistanceSpeed.class.getSimpleName();

    private Distance wheelCircumference;

    public SensorDataCyclingDistanceSpeed(String sensorAddress) {
        super(sensorAddress);
    }

    public SensorDataCyclingDistanceSpeed(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
    }

    @Override
    protected void computeValue(Raw<BluetoothHandlerCyclingDistanceSpeed.WheelData> current) {
        if (previous != null) {
            float timeDiff_ms = UintUtils.diff(current.value().wheelRevolutionsTime(), previous.value().wheelRevolutionsTime(), UintUtils.UINT16_MAX) / 1024f * 1000;
            Duration timeDiff = Duration.ofMillis((long) timeDiff_ms);
            if (timeDiff.isZero() || timeDiff.isNegative()) {
                Log.e(TAG, "Timestamps difference is invalid: cannot compute speed.");
                value = null;
                return;
            }

            if (current.value().wheelRevolutionsCount() < previous.value().wheelRevolutionsCount()) {
                Log.e(TAG, "Wheel revolutions count difference is invalid: cannot compute speed.");
                return;
            }
            long wheelDiff = UintUtils.diff(current.value().wheelRevolutionsCount(), previous.value().wheelRevolutionsCount(), UintUtils.UINT32_MAX);

            Distance distance = wheelCircumference.multipliedBy(wheelDiff);
            Distance distanceOverall = distance;
            if (value != null) {
                distanceOverall = distance.plus(value.distanceOverall);
            }
            Speed speed_mps = Speed.of(distance, timeDiff);
            value = new Data(distance, distanceOverall, speed_mps);
        }
    }

    @Override
    public void reset() {
        if (value != null) {
            value = new Data(value.distance, Distance.of(0), value.speed);
        }
    }

    @NonNull
    @Override
    protected Data getNoneValue() {
        if (value != null) {
            return new Data(value.distance, value.distanceOverall, Speed.zero());
        } else {
            return new Data(Distance.of(0), Distance.of(0), Speed.zero());
        }
    }

    public void setWheelCircumference(Distance wheelCircumference) {
        this.wheelCircumference = wheelCircumference;
    }

    public record Data(Distance distance, Distance distanceOverall, Speed speed) {}
}
