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
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

/**
 * An activity for accessing stats settings.
 * 
 * @author Jimmy Shih
 */
public class StatsSettingsActivity extends AbstractSettingsActivity {
  
  private static final String TAG = MapSettingsActivity.class.getSimpleName();

  private CheckBoxPreference caloriePreference;
  private EditTextPreference weightPreference;

  
  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.stats_settings);
    
    caloriePreference = (CheckBoxPreference) findPreference(getString(R.string.stats_show_calorie_key));
    weightPreference = (EditTextPreference) findPreference(getString(R.string.stats_weight_key));

    configCaloriePreference(caloriePreference, R.string.stats_show_calorie_key,
        PreferencesUtils.STATS_SHOW_CALORIE_DEFAULT);
    configWeightPreference(weightPreference, R.string.stats_weight_key,
        PreferencesUtils.STATS_WEIGHT_DEFAULT, caloriePreference.isChecked());
    /*
     * Note configureUnitsListPreference will trigger
     * configureRateListPreference
     */
    configUnitsListPreference();
  }

  /**
   * Configures the preferred units list preference.
   */
  private void configUnitsListPreference() {
    @SuppressWarnings("deprecation")
    ListPreference listPreference = (ListPreference) findPreference(
        getString(R.string.stats_units_key));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        configRateListPreference(PreferencesUtils.STATS_UNITS_DEFAULT.equals((String) newValue));
        return true;
      }
    };
    String value = PreferencesUtils.getString(
        this, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    String[] values = getResources().getStringArray(R.array.stats_units_values);
    String[] options = getResources().getStringArray(R.array.stats_units_options);
    configureListPreference(listPreference, options, options, values, value, listener);
  }

  /**
   * Configures the preferred rate list preference.
   * 
   * @param metricUnits true if metric units
   */
  private void configRateListPreference(boolean metricUnits) {
    @SuppressWarnings("deprecation")
    ListPreference listPreference = (ListPreference) findPreference(
        getString(R.string.stats_rate_key));
    String value = PreferencesUtils.getString(
        this, R.string.stats_rate_key, PreferencesUtils.STATS_RATE_DEFAULT);
    String[] values = getResources().getStringArray(R.array.stats_rate_values);
    String[] options = getResources().getStringArray(
        metricUnits ? R.array.stats_rate_metric_options : R.array.stats_rate_imperial_options);
    configureListPreference(listPreference, options, options, values, value, null);
  }
  
  /**
   * Configures the calorie preference.
   * 
   * @param reference to configure
   * @param key of the preference
   * @param defaultValue default value of this preference
   */
  private void configCaloriePreference(CheckBoxPreference preference, int key, boolean defaultValue) {
    PreferencesUtils.setBoolean(this, key, PreferencesUtils.getBoolean(this, key, defaultValue));
    preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @SuppressWarnings("hiding")
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        if (value) {
          weightPreference.setEnabled(true);
        } else {
          weightPreference.setEnabled(false);
        }
        return true;
      }
    });
  }

  /**
   * Configures the weight preference.
   * 
   * @param preference to configure
   * @param key of the preference
   * @param defaultValue default value of this preference
   * @param isEnable true means enable the weight preference
   */
  private void configWeightPreference(EditTextPreference preference, int key, int defaultValue,
      boolean isEnable) {
    PreferencesUtils.setInt(this, key, PreferencesUtils.getInt(this, key, defaultValue));
    if (isEnable) {
      preference.setEnabled(true);
    } else {
      preference.setEnabled(false);
    }

    preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @SuppressWarnings("hiding")
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String displayValue = (String) newValue;
        int value;
        try {
          value = Integer.parseInt(displayValue);
        } catch (NumberFormatException e) {
          Log.e(TAG, "invalid value " + displayValue);
          value = PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT;
        }
        PreferencesUtils.setInt(StatsSettingsActivity.this, R.string.stats_weight_key, value);
        return true;
      }
    });
  }
}
