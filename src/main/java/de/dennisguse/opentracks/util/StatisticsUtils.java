package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;

public class StatisticsUtils {

    private StatisticsUtils() {}


    /**
     * @deprecated This method is deprecated and should no longer be used.
     *
     * @param context The context in which the method is being called.
     * @param statTitle The title of the statistic being displayed.
     *
     * @return The empty value string to be displayed for the given statistic.
     */
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


    public static RecordingLayout filterVisible(RecordingLayout recordingLayout, boolean visible) {
        RecordingLayout result = new RecordingLayout(recordingLayout.getName());
        List<DataField> filteredFields = new ArrayList<>();
        for (DataField field : recordingLayout.getFields()) {
            if (field.isVisible() == visible) {
                filteredFields.add(field);
            }
        }
        result.addFields(filteredFields);
        return result;
    }

}