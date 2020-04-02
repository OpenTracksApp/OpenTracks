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
import android.location.Location;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.CustomContentProvider;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

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

    private Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private final long trackId = Math.abs(new Random().nextLong());

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
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        sharedPreferences.edit().clear().apply();

        // Ensure that the database is empty before every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @After
    public void tearDown() throws TimeoutException {
        // Reset service (if some previous test failed)
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
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
        Assert.assertNotNull(mServiceRule.bindService(createStartIntent(context)));
    }

    @MediumTest
    @Test
    public void testBindable() throws TimeoutException {
        IBinder service = mServiceRule.bindService(createStartIntent(context));
        Assert.assertNotNull(service);
    }

    @MediumTest
    @Test
    public void testRecording_noTracks() throws Exception {
        List<Track> tracks = contentProviderUtils.getAllTracks();
        Assert.assertTrue(tracks.isEmpty());

        Intent startIntent = createStartIntent(context);
        mServiceRule.startService(startIntent);
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(startIntent));

        // Test if we start in no-recording mode by default.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_oldTracks() throws Exception {
        createDummyTrack(trackId, -1L, false);

        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_orphanedRecordingTrack() throws Exception {
        Intent startIntent = createStartIntent(context);
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(startIntent));

        // Just set recording track to a bogus value.
        // Make sure that the service will not start recording and will clear the bogus track.
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, 123L);

        // then
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testStartNewTrack_alreadyRecording() throws Exception {
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
        service.startNewTrack();
        Assert.assertTrue(service.isRecording());
        long trackId = service.getRecordingTrackId();

        long newTrackId = service.startNewTrack();
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, newTrackId);

        Assert.assertEquals(trackId, PreferencesUtils.getRecordingTrackId(context));
        Assert.assertEquals(trackId, service.getRecordingTrackId());

        service.endCurrentTrack();
    }

    @MediumTest
    @Test
    public void testEndCurrentTrack_noRecording() throws Exception {
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        // Ending the current track when there is no recording should not result in any error.
        service.endCurrentTrack();

        Assert.assertFalse(PreferencesUtils.isRecording(context));
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    // NOTE: Do not use to create a track that is currently recording.
    private void createDummyTrack(long id, long stopTime, boolean isRecording) {
        Track dummyTrack = new Track();
        dummyTrack.setId(id);
        dummyTrack.setName("Dummy Track");
        TrackStatistics trackStatistics = new TrackStatistics();
        trackStatistics.setStopTime_ms(stopTime);
        dummyTrack.setTrackStatistics(trackStatistics);
        addTrack(dummyTrack, isRecording);
    }

    private void addTrack(Track track, boolean isRecording) {
        Assert.assertTrue(track.getId() >= 0);
        contentProviderUtils.insertTrack(track);
        Assert.assertEquals(track.getId(), contentProviderUtils.getTrack(track.getId()).getId());
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, isRecording ? track.getId() : PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        PreferencesUtils.setBoolean(context, R.string.recording_track_paused_key, !isRecording);
    }

    /**
     * Inserts a location and waits for 200ms.
     */
    private void insertLocation(TrackRecordingServiceInterface trackRecordingService) throws InterruptedException {
        Location location = new Location("gps");
        location.setLongitude(35.0f);
        location.setLatitude(45.0f);
        location.setAccuracy(5);
        location.setSpeed(10);
        location.setTime(System.currentTimeMillis());
        location.setBearing(3.0f);
        trackRecordingService.insertLocation(location);

        Thread.sleep(200);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        long waypointId = service.insertWaypoint(null, null, null, null);
        Assert.assertEquals(-1L, waypointId);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_validWaypoint() throws Exception {
        TrackRecordingServiceInterface service = ((TrackRecordingServiceInterface) mServiceRule.bindService(createStartIntent(context)));
        service.startNewTrack();
        Assert.assertTrue(service.isRecording());
        insertLocation(service);

        long trackId = service.getRecordingTrackId();
        long waypointId = service.insertWaypoint(null, null, null, null);
        Assert.assertNotEquals(-1L, waypointId);
        Waypoint wpt = contentProviderUtils.getWaypoint(waypointId);
        Assert.assertEquals(context.getString(R.string.marker_waypoint_icon_url), wpt.getIcon());
        Assert.assertEquals(context.getString(R.string.marker_name_format, 1), wpt.getName());
        Assert.assertEquals(trackId, wpt.getTrackId());
        Assert.assertEquals(0.0, wpt.getLength(), 0.01);
        Assert.assertNotNull(wpt.getLocation());

        service.endCurrentTrack();
    }
}
