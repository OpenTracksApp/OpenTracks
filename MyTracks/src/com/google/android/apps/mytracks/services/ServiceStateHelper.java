/*
 * Copyright 2011 Google Inc.
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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

/**
 * Helper for reading service state.
 *
 * @author Rodrigo Damazio
 */
public class ServiceStateHelper {

  public static boolean isRecording(Context ctx, SharedPreferences preferences) {
    TrackRecordingServiceBinder serviceBinder = TrackRecordingServiceBinder.getInstance(ctx);
    ITrackRecordingService service = serviceBinder.getServiceIfBound();
    if (service != null) {
      try {
        return service.isRecording();
      } catch (RemoteException e) {
        Log.e(TAG, "Failed to check if service is recording", e);
      } catch (IllegalStateException e) {
        Log.e(TAG, "Failed to check if service is recording", e);
      }
    }

    if (preferences == null) {
      preferences = ctx.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    }
    return preferences.getLong(ctx.getString(R.string.recording_track_key), -1) > 0;
  }

  private ServiceStateHelper() {}
}
