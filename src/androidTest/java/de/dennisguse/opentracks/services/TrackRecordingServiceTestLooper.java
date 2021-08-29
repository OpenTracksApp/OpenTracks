package de.dennisguse.opentracks.services;

import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the track recording service, which require a {@link Looper}.
 *
 * @author Bartlomiej Niechwiej
 * <p>
 * ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTestLooper {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    private final Context context = ApplicationProvider.getApplicationContext();
    private final SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);

    private ContentProviderUtils contentProviderUtils;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    @Before
    public void setUp() {
        // Set up the mock content resolver
        ContentProvider customContentProvider = new CustomContentProvider() {
        };
        customContentProvider.attachInfo(context, null);

        contentProviderUtils = new ContentProviderUtils(context);

        // Let's use default values.
        sharedPreferences.edit().clear().apply();

        // Ensure that the database is empty before every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @After
    public void tearDown() throws TimeoutException {
        // Reset service (if some previous test failed)
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(TrackRecordingServiceTest.createStartIntent(context)))
                .getService();
        if (service.isRecording() || service.isPaused()) {
            service.endCurrentTrack();
        }

        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @MediumTest
    @Test
    public void testWithProperties_minRequiredAccuracy() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.recording_gps_accuracy_key), 500);
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_maxRecordingDistanceDefault() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.max_recording_distance_key), Integer.parseInt(context.getResources().getString(R.string.max_recording_distance_default)));
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_maxRecordingDistance() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.max_recording_distance_key), 50);
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingDistanceDefault() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.recording_distance_interval_key), Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_default)));
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingDistance() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.recording_distance_interval_key), 2);
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_metricUnitsDefault() throws TimeoutException {
        PreferencesUtils.setString(sharedPreferences, context, R.string.stats_units_key, context.getString(R.string.stats_units_default));
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_metricUnitsDisabled() throws TimeoutException {
        PreferencesUtils.setString(sharedPreferences, context, R.string.stats_units_key, context.getString(R.string.stats_units_imperial));
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingIntervalDefault() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.min_recording_interval_key), Integer.parseInt(context.getResources().getString(R.string.min_recording_interval_default)));
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingInterval() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.min_recording_interval_key), 2);
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRequiredAccuracyDefault() throws TimeoutException {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.recording_gps_accuracy_key), Integer.parseInt(context.getResources().getString(R.string.recording_gps_accuracy_default)));
        editor.commit();
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testIntegration_completeRecordingSession() throws TimeoutException {
        List<Track> tracks = contentProviderUtils.getTracks();
        assertTrue(tracks.isEmpty());
        fullRecordingSession();
    }

    private void fullRecordingSession() throws TimeoutException {
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(TrackRecordingServiceTest.createStartIntent(context)))
                .getService();
        assertFalse(service.isRecording());

        // Start a track.
        Track.Id trackId = service.startNewTrack();
        assertNotNull(trackId);
        assertTrue(service.isRecording());
        Track track = contentProviderUtils.getTrack(trackId);
        assertNotNull(track);
        assertEquals(trackId, track.getId());

        // Insert a few points, markers and statistics.
        for (int i = 0; i < 30; i++) {
            //TODO Should send locations to LocationHandler instead of TrackPoints to TrackPointCreator?
            TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, service.getTrackPointCreator().createNow())
                    .setLongitude(35.0f + i / 10.0f)
                    .setLatitude(45.0f - i / 5.0f)
                    .setHorizontalAccuracy(Distance.of(5))
                    .setSpeed(Speed.of(10))
                    .setBearing(3.0f);

            Distance prefAccuracy = PreferencesUtils.getThresholdHorizontalAccuracy(sharedPreferences, context);
            service.getTrackPointCreator().onNewTrackPoint(trackPoint, prefAccuracy);

            if (i % 7 == 0) {
                service.insertMarker(null, null, null, null);
            }
        }

        // Stop the track. Validate if it has correct data.
        service.endCurrentTrack();
        assertFalse(service.isRecording());
        track = contentProviderUtils.getTrack(trackId);
        assertNotNull(track);
        assertEquals(trackId, track.getId());
        TrackStatistics trackStatistics = track.getTrackStatistics();
        assertNotNull(trackStatistics);
        assertTrue(trackStatistics.getStartTime().isAfter(Instant.ofEpochMilli(0)));
        assertTrue(trackStatistics.getStopTime().isAfter(trackStatistics.getStartTime()));
    }
}
