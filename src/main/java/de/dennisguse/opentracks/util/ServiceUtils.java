package de.dennisguse.opentracks.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

import de.dennisguse.opentracks.services.TrackRecordingService;

public class ServiceUtils {

    private ServiceUtils() {
    }

    /**
     * Returns true if the recording service is running.
     *
     * @param context the current context
     */
    @Deprecated
    public static boolean isTrackRecordingServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        //TODO This approach is deprecated as of API level 26 and should be replaced.
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo serviceInfo : services) {
            ComponentName componentName = serviceInfo.service;
            String serviceName = componentName.getClassName();
            if (TrackRecordingService.class.getName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}
