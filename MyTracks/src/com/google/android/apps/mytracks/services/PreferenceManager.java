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

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.maps.mytracks.R;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * A class that manages reading the shared preferences for the service.
 * 
 * @author Sandor Dornbush
 */
public class PreferenceManager {
  private TrackRecordingService service;
  private final String announcementFrequencyKey;
  private final String autoResumeTrackTimeoutKey;
  private final String maxRecordingDistanceKey;
  private final String metricUnitsKey;
  private final String minRecordingDistanceKey;
  private final String minRecordingIntervalKey;
  private final String minRequiredAccuracyKey;
  private final String recordingTrackKey;
  private final String signalSamplingFrequencyKey;
  private final String splitFrequencyKey;

  public PreferenceManager(TrackRecordingService service) {
    this.service = service;

    announcementFrequencyKey =
        service.getString(R.string.announcement_frequency_key);
    autoResumeTrackTimeoutKey =
        service.getString(R.string.auto_resume_track_timeout_key);
    maxRecordingDistanceKey = 
        service.getString(R.string.max_recording_distance_key);
    metricUnitsKey =
        service.getString(R.string.metric_units_key);
    minRecordingDistanceKey =
        service.getString(R.string.min_recording_distance_key);
    minRecordingIntervalKey =
        service.getString(R.string.min_recording_interval_key);
    minRequiredAccuracyKey =
        service.getString(R.string.min_required_accuracy_key);
    splitFrequencyKey =
        service.getString(R.string.split_frequency_key);
    signalSamplingFrequencyKey =
        service.getString(R.string.signal_sampling_frequency_key);
    recordingTrackKey =
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

    if (key == null || key.equals(minRecordingDistanceKey)) {
      service.setMinRecordingDistance(
          sharedPreferences.getInt(
              minRecordingDistanceKey,
              MyTracksSettings.DEFAULT_MIN_RECORDING_DISTANCE));
      Log.d(MyTracksConstants.TAG,
          "TrackRecordingService: minRecordingDistance = "
          + service.getMinRecordingDistance());
    }
    if (key == null || key.equals(maxRecordingDistanceKey)) {
      service.setMaxRecordingDistance(sharedPreferences.getInt(
          maxRecordingDistanceKey,
          MyTracksSettings.DEFAULT_MAX_RECORDING_DISTANCE));
    }
    if (key == null || key.equals(minRecordingIntervalKey)) {
      int minRecordingInterval = sharedPreferences.getInt(
          minRecordingIntervalKey,
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
    if (key == null || key.equals(minRequiredAccuracyKey)) {
      service.setMinRequiredAccuracy(sharedPreferences.getInt(
          minRequiredAccuracyKey,
          MyTracksSettings.DEFAULT_MIN_REQUIRED_ACCURACY));
    }
    if (key == null || key.equals(announcementFrequencyKey)) {
      service.setAnnouncementFrequency(
          sharedPreferences.getInt(announcementFrequencyKey, -1));
    }
    if (key == null || key.equals(autoResumeTrackTimeoutKey)) {
      service.setAutoResumeTrackTimeout(sharedPreferences.getInt(
          autoResumeTrackTimeoutKey,
          MyTracksSettings.DEFAULT_AUTO_RESUME_TRACK_TIMEOUT));
    }
    if (key == null || key.equals(recordingTrackKey)) {
      service.setRecordingTrackId(
          sharedPreferences.getLong(recordingTrackKey, -1));
    }
    if (key == null || key.equals(splitFrequencyKey)) {
      service.getSplitManager().setSplitFrequency(
          sharedPreferences.getInt(splitFrequencyKey, 0));
    }
    if (key == null || key.equals(signalSamplingFrequencyKey)) {
      service.getSignalManager().setFrequency(
          sharedPreferences.getInt(signalSamplingFrequencyKey, -1), service);
    }
    if (key == null || key.equals(metricUnitsKey)) {
      service.getSplitManager().setMetricUnits(
          sharedPreferences.getBoolean(metricUnitsKey, true));
    }
  }
}
