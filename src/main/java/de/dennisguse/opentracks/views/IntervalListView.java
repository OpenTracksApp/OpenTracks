package de.dennisguse.opentracks.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

/**
 * LinearLayout view used to build a list of intervals.
 * See {@link IntervalStatisticsAdapter}.
 */
public class IntervalListView extends LinearLayout {

    protected IntervalStatisticsAdapter adapter;
    protected LinearLayout linearLayoutIntervals;
    protected Spinner spinnerIntervals;
    protected TextView spinnerIntervalsUnit;

    protected IntervalListListener listener;

    protected final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (preferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            if (spinnerIntervalsUnit != null) {
                spinnerIntervalsUnit.setText(PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile));
                listener.unitChanged();
            }
        }
    };

    public IntervalListView(Context context, IntervalListListener listener) {
        super(context);
        this.listener = listener;
        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);

        inflate(getContext(), R.layout.interval_list_view, this);
        linearLayoutIntervals = findViewById(R.id.interval_list);
        spinnerIntervals = findViewById(R.id.spinner_intervals);

        int[] intValues = Arrays.stream(IntervalStatisticsModel.IntervalOption.values()).mapToInt(i -> i.getValue()).toArray();

        spinnerIntervals.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Arrays.stream(intValues).mapToObj(String::valueOf).toArray(String[]::new)));
        spinnerIntervalsUnit = findViewById(R.id.spinner_intervals_unit);
        spinnerIntervalsUnit.setText(PreferencesUtils.isMetricUnits(getContext()) ? getContext().getString(R.string.unit_kilometer) : getContext().getString(R.string.unit_mile));

        spinnerIntervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                IntervalStatisticsModel.IntervalOption interval = IntervalStatisticsModel.IntervalOption.values()[i];
                listener.intervalChanged(interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void destroy() {
        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);
        adapter = null;
        linearLayoutIntervals = null;
        spinnerIntervals = null;
        spinnerIntervalsUnit = null;
        listener = null;
    }

    public void display(List<IntervalStatistics.Interval> intervalList) {
        if (intervalList == null) {
            return;
        }

        adapter = new IntervalStatisticsAdapter(getContext(), intervalList);
        linearLayoutIntervals.removeAllViews();
        for (int i = 0; i < adapter.getCount(); i++) {
            View intervalView = adapter.getView(i, null, linearLayoutIntervals);
            linearLayoutIntervals.addView(intervalView);
        }
    }

    public interface IntervalListListener {
        void intervalChanged(IntervalStatisticsModel.IntervalOption interval);
        void unitChanged();
    }

    /**
     * LinearLayout view used to build a list of intervals in a reverse mode, the last one will appear in the first position on the LinearLayout.
     * This class is an specialization of {@link IntervalListView} that display the views contained in the LinearLayout in a reverse mode.
     */
    public static class IntervalReverseListView extends IntervalListView {

        public IntervalReverseListView(Context context, IntervalListListener listener) {
            super(context, listener);
        }

        public void display(List<IntervalStatistics.Interval> intervalList) {
            if (intervalList == null) {
                return;
            }

            adapter = new IntervalStatisticsAdapter(getContext(), intervalList);
            linearLayoutIntervals.removeAllViews();
            for (int i = 0; i < adapter.getCount(); i++) {
                View intervalView = adapter.getView(i, null, linearLayoutIntervals);
                linearLayoutIntervals.addView(intervalView, 0);
            }
        }
    }
}
