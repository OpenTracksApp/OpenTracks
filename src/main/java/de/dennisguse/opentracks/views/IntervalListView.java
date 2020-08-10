package de.dennisguse.opentracks.views;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatisticsModel;

public class IntervalListView extends LinearLayout {

    private IntervalStatisticsAdapter adapter;
    private LinearLayout linearLayoutIntervals;
    private Spinner spinnerIntervals;
    private TextView spinnerIntervalsUnit;

    private Context context;
    private IntervalListListener listener;

    public IntervalListView(Context context, IntervalListListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.interval_list_view, this);
        linearLayoutIntervals = findViewById(R.id.interval_list);
        spinnerIntervals = findViewById(R.id.spinner_intervals);
        spinnerIntervals.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, IntervalStatisticsModel.IntervalOption.getAllValues()));
        spinnerIntervalsUnit = findViewById(R.id.spinner_intervals_unit);
        spinnerIntervalsUnit.setText(PreferencesUtils.isMetricUnits(context) ? context.getString(R.string.unit_kilometer) : context.getString(R.string.unit_mile));

        spinnerIntervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                IntervalStatisticsModel.IntervalOption interval = IntervalStatisticsModel.IntervalOption.getIntervalOption(i);
                listener.intervalChanged(interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void display(List<IntervalStatistics.Interval> intervalList) {
        if (intervalList != null) {
            adapter = new IntervalStatisticsAdapter(getContext(), intervalList);
            linearLayoutIntervals.removeAllViews();
            for (int i = 0; i < adapter.getCount(); i++) {
                View intervalView = adapter.getView(i, null, linearLayoutIntervals);
                linearLayoutIntervals.addView(intervalView);
            }
        }
    }

    public interface IntervalListListener {
        void intervalChanged(IntervalStatisticsModel.IntervalOption interval);
    }
}
