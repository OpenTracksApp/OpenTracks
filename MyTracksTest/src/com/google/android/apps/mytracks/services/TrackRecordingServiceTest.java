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

import static com.google.android.apps.mytracks.Constants.RESUME_TRACK_EXTRA_NAME;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProvider;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.test.RenamingDelegatingContext;
import android.test.ServiceTestCase;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockCursor;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the MyTracks track recording service.
 *
 * @author Bartlomiej Niechwiej
 *
 * TODO: The original class, ServiceTestCase, has a few limitations, e.g.
 * it's not possible to properly shutdown the service, unless tearDown()
 * is called, which prevents from testing multiple scenarios in a single
 * test (see runFunctionTest for more details).
 */
public class TrackRecordingServiceTest extends ServiceTestCase<TestRecordingService> {

  private Context context;
  private MyTracksProviderUtils providerUtils;

  /*
   * In order to support starting and binding to the service in the same
   * unit test, we provide a workaround, as the original class doesn't allow
   * to bind after the service has been previously started.
   */

  private boolean bound;
  private Intent serviceIntent;

  public TrackRecordingServiceTest() {
    super(TestRecordingService.class);
  }

  /**
   * A context wrapper with the user provided {@link ContentResolver}.
   *
   * TODO: Move to test utils package.
   */
  public static class MockContext extends ContextWrapper {
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
  protected IBinder bindService(Intent intent) {
    if (getService() != null) {
      if (bound) {
        throw new IllegalStateException(
            "Service: " + getService() + " is already bound");
      }
      bound = true;
      serviceIntent = intent.cloneFilter();
      return getService().onBind(intent);
    } else {
      return super.bindService(intent);
    }
  }

  @Override
  protected void shutdownService() {
    if (bound) {
      assertNotNull(getService());
      getService().onUnbind(serviceIntent);
      bound = false;
    }
    super.shutdownService();
  }

  @TargetApi(9)
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    /*
     * Create a mock context that uses a mock content resolver and a renaming
     * delegating context.
     */
    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext renamingDelegatingContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    context = new MockContext(mockContentResolver, renamingDelegatingContext);

    // Set up the mock content resolver
    MyTracksProvider myTracksProvider = new MyTracksProvider();
    myTracksProvider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, myTracksProvider);
    
