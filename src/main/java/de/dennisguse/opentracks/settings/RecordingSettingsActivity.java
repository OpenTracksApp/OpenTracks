/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * An activity for accessing recording settings.
 *
 * @author Jimmy Shih
 */
public class RecordingSettingsActivity extends PreferenceActivity implements ChooseActivityTypeCaller {

    private static final int DIALOG_CHOOSE_ACTIVITY = 0;

    private ActivityTypePreference activityTypePreference;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings_recording);

        boolean metricUnits = PreferencesUtils.isMetricUnits(this);

        configFrequencyPreference(R.string.voice_frequency_key, metricUnits);
        configFrequencyPreference(R.string.split_frequency_key, metricUnits);
        configDefaultActivity();
        configListPreference(R.string.min_recording_interval_key, R.array.min_recording_interval_values, metricUnits);
        configListPreference(R.string.recording_distance_interval_key, R.array.recording_distance_interval_values, metricUnits);
        configListPreference(R.string.max_recording_distance_key, R.array.max_recording_distance_values, metricUnits);
        configListPreference(R.string.recording_gps_accuracy_key, R.array.recording_gps_accuracy_values, metricUnits);
        configListPreference(R.string.auto_resume_track_timeout_key, R.array.auto_resume_track_timeout_values, metricUnits);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        if (id != DIALOG_CHOOSE_ACTIVITY) {
            return null;
        }

        String category = PreferencesUtils.getString(
                this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
        return ChooseActivityTypeDialogFragment.getDialog(this, category, this);
    }

    private void configFrequencyPreference(int key, boolean metricUnits) {
        ListPreference preference = (ListPreference) findPreference(getString(key));

        String[] options = StringUtils.getFrequencyOptions(this, metricUnits);
        preference.setEntries(options);
    }

    private void configDefaultActivity() {
        activityTypePreference = (ActivityTypePreference) findPreference(getString(R.string.default_activity_key));
        String defaultActivity = PreferencesUtils.getString(
                this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
        activityTypePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                String stringValue = (String) newValue;
                pref.setSummary(stringValue != null && !stringValue.equals(PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT) ? stringValue : getString(R.string.value_unknown));
                return true;
            }
        });
        activityTypePreference.setSummary(defaultActivity != null && !defaultActivity.equals(PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT) ? defaultActivity : getString(R.string.value_unknown));
        activityTypePreference.setRecordingSettingsActivity(this);
    }

    private void configListPreference(int key, int valueArray, boolean metricUnits) {
        ListPreference preference = (ListPreference) findPreference(getString(key));

        @Deprecated //TODO Can we make values an int array?
        String[] values = getResources().getStringArray(valueArray);
        String[] options = new String[values.length];
        switch (key) {
            case R.string.min_recording_interval_key:
                setMinRecordingIntervalOptions(options, values);
                break;
            case R.string.recording_distance_interval_key:
                setRecordingDistanceIntervalOptions(options, values, metricUnits);
                break;
            case R.string.max_recording_distance_key:
                setMaxRecordingDistanceOptions(options, values, metricUnits);
                break;
            case R.string.recording_gps_accuracy_key:
                setRecordingGpsAccuracyOptions(options, values, metricUnits);
                break;
            case R.string.auto_resume_track_timeout_key:
                setAutoResumeTrackTimeoutOptions(options, values);
                break;
            default:
                return;
        }

        preference.setEntries(options);
    }

    /**
     * Sets the min recording interval options.
     *
     * @param options the options
     * @param values  the values
     */
    private void setMinRecordingIntervalOptions(String[] options, String[] values) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            switch (value) {
                case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE:
                    options[i] = getString(R.string.value_adapt_battery_life);
                    break;
                case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_ACCURACY:
                    options[i] = getString(R.string.value_adapt_accuracy);
                    break;
                case PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT:
                    options[i] = getString(R.string.value_smallest_recommended);
                    break;
                default:
                    options[i] = value < 60 ? getString(R.string.value_integer_second, value)
                            : getString(R.string.value_integer_minute, value / 60);
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
    private void setRecordingDistanceIntervalOptions(String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = getString(R.string.value_integer_meter, value);
                if (value == PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT) {
                    options[i] = getString(R.string.value_integer_meter_recommended, value);
                } else {
                    options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = getString(R.string.value_integer_feet, feet);
                if (value == PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT) {
                    options[i] = getString(R.string.value_integer_feet_recommended, feet);
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
    private void setMaxRecordingDistanceOptions(String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = getString(R.string.value_integer_meter, value);
                if (value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT) {
                    options[i] = getString(R.string.value_integer_meter_recommended, value);
                } else {
                    options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                if (feet < 2000) {
                    displayValue = getString(R.string.value_integer_feet, feet);
                    if (value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT) {
                        options[i] = getString(R.string.value_integer_feet_recommended, feet);
                    } else {
                        options[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = getString(R.string.value_float_mile, mile);
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
    private void setRecordingGpsAccuracyOptions(String[] options, String[] values, boolean metricUnits) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = getString(R.string.value_integer_meter, value);
                switch (value) {
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT:
                        options[i] = getString(R.string.value_integer_meter_recommended, value);
                        break;
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_EXCELLENT:
                        options[i] = getString(R.string.value_integer_meter_excellent_gps, value);
                        break;
                    case PreferencesUtils.RECORDING_GPS_ACCURACY_POOR:
                        options[i] = getString(R.string.value_integer_meter_poor_gps, value);
                        break;
                    default:
                        options[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                if (feet < 2000) {
                    displayValue = getString(R.string.value_integer_feet, feet);
                    switch (value) {
                        case PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT:
                            options[i] = getString(R.string.value_integer_feet_recommended, feet);
                            break;
                        case PreferencesUtils.RECORDING_GPS_ACCURACY_EXCELLENT:
                            options[i] = getString(R.string.value_integer_feet_excellent_gps, feet);
                            break;
                        default:
                            options[i] = displayValue;
                    }
                } else {
                    double mile = feet * UnitConversions.FT_TO_MI;
                    displayValue = getString(R.string.value_float_mile, mile);
                    if (value == PreferencesUtils.RECORDING_GPS_ACCURACY_POOR) {
                        options[i] = getString(R.string.value_float_mile_poor_gps, mile);
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
    private void setAutoResumeTrackTimeoutOptions(String[] options, String[] values) {
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            switch (value) {
                case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_NEVER:
                    options[i] = getString(R.string.value_never);
                    break;
                case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_ALWAYS:
                    options[i] = getString(R.string.value_always);
                    break;
                default:
                    options[i] = getString(R.string.value_integer_minute, value);
            }
        }
    }

    public void showChooseActivityTypeDialog() {
        try {
            removeDialog(DIALOG_CHOOSE_ACTIVITY);
        } catch (Exception e) {
            // Can safely ignore.
        }
        showDialog(DIALOG_CHOOSE_ACTIVITY);
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        activityTypePreference.updateValue(iconValue);
    }
}
