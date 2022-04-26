package de.dennisguse.opentracks;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import de.dennisguse.opentracks.settings.PreferencesUtils;

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

        PreferencesUtils.initPreferences(this, getResources());
        // Set default values of preferences on first start.
        PreferencesUtils.resetPreferences(this, false);
        PreferencesUtils.applyDefaultUnit();
        PreferencesUtils.applyNightMode();


        // In debug builds: show thread and VM warnings.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.enableDefaults();
        }
    }

    /**
     * Returns the name of the database used by SQLiteOpenHelper.
     * See {@link android.database.sqlite.SQLiteOpenHelper} for details.
     * @return SQLite database name.
     */
    public String getDatabaseName() {
        return "database.db";
    }
}
