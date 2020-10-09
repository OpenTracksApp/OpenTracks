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
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackActivityDataHubInterface;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsFragment extends Fragment implements TrackDataListener {

    private static final String TAG = IntervalsFragment.class.getSimpleName();

    private IntervalStatisticsModel viewModel;
    private ListView intervalListView;
    protected IntervalStatisticsAdapter.StackMode stackModeListView;
    private IntervalStatisticsModel.IntervalOption selectedInterval;

    private String intervalUnit;
    private IntervalStatisticsAdapter adapter;
    protected Spinner spinnerIntervals;
    private ArrayAdapter<IntervalStatisticsModel.IntervalOption> spinnerAdapter;

    private TextView rateLabel;

    private TrackDataHub trackDataHub;
    private String category;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (preferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            intervalUnit = PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                spinnerAdapter.notifyDataSetChanged();
            }
        }
    };

    public static Fragment newInstance() {
        return new IntervalsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.interval_list_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);
        intervalUnit = PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile);

        rateLabel = view.findViewById(R.id.interval_rate);

        intervalListView = view.findViewById(R.id.interval_list);
        intervalListView.setEmptyView(view.findViewById(R.id.interval_list_empty_view));

        stackModeListView = IntervalStatisticsAdapter.StackMode.STACK_FROM_TOP;

        viewModel = new IntervalStatisticsModel();

        spinnerIntervals = view.findViewById(R.id.spinner_intervals);

        spinnerAdapter = new ArrayAdapter<IntervalStatisticsModel.IntervalOption>(getContext(), android.R.layout.simple_spinner_dropdown_item, IntervalStatisticsModel.IntervalOption.values()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(v.getText() + " " + intervalUnit);
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setText(v.getText() + " " + intervalUnit);
                return v;
            }
        };
        spinnerIntervals.setAdapter(spinnerAdapter);

        spinnerIntervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedInterval = IntervalStatisticsModel.IntervalOption.values()[i];
                loadIntervals();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);

        intervalListView = null;
        adapter = null;
        spinnerIntervals = null;
        viewModel = null;
    }

    /**
     * Update intervals through {@link IntervalStatisticsModel} view model.
     */
    protected synchronized void loadIntervals() {
        if (viewModel == null || intervalListView == null) {
            return;
        }

        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext()) ? true : false;
        IntervalStatistics intervalStatistics = viewModel.getIntervalStats(metricUnits, selectedInterval);
        adapter = new IntervalStatisticsAdapter(getContext(), intervalStatistics.getIntervalList(), category, stackModeListView);
        intervalListView.setAdapter(adapter);
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackActivityDataHubInterface) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, true, false, true, true);
    }

    /**
     * Pauses the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void pauseTrackDataHub() {
        trackDataHub.unregisterTrackDataListener(this);
        trackDataHub = null;
    }

    @Override
    public void onTrackUpdated(Track track) {
        if (isResumed()) {
            // Set category.
            category = track != null ? track.getCategory() : "";

            // Set rate label.
            if (rateLabel != null) {
                boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext(), category);
                rateLabel.setText(reportSpeed ? R.string.stats_speed : R.string.stats_pace);
            }
        }
    }

    @Override
    public void clearTrackPoints() {
        if (isResumed()) {
            viewModel.clear();
        }
    }

    @Override
    public void onSampledInTrackPoint(TrackPoint trackPoint) {
        if (isResumed()) {
            viewModel.add(trackPoint);
        }
    }

    @Override
    public void onSampledOutTrackPoint(TrackPoint trackPoint) {
        if (isResumed()) {
            viewModel.add(trackPoint);
        }
    }

    @Override
    public void onNewTrackPointsDone() {
        if (isResumed()) {
            runOnUiThread(() -> loadIntervals());
        }
    }

    @Override
    public void clearMarkers() {
        // We don't care.
    }

    @Override
    public void onNewMarker(Marker marker) {
        // We don't care.
    }

    @Override
    public void onNewMarkersDone() {
        // We don't care.
    }

    /**
     * Runs a runnable on the UI thread.
     *
     * @param runnable the runnable
     */
    private void runOnUiThread(Runnable runnable) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            fragmentActivity.runOnUiThread(runnable);
        }
    }

    public static class IntervalsRecordingFragment extends IntervalsFragment {
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

        public static Fragment newInstance() {
            return new IntervalsRecordingFragment();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
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
            loadIntervals();
        }
    }
}
