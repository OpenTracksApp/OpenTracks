package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.SharedPreferences;

import de.dennisguse.opentracks.R;

public class PreferencesOpenHelper {
    private final Context context;
    private final SharedPreferences sharedPreferences;

    private static final int PREFERENCES_VERSION = 1;

    private PreferencesOpenHelper(Context context, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    public static PreferencesOpenHelper newInstance(Context context, SharedPreferences sharedPreferences) {
        return new PreferencesOpenHelper(context, sharedPreferences);
    }

    public void checkForUpgrade() {
        int lastVersion = PreferencesUtils.getInt(sharedPreferences, context, R.string.prefs_last_version_key, 0);
        if (PREFERENCES_VERSION > lastVersion) {
            onUpgrade();
        }
    }

    private void onUpgrade() {
        PreferencesUtils.setInt(sharedPreferences, context, R.string.prefs_last_version_key, PREFERENCES_VERSION);
        switch (PREFERENCES_VERSION) {
            case 1:
                upgradeFrom0to1();
        }
    }

    private void upgradeFrom0to1() {
        PreferencesUtils.setString(sharedPreferences, context, R.string.stats_custom_layout_fields_key, PreferencesUtils.buildDefaultLayout(context));
    }
}
