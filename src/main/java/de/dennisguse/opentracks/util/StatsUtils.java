package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Layout;

public class StatsUtils {

    public static String emptyValue(@NonNull Context context, @NonNull String statTitle) {
        if (statTitle.equals(context.getString(R.string.stats_moving_time)) || statTitle.equals(context.getString(R.string.stats_total_time)) || statTitle.equals(context.getString(R.string.stats_pace)) || statTitle.equals(context.getString(R.string.stats_average_pace)) || statTitle.equals(context.getString(R.string.stats_average_moving_pace)) || statTitle.equals(context.getString(R.string.stats_fastest_pace))) {
            return context.getString(R.string.stats_empty_value_time);
        } else if (statTitle.equals(context.getString(R.string.stats_distance)) || statTitle.equals(context.getString(R.string.stats_speed)) || statTitle.equals(context.getString(R.string.stats_average_moving_speed)) || statTitle.equals(context.getString(R.string.stats_average_speed)) || statTitle.equals(context.getString(R.string.stats_max_speed))) {
            return context.getString(R.string.stats_empty_value_float);
        } else {
            return context.getString(R.string.stats_empty_value_integer);
        }
    }

    public static Layout filterVisible(Layout layout, boolean visible) {
        Layout result = new Layout(layout.getProfile());
        for (Layout.Field field : layout.getFields()) {
            if (field.isVisible() == visible) {
                result.addField(field);
            }
        }
        return result;
    }
}
