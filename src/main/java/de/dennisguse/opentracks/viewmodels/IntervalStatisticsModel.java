package de.dennisguse.opentracks.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * This view model is used to load intervals for a track.
 * It uses a default interval but it can be set from outside to manage the interval length.
 */
public class IntervalStatisticsModel extends AndroidViewModel {

    private MutableLiveData<IntervalStatistics> intervalStats = new MutableLiveData<>();

    public IntervalStatisticsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<IntervalStatistics> getIntervalStats(@Nullable Track.Id trackId, @Nullable IntervalOption interval) {
        if (interval == null) {
            interval = IntervalOption.OPTION_1;
        }
        if (trackId != null) {
            loadIntervalStats(trackId, interval);
        }
        return intervalStats;
    }

    private void loadIntervalStats(final Track.Id trackId, IntervalOption interval) {
        new Thread(() -> {
            Context context = getApplication().getApplicationContext();
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
            List<TrackPoint> trackPointList = contentProviderUtils.getTrackPoints(trackId);

            IntervalStatistics intervalStatistics = new IntervalStatistics();
            float distanceInterval = PreferencesUtils.isMetricUnits(context) ? (float) (interval.getValue() * UnitConversions.KM_TO_M) : (float) (interval.getValue() * UnitConversions.MI_TO_M);
            intervalStatistics.build(trackPointList, distanceInterval);

            intervalStats.postValue(intervalStatistics);
        }).start();
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

        private int value;

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
