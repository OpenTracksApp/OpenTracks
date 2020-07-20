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

    private static final int INVALID_VALUE_INT = -1;
    private static final float INVALID_VALUE_FLOAT = Float.NaN;

    private SensorDataCycling() {
    }

    public static class Cadence extends SensorData {

        private final long crankRevolutionsCount; // UINT32
        private final int crankRevolutionsTime; // UINT16; 1/1024s
        private float cadence_rpm = INVALID_VALUE_FLOAT;

        public Cadence(String sensorAddress, String sensorName, long crankRevolutionsCount, int crankRevolutionsTime) {
            super(sensorAddress, sensorName);
            this.crankRevolutionsCount = crankRevolutionsCount;
            this.crankRevolutionsTime = crankRevolutionsTime;
        }

        /**
         * Workaround for Wahoo CADENCE: provides speed instead of cadence
         */
        public Cadence(@NonNull SensorDataCycling.Speed speed) {
            this(speed.getSensorAddress(), speed.getSensorName(), speed.getWheelRevolutionsCount(), speed.getWheelRevolutionsTime());
        }

        public boolean hasData() {
            return crankRevolutionsCount != INVALID_VALUE_INT && crankRevolutionsTime != INVALID_VALUE_INT;
        }

        public long getCrankRevolutionsCount() {
            return crankRevolutionsCount;
        }

        public int getCrankRevolutionsTime() {
            return crankRevolutionsTime;
        }

        public boolean hasCadence_rpm() {
            return !Float.isNaN(cadence_rpm);
        }

        public float getCadence_rpm() {
            return cadence_rpm;
        }

        public void compute(Cadence previous) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(crankRevolutionsTime, previous.crankRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                if (timeDiff_ms <= 0) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                    cadence_rpm = INVALID_VALUE_FLOAT;
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
            return "cadence=" + getCadence_rpm() + " time=" + getCrankRevolutionsTime() + " count=" + getCrankRevolutionsCount();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Cadence)) return false;

            Cadence comp = (Cadence) obj;
            return getCrankRevolutionsCount() == comp.getCrankRevolutionsCount() && getCrankRevolutionsTime() == comp.getCrankRevolutionsTime();
        }
    }

    public static class Speed extends SensorData {

        private final int wheelRevolutionsCount; // UINT16
        private final int wheelRevolutionsTime; // UINT16; 1/1024s
        private float speed_mps = INVALID_VALUE_FLOAT;

        public Speed(String sensorAddress, String sensorName, int wheelRevolutionsCount, int wheelRevolutionsTime) {
            super(sensorAddress, sensorName);
            this.wheelRevolutionsCount = wheelRevolutionsCount;
            this.wheelRevolutionsTime = wheelRevolutionsTime;
        }

        public boolean hasData() {
            return wheelRevolutionsCount != INVALID_VALUE_INT && wheelRevolutionsTime != INVALID_VALUE_INT;
        }

        public int getWheelRevolutionsCount() {
            return wheelRevolutionsCount;
        }

        public int getWheelRevolutionsTime() {
            return wheelRevolutionsTime;
        }

        public boolean hasSpeed_mps() {
            return !Float.isNaN(speed_mps);
        }

        public float getSpeed_mps() {
            return speed_mps;
        }

        public void compute(Speed previous, int wheel_circumference_mm) {
            if (hasData() && previous != null && previous.hasData()) {
                float timeDiff_ms = UintUtils.diff(wheelRevolutionsTime, previous.wheelRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * UnitConversions.S_TO_MS;
                if (timeDiff_ms <= 0) {
                    Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                    speed_mps = INVALID_VALUE_FLOAT;
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
            return "speed=" + getSpeed_mps() + " time=" + getWheelRevolutionsTime() + " count=" + getWheelRevolutionsCount();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Speed)) return false;

            Speed comp = (Speed) obj;
            return getWheelRevolutionsCount() == comp.getWheelRevolutionsCount() && getWheelRevolutionsTime() == comp.getWheelRevolutionsTime();
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

