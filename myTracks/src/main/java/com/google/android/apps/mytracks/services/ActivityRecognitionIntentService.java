/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.maps.mytracks.R;

import android.app.IntentService;
import android.content.Intent;

/**
 * Service that receives ActivityRecognition updates.
 * 
 * @author Jimmy Shih
 */
public class ActivityRecognitionIntentService extends IntentService {

  private static final int REQUIRED_CONFIDENCE = 90;

  public ActivityRecognitionIntentService() {
    super(ActivityRecognitionIntentService.class.getSimpleName());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!ActivityRecognitionResult.hasResult(intent)) {
      return;
    }
    ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
    DetectedActivity detectedActivity = result.getMostProbableActivity();
    if (detectedActivity == null) {
      return;
    }
    if (detectedActivity.getConfidence() < REQUIRED_CONFIDENCE) {
      return;
    }

    long recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
    if (recordingTrackId == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
      return;
    }
    boolean recordingTrackPaused = PreferencesUtils.getBoolean(
        this, R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
    if (recordingTrackPaused) {
      return;
    }

    int currentType = PreferencesUtils.getInt(this, R.string.activity_recognition_type_key,
        PreferencesUtils.ACTIVITY_RECOGNITION_TYPE_DEFAULT);

    switch (detectedActivity.getType()) {
      case DetectedActivity.IN_VEHICLE:
        if (currentType != DetectedActivity.IN_VEHICLE) {
          PreferencesUtils.setInt(
              this, R.string.activity_recognition_type_key, DetectedActivity.IN_VEHICLE);
        }
        break;
      case DetectedActivity.ON_BICYCLE:
        if (currentType != DetectedActivity.IN_VEHICLE
            && currentType != DetectedActivity.ON_BICYCLE) {
          PreferencesUtils.setInt(
              this, R.string.activity_recognition_type_key, DetectedActivity.ON_BICYCLE);
        }
        break;
      case DetectedActivity.ON_FOOT:
        if (currentType != DetectedActivity.IN_VEHICLE && currentType != DetectedActivity.ON_BICYCLE
            && currentType != DetectedActivity.ON_FOOT) {
          PreferencesUtils.setInt(
              this, R.string.activity_recognition_type_key, DetectedActivity.ON_FOOT);
        }
        break;
      default:
        break;
    }
  }
}
