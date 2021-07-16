package de.dennisguse.opentracks;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.util.Log;

import java.util.Locale;

import de.dennisguse.opentracks.util.ActivityUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Code that is executed when the application starts.
 * <p>
 * NOTE: How often actual application startup happens depends on the OS.
 * Not every start of an activity will trigger this.
 */
public class Startup extends Application {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        // Include version information into stack traces.
        Log.i(TAG, BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_NAME_FULL + " VersionCode: " + BuildConfig.VERSION_CODE);

        // Set default values of preferences on first start.
        SharedPreferences sharedPreferences = PreferencesUtils.resetPreferences(this, false);
        if (PreferencesUtils.getString(sharedPreferences, this, R.string.stats_units_key, "").equals("")) {
            PreferencesUtils.setMetricUnits(sharedPreferences, this, !Locale.US.equals(Locale.getDefault()));
        }

        ActivityUtils.applyNightMode(sharedPreferences, this);

        // In debug builds: show thread and VM warnings.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.enableDefaults();
        }
    }
}
