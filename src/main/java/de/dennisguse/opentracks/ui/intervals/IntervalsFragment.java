package de.dennisguse.opentracks.ui.intervals;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.IntervalListViewBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsFragment extends Fragment {

    private static final String TAG = IntervalsFragment.class.getSimpleName();

    private static final String FROM_TOP_TO_BOTTOM_KEY = "fromTopToBottom";
    private static final String TRACK_ID_KEY = "trackId";
    private static final String SELECTED_INTERVAL_KEY = "selectedIntervalKey";

    private IntervalStatisticsModel viewModel;
    protected IntervalStatisticsAdapter.StackMode stackModeListView;
    private IntervalStatisticsModel.IntervalOption selectedInterval;

    private Track.Id trackId;
    private boolean metricUnits;
    private IntervalStatisticsAdapter adapter;
    private ArrayAdapter<IntervalStatisticsModel.IntervalOption> intervalsAdapter;

    private boolean isReportSpeed;

    private IntervalListViewBinding viewBinding;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key) || PreferencesUtils.isKey(R.string.stats_rate_key, key)) {
            updateIntervals(PreferencesUtils.isMetricUnits(), selectedInterval);
            if (intervalsAdapter != null) {
                intervalsAdapter.notifyDataSetChanged();
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
        if (savedInstanceState != null) {
            selectedInterval = (IntervalStatisticsModel.IntervalOption) savedInstanceState.getSerializable(SELECTED_INTERVAL_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_INTERVAL_KEY, selectedInterval);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = IntervalListViewBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new IntervalStatisticsAdapter(getContext(), stackModeListView, metricUnits, isReportSpeed);
        viewBinding.intervalList.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO handle empty view: before we did viewBinding.intervalList.setEmptyView(viewBinding.intervalListEmptyView);
        viewBinding.intervalList.setAdapter(adapter);

        intervalsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, IntervalStatisticsModel.IntervalOption.values()) {
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

        viewBinding.intervalsDropdown.setAdapter(intervalsAdapter);
        viewBinding.intervalsDropdown.setOnItemClickListener((parent, view1, position, id) -> updateIntervals(metricUnits, IntervalStatisticsModel.IntervalOption.values()[position]));

        viewBinding.intervalsDropdown.setText(
                getContext().getString(R.string.value_integer_kilometer, Integer.parseInt(selectedInterval != null ? selectedInterval.toString() : IntervalStatisticsModel.IntervalOption.values()[0].toString())),
                false
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getContext());
        Track track = contentProviderUtils.getTrack(trackId);
        if (track != null) {
            isReportSpeed = PreferencesUtils.isReportSpeed(track.getCategory());
        }

        viewModel = new ViewModelProvider(getActivity()).get(IntervalStatisticsModel.class);
        loadIntervals();
    }

    @Override
    public void onPause() {
        super.onPause();

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

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
        boolean update = metricUnits != this.metricUnits
                || selectedInterval == null
                || !selectedInterval.sameMultiplier(this.selectedInterval);
        this.metricUnits = metricUnits;
        this.selectedInterval = selectedInterval;

        if (update && viewModel != null) {
            viewModel.update(trackId, this.metricUnits, this.selectedInterval);
        }
    }
}
