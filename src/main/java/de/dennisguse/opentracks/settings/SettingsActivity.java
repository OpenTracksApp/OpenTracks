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

package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * An activity for accessing settings.
 *
 * @author Jimmy Shih
 */
public class SettingsActivity extends AbstractSettingsActivity {

    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

    /*
     * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
     * Anonymous inner class will get garbage collected.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key != null && key.equals(PreferencesUtils.getKey(SettingsActivity.this, R.string.recording_track_id_key))) {
                recordingTrackId = PreferencesUtils.getLong(SettingsActivity.this, R.string.recording_track_id_key);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI();
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        addPreferencesFromResource(R.xml.settings);

        configPreference(R.string.settings_chart_key, ChartSettingsActivity.class);
        configPreference(R.string.settings_stats_key, StatsSettingsActivity.class);
        configPreference(R.string.settings_recording_key, RecordingSettingsActivity.class);
        configPreference(R.string.settings_sensor_key, SensorSettingsActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferencesUtils.getSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferencesUtils.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    /**
     * Configures a preference by starting a new activity when it is clicked.
     *
     * @param key the preference key
     * @param cl  the class to start the new activity
     */
    private void configPreference(int key, final Class<?> cl) {
        Preference preference = findPreference(getString(key));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = IntentUtils.newIntent(SettingsActivity.this, cl);
                startActivity(intent);
                return true;
            }
        });
    }

    private void updateUI() {
        //TODO Remove the following if recordingTrackId is replaced by direct communication rather than via preferences.
        boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        Preference resetPreference = findPreference(getString(R.string.settings_reset_key));
        resetPreference.setEnabled(!isRecording);
        resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
    }
}
