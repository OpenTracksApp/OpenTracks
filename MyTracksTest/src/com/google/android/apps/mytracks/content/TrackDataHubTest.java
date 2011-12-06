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

import static com.google.android.testing.mocking.AndroidMock.anyInt;
import static com.google.android.testing.mocking.AndroidMock.capture;
import static com.google.android.testing.mocking.AndroidMock.eq;
import static com.google.android.testing.mocking.AndroidMock.expect;
import static com.google.android.testing.mocking.AndroidMock.isA;
import static com.google.android.testing.mocking.AndroidMock.leq;
import static com.google.android.testing.mocking.AndroidMock.same;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener.ProviderState;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.lang.reflect.Constructor;
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

  private MyTracksProviderUtils providerUtils;
  private TrackDataHub hub;
  private TrackDataListeners listeners;
  private DataSourcesWrapper dataSources;
  private SharedPreferences prefs;
  private TrackDataListener listener1;
  private TrackDataListener listener2;
  private Capture<OnSharedPreferenceChangeListener> preferenceListenerCapture =
      new Capture<SharedPreferences.OnSharedPreferenceChangeListener>();
  private MockContext context;
  private float declination;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    context = new MockContext(mockContentResolver, targetContext);

    prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    providerUtils = AndroidMock.createMock("providerUtils", MyTracksProviderUtils.class);
    dataSources = AndroidMock.createNiceMock("dataSources", DataSourcesWrapper.class);

    listeners = new TrackDataListeners();
    hub = new TrackDataHub(context, listeners, prefs, providerUtils, TARGET_POINTS) {
      @Override
      protected DataSourcesWrapper newDataSources() {
        return dataSources;
      }

      @Override
      protected void runInListenerThread(Runnable runnable) {
        // Run everything in the same thread.
        runnable.run();
      }

      @Override
      protected float getDeclinationFor(Location location, long timestamp) {
        return declination;
      }
    };

    listener1 = AndroidMock.createStrictMock("listener1", TrackDataListener.class);
    listener2 = AndroidMock.createStrictMock("listener2", TrackDataListener.class);
  }

  @Override
  protected void tearDown() throws Exception {
    AndroidMock.reset(dataSources);

    // Expect everything to be unregistered.
    if (preferenceListenerCapture.hasCaptured()) {
      dataSources.unregisterOnSharedPreferenceChangeListener(preferenceListenerCapture.getValue());
    }
    dataSources.removeLocationUpdates(isA(LocationListener.class));
    dataSources.unregisterSensorListener(isA(SensorEventListener.class));
    dataSources.unregisterContentObserver(isA(ContentObserver.class));
    AndroidMock.expectLastCall().times(3);

    AndroidMock.replay(dataSources);

    hub.stop();
    hub = null;

    super.tearDown();
  }

  public void testTrackListen() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    Track track = new Track();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();
    expect(providerUtils.getTrack(TRACK_ID)).andStubReturn(track);
    expectStart();
    dataSources.registerContentObserver(
        eq(TracksColumns.CONTENT_URI), eq(false), capture(observerCapture));

    // Expect the initial loading.
    // Both listeners (registered before and after start) should get the same data.
    listener1.onTrackUpdated(track);
    listener2.onTrackUpdated(track);

    replay();

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.TRACK_UPDATES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.TRACK_UPDATES));

    verifyAndReset();

    ContentObserver observer = observerCapture.getValue();
    expect(providerUtils.getTrack(TRACK_ID)).andStubReturn(track);

    // Now expect an update.
    listener1.onTrackUpdated(track);
    listener2.onTrackUpdated(track);

    replay();

    observer.onChange(false);

    verifyAndReset();

    // Unregister one, get another update.
    expect(providerUtils.getTrack(TRACK_ID)).andStubReturn(track);
    listener2.onTrackUpdated(track);

    replay();

    hub.unregisterTrackDataListener(listener1);

    observer.onChange(false);

    verifyAndReset();

    // Unregister the other, expect internal unregistration
    dataSources.unregisterContentObserver(observer);

    replay();

    hub.unregisterTrackDataListener(listener2);
    observer.onChange(false);

    verifyAndReset();
  }

  private static class FixedSizeCursorAnswer implements IAnswer<Cursor> {
    private final int size;

    public FixedSizeCursorAnswer(int size) {
      this.size = size;
    }

    @Override
    public Cursor answer() throws Throwable {
      MatrixCursor cursor = new MatrixCursor(new String[] { BaseColumns._ID });
      for (long i = 1; i <= size; i++) {
        cursor.addRow(new Object[] { i });
      }
      return cursor;
    }
  }

  private static class FixedSizeLocationIterator implements LocationIterator {
    private final long startId;
    private final Location[] locs;
    private final Set<Integer> splitIndexSet = new HashSet<Integer>();
    private int currentIdx = -1;

    public FixedSizeLocationIterator(long startId, int size) {
      this(startId, size, null);
    }

    public FixedSizeLocationIterator(long startId, int size, int... splitIndices) {
      this.startId = startId;
      this.locs = new Location[size];

      for (int i = 0; i < size; i++) {
        Location loc = new Location("gps");
        loc.setLatitude(-15.0 + i / 1000.0);
        loc.setLongitude(37 + i / 1000.0);
        loc.setAltitude(i);

        locs[i] = loc;
      }

      if (splitIndices != null) {
        for (int splitIdx : splitIndices) {
          splitIndexSet.add(splitIdx);

          Location splitLoc = locs[splitIdx];
          splitLoc.setLatitude(100.0);
          splitLoc.setLongitude(200.0);
        }
      }
    }

    public void expectLocationsDelivered(TrackDataListener listener) {
      for (int i = 0; i < locs.length; i++) {
        if (splitIndexSet.contains(i)) {
          listener.onSegmentSplit();
        } else {
          listener.onNewTrackPoint(locs[i]);
        }
      }
    }

    public void expectSampledLocationsDelivered(
        TrackDataListener listener, int sampleFrequency, boolean includeSampledOut) {
      for (int i = 0; i < locs.length; i++) {
        if (splitIndexSet.contains(i)) {
          listener.onSegmentSplit();
        } else if (i % sampleFrequency == 0) {
          listener.onNewTrackPoint(locs[i]);
        } else if (includeSampledOut) {
          listener.onSampledOutTrackPoint(locs[i]);
        }
      }
    }

    @Override
    public boolean hasNext() {
      return currentIdx < (locs.length - 1);
    }

    @Override
    public Location next() {
      currentIdx++;
      return locs[currentIdx];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getLocationId() {
      return startId + currentIdx;
    }

    @Override
    public void close() {
      // Do nothing
    }
  }

  public void testWaypointListen() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    Waypoint wpt1 = new Waypoint(),
             wpt2 = new Waypoint(),
             wpt3 = new Waypoint(),
             wpt4 = new Waypoint();
    Location loc = new Location("gps");
    loc.setLatitude(10.0);
    loc.setLongitude(8.0);
    wpt1.setLocation(loc);
    wpt2.setLocation(loc);
    wpt3.setLocation(loc);
    wpt4.setLocation(loc);

    expect(providerUtils.getWaypointsCursor(
        eq(TRACK_ID), leq(0L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(2));
    expect(providerUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(wpt1)
        .andReturn(wpt2)
        .andReturn(wpt1)
        .andReturn(wpt2);

    expectStart();
    dataSources.registerContentObserver(
        eq(WaypointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    // Expect the initial loading.
    // Both listeners (registered before and after start) should get the same data.
    listener1.clearWaypoints();
    listener1.onNewWaypoint(wpt1);
    listener1.onNewWaypoint(wpt2);
    listener1.onNewWaypointsDone();
    listener2.clearWaypoints();
    listener2.onNewWaypoint(wpt1);
    listener2.onNewWaypoint(wpt2);
    listener2.onNewWaypointsDone();

    replay();

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.WAYPOINT_UPDATES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.WAYPOINT_UPDATES));

    verifyAndReset();

    ContentObserver observer = observerCapture.getValue();

    expect(providerUtils.getWaypointsCursor(
        eq(TRACK_ID), leq(0L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(3));
    expect(providerUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(wpt1)
        .andReturn(wpt2)
        .andReturn(wpt3);

    // Now expect an update.
    listener1.clearWaypoints();
    listener2.clearWaypoints();
    listener1.onNewWaypoint(wpt1);
    listener2.onNewWaypoint(wpt1);
    listener1.onNewWaypoint(wpt2);
    listener2.onNewWaypoint(wpt2);
    listener1.onNewWaypoint(wpt3);
    listener2.onNewWaypoint(wpt3);
    listener1.onNewWaypointsDone();
    listener2.onNewWaypointsDone();

    replay();

    observer.onChange(false);

    verifyAndReset();

    // Unregister one, get another update.
    expect(providerUtils.getWaypointsCursor(
        eq(TRACK_ID), leq(0L), eq(Constants.MAX_DISPLAYED_WAYPOINTS_POINTS)))
        .andStubAnswer(new FixedSizeCursorAnswer(4));
    expect(providerUtils.createWaypoint(isA(Cursor.class)))
        .andReturn(wpt1)
        .andReturn(wpt2)
        .andReturn(wpt3)
        .andReturn(wpt4);

    // Now expect an update.
    listener2.clearWaypoints();
    listener2.onNewWaypoint(wpt1);
    listener2.onNewWaypoint(wpt2);
    listener2.onNewWaypoint(wpt3);
    listener2.onNewWaypoint(wpt4);
    listener2.onNewWaypointsDone();

    replay();

    hub.unregisterTrackDataListener(listener1);

    observer.onChange(false);

    verifyAndReset();

    // Unregister the other, expect internal unregistration
    dataSources.unregisterContentObserver(observer);

    replay();

    hub.unregisterTrackDataListener(listener2);
    observer.onChange(false);

    verifyAndReset();
  }

  public void testPointsListen() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    expectStart();
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(10L);

    listener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.start();
    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Register a second listener - it will get the same points as the previous one
    locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(10L);

    listener2.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener2);
    listener2.onNewTrackPointsDone();

    replay();

    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Deliver more points - should go to both listeners, without clearing.
    ContentObserver observer = observerCapture.getValue();

    locationIterator = new FixedSizeLocationIterator(11, 10, 1);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(11L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(20L);

    locationIterator.expectLocationsDelivered(listener1);
    locationIterator.expectLocationsDelivered(listener2);
    listener1.onNewTrackPointsDone();
    listener2.onNewTrackPointsDone();

    replay();

    observer.onChange(false);

    verifyAndReset();

    // Unregister listener1, switch tracks to ensure data is cleared/reloaded.
    locationIterator = new FixedSizeLocationIterator(101, 10);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID + 1)).andReturn(110L);

    listener2.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener2);
    listener2.onNewTrackPointsDone();

    replay();

    hub.unregisterTrackDataListener(listener1);
    hub.loadTrack(TRACK_ID + 1);

    verifyAndReset();
  }

  public void testPointsListen_beforeStart() {
    
  }

  public void testPointsListen_reRegister() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    expectStart();
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(10L);

    listener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.start();
    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Unregister
    ContentObserver observer = observerCapture.getValue();
    dataSources.unregisterContentObserver(observer);

    replay();

    hub.unregisterTrackDataListener(listener1);

    verifyAndReset();

    // Register again, except only points since unregistered.
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    locationIterator = new FixedSizeLocationIterator(11, 10);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(11L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(20L);

    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Deliver more points - should still be incremental.
    locationIterator = new FixedSizeLocationIterator(21, 10, 1);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(21L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(30L);

    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    observer.onChange(false);

    verifyAndReset();
  }

  public void testPointsListen_reRegisterTrackChanged() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    expectStart();
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(10L);

    listener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.start();
    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Unregister
    ContentObserver observer = observerCapture.getValue();
    dataSources.unregisterContentObserver(observer);

    replay();

    hub.unregisterTrackDataListener(listener1);

    verifyAndReset();

    // Register again after track changed, expect all points.
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    locationIterator = new FixedSizeLocationIterator(1, 10);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID + 1)).andReturn(10L);

    listener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.loadTrack(TRACK_ID + 1);
    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();
  }

  public void testPointsListen_largeTrackSampling() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    expectStart();
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 200, 4, 25, 71, 120);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(200L);

    listener1.clearTrackPoints();
    listener2.clearTrackPoints();
    locationIterator.expectSampledLocationsDelivered(listener1, 4, false);
    locationIterator.expectSampledLocationsDelivered(listener2, 4, true);
    listener1.onNewTrackPointsDone();
    listener2.onNewTrackPointsDone();

    replay();

    hub.registerTrackDataListener(listener1,
        EnumSet.of(ListenerDataType.POINT_UPDATES));
    hub.registerTrackDataListener(listener2,
        EnumSet.of(ListenerDataType.POINT_UPDATES, ListenerDataType.SAMPLED_OUT_POINT_UPDATES));
    hub.start();

    verifyAndReset();
  }

  public void testPointsListen_resampling() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).apply();

    expectStart();
    dataSources.registerContentObserver(
        eq(TrackPointsColumns.CONTENT_URI), eq(false), capture(observerCapture));

    // Deliver 30 points (no sampling happens)
    FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 30, 5);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(30L);

    listener1.clearTrackPoints();
    locationIterator.expectLocationsDelivered(listener1);
    listener1.onNewTrackPointsDone();

    replay();

    hub.start();
    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.POINT_UPDATES));

    verifyAndReset();

    // Now deliver 30 more (incrementally sampled)
    ContentObserver observer = observerCapture.getValue();
    locationIterator = new FixedSizeLocationIterator(31, 30);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(31L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(60L);

    locationIterator.expectSampledLocationsDelivered(listener1, 2, false);
    listener1.onNewTrackPointsDone();

    replay();

    observer.onChange(false);

    verifyAndReset();

    // Now another 30 (triggers resampling)
    locationIterator = new FixedSizeLocationIterator(1, 90);
    expect(providerUtils.getLocationIterator(
        eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class)))
        .andReturn(locationIterator);
    expect(providerUtils.getLastLocationId(TRACK_ID)).andReturn(90L);

    listener1.clearTrackPoints();
    locationIterator.expectSampledLocationsDelivered(listener1, 2, false);
    listener1.onNewTrackPointsDone();

    replay();

    observer.onChange(false);

    verifyAndReset();
  }

  public void testLocationListen() {
    // TODO
  }

  public void testCompassListen() throws Exception {
    AndroidMock.resetToDefault(listener1);

    Sensor compass = newSensor();
    expect(dataSources.getSensor(Sensor.TYPE_ORIENTATION)).andReturn(compass);
    Capture<SensorEventListener> listenerCapture = new Capture<SensorEventListener>();
    dataSources.registerSensorListener(capture(listenerCapture), same(compass), anyInt());

    Capture<LocationListener> locationListenerCapture = new Capture<LocationListener>();
    dataSources.requestLocationUpdates(capture(locationListenerCapture));

    SensorEvent event = newSensorEvent();
    event.sensor = compass;

    // First, get a dummy heading update.
    listener1.onCurrentHeadingChanged(0.0);

    // Then, get a heading update without a known location (thus can't calculate declination).
    listener1.onCurrentHeadingChanged(42.0f);

    // Also expect location updates which are not relevant to us.
    listener1.onProviderStateChange(isA(ProviderState.class));
    AndroidMock.expectLastCall().anyTimes();

    replay();

    hub.registerTrackDataListener(listener1,
        EnumSet.of(ListenerDataType.COMPASS_UPDATES, ListenerDataType.LOCATION_UPDATES));
    hub.start();

    SensorEventListener sensorListener = listenerCapture.getValue();
    LocationListener locationListener = locationListenerCapture.getValue();
    event.values[0] = 42.0f;
    sensorListener.onSensorChanged(event);

    verifyAndReset();

    // Expect the heading update to include declination.
    listener1.onCurrentHeadingChanged(52.0);

    // Also expect location updates which are not relevant to us.
    listener1.onProviderStateChange(isA(ProviderState.class));
    AndroidMock.expectLastCall().anyTimes();
    listener1.onCurrentLocationChanged(isA(Location.class));
    AndroidMock.expectLastCall().anyTimes();

    replay();

    // Now try injecting a location update, triggering a declination update.
    Location location = new Location("gps");
    location.setLatitude(10.0);
    location.setLongitude(20.0);
    location.setAltitude(30.0);
    declination = 10.0f;
    locationListener.onLocationChanged(location);
    sensorListener.onSensorChanged(event);

    verifyAndReset();

    listener1.onCurrentHeadingChanged(52.0);

    replay();

    // Now try changing the known declination - it should still return the old declination, since
    // updates only happen sparsely.
    declination = 20.0f;
    sensorListener.onSensorChanged(event);

    verifyAndReset();
  }

  private Sensor newSensor() throws Exception {
    Constructor<Sensor> constructor = Sensor.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private SensorEvent newSensorEvent() throws Exception {
    Constructor<SensorEvent> constructor = SensorEvent.class.getDeclaredConstructor(int.class);
    constructor.setAccessible(true);
    return constructor.newInstance(3);
  }

  public void testDisplayPreferencesListen() throws Exception {
    String metricUnitsKey = context.getString(R.string.metric_units_key);
    String speedKey = context.getString(R.string.report_speed_key);

    prefs.edit()
        .putBoolean(metricUnitsKey, true)
        .putBoolean(speedKey, true)
        .apply();

    Capture<OnSharedPreferenceChangeListener> listenerCapture =
        new Capture<OnSharedPreferenceChangeListener>();
    dataSources.registerOnSharedPreferenceChangeListener(capture(listenerCapture));

    expect(listener1.onUnitsChanged(true)).andReturn(false);
    expect(listener2.onUnitsChanged(true)).andReturn(false);
    expect(listener1.onReportSpeedChanged(true)).andReturn(false);
    expect(listener2.onReportSpeedChanged(true)).andReturn(false);

    replay();

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.DISPLAY_PREFERENCES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.DISPLAY_PREFERENCES));

    verifyAndReset();

    expect(listener1.onReportSpeedChanged(false)).andReturn(false);
    expect(listener2.onReportSpeedChanged(false)).andReturn(false);

    replay();

    prefs.edit()
        .putBoolean(speedKey, false)
        .apply();
    OnSharedPreferenceChangeListener listener = listenerCapture.getValue();
    listener.onSharedPreferenceChanged(prefs, speedKey);

    AndroidMock.verify(dataSources, providerUtils, listener1, listener2);
    AndroidMock.reset(dataSources, providerUtils, listener1, listener2);

    expect(listener1.onUnitsChanged(false)).andReturn(false);
    expect(listener2.onUnitsChanged(false)).andReturn(false);

    replay();

    prefs.edit()
        .putBoolean(metricUnitsKey, false)
        .apply();
    listener.onSharedPreferenceChanged(prefs, metricUnitsKey);

    verifyAndReset();
  }

  private void expectStart() {
    dataSources.registerOnSharedPreferenceChangeListener(capture(preferenceListenerCapture));
  }

  private void replay() {
    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);
  }

  private void verifyAndReset() {
    AndroidMock.verify(listener1, listener2, dataSources, providerUtils);
    AndroidMock.reset(listener1, listener2, dataSources, providerUtils);
  }
}
