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
 */
public class EulaUtils {
  private static final String EULA_PREFERENCE_FILE = "eula";

  // Accepting Google mobile terms of service
  private static final String EULA_PREFERENCE_KEY = "eula.google_mobile_tos_accepted";

  private static final String HOST_NAME = "m.google.com";

  private EulaUtils() {}

  public static boolean getEulaValue(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(
        EULA_PREFERENCE_FILE, Context.MODE_PRIVATE);
    return preferences.getBoolean(EULA_PREFERENCE_KEY, false);
  }

  public static void setEulaValue(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(
        EULA_PREFERENCE_FILE, Context.MODE_PRIVATE);
    Editor editor = preferences.edit();
    editor.putBoolean(EULA_PREFERENCE_KEY, true);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  public static String getEulaMessage(Context context) {
    String item1 = String.format(context.getString(R.string.eula_message_item1), HOST_NAME,
        HOST_NAME, HOST_NAME, HOST_NAME, HOST_NAME);
    String item3 = String.format(context.getString(R.string.eula_message_item3), HOST_NAME);
    String footer = String.format(context.getString(R.string.eula_message_footer), HOST_NAME);
    String copyright = "©" + Calendar.getInstance().get(Calendar.YEAR);
    return context.getString(R.string.eula_message_date)
        + "\n\n"
        + context.getString(R.string.eula_message_header)
        + "\n\n"
        + context.getString(R.string.eula_message_body)
        + "\n\n"
        + "1. " + item1
        + "\n\n"
        + "2. " + context.getString(R.string.eula_message_item2)
        + "\n\n"
        + "3. " + item3
        + "\n\n"
        + "4. " + context.getString(R.string.eula_message_item4)
        + "\n\n"
        + footer
        + "\n\n"
        + copyright;
  }
}
