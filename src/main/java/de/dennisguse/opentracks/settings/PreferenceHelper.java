package de.dennisguse.opentracks.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

final class PreferenceHelper {

    static String[] getMinRecordingIntervalEntries(Context context) {
        String[] entryValues = context.getResources().getStringArray(R.array.min_recording_interval_values);
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (value == PreferencesUtils.getMinRecordingIntervalAdaptAccuracy(context)) {
                entries[i] = context.getString(R.string.value_adapt_accuracy);
            } else if (value == PreferencesUtils.getMinRecordingIntervalAdaptBatteryLife(context)) {
                entries[i] = context.getString(R.string.value_adapt_battery_life);
            } else if (value == PreferencesUtils.getMinRecordingIntervalDefault(context)) {
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

        final int recordingDistanceIntervalDefault = PreferencesUtils.getRecordingDistanceIntervalDefault(context);

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
                if (feet < 2000) {
                    displayValue = context.getString(R.string.value_integer_feet, feet);
                    if (value == maxRecordingDistanceDefault) {
                        entries[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                    } else {
                        entries[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = context.getString(R.string.value_float_mile, mile);
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
                if (feet < 2000) {
                    displayValue = context.getString(R.string.value_integer_feet, feet);

                    if (value == recordingGPSAccuracyDefault) {
                        entries[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                    } else if (value == recordingGPSAccuracyExcellent) {
                        entries[i] = context.getString(R.string.value_integer_feet_excellent_gps, feet);
                    } else {
                        entries[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = context.getString(R.string.value_float_mile, mile);
                    if (value == recordingGPSAccuracyPoor) {
                        entries[i] = context.getString(R.string.value_float_mile_poor_gps, mile);
                    } else {
                        entries[i] = displayValue;
                    }
                }
            }
        }

        return entries;
    }

    static String[] getAutoResumeTrackTimeoutEntries(Context context) {
        String[] entryValues = context.getResources().getStringArray(R.array.auto_resume_track_timeout_values);
        String[] entries = new String[entryValues.length];

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            if (value == Integer.parseInt(context.getResources().getString(R.string.auto_resume_track_timeout_never))) {
                entries[i] = context.getString(R.string.value_never);
            } else if (value == Integer.parseInt(context.getResources().getString(R.string.auto_resume_track_timeout_always))) {
                entries[i] = context.getString(R.string.value_always);
            } else {
                entries[i] = context.getString(R.string.value_integer_minute, value);
            }
        }

        return entries;
    }

    /**
     * Configures the bluetooth sensor.
     */
    static void configureBluetoothSensorList(ListPreference preference) {
        Context context = preference.getContext();

        String value = PreferencesUtils.getString(context, R.string.settings_sensor_bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);
        List<String> devicesNameList = new ArrayList<>();
        List<String> devicesAddressList = new ArrayList<>();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            BluetoothUtils.populateDeviceLists(bluetoothAdapter, devicesNameList, devicesAddressList);
        }

        // Was the previously configured device unpaired? Then forget it.
        if (!devicesAddressList.contains(value)) {
            value = PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT;
            PreferencesUtils.setString(context, R.string.settings_sensor_bluetooth_sensor_key, value);
        }

        devicesNameList.add(0, context.getString(R.string.value_none));
        devicesAddressList.add(0, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT);

        String[] values = devicesAddressList.toArray(new String[0]);
        preference.setEntryValues(values);

        String[] options = devicesNameList.toArray(new String[0]);
        preference.setEntries(options);
    }
}
