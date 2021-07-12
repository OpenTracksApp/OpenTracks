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

import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Looper;

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

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the track recording service.
 *
 * @author Bartlomiej Niechwiej
 * <p>
 * //TODO ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
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
    private final SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
    private ContentProviderUtils contentProviderUtils;

    private final Track.Id trackId = new Track.Id(Math.abs(new Random().nextLong()));

    static Intent createStartIntent(Context context) {
        return new Intent(context, TrackRecordingService.class);
    }

    @Before
    public void setUp() {
        // Set up the mock content resolver
        ContentProvider customContentProvider = new CustomContentProvider() {
        };
        customContentProvider.attachInfo(context, null);

        contentProviderUtils = new ContentProviderUtils(context);

        // Let's use default values.
        sharedPreferences.edit().clear().commit();

        // Ensure that the database is empty before every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @After
    public void tearDown() throws TimeoutException {
        // Reset service (if some previous test failed)
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        if (service.isRecording() || service.isPaused()) {
            service.endCurrentTrack();
        }

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
    public void testRecording_start() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();

        // when
        Track.Id trackId = service.startNewTrack();

        // then
        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);

        assertEquals(1, trackPoints.size());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(0).getType());
    }

    @MediumTest
    @Test
    public void testRecording_stop() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();

        // when
        service.endCurrentTrack();

        // then
        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);

        assertEquals(2, trackPoints.size());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(0).getType());
        assertEquals(TrackPoint.Type.SEGMENT_END_MANUAL, trackPoints.get(1).getType());
    }

    @MediumTest
    @Test
    public void testRecording_pauseAndResume() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();

        // when
        service.pauseCurrentTrack();

        // then
        assertEquals(2, contentProviderUtils.getTrackPointCursor(trackId, null).getCount());

        //when
        service.resumeTrack(trackId);

        // then
        assertTrue(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        assertEquals(3, trackPoints.size());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(0).getType());
        assertEquals(TrackPoint.Type.SEGMENT_END_MANUAL, trackPoints.get(1).getType());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(2).getType());
    }

    @MediumTest
    @Test
    public void testRecording_resumeStoppedTrack() throws TimeoutException, InterruptedException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();
        assertTrue(service.isRecording());
        service.endCurrentTrack();

        assertEquals(2, contentProviderUtils.getTrackPointCursor(trackId, null).getCount());

        // when
        service.resumeTrack(trackId);
        newTrackPoint(service);

        // then
        assertTrue(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        assertEquals(4, trackPoints.size());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(0).getType());
        assertEquals(TrackPoint.Type.SEGMENT_END_MANUAL, trackPoints.get(1).getType());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(2).getType());
        assertEquals(TrackPoint.Type.TRACKPOINT, trackPoints.get(3).getType());
    }

    @MediumTest
    @Test
    public void testRecording_stopPausedTrack() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();
        assertTrue(service.isRecording());
        service.pauseCurrentTrack();

        assertEquals(2, contentProviderUtils.getTrackPointCursor(trackId, null).getCount());

        // when
        service.endCurrentTrack();

        // then
        assertFalse(service.isRecording());

        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        assertEquals(2, trackPoints.size());
        assertEquals(TrackPoint.Type.SEGMENT_START_MANUAL, trackPoints.get(0).getType());
        assertEquals(TrackPoint.Type.SEGMENT_END_MANUAL, trackPoints.get(1).getType());
    }

    @MediumTest
    @Test
    public void testStartNewTrack_alreadyRecording() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();
        assertTrue(service.isRecording());

        // when
        Track.Id newTrackId = service.startNewTrack();

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
    public void testInsertWaypointMarker_validWaypoint() throws TimeoutException, InterruptedException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(createStartIntent(context)))
                .getService();
        Track.Id trackId = service.startNewTrack();
        assertTrue(service.isRecording());
        newTrackPoint(service);

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

    private static void newTrackPoint(TrackRecordingService trackRecordingService) throws InterruptedException {
        newTrackPoint(trackRecordingService, 45.0f, 35f, 5, 10, System.currentTimeMillis());
    }

    static void newTrackPoint(TrackRecordingService trackRecordingService, double latitude, double longitude, float accuracy, long speed) throws InterruptedException {
        newTrackPoint(trackRecordingService, latitude, longitude, accuracy, speed, System.currentTimeMillis());
    }

    /**
     * Inserts a location and waits for 200ms.
     */
    private static void newTrackPoint(TrackRecordingService trackRecordingService, double latitude, double longitude, float accuracy, long speed, long time) throws InterruptedException {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(time))
                .setLongitude(longitude)
                .setLatitude(latitude)
                .setAccuracy(accuracy)
                .setSpeed(Speed.of(speed))
                .setBearing(3.0f);

        trackRecordingService.getHandlerServer().onNewTrackPoint(trackPoint, 50);
    }
}
