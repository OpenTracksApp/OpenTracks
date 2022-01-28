package de.dennisguse.opentracks.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.ui.customRecordingLayout.Layout;

public class StatisticsDataModel extends AndroidViewModel {

    private MutableLiveData<List<StatisticData>> statsData;

    public StatisticsDataModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<StatisticData>> getStatsData() {
        if (statsData == null) {
            statsData = new MutableLiveData<>();
        }
        return statsData;
    }

    public void update(RecordingData recordingData, Layout layout, boolean metricUnit) {
        new Thread(() -> {
            List<StatisticData> statisticDataList = StatisticDataBuilder.fromRecordingData(getApplication(), recordingData, layout, metricUnit);
            statsData.postValue(statisticDataList);
        }).start();
    }
}
