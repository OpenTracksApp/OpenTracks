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

    private boolean metricUnits;

    public IntervalStatisticsAdapter(Context context, List<IntervalStatistics.Interval> intervalList) {
        super(context, R.layout.interval_stats_list_item, intervalList);
        metricUnits = PreferencesUtils.isMetricUnits(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View intervalView, @NonNull ViewGroup parent) {
        IntervalStatistics.Interval interval = getItem(position);
        ViewHolder viewHolder;

        if (intervalView == null) {
            viewHolder = new ViewHolder();

            intervalView = LayoutInflater.from(getContext()).inflate(R.layout.interval_stats_list_item, parent, false);

            viewHolder.distance = intervalView.findViewById(R.id.interval_item_distance);
            viewHolder.speed = intervalView.findViewById(R.id.interval_item_speed);
            viewHolder.pace = intervalView.findViewById(R.id.interval_item_pace);

            intervalView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) intervalView.getTag();
        }

        viewHolder.distance.setText(StringUtils.formatDistance(getContext(), interval.getDistance(), metricUnits));
        Pair<String, String> speedParts = StringUtils.getSpeedParts(getContext(), interval.getSpeed(), metricUnits, true);
        viewHolder.speed.setText(speedParts.first + " " + speedParts.second);
        viewHolder.pace.setText(StringUtils.getSpeedParts(getContext(), interval.getSpeed(), metricUnits, false).first);

        return intervalView;
    }

    private static class ViewHolder {
        private TextView distance;
        private TextView speed;
        private TextView pace;
    }
}
