/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

/**
 * A class that manages reading the shared preferences for the service.
 * 
 * @author Sandor Dornbush
 */
public class PreferenceManager implements OnSharedPreferenceChangeListener {
  private TrackRecordingService service;
  private SharedPreferences sharedPreferences;

  public PreferenceManager(TrackRecordingService service) {
    this.service = service;
    this.sharedPreferences = service.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    // Refresh all properties.
    onSharedPreferenceChanged(sharedPreferences, null);
  }

  /**
   * Notifies that preferences have changed. Call this with key == null to
   * update all preferences in one call.
   * 
   * @param key the key that changed (may be null to update all preferences)
   */
  @Override
  public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    if (service == null) {
      Log.w(Constants.TAG, "onSharedPreferenceChanged: a preference change (key = " + key
          + ") after a call to shutdown()");
      return;
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.recording_track_id_key).equals(key)) {
      long recordingTrackId = PreferencesUtils.getLong(service, R.string.recording_track_id_key);
      /*
       * Only set the id if it is valid. Setting it to -1L should only happen
       * in TrackRecordingService.endCurrentTrack()
       */
      if (recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
        service.setRecordingTrackId(recordingTrackId);
      }
    }
    if (key == null || PreferencesUtils.getKey(service, R.string.metric_units_key).equals(key)) {
      service.setMetricUnits(PreferencesUtils.getBoolean(
          service, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT));
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.announcement_frequency_key).equals(key)) {
      service.setAnnouncementFrequency(PreferencesUtils.getInt(
          service, R.string.announcement_frequency_key,
          PreferencesUtils.ANNOUNCEMENT_FREQUENCY_DEFAULT));
    }
    if (key == null || PreferencesUtils.getKey(service, R.string.split_frequency_key).equals(key)) {
      service.setSplitFrequency(PreferencesUtils.getInt(
          service, R.string.split_frequency_key, PreferencesUtils.SPLIT_FREQUENCY_DEFAULT));
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.min_recording_interval_key).equals(key)) {
      int minRecordingInterval = PreferencesUtils.getInt(service,
          R.string.min_recording_interval_key, PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT);
      switch (minRecordingInterval) {
        case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE:
          /*
           * Choose battery life over moving time accuracy. min: 30 seconds,
           * max: 5 minutes, inDist: 5 meters.
           */
          service.setLocationListenerPolicy(new AdaptiveLocationListenerPolicy(30000, 300000, 5));
          break;
        case PreferencesUtils.MIN_RECORDING_INTERVAL_ADAPT_ACCURACY:
          /*
           * Get all the updates. min: 1 second, max: 30 seconds, minDist: 0
           * meter.
           */
          service.setLocationListenerPolicy(new AdaptiveLocationListenerPolicy(1000, 30000, 0));
          break;
        default:
          service.setLocationListenerPolicy(
              new AbsoluteLocationListenerPolicy(minRecordingInterval * 1000));
      }
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.min_recording_distance_key).equals(key)) {
      service.setMinRecordingDistance(PreferencesUtils.getInt(
          service, R.string.min_recording_distance_key,
          PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT));
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.max_recording_distance_key).equals(key)) {
      service.setMaxRecordingDistance(PreferencesUtils.getInt(
          service, R.string.max_recording_distance_key,
          PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT));
    }

    if (key == null
        || PreferencesUtils.getKey(service, R.string.min_required_accuracy_key).equals(key)) {
      service.setMinRequiredAccuracy(PreferencesUtils.getInt(
          service, R.string.min_required_accuracy_key,
          PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT));
    }
    if (key == null
        || PreferencesUtils.getKey(service, R.string.auto_resume_track_timeout_key).equals(key)) {
      service.setAutoResumeTrackTimeout(PreferencesUtils.getInt(
          service, R.string.auto_resume_track_timeout_key,
          PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT));
    }
  }

  public void shutdown() {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    service = null;
  }
}
