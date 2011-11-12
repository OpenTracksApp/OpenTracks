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
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.IBinder;
import android.test.RenamingDelegatingContext;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
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
public class TrackRecordingServiceTest
    extends ServiceTestCase<TrackRecordingService> {

  private Context context;
  private MyTracksProviderUtils providerUtils;
  private SharedPreferences sharedPreferences;

  /*
   * In order to support starting and binding to the service in the same
   * unit test, we provide a workaround, as the original class doesn't allow
   * to bind after the service has been previously started.
   */

  private boolean bound;
  private Intent serviceIntent;

  public TrackRecordingServiceTest() {
    super(TrackRecordingService.class);
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

  /**
   * A mock class that forces API level < 5 to make sure we can workaround a bug
   * in ServiceTestCase (throwing a NPE).
   * See http://code.google.com/p/android/issues/detail?id=12122 for more
   * details.
   */
  private static class MockApiFeatures extends ApiFeatures {
    @Override
    protected int getApiLevel() {
      return 4;
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ApiFeatures.injectInstance(new MockApiFeatures());

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
        Constants.SETTINGS_NAME, 0);
    // Let's use default values.
    sharedPreferences.edit().clear().commit();

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

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We expect to resume the previous track.
    assertTrue(getService().isRecording());
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertEquals(123, service.getRecordingTrackId());
  }

  // TODO: shutdownService() has a bug and doesn't set mServiceCreated
  // to false, thus preventing from a second call to onCreate().
  // Report the bug to Android team.  Until then, the following tests
  // and checks must be commented out.
  //
  // TODO: If fixed, remove "disabled" prefix from the test name.
  @MediumTest
  public void disabledTestResumeAfterReboot_simulateReboot() throws Exception {
    updateAutoResumePrefs(0, 10);
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Simulate recording a track.
    long id = service.startNewTrack();
    assertTrue(service.isRecording());
    assertEquals(id, service.getRecordingTrackId());
    shutdownService();
    assertEquals(id, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), -1));

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
    createDummyTrack(123, System.currentTimeMillis(), false);

    // Clear the number of attempts and set the timeout to 10 min.
    updateAutoResumePrefs(0, 10);

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because it was stopped.
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

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because it has expired.
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

    // Start the service in "resume" mode (simulates the on-reboot action).
    Intent startIntent = createStartIntent();
    startIntent.putExtra(RESUME_TRACK_EXTRA_NAME, true);
    startService(startIntent);
    assertNotNull(getService());

    // We don't expect to resume the previous track, because there were already
    // too many attempts.
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
  public void testRecording_orphanedRecordingTrack() throws Exception {
    // Just set recording track to a bogus value.
    setRecordingTrack(256);

    // Make sure that the service will not start recording and will clear
    // the bogus track.
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());
    assertEquals(-1, service.getRecordingTrackId());
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
    assertEquals(sharedPreferences.getString(context.getString(R.string.default_activity_key), ""),
        track.getCategory());
    assertEquals(id, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), -1));
    assertEquals(id, service.getRecordingTrackId());

    // Verify that the start broadcast was received.
    assertTrue(startReceiver.waitUntilReceived(1));
    List<Intent> receivedIntents = startReceiver.getReceivedIntents();
    assertEquals(1, receivedIntents.size());
    Intent broadcastIntent = receivedIntents.get(0);
    assertEquals(startAction, broadcastIntent.getAction());
    assertEquals(id, broadcastIntent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), -1));

    context.unregisterReceiver(startReceiver);
  }

  @MediumTest
  public void testStartNewTrack_alreadyRecording() throws Exception {
    createDummyTrack(123, -1, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // Starting a new track when there is a recording should just return -1L.
    long newTrack = service.startNewTrack();
    assertEquals(-1L, newTrack);

    assertEquals(123, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(123, service.getRecordingTrackId());
  }

  @MediumTest
  public void testEndCurrentTrack_alreadyRecording() throws Exception {
    // See comment above if this fails randomly.
    BlockingBroadcastReceiver stopReceiver = new BlockingBroadcastReceiver();
    String stopAction = context.getString(R.string.track_stopped_broadcast_action);
    context.registerReceiver(stopReceiver, new IntentFilter(stopAction));

    createDummyTrack(123, -1, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    // End the current track.
    service.endCurrentTrack();
    assertFalse(service.isRecording());
    assertEquals(-1, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(-1, service.getRecordingTrackId());

    // Verify that the stop broadcast was received.
    assertTrue(stopReceiver.waitUntilReceived(1));
    List<Intent> receivedIntents = stopReceiver.getReceivedIntents();
    assertEquals(1, receivedIntents.size());
    Intent broadcastIntent = receivedIntents.get(0);
    assertEquals(stopAction, broadcastIntent.getAction());
    assertEquals(123, broadcastIntent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), -1));

    context.unregisterReceiver(stopReceiver);
  }

  @MediumTest
  public void testEndCurrentTrack_noRecording() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    // Ending the current track when there is no recording should not result in any error.
    service.endCurrentTrack();

    assertEquals(-1, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), 0));
    assertEquals(-1, service.getRecordingTrackId());
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

    try {
      service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
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

    assertEquals(1, service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS));
    assertEquals(2, service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS));

    Waypoint wpt = providerUtils.getWaypoint(1);
    assertEquals(getContext().getString(R.string.marker_statistics_icon_url),
        wpt.getIcon());
    assertEquals(getContext().getString(R.string.marker_type_statistics),
        wpt.getName());
    assertEquals(Waypoint.TYPE_STATISTICS, wpt.getType());
    assertEquals(123, wpt.getTrackId());
    assertEquals(0.0, wpt.getLength());
    assertNotNull(wpt.getLocation());
    assertNotNull(wpt.getStatistics());
    // TODO check the rest of the params.

    // TODO: Check waypoint 2.
  }

  @MediumTest
  public void testInsertWaypointMarker_noRecordingTrack() throws Exception {
    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertFalse(service.isRecording());

    try {
      service.insertWaypoint(WaypointCreationRequest.DEFAULT_MARKER);
      fail("Expecting IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  @MediumTest
  public void testInsertWaypointMarker_validWaypoint() throws Exception {
    createDummyTrack(123, -1, true);

    ITrackRecordingService service = bindAndGetService(createStartIntent());
    assertTrue(service.isRecording());

    assertEquals(1, service.insertWaypoint(WaypointCreationRequest.DEFAULT_MARKER));
    Waypoint wpt = providerUtils.getWaypoint(1);
    assertEquals(getContext().getString(R.string.marker_waypoint_icon_url),
        wpt.getIcon());
    assertEquals(getContext().getString(R.string.marker_type_waypoint),
        wpt.getName());
    assertEquals(Waypoint.TYPE_WAYPOINT, wpt.getType());
    assertEquals(123, wpt.getTrackId());
    assertEquals(0.0, wpt.getLength());
    assertNotNull(wpt.getLocation());
    assertNull(wpt.getStatistics());
  }

  @MediumTest
  public void testWithProperties_noAnnouncementFreq() throws Exception {
    functionalTest(R.string.announcement_frequency_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultAnnouncementFreq() throws Exception {
    functionalTest(R.string.announcement_frequency_key, 1);
  }

  @MediumTest
  public void testWithProperties_noMaxRecordingDist() throws Exception {
    functionalTest(R.string.max_recording_distance_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultMaxRecordingDist() throws Exception {
    functionalTest(R.string.max_recording_distance_key, 5);
  }

  @MediumTest
  public void testWithProperties_noMinRecordingDist() throws Exception {
    functionalTest(R.string.min_recording_distance_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultMinRecordingDist() throws Exception {
    functionalTest(R.string.min_recording_distance_key, 2);
  }

  @MediumTest
  public void testWithProperties_noSplitFreq() throws Exception {
    functionalTest(R.string.split_frequency_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultSplitFreqByDist() throws Exception {
    functionalTest(R.string.split_frequency_key, 5);
  }

  @MediumTest
  public void testWithProperties_defaultSplitFreqByTime() throws Exception {
    functionalTest(R.string.split_frequency_key, -2);
  }

  @MediumTest
  public void testWithProperties_noMetricUnits() throws Exception {
    functionalTest(R.string.metric_units_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_metricUnitsEnabled() throws Exception {
    functionalTest(R.string.metric_units_key, true);
  }

  @MediumTest
  public void testWithProperties_metricUnitsDisabled() throws Exception {
    functionalTest(R.string.metric_units_key, false);
  }

  @MediumTest
  public void testWithProperties_noMinRecordingInterval() throws Exception {
    functionalTest(R.string.min_recording_interval_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultMinRecordingInterval()
      throws Exception {
    functionalTest(R.string.min_recording_interval_key, 3);
  }

  @MediumTest
  public void testWithProperties_noMinRequiredAccuracy() throws Exception {
    functionalTest(R.string.min_required_accuracy_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_defaultMinRequiredAccuracy() throws Exception {
    functionalTest(R.string.min_required_accuracy_key, 500);
  }

  @MediumTest
  public void testWithProperties_noSensorType() throws Exception {
    functionalTest(R.string.sensor_type_key, (Object) null);
  }

  @MediumTest
  public void testWithProperties_zephyrSensorType() throws Exception {
    functionalTest(R.string.sensor_type_key,
        context.getString(R.string.sensor_type_value_zephyr));
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
    setRecordingTrack(isRecording ? track.getId() : -1);
  }

  private void setRecordingTrack(long id) {
    Editor editor = sharedPreferences.edit();
    editor.putLong(context.getString(R.string.recording_track_key), id);
    editor.commit();
  }

  // TODO: We support multiple values for readability, however this test's
  // base class doesn't properly shutdown the service, so it's not possible
  // to pass more than 1 value at a time.
  private void functionalTest(int resourceId, Object ...values)
      throws Exception {
    final String key = context.getString(resourceId);
    for (Object value : values) {
      // Remove all properties and set the property for the given key.
      Editor editor = sharedPreferences.edit();
      editor.clear();
      if (value instanceof String) {
        editor.putString(key, (String) value);
      } else if (value instanceof Long) {
        editor.putLong(key, (Long) value);
      } else if (value instanceof Integer) {
        editor.putInt(key, (Integer) value);
      } else if (value instanceof Boolean) {
        editor.putBoolean(key, (Boolean) value);
      } else if (value == null) {
        // Do nothing, as clear above has already removed this property.
      }
      editor.commit();

      fullRecordingSession();
    }
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
    assertEquals(id, sharedPreferences.getLong(
        context.getString(R.string.recording_track_key), -1));
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
      service.recordLocation(loc);

      if (i % 10 == 0) {
        service.insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
      } else if (i % 7 == 0) {
        service.insertWaypoint(WaypointCreationRequest.DEFAULT_MARKER);
      }
    }

    // Stop the track.  Validate if it has correct data.
    service.endCurrentTrack();
    assertFalse(service.isRecording());
    assertEquals(-1, service.getRecordingTrackId());
    track = providerUtils.getTrack(id);
    assertNotNull(track);
    assertEquals(id, track.getId());
    TripStatistics tripStatistics = track.getStatistics();
    assertNotNull(tripStatistics);
    assertTrue(tripStatistics.getStartTime() > 0);
    assertTrue(tripStatistics.getStopTime() >= tripStatistics.getStartTime());
  }
}
