package de.dennisguse.opentracks.content.sensor;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.time.Duration;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.util.UintUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Provides cadence in rpm and speed in milliseconds from Bluetooth LE Cycling Cadence and Speed sensors.
 */
public final class SensorDataCycling {

    private static final String TAG = SensorDataCycling.class.getSimpleName();

    private SensorDataCycling() {
    }

    public static class Cadence extends SensorData<Float> {

        private final Long crankRevolutionsCount; // UINT32
        private final Integer crankRevolutionsTime; // UINT16; 1/1024s

        public Cadence(String sensorAddress) {
            super(sensorAddress);
            this.crankRevolutionsCount = null;
            this.crankRevolutionsTime = null;
        }

        public Cadence(String sensorAddress, String sensorName, long crankRevolutionsCount, int crankRevolutionsTime) {
            super(sensorAddress, sensorName);
            this.crankRevolutionsCount = crankRevolutionsCount;
            this.crankRevolutionsTime = crankRevolutionsTime;
        }

        /**
         * Workaround for Wahoo CADENCE: provides speed instead of cadence
         */
        public Cadence(@NonNull DistanceSpeed speed) {
            this(speed.getSensorAddress(), speed.getSensorName(), speed.wheelRevolutionsCount, speed.wheelRevolutionsTime);
        }

        public boolean hasData() {
            return crankRevolutionsCount != null && crankRevolutionsTime != null;
        }

        public long getCrankRevolutionsCount() {
            return crankRevolutionsCount;
        }

        public int getCrankRevolutionsTime() {
            return crankRevolutionsTime;
        }

        public void compute(Cadence previous) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(crankRevolutionsTime, previous.crankRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                if (timeDiff_ms <= 0) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                    value = null;
                } else {
                    long crankDiff = UintUtils.diff(crankRevolutionsCount, previous.crankRevolutionsCount, UintUtils.UINT32_MAX);
                    float cadence_ms = crankDiff / timeDiff_ms;
                    value = (float) (cadence_ms / UnitConversions.MS_TO_S / UnitConversions.S_TO_MIN);
                }
            }
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " cadence=" + value + " time=" + crankRevolutionsTime + " count=" + crankRevolutionsCount;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Cadence)) return false;

            Cadence comp = (Cadence) obj;
            if (hasData() && comp.hasData() == hasData()) {
                return getCrankRevolutionsCount() == comp.getCrankRevolutionsCount() && getCrankRevolutionsTime() == comp.getCrankRevolutionsTime();
            } else {
                return false;
            }
        }
    }

    public static class DistanceSpeed extends SensorData<DistanceSpeed.Data> {

        private final Long wheelRevolutionsCount; // UINT32
        private final Integer wheelRevolutionsTime; // UINT16; 1/1024s

        public DistanceSpeed(String sensorAddress) {
            super(sensorAddress);
            this.wheelRevolutionsCount = null;
            this.wheelRevolutionsTime = null;
        }

        public DistanceSpeed(String sensorAddress, String sensorName, long wheelRevolutionsCount, int wheelRevolutionsTime) {
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

        public void compute(DistanceSpeed previous, Distance wheelCircumference) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(wheelRevolutionsTime, previous.wheelRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                Duration timeDiff = Duration.ofMillis((long) timeDiff_ms);
                if (timeDiff.isZero() || timeDiff.isNegative()) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute speed.");
                    value = null;
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
            if (!(obj instanceof DistanceSpeed)) return false;

            DistanceSpeed comp = (DistanceSpeed) obj;
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

    public static class CadenceAndSpeed extends SensorData<Pair<Cadence, DistanceSpeed>> {

        public CadenceAndSpeed(String sensorAddress, String sensorName, @Nullable Cadence cadence, @Nullable DistanceSpeed distanceSpeed) {
            super(sensorAddress, sensorName);
            this.value = new Pair<>(cadence, distanceSpeed);
        }

        public Cadence getCadence() {
            return this.value != null ? this.value.first : null;
        }

        public DistanceSpeed getDistanceSpeed() {
            return this.value != null ? this.value.second : null;
        }
    }
}

