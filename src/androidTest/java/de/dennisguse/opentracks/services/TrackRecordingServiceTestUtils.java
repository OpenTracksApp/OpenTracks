package de.dennisguse.opentracks.services;

import android.content.Context;
import android.location.Location;

import androidx.test.rule.ServiceTestRule;

import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class TrackRecordingServiceTestUtils {


    //TODO Workaround as service is not stopped on API23; thus sharedpreferences are not reset between tests.
    //TODO Anyhow, the service should re-create all it's resources if a recording starts and makes sure that there is no leftovers from previous recordings.
    @Deprecated
    public static void resetService(ServiceTestRule mServiceRule, Context context) throws TimeoutException {
        // Let's use default values.
        PreferencesUtils.clear();
    }

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
