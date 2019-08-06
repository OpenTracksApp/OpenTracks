package de.dennisguse.opentracks;

import android.app.Application;

import de.dennisguse.opentracks.util.PreferencesUtils;

public class InitPreferences extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Set default values of preferences on first start.
        PreferencesUtils.resetPreferences(this, false);
    }
}
