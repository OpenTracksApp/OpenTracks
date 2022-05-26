package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Map;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.IntentDashboardUtils;

public class UserInterfaceSettingsFragment extends PreferenceFragmentCompat {

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (PreferencesUtils.isKey(R.string.night_mode_key, key)) {
            getActivity().runOnUiThread(PreferencesUtils::applyNightMode);
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

        setShowOnMapFormatOptions();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_ui_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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

    private void setShowOnMapFormatOptions() {
        Map<String, String> options = TrackFileFormat.toPreferenceIdLabelMap(getResources(), IntentDashboardUtils.SHOW_ON_MAP_TRACK_FILE_FORMATS);
        options.put(IntentDashboardUtils.PREFERENCE_ID_DASHBOARD, getString(R.string.show_on_dashboard));
        options.put(IntentDashboardUtils.PREFERENCE_ID_ASK, getString(R.string.show_on_map_format_ask));
        ListPreference listPreference = findPreference(getString(R.string.show_on_map_format_key));
        listPreference.setEntries(options.values().toArray(new String[0]));
        listPreference.setEntryValues(options.keySet().toArray(new String[0]));
    }

}
