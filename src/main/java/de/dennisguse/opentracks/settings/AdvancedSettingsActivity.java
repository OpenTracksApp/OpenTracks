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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

import de.dennisguse.opentracks.Constants;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * An activity for advanced settings.
 *
 * @author Jimmy Shih
 */
public class AdvancedSettingsActivity extends AbstractSettingsActivity {

    private static final String TAG = AdvancedSettingsActivity.class.getSimpleName();

    private static final int DIALOG_CONFIRM_RESET_ID = 1;

    private Preference resetPreference;

    private SharedPreferences sharedPreferences;
    private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;

    /*
     * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
     * Anonymous inner class will get garbage collected.
     */
    private final OnSharedPreferenceChangeListener
            sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key != null && key.equals(PreferencesUtils.getKey(
                    AdvancedSettingsActivity.this, R.string.recording_track_id_key))) {
                recordingTrackId = PreferencesUtils.getLong(
                        AdvancedSettingsActivity.this, R.string.recording_track_id_key);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.settings_advanced);

        sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

        resetPreference = findPreference(getString(R.string.settings_reset_key));
        resetPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                showDialog(DIALOG_CONFIRM_RESET_ID);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CONFIRM_RESET_ID:
                return DialogUtils.createConfirmationDialog(this, R.string.settings_reset_confirm_title,
                        getString(R.string.settings_reset_confirm_message),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int button) {
                                onResetPreferencesConfirmed();
                            }
                        });
            default:
                return null;
        }
    }

    /**
     * Updates the UI based on the recording state.
     */
    private void updateUi() {
        boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
        resetPreference.setEnabled(!isRecording);
        resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
    }

    /**
     * Callback when the user confirms resetting all settings.
     */
    private void onResetPreferencesConfirmed() {
        // Change preferences in a separate thread
        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Resetting all settings");

                // Actually wipe preferences and save synchronously
                sharedPreferences.edit().clear().apply();

                // Give UI feedback in the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AdvancedSettingsActivity.this, R.string.settings_reset_done, Toast.LENGTH_SHORT)
                                .show();
                        // Restart the settings activity so all changes are loaded
                        Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            }
        }.start();
    }
}
