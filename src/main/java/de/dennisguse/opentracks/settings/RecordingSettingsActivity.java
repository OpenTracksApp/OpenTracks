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
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

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
                PreferenceHelper.setMinRecordingIntervalOptions(this, options, values);
                break;
            case R.string.recording_distance_interval_key:
                PreferenceHelper.setRecordingDistanceIntervalOptions(this, options, values, metricUnits);
                break;
            case R.string.max_recording_distance_key:
                PreferenceHelper.setMaxRecordingDistanceOptions(this, options, values, metricUnits);
                break;
            case R.string.recording_gps_accuracy_key:
                PreferenceHelper.setRecordingGpsAccuracyOptions(this, options, values, metricUnits);
                break;
            case R.string.auto_resume_track_timeout_key:
                PreferenceHelper.setAutoResumeTrackTimeoutOptions(this, options, values);
                break;
            default:
                return;
        }

        preference.setEntries(options);
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
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        if (id != DIALOG_CHOOSE_ACTIVITY) {
            return null;
        }

        String category = PreferencesUtils.getString(this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
        //TODO ATTENTION: Need to switch to FragmentActivity before this can be used.
        // ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), category);
        return null;
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        activityTypePreference.updateValue(iconValue);
    }
}
