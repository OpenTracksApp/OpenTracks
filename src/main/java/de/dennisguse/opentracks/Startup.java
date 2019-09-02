package de.dennisguse.opentracks;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import de.dennisguse.opentracks.util.PreferencesUtils;

public class Startup extends Application {

    private final static String TAG = Startup.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        //Include version information into stack traces.
        Log.i(TAG, BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_NAME_FULL + " VersionCode: " + BuildConfig.VERSION_CODE);

        //Set default values of preferences on first start.
        PreferencesUtils.resetPreferences(this, false);


        //In debug builds: show thread and VM warnings.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Enabling strict mode");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
}
