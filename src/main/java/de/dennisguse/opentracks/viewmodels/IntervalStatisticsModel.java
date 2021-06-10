package de.dennisguse.opentracks.viewmodels;

import android.app.Application;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * This model is used to load intervals for a track.
 * It uses a default interval but it can be set from outside to manage the interval length.
 */
public class IntervalStatisticsModel extends AndroidViewModel {

    private static final String TAG = IntervalStatisticsModel.class.getSimpleName();

    private MutableLiveData<List<IntervalStatistics.Interval>> intervalsLiveData;
    private IntervalStatistics intervalStatistics;
    private Distance distanceInterval;
    private final Distance minGPSDistance;
    private final ContentResolver contentResolver;
    private ContentObserver trackPointsTableObserver;
    private TrackPoint.Id lastTrackPointId;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private HandlerThread handlerThread;
    private Handler handler;

    public IntervalStatisticsModel(@NonNull Application application) {
        super(application);
        minGPSDistance = PreferencesUtils.getRecordingDistanceInterval(PreferencesUtils.getSharedPreferences(application), application);
        contentResolver = getApplication().getContentResolver();
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (trackPointsTableObserver != null) {
            contentResolver.unregisterContentObserver(trackPointsTableObserver);
            trackPointsTableObserver = null;
        }
        if (handlerThread != null) {
            handlerThread.getLooper().quit();
            handlerThread = null;
        }
        handler = null;
    }

    public MutableLiveData<List<IntervalStatistics.Interval>> getIntervalStats(Track.Id trackId, boolean metricUnits, @Nullable IntervalOption interval) {
        if (intervalsLiveData == null) {
            if (interval == null) {
                interval = IntervalOption.OPTION_1;
            }

            intervalsLiveData = new MutableLiveData<>();
            distanceInterval = interval.getDistance(metricUnits);
            intervalStatistics = new IntervalStatistics(distanceInterval, minGPSDistance);

            loadIntervalStatistics(trackId);
        }

        trackPointsTableObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                loadIntervalStatistics(trackId);
            }
        };
        contentResolver.registerContentObserver(TrackPointsColumns.CONTENT_URI_BY_TRACKID, false, trackPointsTableObserver);

        return intervalsLiveData;
    }

    private void loadIntervalStatistics(Track.Id trackId) {
        executor.execute(() -> {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getApplication());
            try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, lastTrackPointId)) {
                lastTrackPointId = intervalStatistics.addTrackPoints(trackPointIterator);
                intervalsLiveData.postValue(intervalStatistics.getIntervalList());
            }
        });
    }

    public void onPause() {
        if (trackPointsTableObserver != null) {
            contentResolver.unregisterContentObserver(trackPointsTableObserver);
        }
    }

    public void update(Track.Id trackId, boolean metricUnits, @Nullable IntervalOption interval) {
        if (interval == null) {
            interval = IntervalOption.OPTION_1;
        }

        lastTrackPointId = null;
        distanceInterval = interval.getDistance(metricUnits);
        intervalStatistics = new IntervalStatistics(distanceInterval, minGPSDistance);
        loadIntervalStatistics(trackId);
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

        private final int multiplier;

        IntervalOption(int multiplier) {
            this.multiplier = multiplier;
        }

        public Distance getDistance(boolean metricUnits) {
            return Distance
                    .one(metricUnits)
                    .multipliedBy(multiplier);
        }

        public boolean sameMultiplier(IntervalOption intervalOption) {
            return intervalOption != null && this.multiplier == intervalOption.multiplier;
        }

        @Override
        public String toString() {
            return "" + multiplier; //TODO Somehow IntervalsFragment relies on a parsable Integer.
        }
    }
}
