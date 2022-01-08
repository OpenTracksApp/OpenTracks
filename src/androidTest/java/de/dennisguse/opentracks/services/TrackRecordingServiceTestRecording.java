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

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.content.sensor.SensorDataRunning;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.importer.TrackPointAssert;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Tests insert location.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTestRecording {

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
        tearDown();

        service = startService();
        service.getTrackPointCreator().stopGPS();
    }

    @After
    public void tearDown() throws TimeoutException {
        TrackRecordingServiceTestUtils.resetService(mServiceRule, context);
        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
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
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

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
        assertEquals(new TrackStatistics(startTime, stopTime, 0, 1, 0, 0, 0f, 0f)
                , contentProviderUtils.getTrack(trackId).getTrackStatistics());

        new TrackPointAssert().assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(startTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(stopTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
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
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        // when
        String pauseTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(pauseTime);
        service.pauseCurrentTrack();

        // then
        assertEquals(new TrackStatistics(startTime, pauseTime, 0, 1, 0, 0, 0f, 0f)
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
        assertEquals(new TrackStatistics(startTime, resumeTime, 0, 1, 0, 0, 0f, 0f)
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
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        String pauseTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(pauseTime);
        service.pauseCurrentTrack();

        // when
        trackPointCreator.setClock("2020-02-02T02:02:04Z");
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse(starTime)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse(pauseTime))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testRecording_startStopResumeStopped() {
        // given
        String startTime = "2020-02-02T02:02:02Z";
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(startTime);
        Track.Id trackId = service.startNewTrack();
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        String stopTime = "2020-02-02T02:02:03Z";
        trackPointCreator.setClock(stopTime);
        service.endCurrentTrack();

        // when
        String resumeTime = "2020-02-02T02:02:04Z";
        trackPointCreator.setClock(resumeTime);
        service.resumeTrack(trackId);
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

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
    public void testOnLocationChangedAsync_movingAccurate() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0001, 35.0, 2, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0002, 35.0, 3, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0003, 35.0, 4, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0004, 35.0, 5, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0005, 35.0, 6, 15);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0001)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(2))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0001)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(3))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0002)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(4))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0003)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(5))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0004)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(6))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_slowMovingAccurate() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.000001, 35.0, 2, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.000002, 35.0, 3, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.000003, 35.0, 4, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.000004, 35.0, 5, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.000005, 35.0, 6, 15);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.0005)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(6))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 6, 0);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(6))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle_withMovement() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);
        service.getTrackPointCreator().stopGPS();

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 3, 0); // will be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 4, 0); // will be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 6, 15);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(2))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(5))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(6))
                        .setSpeed(Speed.of(15))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle_withSensorData() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);
        service.getTrackPointCreator().setRemoteSensorManager(new BluetoothRemoteSensorManager(context, service.getTrackPointCreator()) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public SensorDataSet fill(@NonNull TrackPoint trackPoint) {
                SensorDataSet sensorDataSet = new SensorDataSet();
                sensorDataSet.set(new SensorDataHeartRate("sensorName", "sensorAddress", 5f));
                sensorDataSet.fillTrackPoint(trackPoint);
                return sensorDataSet;
            }
        });

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 6, 0);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(6))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle_withSensorDistance() {
        BluetoothRemoteSensorManager remoteSensorManager = new BluetoothRemoteSensorManager(context, service.getTrackPointCreator()) {

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        AltitudeSumManager altitudeSumManager = new AltitudeSumManager();

        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setRemoteSensorManager(remoteSensorManager);
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);
        altitudeSumManager.stop(service);
        altitudeSumManager.setConnected(true);

        // when
        altitudeSumManager.addAltitudeGain_m(6f);
        altitudeSumManager.addAltitudeLoss_m(6f);
        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(0))); //Should be ignored
        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(2))); //TODO Should be ignored; distance will be added to TrackPoint
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 15);

        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(12)));

        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(13))); //Should be ignored

        altitudeSumManager.addAltitudeGain_m(6f);
        altitudeSumManager.addAltitudeLoss_m(6f);
        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(14))); //Should be ignored
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 4, 15); //Should be ignored

        altitudeSumManager.addAltitudeGain_m(6f);
        altitudeSumManager.addAltitudeLoss_m(6f);
        remoteSensorManager.onChanged(new SensorDataRunning("", "", Speed.of(5), null, Distance.of(16)));

        altitudeSumManager.addAltitudeGain_m(7f);
        altitudeSumManager.addAltitudeLoss_m(7f);
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.SENSORPOINT, null) // TODO Should be ignored; is stored as it assumed to be first in current segment.
                        .setAltitudeGain(6f)
                        .setAltitudeLoss(6f)
                        .setSpeed(Speed.of(5))
                        .setSensorDistance(Distance.of(2)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(5))
                        .setSensorDistance(Distance.of(0)),
                new TrackPoint(TrackPoint.Type.SENSORPOINT, null)
                        .setSpeed(Speed.of(5))
                        .setSensorDistance(Distance.of(10)),
                new TrackPoint(TrackPoint.Type.SENSORPOINT, null)
                        .setSpeed(Speed.of(5))
                        .setAltitudeGain(12f)
                        .setAltitudeLoss(12f)
                        .setSensorDistance(Distance.of(4)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setSensorDistance(Distance.of(11))
                        .setSpeed(Speed.of(5))
                        .setAltitudeGain(7f)
                        .setAltitudeLoss(7f)
                        .setSensorDistance(Distance.of(0))
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_segment() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.1, 35.0, 2, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.1, 35.0, 3, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.2, 35.0, 4, 0);
        TrackRecordingServiceTestUtils.newTrackPoint(service, 45.2, 35.0, 5, 0);

        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);

        TrackPointAssert a = new TrackPointAssert()
                .ignoreTime();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, null),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(1))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),

                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, null)
                        .setLatitude(45.1)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(2))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.1)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(3))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),

                new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, null)
                        .setLatitude(45.2)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(4))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45.2)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(5))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, null)
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
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
