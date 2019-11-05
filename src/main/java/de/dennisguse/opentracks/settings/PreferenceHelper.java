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

    /**
     * Sets the min recording interval options.
     *
     * @param options the options
     * @param values  the values
     */
    static void setMinRecordingIntervalOptions(Context context, String[] options, String[] values) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            switch (value) {
                case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE:
                    options[i] = context.getString(R.string.value_adapt_battery_life);
                    break;
                case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_ACCURACY:
                    options[i] = context.getString(R.string.value_adapt_accuracy);
                    break;
                case PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT:
                    options[i] = context.getString(R.string.value_smallest_recommended);
                    break;
                default:
                    options[i] = value < 60 ? context.getString(R.string.value_integer_second, value) : context.getString(R.string.value_integer_minute, value / 60);
            }
        }
    }

    /**
     * Sets the recording distance interval options.
     *
     * @param options     the options
     * @param values      the values
     * @param metricUnits true for metric units
     */
    static void setRecordingDistanceIntervalOptions(Context context, String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                if (value == PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT) {
                    options[i] = context.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = context.getString(R.string.value_integer_feet, feet);
                if (value == PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT) {
                    options[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    options[i] = displayValue;
                }
            }
        }
    }

    /**
     * Sets the max recording distance options.
     *
     * @param options     the options
     * @param values      the values
     * @param metricUnits true for metric units
     */
    static void setMaxRecordingDistanceOptions(Context context, String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                if (value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT) {
                    options[i] = context.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                if (feet < 2000) {
                    displayValue = context.getString(R.string.value_integer_feet, feet);
                    if (value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT) {
                        options[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                    } else {
                        options[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = context.getString(R.string.value_float_mile, mile);
                    options[i] = displayValue;
                }
            }
        }
    }

    /**
     * Sets the recording gps accuracy options.
     *
     * @param options     the options
     * @param values      the values
     * @param metricUnits true for metric units
     */
    static void setRecordingGpsAccuracyOptions(Context context, String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = context.getString(R.string.value_integer_meter, value);
                switch (value) {
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT:
                        options[i] = context.getString(R.string.value_integer_meter_recommended, value);
                        break;
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_EXCELLENT:
                        options[i] = context.getString(R.string.value_integer_meter_excellent_gps, value);
                        break;
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_POOR:
                        options[i] = context.getString(R.string.value_integer_meter_poor_gps, value);
                        break;
                    default:
                        options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                if (feet < 2000) {
                    displayValue = context.getString(R.string.value_integer_feet, feet);
                    switch (value) {
                        case PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT:
                            options[i] = context.getString(R.string.value_integer_feet_recommended, feet);
                            break;
                        case PreferencesUtils.RECORDING_GPS_ACCURACY_EXCELLENT:
                            options[i] = context.getString(R.string.value_integer_feet_excellent_gps, feet);
                            break;
                        default:
                            options[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = context.getString(R.string.value_float_mile, mile);
                    if (value == PreferencesUtils.RECORDING_GPS_ACCURACY_POOR) {
                        options[i] = context.getString(R.string.value_float_mile_poor_gps, mile);
                    } else {
                        options[i] = displayValue;
                    }
                }
            }
        }
    }

    /**
     * Sets the auto resume track timeout options.
     *
     * @param options the options
     * @param values  the values
     */
    static void setAutoResumeTrackTimeoutOptions(Context context, String[] options, String[] values) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            switch (value) {
                case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_NEVER:
                    options[i] = context.getString(R.string.value_never);
                    break;
                case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_ALWAYS:
                    options[i] = context.getString(R.string.value_always);
                    break;
                default:
                    options[i] = context.getString(R.string.value_integer_minute, value);
            }
        }
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
