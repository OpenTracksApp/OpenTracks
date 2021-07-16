package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

public class IntervalStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<IntervalStatistics.Interval> intervalList;
    private final Context context;
    private final StackMode stackMode;
    private boolean metricUnits;
    private boolean isReportSpeed;

    public IntervalStatisticsAdapter(Context context, StackMode stackMode, boolean metricUnits, boolean isReportSpeed) {
        this.metricUnits = metricUnits;
        this.context = context;
        this.stackMode = stackMode;
        this.isReportSpeed = isReportSpeed;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.interval_stats_list_item, parent, false);
        return new IntervalStatisticsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int actualPosition = stackMode == StackMode.STACK_FROM_TOP ? position : getItemCount() - 1 - position;
        int nextPosition = actualPosition + 1;
        boolean isLast = actualPosition == getItemCount() - 1;
        IntervalStatisticsAdapter.ViewHolder viewHolder = (IntervalStatisticsAdapter.ViewHolder) holder;
        IntervalStatistics.Interval interval = intervalList.get(actualPosition);
        viewHolder.itemView.setTag(actualPosition);

        Distance sumDistance;
        if (isLast && actualPosition > 0) {
            sumDistance = intervalList.get(actualPosition - 1).getDistance()
                    .multipliedBy(actualPosition)
                    .plus(interval.getDistance());
        } else {
            sumDistance = interval.getDistance().multipliedBy(nextPosition);
        }
        viewHolder.distance.setText(StringUtils.formatDistance(context, sumDistance, metricUnits));

        viewHolder.rate.setText(StringUtils.formatSpeed(context, interval.getSpeed(), metricUnits, isReportSpeed));

        viewHolder.gain.setText(StringUtils.formatAltitude(context, interval.getGain_m(), metricUnits));
        viewHolder.loss.setText(StringUtils.formatAltitude(context, interval.getLoss_m(), metricUnits));

    }

    @Override
    public int getItemCount() {
        if (intervalList == null) {
            return 0;
        }
        return intervalList.size();
    }

    public List<IntervalStatistics.Interval> swapData(List<IntervalStatistics.Interval> data, boolean metricUnits, boolean isReportSpeed) {
        this.metricUnits = metricUnits;
        this.isReportSpeed = isReportSpeed;
        intervalList = data;

        if (data != null) {
            this.notifyDataSetChanged();
        }

        return data;
    }

    /**
     * Defines the two modes of list items stacking: from top or from bottom.
     */
    public enum StackMode {
        STACK_FROM_BOTTOM,
        STACK_FROM_TOP
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView distance;
        final TextView rate;
        final TextView gain;
        final TextView loss;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            distance = itemView.findViewById(R.id.interval_item_distance);
            rate = itemView.findViewById(R.id.interval_item_rate);
            gain = itemView.findViewById(R.id.interval_item_gain);
            loss = itemView.findViewById(R.id.interval_item_loss);
        }
    }
}
