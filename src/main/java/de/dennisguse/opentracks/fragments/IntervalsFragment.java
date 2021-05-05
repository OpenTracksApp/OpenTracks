package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackActivityDataHubInterface;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.databinding.IntervalListViewBinding;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsFragment extends Fragment implements TrackDataListener {

    private static final String TAG = IntervalsFragment.class.getSimpleName();

    private static final String FROM_TOP_TO_BOTTOM_KEY = "fromTopToBottom";

    private IntervalStatisticsModel viewModel;
    protected IntervalStatisticsAdapter.StackMode stackModeListView;
    private IntervalStatisticsModel.IntervalOption selectedInterval;

    private boolean metricUnits;
    private IntervalStatisticsAdapter adapter;
    private ArrayAdapter<IntervalStatisticsModel.IntervalOption> spinnerAdapter;

    private SharedPreferences sharedPreferences;

    private TrackDataHub trackDataHub;
    private boolean isReportSpeed;

    private IntervalListViewBinding viewBinding;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            metricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
            uploadIntervals();
            if (spinnerAdapter != null) {
                spinnerAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * Creates an instance of this class.
     *
     * @param  fromTopToBottom If true then the intervals are shown from top to bottom (the first interval on top). Otherwise the intervals are shown from bottom to top.
     * @return IntervalsFragment instance.
     */
    public static Fragment newInstance(boolean fromTopToBottom) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FROM_TOP_TO_BOTTOM_KEY, fromTopToBottom);
        IntervalsFragment intervalsFragment = new IntervalsFragment();
        intervalsFragment.setArguments(bundle);
        return intervalsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stackModeListView = getArguments().getBoolean(FROM_TOP_TO_BOTTOM_KEY, true) ? IntervalStatisticsAdapter.StackMode.STACK_FROM_TOP : IntervalStatisticsAdapter.StackMode.STACK_FROM_BOTTOM;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = IntervalListViewBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());

        adapter = new IntervalStatisticsAdapter(getContext(), stackModeListView, metricUnits, isReportSpeed);
        viewBinding.intervalList.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO handle empty view: before we did viewBinding.intervalList.setEmptyView(viewBinding.intervalListEmptyView);
        viewBinding.intervalList.setAdapter(adapter);

        spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, IntervalStatisticsModel.IntervalOption.values()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                if (metricUnits) {
                    v.setText(getContext().getString(R.string.value_integer_kilometer, Integer.parseInt(v.getText().toString())));
                } else {
                    v.setText(getContext().getString(R.string.value_integer_mile, Integer.parseInt(v.getText().toString())));
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };

        viewBinding.spinnerIntervals.setAdapter(spinnerAdapter);
        viewBinding.spinnerIntervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedInterval = IntervalStatisticsModel.IntervalOption.values()[i];
                uploadIntervals();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);
        viewModel = new ViewModelProvider(getActivity()).get(IntervalStatisticsModel.class);
        loadIntervals();
        resumeTrackDataHub();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sharedPreferences = null;

        adapter = null;
        viewModel = null;
    }

    /**
     * Update intervals through {@link IntervalStatisticsModel} view model.
     */
    protected synchronized void loadIntervals() {
        if (viewModel == null) {
            return;
        }
        LiveData<List<IntervalStatistics.Interval>> liveData = viewModel.getIntervalStats(metricUnits, selectedInterval);
        liveData.observe(getActivity(), intervalList -> adapter.swapData(intervalList, metricUnits, isReportSpeed));
    }

    private synchronized void uploadIntervals() {
        if (viewModel == null) {
            return;
        }
        viewModel.upload(metricUnits, selectedInterval);
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
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    // Set category.
                    String category = track != null ? track.getCategory() : "";

                    // Set rate label.
                    isReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), category);
                    viewBinding.intervalRate.setText(isReportSpeed ? R.string.stats_speed : R.string.stats_pace);
                }
            });
        }
    }

    @Override
    public void clearTrackPoints() {
        if (isResumed()) {
            viewModel.clear();
        }
    }

    @Override
    public void onSampledInTrackPoint(@NonNull TrackPoint trackPoint, @NonNull TrackStatistics unused, Speed unused2, double unused3) {
        if (isResumed()) {
            viewModel.add(trackPoint);
        }
    }

    @Override
    public void onSampledOutTrackPoint(@NonNull TrackPoint trackPoint, @NonNull TrackStatistics unused) {
        if (isResumed()) {
            viewModel.add(trackPoint);
        }
    }

    @Override
    public void onNewTrackPointsDone(@NonNull TrackPoint unused, @NonNull TrackStatistics alsoUnused) {
        if (isResumed()) {
            runOnUiThread(viewModel::onNewTrackPoints);
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
}
