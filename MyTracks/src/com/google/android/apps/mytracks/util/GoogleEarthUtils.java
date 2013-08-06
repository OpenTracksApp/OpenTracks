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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.util.List;

/**
 * Utilities for Google Earth.
 * 
 * @author Jimmy Shih
 */
public class GoogleEarthUtils {

  private static final String GOOGLE_EARTH_CLASS = "com.google.earth.EarthActivity";
  private static final String GOOGLE_EARTH_KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
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
        new Intent().setType(GOOGLE_EARTH_KML_MIME_TYPE), PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo info : infos) {
      if (info.activityInfo != null && info.activityInfo.packageName != null
          && info.activityInfo.packageName.equals(GOOGLE_EARTH_PACKAGE)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets an intent to play a kml file in Google Earth.
   * 
   * @param kmlFilePath the kml file path
   */
  public static Intent getPlayInEarthIntent(String kmlFilePath) {
    return new Intent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(GOOGLE_EARTH_TOUR_FEATURE_ID, TOUR_FEATURE_ID_VALUE)
        .setClassName(GOOGLE_EARTH_PACKAGE, GOOGLE_EARTH_CLASS)
        .setDataAndType(Uri.fromFile(new File(kmlFilePath)), GOOGLE_EARTH_KML_MIME_TYPE);
  }
}
