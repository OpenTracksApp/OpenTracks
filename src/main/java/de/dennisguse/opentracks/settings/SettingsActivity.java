package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;

public class SettingsActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    public static final String EXTRAS_CHECK_EXPORT_DIRECTORY = "Check Export Directory";
    private boolean checkExportDirectory = false;

    public static final String FRAGMENT_KEY = "fragmentKey";

    private PreferenceFragmentCompat fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRAS_CHECK_EXPORT_DIRECTORY)) {
            checkExportDirectory = true;
        }

        if (savedInstanceState != null) {
            fragment = (PreferenceFragmentCompat) getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        } else {
            fragment = new MainSettingsFragment();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment, fragment).commit();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, fragment);
    }

    @Override
    protected View getRootView() {
        return getLayoutInflater().inflate(R.layout.settings, null);
    }

    private PreferenceFragmentCompat getPreferenceScreen(String key) {
        PreferenceFragmentCompat fragment = null;

        if (key.equals(getString(R.string.settings_defaults_key))) {
            fragment = new DefaultsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_ui_key))) {
            fragment = new UserInterfaceSettingsFragment();
        } else if (key.equals(getString(R.string.settings_gps_key))) {
            fragment = new GpsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_sensors_key))) {
            fragment = new SensorsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_announcements_key))) {
            fragment = new AnnouncementsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_import_export_key))) {
            fragment = new ImportExportSettingsFragment();
        } else if (key.equals(getString(R.string.settings_open_tracks_key))) {
            fragment = new OpenTracksSettingsFragment();
        }

        return fragment;
    }

    public PreferenceFragmentCompat openScreen(String key) {
        fragment = getPreferenceScreen(key);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment, fragment).addToBackStack(key).commit();
        return fragment;
    }

    @Override
    public void onChooseActivityTypeDone(String iconValue) {
        try {
            ((ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller) this.fragment).onChooseActivityTypeDone(iconValue);
        } catch (ClassCastException e) {
            throw new ClassCastException(this.fragment.getClass().getSimpleName() + " must implement " + ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller.class.getSimpleName());
        }
    }
}
