package de.dennisguse.opentracks.fragments;

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
import de.dennisguse.opentracks.stats.SensorStatistics;

public class SensorStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private SensorStatistics statistics;
    private List<Stat> statList;

    public void swapData(Context ctx, SensorStatistics statistics) {
        this.statistics = statistics;

        statList = new ArrayList<Stat>();
        // Heart Rate
        if (statistics.hasHeartRate()) {
            Stat maxHr = new Stat();
            maxHr.description_main = ctx.getString(R.string.sensor_state_heart_rate_max);
            maxHr.value = String.valueOf(Math.round(statistics.getMaxHeartRate().getBPM()));
            maxHr.unit = ctx.getString(R.string.sensor_state_heart_rate_unit);

            Stat avgHr = new Stat();
            avgHr.description_main = ctx.getString(R.string.sensor_state_heart_rate_avg);
            avgHr.value = String.valueOf(Math.round(statistics.getAvgHeartRate().getBPM()));
            avgHr.unit = ctx.getString(R.string.sensor_state_heart_rate_unit);

            statList.add(maxHr);
            statList.add(avgHr);
        }
        // Cadence
        if (statistics.hasCadence()) {
            Stat maxCadence = new Stat();
            maxCadence.description_main = ctx.getString(R.string.sensor_state_cadence_max);
            maxCadence.value = String.valueOf(Math.round(statistics.getMaxCadence().getRPM()));
            maxCadence.unit = ctx.getString(R.string.sensor_state_cadence_unit);

            Stat avgCadence = new Stat();
            avgCadence.description_main = ctx.getString(R.string.sensor_state_cadence_avg);
            avgCadence.value = String.valueOf(Math.round(statistics.getAvgCadence().getRPM()));
            avgCadence.unit = ctx.getString(R.string.sensor_state_cadence_unit);

            statList.add(maxCadence);
            statList.add(avgCadence);
        }
        // Power
        if (statistics.hasPower()) {
            Stat avgPower = new Stat();
            avgPower.description_main = ctx.getString(R.string.sensor_state_power_avg);
            avgPower.value = String.valueOf(Math.round(statistics.getAvgPower().getW()));
            avgPower.unit = ctx.getString(R.string.sensor_state_power_unit);

            statList.add(avgPower);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_recorded_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;

        Stat stat = statList.get(position);
        viewHolder.description_main.setText(stat.description_main);
        viewHolder.value.setText(stat.value);
        viewHolder.unit.setText(stat.unit);
        viewHolder.description_secondary.setText("");
    }

    @Override
    public int getItemCount() {
        if (statList == null) {
            return 0;
        }
        return statList.size();
    }

    private class Stat {
        public String description_main;
        public String value;
        public String unit;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView description_main;
        final TextView value;
        final TextView unit;
        final TextView description_secondary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            description_main = itemView.findViewById(R.id.stats_description_main);
            value = itemView.findViewById(R.id.stats_value);
            unit = itemView.findViewById(R.id.stats_unit);
            description_secondary = itemView.findViewById(R.id.stats_description_secondary);
        }
    }
}
