/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.content;

import static com.google.android.testing.mocking.AndroidMock.capture;
import static com.google.android.testing.mocking.AndroidMock.eq;
import static com.google.android.testing.mocking.AndroidMock.expect;
import static com.google.android.testing.mocking.AndroidMock.isA;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.TrackStubUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.IAnswer;

/**
 * Tests for {@link TrackDataHub}.
 * 
 * @author Rodrigo Damazio
 */
public class TrackDataHubTest extends AndroidTestCase {

  private static final long TRACK_ID = 42L;
  private static final int TARGET_POINTS = 50;

  private MockContext context;
  private SharedPreferences sharedPreferences;
  private MyTracksProviderUtils myTracksProviderUtils;
  private DataSource dataSource;
  private TrackDataManager trackDataManager;
  private TrackDataHub trackDataHub;
  private TrackDataListener trackDataListener1;
  private TrackDataListener trackDataListener2;
  private Capture<OnSharedPreferenceChangeListener> preferenceChangeListenerCapture = new Capture<
      SharedPreferences.OnSharedPreferenceChangeListener>();

  @UsesMocks({ MyTracksProviderUtils.class, DataSource.class, TrackDataListener.class })
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = new MockContext(new MockContentResolver(), new RenamingDelegatingContext(
        getContext(), getContext(), "test."));
    sharedPreferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    myTracksProviderUtils = AndroidMock.createMock(MyTracksProviderUtils.class);
    dataSource = AndroidMock.createMock(DataSource.class, context);
    trackDataManager = new TrackDataManager();
    trackDataHub = new TrackDataHub(
        context, trackDataManager, myTracksProviderUtils, TARGET_POINTS) {
        @Override
      protected DataSource newDataSource() {
        return dataSource;
      }

        @Override
      protected void runInHanderThread(Runnable runnable) {
        // Run everything in the same thread
        runnable.run();
      }
    };

