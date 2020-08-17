package de.dennisguse.opentracks.views;

import android.content.Context;
import android.view.View;

import java.util.List;

import de.dennisguse.opentracks.adapters.IntervalStatisticsAdapter;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

/**
 * LinearLayout view used to build a list of intervals in a reverse mode, the last one will appear in the first position on the LinearLayout.
 * This class is an specialization of {@link IntervalListView} that display the views contained in the LinearLayout in a reverse mode.
 */
public class IntervalReverseListView extends IntervalListView {

    public IntervalReverseListView(Context context, IntervalListListener listener) {
        super(context, listener);
    }

    public void display(List<IntervalStatistics.Interval> intervalList) {
        if (intervalList != null) {
            adapter = new IntervalStatisticsAdapter(getContext(), intervalList);
            linearLayoutIntervals.removeAllViews();
            for (int i = 0; i < adapter.getCount(); i++) {
                View intervalView = adapter.getView(i, null, linearLayoutIntervals);
                linearLayoutIntervals.addView(intervalView, 0);
            }
        }
    }
}
