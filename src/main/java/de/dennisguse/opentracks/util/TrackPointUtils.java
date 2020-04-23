package de.dennisguse.opentracks.util;

import android.util.Log;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

public class TrackPointUtils {

    // Anything faster than that (in meters per second) will be considered moving.
    private static final double MAX_NO_MOVEMENT_SPEED = 0.224;

    private final static String TAG = TrackPointUtils.class.getSimpleName();

    private TrackPointUtils() {
    }

    /**
     * 1. Ancient fix for phones that do not set the time in {@link android.location.Location}.
     * 2. Fix for GPS time rollover happening every 19.7 years: https://en.wikipedia.org/wiki/GPS_Week_Number_Rollover
     */
    public static void fixTime(@NonNull TrackPoint trackPoint) {
        if (trackPoint.getTime() == 0L) {
            Log.w(TAG, "Time of provided location was 0. Using current time.");
            trackPoint.setTime(System.currentTimeMillis());
            return;
        }

        {
            long timeDiff = Math.abs(trackPoint.getTime() - System.currentTimeMillis());

            if (timeDiff > 1023 * UnitConversions.ONE_WEEK_MS) {
                Log.w(TAG, "GPS week rollover.");
                trackPoint.setTime(trackPoint.getTime() + 1024 * UnitConversions.ONE_WEEK_MS);
            }
        }
    }

    public static boolean isMoving(@NonNull TrackPoint trackPoint) {
        return trackPoint.hasSpeed() && trackPoint.getSpeed() >= MAX_NO_MOVEMENT_SPEED;
    }

    public static boolean equalTime(TrackPoint t1, TrackPoint t2) {
        if (t1 == null || t2 == null) {
            return false;
        }

        return t1.getTime() == t2.getTime();
    }

    public static boolean after(TrackPoint t1, TrackPoint t2) {
        if (t1 == null || t2 == null) {
            return false;
        }

        return t1.getTime() > t2.getTime();
    }

    /**
     * Is accuracy better than threshold?
     */
    public static boolean fulfillsAccuracy(@NonNull TrackPoint trackPoint, int poorAccuracy) {
        return trackPoint.hasAccuracy() && trackPoint.getAccuracy() < poorAccuracy;
    }
}
