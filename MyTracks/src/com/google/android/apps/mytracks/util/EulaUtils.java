/*
 * Copyright 2008 Google Inc.
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Calendar;

/**
 * Utilities for EULA.
 *
 * @author Jimmy Shih
 */
public class EulaUtils {

  private static final String EULA_PREFERENCE_FILE = "eula";

  // Accepting Google mobile terms of service
  private static final String EULA_PREFERENCE_KEY = "eula.google_mobile_tos_accepted";

  private static final String HOST_NAME = "m.google.com";

  private EulaUtils() {}

  public static boolean getEulaValue(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        EULA_PREFERENCE_FILE, Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(EULA_PREFERENCE_KEY, false);
  }

  public static void setEulaValue(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        EULA_PREFERENCE_FILE, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putBoolean(EULA_PREFERENCE_KEY, true);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  public static String getEulaMessage(Context context) {
    return context.getString(R.string.eula_date) 
        + "\n\n"
        + context.getString(R.string.eula_body, HOST_NAME, HOST_NAME, HOST_NAME, HOST_NAME) 
        + "\n\n" 
        + context.getString(R.string.eula_footer, HOST_NAME) 
        + "\n\n" 
        + "©" + Calendar.getInstance().get(Calendar.YEAR);
  }
}
