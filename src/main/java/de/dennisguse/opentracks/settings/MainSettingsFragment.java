package de.dennisguse.opentracks.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;

public class MainSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

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

        findPreference(getString(R.string.settings_api_key)).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(getString(R.string.settings_api_key));
            return true;
        });
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
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
}
