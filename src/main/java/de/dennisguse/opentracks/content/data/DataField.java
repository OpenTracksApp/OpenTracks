package de.dennisguse.opentracks.content.data;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.CsvLayoutUtils;

public class DataField implements Parcelable {
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

    protected DataField(Parcel in) {
        key = in.readString();
        title = in.readString();
        isVisible = in.readByte() != 0;
        isPrimary = in.readByte() != 0;
        isWide = in.readByte() != 0;
    }

    public static final Creator<DataField> CREATOR = new Creator<>() {
        @Override
        public DataField createFromParcel(Parcel in) {
            return new DataField(in);
        }

        @Override
        public DataField[] newArray(int size) {
            return new DataField[size];
        }
    };

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

    public CustomLayoutFieldType getType(Context context) {
        if (key.equals(context.getString(R.string.stats_custom_layout_clock_key))) {
            return CustomLayoutFieldType.CLOCK;
        } else {
            return CustomLayoutFieldType.GENERIC;
        }
    }

    public String toCsv() {
        String visible = this.isVisible ? YES_VALUE : NOT_VALUE;
        String primary = this.isPrimary ? YES_VALUE : NOT_VALUE;
        String wide = this.isWide ? YES_VALUE : NOT_VALUE;
        return key + CsvLayoutUtils.PROPERTY_SEPARATOR + visible + CsvLayoutUtils.PROPERTY_SEPARATOR + primary + CsvLayoutUtils.PROPERTY_SEPARATOR + wide;
    }

    public static String getTitleByKey(Resources resources, String key) {
        if (key.equals(resources.getString(R.string.stats_custom_layout_total_time_key))) {
            return resources.getString(R.string.stats_total_time);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_moving_time_key))) {
            return resources.getString(R.string.stats_moving_time);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_distance_key))) {
            return resources.getString(R.string.stats_distance);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_speed_key))) {
            return resources.getString(R.string.stats_speed);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_pace_key))) {
            return resources.getString(R.string.stats_pace);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_average_moving_speed_key))) {
            return resources.getString(R.string.stats_average_moving_speed);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_average_speed_key))) {
            return resources.getString(R.string.stats_average_speed);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_max_speed_key))) {
            return resources.getString(R.string.stats_max_speed);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_average_moving_pace_key))) {
            return resources.getString(R.string.stats_average_moving_pace);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_average_pace_key))) {
            return resources.getString(R.string.stats_average_pace);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_fastest_pace_key))) {
            return resources.getString(R.string.stats_fastest_pace);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_altitude_key))) {
            return resources.getString(R.string.stats_altitude);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_gain_key))) {
            return resources.getString(R.string.stats_gain);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_loss_key))) {
            return resources.getString(R.string.stats_loss);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_coordinates_key))) {
            return resources.getString(R.string.stats_coordinates);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_heart_rate_key))) {
            return resources.getString(R.string.stats_sensors_heart_rate);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_cadence_key))) {
            return resources.getString(R.string.stats_sensors_cadence);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_power_key))) {
            return resources.getString(R.string.stats_sensors_power);
        } else if (key.equals(resources.getString(R.string.stats_custom_layout_clock_key))) {
            return resources.getString(R.string.stats_clock);
        } else {
            throw new RuntimeException("It doesn't exists a field with key: " + key);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(key);
        parcel.writeString(title);
        parcel.writeByte((byte) (isVisible ? 1 : 0));
        parcel.writeByte((byte) (isPrimary ? 1 : 0));
        parcel.writeByte((byte) (isWide ? 1 : 0));
    }
}
