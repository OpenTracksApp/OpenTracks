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
import de.dennisguse.opentracks.viewmodels.StatsData;

public class StatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_SHORT = 1;
    public static final int VIEW_TYPE_LONG = 2;

    private List<StatsData> statsDataList;
    private final Context context;

    public StatsAdapter(Context context) {
        this.context = context;
        this.statsDataList = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_item, parent, false);
        return new StatsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StatsAdapter.ViewHolder viewHolder = (StatsAdapter.ViewHolder) holder;
        StatsData statsData = statsDataList.get(position);
        viewHolder.setData(statsData);
    }

    @Override
    public int getItemCount() {
        if (statsDataList == null) {
            return 0;
        } else {
            return statsDataList.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return statsDataList.get(position).isLong() ? VIEW_TYPE_LONG : VIEW_TYPE_SHORT;
    }

    public List<StatsData> swapData(List<StatsData> data) {
        if (statsDataList == data) {
            return null;
        }

        statsDataList = data;

        if (data != null) {
            this.notifyDataSetChanged();
        }

        return data;
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
            descMain = itemView.findViewById(R.id.stats_desc_main);
            descSecondary = itemView.findViewById(R.id.stats_desc_secondary);
        }

        public void setData(StatsData statsData) {
            if (statsData == null) {
                return;
            }
            this.value.setText(statsData.hasValue() ? statsData.getValue() : context.getString(R.string.value_unknown));
            this.value.setTextAppearance(context, statsData.isPrimary() ? R.style.StatsPrimaryValue : R.style.StatsSecondaryValue);
            this.unit.setText(statsData.getUnit());
            this.descMain.setText(statsData.getDescMain());
            this.descMain.setTextAppearance(context, statsData.isPrimary() ? R.style.StatsPrimaryLabel : R.style.StatsSecondaryLabel);
            if (statsData.hasDescSecondary()) {
                this.descSecondary.setVisibility(View.VISIBLE);
                this.descSecondary.setText(statsData.getDescSecondary());
            } else {
                this.descSecondary.setVisibility(View.GONE);
            }
        }
    }
}
