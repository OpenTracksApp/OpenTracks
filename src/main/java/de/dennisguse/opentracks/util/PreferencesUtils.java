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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.time.Duration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.DataField;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Layout;
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

    @Deprecated //Should only be used to get a sharedPreference for more than one interaction!
    public static SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        PreferencesOpenHelper.newInstance(context, sharedPreferences).checkForUpgrade();
        return sharedPreferences;
    }

    public static String getDefaultActivity(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.default_activity_key, context.getString(R.string.default_activity_default));
    }

    public static void setDefaultActivity(SharedPreferences sharedPreferences, Context context, String newDefaultActivity) {
        setString(sharedPreferences, context, R.string.default_activity_key, newDefaultActivity);
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

    public static boolean isKey(Context context, int[] keyIds, String key) {
        for(int keyId : keyIds) {
            if (isKey(context, keyId, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean getBoolean(SharedPreferences sharedPreferences, Context context, int keyId, boolean defaultValue) {
        return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue);
    }

    static int getInt(SharedPreferences sharedPreferences, Context context, int keyId, int defaultValue) {
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

    private static float getFloat(SharedPreferences sharedPreferences, Context context, int keyId, float defaultValue) {
        try {
            return sharedPreferences.getFloat(getKey(context, keyId), defaultValue);
        } catch (ClassCastException e) {
            //Ignore
        }

        //NOTE: We assume that the data was stored as String due to use of ListPreference.
        try {
            String stringValue = sharedPreferences.getString(getKey(context, keyId), null);
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(SharedPreferences sharedPreferences, Context context, int keyId, String defaultValue) {
        return sharedPreferences.getString(getKey(context, keyId), defaultValue);
    }

    @VisibleForTesting
    public static void setString(SharedPreferences sharedPreferences, Context context, int keyId, String value) {
        Editor editor = sharedPreferences.edit();
        editor.putString(getKey(context, keyId), value);
        editor.apply();
    }

    static void setInt(SharedPreferences sharedPreferences, Context context, int keyId, int value) {
        Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(context, keyId), value);
        editor.apply();
    }

    public static boolean isMetricUnits(SharedPreferences sharedPreferences, Context context) {
        final String STATS_UNIT = context.getString(R.string.stats_units_default);
        return STATS_UNIT.equals(getString(sharedPreferences, context, R.string.stats_units_key, STATS_UNIT));
    }

    public static void setMetricUnits(SharedPreferences sharedPreferences, Context context, boolean metricUnits) {
        String unit;
        if (metricUnits) {
            unit = context.getString(R.string.stats_units_metric);
        } else {
            unit = context.getString(R.string.stats_units_imperial);
        }
        setString(sharedPreferences, context, R.string.stats_units_key, unit);
    }

    public static boolean isReportSpeed(SharedPreferences sharedPreferences, Context context, String category) {
        final String STATS_RATE_DEFAULT = context.getString(R.string.stats_rate_default);
        String currentStatsRate = getString(sharedPreferences, context, R.string.stats_rate_key, STATS_RATE_DEFAULT);
        if (currentStatsRate.equals(getString(sharedPreferences, context, R.string.stats_rate_speed_or_pace_default, STATS_RATE_DEFAULT))) {
            return TrackIconUtils.isSpeedIcon(context, category);
        }

        return currentStatsRate.equals(context.getString(R.string.stats_rate_speed));
    }

    private static String getBluetoothSensorAddressNone(Context context) {
        return context.getString(R.string.sensor_type_value_none);
    }

    public static boolean isBluetoothSensorAddressNone(Context context, String currentValue) {
        return getBluetoothSensorAddressNone(context).equals(currentValue);
    }

    public static String getBluetoothHeartRateSensorAddress(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.settings_sensor_bluetooth_heart_rate_key, getBluetoothSensorAddressNone(context));
    }

    public static String getBluetoothCyclingCadenceSensorAddress(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.settings_sensor_bluetooth_cycling_cadence_key, getBluetoothSensorAddressNone(context));
    }

    public static String getBluetoothCyclingSpeedSensorAddress(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.settings_sensor_bluetooth_cycling_speed_key, getBluetoothSensorAddressNone(context));
    }

    public static Distance getWheelCircumference(SharedPreferences sharedPreferences, Context context) {
        final int DEFAULT = Integer.parseInt(context.getResources().getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_default));
        return Distance.ofMM(getInt(sharedPreferences, context, R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key, DEFAULT));
    }

    public static String getBluetoothCyclingPowerSensorAddress(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.settings_sensor_bluetooth_cycling_power_key, getBluetoothSensorAddressNone(context));
    }

    public static String getBluetoothRunningSpeedAndCadenceAddress(SharedPreferences sharedPreferences, Context context) {
        return getString(sharedPreferences, context, R.string.settings_sensor_bluetooth_running_speed_and_cadence_key, getBluetoothSensorAddressNone(context));
    }

    public static boolean shouldShowStatsOnLockscreen(SharedPreferences sharedPreferences, Context context) {
        final boolean STATS_SHOW_ON_LOCKSCREEN_DEFAULT = context.getResources().getBoolean(R.bool.stats_show_on_lockscreen_while_recording_default);
        return getBoolean(sharedPreferences, context, R.string.stats_show_on_lockscreen_while_recording_key, STATS_SHOW_ON_LOCKSCREEN_DEFAULT);
    }

    public static boolean shouldKeepScreenOn(SharedPreferences sharedPreferences, Context context) {
        final boolean DEFAULT = context.getResources().getBoolean(R.bool.stats_keep_screen_on_while_recording_default);
        return getBoolean(sharedPreferences, context, R.string.stats_keep_screen_on_while_recording_key, DEFAULT);
    }

    public static boolean shouldUseFullscreen(SharedPreferences sharedPreferences, Context context) {
        final boolean DEFAULT = context.getResources().getBoolean(R.bool.stats_fullscreen_while_recording_default);
        return getBoolean(sharedPreferences, context, R.string.stats_fullscreen_while_recording_key, DEFAULT);
    }

    public static Duration getVoiceAnnouncementFrequency(SharedPreferences sharedPreferences, Context context) {
        final int DEFAULT = Integer.parseInt(context.getResources().getString(R.string.voice_announcement_frequency_default));
        int value = getInt(sharedPreferences, context, R.string.voice_announcement_frequency_key, DEFAULT);
        return Duration.ofSeconds(value);
    }

    /**
     * @return Result depends on isMetricUnits
     */
    public static Distance getVoiceAnnouncementDistance(SharedPreferences sharedPreferences, Context context) {
        final float DEFAULT = Integer.parseInt(context.getResources().getString(R.string.voice_announcement_distance_default));
        float value = getFloat(sharedPreferences, context, R.string.voice_announcement_distance_key, DEFAULT);
        return Distance.one(isMetricUnits(sharedPreferences, context)).multipliedBy(value);
    }

    public static float getVoiceSpeedRate(SharedPreferences sharedPreferences, Context context) {
        final float DEFAULT = Float.parseFloat(context.getResources().getString(R.string.voice_speed_rate_default));
        return getFloat(sharedPreferences, context, R.string.voice_speed_rate_key, DEFAULT);
    }

    public static Distance getRecordingDistanceInterval(SharedPreferences sharedPreferences, Context context) {
        return Distance.of(getInt(sharedPreferences, context, R.string.recording_distance_interval_key, getRecordingDistanceIntervalDefaultInternal(context)));
    }

    public static Distance getRecordingDistanceIntervalDefault(Context context) {
        return Distance.of(getRecordingDistanceIntervalDefaultInternal(context));
    }

    private static int getRecordingDistanceIntervalDefaultInternal(Context context) {
        return Integer.parseInt(context.getResources().getString(R.string.recording_distance_interval_default));
    }

    public static Distance getMaxRecordingDistance(SharedPreferences sharedPreferences, Context context) {
        final int MAX_RECORDING_DISTANCE = Integer.parseInt(context.getResources().getString(R.string.max_recording_distance_default));
        return Distance.of(getInt(sharedPreferences, context, R.string.max_recording_distance_key, MAX_RECORDING_DISTANCE));
    }

    public static Duration getMinRecordingInterval(SharedPreferences sharedPreferences, Context context) {
        final Duration MIN_RECORDING_INTERVAL = getMinRecordingIntervalDefault(context);
        Duration interval = Duration.ofSeconds(getInt(sharedPreferences, context, R.string.min_recording_interval_key, (int) MIN_RECORDING_INTERVAL.getSeconds()));

        if (interval.isNegative()) {
            // Due to removal of adaptive listener policy; used -1, and -2
            interval = getMinRecordingIntervalDefault(context);
        }
        return interval;
    }

    public static Duration getMinRecordingIntervalDefault(Context context) {
        return Duration.ofSeconds(Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_default)));
    }

    public static Distance getThresholdHorizontalAccuracy(SharedPreferences sharedPreferences, Context context) {
        final int RECORDING_GPS_ACCURACY = Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_default));
        return Distance.of(getInt(sharedPreferences, context, R.string.recording_gps_accuracy_key, RECORDING_GPS_ACCURACY));
    }

    public static boolean shouldInstantExportAfterWorkout(SharedPreferences sharedPreferences, Context context) {
        final boolean INSTANT_POST_WORKOUT_EXPORT_DEFAULT = context.getResources().getBoolean(R.bool.post_workout_export_enabled_default);
        return getBoolean(sharedPreferences, context, R.string.post_workout_export_enabled_key, INSTANT_POST_WORKOUT_EXPORT_DEFAULT) && isDefaultExportDirectoryUri(sharedPreferences, context);
    }

    public static TrackFileFormat getExportTrackFileFormat(SharedPreferences sharedPreferences, Context context) {
        final String TRACKFILEFORMAT_NAME_DEFAULT = getString(sharedPreferences, context, R.string.export_trackfileformat_default, null);
        String trackFileFormatName = getString(sharedPreferences, context, R.string.export_trackfileformat_key, TRACKFILEFORMAT_NAME_DEFAULT);
        try {
            return TrackFileFormat.valueOf(trackFileFormatName);
        } catch (Exception e) {
            return TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA;
        }
    }

    public static boolean getPreventReimportTracks(SharedPreferences sharedPreferences, Context context) {
        final boolean defaultValue = getBoolean(sharedPreferences, context, R.bool.import_prevent_reimport_default, false);
        return getBoolean(sharedPreferences, context, R.string.import_prevent_reimport_key, defaultValue);
    }

    /**
     * @return {@link androidx.appcompat.app.AppCompatDelegate}.MODE_*
     */
    public static int getDefaultNightMode(SharedPreferences sharedPreferences, Context context) {
        final String defaultValue = getKey(context, R.string.night_mode_default);
        final String value = getString(sharedPreferences, context, R.string.night_mode_key, defaultValue);

        return Integer.parseInt(value);
    }

    public static SharedPreferences resetPreferences(Context context, boolean readAgain) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (readAgain) {
            // We want to really clear settings now.
            sharedPreferences.edit().clear().commit();
        }
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);

        return sharedPreferences;
    }

    public static DocumentFile getDefaultExportDirectoryUri(SharedPreferences sharedPreferences, Context context) {
        String singleExportDirectorySettingsKey = getString(sharedPreferences, context, R.string.settings_default_export_directory_key, null);
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

    public static void setDefaultExportDirectoryUri(SharedPreferences sharedPreferences, Context context, Uri directoryUri) {
        String value = directoryUri != null ? directoryUri.toString() : null;
        setString(sharedPreferences, context, R.string.settings_default_export_directory_key, value);
    }

    public static boolean isDefaultExportDirectoryUri(SharedPreferences sharedPreferences, Context context) {
        return getDefaultExportDirectoryUri(sharedPreferences, context) != null;
    }

    public static int getLayoutColumns(SharedPreferences sharedPreferences, Context context) {
        return getInt(sharedPreferences, context, R.string.stats_custom_layout_columns_key, context.getResources().getInteger(R.integer.stats_custom_layout_columns_default));
    }

    public static void setLayoutColumns(SharedPreferences sharedPreferences, Context context, int columns) {
        setInt(sharedPreferences, context, R.string.stats_custom_layout_columns_key, columns);
    }

    private static List<TypedArray> getMultiTypedArray(Context context, String key) {
        List<TypedArray> typedArrays = new ArrayList<>();

        try {
            Class<R.array> resource = R.array.class;
            Field field;
            int i = 0;

            do {
                field = resource.getField(key + context.getString(R.string.stats_custom_layout_fields_default_value_separator) + i);
                typedArrays.add(context.getResources().obtainTypedArray(field.getInt(null)));
                i++;
            } while (field != null);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return typedArrays;
    }

    @SuppressLint("ResourceType")
    static String buildDefaultLayout(Context context) {
        List<TypedArray> fieldsArrays = getMultiTypedArray(context, "stats_custom_layout_fields_default_value");
        return context.getString(R.string.default_activity_default) + CsvConstants.LINE_SEPARATOR
                + fieldsArrays.stream().map(i -> i.getString(0) + CsvConstants.ITEM_SEPARATOR + i.getString(1)).collect(Collectors.joining(CsvConstants.LINE_SEPARATOR))
                + CsvConstants.LINE_SEPARATOR;
    }

    public static Layout getCustomLayout(SharedPreferences sharedPreferences, Context context) {
        String csvCustomLayout = getString(sharedPreferences, context, R.string.stats_custom_layout_fields_key, buildDefaultLayout(context));
        List<String> csvParts = Arrays.asList(csvCustomLayout.split(CsvConstants.LINE_SEPARATOR));
        Layout layout = new Layout(csvParts.get(0));
        for (int i = 1; i < csvParts.size(); i++) {
            String[] fieldParts = csvParts.get(i).split(CsvConstants.ITEM_SEPARATOR);
            layout.addField(fieldParts[0], DataField.getTitleByKey(context, fieldParts[0]), fieldParts[1].equals(DataField.YES_VALUE), fieldParts[2].equals(DataField.YES_VALUE), fieldParts[0].equals(context.getString(R.string.stats_custom_layout_coordinates_key)));
        }

        return layout;
    }

    public static void setCustomLayout(SharedPreferences sharedPreferences, Context context, Layout layout) {
        List<DataField> fields = layout.getFields();
        if (fields.isEmpty()) {
            return;
        }

        String csv = layout.getProfile() + CsvConstants.LINE_SEPARATOR
                + fields.stream().map(DataField::toCsv).collect(Collectors.joining(CsvConstants.LINE_SEPARATOR)) + CsvConstants.LINE_SEPARATOR;
        setString(sharedPreferences, context, R.string.stats_custom_layout_fields_key, csv);
    }

    public static void resetCustomLayoutPreferences(Context context) {
        SharedPreferences settings = getSharedPreferences(context);
        if (settings.contains(context.getString(R.string.stats_custom_layout_fields_key))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(context.getString(R.string.stats_custom_layout_fields_key));
            editor.commit();
        }
        if (settings.contains(context.getString(R.string.stats_custom_layout_columns_key))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(context.getString(R.string.stats_custom_layout_columns_key));
            editor.commit();
        }
    }
}
