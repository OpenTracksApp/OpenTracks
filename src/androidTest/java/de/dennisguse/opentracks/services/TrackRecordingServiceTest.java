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
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.CustomContentProvider;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.content.Waypoint.WaypointType;
import de.dennisguse.opentracks.content.WaypointCreationRequest;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Tests for the track recording service.
 *
 * @author Bartlomiej Niechwiej
 * <p>
 * ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    private Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils providerUtils;

    private long trackId = Math.abs(new Random().nextLong());

    static Intent createStartIntent(Context context) {
        return new Intent(context, TrackRecordingService.class);
    }

    static void updateAutoResumePrefs(Context context, int attempts, int timeoutMins) {
        PreferencesUtils.setInt(context, R.string.auto_resume_track_current_retry_key, attempts);
        PreferencesUtils.setString(context, R.string.auto_resume_track_timeout_key, "" + timeoutMins);
    }

    @Before
    public void setUp() {
        // Set up the mock content resolver
        ContentProvider customContentProvider = new CustomContentProvider() {
        };
        customContentProvider.attachInfo(context, null);

        providerUtils = ContentProviderUtils.Factory.get(context);

        // Let's use default values.
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        sharedPreferences.edit().clear().apply();

        // Disable auto resume by default.
        updateAutoResumePrefs(context, PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, 0);

        // Ensure that the database is empty before every test
        providerUtils.deleteAllTracks(context);
    }

    @After
    public void tearDown() throws TimeoutException {
        // Reset service (if some previous test failed)
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        if (service.isRecording() || service.isPaused()) {
            service.endCurrentTrack();
        }

        // Ensure that the database is empty after every test
        providerUtils.deleteAllTracks(context);
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
    public void testResumeAfterReboot_shouldResume() throws Exception {
        // Insert a dummy track and mark it as recording track.
        createDummyTrack(trackId, System.currentTimeMillis(), true);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(context, PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent(context);
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

        // then
        Assert.assertNotNull(service);

        // We expect to resume the previous track.
        Assert.assertTrue(service.isRecording());
        Assert.assertEquals(trackId, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_simulateReboot() throws Exception {
        updateAutoResumePrefs(context, PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        // Simulate recording a track.
        long id = service.startNewTrack();
        Assert.assertTrue(service.isRecording());
        Assert.assertEquals(id, service.getRecordingTrackId());
        mServiceRule.unbindService();
        Assert.assertEquals(id, PreferencesUtils.getRecordingTrackId(context));

        // Start the service in "resume" mode (simulates the on-reboot action).

        Intent startIntent = createStartIntent(context);
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));

        // then
        Assert.assertNotNull(service);
        Assert.assertTrue(service.isRecording());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_noRecordingTrack() throws Exception {
        // Insert a dummy track and mark it as recording track.
        createDummyTrack(trackId, System.currentTimeMillis(), false);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(context, PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent(context);
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

        // then
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because it was stopped.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_expiredTrack() throws Exception {
        // Insert a dummy track last updated 20 min ago.
        createDummyTrack(trackId, System.currentTimeMillis() - 1500 * 60 * 1000, true);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(context, PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent(context);
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

        // then
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because it has expired.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_tooManyAttempts() throws Exception {
        // Insert a dummy track.
        createDummyTrack(trackId, System.currentTimeMillis(), true);

        // Set the number of attempts to max.
        updateAutoResumePrefs(context, TrackRecordingService.MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent(context);
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

        //then
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because there were already too many attempts.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_noTracks() throws Exception {
        List<Track> tracks = providerUtils.getAllTracks();
        Assert.assertTrue(tracks.isEmpty());

        Intent startIntent = createStartIntent(context);
        mServiceRule.startService(startIntent);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

        // Test if we start in no-recording mode by default.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_oldTracks() throws Exception {
        createDummyTrack(trackId, -1L, false);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_orphanedRecordingTrack() throws Exception {
        Intent startIntent = createStartIntent(context);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));

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
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertTrue(service.isRecording());

        long newTrackId = service.startNewTrack();
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, newTrackId);

        Assert.assertEquals(trackId, PreferencesUtils.getRecordingTrackId(context));
        Assert.assertEquals(trackId, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testEndCurrentTrack_noRecording() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        // Ending the current track when there is no recording should not result in any error.
        service.endCurrentTrack();

        Assert.assertFalse(PreferencesUtils.isRecording(context));
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testInsertStatisticsMarker_noRecordingTrack() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        Assert.assertEquals(-1L, waypointId);
    }

    @MediumTest
    @Test
    public void testInsertStatisticsMarker_validLocation() throws Exception {
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertTrue(service.isRecording());
        Assert.assertFalse(service.isPaused());
        insertLocation(service);

        long waypointId1 = service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        Assert.assertNotEquals(-1L, waypointId1);
        long waypointId2 = service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        Assert.assertNotEquals(-1L, waypointId2);

        Waypoint wpt = providerUtils.getWaypoint(waypointId1);
        Assert.assertEquals(context.getString(R.string.marker_statistics_icon_url), wpt.getIcon());
        Assert.assertEquals(context.getString(R.string.marker_split_name_format, 0), wpt.getName());
        Assert.assertEquals(WaypointType.STATISTICS, wpt.getType());
        Assert.assertEquals(trackId, wpt.getTrackId());
        Assert.assertEquals(0.0, wpt.getLength(), 0.01);
        Assert.assertNotNull(wpt.getLocation());
        Assert.assertNotNull(wpt.getTripStatistics());
        // TODO check the rest of the params.

        // TODO: Check waypoint 2.
    }

    private void createDummyTrack(long id, long stopTime, boolean isRecording) {
        Track dummyTrack = new Track();
        dummyTrack.setId(id);
        dummyTrack.setName("Dummy Track");
        TripStatistics tripStatistics = new TripStatistics();
        tripStatistics.setStopTime(stopTime);
        dummyTrack.setTripStatistics(tripStatistics);
        addTrack(dummyTrack, isRecording);
    }

    private void addTrack(Track track, boolean isRecording) {
        Assert.assertTrue(track.getId() >= 0);
        providerUtils.insertTrack(track);
        Assert.assertEquals(track.getId(), providerUtils.getTrack(track.getId()).getId());
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, isRecording ? track.getId() : PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        PreferencesUtils.setBoolean(context, R.string.recording_track_paused_key, !isRecording);
    }

    /**
     * Inserts a location and waits for 200ms.
     */
    private void insertLocation(ITrackRecordingService trackRecordingService) throws InterruptedException {
        Location location = new Location("gps");
        location.setLongitude(35.0f);
        location.setLatitude(45.0f);
        location.setAccuracy(5);
        location.setSpeed(10);
        location.setTime(System.currentTimeMillis());
        location.setBearing(3.0f);
        trackRecordingService.insertTrackPoint(location);

        Thread.sleep(200);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertFalse(service.isRecording());

        long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT);
        Assert.assertEquals(-1L, waypointId);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_validWaypoint() throws Exception {
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent(context)));
        Assert.assertTrue(service.isRecording());
        insertLocation(service);

        long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT);
        Assert.assertNotEquals(-1L, waypointId);
        Waypoint wpt = providerUtils.getWaypoint(waypointId);
        Assert.assertEquals(context.getString(R.string.marker_waypoint_icon_url), wpt.getIcon());
        Assert.assertEquals(context.getString(R.string.marker_name_format, 1), wpt.getName());
        Assert.assertEquals(WaypointType.WAYPOINT, wpt.getType());
        Assert.assertEquals(trackId, wpt.getTrackId());
        Assert.assertEquals(0.0, wpt.getLength(), 0.01);
        Assert.assertNotNull(wpt.getLocation());
        Assert.assertNull(wpt.getTripStatistics());
    }
}
