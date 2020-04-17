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

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceManager;

import de.dennisguse.opentracks.R;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 *
 * @author Jimmy Shih
 */
public class PreferencesUtils {

    private PreferencesUtils() {
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void register(Context context, SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener) {
        getSharedPreferences(context).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public static void unregister(Context context, SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener) {
        getSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Deprecated
    //NOTE: is at the moment still used to determine if a track is currently recorded; better ask the service directly.
    //NOTE: This was also used to recover from a reboot, but this data should not be exposed to the whole application.
    public static final long RECORDING_TRACK_ID_DEFAULT = -1L;

    public static long getRecordingTrackId(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getLong(getKey(context, R.string.recording_track_id_key), RECORDING_TRACK_ID_DEFAULT);
    }

    public static String getDefaultActivity(Context context) {
        return getString(context, R.string.default_activity_key, context.getString(R.string.default_activity_default));
    }

    public static void setDefaultActivity(Context context, String newDefaultActivity) {
        setString(context, R.string.default_activity_key, newDefaultActivity);
    }

    /**
     * Gets a preference key
     *
     * @param context the context
     * @param keyId   the key id
     */
    private static String getKey(Context context, int keyId) {
        return context.getString(keyId);
    }

    /**
     * Compares if keyId and key belong to the same shared preference key.
     *
     * @param keyId The resource id of the key
     * @param key   The key of the preference
     * @return true if key == null or key belongs to keyId
     */
    public static boolean isKey(Context context, int keyId, String key) {
        return key == null || key.equals(getKey(context, keyId));
    }

    /**
     * Gets a boolean preference value.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue the default value
     */
    private static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
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
    private static int getInt(Context context, int keyId, int defaultValue) {
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
    @VisibleForTesting
    public static void setInt(Context context, int keyId, int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(context, keyId), value);
        editor.apply();
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

    public static boolean isMetricUnits(Context context) {
        final String STATS_UNIT = context.getString(R.string.stats_units_default);
        return STATS_UNIT.equals(getString(context, R.string.stats_units_key, STATS_UNIT));
    }

    public static boolean isReportSpeed(Context context) {
        final String STATS_RATE_DEFAULT = context.getString(R.string.stats_rate_default);
        return STATS_RATE_DEFAULT.equals(getString(context, R.string.stats_rate_key, STATS_RATE_DEFAULT));
    }

    public static boolean isRecordingTrackPaused(Context context) {
        return getBoolean(context, R.string.recording_track_paused_key, isRecordingTrackPausedDefault(context));
    }

    public static boolean isRecordingTrackPausedDefault(Context context) {
        return context.getResources().getBoolean(R.bool.recording_track_paused_default);
    }

    public static void defaultRecordingTrackPaused(Context context) {
        final boolean RECORDING_TRACK_PAUSED = context.getResources().getBoolean(R.bool.recording_track_paused_default);
        setBoolean(context, R.string.recording_track_paused_key, RECORDING_TRACK_PAUSED);
    }

    private static String getBluetoothSensorAddressNone(Context context) {
        return context.getString(R.string.sensor_type_value_none);
    }

    public static boolean isBluetoothSensorAddressNone(Context context, String currentValue) {
        return getBluetoothSensorAddressNone(context).equals(currentValue);
    }


    public static boolean isBluetoothHeartRateSensorAddressNone(Context context) {
        return isBluetoothSensorAddressNone(context, getBluetoothHeartRateSensorAddress(context));
    }

    public static String getBluetoothHeartRateSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_heart_rate_key, getBluetoothSensorAddressNone(context));
    }


    public static boolean isBluetoothCyclingCadenceSensorAddressNone(Context context) {
        return isBluetoothSensorAddressNone(context, getBluetoothCyclingCadenceSensorAddress(context));
    }

    public static String getBluetoothCyclingCadenceSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_cycling_cadence_key, getBluetoothSensorAddressNone(context));
    }


