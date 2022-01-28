package de.dennisguse.opentracks.settings;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;

public class MainSettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = MainSettingsFragment.class.getSimpleName();

    private RecordingStatus recordingStatus = TrackRecordingService.STATUS_DEFAULT;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private final TrackRecordingServiceConnection.Callback bindServiceCallback =
            service -> service.getRecordingStatusObservable()
                    .observe(MainSettingsFragment.this, this::onRecordingStatusChanged);

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindServiceCallback);

        findPreference(getString(R.string.settings_defaults_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_defaults_key));
            return true;
        });

        findPreference(getString(R.string.settings_ui_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_ui_key));
            return true;
        });

        findPreference(getString(R.string.settings_gps_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_gps_key));
            return true;
        });

        findPreference(getString(R.string.settings_sensors_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_sensors_key));
            return true;
        });

        findPreference(getString(R.string.settings_announcements_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_announcements_key));
            return true;
        });

        findPreference(getString(R.string.settings_import_export_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_import_export_key));
            return true;
        });

        findPreference(getString(R.string.settings_open_tracks_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_open_tracks_key));
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.menu_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        trackRecordingServiceConnection.bind(getContext());
        updatePrefsDependOnRecording();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;

        if (preference instanceof ResetDialogPreference) {
            dialogFragment = ResetDialogPreference.ResetPreferenceDialog.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), getClass().getSimpleName());
            return;
        }

        super.onDisplayPreferenceDialog(preference);
    }

    private void onRecordingStatusChanged(RecordingStatus status) {
        this.recordingStatus = status;
        if (isAdded()) {
            updatePrefsDependOnRecording();
        }
    }

    private void updatePrefsDependOnRecording() {
        Preference importExportPreference = findPreference(getString(R.string.settings_import_export_key));
        Preference resetPreference = findPreference(getString(R.string.settings_reset_key));

        boolean isRecording = recordingStatus.isRecording();

        importExportPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : getString(R.string.settings_import_export_summary));
        importExportPreference.setEnabled(!isRecording);

        resetPreference.setSummary(isRecording ? getString(R.string.settings_not_while_recording) : getString(R.string.settings_reset_summary));
        resetPreference.setEnabled(!isRecording);
    }
}
