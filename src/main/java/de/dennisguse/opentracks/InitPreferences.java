package de.dennisguse.opentracks;

import android.app.Application;
import android.preference.PreferenceManager;

public class InitPreferences extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Set default values of preferences on first start.
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_advanced, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_chart, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_recording, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_sensors, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_statistics, false);
    }
}
