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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

/**
 * Helper for reading service state.
 *
 * @author Rodrigo Damazio
 */
public class ServiceUtils {

  /**
   * Checks whether we're currently recording.
   * The checking is done by calling the service, if provided, or alternatively by reading
   * recording state saved to preferences.
   *
   * @param ctx the current context
   * @param service the service, or null if not bound to it
   * @param preferences the preferences, or null if not available
   * @return true if the service is recording (or supposed to be recording), false otherwise
   */
  public static boolean isRecording(Context ctx, ITrackRecordingService service, SharedPreferences preferences) {
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

  /**
   * Checks whether the recording service is currently running.
   *
   * @param ctx the current context
   * @return true if the service is running, false otherwise
   */
  public static boolean isServiceRunning(Context ctx) {
    ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
    List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

    for (RunningServiceInfo serviceInfo : services) {
      ComponentName componentName = serviceInfo.service;
      String serviceName = componentName.getClassName();
      if (serviceName.equals(TrackRecordingService.class.getName())) {
        return true;
      }
    }
    return false;
  }

  private ServiceUtils() {}
}
