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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.Constants;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Utility class for acessing basic Android functionality.
 *
 * @author Rodrigo Damazio
 */
public class SystemUtils {

  private static final int RELEASE_SIGNATURE_HASHCODE = -1855564782;

  /**
   * Returns whether or not this is a release build.
   */
  public static boolean isRelease(Context context) {
    try {
      Signature [] sigs = context.getPackageManager().getPackageInfo(
          context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
      for (Signature sig : sigs) {
        if (sig.hashCode() == RELEASE_SIGNATURE_HASHCODE) {
          return true;
        }
      }
    } catch (NameNotFoundException e) {
      Log.e(Constants.TAG, "Unable to get signatures", e);
    }
    return false;
  }

  /**
   * Get the My Tracks version from the manifest.
   *
   * @return the version, or an empty string in case of failure.
   */
  public static String getMyTracksVersion(Context context) {
    try {
      PackageInfo pi = context.getPackageManager().getPackageInfo(
          "com.google.android.maps.mytracks",
          PackageManager.GET_META_DATA);
      return pi.versionName;
    } catch (NameNotFoundException e)  {
      Log.w(Constants.TAG, "Failed to get version info.", e);
      return "";
    }
  }

  /**
   * Tries to acquire a partial wake lock if not already acquired. Logs errors
   * and gives up trying in case the wake lock cannot be acquired.
   */
  public static WakeLock acquireWakeLock(Activity activity, WakeLock wakeLock) {
    Log.i(Constants.TAG, "LocationUtils: Acquiring wake lock.");
    try {
      PowerManager pm = (PowerManager) activity
          .getSystemService(Context.POWER_SERVICE);
      if (pm == null) {
        Log.e(Constants.TAG, "LocationUtils: Power manager not found!");
        return wakeLock;
      }
      if (wakeLock == null) {
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            Constants.TAG);
        if (wakeLock == null) {
          Log.e(Constants.TAG,
              "LocationUtils: Could not create wake lock (null).");
        }
        return wakeLock;
      }
      if (!wakeLock.isHeld()) {
        wakeLock.acquire();
        if (!wakeLock.isHeld()) {
          Log.e(Constants.TAG,
              "LocationUtils: Could not acquire wake lock.");
        }
      }
    } catch (RuntimeException e) {
      Log.e(Constants.TAG,
          "LocationUtils: Caught unexpected exception: " + e.getMessage(), e);
    }
    return wakeLock;
  }

  private SystemUtils() {}
}