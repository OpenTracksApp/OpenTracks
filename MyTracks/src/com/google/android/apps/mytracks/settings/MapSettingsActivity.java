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

import android.app.Dialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

/**
 * An activity for accessing map settings.
 * 
 * @author Jimmy Shih
 */
public class MapSettingsActivity extends AbstractSettingsActivity {

  private static final String TAG = MapSettingsActivity.class.getSimpleName();

  private EditTextPreference slowPreference;
  private EditTextPreference mediumPreference;
  private EditTextPreference percentagePreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.map_settings);

    slowPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_slow_display_key));
    mediumPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_medium_display_key));
    percentagePreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_percentage_display_key));
    
    configTrackColorModePerference();
    configSpeedPreference(slowPreference, R.string.track_color_mode_slow_key,
        PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
    configSpeedPreference(mediumPreference, R.string.track_color_mode_medium_key,
        PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
    configPercentagePreference();

    updateSpeedSummary(slowPreference, R.string.track_color_mode_slow_key,
        PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
    updateSpeedSummary(mediumPreference, R.string.track_color_mode_medium_key,
        PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
    updatePercentageSummary();
  }

  /**
   * Configures the track color mode preference.
   */
  @SuppressWarnings("deprecation")
  private void configTrackColorModePerference() {
    ListPreference preference = (ListPreference) findPreference(
        getString(R.string.track_color_mode_key));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
        updateUiByTrackColorMode((String) newValue);
        return true;
      }
    };
    String value = PreferencesUtils.getString(
        this, R.string.track_color_mode_key, PreferencesUtils.TRACK_COLOR_MODE_DEFAULT);
    String[] values = getResources().getStringArray(R.array.track_color_mode_values);
    String[] options = getResources().getStringArray(R.array.track_color_mode_options);
    String[] summary = getResources().getStringArray(R.array.track_color_mode_summary);
    configureListPreference(preference, summary, options, values, value, listener);
  }

  /**
   * Configures the speed preference.
   * 
   * @param preference the preference
   * @param key the key 
   * @param defaultValue the default value
   */
  private void configSpeedPreference(
      EditTextPreference preference, final int key, final int defaultValue) {
    preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference pref, Object newValue) {
          
        // Need to obtain the current min and max values  
        int minValue;
        int maxValue;
        if (key == R.string.track_color_mode_slow_key) {
          minValue = 0;
          maxValue = PreferencesUtils.getInt(
              MapSettingsActivity.this, R.string.track_color_mode_medium_key,
              PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
        } else {
          minValue = PreferencesUtils.getInt(
              MapSettingsActivity.this, R.string.track_color_mode_slow_key,
              PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
          maxValue = Integer.MAX_VALUE;
        }
        storeSpeedValue(key, minValue, maxValue, defaultValue, (String) newValue);
        updateSpeedSummary(pref, key, defaultValue);
        return true;
      }
    });
    preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference pref) {
        int value = getSpeedDisplayValue(key, defaultValue);
        ((EditTextPreference) pref).getEditText().setText(String.valueOf(value));
        return true;
      }
    });
    configImeActionDone(preference);
  }

  /**
   * Configures the percentage preference.
   */
  private void configPercentagePreference() {
    percentagePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
        if (value < 0) {
          value = PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT;
        }
        PreferencesUtils.setInt(
            MapSettingsActivity.this, R.string.track_color_mode_percentage_key, value);
        updatePercentageSummary();
        return true;
      }
    });
    percentagePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        int value = PreferencesUtils.getInt(
            MapSettingsActivity.this, R.string.track_color_mode_percentage_key,
            PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT);
        ((EditTextPreference) preference).getEditText().setText(String.valueOf(value));
        return true;
      }
    });
    configImeActionDone(percentagePreference);
  }

  /**
   * Configures the IME action done.
   * 
   * @param editTextPreference the edit text preference
   */
  private void configImeActionDone(final EditTextPreference editTextPreference) {
    editTextPreference.getEditText()
        .setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
              Dialog dialog = editTextPreference.getDialog();
              editTextPreference.onClick(dialog, Dialog.BUTTON_POSITIVE);
              dialog.dismiss();
              return true;
            }
            return false;
          }
        });
  }

  /**
   * Updates the UI by the track color mode.
   * 
   * @param trackColorMode the track color mode
   */
  private void updateUiByTrackColorMode(String trackColorMode) {
    boolean isFixedValue = trackColorMode.equals(
        getString(R.string.settings_map_track_color_mode_fixed_value));
    boolean isDynamicValue = trackColorMode.equals(
        getString(R.string.settings_map_track_color_mode_dynamic_value));
    slowPreference.setEnabled(isFixedValue);
    mediumPreference.setEnabled(isFixedValue);
    percentagePreference.setEnabled(isDynamicValue);
  }

  /**
   * Updates a speed summary.
   * 
   * @param preference the preference
   * @param keyId the key id
   * @param defaultValue the default value
   */
  private void updateSpeedSummary(Preference preference, int keyId, int defaultValue) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(this);
    int displayValue = getSpeedDisplayValue(keyId, defaultValue);
    preference.setSummary(getString(
        metricUnits ? R.string.value_integer_kilometer_hour : R.string.value_integer_mile_hour,
        displayValue));
  }

  /**
   * Updates the percentage summary.
   */
  private void updatePercentageSummary() {
    int value = PreferencesUtils.getInt(this, R.string.track_color_mode_percentage_key,
        PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT);
    percentagePreference.setSummary(getString(R.string.settings_map_percentage_summary, value));
  }

  /**
   * Stores a speed value, always in metric units.
   * 
   * @param keyId the key id
   * @param minValue the min value
   * @param maxValue the max value
   * @param defaultValue the default value
   * @param displayValue the display value
   */
  private void storeSpeedValue(
      int keyId, int minValue, int maxValue, int defaultValue, String displayValue) {
    int value;
    try {
      value = Integer.parseInt(displayValue);
      if (!PreferencesUtils.isMetricUnits(this)) {
        value = (int) (value * UnitConversions.MI_TO_KM);
      }
    } catch (NumberFormatException e) {
      Log.e(TAG, "invalid value " + displayValue);
      value = defaultValue;
    }

    if (value > maxValue) {
      value = maxValue;
    }
    if (value < minValue) {
      value = minValue;
    }
    PreferencesUtils.setInt(this, keyId, value);
  }

  /**
   * Gets a speed display value in metric or imperial depending on the preferred
   * units.
   * 
   * @param keyId the key id
   * @param defaultValue the default value
   */
  private int getSpeedDisplayValue(int keyId, int defaultValue) {
    int value = PreferencesUtils.getInt(this, keyId, defaultValue);
    if (!PreferencesUtils.isMetricUnits(this)) {
      value = (int) (value * UnitConversions.KM_TO_MI);
    }
    return value;
  }
}
