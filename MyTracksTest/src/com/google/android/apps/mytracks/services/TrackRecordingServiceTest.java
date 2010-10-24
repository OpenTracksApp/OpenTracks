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
package com.google.android.apps.mytracks.services;

import static com.google.android.apps.mytracks.MyTracksConstants.RESUME_TRACK_EXTRA_NAME;

import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.content.MyTracksProvider;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.IBinder;
import android.test.RenamingDelegatingContext;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.List;

/**
 * Tests for the MyTracks track recording service.
 * 
 * @author Bartlomiej Niechwiej
 */
public class TrackRecordingServiceTest
    extends ServiceTestCase<TrackRecordingService> {

  private Context context;
  private MyTracksProviderUtils providerUtils;
  private SharedPreferences sharedPreferences;
  
  public TrackRecordingServiceTest() {
    super(TrackRecordingService.class);
  }

  private static class MockContext extends ContextWrapper {
    private final ContentResolver contentResolver;
    
    public MockContext(ContentResolver contentResolver, Context base) {
      super(base);
      this.contentResolver = contentResolver;
    }

    @Override
    public ContentResolver getContentResolver() {
      return contentResolver;
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    context = new MockContext(mockContentResolver, targetContext);
    MyTracksProvider provider = new MyTracksProvider();
    provider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, provider);
    setContext(context);
    
    providerUtils = MyTracksProviderUtils.Factory.get(context);
    
    sharedPreferences = context.getSharedPreferences(
        MyTracksSettings.SETTINGS_NAME, 0);
    // Disable auto resume by default.
    updateAutoResumePrefs(0, -1);
    // No recording track.
    Editor editor = sharedPreferences.edit();
    editor.putLong(context.getString(R.string.recording_track_key), -1);
    editor.commit();
  }

  @SmallTest
  public void testStartable() {
    startService(createStartIntent());
    assertNotNull(getService());
  }

  @MediumTest
  public void testBindable() {
    IBinder service = bindService(createStartIntent());
    assertNotNull(service);
  }
  
  @MediumTest
  public void testResumeAfterReboot_shouldResume() throws Exception {
    // Insert a dummy track and mark it as recording track.
    createDummyTrack(123, System.currentTimeMillis(), true);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(0, 10);

    // Resume the service.  It should resume recording of the previous track.
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    assertTrue(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(123, service.getRecordingTrackId());
  }

  @MediumTest
  public void testResumeAfterReboot_noRecordingTrack() throws Exception {
    // Insert a dummy track and mark it as recording track.
    createDummyTrack(123, System.currentTimeMillis(), false);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(0, 10);

    // Resume the service.  It should resume recording of the previous track.
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());
    
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1, service.getRecordingTrackId());
  }
  
  @MediumTest
  public void testResumeAfterReboot_expiredTrack() throws Exception {
    // Insert a dummy track last updated 20 min ago.
    createDummyTrack(123, System.currentTimeMillis() - 20 * 60 * 1000, true);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(0, 10);

    // Resume the service.  It should resume recording of the previous track.
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());
    
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1, service.getRecordingTrackId());
  }

  @MediumTest
  public void testResumeAfterReboot_tooManyAttempts() throws Exception {
    // Insert a dummy track.
    createDummyTrack(123, System.currentTimeMillis(), true);

    // Set the number of attempts to max.
    updateAutoResumePrefs(
        TrackRecordingService.MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS, 10);

    // Resume the service.  It should resume recording of the previous track.
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());
    
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1, service.getRecordingTrackId());
  }

  @MediumTest
  public void testRecording_noTracks() throws Exception {
    List<Track> tracks = providerUtils.getAllTracks();
    assertTrue(tracks.isEmpty());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    // Test if we start in no-recording mode by default. 
    assertFalse(service.isRecording());
    assertEquals(-1, service.getRecordingTrackId());
  }
  
  @MediumTest
  public void testRecording_oldTracks() throws Exception {
    createDummyTrack(123, -1, false);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertEquals(-1, service.getRecordingTrackId());
  }

  @MediumTest
  public void testStartNewTrack_noRecording() throws Exception {
    List<Track> tracks = providerUtils.getAllTracks();
    assertTrue(tracks.isEmpty());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    
    long id = service.startNewTrack();
    assertTrue(id >= 0);
    assertTrue(service.isRecording());
    Track track = providerUtils.getTrack(id);
    assertNotNull(track);
    assertEquals(id, track.getId());
    assertEquals(id, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), -1));
    assertEquals(id, service.getRecordingTrackId());
  }
  
  @MediumTest
  public void testStartNewTrack_alreadyRecording() throws Exception {
    createDummyTrack(123, -1, true);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    
    try {
      service.startNewTrack();
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertEquals(123, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(123, service.getRecordingTrackId());
  }  
  
  @MediumTest
  public void testEndCurrentTrack_alreadyRecording() throws Exception {
    createDummyTrack(123, -1, true);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // End the current track.
    service.endCurrentTrack();
    assertFalse(service.isRecording());
    assertEquals(-1, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(-1, service.getRecordingTrackId());
  }
  
  @MediumTest
  public void testEndCurrentTrack_noRecording() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // End the current track.
    try {
      service.endCurrentTrack();
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertEquals(-1, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(-1, service.getRecordingTrackId());
  }
  
  @MediumTest
  public void testDeleteAllTracks_noRecording() throws Exception {
    createDummyTrack(123, -1, false);
    assertEquals(1, providerUtils.getAllTracks().size());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Deleting all tracks should succeed.
    service.deleteAllTracks();
    assertFalse(service.isRecording());
    assertTrue(providerUtils.getAllTracks().isEmpty());
  }

  @MediumTest
  public void testDeleteAllTracks_noTracks() throws Exception {
    assertTrue(providerUtils.getAllTracks().isEmpty());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Deleting all tracks should succeed.
    service.deleteAllTracks();
    assertFalse(service.isRecording());
    assertTrue(providerUtils.getAllTracks().isEmpty());
  }
  
  @MediumTest
  public void testDeleteAllTracks_trackInProgress() throws Exception {
    createDummyTrack(123, -1, true);
    assertEquals(1, providerUtils.getAllTracks().size());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // Since we have a track in progress, we expect to fail.
    try {
      service.deleteAllTracks();
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertTrue(service.isRecording());
    assertEquals(1, providerUtils.getAllTracks().size());
  }
  
  @MediumTest
  public void testHasRecorded_noTracks() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertFalse(service.hasRecorded());
  }
  
  @MediumTest
  public void testHasRecorded_trackInProgress() throws Exception {
    createDummyTrack(123, -1, true);
    assertEquals(1, providerUtils.getAllTracks().size());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    assertTrue(service.hasRecorded());
  }
  
  @MediumTest
  public void testHasRecorded_oldTracks() throws Exception {
    createDummyTrack(123, -1, false);
    assertEquals(1, providerUtils.getAllTracks().size());
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertTrue(service.hasRecorded());
  }
  
  @MediumTest
  public void testInsertStatisticsMarker_noRecordingTrack() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    
    Location loc = new Location("gps");
    try {
      service.insertStatisticsMarker(loc);
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }
  
  @MediumTest
  public void testInsertStatisticsMarker_validLocation() throws Exception {
    createDummyTrack(123, -1, true);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    
    Location loc = new Location("gps");
    assertEquals(1, service.insertStatisticsMarker(loc));
    assertEquals(2, service.insertStatisticsMarker(loc));
    
    // TODO: Add more checks.
  }

  @MediumTest
  public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    
    Location loc = new Location("gps");
    Waypoint waypoint = new Waypoint();
    waypoint.setId(1);
    waypoint.setLocation(loc);
    try {
      service.insertWaypointMarker(waypoint);
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }
  
  @MediumTest
  public void testInsertWaypointMarker_invalidWaypoint() throws Exception {
    createDummyTrack(123, -1, true);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    
    Waypoint waypoint = new Waypoint();
    assertEquals(-1, service.insertWaypointMarker(waypoint));
  }
  
  @MediumTest
  public void testInsertWaypointMarker_validWaypoint() throws Exception {
    createDummyTrack(123, -1, true);
    
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    
    Location loc = new Location("gps");
    Waypoint waypoint = new Waypoint();
    waypoint.setId(1);
    waypoint.setLocation(loc);
    assertEquals(1, service.insertWaypointMarker(waypoint));
  }
  
  private ITrackRecordingService bindAndGetService(Intent intent) {
    ITrackRecordingService service = ITrackRecordingService.Stub.asInterface(
        bindService(intent));
    assertNotNull(service);
    return service;
  }

  private Track createDummyTrack(long id, long stopTime, boolean isRecording) {
    Track dummyTrack = new Track();
    dummyTrack.setId(id);
    dummyTrack.setName("Dummy Track");
    TripStatistics tripStatistics = new TripStatistics();
    tripStatistics.setStopTime(stopTime);
    dummyTrack.setStatistics(tripStatistics);
    addTrack(dummyTrack, isRecording);
    return dummyTrack;
  }
  
  private void updateAutoResumePrefs(int attempts, int timeoutMins) {
    Editor editor = sharedPreferences.edit();
    editor.putInt(context.getString(
    R.string.auto_resume_track_current_retry_key), attempts);
    editor.putInt(context.getString(
        R.string.auto_resume_track_timeout_key), timeoutMins);
    editor.commit();
  }
  
  private Intent createStartIntent() {
    Intent startIntent = new Intent();
    startIntent.setClass(context, TrackRecordingService.class);
    return startIntent;
  }
  
  private void addTrack(Track track, boolean isRecording) {
    assertTrue(track.getId() >= 0);
    providerUtils.insertTrack(track);
    assertEquals(track.getId(), providerUtils.getTrack(track.getId()).getId());
    Editor editor = sharedPreferences.edit();
    editor.putLong(context.getString(R.string.recording_track_key),
        isRecording ? track.getId() : -1);
    editor.commit();
  }
}
