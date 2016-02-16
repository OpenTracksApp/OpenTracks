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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.sync.SyncUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * Utilities for Google Earth.
 * 
 * @author Jimmy Shih
 */
public class GoogleEarthUtils {

  private static final String TAG = GoogleEarthUtils.class.getSimpleName();

  private static final String GOOGLE_EARTH_CLASS = "com.google.earth.EarthActivity";
  private static final int GOOGLE_EARTH_MIN_VERSION_CODE = 13246120;
  private static final String GOOGLE_EARTH_PACKAGE = "com.google.earth";
  private static final String
      GOOGLE_EARTH_TOUR_FEATURE_ID = "com.google.earth.EXTRA.tour_feature_id";

  public static final String GOOGLE_EARTH_MARKET_URL = "market://details?id="
      + GOOGLE_EARTH_PACKAGE;

  public static final String TOUR_FEATURE_ID_VALUE = "tour";

  private GoogleEarthUtils() {}

  /**
   * Returns true if Google Earth is installed.
   * 
   * @param context the context
   */
  public static boolean isEarthInstalled(Context context) {
    List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(
        new Intent().setType(SyncUtils.KMZ_MIME_TYPE), PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo info : infos) {
      if (info.activityInfo != null && info.activityInfo.packageName != null
          && info.activityInfo.packageName.equals(GOOGLE_EARTH_PACKAGE)) {
        try {
          PackageInfo packageInfo = context.getPackageManager()
              .getPackageInfo(info.activityInfo.packageName, 0);
          return packageInfo.versionCode >= GOOGLE_EARTH_MIN_VERSION_CODE;
        } catch (NameNotFoundException e) {
          Log.e(TAG, "Unable to get google earth package info", e);
          return false;
        }
      }
    }
    return false;
  }

  /**
   * Gets an intent to play a kml file in Google Earth.
   * 
   * @param kmlFilePath the kml file path
   */
  public static Intent getPlayInEarthIntent(Context context, String kmlFilePath) {    
    Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
        .authority(MyTracksProviderUtils.AUTHORITY).path(kmlFilePath).build();
    context.grantUriPermission(GOOGLE_EARTH_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    return new Intent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .putExtra(GOOGLE_EARTH_TOUR_FEATURE_ID, TOUR_FEATURE_ID_VALUE)
        .setClassName(GOOGLE_EARTH_PACKAGE, GOOGLE_EARTH_CLASS)
        .setDataAndType(uri, SyncUtils.KMZ_MIME_TYPE);
  }
}
