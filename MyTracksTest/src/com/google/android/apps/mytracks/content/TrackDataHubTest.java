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
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.DataSourcesWrapper;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener.ProviderState;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;

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

import org.easymock.Capture;
import org.easymock.IAnswer;

/**
 * Tests for {@link TrackDataHub}.
 *
 * @author Rodrigo Damazio
 */
public class TrackDataHubTest extends AndroidTestCase {

  private static final long TRACK_ID = 42;
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

    prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    providerUtils = AndroidMock.createMock("providerUtils", MyTracksProviderUtils.class);
    dataSources = AndroidMock.createNiceMock("dataSources", DataSourcesWrapper.class);

    listeners = new TrackDataListeners();
    hub = new TrackDataHub(context, dataSources, listeners, prefs, providerUtils) {
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

    listener1 = AndroidMock.createMock("listener1", TrackDataListener.class);
    listener2 = AndroidMock.createMock("listener2", TrackDataListener.class);
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
    hub.destroy();

    super.tearDown();
  }

  public void testTrackListen() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    Track track = new Track();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).commit();
    expect(providerUtils.getTrack(TRACK_ID)).andStubReturn(track);
    expectStart();
    dataSources.registerContentObserver(
        eq(TracksColumns.CONTENT_URI), eq(false), capture(observerCapture));

    // Expect the initial loading.
    // Both listeners (registered before and after start) should get the same data.
    listener1.onTrackUpdated(track);
    listener2.onTrackUpdated(track);

    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.TRACK_UPDATES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.TRACK_UPDATES));

    AndroidMock.verify(listener1, listener2, dataSources);
    AndroidMock.reset(listener1, listener2, dataSources);

    ContentObserver observer = observerCapture.getValue();

    // Now expect an update.
    listener1.onTrackUpdated(track);
    listener2.onTrackUpdated(track);

    AndroidMock.replay(listener1, listener2, dataSources);

    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources);
    AndroidMock.reset(listener1, listener2, dataSources);

    // Unregister one, get another update.
    listener2.onTrackUpdated(track);

    AndroidMock.replay(listener1, listener2, dataSources);

    hub.unregisterTrackDataListener(listener1);

    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources);
    AndroidMock.reset(listener1, listener2, dataSources);

    // Unregister the other, expect internal unregistration
    dataSources.unregisterContentObserver(observer);

    AndroidMock.replay(listener1, listener2, dataSources);

    hub.unregisterTrackDataListener(listener2);
    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources);
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

  public void testWaypointListen() {
    Capture<ContentObserver> observerCapture = new Capture<ContentObserver>();
    prefs.edit().putLong("recordingTrack", TRACK_ID)
                .putLong("selectedTrack", TRACK_ID).commit();

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

    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.WAYPOINT_UPDATES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.WAYPOINT_UPDATES));

    AndroidMock.verify(listener1, listener2, dataSources, providerUtils);
    AndroidMock.reset(listener1, listener2, dataSources, providerUtils);

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

    AndroidMock.replay(listener1, listener2, dataSources, providerUtils);

    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources, providerUtils);
    AndroidMock.reset(listener1, listener2, dataSources, providerUtils);

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

    AndroidMock.replay(listener1, listener2, dataSources, providerUtils);

    hub.unregisterTrackDataListener(listener1);

    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources, providerUtils);
    AndroidMock.reset(listener1, listener2, dataSources, providerUtils);

    // Unregister the other, expect internal unregistration
    dataSources.unregisterContentObserver(observer);

    AndroidMock.replay(listener1, listener2, dataSources, providerUtils);

    hub.unregisterTrackDataListener(listener2);
    observer.onChange(false);

    AndroidMock.verify(listener1, listener2, dataSources, providerUtils);
  }

  public void testPointsListen() {
    // TODO
  }

  public void testPointsListen_resample() {
    // TODO
  }

  public void testLocationListen() {
    // TODO
  }

  public void testCompassListen() throws Exception {
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

    AndroidMock.replay(dataSources, providerUtils, listener1);

    hub.registerTrackDataListener(listener1,
        EnumSet.of(ListenerDataType.COMPASS_UPDATES, ListenerDataType.LOCATION_UPDATES));
    hub.start();

    SensorEventListener sensorListener = listenerCapture.getValue();
    LocationListener locationListener = locationListenerCapture.getValue();
    event.values[0] = 42.0f;
    sensorListener.onSensorChanged(event);

    AndroidMock.verify(dataSources, providerUtils, listener1);
    AndroidMock.reset(dataSources, providerUtils, listener1);

    // Expect the heading update to include declination.
    listener1.onCurrentHeadingChanged(52.0);

    // Also expect location updates which are not relevant to us.
    listener1.onProviderStateChange(isA(ProviderState.class));
    AndroidMock.expectLastCall().anyTimes();
    listener1.onCurrentLocationChanged(isA(Location.class));
    AndroidMock.expectLastCall().anyTimes();

    AndroidMock.replay(dataSources, providerUtils, listener1);

    // Now try injecting a location update, triggering a declination update.
    Location location = new Location("gps");
    location.setLatitude(10.0);
    location.setLongitude(20.0);
    location.setAltitude(30.0);
    declination = 10.0f;
    locationListener.onLocationChanged(location);
    sensorListener.onSensorChanged(event);

    AndroidMock.verify(dataSources, providerUtils, listener1);
    AndroidMock.reset(dataSources, providerUtils, listener1);

    listener1.onCurrentHeadingChanged(52.0);

    AndroidMock.replay(dataSources, providerUtils, listener1);

    // Now try changing the known declination - it should still return the old declination, since
    // updates only happen sparsely.
    declination = 20.0f;
    sensorListener.onSensorChanged(event);

    AndroidMock.verify(dataSources, providerUtils, listener1);
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
        .commit();

    Capture<OnSharedPreferenceChangeListener> listenerCapture =
        new Capture<OnSharedPreferenceChangeListener>();
    dataSources.registerOnSharedPreferenceChangeListener(capture(listenerCapture));

    expect(listener1.onUnitsChanged(true)).andReturn(false);
    expect(listener2.onUnitsChanged(true)).andReturn(false);
    expect(listener1.onReportSpeedChanged(true)).andReturn(false);
    expect(listener2.onReportSpeedChanged(true)).andReturn(false);

    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);

    hub.registerTrackDataListener(listener1, EnumSet.of(ListenerDataType.DISPLAY_PREFERENCES));
    hub.start();
    hub.registerTrackDataListener(listener2, EnumSet.of(ListenerDataType.DISPLAY_PREFERENCES));

    AndroidMock.verify(dataSources, providerUtils, listener1, listener2);
    AndroidMock.reset(dataSources, providerUtils, listener1, listener2);

    expect(listener1.onReportSpeedChanged(false)).andReturn(false);
    expect(listener2.onReportSpeedChanged(false)).andReturn(false);

    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);

    prefs.edit()
        .putBoolean(speedKey, false)
        .commit();
    OnSharedPreferenceChangeListener listener = listenerCapture.getValue();
    listener.onSharedPreferenceChanged(prefs, speedKey);

    AndroidMock.verify(dataSources, providerUtils, listener1, listener2);
    AndroidMock.reset(dataSources, providerUtils, listener1, listener2);

    expect(listener1.onUnitsChanged(false)).andReturn(false);
    expect(listener2.onUnitsChanged(false)).andReturn(false);

    AndroidMock.replay(dataSources, providerUtils, listener1, listener2);

    prefs.edit()
        .putBoolean(metricUnitsKey, false)
        .commit();
    listener.onSharedPreferenceChanged(prefs, metricUnitsKey);

    AndroidMock.verify(dataSources, providerUtils, listener1, listener2);
  }

  public void testFullListener() {
    // TODO: test loading a track, getting updates, loading another, unloading
  }

  private void expectStart() {
    dataSources.registerOnSharedPreferenceChangeListener(capture(preferenceListenerCapture));
  }
}
