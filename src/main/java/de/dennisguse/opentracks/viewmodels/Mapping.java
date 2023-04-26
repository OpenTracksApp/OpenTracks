package de.dennisguse.opentracks.viewmodels;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import de.dennisguse.opentracks.R;

public class Mapping {

    public static Map<String, Callable<StatisticViewHolder<?>>> create(Context context) {
        HashMap<String, Callable<StatisticViewHolder<?>>> m = new HashMap<>();
        m.put(context.getString(R.string.stats_custom_layout_total_time_key), GenericStatisticsViewHolder.TotalTime::new);
        m.put(context.getString(R.string.stats_custom_layout_moving_time_key), GenericStatisticsViewHolder.MovingTime::new);

        m.put(context.getString(R.string.stats_custom_layout_distance_key), GenericStatisticsViewHolder.Distance::new);

        m.put(context.getString(R.string.stats_custom_layout_speed_key), GenericStatisticsViewHolder.SpeedVH::new);
        m.put(context.getString(R.string.stats_custom_layout_pace_key), GenericStatisticsViewHolder.PaceVH::new);
        m.put(context.getString(R.string.stats_custom_layout_average_moving_speed_key), GenericStatisticsViewHolder.AverageMovingSpeed::new);
        m.put(context.getString(R.string.stats_custom_layout_average_speed_key), GenericStatisticsViewHolder.AverageSpeed::new);
        m.put(context.getString(R.string.stats_custom_layout_max_speed_key), GenericStatisticsViewHolder.MaxSpeed::new);
        m.put(context.getString(R.string.stats_custom_layout_average_moving_pace_key), GenericStatisticsViewHolder.AverageMovingPace::new);
        m.put(context.getString(R.string.stats_custom_layout_average_pace_key), GenericStatisticsViewHolder.AveragePace::new);
        m.put(context.getString(R.string.stats_custom_layout_fastest_pace_key), GenericStatisticsViewHolder.FastestPace::new);

        m.put(context.getString(R.string.stats_custom_layout_altitude_key), GenericStatisticsViewHolder.Altitude::new);
        m.put(context.getString(R.string.stats_custom_layout_gain_key), GenericStatisticsViewHolder.Gain::new);
        m.put(context.getString(R.string.stats_custom_layout_loss_key), GenericStatisticsViewHolder.Loss::new);
        m.put(context.getString(R.string.stats_custom_layout_coordinates_key), GenericStatisticsViewHolder.Coordinates::new);

        m.put(context.getString(R.string.stats_custom_layout_heart_rate_key), SensorStatisticsViewHolder.SensorHeartRate::new);
        m.put(context.getString(R.string.stats_custom_layout_cadence_key), SensorStatisticsViewHolder.SensorCadence::new);
        m.put(context.getString(R.string.stats_custom_layout_power_key), SensorStatisticsViewHolder.SensorPower::new);
        m.put(context.getString(R.string.stats_custom_layout_clock_key), ClockViewHolder::new);

        return m;
    }
}