    public static boolean isBluetoothCyclingSpeedSensorAddressNone(Context context) {
        return isBluetoothSensorAddressNone(context, getBluetoothCyclingSpeedSensorAddress(context));
    }

    public static String getBluetoothCyclingSpeedSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_cycling_speed_key, getBluetoothSensorAddressNone(context));
    }


    public static boolean shouldShowStatsOnLockscreen(Context context) {
        final boolean STATS_SHOW_ON_LOCKSCREEN_DEFAULT = context.getResources().getBoolean(R.bool.stats_show_on_lockscreen_while_recording_default);
        return getBoolean(context, R.string.stats_show_on_lockscreen_while_recording_key, STATS_SHOW_ON_LOCKSCREEN_DEFAULT);
    }

    public static boolean shouldKeepScreenOn(Context context) {
        final boolean DEFAULT = context.getResources().getBoolean(R.bool.stats_keep_screen_on_while_recording_default);
        return getBoolean(context, R.string.stats_keep_screen_on_while_recording_key, DEFAULT);
    }

    public static boolean isShowStatsElevation(Context context) {
        final boolean STATS_SHOW_ELEVATION = context.getResources().getBoolean(R.bool.stats_show_elevation_default);
        return getBoolean(context, R.string.stats_show_grade_elevation_key, STATS_SHOW_ELEVATION);
    }

    public static boolean isStatsShowCoordinate(Context context) {
        final boolean STATS_SHOW_COORDINATE = context.getResources().getBoolean(R.bool.stats_show_coordinate_default);
        return getBoolean(context, R.string.stats_show_coordinate_key, STATS_SHOW_COORDINATE);
    }

    public static int getVoiceFrequency(Context context) {
        final int VOICE_FREQUENCY_DEFAULT = Integer.parseInt(context.getResources().getString(R.string.voice_frequency_default));
        return getInt(context, R.string.voice_frequency_key, VOICE_FREQUENCY_DEFAULT);
    }

    public static int getRecordingDistanceInterval(Context context) {
        return getInt(context, R.string.recording_distance_interval_key, getRecordingDistanceIntervalDefault(context));
    }

    public static int getRecordingDistanceIntervalDefault(Context context) {
        return Integer.parseInt(context.getResources().getString(R.string.recording_distance_interval_default));
    }

    public static int getMaxRecordingDistance(Context context) {
        final int MAX_RECORDING_DISTANCE = Integer.parseInt(context.getResources().getString(R.string.max_recording_distance_default));
        return getInt(context, R.string.max_recording_distance_key, MAX_RECORDING_DISTANCE);
    }

    public static int getMinRecordingInterval(Context context) {
        final int MIN_RECORDING_INTERVAL = Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_default));
        return getInt(context, R.string.min_recording_interval_key, MIN_RECORDING_INTERVAL);
    }

    public static int getMinRecordingIntervalAdaptAccuracy(Context context) {
        return Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_adapt_accuracy));
    }

    public static int getMinRecordingIntervalAdaptBatteryLife(Context context) {
        return Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_adapt_battery_life));
    }

    public static int getMinRecordingIntervalDefault(Context context) {
        return Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_default));
    }

    public static int getRecordingGPSAccuracy(Context context) {
        final int RECORDING_GPS_ACCURACY = Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_default));
        return getInt(context, R.string.recording_gps_accuracy_key, RECORDING_GPS_ACCURACY);
    }

    public static boolean isRecording(Context context) {
        long recordingTrackId = getRecordingTrackId(context);
        return isRecording(recordingTrackId);
    }

    @Deprecated
    //TODO Method is very misleading: it only checks if the provided trackId not the default value (i.e., not recording).
    public static boolean isRecording(long recordingTrackId) {
        return recordingTrackId != RECORDING_TRACK_ID_DEFAULT;
    }

    public static void resetPreferences(Context context, boolean readAgain) {
        if (readAgain) {
            // We want to really clear settings now.
            getSharedPreferences(context).edit().clear().commit();
        }
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);
    }
}
