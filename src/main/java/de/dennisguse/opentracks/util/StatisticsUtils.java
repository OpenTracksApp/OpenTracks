package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Layout;

public class StatisticsUtils {

    public static String emptyValue(@NonNull Context context, @NonNull String statTitle) {
        if (PreferencesUtils.isKey(context, R.string.stats_custom_layout_total_time_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_moving_time_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_pace_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_average_moving_pace_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_average_pace_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_fastest_pace_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_time);
        } else if (PreferencesUtils.isKey(context, R.string.stats_custom_layout_distance_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_speed_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_average_speed_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_max_speed_key, statTitle) || PreferencesUtils.isKey(context, R.string.stats_custom_layout_average_moving_speed_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_float);
        } else if (PreferencesUtils.isKey(context, R.string.stats_custom_layout_coordinates_key, statTitle)) {
            return context.getString(R.string.stats_empty_value_coordinates);
        } else {
            return context.getString(R.string.stats_empty_value_integer);
        }
    }

    public static Layout filterVisible(Layout layout, boolean visible) {
        Layout result = new Layout(layout.getProfile());
        result.addFields(layout.getFields().stream().filter(f -> f.isVisible() == visible).collect(Collectors.toList()));
        return result;
    }
}
