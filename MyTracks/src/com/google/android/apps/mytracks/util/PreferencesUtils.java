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
import com.google.android.apps.mytracks.fragments.ExportDialogFragment.ExportType;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.maps.mytracks.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 * 
 * @author Jimmy Shih
 */
public class PreferencesUtils {

  public static final int ACTIVITY_RECOGNITION_TYPE_DEFAULT = DetectedActivity.UNKNOWN;
  
  /*
   * Preferences values. The defaults need to match the defaults in the xml
   * files.
   */
  public static final boolean ALLOW_ACCESS_DEFAULT = false;
  public static final int AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT = 0;

  // Values for auto_resume_track_timeout_key
  public static final int AUTO_RESUME_TRACK_TIMEOUT_ALWAYS = -1;
  public static final int AUTO_RESUME_TRACK_TIMEOUT_DEFAULT = 10;
  public static final int AUTO_RESUME_TRACK_TIMEOUT_NEVER = 0;

  public static final String BLUETOOTH_SENSOR_DEFAULT = "";
  
  public static final boolean CHART_SHOW_CADENCE_DEFAULT = true;
  public static final boolean CHART_SHOW_ELEVATION_DEFAULT = true;
  public static final boolean CHART_SHOW_HEART_RATE_DEFAULT = true;
  public static final boolean CHART_SHOW_POWER_DEFAULT = true;
  public static final boolean CHART_SHOW_SPEED_DEFAULT = true;
  public static final String CHART_X_AXIS_DEFAULT = "DISTANCE";

  public static final String DEFAULT_ACTIVITY_DEFAULT = "";
  
  public static final String DRIVE_DELETED_LIST_DEFAULT = "";
  public static final String DRIVE_EDITED_LIST_DEFAULT = "";
  public static final long DRIVE_LARGEST_CHANGE_ID_DEFAULT = -1L;
  public static final boolean DRIVE_SYNC_DEFAULT = false;

  public static final String EXPORT_EXTERNAL_STORAGE_FORMAT_DEFAULT = TrackFileFormat.KML.name();
  public static final boolean EXPORT_GOOGLE_FUSION_TABLES_PUBLIC_DEFAULT = false;
  public static final boolean EXPORT_GOOGLE_MAPS_PUBLIC_DEFAULT = false;
  public static final String EXPORT_TYPE_DEFAULT = ExportType.GOOGLE_DRIVE.name();
  
  // Value for split_frequency_key and voice_frequency_key
  public static final int FREQUENCY_OFF = 0;

  public static final String GOOGLE_ACCOUNT_DEFAULT = "";
  public static final int MAP_TYPE_DEFAUlT = 1;
  public static final int MAX_RECORDING_DISTANCE_DEFAULT = 200;
  
  // Values for min_recording_interval_key
  public static final int MIN_RECORDING_INTERVAL_ADAPT_ACCURACY = -1;
  public static final int MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE = -2;
  public static final int MIN_RECORDING_INTERVAL_DEFAULT = 0;

  public static final int PHOTO_SIZE_DEFAULT = 1024; // 1024 kB
  public static final int RECORDING_DISTANCE_INTERVAL_DEFAULT = 10;
  
  // Values for recording_gps_accuracy
  public static final int RECORDING_GPS_ACCURACY_DEFAULT = 50;
  public static final int RECORDING_GPS_ACCURACY_EXCELLENT = 10;
  public static final int RECORDING_GPS_ACCURACY_POOR = 2000;
  
  public static final long RECORDING_TRACK_ID_DEFAULT = -1L;
  public static final boolean RECORDING_TRACK_PAUSED_DEFAULT = true;
  public static final long SELECTED_TRACK_ID_DEFAULT = -1L;
  public static final String SENSOR_TYPE_DEFAULT = "NONE";

  // Share track
  public static final String SHARE_TRACK_ACCOUNT_DEFAULT = "";
  public static final boolean SHARE_TRACK_INVITE_DEFAULT = false;
  public static final boolean SHARE_TRACK_PUBLIC_DEFAULT = false;

  public static final int SPLIT_FREQUENCY_DEFAULT = 0;
  
  // Stats
  public static final String STATS_RATE_DEFAULT = "SPEED";
  public static final boolean STATS_SHOW_COORDINATE_DEFAULT = false;
  public static final boolean STATS_SHOW_GRADE_ELEVATION_DEFAULT = false;
  public static final String STATS_UNITS_DEFAULT = "METRIC";
  
  // Track color
  public static final String TRACK_COLOR_MODE_DEFAULT = "SINGLE";
  public static final int TRACK_COLOR_MODE_MEDIUM_DEFAULT = 15;
  public static final int TRACK_COLOR_MODE_PERCENTAGE_DEFAULT = 25;
  public static final int TRACK_COLOR_MODE_SLOW_DEFAULT = 9;
  
