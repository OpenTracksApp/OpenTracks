package de.dennisguse.opentracks.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.HackUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

public class SettingsActivity extends AppCompatActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, ResetDialogPreference.ResetCallback {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private PrefsFragment prefsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_settings);
        setSupportActionBar(toolbar);

        onReset();
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        prefsFragment.setDefaultActivity(iconValue);
    }

    @Override
    public void onReset() {
        prefsFragment = new PrefsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment, prefsFragment).commit();
    }

    public static class PrefsFragment extends PreferenceFragmentCompat {

        /*
         * Note that sharedPreferenceChangeListener cannot be an anonymous inner class.
         * Anonymous inner class will get garbage collected.
         */
        private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
                if (PreferencesUtils.isKey(getActivity(), R.string.recording_track_id_key, key)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateReset();
                        }
                    });
                }
                if (PreferencesUtils.isKey(getActivity(), R.string.stats_units_key, key)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUnits();
                        }
                    });
                }
                if (PreferencesUtils.isKey(getActivity(), R.string.stats_rate_key, key)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateSpeed();
                        }
                    });
                }
            }
        };

        // Used to forward update from ChooseActivityTypeDialogFragment; TODO Could be replaced with LiveData.
        private ActivityTypePreference.ActivityPreferenceDialog activityPreferenceDialog;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                setPreferencesFromResource(R.xml.settings, rootKey);
            } catch (ClassCastException e) {
                // Some sharedPreference is broken: delete all and reset them; it is just a last resort...
                Toast.makeText(getContext(), R.string.settings_error_initial_values_restored, Toast.LENGTH_LONG).show();
                PreferencesUtils.resetPreferences(getContext(), true);

                setPreferencesFromResource(R.xml.settings, rootKey);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferencesUtils.getSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
            updateSpeed();
            updateUnits();
            updateReset();
            updateBluetooth();
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferencesUtils.getSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;
            if (preference instanceof ResetDialogPreference) {
                dialogFragment = ResetDialogPreference.ResetPreferenceDialog.newInstance(preference.getKey());
            } else if (preference instanceof ActivityTypePreference) {
                activityPreferenceDialog = ActivityTypePreference.ActivityPreferenceDialog.newInstance(preference.getKey());
                dialogFragment = activityPreferenceDialog;
            } else if (preference instanceof BluetoothLePreference) {
                dialogFragment = BluetoothLePreference.BluetoothLePreferenceDialog.newInstance(preference.getKey());
            }

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), getClass().getSimpleName());
                return;
            }

            super.onDisplayPreferenceDialog(preference);
        }

        public void setDefaultActivity(String iconValue) {
            if (activityPreferenceDialog != null) {
                activityPreferenceDialog.updateUI(iconValue);
            }
        }

        private void updateReset() {
            final boolean isRecording = PreferencesUtils.isRecording(getActivity());
            Preference resetPreference = findPreference(getString(R.string.settings_reset_key));
            resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
            resetPreference.setEnabled(!isRecording);
        }

        private void updateSpeed() {
            Preference speedCheckBoxPreference = findPreference(getString(R.string.chart_show_speed_key));
            speedCheckBoxPreference.setTitle(PreferencesUtils.isReportSpeed(getActivity()) ? R.string.stats_speed : R.string.stats_pace);
        }

        private void updateBluetooth() {
            // Disable Bluetooth preference if device does not have Bluetooth
            BluetoothLePreference bluetoothPreference = findPreference(getString(R.string.settings_sensor_bluetooth_heart_rate_key));
            bluetoothPreference.setVisible(BluetoothUtils.hasBluetooth(TAG));
        }

        private void updateUnits() {
            boolean metricUnits = PreferencesUtils.isMetricUnits(getActivity());

            ListPreference voiceFrequency = findPreference(getString(R.string.voice_frequency_key));
            voiceFrequency.setEntries(StringUtils.getFrequencyOptions(getActivity(), metricUnits));

            ListPreference splitFrequency = findPreference(getString(R.string.split_frequency_key));
            splitFrequency.setEntries(StringUtils.getFrequencyOptions(getActivity(), metricUnits));

            ListPreference minRecordingInterval = findPreference(getString(R.string.min_recording_interval_key));
            minRecordingInterval.setEntries(PreferenceHelper.getMinRecordingIntervalEntries(getActivity()));

            ListPreference recordingDistanceInterval = findPreference(getString(R.string.recording_distance_interval_key));
            recordingDistanceInterval.setEntries(PreferenceHelper.getRecordingDistanceIntervalEntries(getActivity(), metricUnits));

            ListPreference maxRecordingDistance = findPreference(getString(R.string.max_recording_distance_key));
            maxRecordingDistance.setEntries(PreferenceHelper.getMaxRecordingDistanceEntries(getActivity(), metricUnits));

            ListPreference recordingGpsAccuracy = findPreference(getString(R.string.recording_gps_accuracy_key));
            recordingGpsAccuracy.setEntries(PreferenceHelper.getRecordingGpsAccuracyEntries(getActivity(), metricUnits));

            ListPreference autoresumeTrack = findPreference(getString(R.string.auto_resume_track_timeout_key));
            autoresumeTrack.setEntries(PreferenceHelper.getAutoResumeTrackTimeoutEntries(getActivity()));

            ListPreference statsRatePreferences = findPreference(getString(R.string.stats_rate_key));
            String[] entries = getResources().getStringArray(metricUnits ? R.array.stats_rate_metric_options : R.array.stats_rate_imperial_options);
            statsRatePreferences.setEntries(entries);

            HackUtils.invalidatePreference(statsRatePreferences);
        }
    }
}
