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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

/**
 * Utilities for Google location settings
 * 
 * @author Jimmy Shih
 */
public class GoogleLocationUtils {

  private static final String TAG = GoogleLocationUtils.class.getSimpleName();

  // Action to launch the google location settings
  private static final String
      ACTION_GOOGLE_LOCATION_SETTINGS = "com.google.android.gsf.GOOGLE_LOCATION_SETTINGS";

  // Action to check if the google apps locations settings exists
  private static final String
      ACTION_GOOGLE_APPS_LOCATION_SETTINGS = "com.google.android.gsf.GOOGLE_APPS_LOCATION_SETTINGS";

  // User has disagreed to use location for Google services
  private static final int USE_LOCATION_FOR_SERVICES_OFF = 0;

  // User has agreed to use location for Google services
  public static final int USE_LOCATION_FOR_SERVICES_ON = 1;

  /*
   * The user has neither agreed nor disagreed to use location for Google
   * services yet.
   */
  private static final int USE_LOCATION_FOR_SERVICES_NOT_SET = 2;

  private static final String GOOGLE_SETTINGS_AUTHORITY = "com.google.settings";
  private static final Uri GOOGLE_SETTINGS_CONTENT_URI = Uri.parse(
      "content://" + GOOGLE_SETTINGS_AUTHORITY + "/partner");
  private static final String NAME = "name";
  private static final String VALUE = "value";
  private static final String USE_LOCATION_FOR_SERVICES = "use_location_for_services";

  public static final Uri USE_LOCATION_FOR_SERVICES_URI = Uri.parse(
      "content://" + GOOGLE_SETTINGS_AUTHORITY + "/partner/" + USE_LOCATION_FOR_SERVICES);

  private GoogleLocationUtils() {}

  /**
   * Gets the gps disabled message.
   * 
   * @param context the context
   */
  public static String getGpsDisabledMessage(Context context) {
    int id = ApiAdapterFactory.getApiAdapter().hasLocationMode() ? R.string.gps_disabled_location_mode
        : R.string.gps_disabled;
    return context.getString(id, getLocationSettingsName(context));
  }

  /**
   * Gets the gps disabled message when my location button is pressed.
   * 
   * @param context the context
   */
  public static String getGpsDisabledMyLocationMessage(Context context) {
    int id = ApiAdapterFactory.getApiAdapter().hasLocationMode() ? R.string.gps_disabled_my_location_location_mode
        : R.string.gps_disabled_my_location;
    return context.getString(id, getLocationSettingsName(context));
  }

  /**
   * Creates a new location settings intent.
   * 
   * @param context the context
   */
  public static Intent newLocationSettingsIntent(Context context) {
    Intent intent = new Intent(useGoogleLocationSettings(context) ? ACTION_GOOGLE_LOCATION_SETTINGS
        : Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }

  /**
   * Returns true if there is no enforcement or google location settings allows
   * access.
   * 
   * @param context the context
   */
  public static boolean isAllowed(Context context) {
    if (!isEnforceable(context)) {
      return true;
    }
    if (!ApiAdapterFactory.getApiAdapter().hasLocationMode()) {
      // Before KitKat
      return getUseLocationForServices(context) == USE_LOCATION_FOR_SERVICES_ON;
    } else {
      // KitKat+
      return getUseLocationForServices(context) != USE_LOCATION_FOR_SERVICES_OFF;
    }
  }

  /**
   * Gets the location settings name.
   * 
   * @param context the context
   */
  private static String getLocationSettingsName(Context context) {
    return context.getString(
        useGoogleLocationSettings(context) ? R.string.gps_google_location_settings
            : R.string.gps_location_access);
  }

  /**
   * Returns true to use the google location settings.
   * 
   * @param context the context
   */
  private static boolean useGoogleLocationSettings(Context context) {
    if (!isEnforceable(context)) {
      return false;
    }
    if (!ApiAdapterFactory.getApiAdapter().hasLocationMode()) {
      // Before KitKat
      return true;
    } else {
      // KitKat+
      return getUseLocationForServices(context) == USE_LOCATION_FOR_SERVICES_OFF;
    }
  }

  /**
   * Returns true if the Google location settings is enforceable.
   */
  private static boolean isEnforceable(Context context) {
    Intent intent = new Intent(ACTION_GOOGLE_APPS_LOCATION_SETTINGS);
    ResolveInfo resolveInfo = context.getPackageManager()
        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return resolveInfo != null;
  }

  /**
   * Get the current value for the 'Use value for location' setting.
   * 
   * @return One of {@link #USE_LOCATION_FOR_SERVICES_NOT_SET},
   *         {@link #USE_LOCATION_FOR_SERVICES_OFF} or
   *         {@link #USE_LOCATION_FOR_SERVICES_ON}.
   */
  private static int getUseLocationForServices(Context context) {
    ContentResolver contentResolver = context.getContentResolver();
    Cursor cursor = null;
    String stringValue = null;
    try {
      cursor = contentResolver.query(GOOGLE_SETTINGS_CONTENT_URI, new String[] { VALUE },
          NAME + "=?", new String[] { USE_LOCATION_FOR_SERVICES }, null);
      if (cursor != null && cursor.moveToNext()) {
        stringValue = cursor.getString(0);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Failed to get 'Use My Location' setting", e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    if (stringValue == null) {
      return USE_LOCATION_FOR_SERVICES_NOT_SET;
    }
    int value;
    try {
      value = Integer.parseInt(stringValue);
    } catch (NumberFormatException nfe) {
      value = USE_LOCATION_FOR_SERVICES_NOT_SET;
    }
    return value;
  }
}
