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

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 *
 * ATTENTION: This tests deletes all stored tracks in the database.
 * So, if it is executed on a real device, data might be lost.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();
    private ContentProviderUtils providerUtils;
    private Context context = ApplicationProvider.getApplicationContext();

    private long trackId = Math.abs(new Random().nextLong());

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
        updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, 0);

        // Ensure that the database is empty before every test
        providerUtils.deleteAllTracks(context);
    }

    @After
    public void tearDown() throws TimeoutException {
        // Reset service (if some previous test failed)
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        if (service.isRecording()) {
            service.endCurrentTrack();
        }

        // Ensure that the database is empty after every test
        providerUtils.deleteAllTracks(context);
    }

    @SmallTest
    @Test
    public void testStartable() throws TimeoutException {
        mServiceRule.startService(createStartIntent());
        Assert.assertNotNull(mServiceRule.bindService(createStartIntent()));
    }

    @MediumTest
    @Test
    public void testBindable() throws TimeoutException {
        IBinder service = mServiceRule.bindService(createStartIntent());
        Assert.assertNotNull(service);
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_shouldResume() throws Exception {
        // Insert a dummy track and mark it as recording track.
        createDummyTrack(trackId, System.currentTimeMillis(), true);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent();
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));
        Assert.assertNotNull(service);

        // We expect to resume the previous track.
        Assert.assertTrue(service.isRecording());
        Assert.assertEquals(trackId, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_simulateReboot() throws Exception {
        updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        // Simulate recording a track.
        long id = service.startNewTrack();
        Assert.assertTrue(service.isRecording());
        Assert.assertEquals(id, service.getRecordingTrackId());
        mServiceRule.unbindService();
        Assert.assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent();
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        mServiceRule.startService(startIntent);
        service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertNotNull(service);

        Assert.assertTrue(service.isRecording());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_noRecordingTrack() throws Exception {
        // Insert a dummy track and mark it as recording track.
        createDummyTrack(trackId, System.currentTimeMillis(), false);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
                PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent();
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because it was stopped.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_expiredTrack() throws Exception {
        // Insert a dummy track last updated 20 min ago.
        createDummyTrack(trackId, System.currentTimeMillis() - 20 * 60 * 1000, true);

        // Clear the number of attempts and set the timeout to 10 min.
        updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
                PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent();
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because it has expired.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testResumeAfterReboot_tooManyAttempts() throws Exception {
        // Insert a dummy track.
        createDummyTrack(trackId, System.currentTimeMillis(), true);

        // Set the number of attempts to max.
        updateAutoResumePrefs(TrackRecordingService.MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS, PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

        // Start the service in "resume" mode (simulates the on-reboot action).
        Intent startIntent = createStartIntent();
        startIntent.putExtra(TrackRecordingService.RESUME_TRACK_EXTRA_NAME, true);

        //Explicit start service, so `startCommand()` is executed - which would be called by BootReceiver.
        context.startService(startIntent);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(startIntent));
        Assert.assertNotNull(service);

        // We don't expect to resume the previous track, because there were already too many attempts.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_noTracks() throws Exception {
        List<Track> tracks = providerUtils.getAllTracks();
        Assert.assertTrue(tracks.isEmpty());

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        // Test if we start in no-recording mode by default.
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_oldTracks() throws Exception {
        createDummyTrack(trackId, -1L, false);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testRecording_orphanedRecordingTrack() throws Exception {
        // Just set recording track to a bogus value.
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, 123L);

        // Make sure that the service will not start recording and will clear the bogus track.
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testStartNewTrack_noRecording() throws Exception {
        // NOTICE: due to the way Android permissions work, if this fails,
        // uninstall the test apk then retry - the test must be installed *after* the app (go figure).
        // Reference: http://code.google.com/p/android/issues/detail?id=5521
        BlockingBroadcastReceiver startReceiver = new BlockingBroadcastReceiver();
        String startAction = context.getString(R.string.track_started_broadcast_action);
        context.registerReceiver(startReceiver, new IntentFilter(startAction));

        List<Track> tracks = providerUtils.getAllTracks();
        Assert.assertTrue(tracks.isEmpty());

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        long id = service.startNewTrack();
        Assert.assertTrue(id >= 0);
        Assert.assertTrue(service.isRecording());
        Track track = providerUtils.getTrack(id);
        Assert.assertNotNull(track);
        Assert.assertEquals(id, track.getId());
        Assert.assertEquals(PreferencesUtils.getString(context, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT), track.getCategory());
        Assert.assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
        Assert.assertEquals(id, service.getRecordingTrackId());

        // Verify that the start broadcast was received.
        Assert.assertTrue(startReceiver.waitUntilReceived(1));
        List<Intent> receivedIntents = startReceiver.getReceivedIntents();
        Assert.assertEquals(1, receivedIntents.size());
        Intent broadcastIntent = receivedIntents.get(0);
        Assert.assertEquals(startAction, broadcastIntent.getAction());
        Assert.assertEquals(id, broadcastIntent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L));

        context.unregisterReceiver(startReceiver);
    }

    @MediumTest
    @Test
    public void testStartNewTrack_alreadyRecording() throws Exception {
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertTrue(service.isRecording());

        // Starting a new track when there is a recording should just return -1L.
        long newTrack = service.startNewTrack();
        Assert.assertEquals(-1L, newTrack);

        Assert.assertEquals(trackId, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
        Assert.assertEquals(trackId, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testEndCurrentTrack_alreadyRecording() throws Exception {
        // See comment above if this fails randomly.
        BlockingBroadcastReceiver stopReceiver = new BlockingBroadcastReceiver();
        String stopAction = context.getString(R.string.track_stopped_broadcast_action);
        context.registerReceiver(stopReceiver, new IntentFilter(stopAction));

        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertTrue(service.isRecording());

        // End the current track.
        service.endCurrentTrack();
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());

        // Verify that the stop broadcast was received.
        Assert.assertTrue(stopReceiver.waitUntilReceived(1));
        List<Intent> receivedIntents = stopReceiver.getReceivedIntents();
        Assert.assertEquals(1, receivedIntents.size());
        Intent broadcastIntent = receivedIntents.get(0);
        Assert.assertEquals(stopAction, broadcastIntent.getAction());
        Assert.assertEquals(trackId, broadcastIntent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L));

        context.unregisterReceiver(stopReceiver);
    }

    @MediumTest
    @Test
    public void testEndCurrentTrack_noRecording() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        /*
         * Ending the current track when there is no recording should not result in any error.
         */
        service.endCurrentTrack();

        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
        Assert.assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
    }

    @MediumTest
    @Test
    public void testIntegration_completeRecordingSession() throws Exception {
        List<Track> tracks = providerUtils.getAllTracks();
        Assert.assertTrue(tracks.isEmpty());
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testInsertStatisticsMarker_noRecordingTrack() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        Assert.assertEquals(-1L, waypointId);
    }

    @MediumTest
    @Test
    public void testInsertStatisticsMarker_validLocation() throws Exception {
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
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

    @MediumTest
    @Test
    public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT);
        Assert.assertEquals(-1L, waypointId);
    }

    @MediumTest
    @Test
    public void testInsertWaypointMarker_validWaypoint() throws Exception {
        createDummyTrack(trackId, -1L, true);

        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
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

    @MediumTest
    @Test
    public void testWithProperties_voiceFrequencyDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.voice_frequency_key, PreferencesUtils.VOICE_FREQUENCY_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_voiceFrequencyByDistance() throws Exception {
        PreferencesUtils.setInt(context, R.string.voice_frequency_key, -1);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_voiceFrequencyByTime() throws Exception {
        PreferencesUtils.setInt(context, R.string.voice_frequency_key, 1);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_maxRecordingDistanceDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.max_recording_distance_key, PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_maxRecordingDistance() throws Exception {
        PreferencesUtils.setInt(context, R.string.max_recording_distance_key, 50);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingDistanceDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.recording_distance_interval_key, PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingDistance() throws Exception {
        PreferencesUtils.setInt(context, R.string.recording_distance_interval_key, 2);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_splitFrequencyDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.split_frequency_key, PreferencesUtils.SPLIT_FREQUENCY_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_splitFrequencyByDistance() throws Exception {
        PreferencesUtils.setInt(context, R.string.split_frequency_key, -1);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_splitFrequencyByTime() throws Exception {
        PreferencesUtils.setInt(context, R.string.split_frequency_key, 1);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_metricUnitsDefault() throws Exception {
        PreferencesUtils.setString(context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_metricUnitsDisabled() throws Exception {
        PreferencesUtils.setString(context, R.string.stats_units_key, context.getString(R.string.stats_units_imperial));
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingIntervalDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.min_recording_interval_key, PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRecordingInterval() throws Exception {
        PreferencesUtils.setInt(context, R.string.min_recording_interval_key, 2);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRequiredAccuracyDefault() throws Exception {
        PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT);
        fullRecordingSession();
    }

    @MediumTest
    @Test
    public void testWithProperties_minRequiredAccuracy() throws Exception {
        PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, 500);
        fullRecordingSession();
    }

    private Track createDummyTrack(long id, long stopTime, boolean isRecording) {
        Track dummyTrack = new Track();
        dummyTrack.setId(id);
        dummyTrack.setName("Dummy Track");
        TripStatistics tripStatistics = new TripStatistics();
        tripStatistics.setStopTime(stopTime);
        dummyTrack.setTripStatistics(tripStatistics);
        addTrack(dummyTrack, isRecording);
        return dummyTrack;
    }

    private void updateAutoResumePrefs(int attempts, int timeoutMins) {
        PreferencesUtils.setInt(context, R.string.auto_resume_track_current_retry_key, attempts);
        PreferencesUtils.setInt(context, R.string.auto_resume_track_timeout_key, timeoutMins);
    }

    private Intent createStartIntent() {
        Intent startIntent = new Intent();
        startIntent.setClass(context, TrackRecordingService.class);
        return startIntent;
    }

    private void addTrack(Track track, boolean isRecording) {
        Assert.assertTrue(track.getId() >= 0);
        providerUtils.insertTrack(track);
        Assert.assertEquals(track.getId(), providerUtils.getTrack(track.getId()).getId());
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, isRecording ? track.getId() : PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
        PreferencesUtils.setBoolean(context, R.string.recording_track_paused_key, !isRecording);
    }

    private void fullRecordingSession() throws Exception {
        ITrackRecordingService service = ((ITrackRecordingService) mServiceRule.bindService(createStartIntent()));
        Assert.assertFalse(service.isRecording());

        // Start a track.
        long id = service.startNewTrack();
        Assert.assertTrue(id >= 0);
        Assert.assertTrue(service.isRecording());
        Track track = providerUtils.getTrack(id);
        Assert.assertNotNull(track);
        Assert.assertEquals(id, track.getId());
        Assert.assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
        Assert.assertEquals(id, service.getRecordingTrackId());

        // Insert a few points, markers and statistics.
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            Location loc = new Location("gps");
            loc.setLongitude(35.0f + i / 10.0f);
            loc.setLatitude(45.0f - i / 5.0f);
            loc.setAccuracy(5);
            loc.setSpeed(10);
            loc.setTime(startTime + i * 10000);
            loc.setBearing(3.0f);
            service.insertTrackPoint(loc);

            if (i % 10 == 0) {
                service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
            } else if (i % 7 == 0) {
                service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT);
            }
        }

        // Stop the track. Validate if it has correct data.
        service.endCurrentTrack();
        Assert.assertFalse(service.isRecording());
        Assert.assertEquals(-1L, service.getRecordingTrackId());
        track = providerUtils.getTrack(id);
        Assert.assertNotNull(track);
        Assert.assertEquals(id, track.getId());
        TripStatistics tripStatistics = track.getTripStatistics();
        Assert.assertNotNull(tripStatistics);
        Assert.assertTrue(tripStatistics.getStartTime() > 0);
        Assert.assertTrue(tripStatistics.getStopTime() >= tripStatistics.getStartTime());
    }

    /**
     * Inserts a location and waits for 100ms.
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

    /**
     * Synchronous/waitable broadcast receiver to be used in testing.
     */
    private class BlockingBroadcastReceiver extends BroadcastReceiver {
        private static final long MAX_WAIT_TIME_MS = 3000;
        private final List<Intent> receivedIntents = new ArrayList<>();

        public List<Intent> getReceivedIntents() {
            return receivedIntents;
        }

        @Override
        public void onReceive(Context ctx, Intent intent) {
            Log.d("Test", "Got broadcast: " + intent);
            synchronized (receivedIntents) {
                receivedIntents.add(intent);
                receivedIntents.notifyAll();
            }
        }

        public boolean waitUntilReceived(int receiveCount) {
            long deadline = System.currentTimeMillis() + MAX_WAIT_TIME_MS;
            synchronized (receivedIntents) {
                while (receivedIntents.size() < receiveCount) {
                    try {
                        // Wait releases synchronized lock until it returns
                        receivedIntents.wait(500);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }

                    if (System.currentTimeMillis() > deadline) {
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
