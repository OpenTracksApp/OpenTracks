package de.dennisguse.opentracks.util;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.TrackPoint;

public class TrackPointUtils {

    // Anything faster than that (in meters per second) will be considered moving.
    private static final double MAX_NO_MOVEMENT_SPEED = 0.224;

    private final static String TAG = TrackPointUtils.class.getSimpleName();

    private TrackPointUtils() {
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
