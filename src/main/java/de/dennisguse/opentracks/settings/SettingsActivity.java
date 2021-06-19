package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeCyclingCadenceAndSpeedPreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeCyclingPowerPreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeHeartRatePreference;
import de.dennisguse.opentracks.settings.bluetooth.BluetoothLeSensorPreference;
import de.dennisguse.opentracks.util.ActivityUtils;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.HackUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

public class SettingsActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller, ResetDialogPreference.ResetCallback {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    public static final String EXTRAS_CHECK_EXPORT_DIRECTORY = "Check Export Directory";

    private PrefsFragment prefsFragment;
    private boolean checkExportDirectory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRAS_CHECK_EXPORT_DIRECTORY)) {
            checkExportDirectory = true;
        }
        onReset();
    }

    @Override
    protected View getRootView() {
        return getLayoutInflater().inflate(R.layout.settings, null);
    }

    @Override
    protected void setupActionBarBack(Toolbar toolbar) {
        super.setupActionBarBack(toolbar);
        toolbar.setTitle(R.string.menu_settings);
    }

    @Override
    protected void onResume() {
        if (checkExportDirectory) {
            checkExportDirectory = false;
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_logo_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.export_error_post_workout)
                    .setNeutralButton(android.R.string.ok, null)
                    .create()
                    .show();
        }

        super.onResume();
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

        private TextToSpeech tts;
        private int ttsInitStatus;

        private SharedPreferences sharedPreferences;

        private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (PreferencesUtils.isKey(getActivity(), R.string.stats_units_key, key)) {
                getActivity().runOnUiThread(this::updateUnits);
            }
            if (PreferencesUtils.isKey(getActivity(), R.string.night_mode_key, key)) {
                getActivity().runOnUiThread(() -> ActivityUtils.applyNightMode(sharedPreferences, getContext()));
            }
        };

        private TrackRecordingServiceConnection trackRecordingServiceConnection;
        private TrackRecordingService.RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;

        private final Runnable bindServiceCallback = new Runnable() {
            @Override
            public void run() {
                TrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
                if (service == null) {
                    Log.w(TAG, "could not get TrackRecordingService");
                    return;
                }

                service.getRecordingStatusObservable()
                        .observe(PrefsFragment.this, status -> onRecordingStatusChanged(status));

            }
        };

        // Used to forward update from ChooseActivityTypeDialogFragment; TODO Could be replaced with LiveData.
        private ActivityTypePreference.ActivityPreferenceDialog activityPreferenceDialog;

        private Preference announcementsFrequency;
        private Preference announcementsSpeed;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ttsInitStatus = TextToSpeech.ERROR;
            tts = new TextToSpeech(getContext(), status -> {
                ttsInitStatus = status;
                Log.i(TAG, "TextToSpeech initialized with status " + status);

                if (announcementsFrequency != null && announcementsSpeed != null) {
                    updateVoiceAnnouncements();
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());

            trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindServiceCallback);

            try {
                setPreferencesFromResource(R.xml.settings, rootKey);
            } catch (ClassCastException e) {
                // Some sharedPreference is broken: delete all and reset them; it is just a last resort...
                Toast.makeText(getContext(), R.string.settings_error_initial_values_restored, Toast.LENGTH_LONG).show();
                PreferencesUtils.resetPreferences(getContext(), true);

                setPreferencesFromResource(R.xml.settings, rootKey);
            }

            setExportTrackFileFormatOptions();
            setWheelCircumferenceInputFilter();

            Preference instantExportDirectoryPreference = findPreference(getString(R.string.settings_default_export_directory_key));
            instantExportDirectoryPreference.setSummaryProvider(preference -> {
                DocumentFile directory = PreferencesUtils.getDefaultExportDirectoryUri(sharedPreferences, getContext());
                //Use same value for not set as Androidx ListPreference and EditTextPreference
                return directory != null ? directory.getName() : getString(R.string.not_set);
            });

            Preference customLayoutPreference = findPreference(getString(R.string.stats_custom_layout_key));
            customLayoutPreference.setOnPreferenceClickListener((preference) -> {
                Intent intent = new Intent(getContext(), SettingsCustomLayoutActivity.class);
                startActivity(intent);
                return true;
            });

            announcementsFrequency = findPreference(getString(R.string.voice_frequency_key));
            announcementsSpeed = findPreference(getString(R.string.voice_speed_rate_key));
            updateVoiceAnnouncements();
        }

        @Override
        public void onResume() {
            super.onResume();

            trackRecordingServiceConnection.bind(getContext());

            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
            sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

            updateUnits();
            updatePrefsDependOnRecording();
            updateBluetooth();
            updatePostWorkoutExport();
        }

        private void updatePostWorkoutExport() {
            Preference instantExportEnabledPreference = findPreference(getString(R.string.post_workout_export_enabled_key));
            instantExportEnabledPreference.setEnabled(PreferencesUtils.isDefaultExportDirectoryUri(sharedPreferences, getContext()));
        }

        @Override
        public void onPause() {
            super.onPause();
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;
            if (preference instanceof ResetDialogPreference) {
                dialogFragment = ResetDialogPreference.ResetPreferenceDialog.newInstance(preference.getKey());
            } else if (preference instanceof ActivityTypePreference) {
                activityPreferenceDialog = ActivityTypePreference.ActivityPreferenceDialog.newInstance(preference.getKey());
                dialogFragment = activityPreferenceDialog;
            } else if (preference instanceof BluetoothLeHeartRatePreference) {
                dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.HEART_RATE_SUPPORTING_DEVICES);
            } else if (preference instanceof BluetoothLeCyclingCadenceAndSpeedPreference) {
                dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.CYCLING_SPEED_CADENCE_SERVICE_UUID);
            } else if (preference instanceof BluetoothLeCyclingPowerPreference) {
                dialogFragment = BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog.newInstance(preference.getKey(), BluetoothUtils.CYCLING_POWER_UUID);
            }

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
                return;
            }

            super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            trackRecordingServiceConnection.unbind(getContext());
            sharedPreferences = null;
            announcementsFrequency = null;
            announcementsSpeed = null;
            tts = null;
        }

        public void setDefaultActivity(String iconValue) {
            if (activityPreferenceDialog != null) {
                activityPreferenceDialog.updateUI(iconValue);
            }
        }

        private void updatePrefsDependOnRecording() {
            Preference resetPreference = findPreference(getString(R.string.settings_reset_key));
            Preference importPreference = findPreference(getString(R.string.settings_import));
            Preference exportPreference = findPreference(getString(R.string.settings_export));

            boolean isRecording = recordingStatus.isRecording();
            resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
            importPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");
            exportPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : "");

            resetPreference.setEnabled(!isRecording);
            importPreference.setEnabled(!isRecording);
            exportPreference.setEnabled(!isRecording);
        }

        private void updateBluetooth() {
            // Disable Bluetooth preference if device does not have Bluetooth
            BluetoothLeSensorPreference bluetoothPreference = findPreference(getString(R.string.settings_sensor_bluetooth_heart_rate_key));
            bluetoothPreference.setVisible(BluetoothUtils.hasBluetooth(getContext()));
        }

        private void updateUnits() {
            boolean metricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getActivity());

            ListPreference voiceFrequency = findPreference(getString(R.string.voice_frequency_key));
            voiceFrequency.setEntries(StringUtils.getFrequencyOptions(getActivity(), metricUnits));

            ListPreference minRecordingInterval = findPreference(getString(R.string.min_recording_interval_key));
            minRecordingInterval.setEntries(PreferenceHelper.getMinRecordingIntervalEntries(getActivity()));

            ListPreference recordingDistanceInterval = findPreference(getString(R.string.recording_distance_interval_key));
            recordingDistanceInterval.setEntries(PreferenceHelper.getRecordingDistanceIntervalEntries(getActivity(), metricUnits));

            ListPreference maxRecordingDistance = findPreference(getString(R.string.max_recording_distance_key));
            maxRecordingDistance.setEntries(PreferenceHelper.getMaxRecordingDistanceEntries(getActivity(), metricUnits));

            ListPreference recordingGpsAccuracy = findPreference(getString(R.string.recording_gps_accuracy_key));
            recordingGpsAccuracy.setEntries(PreferenceHelper.getRecordingGpsAccuracyEntries(getActivity(), metricUnits));

            ListPreference statsRatePreferences = findPreference(getString(R.string.stats_rate_key));
            String[] entries = getResources().getStringArray(metricUnits ? R.array.stats_rate_metric_options : R.array.stats_rate_imperial_options);
            statsRatePreferences.setEntries(entries);

            HackUtils.invalidatePreference(statsRatePreferences);
        }


        private void setExportTrackFileFormatOptions() {
            final TrackFileFormat[] trackFileFormats = {TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES, TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA, TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA, TrackFileFormat.GPX};
            String[] entries = new String[trackFileFormats.length];
            String[] entryValues = new String[trackFileFormats.length];

            for (int i = 0; i < entries.length; i++) {
                TrackFileFormat trackFileFormat = trackFileFormats[i];
                String trackFileFormatUpperCase = trackFileFormat.getExtension().toUpperCase(Locale.US); //ASCII upper case
                int photoMessageId = trackFileFormat.includesPhotos() ? R.string.export_with_photos : R.string.export_without_photos;
                entries[i] = String.format("%s (%s)", trackFileFormatUpperCase, getString(photoMessageId));
                entryValues[i] = trackFileFormat.name();
            }

            ListPreference listPreference = findPreference(getString(R.string.export_trackfileformat_key));
            listPreference.setEntries(entries);
            listPreference.setEntryValues(entryValues);
        }

        private void setWheelCircumferenceInputFilter() {
            EditTextPreference wheelPreference = findPreference(getString(R.string.settings_sensor_bluetooth_cycling_speed_wheel_circumference_key));
            wheelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof String) {
                    try {
                        int newValueInt = Integer.parseInt((String) newValue);
                        return newValueInt > 500 && newValueInt < 4000;
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Entered string is no number.");
                    }
                }
                return false;
            });
        }

        private void updateVoiceAnnouncements() {
            boolean enabled = ttsInitStatus == TextToSpeech.SUCCESS;
            announcementsFrequency.setEnabled(enabled);
            announcementsSpeed.setEnabled(enabled);
        }

        private void onRecordingStatusChanged(TrackRecordingService.RecordingStatus status) {
            this.recordingStatus = status;
            if (!status.isRecording() && isAdded()) {
                updatePrefsDependOnRecording();
            }
        }
    }
}
