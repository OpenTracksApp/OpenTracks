package de.dennisguse.opentracks.services;

import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.importer.TrackPointAssert;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.junit.Assert.assertFalse;

/**
 * Tests insert location.
 * <p>
 * //TODO ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
 */
@RunWith(AndroidJUnit4.class)
//TODO Implement as mock test; no need to store data in database
public class TrackRecordingServiceTestLocation {

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

    @Before
    public void setUp() throws TimeoutException {
        // Set up the mock content resolver
        ContentProvider customContentProvider = new CustomContentProvider() {
        };
        customContentProvider.attachInfo(context, null);

        contentProviderUtils = new ContentProviderUtils(context);
        tearDown();

        // Let's use default values.
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        sharedPreferences.edit().clear().commit();

        service = ((TrackRecordingService.Binder) mServiceRule.bindService(TrackRecordingServiceTest.createStartIntent(context)))
                .getService();
        service.getTrackPointCreator().stopGPS();
    }

    @After
    public void tearDown() {
        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_movingAccurate() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0001, 35.0, 2, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0002, 35.0, 3, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0003, 35.0, 4, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0004, 35.0, 5, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0005, 35.0, 6, 15);

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
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.000001, 35.0, 2, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.000002, 35.0, 3, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.000003, 35.0, 4, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.000004, 35.0, 5, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.000005, 35.0, 6, 15);

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
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 6, 0);

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
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null) //TODO Why is this added? Systems is idle and not moving at all.
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(2))
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
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);
        service.getTrackPointCreator().stopGPS();

        // when
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 6, 15);

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
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null) //TODO Check why this trackPoint is inserted.
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(2))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null) //TODO Check why this trackPoint is inserted.
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
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);
        service.getTrackPointCreator().setRemoteSensorManager(new BluetoothRemoteSensorManager(context) {

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
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 6, 0);

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
                        .setHorizontalAccuracy(Distance.of(2))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(3))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(4))
                        .setSpeed(Speed.of(0))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
                        .setHeartRate_bpm(5f),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, null)
                        .setLatitude(45)
                        .setLongitude(35)
                        .setHorizontalAccuracy(Distance.of(5))
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
    public void testOnLocationChangedAsync_segment() {
        // given
        Track.Id trackId = service.startNewTrack();
        service.getTrackPointCreator().setAltitudeSumManager(altitudeSumManager);

        // when
        TrackRecordingServiceTest.newTrackPoint(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.1, 35.0, 2, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.1, 35.0, 3, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.2, 35.0, 4, 0);
        TrackRecordingServiceTest.newTrackPoint(service, 45.2, 35.0, 5, 0);

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
}
