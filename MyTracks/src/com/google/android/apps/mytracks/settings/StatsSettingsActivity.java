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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;

/**
 * An activity for accessing stats settings.
 * 
 * @author Jimmy Shih
 */
public class StatsSettingsActivity extends AbstractSettingsActivity {

  /*
   * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
   * Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (key == null || key.equals(
              PreferencesUtils.getKey(StatsSettingsActivity.this, R.string.metric_units_key))) {
            metricUnits = PreferencesUtils.getBoolean(StatsSettingsActivity.this,
                R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
          }
          if (key != null) {
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                updateUi();
              }
            });
          }
        }
      };

  private SharedPreferences sharedPreferences;
  private boolean metricUnits = PreferencesUtils.METRIC_UNITS_DEFAULT;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.stats_settings);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
  }

  @Override
  protected void onStart() {
    super.onStart();
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateUi();
  }

  @Override
  protected void onStop() {
    super.onStop();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  @SuppressWarnings("deprecation")
  private void updateUi() {
    CheckBoxPreference reportSpeedCheckBoxPreference = (CheckBoxPreference) findPreference(
        getString(R.string.report_speed_key));
    reportSpeedCheckBoxPreference.setSummaryOn(
        metricUnits ? getString(R.string.description_speed_metric)
            : getString(R.string.description_speed_imperial));
    reportSpeedCheckBoxPreference.setSummaryOff(
        metricUnits ? getString(R.string.description_pace_metric)
            : getString(R.string.description_pace_imperial));
  }
}
