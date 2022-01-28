package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.StatisticsAdapter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.customRecordingLayout.Layout;
import de.dennisguse.opentracks.viewmodels.StatisticData;
import de.dennisguse.opentracks.viewmodels.StatisticsDataModel;

/**
 * A fragment to display track statistics to the user for a currently recording {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordingFragment extends Fragment {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private RecordingData recordingData = TrackRecordingService.NOT_RECORDING;
    private TrackPoint latestTrackPoint;
    private Layout layout;

    private StatisticsRecordingBinding viewBinding;
    private StatisticsAdapter statisticsAdapter;
    private GridLayoutManager gridLayoutManager;
    private StatisticsDataModel viewModel;
    private LiveData<List<StatisticData>> statisticsLiveData;

    private boolean preferenceMetricUnits;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            updateUInecessary = true;
            preferenceMetricUnits = PreferencesUtils.isMetricUnits();
        }

        if (PreferencesUtils.isKey(R.string.stats_custom_layouts_key, key) || PreferencesUtils.isKey(R.string.stats_custom_layout_selected_layout_key, key)) {
            updateUInecessary = true;
            layout = PreferencesUtils.getCustomLayout();
            gridLayoutManager.setSpanCount(layout.getColumnsPerRow());
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(this::updateUI);
        }
    };

    private final TrackRecordingServiceConnection.Callback bindChangedCallback = service -> service.getRecordingDataObservable()
            .observe(StatisticsRecordingFragment.this, this::onRecordingDataChanged);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);

        statisticsAdapter = new StatisticsAdapter(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = viewBinding.statsRecyclerView;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), RecyclerView.VERTICAL));
        layout = PreferencesUtils.getCustomLayout();
        gridLayoutManager = new GridLayoutManager(getContext(), layout.getColumnsPerRow());
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (statisticsAdapter.isItemWide(position)) {
                    return layout.getColumnsPerRow();
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(statisticsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        viewModel = new ViewModelProvider(getActivity()).get(StatisticsDataModel.class);
        statisticsLiveData = viewModel.getStatsData();
        statisticsLiveData.observe(getActivity(), statsDataList -> statisticsAdapter.swapData(statsDataList));

        trackRecordingServiceConnection.startConnection(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
        viewModel = null;

        if (statisticsLiveData != null) {
            statisticsLiveData.removeObservers(getActivity());
        }
        statisticsLiveData = null;
    }

    private void updateUI() {
        if (isResumed()) {
            viewModel.update(recordingData, layout, preferenceMetricUnits);
        }
    }

    private void onRecordingDataChanged(RecordingData recordingData) {
        String oldCategory = this.recordingData.getTrackCategory();
        String newCategory = recordingData.getTrackCategory();
        this.recordingData = recordingData;

        if (!oldCategory.equals(newCategory)) {
            sharedPreferenceChangeListener.onSharedPreferenceChanged(null, getString(R.string.stats_rate_key));
        }

        latestTrackPoint = recordingData.getLatestTrackPoint();
        if (latestTrackPoint != null && latestTrackPoint.hasLocation() && !latestTrackPoint.isRecent()) {
            latestTrackPoint = null;
        }

        updateUI();
    }
}
