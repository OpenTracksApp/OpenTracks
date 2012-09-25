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

package com.google.android.apps.mytracks.settings;

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

/**
 * An activity for accessing recording settings.
 * 
 * @author Jimmy Shih
 */
public class RecordingSettingsActivity extends AbstractSettingsActivity {

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.recording_settings);

    boolean metricUnits = PreferencesUtils.getBoolean(
        this, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);

    int voiceFrequencyValue = PreferencesUtils.getInt(
        this, R.string.voice_frequency_key, PreferencesUtils.VOICE_FREQUENCY_DEFAULT);
    ListPreference voiceFrequencyListPreference = (ListPreference) findPreference(
        getString(R.string.voice_frequency_key));
    configurePreference(voiceFrequencyListPreference,
        StringUtils.getFrequencyOptions(this, metricUnits),
        getResources().getStringArray(R.array.frequency_values),
        R.string.settings_recording_voice_frequency_summary, String.valueOf(voiceFrequencyValue));

    int splitFrequencyValue = PreferencesUtils.getInt(
        this, R.string.split_frequency_key, PreferencesUtils.SPLIT_FREQUENCY_DEFAULT);
    ListPreference splitFrequencyListPreference = (ListPreference) findPreference(
        getString(R.string.split_frequency_key));
    configurePreference(splitFrequencyListPreference,
        StringUtils.getFrequencyOptions(this, metricUnits),
        getResources().getStringArray(R.array.frequency_values),
        R.string.settings_recording_split_frequency_summary, String.valueOf(splitFrequencyValue));

    String trackNameValue = PreferencesUtils.getString(
        this, R.string.track_name_key, PreferencesUtils.TRACK_NAME_DEFAULT);
    ListPreference trackNameListPreference = (ListPreference) findPreference(
        getString(R.string.track_name_key));
    configurePreference(trackNameListPreference,
        getResources().getStringArray(R.array.track_name_options),
        getResources().getStringArray(R.array.track_name_values),
        R.string.settings_recording_track_name_summary, trackNameValue);

    String defaultActivityValue = PreferencesUtils.getString(
        this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
    Preference defaultActivityPreference = findPreference(getString(R.string.default_activity_key));
    configurePreference(defaultActivityPreference, null, null,
        R.string.settings_recording_default_activity_summary, defaultActivityValue);

    int minRecordingIntervalValue = PreferencesUtils.getInt(
        this, R.string.min_recording_interval_key, PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT);
    ListPreference minRecordingIntervalListPreference = (ListPreference) findPreference(
        getString(R.string.min_recording_interval_key));
    configurePreference(minRecordingIntervalListPreference, getMinRecordingIntervalOptions(),
        getResources().getStringArray(R.array.min_recording_interval_values),
        R.string.settings_recording_min_recording_interval_summary,
        String.valueOf(minRecordingIntervalValue));

    int minRecordingDistanceValue = PreferencesUtils.getInt(
        this, R.string.min_recording_distance_key, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    ListPreference minRecordingDistanceListPreference = (ListPreference) findPreference(
        getString(R.string.min_recording_distance_key));
    configurePreference(minRecordingDistanceListPreference,
        getMinRecordingDistanceOptions(metricUnits),
        getResources().getStringArray(R.array.min_recording_distance_values),
        R.string.settings_recording_min_recording_distance_summary,
        String.valueOf(minRecordingDistanceValue));

    int maxRecordingDistanceValue = PreferencesUtils.getInt(
        this, R.string.max_recording_distance_key, PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT);
    ListPreference maxRecordingDistanceListPreference = (ListPreference) findPreference(
        getString(R.string.max_recording_distance_key));
    configurePreference(maxRecordingDistanceListPreference,
        getMaxRecordingDistanceOptions(metricUnits),
        getResources().getStringArray(R.array.max_recording_distance_values),
        R.string.settings_recording_max_recording_distance_summary,
        String.valueOf(maxRecordingDistanceValue));

    int minRequiredAcuracyValue = PreferencesUtils.getInt(
        this, R.string.min_required_accuracy_key, PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT);
    ListPreference minRequiredAccuracyListPreference = (ListPreference) findPreference(
        getString(R.string.min_required_accuracy_key));
    configurePreference(minRequiredAccuracyListPreference,
        getMinRequiredAccuracyOptions(metricUnits),
        getResources().getStringArray(R.array.min_required_accuracy_values),
        R.string.settings_recording_min_required_accuracy_summary,
        String.valueOf(minRequiredAcuracyValue));

    int autoResumeTrackTimeoutValue = PreferencesUtils.getInt(this,
        R.string.auto_resume_track_timeout_key, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);
    ListPreference autoResumeTrackTimeoutListPreference = (ListPreference) findPreference(
        getString(R.string.auto_resume_track_timeout_key));
    configurePreference(autoResumeTrackTimeoutListPreference, getAutoResumeTrackTimeoutOptions(),
        getResources().getStringArray(R.array.auto_resume_track_timeout_values),
        R.string.settings_recording_auto_resume_track_timeout_summary,
        String.valueOf(autoResumeTrackTimeoutValue));
  }

  private void configurePreference(final Preference preference, final String[] options,
      final String[] values, final int summaryId, String value) {
    if (options != null) {
      ((ListPreference) preference).setEntries(options);
    }
    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        updatePreferenceSummary(pref, options, values, summaryId, (String) newValue);
        return true;
      }
    });
    updatePreferenceSummary(preference, options, values, summaryId, value);
  }

  private void updatePreferenceSummary(
      Preference listPreference, String[] options, String[] values, int summaryId, String value) {
    String summary = getString(summaryId);
    String option;
    if (options != null && values != null) {
      option = getOption(options, values, value);
    } else {
      option = value != null && value.length() != 0 ? value : getString(R.string.value_unknown);
    }
    if (option != null) {
      summary += "\n" + option;
    }
    listPreference.setSummary(summary);
  }

  /**
   * Gets the display option for a stored value.
   * 
   * @param options list of the display options
   * @param values list of the stored values
   * @param value the store value
   */
  private String getOption(String[] options, String[] values, String value) {
    for (int i = 0; i < values.length; i++) {
      if (value.equals(values[i])) {
        return options[i];
      }
    }
    return null;
  }

  /**
   * Gets the min recording interval display options.
   */
  private String[] getMinRecordingIntervalOptions() {
    String[] values = getResources().getStringArray(R.array.min_recording_interval_values);
    String[] options = new String[values.length];
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
    return options;
  }

  /**
   * Gets the min recording distance display options.
   * 
   * @param metricUnits true to display metric units
   */
  private String[] getMinRecordingDistanceOptions(boolean metricUnits) {
    String[] values = getResources().getStringArray(R.array.min_recording_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (metricUnits) {
        options[i] = getString(
            value == PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT ? R.string.value_integer_meter_recommended
                : R.string.value_integer_meter, value);
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        options[i] = getString(
            value == PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT ? R.string.value_integer_feet_recommended
                : R.string.value_integer_feet, feet);
      }
    }
    return options;
  }

  /**
   * Gets the max recording distance display options.
   * 
   * @param metricUnits true to display metric units
   */
  private String[] getMaxRecordingDistanceOptions(boolean metricUnits) {
    String[] values = getResources().getStringArray(R.array.max_recording_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (metricUnits) {
        options[i] = getString(
            value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT ? R.string.value_integer_meter_recommended
                : R.string.value_integer_meter, value);
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        if (feet < 2000) {
          options[i] = getString(
              value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT ? R.string.value_integer_feet_recommended
                  : R.string.value_integer_feet, feet);
        } else {
          double mile = feet * UnitConversions.FT_TO_MI;
          options[i] = getString(R.string.value_float_mile, mile);
        }
      }
    }
    return options;
  }

  /**
   * Gets the min required accuracy display options.
   * 
   * @param metricUnits true to display metric units
   */
  private String[] getMinRequiredAccuracyOptions(boolean metricUnits) {
    String[] values = getResources().getStringArray(R.array.min_required_accuracy_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (metricUnits) {
        switch (value) {
          case PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT:
            options[i] = getString(R.string.value_integer_meter_recommended, value);
            break;
          case PreferencesUtils.MIN_REQUIRED_ACCURACY_EXCELLENT:
            options[i] = getString(R.string.value_integer_meter_excellent_gps, value);
            break;
          case PreferencesUtils.MIN_REQUIRED_ACCURACY_POOR:
            options[i] = getString(R.string.value_integer_meter_poor_gps, value);
            break;
          default:
            options[i] = getString(R.string.value_integer_meter, value);
        }
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        if (feet < 2000) {
          switch (value) {
            case PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT:
              options[i] = getString(R.string.value_integer_feet_recommended, feet);
              break;
            case PreferencesUtils.MIN_REQUIRED_ACCURACY_EXCELLENT:
              options[i] = getString(R.string.value_integer_feet_excellent_gps, feet);
              break;
            default:
              options[i] = getString(R.string.value_integer_feet, feet);
          }
        } else {
          double mile = feet * UnitConversions.FT_TO_MI;
          options[i] = getString(
              value == PreferencesUtils.MIN_REQUIRED_ACCURACY_POOR ? R.string.value_float_mile_poor_gps
                  : R.string.value_float_mile, mile);
        }
      }
    }
    return options;
  }

  /**
   * Gets the auto resume track timeout display options.
   */
  private String[] getAutoResumeTrackTimeoutOptions() {
    String[] values = getResources().getStringArray(R.array.auto_resume_track_timeout_values);
    String[] options = new String[values.length];
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
    return options;
  }
}
