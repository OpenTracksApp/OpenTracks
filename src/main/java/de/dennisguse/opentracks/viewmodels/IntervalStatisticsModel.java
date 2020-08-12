package de.dennisguse.opentracks.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

public class IntervalStatisticsModel extends AndroidViewModel {

    private MutableLiveData<IntervalStatistics> intervalStats;
    private IntervalOption interval;

    public IntervalStatisticsModel(@NonNull Application application) {
        super(application);
        interval = IntervalOption.OPTION_1;
    }

    public LiveData<IntervalStatistics> getIntervalStats(long trackId, IntervalOption interval) {
        if (intervalStats == null || this.interval != interval) {
            intervalStats = new MutableLiveData<>();
            this.interval = interval;
            loadIntervalStats(trackId);
        }
        return intervalStats;
    }

    public LiveData<IntervalStatistics> getIntervalStats(long trackId) {
        return getIntervalStats(trackId, interval);
    }

    private void loadIntervalStats(long trackId) {
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

    public enum IntervalOption {
        OPTION_1(1),
        OPTION_2(2),
        OPTION_3(3),
        OPTION_4(4),
        OPTION_5(5),
        OPTION_10(10);

        private int value;

        IntervalOption(int value) {
            this.value = value;
        }

        /**
         * @param pos position of the interval option.
         * @return    the interval option that is in the position pos.
         */
        public static IntervalOption getIntervalOption(int pos) {
            if (values().length > pos) {
                return values()[pos];
            } else {
                return OPTION_1;
            }
        }

        public int getValue() {
            return value;
        }

        /**
         * @return a string array with all options.
         */
        public static String[] getAllValues() {
            IntervalOption[] options = values();
            String[] values = new String[options.length];

            for (int i = 0; i < options.length; i++) {
                values[i] = String.valueOf(options[i].getValue());
            }

            return values;
        }
    }
}
