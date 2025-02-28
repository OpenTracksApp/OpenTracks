package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

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

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.AltitudeGainLoss;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.importer.TrackPointAssert;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;
import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorHeartRate;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorRunning;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Tests insert location.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceRecordingTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private TrackRecordingService service;

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

        PreferencesUtils.setString(R.string.recording_distance_interval_key, R.string.recording_distance_interval_default);
        PreferencesUtils.setString(R.string.idle_duration_key, R.string.idle_duration_default);

        service = startService();
    }

    @MediumTest
    @Test
    public void recording_startStop() {
        // given
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        // when
        String startTime = "2020-02-02T02:02:02Z";
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);


        // then
        assertEquals(new TrackStatistics(startTime, startTime, 0, 0, 0, 0, null, null)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));


        // when
        String stopTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, stopTime, 0, 1, 1, 0, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }


    /**
     * Test that an IDLE event, doesn't store invalid GPS-provided data.
     */
    @MediumTest
    @Test
    public void recording_startIdle() throws InterruptedException {
        // given
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        String startTime = "2020-02-02T02:02:02Z";
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);
        String gps2 = "2020-02-02T02:02:04Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.0, 35.0, 1, 15);

        // when
        String idleTime = "2020-02-02T02:02:17Z";
        trackPointCreator.setClock(idleTime);
        Thread.sleep(Duration.ofSeconds(15).toMillis());

        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15))),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps2),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15))),
                new TrackPoint(TrackPoint.Type.IDLE, Instant.parse(idleTime))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_startPauseResume() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);


        // when
        String pauseTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(pauseTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, pauseTime, 0, 1, 1, 0, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(pauseTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));

        //when
        String resumeTime = "2020-02-02T02:02:04Z";
        trackPointCreator.setClock(resumeTime);
        service.resumeTrack(trackId);

        // then
        assertEquals(new TrackStatistics(startTime, resumeTime, 0, 1, 1, 0, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(pauseTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(resumeTime))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }


    @MediumTest
    @Test
    public void testRecording_startPauseStop() {
        // given
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        String starTime = "2020-02-02T02:02:02Z";
        trackPointCreator.setClock(starTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);


        String pauseTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(pauseTime);
        service.endCurrentTrack();

        // when
        trackPointCreator.setClock("2020-02-02T02:02:04Z");
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(starTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(pauseTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testRecording_startStopResumeStop() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);

        String stopTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // when
        String resumeTime = "2020-02-02T02:02:04Z";
        trackPointCreator.setClock(resumeTime);
        service.resumeTrack(trackId);
        mockAltitudeChange(trackPointCreator, 0);

        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(resumeTime))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_blesensor_only_no_distance() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);

        SensorManager sensorManager = trackPointCreator.getSensorManager();
        sensorManager.sensorDataSet.add(new AggregatorHeartRate("", ""));
        // when
        String sensor1 = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(sensor1);

        sensorManager.onChanged(new Raw<>(sensor1, HeartRate.of(5))); //Should be ignored

        String sensor3 = "2020-02-02T02:02:13Z";
        trackPointCreator.setClock(sensor3);
        sensorManager.onChanged(new Raw<>(sensor3, HeartRate.of(7)));

        String stopTime = "2020-02-02T02:02:15Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse(sensor3))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate(HeartRate.of(7)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate(HeartRate.of(7))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_gpsOnly_recordingDistance_above() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);


        // when
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);

        // then
        assertEquals(new TrackStatistics(startTime, gps1, 0, 1, 1, 15, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String gps2 = "2020-02-02T02:02:06Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.0001, 35.0, 1, 15);

        // then
        assertEquals(new TrackStatistics(startTime, gps2, 11.113178253173828f, 4, 4, 15, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String gps3 = "2020-02-02T02:02:08Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps3, 45.0002, 35.0, 1, 15);

        // then
        assertEquals(new TrackStatistics(startTime, gps3, 22.226356506347656, 6, 6, 15, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());


        // when
        String stopTime = "2020-02-02T02:02:12Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, stopTime, 22.226356506347656, 10, 10, 15, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps2),
                                45.001, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps3),
                                45.001, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_gpsOnly_recordingDistance_below() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);

        // when
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);

        // then
        TrackStatistics gps1Statistics = new TrackStatistics(startTime, gps1, 0, 1, 1, 15, 0f, 0f);
        assertEquals(gps1Statistics, contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String gps2 = "2020-02-02T02:02:06Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.00001, 35.0, 1, 15);

        // then
        assertEquals(gps1Statistics, contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String gps3 = "2020-02-02T02:02:08Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps3, 45.00002, 35.0, 1, 15);

        // then
        assertEquals(gps1Statistics, contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String stopTime = "2020-02-02T02:02:12Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, stopTime, 2.222635507583618, 10, 10, 15, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps3),
                                45.00002, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,
                        new Position(
                                Instant.parse(stopTime),
                                45.00002, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_gpsOnly_recordingDistance_movement_non_idle() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);


        // when
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);

        // when - will be ignored
        String gps2 = "2020-02-02T02:02:04Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.0, 35.0, 1, 15);

        // when
        String gps3 = "2020-02-02T02:02:05Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps3, 45.0001, 35.0, 1, 15);

        // when
        String stopTime = "2020-02-02T02:02:12Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps3),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_gpsOnly_ignore_inaccurate() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);

        // when
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 100, 15);

        // then
        TrackStatistics startStatistics = new TrackStatistics(startTime, startTime, 0, 0, 0, 0, null, null);
        assertEquals(startStatistics, contentProviderUtils.getTrack(trackId).getTrackStatistics());


        // when
        String gps2 = "2020-02-02T02:02:06Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.1, 35.0, 100, 15);

        // then
        assertEquals(startStatistics, contentProviderUtils.getTrack(trackId).getTrackStatistics());


        // when
        String stopTime = "2020-02-02T02:02:12Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, stopTime, 0, 10, 10, 0, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());


        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    @MediumTest
    @Test
    public void testRecording_gpsOnly_segment() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        mockAltitudeChange(trackPointCreator, 0);

        // when
        String gps1 = "2020-02-02T02:02:03Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);

        // then
        assertEquals(new TrackStatistics(startTime, gps1, 0, 1, 1, 15, 0f, 0f), contentProviderUtils.getTrack(trackId).getTrackStatistics());

        // when
        String gps2 = "2020-02-02T02:02:06Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.1, 35.0, 1, 15);

        // then
        assertEquals(new TrackStatistics(startTime, gps2, 11113.275390625, 4, 4, 2778.31884765625f, 0f, 0f).toString(), contentProviderUtils.getTrack(trackId).getTrackStatistics().toString());


        // when
        String stopTime = "2020-02-02T02:02:12Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, stopTime, 11113.275390625, 10, 10, 1111.3275390625f, 0f, 0f).toString(), contentProviderUtils.getTrack(trackId).getTrackStatistics().toString());


        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC,
                        new Position(
                                Instant.parse(gps2),
                                45.1, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(15)))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    /**
     * Make sure that GPS-based TrackPoints are stored, if the distance to the previous GPS-based TrackPoint is greater than recordingDistanceInterval.
     */
    @MediumTest
    @Test
    public void testRecording_gpsAndSensor_gpsIdleMoving_sensorMoving() {
        // TODO Check TrackStatistics
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();

        SensorManager sensorManager = trackPointCreator.getSensorManager();
        sensorManager.sensorDataSet.add(new AggregatorRunning("", ""));
        sensorManager.sensorDataSet.barometer = null;

        // when
        String sensor1 = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(sensor1);
        sensorManager.onChanged(new Raw<>(sensor1, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(5), null, Distance.of(0)))); //Should be ignored

        // when
        String sensor2 = "2020-02-02T02:02:04Z";
        trackPointCreator.setClock(sensor2);
        sensorManager.onChanged(new Raw<>(sensor2, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(5), null, Distance.of(2))));

        // when
        String gps1 = "2020-02-02T02:02:05Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps1, 45.0, 35.0, 1, 15);

        // when
        String sensor3 = "2020-02-02T02:02:06Z";
        trackPointCreator.setClock(sensor3);
        sensorManager.onChanged(new Raw<>(sensor3, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(5), null, Distance.of(12))));

        // when
        String sensor4 = "2020-02-02T02:02:07Z";
        trackPointCreator.setClock(sensor4);
        sensorManager.onChanged(new Raw<>(sensor4, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(5), null, Distance.of(14)))); //Should be ignored

        // when
        String gps2 = "2020-02-02T02:02:08Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps2, 45.0, 35.0, 4, 15); //Should be ignored

        // when
        String sensor5 = "2020-02-02T02:02:10Z";
        trackPointCreator.setClock(sensor5);
        sensorManager.onChanged(new Raw<>(sensor5, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(5), null, Distance.of(16)))); //Should be ignored

        // when
        String gps3 = "2020-02-02T02:02:12Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps3, 45.001, 35.0, 1, 15);

        // when
        String gps4 = "2020-02-02T02:02:14Z";
        TrackRecordingServiceTestUtils.sendGPSLocation(trackPointCreator, gps4, 45.001, 35.0, 1, 15);


        // when
        String stopTime = "2020-02-02T02:02:16Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // then
        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse(sensor2)) //First moving TrackPoint: store as the time might be interesting.
                        .setSpeed(Speed.of(5))
                        .setSensorDistance(Distance.of(2)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps1),
                                45d, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(5)))
                        .setSensorDistance(Distance.of(0)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse(sensor3))
                        .setSpeed(Speed.of(5))
                        .setSensorDistance(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps3),
                                45.001, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(5)))
                        .setSensorDistance(Distance.of(4.0)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT,
                        new Position(
                                Instant.parse(gps4),
                                45.001, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(5)))
                        .setSensorDistance(Distance.of(0)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL,
                        new Position(
                                Instant.parse(stopTime),
                                45.001, 35d, Distance.of(1),
                                null, null,
                                null,
                                Speed.of(0))) //Sensor data is now outdated, but we do not fall back to GPS.
                        .setSensorDistance(Distance.of(0))
        ), TestDataUtil.getTrackPoints(contentProviderUtils, trackId));
    }

    private void mockAltitudeChange(TrackPointCreator trackPointCreator, float altitudeGain) {
        trackPointCreator.getSensorManager().sensorDataSet.barometer = new AggregatorBarometer("", "") {
            @Override
            public boolean hasReceivedData() {
                return true;
            }

            @Override
            public AltitudeGainLoss getAggregatedValue(Instant now) {
                return new AltitudeGainLoss(altitudeGain, altitudeGain);
            }
        };
    }
}
