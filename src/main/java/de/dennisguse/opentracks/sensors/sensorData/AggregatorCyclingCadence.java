package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingCadence;
import de.dennisguse.opentracks.sensors.UintUtils;

public class AggregatorCyclingCadence extends Aggregator<BluetoothHandlerCyclingCadence.CrankData, Cadence> {

    private final String TAG = AggregatorCyclingCadence.class.getSimpleName();

    public AggregatorCyclingCadence(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
    }

    @Override
    protected void computeValue(Raw<BluetoothHandlerCyclingCadence.CrankData> current) {
        if (previous != null) {
            float timeDiff_ms = UintUtils.diff(current.value().crankRevolutionsTime(), previous.value().crankRevolutionsTime(), UintUtils.UINT16_MAX) / 1024f * 1000;
            Duration timeDiff = Duration.ofMillis((long) timeDiff_ms);

            if (timeDiff.isZero()) {
                return;
            }
            if (timeDiff.isNegative()) {
                Log.e(TAG, "Timestamps difference is invalid: cannot compute cadence.");
                value = null;
                return;
            }

            // TODO We have to treat with overflow according to the documentation: read https://github.com/OpenTracksApp/OpenTracks/pull/953#discussion_r711625268
            if (current.value().crankRevolutionsCount() < previous.value().crankRevolutionsCount()) {
                Log.e(TAG, "Crank revolutions count difference is invalid: cannot compute cadence.");
                return;
            }

            long crankDiff = UintUtils.diff(current.value().crankRevolutionsCount(), previous.value().crankRevolutionsCount(), UintUtils.UINT32_MAX);
            value = Cadence.of(crankDiff, timeDiff);
        }
    }

    @NonNull
    @Override
    protected Cadence getNoneValue() {
        return Cadence.of(0);
    }
}
