/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.importer.TrackPointAssert;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Tests for the track recording service.
 *
 * @author Bartlomiej Niechwiej
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private final Track.Id trackId = new Track.Id(Math.abs(new Random().nextLong()));

    static Intent createStartIntent(Context context) {
        return new Intent(context, TrackRecordingService.class);
    }

    private final AltitudeSumManager altitudeSumManager = new AltitudeSumManager() {
        @Override
        public void fill(@NonNull TrackPoint trackPoint) {
            trackPoint.setAltitudeGain(0f);
            trackPoint.setAltitudeLoss(0f);
        }
    };

    @Before
    public void setUp() throws TimeoutException {
        contentProviderUtils = new ContentProviderUtils(context);

        tearDown();
    }

    @After
    public void tearDown() throws TimeoutException {
        TrackRecordingServiceTest.resetService(mServiceRule, context);

        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @SmallTest
    @Test
    public void testStartable() throws TimeoutException {
        mServiceRule.startService(createStartIntent(context));
        assertNotNull(mServiceRule.bindService(createStartIntent(context)));
    }

    @MediumTest
    @Test
    public void testBindable() throws TimeoutException {
        IBinder service = mServiceRule.bindService(createStartIntent(context));
        assertNotNull(service);
    }

    @MediumTest
    @Test
    public void testRecording_noTracks() throws TimeoutException {
        // given
        List<Track> tracks = contentProviderUtils.getTracks();
        assertTrue(tracks.isEmpty());

        // when
        Intent startIntent = createStartIntent(context);
        mServiceRule.startService(startIntent);
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(startIntent))
                .getService();

        // then
        // Test if we start in no-recording mode by default.
        assertFalse(service.isRecording());
    }

    @MediumTest
    @Test
    public void testRecording_oldTracks() throws TimeoutException {
        // given
        createDummyTrack(trackId);

        // when
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        // then
        assertFalse(service.isRecording());
    }

    @MediumTest
    @Test
    public void testRecording_serviceRestart_whileRecording() throws TimeoutException {
        // given
        createDummyTrack(trackId);

        //when
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        service.resumeTrack(trackId);

        // then
        assertTrue(service.isRecording());
    }

    @MediumTest
    @Test
    public void testRecording_stop() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        // when
        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:03Z"), ZoneId.of("CET")));
        service.endCurrentTrack();

        // then
        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);

        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:02Z")),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:02:03Z"))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testRecording_pauseAndResume() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        // when
        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:03Z"), ZoneId.of("CET")));
        service.pauseCurrentTrack();
        service.stopUpdateRecordingData();

        // then
        assertEquals(2, contentProviderUtils.getTrackPointCursor(trackId, null).getCount());

        //when
        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:04Z"), ZoneId.of("CET")));
        service.resumeTrack(trackId);
        service.stopUpdateRecordingData();

        // then
        assertTrue(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:02Z")),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:02:03Z"))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:04Z"))
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testRecording_resumeStoppedTrack() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:03Z"), ZoneId.of("CET")));
        service.endCurrentTrack();

        // when
        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:04Z"), ZoneId.of("CET")));
        service.resumeTrack(trackId);
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        trackPointCreator.onNewTrackPoint(new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:05Z")));

        // then
        assertTrue(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:02Z")),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:02:03Z"))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f),
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:04Z")),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:05Z"))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testRecording_stopPausedTrack() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();
        trackPointCreator.stopGPS();
        trackPointCreator.setAltitudeSumManager(altitudeSumManager);

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:03Z"), ZoneId.of("CET")));
        service.pauseCurrentTrack();

        // when
        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:04Z"), ZoneId.of("CET")));
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        TrackPointAssert a = new TrackPointAssert();
        a.assertEquals(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.parse("2020-02-02T02:02:02Z")),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.parse("2020-02-02T02:02:03Z"))
                        .setAltitudeGain(0f)
                        .setAltitudeLoss(0f)
        ), trackPoints);
    }

    @MediumTest
    @Test
    public void testStartNewTrack_alreadyRecording() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();

        assertTrue(service.isRecording());

        // when
        Track.Id newTrackId = service.startNewTrack();
        service.stopUpdateRecordingData();

        // then
        assertNotNull(trackId);
        assertNull(newTrackId);
    }

    @MediumTest
    @Test
    public void testEndCurrentTrack_noRecording() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        assertFalse(service.isRecording());

        // when
        // Ending the current track when there is no recording should not result in any error.
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_noRecordingTrack() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        assertFalse(service.isRecording());

        // when
        Marker.Id markerId = service.insertMarker(null, null, null, null);

        // then
        assertNull(markerId);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_validWaypoint() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        TrackPointCreator trackPointCreator = service.getTrackPointCreator();
        trackPointCreator.stopGPS();

        trackPointCreator.setClock(Clock.fixed(Instant.parse("2020-02-02T02:02:02Z"), ZoneId.of("CET")));
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();

        assertTrue(service.isRecording());
        trackPointCreator.onNewTrackPoint(
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:03Z"))
                        .setLatitude(10)
                        .setLongitude(10)
        );

        // when
        Marker.Id markerId = service.insertMarker(null, null, null, null);

        // then
        assertNotEquals(new Marker.Id(-1L), markerId);
        Marker wpt = contentProviderUtils.getMarker(markerId);
        assertEquals(context.getString(R.string.marker_icon_url), wpt.getIcon());
        assertEquals(context.getString(R.string.marker_name_format, 1), wpt.getName());
        assertEquals(trackId, wpt.getTrackId());
        assertEquals(0.0, wpt.getLength().toM(), 0.01);
        assertNotNull(wpt.getLocation());

        service.endCurrentTrack();
    }

    private void addTrack(Track track) {
        assertNotNull(track.getId());
        contentProviderUtils.insertTrack(track);
        assertEquals(track.getId(), contentProviderUtils.getTrack(track.getId()).getId());
    }

    // NOTE: Do not use to create a track that is currently recording.
    private void createDummyTrack(Track.Id id) {
        Track dummyTrack = new Track();
        dummyTrack.setId(id);
        dummyTrack.setName("Dummy Track");
        TrackStatistics trackStatistics = new TrackStatistics();
        Instant now = Instant.now();
        trackStatistics.setStartTime(now.minusSeconds(5L));
        trackStatistics.setStopTime(now.minusSeconds(1L));
        dummyTrack.setTrackStatistics(trackStatistics);
        addTrack(dummyTrack);
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

    //TODO Workaround as service is not stopped on API23; thus sharedpreferences are not reset between tests.
    //TODO Anyhow, the service should re-create all it's resources if a recording starts and makes sure that there is no leftovers from previous recordings.
    @Deprecated
    public static void resetService(ServiceTestRule mServiceRule, Context context) throws TimeoutException {
        // Let's use default values.
        PreferencesUtils.clear();

        // Reset service (if some previous test failed)
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();
        service.endCurrentTrack();
        service.getTrackPointCreator().setClock(Clock.systemUTC());
        service.sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
    }
}
