package de.dennisguse.opentracks.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.services.TrackRecordingService;

public class StatsDataModel extends AndroidViewModel {

    private MutableLiveData<List<StatsData>> statsData;

    public StatsDataModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<StatsData>> getStatsData() {
        if (statsData == null) {
            statsData = new MutableLiveData<>();
        }
        return statsData;
    }

    public void update(TrackRecordingService.RecordingData recordingData, Layout layout, boolean metricUnit) {
        new Thread(() -> {
            List<StatsData> statsDataList = StatsDataBuilder.fromRecordingData(getApplication(), recordingData, layout, metricUnit);
            statsData.postValue(statsDataList);
        }).start();
    }
}
