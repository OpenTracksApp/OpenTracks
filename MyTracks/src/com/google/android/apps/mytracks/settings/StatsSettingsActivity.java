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
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

/**
 * An activity for accessing stats settings.
 * 
 * @author Jimmy Shih
 */
public class StatsSettingsActivity extends AbstractSettingsActivity {

  private EditTextPreference weightPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.stats_settings);

    weightPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_stats_weight_display_key));
    configWeightPreference();
    updateWeightSummary(PreferencesUtils.isMetricUnits(this));

    /*
     * Note configureUnitsListPreference will trigger
     * configureRateListPreference
     */
    configUnitsListPreference();
  }

  /**
   * Configures the weight preference.
   */
  private void configWeightPreference() {
    weightPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        PreferencesUtils.storeWeightValue(StatsSettingsActivity.this, (String) newValue);
        updateWeightSummary(PreferencesUtils.isMetricUnits(StatsSettingsActivity.this));
        return true;
      }
    });

    weightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference pref) {
        double value = PreferencesUtils.getWeightDisplayValue(StatsSettingsActivity.this);
        ((EditTextPreference) pref).getEditText().setText(StringUtils.formatWeight(value));
        return true;
      }
    });
    weightPreference.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
        @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          Dialog dialog = weightPreference.getDialog();
          weightPreference.onClick(dialog, Dialog.BUTTON_POSITIVE);
          dialog.dismiss();
          return true;
        }
        return false;
      }
    });
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
        boolean metricUnits = PreferencesUtils.STATS_UNITS_DEFAULT.equals((String) newValue);
        configRateListPreference(metricUnits);
        updateWeightSummary(metricUnits);
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
   * Updates the weight summary.
   */
  private void updateWeightSummary(boolean metricUnits) {
    weightPreference.setSummary(getString(
        metricUnits ? R.string.value_kilogram : R.string.value_pound, StringUtils.formatWeight(
            PreferencesUtils.getWeightDisplayValue(StatsSettingsActivity.this, metricUnits))));
  }
}
