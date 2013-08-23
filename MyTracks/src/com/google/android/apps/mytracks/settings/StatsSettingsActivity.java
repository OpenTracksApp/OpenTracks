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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

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

    configCaloriePreference(caloriePreference);
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
    ListPreference listPreference = (ListPreference) findPreference(getString(R.string.stats_units_key));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        configRateListPreference(PreferencesUtils.STATS_UNITS_DEFAULT.equals((String) newValue));

        /*
         * It is necessary to change the weight summary as they are in the same
         * activity.
         */
        updateWeightSummary(weightPreference, R.string.stats_weight_key,
            PreferencesUtils.STATS_WEIGHT_DEFAULT,
            PreferencesUtils.STATS_UNITS_DEFAULT.equals((String) newValue));
        return true;
      }
    };
    String value = PreferencesUtils.getString(this, R.string.stats_units_key,
        PreferencesUtils.STATS_UNITS_DEFAULT);
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
    ListPreference listPreference = (ListPreference) findPreference(getString(R.string.stats_rate_key));
    String value = PreferencesUtils.getString(this, R.string.stats_rate_key,
        PreferencesUtils.STATS_RATE_DEFAULT);
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
  private void configCaloriePreference(CheckBoxPreference preference) {
    preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @SuppressWarnings("hiding")
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        if (value) {
          weightPreference.setEnabled(true);
          showWeightInputPreferenceDialog();
        } else {
          weightPreference.setEnabled(false);
        }
        return true;
      }
    });
  }

  /**
   * Shows this dialog to input weight value while enable show calorie and the
   * value is not set yet.
   */
  private void showWeightInputPreferenceDialog() {
    int value = PreferencesUtils.getInt(this, R.string.stats_weight_key, -1);
    if (value == -1) {
      final EditText weightInput = new EditText(this);
      weightInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
      weightInput.setText(Integer.toString(PreferencesUtils.STATS_WEIGHT_DEFAULT));
      weightInput.setSelectAllOnFocus(true);
      AlertDialog.Builder inputDialogBuilder = new AlertDialog.Builder(this);
      inputDialogBuilder
          .setTitle(R.string.settings_stats_calorie_weight)
          .setMessage(R.string.settings_stats_calorie_weight_description)
          .setCancelable(false)
          .setView(weightInput)
          .setPositiveButton(getString(R.string.generic_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              storeWeightValue(R.string.stats_weight_key, PreferencesUtils.STATS_WEIGHT_DEFAULT,
                  weightInput.getText().toString());
              updateWeightSummary(weightPreference, R.string.stats_weight_key,
                  PreferencesUtils.STATS_WEIGHT_DEFAULT);
              dialog.cancel();
            }
          })
          .setNegativeButton(getString(R.string.generic_cancel),
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
      inputDialogBuilder.create().show();
    }
  }

  /**
   * Configures the weight preference.
   * 
   * @param preference to configure
   * @param key of the preference
   * @param defaultValue default value of this preference
   * @param isEnable true means enable the weight preference
   */
  private void configWeightPreference(EditTextPreference preference, final int key,
      final int defaultValue, boolean isEnable) {
    updateWeightSummary(preference, key, defaultValue);
    if (isEnable) {
      preference.setEnabled(true);
    } else {
      preference.setEnabled(false);
    }

    preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @SuppressWarnings("hiding")
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        storeWeightValue(key, defaultValue, (String) newValue);
        updateWeightSummary(preference, key, defaultValue);
        return true;
      }
    });

    preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference pref) {
        int value = getWeightDisplayValue(key, defaultValue,
            PreferencesUtils.isMetricUnits(getApplicationContext()));
        ((EditTextPreference) pref).getEditText().setText(String.valueOf(value));
        return true;
      }
    });
  }

  /**
   * Updates the weight summary.
   * 
   * @param preference the preference
   * @param keyId the key id
   * @param defaultValue the default value
   */
  private void updateWeightSummary(Preference preference, int keyId, int defaultValue) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(this);
    updateWeightSummary(preference, keyId, defaultValue, metricUnits);
  }

  /**
   * Updates the weight summary.
   * 
   * @param preference the preference
   * @param keyId the key id
   * @param defaultValue the default value
   * @param metricUnits the status of metric units
   */
  private void updateWeightSummary(Preference preference, int keyId, int defaultValue,
      boolean metricUnits) {
    int displayValue = getWeightDisplayValue(keyId, defaultValue, metricUnits);
    preference.setSummary(getString(metricUnits ? R.string.value_integer_kilogram
        : R.string.value_integer_pound, displayValue));
  }

  /**
   * Gets the weight display value in metric or imperial depending on the
   * preferred units.
   * 
   * @param keyId the key id
   * @param defaultValue the default value
   */
  private int getWeightDisplayValue(int keyId, int defaultValue, boolean metricUnits) {
    int value = PreferencesUtils.getInt(this, keyId, defaultValue);
    if (!metricUnits) {
      value = (int) (value * UnitConversions.KG_TO_LB);
    }
    return value;
  }

  /**
   * Stores the weight value, always in metric units.
   * 
   * @param keyId the key id
   * @param defaultValue the default value
   * @param displayValue the display value
   */
  private void storeWeightValue(int keyId, int defaultValue, String displayValue) {
    int value;
    try {
      value = Integer.parseInt(displayValue);
      if (!PreferencesUtils.isMetricUnits(this)) {
        value = (int) (value * UnitConversions.LB_TO_KG);
      }
    } catch (NumberFormatException e) {
      Log.e(TAG, "invalid value " + displayValue);
      value = defaultValue;
    }

    PreferencesUtils.setInt(this, keyId, value);
  }
}
