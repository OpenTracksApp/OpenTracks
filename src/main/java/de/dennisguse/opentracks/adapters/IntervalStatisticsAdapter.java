package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

public class IntervalStatisticsAdapter extends ArrayAdapter<IntervalStatistics.Interval> {

    private StackMode stackMode;
    private boolean metricUnits;
    private float sumDistance_m;
    private String category;

    public IntervalStatisticsAdapter(Context context, List<IntervalStatistics.Interval> intervalList, String category, StackMode stackMode) {
        super(context, R.layout.interval_stats_list_item, intervalList);
        metricUnits = PreferencesUtils.isMetricUnits(context);
        this.category = category;
        this.stackMode = stackMode;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View intervalView, @NonNull ViewGroup parent) {
        int actualPosition = stackMode == StackMode.STACK_FROM_TOP ? position : getCount() - 1 - position;
        IntervalStatistics.Interval interval = getItem(actualPosition);
        ViewHolder viewHolder;

        if (intervalView == null) {
            viewHolder = new ViewHolder();

            intervalView = LayoutInflater.from(getContext()).inflate(R.layout.interval_stats_list_item, parent, false);

            viewHolder.distance = intervalView.findViewById(R.id.interval_item_distance);
            viewHolder.rate = intervalView.findViewById(R.id.interval_item_rate);
            viewHolder.gain = intervalView.findViewById(R.id.interval_item_gain);

            intervalView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) intervalView.getTag();
        }

        if (actualPosition + 1 == getCount() && actualPosition > 0) {
            sumDistance_m = actualPosition * getItem(actualPosition - 1).getDistance_m() + interval.getDistance_m();
        } else {
            sumDistance_m = (actualPosition + 1) * interval.getDistance_m();
        }
        viewHolder.distance.setText(StringUtils.formatDistance(getContext(), sumDistance_m, metricUnits));

        if (PreferencesUtils.isReportSpeed(getContext(), category)) {
            Pair<String, String> speedParts = StringUtils.getSpeedParts(getContext(), interval.getSpeed_ms(), metricUnits, true);
            viewHolder.rate.setText(speedParts.first + " " + speedParts.second);
        } else {
            Pair<String, String> paceParts = StringUtils.getSpeedParts(getContext(), interval.getSpeed_ms(), metricUnits, false);
            viewHolder.rate.setText(paceParts.first + " " + paceParts.second);
        }

        Pair<String, String> gainParts = StringUtils.formatElevation(getContext(), interval.getGain_m(), metricUnits);
        viewHolder.gain.setText(gainParts.first + " " + gainParts.second);

        return intervalView;
    }

    /**
     * Defines the two modes of list items stacking: from top or from bottom.
     */
    public enum StackMode {
        STACK_FROM_BOTTOM,
        STACK_FROM_TOP
    }

    private static class ViewHolder {
        private TextView distance;
        private TextView rate;
        private TextView gain;
    }
}
