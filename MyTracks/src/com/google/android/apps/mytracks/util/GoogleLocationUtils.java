/*
 * Copyright 2012 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

/**
 * Utilities for Google location settings
 * 
 * @author Jimmy Shih
 */
public class GoogleLocationUtils {

  private static final String
      ACTION_GOOGLE_LOCATION_SETTINGS = "com.google.android.gsf.GOOGLE_LOCATION_SETTINGS";
  private static final String
      ACTION_GOOGLE_APPS_LOCATION_SETTINGS = "com.google.android.gsf.GOOGLE_APPS_LOCATION_SETTINGS";

  private GoogleLocationUtils() {}

  /**
   * Returns true if the Google location settings is available.
   */
  public static boolean isAvailable(Context context) {
    Intent intent = new Intent(ACTION_GOOGLE_APPS_LOCATION_SETTINGS);
    ResolveInfo resolveInfo = context.getPackageManager()
        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return resolveInfo != null;
  }

  /**
   * Gets the location settings name.
   * 
   * @param context the context
   */
  public static String getLocationSettingsName(Context context) {
    return context.getString(isAvailable(context) ? R.string.gps_google_location_settings
        : R.string.gps_location_access);
  }

  /**
   * Creates a new location settings intent.
   * 
   * @param context the context
   */
  public static Intent newLocationSettingsIntent(Context context) {
    Intent intent = isAvailable(context) ? new Intent(
        GoogleLocationUtils.ACTION_GOOGLE_LOCATION_SETTINGS)
        : new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }
}
