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
import com.google.android.maps.mytracks.R;

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
   * Gets the metric units key.
   *
   * @param context the context
   */
  public static String getMetricUnitsKey(Context context) {
    return getKey(context, R.string.metric_units_key);
  }

  /**
   * Returns the metric units value.
   *
   * @param context the context
   */
  public static boolean isMetricUnits(Context context) {
    return getBoolean(context, R.string.metric_units_key, true);
  }

  /**
   * Sets the metric units value.
   *
   * @param context the context
   * @param value the value
   */
  public static void setMetricUnits(Context context, boolean value) {
    setBoolean(context, R.string.metric_units_key, value);
  }

  /**
   * Gets the recording track id key.
   * 
   * @param context the context
   */
  public static String getRecordingTrackIdKey(Context context) {
    return getKey(context, R.string.recording_track_id_key);
  }

  /**
   * Gets the recording track id.
   * 
   * @param context the context
   */
  public static long getRecordingTrackId(Context context) {
    return getLong(context, R.string.recording_track_id_key);
  }

  /**
   * Sets the recording track id.
   * 
   * @param context the context
   * @param trackId the track id
   */
  public static void setRecordingTrackId(Context context, long trackId) {
    setLong(context, R.string.recording_track_id_key, trackId);
  }

  /**
   * Gets the report speed key.
   *
   * @param context the context
   */
  public static String getReportSpeedKey(Context context) {
    return getKey(context, R.string.report_speed_key);
  }

  /**
   * Returns the report speed value.
   *
   * @param context the context
   */
  public static boolean isReportSpeed(Context context) {
    return getBoolean(context, R.string.report_speed_key, true);
  }

  /**
   * Sets the report speed value.
   *
   * @param context the context
   * @param value the value
   */
  public static void setReportSpeed(Context context, boolean value) {
    setBoolean(context, R.string.report_speed_key, value);
  }
  
  /**
   * Gets the selected track id key.
   * 
   * @param context the context
   */
  public static String getSelectedTrackIdKey(Context context) {
    return getKey(context, R.string.selected_track_id_key);
  }

  /**
   * Gets the selected track id.
   * 
   * @param context the context
   */
  public static long getSelectedTrackId(Context context) {
    return getLong(context, R.string.selected_track_id_key);
  }

  /**
   * Sets the selected track id.
   * 
   * @param context the context
   * @param trackId the track id
   */
  public static void setSelectedTrackId(Context context, long trackId) {
    setLong(context, R.string.selected_track_id_key, trackId);
  }

  /**
   * Returns the show check units dialog value.
   *
   * @param context the context
   */
  public static boolean isShowCheckUnitsDialog(Context context) {
    return getBoolean(context, R.string.show_check_units_dialog_key, true);
  }

  /**
   * Sets the show check units dialog value to false.
   *
   * @param context the context
   */
  public static void setShowCheckUnitsDialog(Context context) {
    setBoolean(context, R.string.show_check_units_dialog_key, false);
  }

  /**
   * Returns the show welcomes dialog value.
   *
   * @param context the context
   */
  public static boolean isShowWelcomeDialog(Context context) {
    return getBoolean(context, R.string.show_welcome_dialog_key, true);
  }

  /**
   * Sets the show welcome dialog value to false.
   *
   * @param context the context
   */
  public static void setShowWelcome(Context context) {
    setBoolean(context, R.string.show_welcome_dialog_key, false);
  }
  
  /**
   * Gets a preference key
   * 
   * @param context the context
   * @param keyId the key id
   */
  private static String getKey(Context context, int keyId) {
    return context.getString(keyId);
  }

  /**
   * Gets a boolean preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue the default value
   */
  private static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
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
  private static void setBoolean(Context context, int keyId, boolean value) {
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
  private static long getLong(Context context, int keyId) {
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
  private static void setLong(Context context, int keyId, long value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putLong(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }
}
