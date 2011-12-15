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

/**
 * Utilities for EULA.
 */
public class EulaUtil {
  private static final String EULA_PREFERENCE_FILE = "eula";

  // Accepting Google mobile terms of service
  private static final String EULA_PREFERENCE_KEY = "eula.google_mobile_tos_accepted";

  private static final String COPYRIGHT = "©2011";
  private static final String LEGAL_NOTICES_URL = "m.google.com/legalnotices";
  private static final String PRIVACY_URL = "m.google.com/privacy";
  private static final String YOUTUBE_TOS_URL = "m.google.com/tos_youtube";
  private static final String MAPS_TOS_URL = "m.google.com/tos_maps";
  private static final String BUZZ_TOS_URL = "m.google.com/tos_buzz";
  private static final String BOOKS_TOS_URL = "m.google.com/tos_books";
  private static final String GOOGLE_TOS_URL = "m.google.com/utos";

  private EulaUtil() {}

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
    ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
  }

  public static String getEulaMessage(Context context) {
    String item1 = String.format(context.getString(R.string.eula_message_item1), YOUTUBE_TOS_URL,
        MAPS_TOS_URL, BUZZ_TOS_URL, BOOKS_TOS_URL, GOOGLE_TOS_URL);
    String item3 = String.format(context.getString(R.string.eula_message_item3), PRIVACY_URL);
    String footer = String.format(
        context.getString(R.string.eula_message_footer), LEGAL_NOTICES_URL);

    return context.getString(R.string.eula_message_date)
        + "\n\n"
        + context.getString(R.string.eula_message_header)
        + "\n\n"
        + context.getString(R.string.eula_message_body)
        + "\n\n"
        + item1
        + "\n\n"
        + context.getString(R.string.eula_message_item2)
        + "\n\n"
        + item3
        + "\n\n"
        + context.getString(R.string.eula_message_item4)
        + "\n\n"
        + footer
        + "\n\n"
        + COPYRIGHT;
  }
}
