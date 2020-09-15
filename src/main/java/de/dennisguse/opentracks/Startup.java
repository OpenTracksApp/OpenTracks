package de.dennisguse.opentracks;

import android.app.Application;
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

        //Include version information into stack traces.
        Log.i(TAG, BuildConfig.APPLICATION_ID + "; BuildType: " + BuildConfig.BUILD_TYPE + "; VersionName: " + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_NAME_FULL + " VersionCode: " + BuildConfig.VERSION_CODE);

        //Set default values of preferences on first start.
        PreferencesUtils.resetPreferences(this, false);
        if (PreferencesUtils.getString(this, R.string.stats_units_key, "").equals("")) {
            String statsUnits = getString(Locale.US.equals(Locale.getDefault()) ? R.string.stats_units_imperial : R.string.stats_units_metric);
            PreferencesUtils.setString(this, R.string.stats_units_key, statsUnits);
        }

        ActivityUtils.applyNightMode(this);

        //TODO Workaround to reset recordingTrackId on app startup as the TrackRecordingService (likely) crashed.
        if (PreferencesUtils.isRecording(this)) {
            Log.e(TAG, "Reset recordingTrackId; likely the TrackRecordingService crashed.");
            PreferencesUtils.setLong(this, R.string.recording_track_id_key, PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        }

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
