package de.dennisguse.opentracks.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

public class AggregatedStatisticsModel extends AndroidViewModel {

    private MutableLiveData<AggregatedStatistics> aggregatedStats;

    public AggregatedStatisticsModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<AggregatedStatistics> getAggregatedStats(@Nullable List<Track.Id> trackIds) {
        if (aggregatedStats == null) {
            aggregatedStats = new MutableLiveData<>();
            loadAggregatedStats(trackIds);
        }
        return aggregatedStats;
    }

    private void loadAggregatedStats(@Nullable List<Track.Id> trackIds) {
        new Thread(() -> {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getApplication().getApplicationContext());
            List<Track> tracks = contentProviderUtils.getTracks(trackIds);

            AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

            aggregatedStats.postValue(aggregatedStatistics);
        }).start();
    }
}
