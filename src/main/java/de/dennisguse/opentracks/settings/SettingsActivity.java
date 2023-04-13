package de.dennisguse.opentracks.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceFragmentCompat;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.databinding.SettingsBinding;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;

public class SettingsActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    public static final String EXTRAS_CHECK_EXPORT_DIRECTORY = "Check Export Directory";
    private boolean checkExportDirectory = false;

    public static final String FRAGMENT_KEY = "fragmentKey";

    private PreferenceFragmentCompat fragment = null;

    private SettingsBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRAS_CHECK_EXPORT_DIRECTORY)) {
            checkExportDirectory = true;
        }

        if (savedInstanceState != null) {
            fragment = (PreferenceFragmentCompat) getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }
        if (fragment == null) {
            fragment = new MainSettingsFragment();
        }

        viewBinding.bottomAppBarLayout.bottomAppBarTitle.setText(getString(R.string.menu_settings));
        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (fragment != null && fragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, fragment);
        }
    }

    @Override
    protected View getRootView() {
        viewBinding = SettingsBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    private PreferenceFragmentCompat getPreferenceScreen(String key) {
        PreferenceFragmentCompat preferenceScreenFragment = null;

        if (key.equals(getString(R.string.settings_defaults_key))) {
            preferenceScreenFragment = new DefaultsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_ui_key))) {
            preferenceScreenFragment = new UserInterfaceSettingsFragment();
        } else if (key.equals(getString(R.string.settings_gps_key))) {
            preferenceScreenFragment = new GpsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_sensors_key))) {
            preferenceScreenFragment = new SensorsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_announcements_key))) {
            preferenceScreenFragment = new AnnouncementsSettingsFragment();
        } else if (key.equals(getString(R.string.settings_import_export_key))) {
            preferenceScreenFragment = new ImportExportSettingsFragment();
        } else if (key.equals(getString(R.string.settings_api_key))) {
            preferenceScreenFragment = new PublicAPISettingsFragment();
        } else if (key.equals(getString(R.string.settings_open_tracks_key))) {
            preferenceScreenFragment = new OpenTracksSettingsFragment();
        }

        return preferenceScreenFragment;
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
