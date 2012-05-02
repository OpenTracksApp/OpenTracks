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

import com.google.android.apps.mytracks.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 * 
 * @author Jimmy Shih
 */
public class PreferencesUtils {

  private PreferencesUtils() {}

  /**
   * Gets a preference key
   * 
   * @param context the context
   * @param keyId the key id
   */
  public static String getKey(Context context, int keyId) {
    return context.getString(keyId);
  }

  /**
   * Gets a boolean preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue the default value
   */
  public static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue);
  }

  /**
   * Sets a boolean preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param value the value
   */
  public static void setBoolean(Context context, int keyId, boolean value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putBoolean(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Gets a long preference value.
   * 
   * @param context the context
   * @param keyId the key id
   */
  public static long getLong(Context context, int keyId) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getLong(getKey(context, keyId), -1L);
  }

  /**
   * Sets a long preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param value the value
   */
  public static void setLong(Context context, int keyId, long value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putLong(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }
}
