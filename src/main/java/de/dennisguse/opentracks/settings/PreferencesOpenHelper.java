package de.dennisguse.opentracks.settings;

import de.dennisguse.opentracks.R;

class PreferencesOpenHelper {

    private static final int PREFERENCES_VERSION = 1;

    private PreferencesOpenHelper() {
    }

    static PreferencesOpenHelper newInstance() {
        return new PreferencesOpenHelper();
    }

    void checkForUpgrade() {
        int lastVersion = PreferencesUtils.getInt(R.string.prefs_last_version_key, 0);
        if (PREFERENCES_VERSION > lastVersion) {
            onUpgrade();
        }
    }

    private void onUpgrade() {
        PreferencesUtils.setInt(R.string.prefs_last_version_key, PREFERENCES_VERSION);
        switch (PREFERENCES_VERSION) {
            case 1:
                upgradeFrom0to1();
        }
    }

    private void upgradeFrom0to1() {
        PreferencesUtils.setString(R.string.stats_custom_layout_fields_key, PreferencesUtils.buildDefaultLayout());
    }
}
