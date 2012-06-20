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

import static com.google.android.apps.mytracks.signalstrength.SignalStrengthConstants.*;

import com.google.android.apps.mytracks.signalstrength.SignalStrengthListener.SignalStrengthCallback;

import android.content.Context;
import android.util.Log;

/**
 * Factory for producing a proper {@link SignalStrengthListenerCupcake} according to the
 * current API level.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthListenerFactory {

  public SignalStrengthListener create(Context context, SignalStrengthCallback callback) {
    if (hasModernSignalStrength()) {
      Log.d(TAG, "TrackRecordingService using modern signal strength api.");
      return new SignalStrengthListenerEclair(context, callback);
    } else {
      Log.w(TAG, "TrackRecordingService using legacy signal strength api.");
      return new SignalStrengthListenerCupcake(context, callback);
    }
  }

  // @VisibleForTesting
  protected boolean hasModernSignalStrength() {
    return ANDROID_API_LEVEL >= 7;
  }
}
