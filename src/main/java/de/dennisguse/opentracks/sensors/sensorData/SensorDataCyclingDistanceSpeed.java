package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.UintUtils;

public class SensorDataCyclingDistanceSpeed extends SensorData<SensorDataCyclingDistanceSpeed.Data> {

    private final String TAG = SensorDataCyclingDistanceSpeed.class.getSimpleName();

    private final Long wheelRevolutionsCount; // UINT32
    private final Integer wheelRevolutionsTime; // UINT16; 1/1024s

    public SensorDataCyclingDistanceSpeed(String sensorAddress) {
        super(sensorAddress);
        this.wheelRevolutionsCount = null;
        this.wheelRevolutionsTime = null;
    }

    public SensorDataCyclingDistanceSpeed(String sensorAddress, String sensorName, long wheelRevolutionsCount, int wheelRevolutionsTime) {
        super(sensorAddress, sensorName);
        this.wheelRevolutionsCount = wheelRevolutionsCount;
        this.wheelRevolutionsTime = wheelRevolutionsTime;
    }

    public boolean hasData() {
        return wheelRevolutionsCount != null && wheelRevolutionsTime != null;
    }

    public long getWheelRevolutionsCount() {
        return wheelRevolutionsCount;
    }

    public int getWheelRevolutionsTime() {
        return wheelRevolutionsTime;
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

    public void compute(SensorDataCyclingDistanceSpeed previous, Distance wheelCircumference) {
        if (hasData() && previous != null && previous.hasData()) {
            float timeDiffMs = UintUtils.diff(wheelRevolutionsTime, previous.wheelRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * 1000;
            Duration timeDiff = Duration.ofMillis((long) timeDiffMs);
            if (timeDiff.isZero() || timeDiff.isNegative()) {
                Log.e(TAG, "Timestamps difference is invalid: cannot compute speed.");
                value = null;
                return;
            }

            if (wheelRevolutionsCount < previous.wheelRevolutionsCount) {
                Log.e(TAG, "Wheel revolutions count difference is invalid: cannot compute speed.");
                return;
            }
            long wheelDiff = UintUtils.diff(wheelRevolutionsCount, previous.wheelRevolutionsCount, UintUtils.UINT32_MAX);

            Distance distance = wheelCircumference.multipliedBy(wheelDiff);
            Distance distanceOverall = distance;
            if (previous.hasValue()) {
                distanceOverall = distance.plus(previous.getValue().distanceOverall);
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
    public String toString() {
        return super.toString() + " data=" + value + " time=" + wheelRevolutionsTime + " count=" + wheelRevolutionsCount;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SensorDataCyclingDistanceSpeed)) return false;

        SensorDataCyclingDistanceSpeed comp = (SensorDataCyclingDistanceSpeed) obj;
        if (!(hasData() && comp.hasData())) {
            return false;
        }

        return getWheelRevolutionsCount() == comp.getWheelRevolutionsCount() && getWheelRevolutionsTime() == comp.getWheelRevolutionsTime();
    }

    public static class Data {
        private final Distance distance;
        private final Distance distanceOverall;
        private final Speed speed;

        private Data(Distance distance, Distance distanceOverall, Speed speed) {
            this.distance = distance;
            this.distanceOverall = distanceOverall;
            this.speed = speed;
        }

        public Distance getDistance() {
            return distance;
        }

        public Distance getDistanceOverall() {
            return distanceOverall;
        }

        public Speed getSpeed() {
            return speed;
        }

        @NonNull
        @Override
        public String toString() {
            return "Data{" +
                    "distance=" + getDistance() +
                    ", distance_overall=" + getDistanceOverall() +
                    ", speed=" + getSpeed() +
                    '}';
        }
    }
}
