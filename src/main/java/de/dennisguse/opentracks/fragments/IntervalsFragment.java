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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.databinding.IntervalListViewBinding;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsFragment extends Fragment {

    private static final String TAG = IntervalsFragment.class.getSimpleName();

    private static final String FROM_TOP_TO_BOTTOM_KEY = "fromTopToBottom";
    private static final String TRACK_ID_KEY = "trackId";

    private IntervalStatisticsModel viewModel;
    protected IntervalStatisticsAdapter.StackMode stackModeListView;
    private IntervalStatisticsModel.IntervalOption selectedInterval;

    private Track.Id trackId;
    private boolean metricUnits;
    private IntervalStatisticsAdapter adapter;
    private ArrayAdapter<IntervalStatisticsModel.IntervalOption> spinnerAdapter;

    private SharedPreferences sharedPreferences;

    private boolean isReportSpeed;

    private IntervalListViewBinding viewBinding;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            updateIntervals(PreferencesUtils.isMetricUnits(sharedPreferences, getContext()), selectedInterval);
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
    public static Fragment newInstance(Track.Id trackId, boolean fromTopToBottom) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FROM_TOP_TO_BOTTOM_KEY, fromTopToBottom);
        bundle.putParcelable(TRACK_ID_KEY, trackId);
        IntervalsFragment intervalsFragment = new IntervalsFragment();
        intervalsFragment.setArguments(bundle);
        return intervalsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stackModeListView = getArguments().getBoolean(FROM_TOP_TO_BOTTOM_KEY, true) ? IntervalStatisticsAdapter.StackMode.STACK_FROM_TOP : IntervalStatisticsAdapter.StackMode.STACK_FROM_BOTTOM;
        trackId = getArguments().getParcelable(TRACK_ID_KEY);
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
                updateIntervals(metricUnits, IntervalStatisticsModel.IntervalOption.values()[i]);
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

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getContext());
        Track track = contentProviderUtils.getTrack(trackId);
        if (track != null) {
            isReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), track.getCategory());
        }

        viewModel = new ViewModelProvider(getActivity()).get(IntervalStatisticsModel.class);
        loadIntervals();
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        if (viewModel != null) {
            viewModel.onPause();
        }
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
        viewBinding.intervalRate.setText(isReportSpeed ? getString(R.string.stats_speed) : getString(R.string.stats_pace));
        LiveData<List<IntervalStatistics.Interval>> liveData = viewModel.getIntervalStats(trackId, metricUnits, selectedInterval);
        liveData.observe(getActivity(), intervalList -> adapter.swapData(intervalList, metricUnits, isReportSpeed));
    }

    private synchronized void updateIntervals(boolean metricUnits, IntervalStatisticsModel.IntervalOption selectedInterval) {
        boolean update = metricUnits != this.metricUnits || !selectedInterval.sameMultiplier(this.selectedInterval);
        this.metricUnits = metricUnits;
        this.selectedInterval = selectedInterval;

        if (update && viewModel != null) {
            viewModel.update(trackId, this.metricUnits, this.selectedInterval);
        }
    }
}
