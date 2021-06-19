package de.dennisguse.opentracks.content.data;

import android.content.Context;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.CsvConstants;

public class DataField {
    public static final String YES_VALUE = "1";
    public static final String NOT_VALUE = "0";

    private final String key;
    private final String title;
    private boolean isVisible;
    private boolean isPrimary;
    private final boolean isWide;

    public DataField(String key, String title, boolean isVisible, boolean isPrimary, boolean isWide) {
        this.key = key;
        this.title = title;
        this.isVisible = isVisible;
        this.isPrimary = isPrimary;
        this.isWide = isWide;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void togglePrimary() {
        isPrimary = !isPrimary;
    }

    public boolean isWide() {
        return isWide;
    }

    public String toCsv() {
        String visible = this.isVisible ? YES_VALUE : NOT_VALUE;
        String primary = this.isPrimary ? YES_VALUE : NOT_VALUE;
        return key + CsvConstants.ITEM_SEPARATOR + visible + CsvConstants.ITEM_SEPARATOR + primary;
    }

    public static String getTitleByKey(Context context, String key) {
        if (key.equals(context.getString(R.string.stats_custom_layout_total_time_key))) {
            return context.getString(R.string.stats_total_time);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_moving_time_key))) {
            return context.getString(R.string.stats_moving_time);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_distance_key))) {
            return context.getString(R.string.stats_distance);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_speed_key))) {
            return context.getString(R.string.stats_speed);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_pace_key))) {
            return context.getString(R.string.stats_pace);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_average_moving_speed_key))) {
            return context.getString(R.string.stats_average_moving_speed);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_average_speed_key))) {
            return context.getString(R.string.stats_average_speed);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_max_speed_key))) {
            return context.getString(R.string.stats_max_speed);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_average_moving_pace_key))) {
            return context.getString(R.string.stats_average_moving_pace);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_average_pace_key))) {
            return context.getString(R.string.stats_average_pace);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_fastest_pace_key))) {
            return context.getString(R.string.stats_fastest_pace);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_altitude_key))) {
            return context.getString(R.string.stats_altitude);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_gain_key))) {
            return context.getString(R.string.stats_gain);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_loss_key))) {
            return context.getString(R.string.stats_loss);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_coordinates_key))) {
            return context.getString(R.string.stats_coordinates);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_heart_rate_key))) {
            return context.getString(R.string.stats_sensors_heart_rate);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_cadence_key))) {
            return context.getString(R.string.stats_sensors_cadence);
        } else if (key.equals(context.getString(R.string.stats_custom_layout_power_key))) {
            return context.getString(R.string.stats_sensors_power);
        } else {
            throw new RuntimeException("It doesn't exists a field with key: " + key);
        }
    }
}
