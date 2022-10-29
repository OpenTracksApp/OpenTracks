package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.StatsClockItemBinding;
import de.dennisguse.opentracks.databinding.StatsGenericItemBinding;
import de.dennisguse.opentracks.databinding.StatsRecordedItemBinding;
import de.dennisguse.opentracks.ui.customRecordingLayout.CustomLayoutFieldType;
import de.dennisguse.opentracks.viewmodels.StatisticData;

public class StatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<StatisticData> statisticDataList;
    private final Context context;

    public StatisticsAdapter(Context context) {
        this.context = context;
        this.statisticDataList = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == CustomLayoutFieldType.CLOCK.value()) {
            return new ViewClockHolder(StatsClockItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new GenericViewHolder(StatsGenericItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StatisticData statisticData = statisticDataList.get(position);
        if (getItemViewType(position) == CustomLayoutFieldType.CLOCK.value()) {
            StatisticsAdapter.ViewClockHolder viewHolder = (StatisticsAdapter.ViewClockHolder) holder;
            viewHolder.setData(statisticData);
        } else {
            GenericViewHolder genericViewHolder = (GenericViewHolder) holder;
            genericViewHolder.setData(statisticData);
        }
    }

    @Override
    public int getItemCount() {
        if (statisticDataList == null) {
            return 0;
        } else {
            return statisticDataList.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (statisticDataList.get(position).getType(context) == CustomLayoutFieldType.CLOCK) {
            return CustomLayoutFieldType.CLOCK.value();
        } else {
            return CustomLayoutFieldType.GENERIC.value();
        }
    }

    public boolean isItemWide(int position) {
        return statisticDataList.get(position).isWide();
    }

    public List<StatisticData> swapData(List<StatisticData> data) {
        if (statisticDataList == data) {
            return null;
        }

        statisticDataList = data;

        if (data != null) {
            this.notifyDataSetChanged();
        }

        return data;
    }

    public static class WithRecordedLayout extends StatisticsAdapter {
        public WithRecordedLayout(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewRecordedHolder(StatsRecordedItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    private class GenericViewHolder extends RecyclerView.ViewHolder {

        private final StatsGenericItemBinding itemBinding;

        public GenericViewHolder(@NonNull StatsGenericItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            itemBinding.statsValue.setText(statisticData.hasValue() ? statisticData.getValue() : context.getString(R.string.value_unknown));
            itemBinding.statsValue.setTextAppearance(context, statisticData.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);

            itemBinding.statsUnit.setText(statisticData.getUnit());

            itemBinding.statsDescriptionMain.setText(statisticData.getTitle());
            itemBinding.statsDescriptionMain.setTextAppearance(context, statisticData.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader);

            if (statisticData.hasDescription()) {
                itemBinding.statsDescriptionSecondary.setVisibility(View.VISIBLE);
                itemBinding.statsDescriptionSecondary.setText(statisticData.getDescription());
            } else {
                itemBinding.statsDescriptionSecondary.setVisibility(View.GONE);
            }
        }
    }

    private class ViewClockHolder extends RecyclerView.ViewHolder {

        private final StatsClockItemBinding itemView;

        public ViewClockHolder(@NonNull StatsClockItemBinding itemView) {
            super(itemView.getRoot());
            this.itemView = itemView;
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            itemView.statsClock.setTextAppearance(context, statisticData.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
            itemView.statsDescriptionMain.setTextAppearance(context, statisticData.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader);
        }
    }

    private class ViewRecordedHolder extends RecyclerView.ViewHolder {

        private final StatsRecordedItemBinding itemBinding;

        public ViewRecordedHolder(@NonNull StatsRecordedItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            itemBinding.statsValue.setText(statisticData.hasValue() ? statisticData.getValue() : context.getString(R.string.value_unknown));
            itemBinding.statsUnit.setText(statisticData.getUnit());
            itemBinding.statsDescriptionMain.setText(statisticData.getTitle());
            if (statisticData.hasDescription()) {
                itemBinding.statsDescriptionSecondary.setVisibility(View.VISIBLE);
                itemBinding.statsDescriptionSecondary.setText(statisticData.getDescription());
            } else {
                itemBinding.statsDescriptionSecondary.setVisibility(View.GONE);
            }
        }
    }
}