    trackDataListener1 = AndroidMock.createStrictMock(
        "trackDataListener1", TrackDataListener.class);
    trackDataListener2 = AndroidMock.createStrictMock(
        "trackDataListener2", TrackDataListener.class);
    PreferencesUtils.setLong(context, R.string.recording_track_id_key, TRACK_ID);
  }

  @Override
  protected void tearDown() throws Exception {
    AndroidMock.reset(dataSource);

    // Expect everything to be unregistered.
    dataSource.unregisterContentObserver(isA(ContentObserver.class));
    AndroidMock.expectLastCall().times(3);
    dataSource.unregisterOnSharedPreferenceChangeListener(
        isA(OnSharedPreferenceChangeListener.class));
    AndroidMock.replay(dataSource);

    trackDataHub.stop();
    trackDataHub = null;
    super.tearDown();
  }

  /**
   * Tests registering for tracks table update.
   */
  public void testTracksTableUpdate() {

    // Register two listeners
    Capture<ContentObserver> contentObserverCapture = new Capture<ContentObserver>();
    Track track = new Track();
    expect(myTracksProviderUtils.getTrack(TRACK_ID)).andStubReturn(track);
    dataSource.registerContentObserver(
        eq(TracksColumns.CONTENT_URI), capture(contentObserverCapture));
    trackDataListener1.onTrackUpdated(track);
    trackDataListener2.onTrackUpdated(track);
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.TRACKS_TABLE));
    trackDataHub.registerTrackDataListener(
        trackDataListener2, EnumSet.of(TrackDataType.TRACKS_TABLE));
    verifyAndReset();

    // Causes tracks table update
    ContentObserver contentObserver = contentObserverCapture.getValue();
    expect(myTracksProviderUtils.getTrack(TRACK_ID)).andStubReturn(track);
    trackDataListener1.onTrackUpdated(track);
    trackDataListener2.onTrackUpdated(track);
    replay();

    contentObserver.onChange(false);
    verifyAndReset();

    // Unregister one listener
    expect(myTracksProviderUtils.getTrack(TRACK_ID)).andStubReturn(track);
    trackDataListener2.onTrackUpdated(track);
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener1);
    contentObserver.onChange(false);
    verifyAndReset();

    // Unregister the second listener
    dataSource.unregisterContentObserver(contentObserver);
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener2);
    contentObserver.onChange(false);
    verifyAndReset();
  }

  /**
   * Tests registering for waypoints table update.
   */
  public void testWaypointsTableUpdate() {
    Waypoint waypoint1 = new Waypoint();
    Waypoint waypoint2 = new Waypoint();
    Waypoint waypoint3 = new Waypoint();
    Waypoint waypoint4 = new Waypoint();
    Location location = new Location("gps");
    location.setLatitude(10.0);
    location.setLongitude(8.0);
    waypoint1.setLocation(location);
    waypoint2.setLocation(location);
    waypoint3.setLocation(location);
    waypoint4.setLocation(location);

    // Register two listeners
    Capture<ContentObserver> contentObserverCapture = new Capture<ContentObserver>();
    expect(myTracksProviderUtils.getWaypointCursor(
        eq(TRACK_ID), AndroidMock.leq(-1L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(2));
    expect(myTracksProviderUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(waypoint1).andReturn(waypoint2).andReturn(waypoint1).andReturn(waypoint2);
    dataSource.registerContentObserver(
        eq(WaypointsColumns.CONTENT_URI), capture(contentObserverCapture));
    trackDataListener1.clearWaypoints();
    trackDataListener2.clearWaypoints();
    trackDataListener1.onNewWaypoint(waypoint1);
    trackDataListener2.onNewWaypoint(waypoint1);
    trackDataListener1.onNewWaypoint(waypoint2);
    trackDataListener2.onNewWaypoint(waypoint2);
    trackDataListener1.onNewWaypointsDone();
    trackDataListener2.onNewWaypointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
    trackDataHub.registerTrackDataListener(
        trackDataListener2, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
    verifyAndReset();

    // Cause waypoints table update
    ContentObserver contentObserver = contentObserverCapture.getValue();
    expect(myTracksProviderUtils.getWaypointCursor(
        eq(TRACK_ID), AndroidMock.leq(-1L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(3));
    expect(myTracksProviderUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(waypoint1).andReturn(waypoint2).andReturn(waypoint3);
    trackDataListener1.clearWaypoints();
    trackDataListener2.clearWaypoints();
    trackDataListener1.onNewWaypoint(waypoint1);
    trackDataListener2.onNewWaypoint(waypoint1);
    trackDataListener1.onNewWaypoint(waypoint2);
    trackDataListener2.onNewWaypoint(waypoint2);
    trackDataListener1.onNewWaypoint(waypoint3);
    trackDataListener2.onNewWaypoint(waypoint3);
    trackDataListener1.onNewWaypointsDone();
    trackDataListener2.onNewWaypointsDone();
    replay();

    contentObserver.onChange(false);
    verifyAndReset();

    // Unregister one listener
    expect(myTracksProviderUtils.getWaypointCursor(
        eq(TRACK_ID), AndroidMock.leq(-1L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(4));
    expect(myTracksProviderUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(waypoint1).andReturn(waypoint2).andReturn(waypoint3).andReturn(waypoint4);
    trackDataListener2.clearWaypoints();
    trackDataListener2.onNewWaypoint(waypoint1);
    trackDataListener2.onNewWaypoint(waypoint2);
    trackDataListener2.onNewWaypoint(waypoint3);
    trackDataListener2.onNewWaypoint(waypoint4);
    trackDataListener2.onNewWaypointsDone();
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener1);
    contentObserver.onChange(false);
    verifyAndReset();

    // Unregister the second listener
    dataSource.unregisterContentObserver(contentObserver);
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener2);
    contentObserver.onChange(false);
    verifyAndReset();
  }

  /**
   * Tests track points table update.
   */
  public void testTrackPointsTableUpdate() {
    // Register one listener
    Capture<ContentObserver> contentObserverCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(contentObserverCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(10L);
    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();

    // Register a second listener
    locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(10L);
    trackDataListener2.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener2);
    trackDataListener2.onNewTrackPointsDone();
    replay();

    trackDataHub.registerTrackDataListener(
        trackDataListener2, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();

    // Deliver more points - should go to both listeners without clearing
    ContentObserver contentObserver = contentObserverCapture.getValue();
    locationIterator = new FixedSizeLocationIterator(11, 10, 1);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(11L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(20L);
    locationIterator.expectLocationsDelivered(trackDataListener1);
    locationIterator.expectLocationsDelivered(trackDataListener2);
    trackDataListener1.onNewTrackPointsDone();
    trackDataListener2.onNewTrackPointsDone();
    replay();

    contentObserver.onChange(false);
    verifyAndReset();

    // Unregister one listener and change track
    locationIterator = new FixedSizeLocationIterator(101, 10);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID + 1)).andReturn(110L);
    trackDataListener2.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener2);
    trackDataListener2.onNewTrackPointsDone();
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener1);
    trackDataHub.loadTrack(TRACK_ID + 1);
    verifyAndReset();
  }

  /**
   * Tests track points table update with registering the same listener.
   */
  public void testTrackPointsTableUpdate_reRegister() {

    // Register one listener
    Capture<ContentObserver> contentObserverCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(contentObserverCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(10L);

    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();

    // Unregister the listener
    ContentObserver observer = contentObserverCapture.getValue();
    dataSource.unregisterContentObserver(observer);
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener1);
    verifyAndReset();

    // Register again
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(contentObserverCapture));
    locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(10L);
    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();
  }

  /**
   * Tests tracks point able change. Register a listener after a track change.
   */
  public void testTrackPointsTableUpdate_reRegisterAfterTrackChange() {

    // Register one listener
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(observerCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(10L);
    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();

    // Unregister the listener
    ContentObserver observer = observerCapture.getValue();
    dataSource.unregisterContentObserver(observer);
    replay();

    trackDataHub.unregisterTrackDataListener(trackDataListener1);
    verifyAndReset();

    // Register the listener after a new track
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(observerCapture));
    locationIterator = new FixedSizeLocationIterator(1, 10);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID + 1)).andReturn(10L);
    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.loadTrack(TRACK_ID + 1);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();
  }

  /**
   * Tests track points table update with large track sampling.
   */
  public void testTrackPointsTableUpdate_largeTrackSampling() {
    Capture<ContentObserver> contentObserverCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(contentObserverCapture));

    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(200L);
    AndroidMock.expectLastCall().anyTimes();
    FixedSizeLocationIterator locationIterator1 = new FixedSizeLocationIterator(
        1, 200, 4, 25, 71, 120);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator1);
    FixedSizeLocationIterator locationIterator2 = new FixedSizeLocationIterator(
        1, 200, 4, 25, 71, 120);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator2);

    trackDataListener1.clearTrackPoints();
    locationIterator1.expectSampledLocationsDelivered(trackDataListener1, 4, false);
    trackDataListener1.onNewTrackPointsDone();
    trackDataListener2.clearTrackPoints();
    locationIterator2.expectSampledLocationsDelivered(trackDataListener2, 4, true);
    trackDataListener2.onNewTrackPointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(
        TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE, TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE));
    verifyAndReset();
  }

  /**
   * Tests track points table update with resampling.
   */
  public void testTrackPointsTableUpdate_resampling() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), capture(observerCapture));

    // Deliver 30 points (no sampling happens)
    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 30, 5);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(30L);

    trackDataListener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(trackDataListener1);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    trackDataHub.start();
    trackDataHub.loadTrack(TRACK_ID);
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
    verifyAndReset();

    // Now deliver 30 more (incrementally sampled)
    ContentObserver observer = observerCapture.getValue();
    locationIterator = new FixedSizeLocationIterator(31, 30);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(31L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(60L);
    locationIterator.expectSampledLocationsDelivered(trackDataListener1, 2, false);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    observer.onChange(false);
    verifyAndReset();

    // Now another 30 (triggers resampling)
    locationIterator = new FixedSizeLocationIterator(1, 90);
    expect(myTracksProviderUtils.getTrackPointLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).andReturn(locationIterator);
    expect(myTracksProviderUtils.getLastTrackPointId(TRACK_ID)).andReturn(90L);
    trackDataListener1.clearTrackPoints();
    locationIterator.expectSampledLocationsDelivered(trackDataListener1, 2, false);
    trackDataListener1.onNewTrackPointsDone();
    replay();

    observer.onChange(false);
    verifyAndReset();
  }

  /**
   * Tests preferences change.
   */
  public void testPreferencesChange() throws Exception {

    // Register two listeners
    PreferencesUtils.setBoolean(context, R.string.report_speed_key, true);
    PreferencesUtils.setString(
        context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key,
        PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT);
    PreferencesUtils.setInt(context, R.string.recording_distance_interval_key,
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);

    dataSource.registerOnSharedPreferenceChangeListener(capture(preferenceChangeListenerCapture));
    expect(trackDataListener1.onMetricUnitsChanged(true)).andReturn(false);
    expect(trackDataListener1.onReportSpeedChanged(true)).andReturn(false);
    expect(
        trackDataListener1.onRecordingGpsAccuracy(PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT))
        .andReturn(false);
    expect(trackDataListener1.onRecordingDistanceIntervalChanged(
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT)).andReturn(false);
    expect(trackDataListener2.onMetricUnitsChanged(true)).andReturn(false);
    expect(trackDataListener2.onReportSpeedChanged(true)).andReturn(false);
    expect(
        trackDataListener2.onRecordingGpsAccuracy(PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT))
        .andReturn(false);
    expect(trackDataListener2.onRecordingDistanceIntervalChanged(
        PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT)).andReturn(false);
    replay();

    trackDataHub.start();
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.PREFERENCE));
    trackDataHub.registerTrackDataListener(
        trackDataListener2, EnumSet.of(TrackDataType.PREFERENCE));
    verifyAndReset();

    // Change report speed to false
    expect(trackDataListener1.onReportSpeedChanged(false)).andReturn(false);
    expect(trackDataListener2.onReportSpeedChanged(false)).andReturn(false);
    replay();

    PreferencesUtils.setBoolean(context, R.string.report_speed_key, false);
    OnSharedPreferenceChangeListener listener = preferenceChangeListenerCapture.getValue();
    listener.onSharedPreferenceChanged(
        sharedPreferences, PreferencesUtils.getKey(context, R.string.report_speed_key));
    verifyAndReset();

    // Change metric units to false
    expect(trackDataListener1.onMetricUnitsChanged(false)).andReturn(false);
    expect(trackDataListener2.onMetricUnitsChanged(false)).andReturn(false);
    replay();

    String imperialUnits = context.getString(R.string.stats_units_imperial);
    PreferencesUtils.setString(context, R.string.stats_units_key, imperialUnits);
    listener.onSharedPreferenceChanged(
        sharedPreferences, PreferencesUtils.getKey(context, R.string.stats_units_key));
    verifyAndReset();
  }

  /**
   * Replays mocks.
   */
  private void replay() {
    AndroidMock.replay(myTracksProviderUtils, dataSource, trackDataListener1, trackDataListener2);
  }

  /**
   * Verifies and resets mocks.
   */
  private void verifyAndReset() {
    AndroidMock.verify(myTracksProviderUtils, dataSource, trackDataListener1, trackDataListener2);
    AndroidMock.reset(myTracksProviderUtils, dataSource, trackDataListener1, trackDataListener2);
  }

  /**
   * Fixed size cursor answer.
   * 
   * @author Jimmy Shih
   */
  private static class FixedSizeCursorAnswer implements IAnswer<Cursor> {
    private final int size;

    public FixedSizeCursorAnswer(int size) {
      this.size = size;
    }

    @Override
    public Cursor answer() throws Throwable {
      MatrixCursor cursor = new MatrixCursor(new String[] { BaseColumns._ID });
      for (long i = 0; i < size; i++) {
        cursor.addRow(new Object[] { i });
      }
      return cursor;
    }
  }

  /**
   * Fixed size location iterator.
   * 
   * @author Jimmy Shih
   */
  private static class FixedSizeLocationIterator implements LocationIterator {
    private final long startId;
    private final Location[] locations;
    private final Set<Integer> splitIndexSet = new HashSet<Integer>();
    private int currentIndex = -1;

    public FixedSizeLocationIterator(long startId, int size) {
      this(startId, size, null);
    }

    public FixedSizeLocationIterator(long startId, int size, int... splitIndexes) {
      this.startId = startId;
      this.locations = new Location[size];

      for (int i = 0; i < size; i++) {
        Location location = new Location("gps");
        location.setLatitude(-15.0 + i / 1000.0);
        location.setLongitude(37 + i / 1000.0);
        location.setAltitude(i);
        locations[i] = location;
      }

      if (splitIndexes != null) {
        for (int splitIndex : splitIndexes) {
          splitIndexSet.add(splitIndex);

          Location splitLocation = locations[splitIndex];
          splitLocation.setLatitude(100.0);
          splitLocation.setLongitude(200.0);
        }
      }
    }

    public void expectLocationsDelivered(TrackDataListener listener) {
      for (int i = 0; i < locations.length; i++) {
        if (splitIndexSet.contains(i)) {
          listener.onSegmentSplit(locations[i]);
        } else {
          listener.onSampledInTrackPoint(locations[i]);
        }
      }
    }

    public void expectSampledLocationsDelivered(
        TrackDataListener listener, int sampleFrequency, boolean includeSampledOut) {
      boolean includeNext = false;
      for (int i = 0; i < locations.length; i++) {
        if (splitIndexSet.contains(i)) {
          listener.onSegmentSplit(locations[i]);
          includeNext = true;
        } else if (includeNext || (i % sampleFrequency == 0)) {
          listener.onSampledInTrackPoint(locations[i]);
          includeNext = false;
        } else if (includeSampledOut) {
          listener.onSampledOutTrackPoint(locations[i]);
        }
      }
    }

    @Override
    public boolean hasNext() {
      return currentIndex < locations.length - 1;
    }

    @Override
    public Location next() {
      currentIndex++;
      return locations[currentIndex];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getLocationId() {
      return startId + currentIndex;
    }

    @Override
    public void close() {
      // Do nothing
    }
  }

  /**
   * Tests the method {@link TrackDataHub#start()}.
   */
  public void testRegisterTracksTableListener() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(eq(TracksColumns.CONTENT_URI), capture(observerCapture));
    Track track = TrackStubUtils.createTrack(1);
    expect(myTracksProviderUtils.getTrack(capture(new Capture<Long>()))).andReturn(track);
    // Make the track id is unique.
    PreferencesUtils.setLong(context, R.string.recording_track_id_key, System.currentTimeMillis());
    trackDataListener1.onTrackUpdated(track);
    replay();
    trackDataHub.start();
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.TRACKS_TABLE));
    verifyAndReset();
  }

  /**
   * Tests the method {@link TrackDataHub#start()}.
   */
  public void testRegisterWaypointsTableListener() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    dataSource.registerContentObserver(eq(WaypointsColumns.CONTENT_URI), capture(observerCapture));
    expect(myTracksProviderUtils.getWaypointCursor(
        capture(new Capture<Long>()), capture(new Capture<Long>()),
        capture(new Capture<Integer>()))).andReturn(null);
    trackDataListener1.clearWaypoints();
    trackDataListener1.onNewWaypointsDone();
    replay();
    trackDataHub.start();
    trackDataHub.registerTrackDataListener(
        trackDataListener1, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
    verifyAndReset();
  }

  /**
   * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
   * the key is R.string.min_required_accuracy_key.
   */
  public void testNotifyPreferenceChanged_minRequiredAccuracy() {
    int value = 1;
    PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, value);
    trackDataHub.notifyPreferenceChanged(PreferencesUtils
        .getKey(context, R.string.recording_gps_accuracy_key));
    assertEquals(value, trackDataHub.getRecordingGpsAccuracy());
    PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, value + 1);
    trackDataHub.notifyPreferenceChanged(PreferencesUtils
        .getKey(context, R.string.recording_gps_accuracy_key));
    assertEquals(value + 1, trackDataHub.getRecordingGpsAccuracy());
  }
  
  /**
   * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
   * the key is R.string.metric_units_key.
   */
  public void testNotifyPreferenceChanged_metricUnitsNoNotify() {
    String imperialUnits = context.getString(R.string.stats_units_imperial);

    PreferencesUtils.setString(context, R.string.stats_units_key, imperialUnits);
    trackDataHub.notifyPreferenceChanged(
        PreferencesUtils.getKey(context, R.string.stats_units_key));
    assertEquals(false, trackDataHub.isMetricUnits());
    PreferencesUtils.setString(
        context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    trackDataHub.notifyPreferenceChanged(
        PreferencesUtils.getKey(context, R.string.stats_units_key));
    assertEquals(true, trackDataHub.isMetricUnits());
  }
  
  
  /**
   * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
   * the key is R.string.metric_units_key.
   */
  public void testNotifyPreferenceChanged_reportSpeedNoNotify() {
    boolean value = false;
    PreferencesUtils.setBoolean(context, R.string.report_speed_key, value);
    trackDataHub.notifyPreferenceChanged(PreferencesUtils
        .getKey(context, R.string.report_speed_key));
    assertEquals(value, trackDataHub.isReportSpeed());
    PreferencesUtils.setBoolean(context, R.string.report_speed_key, !value);
    trackDataHub.notifyPreferenceChanged(PreferencesUtils
        .getKey(context, R.string.report_speed_key));
    assertEquals(!value, trackDataHub.isReportSpeed());
  }
}
