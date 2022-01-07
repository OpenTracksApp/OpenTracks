package de.dennisguse.opentracks.services;

import android.content.Context;
import android.content.Intent;

import androidx.test.rule.ServiceTestRule;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class TrackRecordingServiceTestUtils {


    //TODO Workaround as service is not stopped on API23; thus sharedpreferences are not reset between tests.
    //TODO Anyhow, the service should re-create all it's resources if a recording starts and makes sure that there is no leftovers from previous recordings.
    @Deprecated
    public static void resetService(ServiceTestRule mServiceRule, Context context) throws TimeoutException {
        // Let's use default values.
        PreferencesUtils.clear();

        // Reset service (if some previous test failed)
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();

        service.getTrackPointCreator().setRemoteSensorManager(new BluetoothRemoteSensorManager(context, service.getTrackPointCreator()));
        service.getTrackPointCreator().setClock(Clock.systemUTC());
        service.endCurrentTrack();
        service.sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
    }

    static void newTrackPoint(TrackRecordingService trackRecordingService, double latitude, double longitude, float accuracy, long speed) {
        newTrackPoint(trackRecordingService, latitude, longitude, accuracy, speed, System.currentTimeMillis());
    }

    static void newTrackPoint(TrackRecordingService trackRecordingService, double latitude, double longitude, float accuracy, long speed, long time) {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(time))
                .setLongitude(longitude)
                .setLatitude(latitude)
                .setHorizontalAccuracy(Distance.of(accuracy))
                .setSpeed(Speed.of(speed))
                .setBearing(3.0f);

        trackRecordingService.getTrackPointCreator().onNewTrackPoint(trackPoint);
    }
}
