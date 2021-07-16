package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import de.dennisguse.opentracks.adapters.SettingsCustomLayoutAdapter;
import de.dennisguse.opentracks.adapters.StatsAdapter;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.StatsData;
import de.dennisguse.opentracks.viewmodels.StatsDataModel;

import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;

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
    private TrackRecordingService.RecordingData recordingData = TrackRecordingService.NOT_RECORDING;
    private TrackPoint latestTrackPoint;
    private Layout layout;

    private StatisticsRecordingBinding viewBinding;
    private StatsAdapter statsAdapter;
    private GridLayoutManager gridLayoutManager;
    private StatsDataModel viewModel;
    private LiveData<List<StatsData>> liveData;

    private SharedPreferences sharedPreferences;
    private boolean preferenceMetricUnits;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
            updateUInecessary = true;
            preferenceMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_custom_layout_fields_key, key)) {
            updateUInecessary = true;
            layout = PreferencesUtils.getCustomLayout(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_custom_layout_columns_key, key)) {
            gridLayoutManager.setSpanCount(PreferencesUtils.getLayoutColumns(sharedPreferences, getContext()));
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(this::updateUI);
        }
    };

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            TrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
            if (service == null) {
                Log.w(TAG, "could not get TrackRecordingService");
                return;
            }

            service.getRecordingDataObservable()
                    .observe(StatisticsRecordingFragment.this, recordingData -> onRecordingDataChanged(recordingData));
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);

        statsAdapter = new StatsAdapter(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = viewBinding.statsRecyclerView;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), RecyclerView.VERTICAL));
        final int numColumns = PreferencesUtils.getLayoutColumns(sharedPreferences, getContext());
        gridLayoutManager = new GridLayoutManager(getContext(), numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (statsAdapter.getItemViewType(position) == StatsAdapter.VIEW_TYPE_LONG) {
                    return numColumns;
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(statsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        viewModel = new ViewModelProvider(getActivity()).get(StatsDataModel.class);
        liveData = viewModel.getStatsData();
        liveData.observe(getActivity(), statsDataList -> statsAdapter.swapData(statsDataList));

        trackRecordingServiceConnection.startConnection(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
        sharedPreferences = null;
        viewModel = null;
        liveData.removeObservers(getActivity());
    }

    private void updateUI() {
        if (isResumed()) {
            viewModel.update(recordingData, layout, preferenceMetricUnits);
        }
    }

    private void onRecordingDataChanged(TrackRecordingService.RecordingData recordingData) {
        String oldCategory = this.recordingData.getTrackCategory();
        String newCategory = recordingData.getTrackCategory();
        this.recordingData = recordingData;

        if (!oldCategory.equals(newCategory)) {
            sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.stats_rate_key));
        }

        latestTrackPoint = recordingData.getLatestTrackPoint();
        if (latestTrackPoint != null && latestTrackPoint.hasLocation() && !latestTrackPoint.isRecent()) {
            latestTrackPoint = null;
        }

        updateUI();
    }
}
