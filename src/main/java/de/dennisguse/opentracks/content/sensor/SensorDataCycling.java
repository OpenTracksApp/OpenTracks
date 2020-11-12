package de.dennisguse.opentracks.content.sensor;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.util.UintUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Provides cadence in rpm and speed in milliseconds from Bluetooth LE Cycling Cadence and Speed sensors.
 * <p>
 * https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=261449
 */
public final class SensorDataCycling {

    private static final String TAG = SensorDataCycling.class.getSimpleName();

    private SensorDataCycling() {
    }

    public static class Cadence extends SensorData {

        private final Long crankRevolutionsCount; // UINT32
        private final Integer crankRevolutionsTime; // UINT16; 1/1024s
        private Float cadence_rpm;

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
        public Cadence(@NonNull SensorDataCycling.Speed speed) {
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

        public boolean hasCadence_rpm() {
            return cadence_rpm != null;
        }

        public float getCadence_rpm() {
            return cadence_rpm;
        }

        public void compute(Cadence previous) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(crankRevolutionsTime, previous.crankRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                if (timeDiff_ms <= 0) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                    cadence_rpm = null;
                } else {
                    long crankDiff = UintUtils.diff(crankRevolutionsCount, previous.crankRevolutionsCount, UintUtils.UINT32_MAX);
                    float cadence_ms = crankDiff / timeDiff_ms;
                    cadence_rpm = (float) (cadence_ms / UnitConversions.MS_TO_S / UnitConversions.S_TO_MIN);
                }
            }
        }

        @NonNull
        @Override
        public String toString() {
            return "cadence=" + cadence_rpm + " time=" + crankRevolutionsTime + " count=" + crankRevolutionsCount;
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

    public static class Speed extends SensorData {

        private final Integer wheelRevolutionsCount; // UINT16
        private final Integer wheelRevolutionsTime; // UINT16; 1/1024s
        private Float speed_mps;

        public Speed(String sensorAddress) {
            super(sensorAddress);
            this.wheelRevolutionsCount = null;
            this.wheelRevolutionsTime = null;
        }

        public Speed(String sensorAddress, String sensorName, int wheelRevolutionsCount, int wheelRevolutionsTime) {
            super(sensorAddress, sensorName);
            this.wheelRevolutionsCount = wheelRevolutionsCount;
            this.wheelRevolutionsTime = wheelRevolutionsTime;
        }

        public boolean hasData() {
            return wheelRevolutionsCount != null && wheelRevolutionsTime != null;
        }

        public int getWheelRevolutionsCount() {
            return wheelRevolutionsCount;
        }

        public int getWheelRevolutionsTime() {
            return wheelRevolutionsTime;
        }

        public boolean hasSpeed_mps() {
            return speed_mps != null;
        }

        public float getSpeed_mps() {
            return speed_mps;
        }

        public void compute(Speed previous, int wheel_circumference_mm) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(wheelRevolutionsTime, previous.wheelRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                if (timeDiff_ms <= 0) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                    speed_mps = null;
                } else {
                    long wheelDiff = UintUtils.diff(wheelRevolutionsCount, previous.wheelRevolutionsCount, UintUtils.UINT16_MAX);
                    double timeDiff_s = timeDiff_ms * UnitConversions.MS_TO_S;
                    speed_mps = (float) (wheelDiff * wheel_circumference_mm * UnitConversions.MM_TO_M / timeDiff_s);
                }
            }
        }

        @NonNull
        @Override
        public String toString() {
            return "speed=" + speed_mps + " time=" + wheelRevolutionsTime + " count=" + wheelRevolutionsCount;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Speed)) return false;

            Speed comp = (Speed) obj;
            if (hasData() && comp.hasData() == hasData()) {
                return getWheelRevolutionsCount() == comp.getWheelRevolutionsCount() && getWheelRevolutionsTime() == comp.getWheelRevolutionsTime();
            } else {
                return false;
            }
        }
    }

    public static class CadenceAndSpeed extends SensorData {

        private final Cadence cadence;
        private final Speed speed;

        public CadenceAndSpeed(String sensorAddress, String sensorName, @NonNull Cadence cadence, @NonNull Speed speed) {
            super(sensorAddress, sensorName);
            this.cadence = cadence;
            this.speed = speed;
        }

        public Cadence getCadence() {
            return cadence;
        }

        public Speed getSpeed() {
            return speed;
        }
    }
}

