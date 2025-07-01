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
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.HeartRateZones;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.TrackFilenameGenerator;
import de.dennisguse.opentracks.sensors.SensorType;
import de.dennisguse.opentracks.ui.customRecordingLayout.CsvLayoutUtils;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayoutIO;

/**
 * Utilities to access preferences stored in {@link SharedPreferences}.
 *
 * @author Jimmy Shih
 */
public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();

    private static final int PREFERENCES_VERSION = 2;

    private PreferencesUtils() {
    }

    private static SharedPreferences sharedPreferences;

    private static Resources resources;

    /**
     * Must be called during application startup.
     */
    public static void initPreferences(final Context context) {
        PreferencesUtils.resources = context.getResources();
        PreferencesUtils.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        PreferencesOpenHelper.newInstance(PREFERENCES_VERSION).check();
    }

    public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
        changeListener.onSharedPreferenceChanged(sharedPreferences, null);
    }

    public static void registerOnSharedPreferenceChangeListenerSilent(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
    }

    public static String getDefaultActivityTypeLocalized() {
        return getString(R.string.default_activity_key, resources.getString(R.string.default_activity_default));
    }

    public static void setDefaultActivityLocalized(String newDefaultActivity) {
        setString(R.string.default_activity_key, newDefaultActivity);
    }

    /**
     * Gets a preference key
     *
     * @param keyId the key id
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
        for (int keyId : keyIds) {
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

    @VisibleForTesting
    public static void setString(int keyId, int valueId) {
        setString(keyId, resources.getString(valueId));
    }

    @VisibleForTesting
    public static void setBoolean(int keyId, Boolean value) {
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(getKey(keyId), value);
        editor.apply();
    }

    static void setInt(int keyId, int value) {
        Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(keyId), value);
        editor.apply();
    }

    public static boolean isPublicAPIenabled() {
        return getBoolean(R.string.publicapi_enabled_key, resources.getBoolean(R.bool.publicapi_enabled_default));
    }

    public static boolean isPublicAPIDashboardEnabled() {
        return getBoolean(R.string.publicapi_dashboard_enabled_key, resources.getBoolean(R.bool.publicapi_dashboard_enabled_default));
    }

    public static boolean shouldShowIntroduction() {
        return getBoolean(R.string.show_introduction_screen_key, resources.getBoolean(R.bool.show_introduction_screen_default));
    }

    public static void setShowIntroduction(boolean introduction) {
        setBoolean(R.string.show_introduction_screen_key, introduction);
    }

    public static UnitSystem getUnitSystem() {
        final String STATS_UNIT_DEFAULT = resources.getString(R.string.stats_units_default);

        final String VALUE = getString(R.string.stats_units_key, STATS_UNIT_DEFAULT);
        return Arrays.stream(UnitSystem.values())
                .filter(d -> VALUE.equals(resources.getString(d.getPreferenceId(), STATS_UNIT_DEFAULT)))
                .findFirst()
                .orElse(UnitSystem.defaultUnitSystem()); //TODO This AGAIN defines the default
    }

    public static void setUnit(UnitSystem unitSystem) {
        setString(R.string.stats_units_key, unitSystem.getPreferenceId());
    }

    //TODO Check if actually needed or can be superseeded by a flexible default in getUnit()
    public static void applyDefaultUnit() {
        if (getString(R.string.stats_units_key, "").isEmpty()) {
            if (!Locale.US.equals(Locale.getDefault())) {
                setUnit(UnitSystem.METRIC);
            } else {
                setUnit(UnitSystem.IMPERIAL_FEET);
            }
        }
    }

    public static boolean isReportSpeed(String activityTypeLocalized) {
        final String STATS_RATE_DEFAULT = resources.getString(R.string.stats_rate_default);
        String currentStatsRate = getString(R.string.stats_rate_key, STATS_RATE_DEFAULT);
        if (currentStatsRate.equals(getString(R.string.stats_rate_speed_or_pace_default, STATS_RATE_DEFAULT))) {
            return ActivityType.findByLocalizedString(resources, activityTypeLocalized)
                    .isShowSpeedPreferred();
        }

        return currentStatsRate.equals(resources.getString(R.string.stats_rate_speed));
    }

    public static boolean isReportSpeed(Track track) {
        return isReportSpeed(track.getActivityTypeLocalized());
    }

    private static String getBluetoothSensorAddressNone() {
        return SensorType.NONE.getPreferenceValue();
    }

    public static SensorType getSensorType(String address) {
        return Arrays.stream(SensorType.values())
                .filter(it -> it.getPreferenceValue().equals(address))
                .findFirst()
                .orElse(SensorType.REMOTE);
    }

    public static String getBarometerSensorAddress() {
        return getString(R.string.settings_sensor_bluetooth_pressure_key, getBluetoothSensorAddressNone());
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

    public static boolean getBluetoothFilterEnabled() {
        final boolean DEFAULT = resources.getBoolean(R.bool.settings_sensor_bluetooth_service_filter_enabled_default);
        return getBoolean(R.string.settings_sensor_bluetooth_service_filter_enabled_key, DEFAULT);
    }

    public static HeartRateZones getHeartRateZones() {
        final int DEFAULT = Integer.parseInt(resources.getString(R.string.settings_sensor_heart_rate_max_default));
        int value = getInt(R.string.settings_sensor_heart_rate_max_key, DEFAULT);
        return new HeartRateZones(HeartRate.of(value));
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

    public static boolean shouldShowGpsDisabledWarning() {
        final boolean DEFAULT = resources.getBoolean(R.bool.stats_show_gps_disabled_warnings_default);
        return getBoolean(R.string.stats_show_gps_disabled_warnings_key, DEFAULT);
    }

    public static boolean shouldShowElevation() {
        final boolean DEFAULT = resources.getBoolean(R.bool.chart_display_elevation_default);
        return getBoolean(R.string.chart_display_elevation_key, DEFAULT);
    }

    public static boolean shouldShowPaceOrSpeed() {
        final boolean DEFAULT = resources.getBoolean(R.bool.chart_display_pace_or_speed_default);
        return getBoolean(R.string.chart_display_pace_or_speed_key, DEFAULT);
    }

    public static boolean shouldShowHeartRate() {
        final boolean DEFAULT = resources.getBoolean(R.bool.chart_display_heart_rate_default);
        return getBoolean(R.string.chart_display_heart_rate_key, DEFAULT);
    }

    public static boolean shouldVoiceAnnouncementOnDeviceSpeaker() {
        final boolean DEFAULT = resources.getBoolean(R.bool.voice_on_device_speaker_default);
        return getBoolean(R.string.voice_on_device_speaker_key, DEFAULT);
    }

    public static boolean shouldVoiceAnnouncementIdle() {
        return getBoolean(R.string.voice_announce_idle_key, true);
    }

    public static boolean shouldVoiceAnnounceTime() {
        return getBoolean(R.string.voice_announce_unit_key, true);
    }

    public static boolean shouldVoiceAnnounceUnit() {
        return getBoolean(R.string.voice_announce_unit_key, true);
    }

    public static Duration getVoiceAnnouncementFrequency() {
        final int DEFAULT = Integer.parseInt(resources.getString(R.string.voice_announcement_frequency_default));
        int value = getInt(R.string.voice_announcement_frequency_key, DEFAULT);
        return Duration.ofSeconds(value);
    }

    static String[] getVoiceAnnouncementFrequencyEntries() {
        String[] entryValues = resources.getStringArray(R.array.voice_announcement_frequency_values);
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(entryValues[i])) {
                entries[i] = resources.getString(R.string.value_off);
            } else {
                int value = Integer.parseInt(entryValues[i]);
                entries[i] = resources.getString(R.string.value_integer_minute, Duration.ofSeconds(value).toMinutes());
            }
        }
        return entries;
    }

    /**
     * @return Result depends on getUnitSystem
     */
    public static Distance getVoiceAnnouncementDistance() {
        final float DEFAULT = Integer.parseInt(resources.getString(R.string.voice_announcement_distance_default));
        float value = getFloat(R.string.voice_announcement_distance_key, DEFAULT);
        return Distance.one(getUnitSystem()).multipliedBy(value);
    }

    /**
     * @return Result depends on getUnitSystem
     */
    static String[] getVoiceAnnouncementDistanceEntries() {
        String[] entryValues = resources.getStringArray(R.array.voice_announcement_distance_values);
        String[] entries = new String[entryValues.length];
        UnitSystem unitSystem = getUnitSystem();

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setUnit(unitSystem)
                .build(resources);
        for (int i = 0; i < entryValues.length; i++) {
            if (resources.getString(R.string.announcement_off).equals(entryValues[i])) {
                entries[i] = resources.getString(R.string.value_off);
            } else {
                Distance distance = Distance.one(unitSystem).multipliedBy(Double.parseDouble(entryValues[i]));
                entries[i] = formatter.formatDistance(distance);
            }
        }
        return entries;
    }

    public static float getVoiceSpeedRate() {
        final float DEFAULT = Float.parseFloat(resources.getString(R.string.voice_speed_rate_default));
        return getFloat(R.string.voice_speed_rate_key, DEFAULT);
    }

    public static boolean shouldVoiceAnnounceTotalDistance() {
        return getBoolean(R.string.voice_announce_total_distance_key, true);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceTotalDistance(boolean value) {
        setBoolean(R.string.voice_announce_total_distance_key, value);
    }

    public static boolean shouldVoiceAnnounceMovingTime() {
        return getBoolean(R.string.voice_announce_moving_time_key, true);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceMovingTime(boolean value) {
        setBoolean(R.string.voice_announce_moving_time_key, value);
    }

    public static boolean shouldVoiceAnnounceAverageSpeedPace() {
        return getBoolean(R.string.voice_announce_average_speed_pace_key, true);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceAverageSpeedPace(boolean value) {
        setBoolean(R.string.voice_announce_average_speed_pace_key, value);
    }

    public static boolean shouldVoiceAnnounceLapSpeedPace() {
        return getBoolean(R.string.voice_announce_lap_speed_pace_key, true);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceLapSpeedPace(boolean value) {
        setBoolean(R.string.voice_announce_lap_speed_pace_key, value);
    }

    public static boolean shouldVoiceAnnounceLapPower() {
        return getBoolean(R.string.voice_announce_lap_power_key, false);
    }

    public static boolean shouldVoiceAnnounceHeartRateCurrent() {
        return getBoolean(R.string.voice_announce_heart_rate_current_key, false);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceHeartRateCurrent(boolean value) {
        setBoolean(R.string.voice_announce_heart_rate_current_key, value);
    }

    public static boolean shouldVoiceAnnounceLapHeartRate() {
        return getBoolean(R.string.voice_announce_lap_heart_rate_key, false);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceLapHeartRate(boolean value) {
        setBoolean(R.string.voice_announce_lap_heart_rate_key, value);
    }

    public static boolean shouldVoiceAnnounceAverageHeartRate() {
        return getBoolean(R.string.voice_announce_average_heart_rate_key, false);
    }

    @VisibleForTesting
    public static void setVoiceAnnounceAverageHeartRate(boolean value) {
        setBoolean(R.string.voice_announce_average_heart_rate_key, value);
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
        UnitSystem unitSystem = getUnitSystem();

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setUnit(unitSystem)
                .setDecimalCount(0)
                .setThreshold(Double.MAX_VALUE)
                .build(resources);
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            Distance distance = Distance.of(1).multipliedBy(value);

            String displayValue = formatter.formatDistance(distance);
            switch (unitSystem) {
                case METRIC, IMPERIAL_METER -> {
                    if (value == recordingDistanceIntervalDefault) {
                        entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                    } else {
                        entries[i] = displayValue;
                    }
                }
                case IMPERIAL_FEET, NAUTICAL_IMPERIAL -> {
                    if (value == recordingDistanceIntervalDefault) {
                        entries[i] = resources.getString(R.string.value_integer_feet_recommended, (int) distance.toFT());
                    } else {
                        entries[i] = displayValue;
                    }
                }
                default -> throw new RuntimeException("Not implemented");
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
        UnitSystem unitSystem = getUnitSystem();

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setThreshold(Double.MAX_VALUE)
                .setUnit(unitSystem)
                .build(resources);
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            Distance distance = Distance.of(1).multipliedBy(value);

            String displayValue = formatter.formatDistance(distance);
            switch (unitSystem) {
                case METRIC, IMPERIAL_METER -> {
                    if (value == maxRecordingDistanceDefault) {
                        entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                    } else {
                        entries[i] = displayValue;
                    }
                }
                case IMPERIAL_FEET, NAUTICAL_IMPERIAL -> {
                    if (value == maxRecordingDistanceDefault) {
                        entries[i] = resources.getString(R.string.value_integer_feet_recommended, (int) distance.toFT());
                    } else {
                        entries[i] = displayValue;
                    }
                }
                default -> throw new RuntimeException("Not implemented");
            }
        }

        return entries;
    }

    public static Duration getMinSamplingInterval() {
        final Duration MIN_SAMPLING_INTERVAL = getMinSamplingIntervalDefault();
        return Duration.ofSeconds(getInt(R.string.min_sampling_interval_key, (int) MIN_SAMPLING_INTERVAL.getSeconds()));
    }

    public static Duration getMinSamplingIntervalDefault() {
        return Duration.ofSeconds(Integer.parseInt(resources.getString(R.string.min_sampling_interval_default)));
    }

    static String[] getMinSamplingIntervalEntries() {
        String[] entryValues = resources.getStringArray(R.array.min_sampling_interval_values);
        long recommended = PreferencesUtils.getMinSamplingIntervalDefault().getSeconds();
        String[] entries = new String[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (value == recommended) {
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

        UnitSystem unitSystem = getUnitSystem();

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setThreshold(Double.MAX_VALUE)
                .setUnit(unitSystem)
                .build(resources);

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);
            Distance distance = Distance.of(1).multipliedBy(value);

            String displayValue = formatter.formatDistance(distance);
            switch (unitSystem) {
                case METRIC, IMPERIAL_METER -> {
                    if (value == recordingGPSAccuracyDefault) {
                        entries[i] = resources.getString(R.string.value_integer_meter_recommended, value);
                    } else if (value == recordingGPSAccuracyExcellent) {
                        entries[i] = resources.getString(R.string.value_integer_meter_excellent_gps, value);
                    } else if (value == recordingGPSAccuracyPoor) {
                        entries[i] = resources.getString(R.string.value_integer_meter_poor_gps, value);
                    } else {
                        entries[i] = displayValue;
                    }
                }
                case IMPERIAL_FEET, NAUTICAL_IMPERIAL -> {
                    if (value == recordingGPSAccuracyDefault) {
                        entries[i] = resources.getString(R.string.value_integer_feet_recommended, (int) distance.toFT());
                    } else if (value == recordingGPSAccuracyExcellent) {
                        entries[i] = resources.getString(R.string.value_integer_feet_excellent_gps, (int) distance.toFT());
                    } else {
                        entries[i] = displayValue;
                    }
                }
                default -> throw new RuntimeException("Not implemented");
            }
        }

        return entries;
    }

    public static Duration getIdleDurationTimeout() {
        final int DEFAULT = Integer.parseInt(resources.getString(R.string.idle_duration_default));
        int value = getInt(R.string.idle_duration_key, DEFAULT);
        return Duration.ofSeconds(value);
    }

    static String[] getIdleDurationEntries() {
        String[] entryValues = resources.getStringArray(R.array.idle_duration_values);
        String[] entries = new String[entryValues.length];

        final int idleDurationDefault = Integer.parseInt(resources.getString(R.string.idle_duration_default));

        for (int i = 0; i < entryValues.length; i++) {
            int value = Integer.parseInt(entryValues[i]);

            if (resources.getString(R.string.announcement_off).equals(entryValues[i])) {
                entries[i] = resources.getString(R.string.value_off);
            } else if (value == idleDurationDefault) {
                entries[i] = resources.getString(R.string.value_int_seconds, value);
            } else {
                entries[i] = value < 60 ? resources.getString(R.string.value_integer_second, value) : resources.getString(R.string.value_integer_minute, value / 60);
            }
        }

        return entries;
    }


    public static boolean shouldInstantExportAfterWorkout() {
        final boolean INSTANT_POST_WORKOUT_EXPORT_DEFAULT = resources.getBoolean(R.bool.post_workout_export_enabled_default);
        return getBoolean(R.string.post_workout_export_enabled_key, INSTANT_POST_WORKOUT_EXPORT_DEFAULT) && isDefaultExportDirectoryUri();
    }

    public static TrackFilenameGenerator getTrackFileformatGenerator() {
        String DEFAULT = resources.getString(R.string.export_filename_format_default);
        TrackFilenameGenerator generator = new TrackFilenameGenerator(getString(R.string.export_filename_format_key, DEFAULT));
        if (generator.isValid()) {
            return generator;
        } else {
            return new TrackFilenameGenerator(DEFAULT);
        }
    }

    public static TrackFileFormat getExportTrackFileFormat() {
        final String TRACKFILEFORMAT_NAME_DEFAULT = getString(R.string.export_trackfileformat_default, null);
        String trackFileFormatName = getString(R.string.export_trackfileformat_key, TRACKFILEFORMAT_NAME_DEFAULT);
        return Arrays.stream(TrackFileFormat.values())
                .filter(format -> format.getPreferenceId().equals(trackFileFormatName))
                .findFirst().orElse(TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES);
    }

    public static boolean getPreventReimportTracks() {
        final boolean defaultValue = getBoolean(R.bool.import_prevent_reimport_default, false);
        return getBoolean(R.string.import_prevent_reimport_key, defaultValue);
    }

    record ThemeConfig(int themeResourceId, int dayNight, boolean dynamicColor) {
    }

    private static ThemeConfig getThemeMode() {
        final String defaultValue = getKey(R.string.theme_default);
        final String value = getString(R.string.theme_key, defaultValue);

        switch (value) {
            case "0" -> {
                return new ThemeConfig(R.style.DayNightColorTheme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, false);
            }
            case "1" -> {
                return new ThemeConfig(R.style.DayNightDynamicTheme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, true);
            }
            case "2" -> {
                return new ThemeConfig(R.style.DayNightColorTheme, AppCompatDelegate.MODE_NIGHT_NO, false);
            }
            case "3" -> {
                return new ThemeConfig(R.style.DayNightDynamicTheme, AppCompatDelegate.MODE_NIGHT_NO, true);
            }
            case "4" -> {
                return new ThemeConfig(R.style.DayNightColorTheme, AppCompatDelegate.MODE_NIGHT_YES, false);
            }
            case "5" -> {
                return new ThemeConfig(R.style.DayNightDynamicTheme, AppCompatDelegate.MODE_NIGHT_YES, true);
            }
            case "6" -> {
                return new ThemeConfig(R.style.NightOledColorTheme, AppCompatDelegate.MODE_NIGHT_YES, false);
            }
            case "7" -> {
                return new ThemeConfig(R.style.NightOledDynamicTheme, AppCompatDelegate.MODE_NIGHT_YES, true);
            }

            default -> {
                return new ThemeConfig(R.style.DayNightColorTheme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, false);
            }
        }
    }

    public static void applyTheme(Context context) {
        context.setTheme(getThemeMode().themeResourceId());
    }

    public static void applyNightMode() {
        ThemeConfig themeConfig = getThemeMode();

        AppCompatDelegate.setDefaultNightMode(themeConfig.dayNight);
    }

    public static void applyNightModeAndDynamicColors(Application application) {
        ThemeConfig themeConfig = getThemeMode();

        applyNightMode();

        if (application != null && themeConfig.dynamicColor)
            DynamicColors.applyToActivitiesIfAvailable(application);
    }

    public static void resetPreferences(Context context, boolean readAgain) {
        if (readAgain) {
            // We want to really clear settings now.
            sharedPreferences.edit().clear().commit();
        }
        PreferenceManager.setDefaultValues(context, R.xml.settings, readAgain);
    }

    public static Uri getDefaultExportDirectoryUri() {
        String singleExportDirectory = getString(R.string.settings_default_export_directory_key, null);
        if (singleExportDirectory == null) {
            return null;
        }
        try {
            Log.d(TAG, "DefaultExportDirectoryUri: " + singleExportDirectory);
            return Uri.parse(singleExportDirectory);
        } catch (Exception e) {
            Log.w(TAG, "Could not parse default export directory Uri: " + e.getMessage());
        }
        return null;
    }

    public static void setDefaultExportDirectoryUri(Uri directoryUri) {
        String value = directoryUri != null ? directoryUri.toString() : null;
        Log.d(TAG, "Set ExportDirectoryUri: " + directoryUri);

        setString(R.string.settings_default_export_directory_key, value);
    }

    public static boolean isDefaultExportDirectoryUri() {
        return getDefaultExportDirectoryUri() != null;
    }

    public static int getLayoutColumnsByDefault() {
        return resources.getInteger(R.integer.stats_custom_layout_columns_default);
    }

    private static List<TypedArray> getMultiTypedArray() {
        return Stream.of(
                R.array.stats_custom_layout_fields_default_value_0,
                R.array.stats_custom_layout_fields_default_value_1,
                R.array.stats_custom_layout_fields_default_value_2,
                R.array.stats_custom_layout_fields_default_value_3,
                R.array.stats_custom_layout_fields_default_value_4,
                R.array.stats_custom_layout_fields_default_value_5,
                R.array.stats_custom_layout_fields_default_value_6,
                R.array.stats_custom_layout_fields_default_value_7,
                R.array.stats_custom_layout_fields_default_value_8,
                R.array.stats_custom_layout_fields_default_value_9,
                R.array.stats_custom_layout_fields_default_value_10,
                R.array.stats_custom_layout_fields_default_value_11,
                R.array.stats_custom_layout_fields_default_value_12,
                R.array.stats_custom_layout_fields_default_value_13,
                R.array.stats_custom_layout_fields_default_value_14,
                R.array.stats_custom_layout_fields_default_value_15,
                R.array.stats_custom_layout_fields_default_value_16,
                R.array.stats_custom_layout_fields_default_value_17,
                R.array.stats_custom_layout_fields_default_value_18
        ).map(id -> resources.obtainTypedArray(id)).collect(Collectors.toList());
    }

    @SuppressLint("ResourceType")
    private static String buildDefaultFields() {
        List<TypedArray> fieldsArrays = getMultiTypedArray();
        return fieldsArrays.stream().map(i -> i.getString(0) + CsvLayoutUtils.PROPERTY_SEPARATOR + i.getString(1)).collect(Collectors.joining(CsvLayoutUtils.ITEM_SEPARATOR))
                + CsvLayoutUtils.ITEM_SEPARATOR;
    }

    static String buildDefaultLayout() {
        return resources.getString(R.string.stats_custom_layout_default_layout) + CsvLayoutUtils.ITEM_SEPARATOR + getLayoutColumnsByDefault() + CsvLayoutUtils.ITEM_SEPARATOR + buildDefaultFields();
    }

    public static String getDefaultLayoutName() {
        return resources.getString(R.string.stats_custom_layout_default_layout);
    }

    /**
     * @return custom layout selected or the first one if any has been selected or the one selected is not exists anymore.
     */
    public static RecordingLayout getCustomLayout() {
        String csvCustomLayouts = getString(R.string.stats_custom_layouts_key, buildDefaultLayout());
        String[] csvLines = csvCustomLayouts.split(CsvLayoutUtils.LINE_SEPARATOR);
        String layoutSelected = getString(R.string.stats_custom_layout_selected_layout_key, null);
        if (layoutSelected == null) {
            return RecordingLayoutIO.fromCsv(csvLines[0], resources);
        }

        for (String line : csvLines) {
            RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(line, resources);
            if (recordingLayout.sameName(layoutSelected)) {
                return recordingLayout;
            }
        }

        return RecordingLayoutIO.fromCsv(csvLines[0], resources);
    }

    public static void updateCustomLayouts(@NonNull List<RecordingLayout> recordingLayouts) {
        setString(R.string.stats_custom_layouts_key, RecordingLayoutIO.toCSV(recordingLayouts));
    }

    public static void updateCustomLayout(@NonNull RecordingLayout recordingLayout) {
        List<RecordingLayout> preferenceRecordingLayouts = PreferencesUtils.getAllCustomLayouts();
        Optional<RecordingLayout> layoutToBeUpdated = preferenceRecordingLayouts.stream().filter(l -> l.sameName(recordingLayout)).findFirst();
        if (layoutToBeUpdated.isPresent()) {
            layoutToBeUpdated.get().replaceAllFields(recordingLayout.getFields());
            layoutToBeUpdated.get().setColumnsPerRow(recordingLayout.getColumnsPerRow());
            PreferencesUtils.updateCustomLayouts(preferenceRecordingLayouts);
        }
    }

    public static void addCustomLayout(@NonNull String layoutName) {
        String newLayoutCsv = layoutName + CsvLayoutUtils.ITEM_SEPARATOR + getLayoutColumnsByDefault() + CsvLayoutUtils.ITEM_SEPARATOR + buildDefaultFields();
        String customLayoutCsv = getString(R.string.stats_custom_layouts_key, buildDefaultLayout()) + CsvLayoutUtils.LINE_SEPARATOR + newLayoutCsv;
        setString(R.string.stats_custom_layouts_key, customLayoutCsv);
    }

    public static void setDefaultLayout(String layoutName) {
        setString(R.string.stats_custom_layout_selected_layout_key, layoutName);
    }

    public static List<RecordingLayout> getAllCustomLayouts() {
        List<RecordingLayout> recordingLayouts = new ArrayList<>();
        String csvCustomLayout = getString(R.string.stats_custom_layouts_key, buildDefaultLayout());
        String[] csvLines = csvCustomLayout.split(CsvLayoutUtils.LINE_SEPARATOR);
        for (String line : csvLines) {
            recordingLayouts.add(RecordingLayoutIO.fromCsv(line, resources));
        }

        return recordingLayouts;
    }

    public static List<String> getAllCustomLayoutNames() {
        return getAllCustomLayouts().stream().map(RecordingLayout::getName).collect(Collectors.toList());
    }

    public static void resetCustomLayoutPreferences() {
        if (sharedPreferences.contains(resources.getString(R.string.stats_custom_layouts_key))) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(resources.getString(R.string.stats_custom_layouts_key));
            editor.commit();
        }
    }

    //TODO Check if resetPreferences can be used instead.
    @Deprecated
    @VisibleForTesting
    public static void clear() {
        sharedPreferences.edit().clear().commit();
    }

    public static int getTotalRowsDeleted() {
        return getInt(R.string.total_rows_deleted_key, 0);
    }

    public static void addTotalRowsDeleted(final int totalRowsDeletedToAdd) {
        int newTotalRowsDeleted = getTotalRowsDeleted() + totalRowsDeletedToAdd;
        setInt(R.string.total_rows_deleted_key, newTotalRowsDeleted);
    }

    public static void resetTotalRowsDeleted() {
        setInt(R.string.total_rows_deleted_key, 0);
    }

}
