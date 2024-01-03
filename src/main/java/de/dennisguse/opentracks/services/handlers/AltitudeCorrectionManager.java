package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
import android.location.altitude.AltitudeConverter;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.util.EGM2008Utils;

/**
 * More infos regarding Android 34's <a href="https://issuetracker.google.com/issues/195660815#comment1">AltitudeConverter</a>.
 */
public class AltitudeCorrectionManager {

    private static final String TAG = AltitudeCorrectionManager.class.getSimpleName();

    private AltitudeConverter altitudeConverter;

    private final EGM2008Internal altitudeConverterFallback;

    public AltitudeCorrectionManager() {
        this.altitudeConverterFallback = new EGM2008Internal();
        this.altitudeConverter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ? new AltitudeConverter() : null;
    }

    public void correctAltitude(Context context, TrackPoint trackPoint) {
        if (!trackPoint.hasLocation() || !trackPoint.hasAltitude()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && altitudeConverter != null) {
            try {
                Location loc = trackPoint.getLocation();
                altitudeConverter.addMslAltitudeToLocation(context, loc);
                trackPoint.setAltitude(Altitude.EGM2008.of(loc.getMslAltitudeMeters()));
                return;
            } catch (IOException e) {
                Log.w(TAG, "Android's AltitudeConverter crashed; falling back to internal.");
                altitudeConverter = null;
                // Should we fallback
            }
        }

        altitudeConverterFallback.correctAltitude(context, trackPoint);
    }

    private static class EGM2008Internal {

        private static final String TAG = EGM2008Internal.class.getSimpleName();

        private EGM2008Utils.EGM2008Correction egm2008Correction;

        public void correctAltitude(Context context, TrackPoint trackPoint) {
            if (egm2008Correction == null || !egm2008Correction.canCorrect(trackPoint.getLocation())) {
                try {
                    egm2008Correction = EGM2008Utils.createCorrection(context, trackPoint.getLocation());
                } catch (IOException e) {
                    Log.e(TAG, "Could not load altitude correction for " + trackPoint, e);
                    return;
                }
            }

            trackPoint.setAltitude(egm2008Correction.correctAltitude(trackPoint.getLocation()));
        }
    }
}