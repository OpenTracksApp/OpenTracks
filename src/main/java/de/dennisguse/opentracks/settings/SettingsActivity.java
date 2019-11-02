package de.dennisguse.opentracks.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

public class SettingsActivity extends AppCompatActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    private PrefsFragment prefsFragment = new PrefsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_settings);
        setSupportActionBar(toolbar);

        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment, prefsFragment).commit();
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        prefsFragment.setDefaultActivity(iconValue);
    }

    public static class PrefsFragment extends PreferenceFragmentCompat {

        /*
         * Note that sharedPreferenceChangeListener cannot be an anonymous inner class.
         * Anonymous inner class will get garbage collected.
         */
        private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI();
                    }
                });

                if (PreferencesUtils.isKey(getActivity(), R.string.stats_units_key, key)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUnits();
                        }
                    });
                }
            }
        };

        // Used to forward update from ChooseActivityTypeDialogFragment; TODO Could be replaced with LiveData.
        private ActivityTypePreference.ActivityPreferenceDialog activityPreferenceDialog;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings, rootKey);
            updateUnits();
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferencesUtils.getSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
            updateUI();
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferencesUtils.getSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }

        private void configFrequencyPreference(int key, boolean metricUnits) {
            ListPreference preference = findPreference(getString(key));

            String[] options = StringUtils.getFrequencyOptions(getActivity(), metricUnits);
            preference.setEntries(options);
        }

        private void configListPreference(int key, int valueArray, boolean metricUnits) {
            //TODO Can we make values an int array?
            String[] values = getResources().getStringArray(valueArray);
            final String[] options = new String[values.length];
            switch (key) {
                case R.string.min_recording_interval_key:
                    PreferenceHelper.setMinRecordingIntervalOptions(getActivity(), options, values);
                    break;
                case R.string.recording_distance_interval_key:
                    PreferenceHelper.setRecordingDistanceIntervalOptions(getActivity(), options, values, metricUnits);
                    break;
                case R.string.max_recording_distance_key:
                    PreferenceHelper.setMaxRecordingDistanceOptions(getActivity(), options, values, metricUnits);
                    break;
                case R.string.recording_gps_accuracy_key:
                    PreferenceHelper.setRecordingGpsAccuracyOptions(getActivity(), options, values, metricUnits);
                    break;
                case R.string.auto_resume_track_timeout_key:
                    PreferenceHelper.setAutoResumeTrackTimeoutOptions(getActivity(), options, values);
                    break;
                default:
                    return;
            }

            final ListPreference listPreference = findPreference(getString(key));
            listPreference.setEntries(options);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;
            if (preference instanceof ResetDialogPreference) {
                dialogFragment = ResetDialogPreference.ResetPreferenceDialog.newInstance(preference.getKey());
            } else if (preference instanceof ActivityTypePreference) {
                activityPreferenceDialog = ActivityTypePreference.ActivityPreferenceDialog.newInstance(preference.getKey());
                dialogFragment = activityPreferenceDialog;
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

        private void updateUI() {
            final boolean isRecording = PreferencesUtils.isRecording(getActivity());
            Preference resetPreference = findPreference(getString(R.string.settings_reset_key));
            resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
            resetPreference.setEnabled(!isRecording);

            Preference speedCheckBoxPreference = findPreference(getString(R.string.chart_show_speed_key));
            speedCheckBoxPreference.setTitle(PreferencesUtils.isReportSpeed(getActivity()) ? R.string.stats_speed : R.string.stats_pace);

            ListPreference bluetoothPreference = findPreference(getString(R.string.bluetooth_sensor_key));
            PreferenceHelper.configureBluetoothSensorList(bluetoothPreference);
        }

        private void updateUnits() {
            boolean metricUnits = PreferencesUtils.isMetricUnits(getActivity());

            configFrequencyPreference(R.string.voice_frequency_key, metricUnits);
            configFrequencyPreference(R.string.split_frequency_key, metricUnits);
            configListPreference(R.string.min_recording_interval_key, R.array.min_recording_interval_values, metricUnits);
            configListPreference(R.string.recording_distance_interval_key, R.array.recording_distance_interval_values, metricUnits);
            configListPreference(R.string.max_recording_distance_key, R.array.max_recording_distance_values, metricUnits);
            configListPreference(R.string.recording_gps_accuracy_key, R.array.recording_gps_accuracy_values, metricUnits);
            configListPreference(R.string.auto_resume_track_timeout_key, R.array.auto_resume_track_timeout_values, metricUnits);
            configListPreference(R.string.auto_resume_track_timeout_key, R.array.auto_resume_track_timeout_values, metricUnits);

            final ListPreference statsRatePreferences = findPreference(getString(R.string.stats_rate_key));
            String[] options = getResources().getStringArray(metricUnits ? R.array.stats_rate_metric_options : R.array.stats_rate_imperial_options);
            statsRatePreferences.setEntries(options);
            //TODO This is a hack!!! Need to manually updated the summary as otherwise it will only be updated after scrolling down and up again (bring the object out of view).
            //Check if this is still needed after upgraded PreferenceFragment.
            statsRatePreferences.setSummary(statsRatePreferences.getEntry());
        }
    }
}
