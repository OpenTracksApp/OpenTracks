package de.dennisguse.opentracks.settings;

import android.content.Context;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

final class PreferenceHelper {

    static String[] getMinRecordingIntervalEntries(Context context) {
        String[] entryValues = context.getResources().getStringArray(R.array.min_recording_interval_values);
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (value == PreferencesUtils.getMinRecordingIntervalDefault(context).getSeconds()) {
                entries[i] = context.getString(R.string.value_smallest_recommended);
            } else {
                entries[i] = value < 60 ? context.getString(R.string.value_integer_second, value) : context.getString(R.string.value_integer_minute, value / 60);
            }
        }

        return entries;
    }

    static String[] getRecordingDistanceIntervalEntries(Context context, boolean metricUnits) {
        String[] entryValues = context.getResources().getStringArray(R.array.recording_distance_interval_values);
        String[] entries = new String[entryValues.length];

        final int recordingDistanceIntervalDefault = (int) PreferencesUtils.getRecordingDistanceIntervalDefault(context).toM();

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = context.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = context.getString(R.string.value_integer_feet, feet);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    static String[] getMaxRecordingDistanceEntries(Context context, boolean metricUnits) {
        String[] entryValues = context.getResources().getStringArray(R.array.max_recording_distance_values);
        String[] entries = new String[entryValues.length];

        final int maxRecordingDistanceDefault = Integer.parseInt(context.getResources().getString(R.string.max_recording_distance_default));

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = context.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = context.getString(R.string.value_integer_feet, feet);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    static String[] getRecordingGpsAccuracyEntries(Context context, boolean metricUnits) {
        String[] entryValues = context.getResources().getStringArray(R.array.recording_gps_accuracy_values);
        String[] entries = new String[entryValues.length];

        final int recordingGPSAccuracyDefault = Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_default));
        final int recordingGPSAccuracyExcellent = Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_excellent));
        final int recordingGPSAccuracyPoor = Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_poor));

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = context.getString(R.string.value_integer_meter_recommended, value);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = context.getString(R.string.value_integer_meter_excellent_gps, value);
                } else if (value == recordingGPSAccuracyPoor) {
                    entries[i] = context.getString(R.string.value_integer_meter_poor_gps, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = context.getString(R.string.value_integer_feet, feet);

                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = context.getString(R.string.value_integer_feet_excellent_gps, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }
}
