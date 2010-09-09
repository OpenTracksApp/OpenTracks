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

import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.maps.mytracks.R;

/**
 * A class that manages reading the shared preferences for the service.
 * 
 * @author Sandor Dornbush
 */
public class PreferenceManager {
  private TrackRecordingService service;
  private String announcement_frequency_key;
  private String max_recording_distance_key;
  private String metric_units_key;
  private String min_recording_distance_key;
  private String min_recording_interval_key;
  private String min_required_accuracy_key;
  private String recording_track_key;
  private String signal_sampling_frequency_key;
  private String split_frequency_key;
  
  public PreferenceManager(TrackRecordingService service) {
    this.service = service;
    announcement_frequency_key =
      service.getString(R.string.announcement_frequency_key);
    max_recording_distance_key = 
      service.getString(R.string.max_recording_distance_key);
    metric_units_key =
      service.getString(R.string.metric_units_key);
    min_recording_distance_key =
        service.getString(R.string.min_recording_distance_key);
    min_recording_interval_key =
      service.getString(R.string.min_recording_interval_key);
    min_required_accuracy_key =
      service.getString(R.string.min_required_accuracy_key);
    split_frequency_key =
      service.getString(R.string.split_frequency_key);
    signal_sampling_frequency_key =
      service.getString(R.string.signal_sampling_frequency_key);
    recording_track_key =
      service.getString(R.string.recording_track_key);
  }
  
  /**
   * Notifies that preferences have changed.
   * Call this with key == null to update all preferences in one call.
   *
   * @param key the key that changed (may be null to update all preferences)
   */
  public void onSharedPreferenceChanged(String key) {
    SharedPreferences sharedPreferences =
        service.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (sharedPreferences == null) {
      Log.w(MyTracksConstants.TAG,
          "TrackRecordingService: Couldn't get shared preferences.");
      return;
    }

    if (key == null || key.equals(min_recording_distance_key)) {
      service.setMinRecordingDistance(
          sharedPreferences.getInt(
              min_recording_distance_key,
              MyTracksSettings.DEFAULT_MIN_RECORDING_DISTANCE));
      Log.d(MyTracksConstants.TAG,
          "TrackRecordingService: minRecordingDistance = "
          + service.getMinRecordingDistance());
    }
    if (key == null || key.equals(max_recording_distance_key)) {
      service.setMaxRecordingDistance(
          sharedPreferences.getInt(
              max_recording_distance_key,
              MyTracksSettings.DEFAULT_MAX_RECORDING_DISTANCE));
    }
    if (key == null || key.equals(min_recording_interval_key)) {
      int minRecordingInterval = sharedPreferences.getInt(
          min_recording_interval_key,
          MyTracksSettings.DEFAULT_MIN_RECORDING_INTERVAL);
      switch (minRecordingInterval) {
        case -2:
          // Battery Miser
          // min: 30 seconds
          // max: 5 minutes
          // minDist: 5 meters Choose battery life over moving time accuracy.
          service.setLocationListenerPolicy(
              new AdaptiveLocationListenerPolicy(30000, 300000, 5));
          break;
        case -1:
          // High Accuracy
          // min: 1 second
          // max: 30 seconds
          // minDist: 0 meters get all updates to properly measure moving time.
          service.setLocationListenerPolicy(
              new AdaptiveLocationListenerPolicy(1000, 30000, 0));
          break;
        default:
          service.setLocationListenerPolicy(
              new AbsoluteLocationListenerPolicy(minRecordingInterval * 1000));
      }
    }
    if (key == null || key.equals(min_required_accuracy_key)) {
      service.setMinRequiredAccuracy(
          sharedPreferences.getInt(
              min_required_accuracy_key,
              MyTracksSettings.DEFAULT_MIN_REQUIRED_ACCURACY));
    }
    if (key == null || key.equals(announcement_frequency_key)) {
      service.setAnnouncementFrequency(
          sharedPreferences.getInt(announcement_frequency_key,
              -1));
    }
    if (key == null || key.equals(recording_track_key)) {
      service.setRecordingTrackId(
          sharedPreferences.getLong(recording_track_key, -1));
    }
    if (key == null || key.equals(split_frequency_key)) {
      service.getSplitManager().setSplitFrequency(
          sharedPreferences.getInt(split_frequency_key, 0));
    }
    if (key == null || key.equals(signal_sampling_frequency_key)) {
      service.getSignalManager().setFrequency(
          sharedPreferences.getInt(
              signal_sampling_frequency_key, -1), service);
    }
    if (key == null || key.equals(metric_units_key)) {
      service.getSplitManager().setMetricUnits(sharedPreferences.getBoolean(
          metric_units_key, true));
    }
  }
}
