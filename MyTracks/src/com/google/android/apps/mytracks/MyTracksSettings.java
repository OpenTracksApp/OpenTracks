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

  public static final String ANNOUNCEMENT_FREQUENCY = "announcementFrequency";
  public static final String DEFAULT_MAP_PUBLIC = "defaultMapPublic";
  public static final String MAX_RECORDING_DISTANCE = "maxRecordingDistance";
  public static final String METRIC_UNITS = "metricUnits";
  public static final String MIN_RECORDING_DISTANCE = "minRecordingDistance";
  public static final String MIN_RECORDING_INTERVAL = "minRecordingInterval";
  public static final String MIN_REQUIRED_ACCURACY = "minRequiredAccuracy";
  public static final String PICK_EXISTING_MAP = "pickExistingMap";
  public static final String RECORDING_TRACK = "recordingTrack";
  public static final String REPORT_SPEED = "reportSpeed";
  public static final String SELECTED_TRACK = "selectedTrack";
  public static final String SEND_STATS_AND_POINTS = "sendStatsAndPoints";
  public static final String SEND_TO_DOCS = "sendToDocs";
  public static final String SEND_TO_MYMAPS = "sendToMyMaps";
  public static final String SHARE_URL_ONLY = "shareUrlOnly";
  public static final String SIGNAL_SAMPLING_FREQUENCY =
      "signalSamplingFrequency";
  public static final String SPLIT_FREQUENCY = "splitFrequency";
  public static final String WRITE_TO_SD_CARD = "writeToSdCard";

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
        (CheckBoxPreference) findPreference(METRIC_UNITS);
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
          (IntegerListPreference) findPreference(ANNOUNCEMENT_FREQUENCY);
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
        (ListPreference) findPreference(MIN_RECORDING_DISTANCE);
    final ListPreference maxRecordingDistance =
        (ListPreference) findPreference(MAX_RECORDING_DISTANCE);
    final ListPreference minRequiredAccuracy =
        (ListPreference) findPreference(MIN_REQUIRED_ACCURACY);
    final ListPreference splitFrequency =
        (ListPreference) findPreference(SPLIT_FREQUENCY);

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
