package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.core.location.LocationCompat;
import androidx.core.location.altitude.AltitudeConverterCompat;

import java.io.IOException;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.TrackPoint;

/**
 * More infos regarding Android 34's <a href="https://issuetracker.google.com/issues/195660815#comment1">AltitudeConverter</a>.
 */
public class AltitudeCorrectionManager {

    private static final String TAG = AltitudeCorrectionManager.class.getSimpleName();

    public void correctAltitude(Context context, TrackPoint trackPoint) {
        if (!trackPoint.hasLocation() || !trackPoint.hasAltitude()) {
            return;
        }

        try {
            Location loc = trackPoint.getLocation();
            AltitudeConverterCompat.addMslAltitudeToLocation(context, loc);
            trackPoint.setAltitude(Altitude.EGM2008.of(LocationCompat.getMslAltitudeMeters(loc)));
        } catch (IOException e) {
            Log.w(TAG, "Android's AltitudeConverterCompat failed with " + e.getMessage());
        }
    }
}