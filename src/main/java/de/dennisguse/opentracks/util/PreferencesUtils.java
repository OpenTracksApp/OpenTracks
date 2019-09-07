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

package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import de.dennisguse.opentracks.R;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 *
 * @author Jimmy Shih
 */
public class PreferencesUtils {

    /*
     * Preferences values.
     * The defaults need to match the defaults in the xml files.
     */
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
    @Deprecated
    //NOTE: is at the moment still used to determine if a track is currently recorded; better ask the service directly.
    //NOTE: This is also used to recover from a reboot, but this data should not be exposed to the whole application.
    public static final long RECORDING_TRACK_ID_DEFAULT = -1L;

    public static final String DEFAULT_ACTIVITY_DEFAULT = "";

    // Value for split_frequency_key and voice_frequency_key
    public static final int FREQUENCY_OFF = 0;

    public static final int MAX_RECORDING_DISTANCE_DEFAULT = 200;

    // Values for min_recording_interval_key
    public static final int MIN_RECORDING_INTERVAL_ADAPT_ACCURACY = -1;
    public static final int MIN_RECORDING_INTERVAL_ADAPT_BATTERY_LIFE = -2;
    public static final int MIN_RECORDING_INTERVAL_DEFAULT = 0;

    public static final int RECORDING_DISTANCE_INTERVAL_DEFAULT = 10;

    // Values for recording_gps_accuracy
    public static final int RECORDING_GPS_ACCURACY_DEFAULT = 50;
    public static final int RECORDING_GPS_ACCURACY_EXCELLENT = 10;
    public static final int RECORDING_GPS_ACCURACY_POOR = 2000;
    static final boolean STATS_SHOW_COORDINATE_DEFAULT = false;
    public static final boolean RECORDING_TRACK_PAUSED_DEFAULT = true;

    public static final int SPLIT_FREQUENCY_DEFAULT = 0;
    static final boolean STATS_SHOW_ELEVATION_DEFAULT = false;
    static final String TRACK_NAME_DEFAULT = "DATE_ISO_8601";
    private static final String CHART_X_AXIS_DEFAULT = "DISTANCE";
    public static final String STATS_UNITS_DEFAULT = "METRIC";
    // Stats
    private static final String STATS_RATE_DEFAULT = "SPEED";

    public static final boolean SHOW_TRACKDETAIL_WHILE_RECORDING_ON_LOCKSCREEN = false;

    // Track widget
    public static final int TRACK_WIDGET_ITEM1_DEFAULT = 3; // moving time
    public static final int TRACK_WIDGET_ITEM2_DEFAULT = 0; // distance
    public static final int TRACK_WIDGET_ITEM3_DEFAULT = 1; // total time
    public static final int TRACK_WIDGET_ITEM4_DEFAULT = 2; // average speed
    public static final int VOICE_FREQUENCY_DEFAULT = 0;

    private PreferencesUtils() {
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Gets a preference key
     *
     * @param context the context
     * @param keyId   the key id
     */
    public static String getKey(Context context, int keyId) {
        return context.getString(keyId);
    }

    /**
     * Gets a boolean preference value.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue the default value
     */
    public static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue);
    }

    /**
     * Sets a boolean preference value.
     *
     * @param context the context
     * @param keyId   the key id
     * @param value   the value
     */
    public static void setBoolean(Context context, int keyId, boolean value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(getKey(context, keyId), value);
        editor.apply();
    }

    /**
     * Gets an integer preference value.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue the default value
     */
    public static int getInt(Context context, int keyId, int defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        try {
            return sharedPreferences.getInt(getKey(context, keyId), defaultValue);
        } catch (ClassCastException e) {
            //Ignore
        }

        //NOTE: We assume that the data was stored as String due to use of ListPreference.
        try {
            String stringValue = sharedPreferences.getString(getKey(context, keyId), null);
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets an integer preference value.
     *
     * @param context the context
     * @param keyId   the key id
     * @param value   the value
     */
    public static void setInt(Context context, int keyId, int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(context, keyId), value);
        editor.apply();
    }

    /**
     * Gets a long preference value.
     *
     * @param context the context
     * @param keyId   the key id
     */
    public static long getLong(Context context, int keyId) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getLong(getKey(context, keyId), -1L);
    }

    /**
     * Sets a long preference value.
     *
     * @param context the context
     * @param keyId   the key id
     * @param value   the value
     */
    public static void setLong(Context context, int keyId, long value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putLong(getKey(context, keyId), value);
        editor.apply();
    }

    /**
     * Gets a string preference value.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue default value
     */
    public static String getString(Context context, int keyId, String defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(getKey(context, keyId), defaultValue);
    }

    /**
     * Sets a string preference value.
     *
     * @param context the context
     * @param keyId   the key id
     * @param value   the value
     */
    public static void setString(Context context, int keyId, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putString(getKey(context, keyId), value);
        editor.apply();
    }

    /**
     * Returns true if metric units.
     *
     * @param context the context
     */
    public static boolean isMetricUnits(Context context) {
        return STATS_UNITS_DEFAULT.equals(getString(context, R.string.stats_units_key, STATS_UNITS_DEFAULT));
    }

    /**
     * Returns true if the preferred rate is speed, false if the preferred rate is
     * pace.
     *
     * @param context the context
     */
    public static boolean isReportSpeed(Context context) {
        return STATS_RATE_DEFAULT.equals(getString(context, R.string.stats_rate_key, STATS_RATE_DEFAULT));
    }

    /**
     * Returns true if chart x axis is by distance, false if by time.
     *
     * @param context the context
     */
    public static boolean isChartByDistance(Context context) {
        return CHART_X_AXIS_DEFAULT.equals(getString(context, R.string.chart_x_axis_key, CHART_X_AXIS_DEFAULT));
    }

    public static void resetPreferences(Context context, boolean readAgain) {
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);
        PreferenceManager.setDefaultValues(context, R.xml.settings_recording, readAgain);
        PreferenceManager.setDefaultValues(context, R.xml.settings_statistics, readAgain);
    }
}
