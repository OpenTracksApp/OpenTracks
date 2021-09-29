package de.dennisguse.opentracks.settings;

import android.content.res.Resources;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

final class PreferenceHelper {

    static String[] getMinRecordingIntervalEntries(Resources resources) {
        String[] entryValues = resources.getStringArray(R.array.min_recording_interval_values);
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (value == PreferencesUtils.getMinRecordingIntervalDefault().getSeconds()) {
                entries[i] = resources.getString(R.string.value_smallest_recommended);
            } else {
                entries[i] = value < 60 ? resources.getString(R.string.value_integer_second, value) : resources.getString(R.string.value_integer_minute, value / 60);
            }
        }

        return entries;
    }

    static String[] getRecordingDistanceIntervalEntries(Resources resources, boolean metricUnits) {
        String[] entryValues = resources.getStringArray(R.array.recording_distance_interval_values);
        String[] entries = new String[entryValues.length];

        final int recordingDistanceIntervalDefault = (int) PreferencesUtils.getRecordingDistanceIntervalDefault().toM();

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    static String[] getMaxRecordingDistanceEntries(Resources resources, boolean metricUnits) {
        String[] entryValues = resources.getStringArray(R.array.max_recording_distance_values);
        String[] entries = new String[entryValues.length];

        final int maxRecordingDistanceDefault = Integer.parseInt(resources.getString(R.string.max_recording_distance_default));

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    static String[] getRecordingGpsAccuracyEntries(Resources resources, boolean metricUnits) {
        String[] entryValues = resources.getStringArray(R.array.recording_gps_accuracy_values);
        String[] entries = new String[entryValues.length];

        final int recordingGPSAccuracyDefault = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_default));
        final int recordingGPSAccuracyExcellent = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_excellent));
        final int recordingGPSAccuracyPoor = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_poor));

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = resources.getString(R.string.value_integer_meter_excellent_gps, value);
                } else if (value == recordingGPSAccuracyPoor) {
                    entries[i] = resources.getString(R.string.value_integer_meter_poor_gps, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);

                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = resources.getString(R.string.value_integer_feet_excellent_gps, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    static String[] getAnnouncementFrequency(Resources resources) {
        String[] values = resources.getStringArray(R.array.voice_announcement_frequency_values);
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(values[i])) {
                options[i] = resources.getString(R.string.value_off);
            } else {
                int value = Integer.parseInt(values[i]);
                options[i] = resources.getString(R.string.value_integer_minute, Duration.ofSeconds(value).toMinutes());
            }
        }
        return options;
    }

    static String[] getAnnouncementDistance(Resources resources, boolean metricUnits) {
        String[] values = resources.getStringArray(R.array.voice_announcement_distance_values);
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(values[i])) {
                options[i] = resources.getString(R.string.value_off);
            } else {
                int value = Integer.parseInt(values[i]);
                options[i] = resources.getString(metricUnits ? R.string.value_integer_kilometer : R.string.value_integer_mile, value);
            }
        }
        return options;
    }
}
