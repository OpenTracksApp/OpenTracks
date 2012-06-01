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
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.preference.ListPreference;

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

    ListPreference minRecordingIntervalListPreference = (ListPreference) findPreference(
        getString(R.string.min_recording_interval_key));
    minRecordingIntervalListPreference.setEntries(getMinRecordingIntervalDisplayOptions());

    ListPreference minRecordingDistanceListPreference = (ListPreference) findPreference(
        getString(R.string.min_recording_distance_key));
    minRecordingDistanceListPreference.setEntries(
        getMinRecordingDistanceDisplayOptions(metricUnits));

    ListPreference maxRecordingDistanceListPreference = (ListPreference) findPreference(
        getString(R.string.max_recording_distance_key));
    maxRecordingDistanceListPreference.setEntries(
        getMaxRecordingDistanceDisplayOptions(metricUnits));

    ListPreference minRequiredAccuracyListPreference = (ListPreference) findPreference(
        getString(R.string.min_required_accuracy_key));
    minRequiredAccuracyListPreference.setEntries(getMinRequiredAccuracyDisplayOptions(metricUnits));

    ListPreference autoResumeTrackTimeoutListPreference = (ListPreference) findPreference(
        getString(R.string.auto_resume_track_timeout_key));
    autoResumeTrackTimeoutListPreference.setEntries(getAutoResumeTrackTimeoutDisplayOptions());
  }

  /**
   * Gets the min recording interval display options.
   */
  private String[] getMinRecordingIntervalDisplayOptions() {
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
  private String[] getMinRecordingDistanceDisplayOptions(boolean metricUnits) {
    String[] values = getResources().getStringArray(R.array.min_recording_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (metricUnits) {
        options[i] = getString(value == PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT
            ? R.string.value_integer_meter_recommended
            : R.string.value_integer_meter, value);
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        options[i] = getString(value == PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT
            ? R.string.value_integer_feet_recommended
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
  private String[] getMaxRecordingDistanceDisplayOptions(boolean metricUnits) {
    String[] values = getResources().getStringArray(R.array.max_recording_distance_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (metricUnits) {
        options[i] = getString(value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT
            ? R.string.value_integer_meter_recommended
            : R.string.value_integer_meter, value);
      } else {
        int feet = (int) (value * UnitConversions.M_TO_FT);
        if (feet < 2000) {
          options[i] = getString(value == PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT
              ? R.string.value_integer_feet_recommended
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
  private String[] getMinRequiredAccuracyDisplayOptions(boolean metricUnits) {
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
          options[i] = getString(value == PreferencesUtils.MIN_REQUIRED_ACCURACY_POOR
              ? R.string.value_float_mile_poor_gps
              : R.string.value_float_mile, mile);
        }
      }
    }
    return options;
  }

  /**
   * Gets the auto resume track timeout display options.
   */
  private String[] getAutoResumeTrackTimeoutDisplayOptions() {
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
