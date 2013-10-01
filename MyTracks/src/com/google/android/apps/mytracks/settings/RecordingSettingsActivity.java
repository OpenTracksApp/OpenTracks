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

    boolean metricUnits = PreferencesUtils.isMetricUnits(this);

    configFrequencyPreference(R.string.voice_frequency_key,
        PreferencesUtils.VOICE_FREQUENCY_DEFAULT, R.array.frequency_values, metricUnits);
    configFrequencyPreference(R.string.split_frequency_key,
        PreferencesUtils.SPLIT_FREQUENCY_DEFAULT, R.array.frequency_values, metricUnits);
    configTrackName();
    configDefaultActivity();
    configListPreference(R.string.min_recording_interval_key,
        PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT, R.array.min_recording_interval_values,
        metricUnits);
    configListPreference(R.string.recording_distance_interval_key,
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT,
        R.array.recording_distance_interval_values, metricUnits);
    configListPreference(R.string.max_recording_distance_key,
        PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT, R.array.max_recording_distance_values,
        metricUnits);
    configListPreference(R.string.recording_gps_accuracy_key,
        PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT, R.array.recording_gps_accuracy_values,
        metricUnits);
    configListPreference(R.string.auto_resume_track_timeout_key,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT,
        R.array.auto_resume_track_timeout_values, metricUnits);
  }

  @SuppressWarnings("deprecation")
  private void configFrequencyPreference(
      int key, int defaultValue, int valueArray, boolean metricUnits) {
    ListPreference preference = (ListPreference) findPreference(getString(key));
    int value = PreferencesUtils.getInt(this, key, defaultValue);
    String[] values = getResources().getStringArray(valueArray);
    String[] options = StringUtils.getFrequencyOptions(this, metricUnits);
    configureListPreference(preference, options, options, values, String.valueOf(value), null);
  }

  @SuppressWarnings("deprecation")
  private void configTrackName() {
    ListPreference preference = (ListPreference) findPreference(getString(R.string.track_name_key));
    String value = PreferencesUtils.getString(
        this, R.string.track_name_key, PreferencesUtils.TRACK_NAME_DEFAULT);
    String[] values = getResources().getStringArray(R.array.track_name_values);
    String[] options = getResources().getStringArray(R.array.track_name_options);
    configureListPreference(preference, options, options, values, value, null);
  }

  @SuppressWarnings("deprecation")
  private void configDefaultActivity() {
    Preference preference = findPreference(getString(R.string.default_activity_key));
    String value = PreferencesUtils.getString(
        this, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
    preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        String stringValue = (String) newValue;
        pref.setSummary(stringValue != null
            && !stringValue.equals(PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT) ? stringValue
            : getString(R.string.value_unknown));
        return true;
      }
    });
    preference.setSummary(
        value != null && !value.equals(PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT) ? value
            : getString(R.string.value_unknown));
  }

  @SuppressWarnings("deprecation")
  private void configListPreference(
      int key, int defaultValue, int valueArray, boolean metricUnits) {
    ListPreference preference = (ListPreference) findPreference(getString(key));
    int value = PreferencesUtils.getInt(this, key, defaultValue);
    String[] values = getResources().getStringArray(valueArray);
    String[] options = new String[values.length];
    String[] summary = new String[values.length];
    switch (key) {
      case R.string.min_recording_interval_key:
        setMinRecordingIntervalSummaryAndOptions(summary, options, values);
        break;
      case R.string.recording_distance_interval_key:
        setRecordingDistanceIntervalSummaryAndOptions(summary, options, values, metricUnits);
        break;
      case R.string.max_recording_distance_key:
        setMaxRecordingDistanceSummaryAndOptions(summary, options, values, metricUnits);
        break;
      case R.string.recording_gps_accuracy_key:
        setRecordingGpsAccuracySummaryAndOptions(summary, options, values, metricUnits);
        break;
      case R.string.auto_resume_track_timeout_key:
        setAutoResumeTrackTimeoutSummaryAndOptions(summary, options, values);
        break;
      default:
        return;
    }
    configureListPreference(preference, summary, options, values, String.valueOf(value), null);
  }

  /**
   * Sets the min recording interval summary and options.
   * 
   * @param summary the summary
   * @param options the options
   * @param values the values
   */
  private void setMinRecordingIntervalSummaryAndOptions(
      String[] summary, String[] options, String[] values) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      switch (value) {
        case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE:
          options[i] = getString(R.string.value_adapt_battery_life);
          summary[i] = options[i];
          break;
        case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_ACCURACY:
          options[i] = getString(R.string.value_adapt_accuracy);
          summary[i] = options[i];
          break;
        case PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT:
          options[i] = getString(R.string.value_smallest_recommended);
          summary[i] = getString(R.string.value_smallest);
          break;
        default:
          options[i] = value < 60 ? getString(R.string.value_integer_second, value)
              : getString(R.string.value_integer_minute, value / 60);
          summary[i] = getString(
              R.string.settings_recording_location_frequency_summary, options[i]);
      }
    }
  }

  /**
   * Sets the recording distance interval summary and options.
   * 
   * @param summary the summary
   * @param options the options
   * @param values the values
   * @param metricUnits true for metric units
   */
  private void setRecordingDistanceIntervalSummaryAndOptions(
      String[] summary, String[] options, String[] values, boolean metricUnits) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      String displayValue;
      if (metricUnits) {
        displayValue = getString(R.string.value_integer_meter, value);
        switch (value) {
          case PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT:
            options[i] = getString(R.string.value_integer_meter_recommended, value);
            break;
          default:
            options[i] = displayValue;
        }
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        displayValue = getString(R.string.value_integer_feet, feet);
        switch (value) {
          case PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT:
            options[i] = getString(R.string.value_integer_feet_recommended, feet);
            break;
          default:
            options[i] = displayValue;
        }
      }
      summary[i] = getString(R.string.settings_recording_location_frequency_summary, displayValue);
    }
  }

  /**
   * Sets the max recording distance summary and options.
   * 
   * @param summary the summary
   * @param options the options
   * @param values the values
   * @param metricUnits true for metric units
   */
  private void setMaxRecordingDistanceSummaryAndOptions(
      String[] summary, String[] options, String[] values, boolean metricUnits) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      String displayValue;
      if (metricUnits) {
        displayValue = getString(R.string.value_integer_meter, value);
        switch (value) {
          case PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT:
            options[i] = getString(R.string.value_integer_meter_recommended, value);
            break;
          default:
            options[i] = displayValue;
        }
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        if (feet < 2000) {
          displayValue = getString(R.string.value_integer_feet, feet);
          switch (value) {
            case PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT:
              options[i] = getString(R.string.value_integer_feet_recommended, feet);
              break;
            default:
              options[i] = displayValue;
          }
        } else {
          double mile = feet * UnitConversions.FT_TO_MI;
          displayValue = getString(R.string.value_float_mile, mile);
          options[i] = displayValue;
        }
      }
      summary[i] = getString(
          R.string.settings_recording_max_recording_distance_summary, displayValue);
    }
  }

  /**
   * Sets the recording gps accuracy summary and options.
   * 
   * @param summary the summary
   * @param options the options
   * @param values the values
   * @param metricUnits true for metric units
   */
  private void setRecordingGpsAccuracySummaryAndOptions(
      String[] summary, String[] options, String[] values, boolean metricUnits) {
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
          switch (value) {
            case PreferencesUtils.RECORDING_GPS_ACCURACY_POOR:
              options[i] = getString(R.string.value_float_mile_poor_gps, mile);
              break;
            default:
              options[i] = displayValue;
          }
        }
      }
      summary[i] = getString(
          R.string.settings_recording_min_required_accuracy_summary, displayValue);
    }
  }

  /**
   * Sets the auto resume track timeout summary and options.
   * 
   * @param summary the summary
   * @param options the options
   * @param values the values
   */
  private void setAutoResumeTrackTimeoutSummaryAndOptions(
      String[] summary, String[] options, String[] values) {
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      switch (value) {
        case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_NEVER:
          options[i] = getString(R.string.value_never);
          summary[i] = getString(
              R.string.settings_recording_auto_resume_track_timeout_never_summary);
          break;
        case PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_ALWAYS:
          options[i] = getString(R.string.value_always);
          summary[i] = getString(
              R.string.settings_recording_auto_resume_track_timeout_always_summary);
          break;
        default:
          options[i] = getString(R.string.value_integer_minute, value);
          summary[i] = getString(
              R.string.settings_recording_auto_resume_track_timeout_summary, options[i]);
      }
    }
  }
}
