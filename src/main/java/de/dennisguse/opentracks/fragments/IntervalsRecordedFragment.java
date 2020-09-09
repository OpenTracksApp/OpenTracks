package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * A fragment to display the intervals from recorded track.
 */
public class IntervalsRecordedFragment extends Fragment {

    private static final String TAG = IntervalsRecordedFragment.class.getSimpleName();

    private static final String TRACK_ID_KEY = "trackId";

    private IntervalStatisticsModel viewModel;
    private ListView intervalListView;
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

        Fragment fragment = new IntervalsRecordedFragment();
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

        trackId = getArguments().getParcelable(TRACK_ID_KEY);

        intervalListView = view.findViewById(R.id.interval_list);
        intervalListView.setEmptyView(view.findViewById(R.id.interval_list_empty_view));

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
                adapter = new IntervalStatisticsAdapter(getContext(), intervalStatistics.getIntervalList());
                intervalListView.setAdapter(adapter);
            }
        });
    }
}