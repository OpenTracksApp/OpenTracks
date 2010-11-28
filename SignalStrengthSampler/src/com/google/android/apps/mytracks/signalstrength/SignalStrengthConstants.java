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

import android.os.Build;

/**
 * Constants for the signal sampler.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthConstants {
  public static final String START_SAMPLING =
      "com.google.android.apps.mytracks.signalstrength.START";
  public static final String STOP_SAMPLING =
    "com.google.android.apps.mytracks.signalstrength.STOP";

  public static final int ANDROID_API_LEVEL = Integer.parseInt(
      Build.VERSION.SDK);

  public static final String TAG = "SignalStrengthSampler";

  private SignalStrengthConstants() {}
}
