package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.UintUtils;

public class SensorDataCyclingCadence extends SensorData<Cadence> {

    private final String TAG = SensorDataCyclingCadence.class.getSimpleName();

    private final Long crankRevolutionsCount; // UINT32
    private final Integer crankRevolutionsTime; // UINT16; 1/1024s

    public SensorDataCyclingCadence(String sensorAddress) {
        super(sensorAddress);
        this.crankRevolutionsCount = null;
        this.crankRevolutionsTime = null;
    }

    public SensorDataCyclingCadence(String sensorAddress, String sensorName, long crankRevolutionsCount, int crankRevolutionsTime) {
        super(sensorAddress, sensorName);
        this.crankRevolutionsCount = crankRevolutionsCount;
        this.crankRevolutionsTime = crankRevolutionsTime;
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

    @NonNull
    @Override
    protected Cadence getNoneValue() {
        return Cadence.of(0);
    }

    public void compute(SensorDataCyclingCadence previous) {
        if (hasData() && previous != null && previous.hasData()) {
            float timeDiff_ms = UintUtils.diff(crankRevolutionsTime, previous.crankRevolutionsTime, UintUtils.UINT16_MAX) / 1024f * 1000;
            Duration timeDiff = Duration.ofMillis((long) timeDiff_ms);
            if (timeDiff.isZero() || timeDiff.isNegative()) {
                Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                value = null;
                return;
            }

            // TODO We have to treat with overflow according to the documentation: read https://github.com/OpenTracksApp/OpenTracks/pull/953#discussion_r711625268
            if (crankRevolutionsCount < previous.crankRevolutionsCount) {
                Log.e(TAG, "Crank revolutions count difference is invalid: cannot compute cadence.");
                return;
            }

            long crankDiff = UintUtils.diff(crankRevolutionsCount, previous.crankRevolutionsCount, UintUtils.UINT32_MAX);
            value = Cadence.of(crankDiff, timeDiff);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " cadence=" + value + " time=" + crankRevolutionsTime + " count=" + crankRevolutionsCount;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SensorDataCyclingCadence comp)) return false;

        if (hasData() && comp.hasData() == hasData()) {
            return getCrankRevolutionsCount() == comp.getCrankRevolutionsCount() && getCrankRevolutionsTime() == comp.getCrankRevolutionsTime();
        } else {
            return false;
        }
    }
}
