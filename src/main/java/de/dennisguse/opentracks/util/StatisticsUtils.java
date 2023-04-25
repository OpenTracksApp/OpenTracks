package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class StatisticsUtils {

    @Deprecated
    public static String emptyValue(@NonNull Context context, @NonNull String statTitle) {
        if (PreferencesUtils.isKey(R.string.stats_custom_layout_total_time_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_moving_time_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_pace_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_average_moving_pace_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_average_pace_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_fastest_pace_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_clock_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_time);
        } else if (PreferencesUtils.isKey(R.string.stats_custom_layout_distance_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_speed_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_average_speed_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_max_speed_key, statTitle) || PreferencesUtils.isKey(R.string.stats_custom_layout_average_moving_speed_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_float);
        } else if (PreferencesUtils.isKey(R.string.stats_custom_layout_coordinates_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_coordinates);
        } else {
            return context.getString(R.string.stats_empty_value_integer);
        }
    }
}
