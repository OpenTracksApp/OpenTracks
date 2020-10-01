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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 *
 * @author Jimmy Shih
 */
public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();

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
    //NOTE: This was also used to recover from a service restart, but this data should not be exposed to the whole application.
    public static final long RECORDING_TRACK_ID_DEFAULT = -1L;

    @Deprecated //Use the TrackRecordingService
    public static Track.Id getRecordingTrackId(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return new Track.Id(sharedPreferences.getLong(getKey(context, R.string.recording_track_id_key), RECORDING_TRACK_ID_DEFAULT));
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
    //TODO Don't use; this function is only to be used TrackRecordingService and will be removed.
    @VisibleForTesting
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
    //TODO Don't use; this function is only to be used TrackRecordingService and will be removed.
    @Deprecated
    @VisibleForTesting
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
    @VisibleForTesting
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

    public static boolean isReportSpeed(Context context, String category) {
        final String STATS_RATE_DEFAULT = context.getString(R.string.stats_rate_default);
        String currentStatsRate = getString(context, R.string.stats_rate_key, STATS_RATE_DEFAULT);
        if (currentStatsRate.equals(getString(context, R.string.stats_rate_speed_or_pace_default, STATS_RATE_DEFAULT))) {
            return TrackIconUtils.isSpeedIcon(context, category);
        }

        return currentStatsRate.equals(context.getString(R.string.stats_rate_speed));
    }

    @Deprecated //Use TrackRecordingService
    public static boolean isRecordingTrackPaused(Context context) {
        return getBoolean(context, R.string.recording_track_paused_key, isRecordingTrackPausedDefault(context));
    }

    @Deprecated //Use TrackRecordingService
    public static boolean isRecordingTrackPausedDefault(Context context) {
        return context.getResources().getBoolean(R.bool.recording_track_paused_default);
    }

    @Deprecated //Use TrackRecordingService
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

    public static String getBluetoothHeartRateSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_heart_rate_key, getBluetoothSensorAddressNone(context));
    }

    public static String getBluetoothCyclingCadenceSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_cycling_cadence_key, getBluetoothSensorAddressNone(context));
    }

    public static String getBluetoothCyclingSpeedSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_cycling_speed_key, getBluetoothSensorAddressNone(context));
    }

    public static int getWheelCircumference(Context context) {
        final int DEFAULT = Integer.parseInt(context.getResources().getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_default));
        return getInt(context, R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key, DEFAULT);
    }

    public static boolean isBluetoothCyclingPowerSensorAddressNone(Context context) {
        return isBluetoothSensorAddressNone(context, getBluetoothCyclingPowerSensorAddress(context));
    }

    public static String getBluetoothCyclingPowerSensorAddress(Context context) {
        return getString(context, R.string.settings_sensor_bluetooth_cycling_power_key, getBluetoothSensorAddressNone(context));
    }


    public static boolean shouldShowStatsOnLockscreen(Context context) {
        final boolean STATS_SHOW_ON_LOCKSCREEN_DEFAULT = context.getResources().getBoolean(R.bool.stats_show_on_lockscreen_while_recording_default);
        return getBoolean(context, R.string.stats_show_on_lockscreen_while_recording_key, STATS_SHOW_ON_LOCKSCREEN_DEFAULT);
    }

    public static boolean shouldKeepScreenOn(Context context) {
        final boolean DEFAULT = context.getResources().getBoolean(R.bool.stats_keep_screen_on_while_recording_default);
        return getBoolean(context, R.string.stats_keep_screen_on_while_recording_key, DEFAULT);
    }

    public static boolean shouldUseFullscreen(Context context) {
        final boolean DEFAULT = context.getResources().getBoolean(R.bool.stats_fullscreen_while_recording_default);
        return getBoolean(context, R.string.stats_fullscreen_while_recording_key, DEFAULT);
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

    public static boolean shouldInstantExportAfterWorkout(Context context) {
        final boolean INSTANT_POST_WORKOUT_EXPORT_DEFAULT = context.getResources().getBoolean(R.bool.post_workout_export_enabled_default);
        return getBoolean(context, R.string.post_workout_export_enabled_key, INSTANT_POST_WORKOUT_EXPORT_DEFAULT);
    }

    public static TrackFileFormat getExportTrackFileFormat(Context context) {
        final String TRACKFILEFORMAT_NAME_DEFAULT = getString(context, R.string.export_trackfileformat_default, null);
        String trackFileFormatName = getString(context, R.string.export_trackfileformat_key, TRACKFILEFORMAT_NAME_DEFAULT);
        try {
            return TrackFileFormat.valueOf(trackFileFormatName);
        } catch (Exception e) {
            return TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA;
        }
    }

    public static boolean getPreventReimportTracks(Context context) {
        final boolean defaultValue = getBoolean(context, R.bool.import_prevent_reimport_default, false);
        return getBoolean(context, R.string.import_prevent_reimport_key, defaultValue);
    }

    /**
     * @return {@link androidx.appcompat.app.AppCompatDelegate}.MODE_*
     */
    public static int getDefaultNightMode(Context context) {
        final String defaultValue = getKey(context, R.string.night_mode_default);
        final String value = getString(context, R.string.night_mode_key, defaultValue);

        return Integer.parseInt(value);
    }

    @Deprecated //Use TrackRecordingService
    public static boolean isRecording(Context context) {
        return isRecording(getRecordingTrackId(context));
    }

    @Deprecated
    //TODO Method is very misleading: it only checks if the provided trackId not the default value (i.e., not recording).
    public static boolean isRecording(Track.Id recordingTrackId) {
        if (recordingTrackId == null) {
            return false;
        }
        return recordingTrackId.getId() != RECORDING_TRACK_ID_DEFAULT;
    }

    public static void resetPreferences(Context context, boolean readAgain) {
        if (readAgain) {
            // We want to really clear settings now.
            getSharedPreferences(context).edit().clear().commit();
        }
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);
    }

    public static DocumentFile getDefaultExportDirectoryUri(Context context) {
        String singleExportDirectorySettingsKey = getString(context, R.string.settings_default_export_directory_key, null);
        if (singleExportDirectorySettingsKey == null) {
            return null;
        }
        try {
            return DocumentFile.fromTreeUri(context, Uri.parse(singleExportDirectorySettingsKey));
        } catch (Exception e) {
            Log.w(TAG, "Could not decode default export directory: " + e.getMessage());
        }
        return null;
    }

    public static void setDefaultExportDirectoryUri(Context context, Uri directoryUri) {
        String value = directoryUri != null ? directoryUri.toString() : null;
        setString(context, R.string.settings_default_export_directory_key, value);
    }

    public static boolean isDefaultExportDirectoryUri(Context context) {
        return getDefaultExportDirectoryUri(context) != null;
    }
}
