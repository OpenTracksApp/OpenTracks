package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * This model is used to load intervals for a track.
 * It uses a default interval but it can be set from outside to manage the interval length.
 */
public class IntervalStatisticsModel {

    private final List<TrackPoint> trackPoints = new ArrayList<>();

    public IntervalStatistics getIntervalStats(boolean metricUnits, @Nullable IntervalOption interval) {
        synchronized (trackPoints) {
            if (interval == null) {
                interval = IntervalOption.OPTION_1;
            }

            float distanceInterval = metricUnits ? (float) (interval.getValue() * UnitConversions.KM_TO_M) : (float) (interval.getValue() * UnitConversions.MI_TO_M);
            return new IntervalStatistics(trackPoints, distanceInterval);
        }
    }

    public void add(TrackPoint trackPoint) {
        synchronized (trackPoints) {
            trackPoints.add(trackPoint);
        }
    }

    public void clear() {
        synchronized (trackPoints) {
            trackPoints.clear();
        }
    }

    /**
     * Intervals length this view model support.
     */
    public enum IntervalOption {
        OPTION_1(1),
        OPTION_2(2),
        OPTION_3(3),
        OPTION_4(4),
        OPTION_5(5),
        OPTION_10(10),
        OPTION_20(20),
        OPTION_50(50);

        private final int value;

        IntervalOption(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }
}
