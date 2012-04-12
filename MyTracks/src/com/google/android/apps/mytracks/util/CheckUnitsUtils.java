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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Utilities for checking units.
 *
 * @author Jimmy Shih
 */
public class CheckUnitsUtils {

  private static final String CHECK_UNITS_PREFERENCE_FILE = "checkunits";
  private static final String CHECK_UNITS_PREFERENCE_KEY = "checkunits.checked";
  
  private CheckUnitsUtils() {}

  public static boolean getCheckUnitsValue(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        CHECK_UNITS_PREFERENCE_FILE, Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(CHECK_UNITS_PREFERENCE_KEY, false);
  }

  public static void setCheckUnitsValue(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        CHECK_UNITS_PREFERENCE_FILE, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit().putBoolean(CHECK_UNITS_PREFERENCE_KEY, true);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }
}