    MockContentProvider settingsProvider = new MockContentProvider(context) {
        @Override
      public Bundle call(String method, String arg, Bundle extras) {
        return null;
      }
        @Override
      public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
          String sortOrder) {
        return null;
      }
    };
    mockContentResolver.addProvider(Settings.AUTHORITY, settingsProvider);
    
    MockContentProvider googleSettingsProvider = new MockContentProvider(context) {
        @Override
      public Bundle call(String method, String arg, Bundle extras) {
        return null;
      }
        @Override
      public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
          String sortOrder) {
        MockCursor mockCursor = new MockCursor() {
            @Override
          public int getCount() {
            return 1;
          }
            @Override
          public boolean moveToNext() {
            return true;
          }
            @Override
          public String getString(int columnIndex) {
            return MyTracksLocationManager.USE_LOCATION_FOR_SERVICES_ON;
          }
            @Override
          public void close() {}
        };
        return mockCursor;
      }
    };
    mockContentResolver.addProvider("com.google.settings", googleSettingsProvider);
    
    // Set the context
    setContext(context);

    providerUtils = MyTracksProviderUtils.Factory.get(context);

    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    // Let's use default values.
    sharedPreferences.edit().clear().apply();

    // Disable auto resume by default.
    updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT, 0);
    // No recording track.
    PreferencesUtils.setLong(
        context, R.string.recording_track_id_key, PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
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
    createDummyTrack(123L, System.currentTimeMillis(), true);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We expect to resume the previous track.
    assertTrue(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(123L, service.getRecordingTrackId());
  }

  // TODO: shutdownService() has a bug and doesn't set mServiceCreated
  // to false, thus preventing from a second call to onCreate().
  // Report the bug to Android team.  Until then, the following tests
  // and checks must be commented out.
  //
  // TODO: If fixed, remove "disabled" prefix from the test name.
  @MediumTest
  public void disabledTestResumeAfterReboot_simulateReboot() throws Exception {
    updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Simulate recording a track.
    long id = service.startNewTrack();
    assertTrue(service.isRecording());
    assertEquals(id, service.getRecordingTrackId());
    shutdownService();
    assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    assertTrue(getService().isRecording());
  }

  @MediumTest
  public void testResumeAfterReboot_noRecordingTrack() throws Exception {
    // Insert a dummy track and mark it as recording track.
    createDummyTrack(123L, System.currentTimeMillis(), false);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because it was stopped.
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testResumeAfterReboot_expiredTrack() throws Exception {
    // Insert a dummy track last updated 20 min ago.
    createDummyTrack(123L, System.currentTimeMillis() - 20 * 60 * 1000, true);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(PreferencesUtils.AUTO_RESUME_TRACK_CURRENT_RETRY_DEFAULT,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because it has expired.
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testResumeAfterReboot_tooManyAttempts() throws Exception {
    // Insert a dummy track.
    createDummyTrack(123L, System.currentTimeMillis(), true);

    // Set the number of attempts to max.
    updateAutoResumePrefs(TrackRecordingService.MAX_AUTO_RESUME_TRACK_RETRY_ATTEMPTS,
        PreferencesUtils.AUTO_RESUME_TRACK_TIMEOUT_DEFAULT);

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because there were already
    // too many attempts.
    assertFalse(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testRecording_noTracks() throws Exception {
    List<Track> tracks = providerUtils.getAllTracks();
    assertTrue(tracks.isEmpty());

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    // Test if we start in no-recording mode by default.
    assertFalse(service.isRecording());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testRecording_oldTracks() throws Exception {
    createDummyTrack(123L, -1L, false);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testRecording_orphanedRecordingTrack() throws Exception {
    // Just set recording track to a bogus value.
    PreferencesUtils.setLong(context, R.string.recording_track_id_key, 256L);

    // Make sure that the service will not start recording and will clear
    // the bogus track.
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertEquals(-1L, service.getRecordingTrackId());
  }

  /**
   * Synchronous/waitable broadcast receiver to be used in testing.
   */
  private class BlockingBroadcastReceiver extends BroadcastReceiver {
    private static final long MAX_WAIT_TIME_MS = 10000;
    private List<Intent> receivedIntents = new ArrayList<Intent>();

    public List<Intent> getReceivedIntents() {
      return receivedIntents;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
      Log.d("MyTracksTest", "Got broadcast: " + intent);
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

  @MediumTest
  public void testStartNewTrack_noRecording() throws Exception {
    // NOTICE: due to the way Android permissions work, if this fails,
    // uninstall the test apk then retry - the test must be installed *after*
    // My Tracks (go figure).
    // Reference: http://code.google.com/p/android/issues/detail?id=5521
    BlockingBroadcastReceiver startReceiver = new BlockingBroadcastReceiver();
    String startAction = context.getString(R.string.track_started_broadcast_action);
    context.registerReceiver(startReceiver, new IntentFilter(startAction));

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
    assertEquals(PreferencesUtils.getString(
        context, R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT),
        track.getCategory());
    assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
    assertEquals(id, service.getRecordingTrackId());

    // Verify that the start broadcast was received.
    assertTrue(startReceiver.waitUntilReceived(1));
    List<Intent> receivedIntents = startReceiver.getReceivedIntents();
    assertEquals(1, receivedIntents.size());
    Intent broadcastIntent = receivedIntents.get(0);
    assertEquals(startAction, broadcastIntent.getAction());
    assertEquals(id, broadcastIntent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), -1L));

    context.unregisterReceiver(startReceiver);
  }

  @MediumTest
  public void testStartNewTrack_alreadyRecording() throws Exception {
    createDummyTrack(123L, -1L, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // Starting a new track when there is a recording should just return -1L.
    long newTrack = service.startNewTrack();
    assertEquals(-1L, newTrack);

    assertEquals(123L, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
    assertEquals(123L, service.getRecordingTrackId());
  }

  @MediumTest
  public void testEndCurrentTrack_alreadyRecording() throws Exception {
    // See comment above if this fails randomly.
    BlockingBroadcastReceiver stopReceiver = new BlockingBroadcastReceiver();
    String stopAction = context.getString(R.string.track_stopped_broadcast_action);
    context.registerReceiver(stopReceiver, new IntentFilter(stopAction));

    createDummyTrack(123L, -1L, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // End the current track.
    service.endCurrentTrack();
    assertFalse(service.isRecording());
    assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
        PreferencesUtils.getLong(context, R.string.recording_track_id_key));
    assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());

    // Verify that the stop broadcast was received.
    assertTrue(stopReceiver.waitUntilReceived(1));
    List<Intent> receivedIntents = stopReceiver.getReceivedIntents();
    assertEquals(1, receivedIntents.size());
    Intent broadcastIntent = receivedIntents.get(0);
    assertEquals(stopAction, broadcastIntent.getAction());
    assertEquals(123L, broadcastIntent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), -1L));

    context.unregisterReceiver(stopReceiver);
  }

  @MediumTest
  public void testEndCurrentTrack_noRecording() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Ending the current track when there is no recording should not result in any error.
    service.endCurrentTrack();

    assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
        PreferencesUtils.getLong(context, R.string.recording_track_id_key));
    assertEquals(PreferencesUtils.RECORDING_TRACK_ID_DEFAULT, service.getRecordingTrackId());
  }

  @MediumTest
  public void testIntegration_completeRecordingSession() throws Exception {
    List<Track> tracks = providerUtils.getAllTracks();
    assertTrue(tracks.isEmpty());
    fullRecordingSession();
  }

  @MediumTest
  public void testInsertStatisticsMarker_noRecordingTrack() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
    assertEquals(-1L, waypointId);
  }

  @MediumTest
  public void testInsertStatisticsMarker_validLocation() throws Exception {
    createDummyTrack(123L, -1L, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    assertFalse(service.isPaused());
    insertLocation(service);

    assertEquals(1, service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS));
    assertEquals(2, service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS));

    Waypoint wpt = providerUtils.getWaypoint(2);
    assertEquals(getContext().getString(R.string.marker_statistics_icon_url), wpt.getIcon());
    assertEquals(getContext().getString(R.string.marker_split_name_format, 1), wpt.getName());
    assertEquals(Waypoint.TYPE_STATISTICS, wpt.getType());
    assertEquals(123L, wpt.getTrackId());
    assertEquals(0.0, wpt.getLength());
    assertNotNull(wpt.getLocation());
    assertNotNull(wpt.getTripStatistics());
    // TODO check the rest of the params.

    // TODO: Check waypoint 2.
  }

  @MediumTest
  public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    long waypointId = service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT);
    assertEquals(-1L, waypointId);
  }

  @MediumTest
  public void testInsertWaypointMarker_validWaypoint() throws Exception {
    createDummyTrack(123L, -1L, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());
    insertLocation(service);

    assertEquals(1, service.insertWaypoint(WaypointCreationRequest.DEFAULT_WAYPOINT));
    Waypoint wpt = providerUtils.getWaypoint(1);
    assertEquals(getContext().getString(R.string.marker_waypoint_icon_url),
        wpt.getIcon());
    assertEquals(getContext().getString(R.string.marker_name_format, 1), wpt.getName());
    assertEquals(Waypoint.TYPE_WAYPOINT, wpt.getType());
    assertEquals(123L, wpt.getTrackId());
    assertEquals(0.0, wpt.getLength());
    assertNotNull(wpt.getLocation());
    assertNull(wpt.getTripStatistics());
  }

  @MediumTest
  public void testWithProperties_voiceFrequencyDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.voice_frequency_key,
        PreferencesUtils.VOICE_FREQUENCY_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_voiceFrequencyByDistance() throws Exception {
    PreferencesUtils.setInt(context, R.string.voice_frequency_key, -1);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_voiceFrequencyByTime() throws Exception {
    PreferencesUtils.setInt(context, R.string.voice_frequency_key, 1);
    fullRecordingSession();
  }
  
  @MediumTest
  public void testWithProperties_maxRecordingDistanceDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.max_recording_distance_key,
        PreferencesUtils.MAX_RECORDING_DISTANCE_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_maxRecordingDistance() throws Exception {
    PreferencesUtils.setInt(context, R.string.max_recording_distance_key, 50);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRecordingDistanceDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_recording_distance_key,
        PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRecordingDistance() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_recording_distance_key, 2);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_splitFrequencyDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.split_frequency_key,
        PreferencesUtils.SPLIT_FREQUENCY_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_splitFrequencyByDistance() throws Exception {
    PreferencesUtils.setInt(context, R.string.split_frequency_key, -1);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_splitFrequencyByTime() throws Exception {
    PreferencesUtils.setInt(context, R.string.split_frequency_key, 1);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_metricUnitsDefault() throws Exception {
    PreferencesUtils.setBoolean(
        context, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_metricUnitsDisabled() throws Exception {
    PreferencesUtils.setBoolean(context, R.string.metric_units_key, false);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRecordingIntervalDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_recording_interval_key,
        PreferencesUtils.MIN_RECORDING_INTERVAL_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRecordingInterval() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_recording_interval_key, 2);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRequiredAccuracyDefault() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_required_accuracy_key,
        PreferencesUtils.MIN_REQUIRED_ACCURACY_DEFAULT);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_minRequiredAccuracy() throws Exception {
    PreferencesUtils.setInt(context, R.string.min_required_accuracy_key, 500);
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_sensorTypeDefault() throws Exception {
    PreferencesUtils.setString(
        context, R.string.sensor_type_key, context.getString(R.string.sensor_type_value_none));
    fullRecordingSession();
  }

  @MediumTest
  public void testWithProperties_sensorTypeZephyr() throws Exception {
    PreferencesUtils.setString(
        context, R.string.sensor_type_key, context.getString(R.string.sensor_type_value_zephyr));
    fullRecordingSession();
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
    assertTrue(track.getId() >= 0);
    providerUtils.insertTrack(track);
    assertEquals(track.getId(), providerUtils.getTrack(track.getId()).getId());
    PreferencesUtils.setLong(context, R.string.recording_track_id_key, isRecording ? track.getId()
        : PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
    PreferencesUtils.setBoolean(context, R.string.recording_track_paused_key, !isRecording);
  }

  private void fullRecordingSession() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Start a track.
    long id = service.startNewTrack();
    assertTrue(id >= 0);
    assertTrue(service.isRecording());
    Track track = providerUtils.getTrack(id);
    assertNotNull(track);
    assertEquals(id, track.getId());
    assertEquals(id, PreferencesUtils.getLong(context, R.string.recording_track_id_key));
    assertEquals(id, service.getRecordingTrackId());

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

    // Stop the track.  Validate if it has correct data.
    service.endCurrentTrack();
    assertFalse(service.isRecording());
    assertEquals(-1L, service.getRecordingTrackId());
    track = providerUtils.getTrack(id);
    assertNotNull(track);
    assertEquals(id, track.getId());
    TripStatistics tripStatistics = track.getTripStatistics();
    assertNotNull(tripStatistics);
    assertTrue(tripStatistics.getStartTime() > 0);
    assertTrue(tripStatistics.getStopTime() >= tripStatistics.getStartTime());
  }
  
  /**
   * Inserts a location and waits for 100ms.
   * 
   * @param trackRecordingService the track recording service
   */
  private void insertLocation(ITrackRecordingService trackRecordingService)
      throws RemoteException, InterruptedException {
    Location location = new Location("gps");
    location.setLongitude(35.0f);
    location.setLatitude(45.0f);
    location.setAccuracy(5);
    location.setSpeed(10);
    location.setTime(System.currentTimeMillis());
    location.setBearing(3.0f);
    trackRecordingService.insertTrackPoint(location);
    Thread.sleep(100);
  }
}
