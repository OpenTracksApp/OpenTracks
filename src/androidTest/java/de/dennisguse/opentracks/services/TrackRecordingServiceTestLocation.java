package de.dennisguse.opentracks.services;

import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Tests insert location.
 * <p>
 * //TODO ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTestLocation {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private TrackRecordingServiceInterface service;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @Before
    public void setUp() throws TimeoutException {
        // Set up the mock content resolver
        ContentProvider customContentProvider = new CustomContentProvider() {
        };
        customContentProvider.attachInfo(context, null);

        contentProviderUtils = new ContentProviderUtils(context);

        // Let's use default values.
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        sharedPreferences.edit().clear().commit();

        service = ((TrackRecordingServiceInterface) mServiceRule.bindService(TrackRecordingServiceTest.createStartIntent(context)));
        //Disable executorService to not insert locations from GPS via LocationManager
        service.enableLocationExecutor(false);
    }

    @After
    public void tearDown() {
        // Reset service (if some previous test failed)
        service.enableLocationExecutor(true);
        if (service.isRecording() || service.isPaused()) {
            service.endCurrentTrack();
        }

        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_movingAccurate() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0001, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0002, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0003, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0004, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0005, 35.0, 5, 15);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(6, trackPoints.size());
        Assert.assertEquals(45.0005, trackPoints.get(5).getLatitude(), 0.01);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_movingInaccurate() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.1, 35.0, Long.MAX_VALUE, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.2, 35.0, Long.MAX_VALUE, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.3, 35.0, Long.MAX_VALUE, 15);
        TrackRecordingServiceTest.insertLocation(service, 99.0, 35.0, Long.MAX_VALUE, 15);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(1, trackPoints.size());
        Assert.assertEquals(45.0, trackPoints.get(0).getLatitude(), 0.01);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_slowMovingAccurate() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.000001, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.000002, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.000003, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.000004, 35.0, 5, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.000005, 35.0, 5, 15);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(2, trackPoints.size());
        Assert.assertEquals(45.000005, trackPoints.get(1).getLatitude(), 0.01);
    }

//    @MediumTest
//    @Test
//    public void testOnLocationChangedAsync_repeatedTime() throws Exception {
//        // when
//        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 15, 5);
//        TrackRecordingServiceTest.insertLocation(service, 55.0, 35.0, 5, 15, 5);
//        TrackRecordingServiceTest.insertLocation(service, 65.0, 35.0, 5, 15, 5);
//
//        service.endCurrentTrack();
//
//        // then
//        Assert.assertFalse(service.isRecording());
//
//        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
//        Assert.assertEquals(1, trackPoints.size());
//        Assert.assertEquals(45.0, trackPoints.get(0).getLatitude(), 0.01);
//    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 6, 0);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(3, trackPoints.size());
        Assert.assertEquals(1, trackPoints.get(0).getAccuracy(), 0.01);
        Assert.assertEquals(2, trackPoints.get(1).getAccuracy(), 0.01);
        Assert.assertEquals(6, trackPoints.get(2).getAccuracy(), 0.01);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle_withMovement() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 1, 15);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 6, 15);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(4, trackPoints.size());
        Assert.assertEquals(1, trackPoints.get(0).getAccuracy(), 0.01);
        Assert.assertEquals(2, trackPoints.get(1).getAccuracy(), 0.01);
        Assert.assertEquals(5, trackPoints.get(2).getAccuracy(), 0.01); //TODO Check why this trackPoint is inserted.
        Assert.assertEquals(6, trackPoints.get(3).getAccuracy(), 0.01);
    }


    @MediumTest
    @Test
    public void testOnLocationChangedAsync_idle_withSensorData() throws Exception {
        // given
        long trackId = service.startNewTrack();

        service.setRemoteSensorManager(new BluetoothRemoteSensorManager(context) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public SensorDataSet getSensorData() {
                SensorDataSet sensorDataSet = new SensorDataSet();
                sensorDataSet.set(new SensorDataHeartRate("sensorName", "sensorAddress", 5f));
                return sensorDataSet;
            }
        });

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 0, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 2, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 3, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 4, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 5, 0);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(6, trackPoints.size());
        Assert.assertEquals(0, trackPoints.get(0).getAccuracy(), 0.01);
        Assert.assertEquals(1, trackPoints.get(1).getAccuracy(), 0.01);
        Assert.assertEquals(2, trackPoints.get(2).getAccuracy(), 0.01);
        Assert.assertEquals(3, trackPoints.get(3).getAccuracy(), 0.01);
        Assert.assertEquals(4, trackPoints.get(4).getAccuracy(), 0.01);
        Assert.assertEquals(5, trackPoints.get(5).getAccuracy(), 0.01);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_segment() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 1, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.1, 35.0, 2, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.1, 35.0, 3, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.2, 35.0, 4, 0);
        TrackRecordingServiceTest.insertLocation(service, 45.2, 35.0, 5, 0);

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(7, trackPoints.size());
        Assert.assertEquals(1, trackPoints.get(0).getAccuracy(), 0.01);
        Assert.assertEquals(TrackPointsColumns.PAUSE_LATITUDE, trackPoints.get(1).getLatitude(), 0.01);
        Assert.assertEquals(2, trackPoints.get(2).getAccuracy(), 0.01);
        Assert.assertEquals(3, trackPoints.get(3).getAccuracy(), 0.01);
        Assert.assertEquals(TrackPointsColumns.PAUSE_LATITUDE, trackPoints.get(4).getLatitude(), 0.01);
        Assert.assertEquals(4, trackPoints.get(5).getAccuracy(), 0.01);
        Assert.assertEquals(5, trackPoints.get(6).getAccuracy(), 0.01);
    }

    @MediumTest
    @Test
    public void testOnLocationChangedAsync_firstTrackPointInvalid() throws Exception {
        // given
        long trackId = service.startNewTrack();

        // when
        service.insertLocation(TrackPoint.createPause().getLocation());
        TrackRecordingServiceTest.insertLocation(service, 45.0, 35.0, 0, 0);
        service.insertLocation(TrackPoint.createPause().getLocation());

        service.endCurrentTrack();

        // then
        Assert.assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertEquals(1, trackPoints.size());
        Assert.assertEquals(0, trackPoints.get(0).getAccuracy(), 0.01);
    }
}
