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
package com.google.android.apps.mytracks.signalstrength;

import android.content.ComponentName;
import android.os.Build;

/**
 * Constants for the signal sampler.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthConstants {
  // TODO: Many of these could be moved into a common library for third-party apps
  public static final String START_TRACK_ACTION =
      "com.google.android.apps.mytracks.TRACK_STARTED";
  public static final String PAUSE_TRACK_ACTION =
      "com.google.android.apps.mytracks.TRACK_PAUSED";
  public static final String RESUME_TRACK_ACTION =
      "com.google.android.apps.mytracks.TRACK_RESUMED";
  public static final String STOP_TRACK_ACTION =
      "com.google.android.apps.mytracks.TRACK_STOPPED";
  public static final String TRACK_ID_EXTRA =
      "com.google.android.apps.mytracks.TRACK_ID";
  public static final String MYTRACKS_SERVICE =
      "com.google.android.apps.mytracks.services.TrackRecordingService";
  public static final String MYTRACKS_SERVICE_PACKAGE =
      "com.google.android.maps.mytracks";
  public static final ComponentName MYTRACKS_SERVICE_COMPONENT =
      new ComponentName(MYTRACKS_SERVICE_PACKAGE, MYTRACKS_SERVICE);

  public static final String START_SAMPLING =
      "com.google.android.apps.mytracks.signalstrength.START";
  public static final String STOP_SAMPLING =
    "com.google.android.apps.mytracks.signalstrength.STOP";

  public static final int ANDROID_API_LEVEL = Integer.parseInt(
      Build.VERSION.SDK);

  public static final String TAG = "SignalStrengthSampler";

  private SignalStrengthConstants() {}
}
