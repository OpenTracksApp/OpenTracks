package de.dennisguse.opentracks.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * This model is used to load intervals for a track.
 * It uses a default interval but it can be set from outside to manage the interval length.
 */
public class IntervalStatisticsModel extends AndroidViewModel {

    private final List<TrackPoint> trackPoints = new ArrayList<>();
    private MutableLiveData<List<IntervalStatistics.Interval>> intervalsLiveData;
    private float distanceInterval;

    public IntervalStatisticsModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<IntervalStatistics.Interval>> getIntervalStats(boolean metricUnits, @Nullable IntervalOption interval) {
        synchronized (trackPoints) {
            if (intervalsLiveData == null) {
                if (interval == null) {
                    interval = IntervalOption.OPTION_1;
                }

                intervalsLiveData = new MutableLiveData<>();
                distanceInterval = metricUnits ? (float) (interval.getValue() * UnitConversions.KM_TO_M) : (float) (interval.getValue() * UnitConversions.MI_TO_M);
                loadIntervalStatistics();
            }
            return intervalsLiveData;
        }
    }

    private void loadIntervalStatistics() {
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, distanceInterval);
        intervalsLiveData.postValue(intervalStatistics.getIntervalList());
    }

    public void add(TrackPoint trackPoint) {
        synchronized (trackPoints) {
            trackPoints.add(trackPoint);
        }
    }

    public void onNewTrackPoints() {
        synchronized (trackPoints) {
            if (intervalsLiveData != null) {
                loadIntervalStatistics();
            }
        }
    }

    public void clear() {
        synchronized (trackPoints) {
            trackPoints.clear();
        }
    }

    public void upload(boolean metricUnits, @Nullable IntervalOption interval) {
        synchronized (trackPoints) {
            if (interval == null) {
                interval = IntervalOption.OPTION_1;
            }

            distanceInterval = metricUnits ? (float) (interval.getValue() * UnitConversions.KM_TO_M) : (float) (interval.getValue() * UnitConversions.MI_TO_M);
            loadIntervalStatistics();
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
