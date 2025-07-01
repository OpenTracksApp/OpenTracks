package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.R;

public class UserInterfaceSettingsFragment extends PreferenceFragmentCompat {

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.theme_key, key)) {
            getActivity().runOnUiThread(() -> {
                PreferencesUtils.applyNightMode();
                Toast.makeText(getContext(), R.string.settings_theme_switch_restart, Toast.LENGTH_LONG).show();
            });
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_user_interface);

        Preference customLayoutPreference = findPreference(getString(R.string.stats_custom_layout_key));
        customLayoutPreference.setOnPreferenceClickListener((preference) -> {
            Intent intent = new Intent(getContext(), SettingsCustomLayoutListActivity.class);
            startActivity(intent);
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_ui_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferencesUtils.registerOnSharedPreferenceChangeListenerSilent(sharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
}
