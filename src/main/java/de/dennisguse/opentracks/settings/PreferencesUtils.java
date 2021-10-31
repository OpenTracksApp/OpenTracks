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

package de.dennisguse.opentracks.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;
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
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.CsvConstants;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 *
 * @author Jimmy Shih
 */
public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();

    private PreferencesUtils() {
    }

    private static SharedPreferences sharedPreferences;

    private static Resources resources;

    /**
     * Must be called during application startup.
     */
    public static void initPreferences(final Context context, final Resources resources) {
        PreferencesUtils.resources = resources;
        PreferencesUtils.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        PreferencesOpenHelper.newInstance().checkForUpgrade();
    }

    public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
        changeListener.onSharedPreferenceChanged(sharedPreferences, null);
    }

    public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
    }

    public static String getDefaultActivity() {
        return getString(R.string.default_activity_key, resources.getString(R.string.default_activity_default));
    }

    public static void setDefaultActivity(String newDefaultActivity) {
        setString(R.string.default_activity_key, newDefaultActivity);
    }

    /**
     * Gets a preference key
     *
     * @param keyId   the key id
     */
    private static String getKey(int keyId) {
        return resources.getString(keyId);
    }

    /**
     * Compares if keyId and key belong to the same shared preference key.
     *
     * @param keyId The resource id of the key
     * @param key   The key of the preference
     * @return true if key == null or key belongs to keyId
     */
    public static boolean isKey(int keyId, String key) {
        return key == null || key.equals(getKey(keyId));
    }

    public static boolean isKey(int[] keyIds, String key) {
        for(int keyId : keyIds) {
            if (isKey(keyId, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean getBoolean(int keyId, boolean defaultValue) {
        return sharedPreferences.getBoolean(getKey(keyId), defaultValue);
    }

    static int getInt(int keyId, int defaultValue) {
        try {
            return sharedPreferences.getInt(getKey(keyId), defaultValue);
        } catch (ClassCastException e) {
            //Ignore
        }

        //NOTE: We assume that the data was stored as String due to use of ListPreference.
        try {
            String stringValue = sharedPreferences.getString(getKey(keyId), null);
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float getFloat(int keyId, float defaultValue) {
        try {
            return sharedPreferences.getFloat(getKey(keyId), defaultValue);
        } catch (ClassCastException e) {
            //Ignore
        }

        //NOTE: We assume that the data was stored as String due to use of ListPreference.
        try {
            String stringValue = sharedPreferences.getString(getKey(keyId), null);
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(int keyId, String defaultValue) {
        return sharedPreferences.getString(getKey(keyId), defaultValue);
    }

    @VisibleForTesting
    public static void setString(int keyId, String value) {
        Editor editor = sharedPreferences.edit();
        editor.putString(getKey(keyId), value);
        editor.apply();
    }

    static void setInt(int keyId, int value) {
        Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(keyId), value);
        editor.apply();
    }

    public static boolean isMetricUnits() {
        final String STATS_UNIT = resources.getString(R.string.stats_units_default);
        return STATS_UNIT.equals(getString(R.string.stats_units_key, STATS_UNIT));
    }

    public static void setMetricUnits(boolean metricUnits) {
        String unit;
        if (metricUnits) {
            unit = resources.getString(R.string.stats_units_metric);
        } else {
            unit = resources.getString(R.string.stats_units_imperial);
        }
        setString( R.string.stats_units_key, unit);
    }

    public static boolean isReportSpeed(String category) {
        final String STATS_RATE_DEFAULT = resources.getString(R.string.stats_rate_default);
        String currentStatsRate = getString(R.string.stats_rate_key, STATS_RATE_DEFAULT);
        if (currentStatsRate.equals(getString(R.string.stats_rate_speed_or_pace_default, STATS_RATE_DEFAULT))) {
            return TrackIconUtils.isSpeedIcon(resources, category);
        }

        return currentStatsRate.equals(resources.getString(R.string.stats_rate_speed));
    }

    private static String getBluetoothSensorAddressNone() {
        return resources.getString(R.string.sensor_type_value_none);
    }

    public static boolean isBluetoothSensorAddressNone(String currentValue) {
        return getBluetoothSensorAddressNone().equals(currentValue);
    }

    public static String getBluetoothHeartRateSensorAddress() {
        return getString(R.string.settings_sensor_bluetooth_heart_rate_key, getBluetoothSensorAddressNone());
    }

    public static String getBluetoothCyclingCadenceSensorAddress() {
        return getString(R.string.settings_sensor_bluetooth_cycling_cadence_key, getBluetoothSensorAddressNone());
    }

    public static String getBluetoothCyclingSpeedSensorAddress() {
        return getString(R.string.settings_sensor_bluetooth_cycling_speed_key, getBluetoothSensorAddressNone());
    }

    public static Distance getWheelCircumference() {
        final int DEFAULT = Integer.parseInt(resources.getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_default));
        return Distance.ofMM(getInt(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key, DEFAULT));
    }

    public static String getBluetoothCyclingPowerSensorAddress() {
        return getString(R.string.settings_sensor_bluetooth_cycling_power_key, getBluetoothSensorAddressNone());
    }

    public static String getBluetoothRunningSpeedAndCadenceAddress() {
        return getString(R.string.settings_sensor_bluetooth_running_speed_and_cadence_key, getBluetoothSensorAddressNone());
    }

    public static boolean shouldShowStatsOnLockscreen() {
        final boolean STATS_SHOW_ON_LOCKSCREEN_DEFAULT = resources.getBoolean(R.bool.stats_show_on_lockscreen_while_recording_default);
        return getBoolean(R.string.stats_show_on_lockscreen_while_recording_key, STATS_SHOW_ON_LOCKSCREEN_DEFAULT);
    }

    public static boolean shouldKeepScreenOn() {
        final boolean DEFAULT = resources.getBoolean(R.bool.stats_keep_screen_on_while_recording_default);
        return getBoolean(R.string.stats_keep_screen_on_while_recording_key, DEFAULT);
    }

    public static boolean shouldUseFullscreen() {
        final boolean DEFAULT = resources.getBoolean(R.bool.stats_fullscreen_while_recording_default);
        return getBoolean(R.string.stats_fullscreen_while_recording_key, DEFAULT);
    }

    public static Duration getVoiceAnnouncementFrequency() {
        final int DEFAULT = Integer.parseInt(resources.getString(R.string.voice_announcement_frequency_default));
        int value = getInt(R.string.voice_announcement_frequency_key, DEFAULT);
        return Duration.ofSeconds(value);
    }

    static String[] getVoiceAnnouncementFrequencyEntries() {
        String[] values = resources.getStringArray(R.array.voice_announcement_frequency_values);
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(values[i])) {
                options[i] = resources.getString(R.string.value_off);
            } else {
                int value = Integer.parseInt(values[i]);
                options[i] = resources.getString(R.string.value_integer_minute, Duration.ofSeconds(value).toMinutes());
            }
        }
        return options;
    }

    /**
     * @return Result depends on isMetricUnits
     */
    public static Distance getVoiceAnnouncementDistance() {
        final float DEFAULT = Integer.parseInt(resources.getString(R.string.voice_announcement_distance_default));
        float value = getFloat(R.string.voice_announcement_distance_key, DEFAULT);
        return Distance.one(isMetricUnits()).multipliedBy(value);
    }

    /**
     * @return Result depends on isMetricUnits
     */
    static String[] getVoiceAnnouncementDistanceEntries() {
        String[] values = resources.getStringArray(R.array.voice_announcement_distance_values);
        String[] options = new String[values.length];
        boolean metricUnits = isMetricUnits();
        for (int i = 0; i < values.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(values[i])) {
                options[i] = resources.getString(R.string.value_off);
            } else {
                int value = Integer.parseInt(values[i]);
                options[i] = resources.getString(metricUnits ? R.string.value_integer_kilometer : R.string.value_integer_mile, value);
            }
        }
        return options;
    }

    public static float getVoiceSpeedRate() {
        final float DEFAULT = Float.parseFloat(resources.getString(R.string.voice_speed_rate_default));
        return getFloat(R.string.voice_speed_rate_key, DEFAULT);
    }

    public static Distance getRecordingDistanceInterval() {
        return Distance.of(getInt(R.string.recording_distance_interval_key, getRecordingDistanceIntervalDefaultInternal()));
    }

    public static Distance getRecordingDistanceIntervalDefault() {
        return Distance.of(getRecordingDistanceIntervalDefaultInternal());
    }

    private static int getRecordingDistanceIntervalDefaultInternal() {
        return Integer.parseInt(resources.getString(R.string.recording_distance_interval_default));
    }

    static String[] getRecordingDistanceIntervalEntries() {
        String[] entryValues = resources.getStringArray(R.array.recording_distance_interval_values);
        String[] entries = new String[entryValues.length];

        final int recordingDistanceIntervalDefault = (int) getRecordingDistanceIntervalDefault().toM();
        boolean metricUnits = isMetricUnits();

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);
                if (value == recordingDistanceIntervalDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    public static Distance getMaxRecordingDistance() {
        final int MAX_RECORDING_DISTANCE = Integer.parseInt(resources.getString(R.string.max_recording_distance_default));
        return Distance.of(getInt(R.string.max_recording_distance_key, MAX_RECORDING_DISTANCE));
    }

    static String[] getMaxRecordingDistanceEntries() {
        String[] entryValues = resources.getStringArray(R.array.max_recording_distance_values);
        String[] entries = new String[entryValues.length];

        final int maxRecordingDistanceDefault = Integer.parseInt(resources.getString(R.string.max_recording_distance_default));
        boolean metricUnits = isMetricUnits();

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);
                if (value == maxRecordingDistanceDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }

    public static Duration getMinRecordingInterval() {
        final Duration MIN_RECORDING_INTERVAL = getMinRecordingIntervalDefault();
        return Duration.ofSeconds(getInt(R.string.min_recording_interval_key, (int) MIN_RECORDING_INTERVAL.getSeconds()));
    }

    public static Duration getMinRecordingIntervalDefault() {
        return Duration.ofSeconds(Integer.parseInt(resources.getString(R.string.min_recording_interval_default)));
    }

    static String[] getMinRecordingIntervalEntries() {
        String[] entryValues = resources.getStringArray(R.array.min_recording_interval_values);
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (value == PreferencesUtils.getMinRecordingIntervalDefault().getSeconds()) {
                entries[i] = resources.getString(R.string.value_smallest_recommended);
            } else {
                entries[i] = value < 60 ? resources.getString(R.string.value_integer_second, value) : resources.getString(R.string.value_integer_minute, value / 60);
            }
        }

        return entries;
    }

    public static Distance getThresholdHorizontalAccuracy() {
        final int RECORDING_GPS_ACCURACY = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_default));
        return Distance.of(getInt(R.string.recording_gps_accuracy_key, RECORDING_GPS_ACCURACY));
    }

    static String[] getThresholdHorizontalAccuracyEntries() {
        String[] entryValues = resources.getStringArray(R.array.recording_gps_accuracy_values);
        String[] entries = new String[entryValues.length];

        final int recordingGPSAccuracyDefault = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_default));
        final int recordingGPSAccuracyExcellent = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_excellent));
        final int recordingGPSAccuracyPoor = Integer.parseInt(resources.getString(R.string.recording_gps_accuracy_poor));

        boolean metricUnits = isMetricUnits();

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            String displayValue;
            if (metricUnits) {
                displayValue = resources.getString(R.string.value_integer_meter, value);
                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = resources.getString(R.string.value_integer_meter_excellent_gps, value);
                } else if (value == recordingGPSAccuracyPoor) {
                    entries[i] = resources.getString(R.string.value_integer_meter_poor_gps, value);
                } else {
                    entries[i] = displayValue;
                }
            } else {
                int feet = (int) (value * UnitConversions.M_TO_FT);
                displayValue = resources.getString(R.string.value_integer_feet, feet);

                if (value == recordingGPSAccuracyDefault) {
                    entries[i] = resources.getString(R.string.value_integer_feet_recommended, feet);
                } else if (value == recordingGPSAccuracyExcellent) {
                    entries[i] = resources.getString(R.string.value_integer_feet_excellent_gps, feet);
                } else {
                    entries[i] = displayValue;
                }
            }
        }

        return entries;
    }


    public static Speed getIdleSpeed() {
        final float DEFAULT = Float.parseFloat(resources.getString(R.string.idle_speed_default));
        float value = getFloat(R.string.idle_speed_key, DEFAULT);
        return Speed.ofKMH(value);
    }

    static String[] getIdleSpeedEntries() {
        String[] entryValues = resources.getStringArray(R.array.idle_speed_values);
        String[] entries = new String[entryValues.length];

        final float idleSpeedDefault = Float.parseFloat(resources.getString(R.string.idle_speed_default));

        boolean metricUnits = isMetricUnits();

        for (int i = 0; i < entryValues.length; i++) {
            float value = Float.parseFloat(entryValues[i]);
            if (metricUnits) {
                if (value == idleSpeedDefault) {
                    entries[i] = resources.getString(R.string.value_float_kilometer_hour_recommended, value);
                } else {
                    entries[i] = resources.getString(R.string.value_float_kilometer_hour, value);
                }
            } else {
                double valueMPH = Speed.ofKMH(value).toMPH();

                if (value == idleSpeedDefault) {
                    entries[i] = resources.getString(R.string.value_float_mile_hour_recommended, valueMPH);
                } else {
                    entries[i] = resources.getString(R.string.value_float_mile_hour, valueMPH);
                }
            }
        }

        return entries;
    }


    public static boolean shouldInstantExportAfterWorkout(Context context) {
        final boolean INSTANT_POST_WORKOUT_EXPORT_DEFAULT = resources.getBoolean(R.bool.post_workout_export_enabled_default);
        return getBoolean(R.string.post_workout_export_enabled_key, INSTANT_POST_WORKOUT_EXPORT_DEFAULT) && isDefaultExportDirectoryUri(context);
    }

    public static TrackFileFormat getExportTrackFileFormat() {
        final String TRACKFILEFORMAT_NAME_DEFAULT = getString(R.string.export_trackfileformat_default, null);
        String trackFileFormatName = getString(R.string.export_trackfileformat_key, TRACKFILEFORMAT_NAME_DEFAULT);
        try {
            return TrackFileFormat.valueOf(trackFileFormatName);
        } catch (Exception e) {
            return TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA;
        }
    }

    public static boolean getPreventReimportTracks() {
        final boolean defaultValue = getBoolean(R.bool.import_prevent_reimport_default, false);
        return getBoolean(R.string.import_prevent_reimport_key, defaultValue);
    }

    /**
     * @return {@link androidx.appcompat.app.AppCompatDelegate}.MODE_*
     */
    public static int getDefaultNightMode() {
        final String defaultValue = getKey(R.string.night_mode_default);
        final String value = getString(R.string.night_mode_key, defaultValue);

        return Integer.parseInt(value);
    }

    public static void resetPreferences(Context context, boolean readAgain) {
        if (readAgain) {
            // We want to really clear settings now.
            sharedPreferences.edit().clear().commit();
        }
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);
    }

    public static DocumentFile getDefaultExportDirectoryUri(Context context) {
        String singleExportDirectorySettingsKey = getString(R.string.settings_default_export_directory_key, null);
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

    public static void setDefaultExportDirectoryUri(Uri directoryUri) {
        String value = directoryUri != null ? directoryUri.toString() : null;
        setString(R.string.settings_default_export_directory_key, value);
    }

    public static boolean isDefaultExportDirectoryUri(Context context) {
        return getDefaultExportDirectoryUri(context) != null;
    }

    public static int getLayoutColumns() {
        return getInt(R.string.stats_custom_layout_columns_key, resources.getInteger(R.integer.stats_custom_layout_columns_default));
    }

    public static void setLayoutColumns(int columns) {
        setInt(R.string.stats_custom_layout_columns_key, columns);
    }

    private static List<TypedArray> getMultiTypedArray(String key) {
        List<TypedArray> typedArrays = new ArrayList<>();

        try {
            Class<R.array> resource = R.array.class;
            Field field;
            int i = 0;

            do {
                field = resource.getField(key + resources.getString(R.string.stats_custom_layout_fields_default_value_separator) + i);
                typedArrays.add(resources.obtainTypedArray(field.getInt(null)));
                i++;
            } while (field != null); //TODO Catch no such field exception instead of using an endless loop.
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return typedArrays;
    }

    @SuppressLint("ResourceType")
    static String buildDefaultLayout() {
        List<TypedArray> fieldsArrays = getMultiTypedArray("stats_custom_layout_fields_default_value");
        return resources.getString(R.string.default_activity_default) + CsvConstants.LINE_SEPARATOR
                + fieldsArrays.stream().map(i -> i.getString(0) + CsvConstants.ITEM_SEPARATOR + i.getString(1)).collect(Collectors.joining(CsvConstants.LINE_SEPARATOR))
                + CsvConstants.LINE_SEPARATOR;
    }

    public static Layout getCustomLayout() {
        String csvCustomLayout = getString(R.string.stats_custom_layout_fields_key, buildDefaultLayout());
        List<String> csvParts = Arrays.asList(csvCustomLayout.split(CsvConstants.LINE_SEPARATOR));
        Layout layout = new Layout(csvParts.get(0));
        for (int i = 1; i < csvParts.size(); i++) {
            String[] fieldParts = csvParts.get(i).split(CsvConstants.ITEM_SEPARATOR);
            layout.addField(fieldParts[0], DataField.getTitleByKey(resources, fieldParts[0]), fieldParts[1].equals(DataField.YES_VALUE), fieldParts[2].equals(DataField.YES_VALUE), fieldParts[0].equals(resources.getString(R.string.stats_custom_layout_coordinates_key)));
        }

        return layout;
    }

    public static void setCustomLayout(Layout layout) {
        List<DataField> fields = layout.getFields();
        if (fields.isEmpty()) {
            return;
        }

        String csv = layout.getProfile() + CsvConstants.LINE_SEPARATOR
                + fields.stream().map(DataField::toCsv).collect(Collectors.joining(CsvConstants.LINE_SEPARATOR))
                + CsvConstants.LINE_SEPARATOR;
        setString(R.string.stats_custom_layout_fields_key, csv);
    }

    public static void resetCustomLayoutPreferences() {
        if (sharedPreferences.contains(resources.getString(R.string.stats_custom_layout_fields_key))) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(resources.getString(R.string.stats_custom_layout_fields_key));
            editor.commit();
        }
        if (sharedPreferences.contains(resources.getString(R.string.stats_custom_layout_columns_key))) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(resources.getString(R.string.stats_custom_layout_columns_key));
            editor.commit();
        }
    }

    public static void applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(PreferencesUtils.getDefaultNightMode());
    }

    //TODO Check if resetPreferences can be used instead.
    @Deprecated
    @VisibleForTesting
    public static void clear() {
        sharedPreferences.edit().clear().commit();
    }
}
