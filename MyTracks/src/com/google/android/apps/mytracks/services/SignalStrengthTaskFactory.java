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
import com.google.android.apps.mytracks.util.ApiFeatures;

import android.content.Context;
import android.util.Log;

/**
 * Factory for producing a proper {@link SignalStrengthTask} according to the
 * current API level.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthTaskFactory {
  private final boolean hasModernSignalStrength;

  SignalStrengthTaskFactory(ApiFeatures apiFeatures) {
    this.hasModernSignalStrength = apiFeatures.hasModernSignalStrength();
  }

  public PeriodicTask create(Context context) {
    if (hasModernSignalStrength) {
      Log.d(MyTracksConstants.TAG,
          "TrackRecordingService using modern signal strength api.");
      return new SignalStrengthTaskModern(context);
    } else {
      Log.w(MyTracksConstants.TAG,
          "TrackRecordingService using legacy signal strength api.");
      return new SignalStrengthTask(context);
    }
  }
}
