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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * An activity for accessing stats settings.
 * 
 * @author Jimmy Shih
 */
public class StatsSettingsActivity extends AbstractSettingsActivity {

  private String statsUnits;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.stats_settings);

    ListPreference preference = (ListPreference) findPreference(
        getString(R.string.stats_units_key));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        statsUnits = (String) newValue;
        updateUi();
        return true;
      }
    };
    String value = PreferencesUtils.getString(
        this, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    String[] values = getResources().getStringArray(R.array.stats_units_values);
    String[] options = getResources().getStringArray(R.array.stats_units_options);
    configureListPreference(preference, options, options, values, value, listener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    statsUnits = PreferencesUtils.getString(
        this, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    updateUi();
  }

  @SuppressWarnings("deprecation")
  private void updateUi() {
    CheckBoxPreference reportSpeedCheckBoxPreference = (CheckBoxPreference) findPreference(
        getString(R.string.report_speed_key));
    boolean metricUnits = PreferencesUtils.STATS_UNITS_DEFAULT.equals(statsUnits);
    reportSpeedCheckBoxPreference.setSummaryOn(
        metricUnits ? getString(R.string.description_speed_metric)
            : getString(R.string.description_speed_imperial));
    reportSpeedCheckBoxPreference.setSummaryOff(
        metricUnits ? getString(R.string.description_pace_metric)
            : getString(R.string.description_pace_imperial));
  }
}
