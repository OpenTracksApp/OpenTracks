package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_item, parent, false);
        return new StatisticsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StatisticsAdapter.ViewHolder viewHolder = (StatisticsAdapter.ViewHolder) holder;
        StatisticData statisticData = statisticDataList.get(position);
        viewHolder.setData(statisticData);
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
        return statisticDataList.get(position).getField().isWide() ? CustomLayoutFieldType.WIDE.value() : CustomLayoutFieldType.SHORT.value();
    }

    public boolean isItemWide(int position) {
        return getItemViewType(position) == CustomLayoutFieldType.WIDE.value();
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
            this.value.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.StatsPrimaryValue : R.style.StatsSecondaryValue);
            this.unit.setText(statisticData.getUnit());
            this.descMain.setText(statisticData.getField().getTitle());
            this.descMain.setTextAppearance(context, statisticData.getField().isPrimary() ? R.style.StatsPrimaryLabel : R.style.StatsSecondaryLabel);
            if (statisticData.hasDescription()) {
                this.descSecondary.setVisibility(View.VISIBLE);
                this.descSecondary.setText(statisticData.getDescription());
            } else {
                this.descSecondary.setVisibility(View.GONE);
            }
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
