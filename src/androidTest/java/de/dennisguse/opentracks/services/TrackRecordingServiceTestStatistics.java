package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Tests resulting TrackStatistics.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTestStatistics {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private TrackRecordingService service;

    private final AltitudeSumManager altitudeSumManager = new AltitudeSumManager() {
        @Override
        public void fill(@NonNull TrackPoint trackPoint) {
            trackPoint.setAltitudeGain(0f);
            trackPoint.setAltitudeLoss(0f);
        }
    };

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    private TrackRecordingService startService() throws TimeoutException {
        Intent startIntent = new Intent(context, TrackRecordingService.class);
        return ((TrackRecordingService.Binder) mServiceRule.bindService(startIntent))
                .getService();
    }

    @Before
    public void setUp() throws TimeoutException {
        contentProviderUtils = new ContentProviderUtils(context);

        service = startService();
        tearDown();
    }

    @After
    public void tearDown() throws TimeoutException {
        TrackRecordingServiceTestUtils.resetService(mServiceRule, context);
        contentProviderUtils.deleteAllTracks(context);
    }

    /**
     * Moving time should increase if the previous and current TrackPoint have speed > threshold by the timeDiff(previousTrackPoint, currentTrackPoint).
     */
    @MediumTest
    @Test
    public void movingtime_with_pauses() throws TimeoutException {
        // given
        service.getTrackPointCreator().stopGPS();

        service.getTrackPointCreator().setClock(Instant.ofEpochMilli(0).toString());
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        Function<Integer, Void> assertMovingTime = expected -> {
            Duration actual = contentProviderUtils.getTrack(trackId).getTrackStatistics().getMovingTime();
            assertEquals(Duration.ofSeconds(expected), actual);
            return null;
        };

        Function<Integer, Void> assertTotalTime = expected -> {
            Duration actual = contentProviderUtils.getTrack(trackId).getTrackStatistics().getTotalTime();
            assertEquals(Duration.ofSeconds(expected), actual);
            return null;
        };

        // when / then
        int movingtime_s = 0;

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 15, 5 * 60000);
        assertMovingTime.apply(movingtime_s);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0001, 35.0, 2, 15, 6 * 60000);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0002, 35.0, 2, 15, (long) (6.5 * 60000));
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0003, 35.0, 2, 15, 7 * 60000);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 15, 8 * 60000);
        movingtime_s += 3 * 60;
        assertMovingTime.apply(movingtime_s);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 0, 9 * 60000); //will be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 0, 10 * 60000); //will be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 0, 11 * 60000);
        assertMovingTime.apply(movingtime_s);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 15, 13 * 60000);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 2, 15, (long) (13.5 * 60000)); //will be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0005, 35.0, 2, 15, 14 * 60000);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0006, 35.0, 2, 15, 15 * 60000);
        movingtime_s += 2 * 60;
        assertMovingTime.apply(movingtime_s);
        assertTotalTime.apply(15 * 60);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0006, 35.0, 2, 0, 16 * 60000); //will be ignored
        assertMovingTime.apply(movingtime_s);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0015, 35.0, 2, 0, 17 * 60000);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0016, 35.0, 2, 15, 18 * 60000);
        assertMovingTime.apply(movingtime_s);

        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0016, 35.0, 2, 0, 19 * 60000); //TODO we could ignore this TrackPoint
        assertMovingTime.apply(movingtime_s);
        assertTotalTime.apply(19 * 60);

        service.getTrackPointCreator().setClock(Instant.ofEpochSecond(40 * 60).toString());
        assertMovingTime.apply(movingtime_s);
        service.pauseCurrentTrack();
        assertTotalTime.apply(40 * 60);

        service.getTrackPointCreator().setClock(Instant.ofEpochSecond(41 * 60).toString());
        service.resumeCurrentTrack();
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0016, 35.0, 2, 15, 42 * 60000);
        assertMovingTime.apply(movingtime_s);
        assertTotalTime.apply(41 * 60);

        service.getTrackPointCreator().setClock(Instant.ofEpochSecond(50 * 60).toString());
        service.endCurrentTrack();
        assertMovingTime.apply(movingtime_s);
        assertTotalTime.apply(49 * 60);

        // then
        assertFalse(service.isRecording());

        TrackStatistics trackStatistics = contentProviderUtils.getTrack(trackId).getTrackStatistics();

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        assertEquals(20, trackPoints.size());

        assertEquals(Duration.ofMinutes(49), trackStatistics.getTotalTime());
    }
}
