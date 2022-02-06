package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
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
        View view;
        if (viewType == CustomLayoutFieldType.CLOCK.value()) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_clock_item, parent, false);
            return new StatisticsAdapter.ViewClockHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_generic_item, parent, false);
            return new StatisticsAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StatisticData statisticData = statisticDataList.get(position);
        if (getItemViewType(position) == CustomLayoutFieldType.CLOCK.value()) {
            StatisticsAdapter.ViewClockHolder viewHolder = (StatisticsAdapter.ViewClockHolder) holder;
            viewHolder.setData(statisticData);
        } else {
            StatisticsAdapter.ViewHolder viewHolder = (StatisticsAdapter.ViewHolder) holder;
            viewHolder.setData(statisticData);
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
        if (statisticDataList.get(position).getField().getType(context) == CustomLayoutFieldType.CLOCK) {
            return CustomLayoutFieldType.CLOCK.value();
        } else {
            return CustomLayoutFieldType.GENERIC.value();
        }
    }

    public boolean isItemWide(int position) {
        return statisticDataList.get(position).getField().isWide();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_recorded_item, parent, false);
            return new StatisticsAdapter.ViewRecordedHolder(view);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        final TextView value;
        final TextView unit;
        final TextView descMain;
        final TextView descSecondary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            value = itemView.findViewById(R.id.stats_value);
            unit = itemView.findViewById(R.id.stats_unit);
            descMain = itemView.findViewById(R.id.stats_description_main);
            descSecondary = itemView.findViewById(R.id.stats_description_secondary);
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            this.value.setText(statisticData.hasValue() ? statisticData.getValue() : context.getString(R.string.value_unknown));
            this.value.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
            this.unit.setText(statisticData.getUnit());
            this.descMain.setText(statisticData.getField().getTitle());
            this.descMain.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader);
            if (statisticData.hasDescription()) {
                this.descSecondary.setVisibility(View.VISIBLE);
                this.descSecondary.setText(statisticData.getDescription());
            } else {
                this.descSecondary.setVisibility(View.GONE);
            }
        }
    }

    private class ViewClockHolder extends RecyclerView.ViewHolder {
        final TextClock value;
        final TextView descMain;

        public ViewClockHolder(@NonNull View itemView) {
            super(itemView);
            value = itemView.findViewById(R.id.stats_clock);
            descMain = itemView.findViewById(R.id.stats_description_main);
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            this.value.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
            this.descMain.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader);
        }
    }

    private class ViewRecordedHolder extends ViewHolder {

        public ViewRecordedHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setData(StatisticData statisticData) {
            if (statisticData == null) {
                return;
            }
            this.value.setText(statisticData.hasValue() ? statisticData.getValue() : context.getString(R.string.value_unknown));
            this.unit.setText(statisticData.getUnit());
            this.descMain.setText(statisticData.getField().getTitle());
            if (statisticData.hasDescription()) {
                this.descSecondary.setVisibility(View.VISIBLE);
                this.descSecondary.setText(statisticData.getDescription());
            } else {
                this.descSecondary.setVisibility(View.GONE);
            }
        }
    }
}
