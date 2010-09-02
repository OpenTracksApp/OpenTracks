/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.services.SafeStatusAnnouncerTask;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * An activity that let's the user see and edit the settings.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MyTracksSettings extends PreferenceActivity {

  public static final String SETTINGS_NAME = "MyTracksSettings";

  /*
   * Default values - keep in sync with those in preferences.xml.
   */
  public static final int DEFAULT_ANNOUNCEMENT_FREQUENCY = -1;
  public static final int DEFAULT_MAX_RECORDING_DISTANCE = 200;
  public static final int DEFAULT_MIN_RECORDING_DISTANCE = 5;
  public static final int DEFAULT_MIN_RECORDING_INTERVAL = 0;
  public static final int DEFAULT_MIN_REQUIRED_ACCURACY = 200;
  public static final int DEFAULT_SPLIT_FREQUENCY = 0;

  private static boolean mTTSAvailable;

  /* establish whether the tts class is available to us */
  static {
    try {
      SafeStatusAnnouncerTask.checkAvailable();
      mTTSAvailable = true;
    } catch (Throwable t) {
      Log.d(MyTracksConstants.TAG, "TTS not available.", t);
      mTTSAvailable = false;
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Tell it where to read/write preferences
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(SETTINGS_NAME);
    preferenceManager.setSharedPreferencesMode(0);

    // Load the preferences to be displayed
    addPreferencesFromResource(R.xml.preferences);

    // Hook up switching of displayed list entries between metric and imperial
    // units
    CheckBoxPreference metricUnitsPreference =
        (CheckBoxPreference) findPreference(getString(R.string.metric_units_key));
    metricUnitsPreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference,
              Object newValue) {
            boolean isMetric = (Boolean) newValue;
            updatePreferenceUnits(isMetric);
            return true;
          }
        });
    updatePreferenceUnits(metricUnitsPreference.isChecked());

    // Disable TTS announcement preference if not available
    if (!mTTSAvailable) {
      IntegerListPreference announcementFrequency =
          (IntegerListPreference) findPreference(
              getString(R.string.announcement_frequency_key));
      announcementFrequency.setEnabled(false);
      announcementFrequency.setValue("-1");
      announcementFrequency.setSummary(
          R.string.settings_announcement_not_available_summary);
    }
  }

  /**
   * Updates all the preferences which give options with distance units to use
   * the proper unit the user has selected.
   *
   * @param isMetric true to use metric units, false to use imperial
   */
  private void updatePreferenceUnits(boolean isMetric) {
    final ListPreference minRecordingDistance =
        (ListPreference) findPreference(
            getString(R.string.min_recording_distance_key));
    final ListPreference maxRecordingDistance =
        (ListPreference) findPreference(
            getString(R.string.max_recording_distance_key));
    final ListPreference minRequiredAccuracy =
        (ListPreference) findPreference(
            getString(R.string.min_required_accuracy_key));
    final ListPreference splitFrequency =
        (ListPreference) findPreference(
            getString(R.string.split_frequency_key));

    minRecordingDistance.setEntries(isMetric
        ? R.array.min_recording_distance_options
        : R.array.min_recording_distance_options_ft);
    maxRecordingDistance.setEntries(isMetric
        ? R.array.max_recording_distance_options
        : R.array.max_recording_distance_options_ft);
    minRequiredAccuracy.setEntries(isMetric
        ? R.array.min_required_accuracy_options
        : R.array.min_required_accuracy_options_ft);
    splitFrequency.setEntries(isMetric
        ? R.array.split_frequency_options
        : R.array.split_frequency_options_ft);
  }
}
