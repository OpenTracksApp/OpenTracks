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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Utilities for Google location settings
 * 
 * @author Jimmy Shih
 */
public class GoogleLocationUtils {

  public static final String
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
}
