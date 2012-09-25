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

  private EditTextPreference slowEditTextPreference;
  private EditTextPreference mediumEditTextPreference;
  private EditTextPreference percentageEditTextPreference;

  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.map_settings);

    slowEditTextPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_slow_display_key));
    mediumEditTextPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_medium_display_key));
    percentageEditTextPreference = (EditTextPreference) findPreference(
        getString(R.string.settings_map_percentage_display_key));

    ListPreference trackColorModeListPreference = (ListPreference) findPreference(
        getString(R.string.track_color_mode_key));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateUiByTrackColorMode((String) newValue);
        return true;
      }
    };
    String trackColorModeValue = PreferencesUtils.getString(
        this, R.string.track_color_mode_key, PreferencesUtils.TRACK_COLOR_MODE_DEFAULT);
    configurePreference(trackColorModeListPreference,
        getResources().getStringArray(R.array.track_color_mode_options),
        getResources().getStringArray(R.array.track_color_mode_values),
        R.string.settings_map_track_color_mode_summary, trackColorModeValue, listener);

    configureSpeedEditTextPreference(R.string.track_color_mode_slow_key);
    configureSpeedEditTextPreference(R.string.track_color_mode_medium_key);
    configurePercentageEditTextPreference();

    updatePercentageSummary();
  }

  /**
   * Configures the speed edit text preference.
   * 
   * @param keyId the key id
   */
  private void configureSpeedEditTextPreference(final int keyId) {
    final EditTextPreference editTextPreference = keyId == R.string.track_color_mode_slow_key ? slowEditTextPreference
        : mediumEditTextPreference;
    editTextPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        storeSpeedValue(keyId, (String) newValue);
        updateSpeedSummary(keyId);
        return true;
      }
    });
    editTextPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        int value = getSpeedDisplayValue(keyId);
        ((EditTextPreference) preference).getEditText().setText(String.valueOf(value));
        return true;
      }
    });
    configureImeActionDone(editTextPreference);
  }

  /**
   * Configures the percentage edit text preference.
   */
  private void configurePercentageEditTextPreference() {
    percentageEditTextPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
    percentageEditTextPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
      public boolean onPreferenceClick(Preference preference) {
        int value = PreferencesUtils.getInt(
            MapSettingsActivity.this, R.string.track_color_mode_percentage_key,
            PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT);
        ((EditTextPreference) preference).getEditText().setText(String.valueOf(value));
        return true;
      }
    });
    configureImeActionDone(percentageEditTextPreference);
  }

  /**
   * Configures the IME action done.
   * 
   * @param editTextPreference the edit text preference
   */
  private void configureImeActionDone(final EditTextPreference editTextPreference) {
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

  @Override
  protected void onResume() {
    super.onResume();
    updateSpeedSummary(R.string.track_color_mode_slow_key);
    updateSpeedSummary(R.string.track_color_mode_medium_key);
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
    slowEditTextPreference.setEnabled(isFixedValue);
    mediumEditTextPreference.setEnabled(isFixedValue);
    percentageEditTextPreference.setEnabled(isDynamicValue);
  }

  /**
   * Stores the speed value, always in metric units.
   * 
   * @param keyId the key id
   * @param displayValue the display value
   */
  private void storeSpeedValue(int keyId, String displayValue) {
    int maxValue;
    int minValue;
    int defaultValue;
    if (keyId == R.string.track_color_mode_slow_key) {
      minValue = 0;
      maxValue = PreferencesUtils.getInt(this, R.string.track_color_mode_medium_key,
          PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
      defaultValue = PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT;
    } else {
      minValue = PreferencesUtils.getInt(
          this, R.string.track_color_mode_slow_key, PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
      maxValue = Integer.MAX_VALUE;
      defaultValue = PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT;
    }
    int value;
    try {
      value = Integer.parseInt(displayValue);
      if (!PreferencesUtils.getBoolean(
          this, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT)) {
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
   * Gets the speed display value, in metric or imperial depending on the
   * preferred units.
   * 
   * @param keyId the key id
   */
  private int getSpeedDisplayValue(int keyId) {
    int defaultValue = keyId == R.string.track_color_mode_slow_key ? PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT
        : PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT;
    int value = PreferencesUtils.getInt(this, keyId, defaultValue);
    if (!PreferencesUtils.getBoolean(
        this, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT)) {
      value = (int) (value * UnitConversions.KM_TO_MI);
    }
    return value;
  }

  /**
   * Updates the speed summary.
   * 
   * @param keyId the key id
   */
  private void updateSpeedSummary(int keyId) {
    EditTextPreference editTextPreference = keyId == R.string.track_color_mode_slow_key ? slowEditTextPreference
        : mediumEditTextPreference;
    boolean metric = PreferencesUtils.getBoolean(
        this, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
    int displayValue = getSpeedDisplayValue(keyId);
    editTextPreference.setSummary(getString(
        metric ? R.string.value_integer_kilometer_hour : R.string.value_integer_mile_hour,
        displayValue));
  }

  /**
   * Updates the percentage summary.
   */
  private void updatePercentageSummary() {
    int value = PreferencesUtils.getInt(MapSettingsActivity.this,
        R.string.track_color_mode_percentage_key,
        PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT);
    percentageEditTextPreference.setSummary(getString(R.string.settings_map_percentage_summary)
        + "\n" + getString(R.string.value_integer_percent, value));
  }
}
