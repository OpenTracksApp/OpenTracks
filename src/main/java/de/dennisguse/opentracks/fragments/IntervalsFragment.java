package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsFragment extends Fragment {

    private static final String TAG = IntervalsFragment.class.getSimpleName();

    private static final String TRACK_ID_KEY = "trackId";

    private IntervalStatisticsModel viewModel;
    private ListView intervalListView;
    protected IntervalStatisticsAdapter.StackMode stackModeListView;
    private IntervalStatisticsModel.IntervalOption selectedInterval;
    private IntervalStatisticsAdapter adapter;

    protected Spinner spinnerIntervals;
    protected TextView spinnerIntervalsUnit;

    private Track.Id trackId;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (preferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            if (spinnerIntervalsUnit != null) {
                spinnerIntervalsUnit.setText(PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile));
                intervalChanged();
            }
        }
    };

    public static Fragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        Fragment fragment = new IntervalsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.interval_list_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);

        if (savedInstanceState != null) {
            trackId = savedInstanceState.getParcelable(TRACK_ID_KEY);
        } else {
            trackId = getArguments().getParcelable(TRACK_ID_KEY);
        }

        intervalListView = view.findViewById(R.id.interval_list);
        intervalListView.setEmptyView(view.findViewById(R.id.interval_list_empty_view));

        stackModeListView = IntervalStatisticsAdapter.StackMode.STACK_FROM_TOP;

        viewModel = new ViewModelProvider(this).get(IntervalStatisticsModel.class);

        spinnerIntervals = view.findViewById(R.id.spinner_intervals);

        int[] intValues = Arrays.stream(IntervalStatisticsModel.IntervalOption.values()).mapToInt(i -> i.getValue()).toArray();

        spinnerIntervals.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Arrays.stream(intValues).mapToObj(String::valueOf).toArray(String[]::new)));
        spinnerIntervalsUnit = view.findViewById(R.id.spinner_intervals_unit);
        spinnerIntervalsUnit.setText(PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile));

        spinnerIntervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedInterval = IntervalStatisticsModel.IntervalOption.values()[i];
                intervalChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        intervalChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);

        intervalListView = null;
        adapter = null;
        spinnerIntervals = null;
        spinnerIntervalsUnit = null;
        viewModel = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TRACK_ID_KEY, trackId);
    }

    /**
     * Update intervals through {@link IntervalStatisticsModel} view model.
     */
    public void intervalChanged() {
        if (viewModel == null || intervalListView == null) {
            return;
        }

        LiveData<IntervalStatistics> liveData = viewModel.getIntervalStats(trackId, selectedInterval);
        liveData.observe(getActivity(), intervalStatistics -> {
            if (intervalStatistics != null) {
                adapter = new IntervalStatisticsAdapter(getContext(), intervalStatistics.getIntervalList(), stackModeListView);
                intervalListView.setAdapter(adapter);
            }
        });
    }

    public void setTrackId(Track.Id trackId) {
        this.trackId = trackId;
    }

    public static class IntervalsRecordingFragment extends IntervalsFragment implements TrackRecordingActivity.OnTrackIdListener {
        // Refreshing intervals stats it's not so demanding so 5 seconds is enough to balance performance and user experience.
        private static final long UI_UPDATE_INTERVAL = 5 * UnitConversions.ONE_SECOND_MS;

        private Handler intervalHandler;

        private final Runnable intervalRunner = new Runnable() {
            @Override
            public void run() {
                if (isResumed()) {
                    updateIntervals();
                    intervalHandler.postDelayed(intervalRunner, UI_UPDATE_INTERVAL);
                }
            }
        };

        public static Fragment newInstance(Track.Id trackId) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(TRACK_ID_KEY, trackId);

            Fragment fragment = new IntervalsRecordingFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ((TrackRecordingActivity) getActivity()).setTrackIdListener(this);
            intervalHandler = new Handler();
            stackModeListView = IntervalStatisticsAdapter.StackMode.STACK_FROM_BOTTOM;
        }

        @Override
        public void onResume() {
            super.onResume();
            intervalHandler.post(intervalRunner);
        }

        @Override
        public void onPause() {
            super.onPause();
            intervalHandler.removeCallbacks(intervalRunner);
        }

        private void updateIntervals() {
            intervalChanged();
        }

        @Override
        public void onTrackId(Track.Id trackId) {
            setTrackId(trackId);
        }
    }
}