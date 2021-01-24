package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LocationHandlerTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Mock
    private HandlerServer handlerServer;

    @InjectMocks
    private LocationHandler locationHandler;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @Before
    public void setUp() {
        // Let's use default values.
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        sharedPreferences.edit().clear().commit();

        locationHandler.onSharedPreferenceChanged(context, PreferencesUtils.getSharedPreferences(context), context.getString(R.string.recording_gps_accuracy_key));
        locationHandler.onSharedPreferenceChanged(context, PreferencesUtils.getSharedPreferences(context), context.getString(R.string.min_recording_interval_key));
        locationHandler.onStart(context);
    }

    /**
     * When a valid location changed in LocationHandler -> newTrackPoint service method is called.
     */
    @Test
    public void testOnLocationChanged_okay() {
        // when
        locationHandler.onLocationChanged(createLocation(45f, 35f, 3, 5, System.currentTimeMillis()));

        // then
        verify(handlerServer, times(1)).sendTrackPoint(any(TrackPoint.class), any(Integer.class));
    }

    /**
     * When location changed in LocationHandler with bad location -> newTrackPoint service method is not called.
     */
    @Test
    public void testOnLocationChanged_badLocation() {
        // given
        float latitude = 91f;

        // when
        // bad latitude
        locationHandler.onLocationChanged(createLocation(latitude, 35f, 3, 5, System.currentTimeMillis()));

        // then
        verify(handlerServer, times(0)).sendTrackPoint(any(TrackPoint.class), any(Integer.class));
    }

    /**
     * When location changed in LocationHandler with poor accuracy -> newTrackPoint service method is not called.
     */
    @Test
    public void testOnLocationChanged_poorAccuracy() {
        // given
        int prefAccuracy = PreferencesUtils.getRecordingGPSAccuracy(context);

        // when
        locationHandler.onLocationChanged(createLocation(45f, 35f, prefAccuracy + 1, 5, System.currentTimeMillis()));

        // then
        // no newTrackPoint called
        verify(handlerServer, times(0)).sendTrackPoint(any(TrackPoint.class), any(Integer.class));
    }

    @Test
    public void testOnLocationChanged_movingInaccurate() {
        // when
        locationHandler.onLocationChanged(createLocation(45.0, 35.0, 5, 15, System.currentTimeMillis()));
        locationHandler.onLocationChanged(createLocation(45.1, 35.0, Long.MAX_VALUE, 15, System.currentTimeMillis()));
        locationHandler.onLocationChanged(createLocation(45.2, 35.0, Long.MAX_VALUE, 15, System.currentTimeMillis()));
        locationHandler.onLocationChanged(createLocation(45.3, 35.0, Long.MAX_VALUE, 15, System.currentTimeMillis()));
        locationHandler.onLocationChanged(createLocation(99.0, 35.0, Long.MAX_VALUE, 15, System.currentTimeMillis()));

        // then
        verify(handlerServer, times(1)).sendTrackPoint(any(TrackPoint.class), any(Integer.class));
    }

    /**
     * Creates a location with parameters and returns the Location object.
     */
    private static Location createLocation(double latitude, double longitude, float accuracy, long speed, long time) {
        Location location = new Location("gps");
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        location.setTime(time);
        location.setBearing(3.0f);
        return location;
    }
}