package de.dennisguse.opentracks.services;

import android.location.Location;

import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

public class TrackRecordingServiceTestUtils {

    static void sendGPSLocation(TrackPointCreator trackPointCreator, String time, double latitude, double longitude, float accuracy, long speed) {
        Location location = new Location("mock");
        location.setTime(1L); // Should be ignored anyhow.
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);

        trackPointCreator.setClock(time);
        trackPointCreator.getSensorManager().getGpsManager().onLocationChanged(location);
    }
}
