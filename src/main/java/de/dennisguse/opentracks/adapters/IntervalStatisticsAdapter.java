package de.dennisguse.opentracks.adapters;

import android.content.Context;
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

    private final StackMode stackMode;
    private final boolean metricUnits;
    private final String category;

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
            viewHolder.loss = intervalView.findViewById(R.id.interval_item_loss);

            intervalView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) intervalView.getTag();
        }

        float sumDistance_m;
        if (actualPosition + 1 == getCount() && actualPosition > 0) {
            sumDistance_m = actualPosition * getItem(actualPosition - 1).getDistance_m() + interval.getDistance_m();
        } else {
            sumDistance_m = (actualPosition + 1) * interval.getDistance_m();
        }
        viewHolder.distance.setText(StringUtils.formatDistance(getContext(), sumDistance_m, metricUnits));

        if (PreferencesUtils.isReportSpeed(getContext(), category)) {
            viewHolder.rate.setText(StringUtils.formatSpeed(getContext(), interval.getSpeed_ms(), metricUnits, true));
        } else {
            viewHolder.rate.setText(StringUtils.formatSpeed(getContext(), interval.getSpeed_ms(), metricUnits, false));
        }

        viewHolder.gain.setText(StringUtils.formatDistance(getContext(), interval.getGain_m(), metricUnits));
        viewHolder.loss.setText(StringUtils.formatDistance(getContext(), interval.getLoss_m(), metricUnits));

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
        private TextView loss;
    }
}