  public static final String TRACK_NAME_DEFAULT = "LOCATION";
  
  // Track widget
  public static final int TRACK_WIDGET_ITEM1_DEFAULT = 3; // moving time
  public static final int TRACK_WIDGET_ITEM2_DEFAULT = 0; // distance
  public static final int TRACK_WIDGET_ITEM3_DEFAULT = 1; // total time
  public static final int TRACK_WIDGET_ITEM4_DEFAULT = 2; // average speed
  public static final int VOICE_FREQUENCY_DEFAULT = 0;
  
  private static final String TAG = PreferencesUtils.class.getSimpleName();
  
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
  @SuppressLint("CommitPrefEdits")
  public static void setBoolean(Context context, int keyId, boolean value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putBoolean(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Gets an integer preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue the default value
   */
  public static int getInt(Context context, int keyId, int defaultValue) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getInt(getKey(context, keyId), defaultValue);
  }

  /**
   * Sets an integer preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param value the value
   */
  @SuppressLint("CommitPrefEdits")
  public static void setInt(Context context, int keyId, int value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putInt(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Gets a float preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue the default value
   */
  public static float getFloat(Context context, int keyId, float defaultValue) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getFloat(getKey(context, keyId), defaultValue);
  }

  /**
   * Sets a float preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param value the value
   */
  @SuppressLint("CommitPrefEdits")
  public static void setFloat(Context context, int keyId, float value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putFloat(getKey(context, keyId), value);
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
  @SuppressLint("CommitPrefEdits")
  public static void setLong(Context context, int keyId, long value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putLong(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Gets a string preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue default value
   */
  public static String getString(Context context, int keyId, String defaultValue) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getString(getKey(context, keyId), defaultValue);
  }

  /**
   * Sets a string preference value.
   * 
   * @param context the context
   * @param keyId the key id
   * @param value the value
   */
  @SuppressLint("CommitPrefEdits")
  public static void setString(Context context, int keyId, String value) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = sharedPreferences.edit();
    editor.putString(getKey(context, keyId), value);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }
  
  /**
   * Returns true if metric units.
   * 
   * @param context the context
   */
  public static boolean isMetricUnits(Context context) {
    return STATS_UNITS_DEFAULT.equals(
        getString(context, R.string.stats_units_key, STATS_UNITS_DEFAULT));
  }

  /**
   * Returns true if the preferred rate is speed, false if the preferred rate is
   * pace.
   * 
   * @param context the context
   */
  public static boolean isReportSpeed(Context context) {
    return STATS_RATE_DEFAULT.equals(
        getString(context, R.string.stats_rate_key, STATS_RATE_DEFAULT));
  }

  /**
   * Returns true if chart x axis is by distance, false if by time.
   * 
   * @param context the context
   */
  public static boolean isChartByDistance(Context context) {
    return CHART_X_AXIS_DEFAULT.equals(
        getString(context, R.string.chart_x_axis_key, CHART_X_AXIS_DEFAULT));
  }
  
  /**
   * Adds a value to a list.
   * 
   * @param context the context
   * @param keyId the key id
   * @param defaultValue the default value
   * @param value the value
   */
  public static void addToList(Context context, int keyId, String defaultValue, String value) {
    String list = getString(context, keyId, defaultValue);
    if (defaultValue.equals(list)) {
      setString(context, keyId, value);
      return;
    }
    String[] items = TextUtils.split(list, ";");
    for (String item : items) {
      if (value.equals(item)) {
        return;
      }
    }
    setString(context, keyId, list + ";" + value);
  }
  
  /**
   * Gets the default weight.
   * 
   * @param context the context
   */
  public static float getDefaultWeight(Context context) {
    if (isMetricUnits(context)) {
      return 65.0f; // in kg
    } else {
      return 68.0389f; // 150 lb in kg
    }
  }
  /**
   * Stores the weight value, always in metric units.
   * 
   * @param displayValue the display value
   */
  public static void storeWeightValue(Context context, String displayValue) {
    double value;
    try {
      value = Double.parseDouble(displayValue);
      if (!isMetricUnits(context)) {
        value = value * UnitConversions.LB_TO_KG;
      }
    } catch (NumberFormatException e) {
      Log.e(TAG, "invalid value " + displayValue);
      value = getDefaultWeight(context);
    }
    setFloat(context, R.string.weight_key, (float) value);
  }
  
  /**
   * Gets the weight display value.
   * 
   * @param context the context
   */
  public static double getWeightDisplayValue(Context context) {
    double value = getFloat(context, R.string.weight_key, getDefaultWeight(context));
    if (!isMetricUnits(context)) {
      value = value * UnitConversions.KG_TO_LB;
    }
    return value;
  }  
}
