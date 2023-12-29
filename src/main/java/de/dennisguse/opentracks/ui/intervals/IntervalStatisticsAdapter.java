package de.dennisguse.opentracks.ui.intervals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.databinding.IntervalStatsListItemBinding;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;

public class IntervalStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<IntervalStatistics.Interval> intervalList;
    private final Context context;
    private final StackMode stackMode;
    private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();
    private boolean isReportSpeed;

    public IntervalStatisticsAdapter(Context context, StackMode stackMode, UnitSystem unitSystem, boolean isReportSpeed) {
        this.unitSystem = unitSystem;
        this.context = context;
        this.stackMode = stackMode;
        this.isReportSpeed = isReportSpeed;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IntervalStatisticsAdapter.ViewHolder(IntervalStatsListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
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
        viewHolder.viewBinding.intervalItemDistance.setText(DistanceFormatter.Builder()
                .setUnit(unitSystem)
                .build(context).formatDistance(sumDistance));

        SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(isReportSpeed).build(context);
        viewHolder.viewBinding.intervalItemRate.setText(formatter.formatSpeed(interval.getSpeed()));

        viewHolder.viewBinding.intervalItemGain.setText(StringUtils.formatAltitude(context, interval.getGain_m(), unitSystem));
        viewHolder.viewBinding.intervalItemLoss.setText(StringUtils.formatAltitude(context, interval.getLoss_m(), unitSystem));
    }

    @Override
    public int getItemCount() {
        if (intervalList == null) {
            return 0;
        }
        return intervalList.size();
    }

    public List<IntervalStatistics.Interval> swapData(List<IntervalStatistics.Interval> data, UnitSystem unitSystem, boolean isReportSpeed) {
        this.unitSystem = unitSystem;
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
        final IntervalStatsListItemBinding viewBinding;

        public ViewHolder(@NonNull IntervalStatsListItemBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
        }
    }
}
