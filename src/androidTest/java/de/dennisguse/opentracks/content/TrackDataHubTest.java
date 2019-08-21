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
package de.dennisguse.opentracks.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.ContentProviderUtils.LocationFactory;
import de.dennisguse.opentracks.content.ContentProviderUtils.LocationIterator;
import de.dennisguse.opentracks.util.PreferencesUtils;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TrackDataHub}.
 *
 * @author Rodrigo Damazio
 */
@RunWith(MockitoJUnitRunner.class)
public class TrackDataHubTest {
    private static final long TRACK_ID = 42L;
    private static final int TARGET_POINTS = 50;

    private Context context = ApplicationProvider.getApplicationContext();
    private SharedPreferences sharedPreferences;

    @Mock
    private ContentProviderUtils contentProviderUtils;

    @Mock
    private DataSource dataSource;

    private TrackDataHub trackDataHub;

    @Mock
    private TrackDataListener trackDataListener1;

    @Mock
    private TrackDataListener trackDataListener2;
    private ArgumentCaptor<OnSharedPreferenceChangeListener> preferenceChangeListenerCapture = ArgumentCaptor.forClass(OnSharedPreferenceChangeListener.class);

    @Before
    public void setUp() {
        sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        trackDataHub = new TrackDataHub(context, new TrackDataManager(), contentProviderUtils, TARGET_POINTS) {
            @Override
            protected DataSource newDataSource() {
                return dataSource;
            }

            @Override
            protected void runInHandlerThread(Runnable runnable) {
                // Run everything in the same thread
                runnable.run();
            }
        };
        PreferencesUtils.setLong(context, R.string.recording_track_id_key, TRACK_ID);
    }

    @After
    public void tearDown() {
        reset(dataSource);

        // Expect everything to be unregistered.
        dataSource.unregisterContentObserver(isA(ContentObserver.class));
        //TODO
//        AndroidMock.expectLastCall().times(3);
        dataSource.unregisterOnSharedPreferenceChangeListener(isA(OnSharedPreferenceChangeListener.class));
        verify(dataSource);
        trackDataHub.stop();
        trackDataHub = null;
    }

    /**
     * Tests registering for tracks table update.
     */
    @Test
    public void testTracksTableUpdate() {
        // Register two listeners
        ArgumentCaptor<ContentObserver> contentObserverCapture = ArgumentCaptor.forClass(ContentObserver.class);
        Track track = new Track();
        when(contentProviderUtils.getTrack(TRACK_ID)).thenReturn(track);
        dataSource.registerContentObserver(eq(TracksColumns.CONTENT_URI), contentObserverCapture.capture());
        trackDataListener1.onTrackUpdated(track);
        trackDataListener2.onTrackUpdated(track);
        replay();

        trackDataHub.start();
        trackDataHub.loadTrack(TRACK_ID);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.TRACKS_TABLE));
        trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(TrackDataType.TRACKS_TABLE));
        verifyAndReset();

        // Causes tracks table update
        ContentObserver contentObserver = contentObserverCapture.getValue();
        when(contentProviderUtils.getTrack(TRACK_ID)).thenReturn(track);
        trackDataListener1.onTrackUpdated(track);
        trackDataListener2.onTrackUpdated(track);
        replay();

        contentObserver.onChange(false);
        verifyAndReset();

        // Unregister one listener
        when(contentProviderUtils.getTrack(TRACK_ID)).thenReturn(track);
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

    //TODO
    /**
     * Tests registering for waypoints table update.
     */
//    @Test
//    public void testWaypointsTableUpdate() {
//        Waypoint waypoint1 = new Waypoint();
//        Waypoint waypoint2 = new Waypoint();
//        Waypoint waypoint3 = new Waypoint();
//        Waypoint waypoint4 = new Waypoint();
//        Location location = new Location("gps");
//        location.setLatitude(10.0);
//        location.setLongitude(8.0);
//        waypoint1.setLocation(location);
//        waypoint2.setLocation(location);
//        waypoint3.setLocation(location);
//        waypoint4.setLocation(location);
//
//        // Register two listeners
//        ArgumentCaptor<ContentObserver> contentObserverCapture = ArgumentCaptor.forClass(ContentObserver.class);
//        when(contentProviderUtils.getWaypointCursor(
//                eq(TRACK_ID), AndroidMock.leq(-1L), eq(TrackDataHub.MAX_DISPLAYED_WAYPOINTS)))
//                .andStubAnswer(new FixedSizeCursorAnswer(2));
//        when(contentProviderUtils.createWaypoint(isA(Cursor.class)))
//                .thenReturn(waypoint1)
//                .thenReturn(waypoint2)
//                .thenReturn(waypoint1)
//                .thenReturn(waypoint2);
//        dataSource.registerContentObserver(eq(WaypointsColumns.CONTENT_BASE_URI), contentObserverCapture.capture());
//        trackDataListener1.clearWaypoints();
//        trackDataListener2.clearWaypoints();
//        trackDataListener1.onNewWaypoint(waypoint1);
//        trackDataListener2.onNewWaypoint(waypoint1);
//        trackDataListener1.onNewWaypoint(waypoint2);
//        trackDataListener2.onNewWaypoint(waypoint2);
//        trackDataListener1.onNewWaypointsDone();
//        trackDataListener2.onNewWaypointsDone();
//        replay();
//
//        trackDataHub.start();
//        trackDataHub.loadTrack(TRACK_ID);
//        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
//        trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
//        verifyAndReset();
//
//        // Cause waypoints table update
//        ContentObserver contentObserver = contentObserverCapture.getValue();
//        when(contentProviderUtils.getWaypointCursor(
//                eq(TRACK_ID), AndroidMock.leq(-1L), eq(TrackDataHub.MAX_DISPLAYED_WAYPOINTS)))
//                .andStubAnswer(new FixedSizeCursorAnswer(3));
//        when(contentProviderUtils.createWaypoint(isA(Cursor.class)))
//                .thenReturn(waypoint1)
//                .thenReturn(waypoint2)
//                .thenReturn(waypoint3);
//        trackDataListener1.clearWaypoints();
//        trackDataListener2.clearWaypoints();
//        trackDataListener1.onNewWaypoint(waypoint1);
//        trackDataListener2.onNewWaypoint(waypoint1);
//        trackDataListener1.onNewWaypoint(waypoint2);
//        trackDataListener2.onNewWaypoint(waypoint2);
//        trackDataListener1.onNewWaypoint(waypoint3);
//        trackDataListener2.onNewWaypoint(waypoint3);
//        trackDataListener1.onNewWaypointsDone();
//        trackDataListener2.onNewWaypointsDone();
//        replay();
//
//        contentObserver.onChange(false);
//        verifyAndReset();
//
//        // Unregister one listener
//        when(contentProviderUtils.getWaypointCursor(
//                eq(TRACK_ID), AndroidMock.leq(-1L), eq(TrackDataHub.MAX_DISPLAYED_WAYPOINTS)))
//                .andStubAnswer(new FixedSizeCursorAnswer(4));
//        when(contentProviderUtils.createWaypoint(isA(Cursor.class)))
//                .thenReturn(waypoint1)
//                .thenReturn(waypoint2)
//                .thenReturn(waypoint3)
//                .thenReturn(waypoint4);
//        trackDataListener2.clearWaypoints();
//        trackDataListener2.onNewWaypoint(waypoint1);
//        trackDataListener2.onNewWaypoint(waypoint2);
//        trackDataListener2.onNewWaypoint(waypoint3);
//        trackDataListener2.onNewWaypoint(waypoint4);
//        trackDataListener2.onNewWaypointsDone();
//        replay();
//
//        trackDataHub.unregisterTrackDataListener(trackDataListener1);
//        contentObserver.onChange(false);
//        verifyAndReset();
//
//        // Unregister the second listener
//        dataSource.unregisterContentObserver(contentObserver);
//        replay();
//
//        trackDataHub.unregisterTrackDataListener(trackDataListener2);
//        contentObserver.onChange(false);
//        verifyAndReset();
//    }

    /**
     * Tests track points table update.
     */
    @Test
    public void testTrackPointsTableUpdate() {
        // Register one listener
        ArgumentCaptor<ContentObserver> contentObserverCapture = ArgumentCaptor.forClass(ContentObserver.class);
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), contentObserverCapture.capture());

        FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(10L);
        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.start();
        trackDataHub.loadTrack(TRACK_ID);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();

        // Register a second listener
        locationIterator = new FixedSizeLocationIterator(1, 10, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(10L);
        trackDataListener2.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener2);
        trackDataListener2.onNewTrackPointsDone();
        replay();

        trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();

        // Deliver more points - should go to both listeners without clearing
        ContentObserver contentObserver = contentObserverCapture.getValue();
        locationIterator = new FixedSizeLocationIterator(11, 10, 1);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(11L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(20L);
        locationIterator.expectLocationsDelivered(trackDataListener1);
        locationIterator.expectLocationsDelivered(trackDataListener2);
        trackDataListener1.onNewTrackPointsDone();
        trackDataListener2.onNewTrackPointsDone();
        replay();

        contentObserver.onChange(false);
        verifyAndReset();

        // Unregister one listener and change track
        locationIterator = new FixedSizeLocationIterator(101, 10);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
                .thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID + 1)).thenReturn(110L);
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
    @Test
    public void testTrackPointsTableUpdate_reRegister() {
        // Register one listener
        ArgumentCaptor<ContentObserver> contentObserverCapture = ArgumentCaptor.forClass(ContentObserver.class);
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), contentObserverCapture.capture());

        FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(
                eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(10L);

        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.start();
        trackDataHub.loadTrack(TRACK_ID);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();

        // Unregister the listener
        ContentObserver observer = contentObserverCapture.getValue();
        dataSource.unregisterContentObserver(observer);
        replay();

        trackDataHub.unregisterTrackDataListener(trackDataListener1);
        verifyAndReset();

        // Register again
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), contentObserverCapture.capture());
        locationIterator = new FixedSizeLocationIterator(1, 10, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(10L);
        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();
    }

    /**
     * Tests tracks point able change. Register a listener after a track change.
     */
    @Test
    public void testTrackPointsTableUpdate_reRegisterAfterTrackChange() {
        // Register one listener
        ArgumentCaptor<ContentObserver> observerCapture = ArgumentCaptor.forClass(ContentObserver.class);
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), observerCapture.capture());

        FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 10, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(
                eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(10L);
        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.start();
        trackDataHub.loadTrack(TRACK_ID);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();

        // Unregister the listener
        ContentObserver observer = observerCapture.getValue();
        dataSource.unregisterContentObserver(observer);
        replay();

        trackDataHub.unregisterTrackDataListener(trackDataListener1);
        verifyAndReset();

        // Register the listener after a new track
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), observerCapture.capture());
        locationIterator = new FixedSizeLocationIterator(1, 10);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID + 1), eq(0L), eq(false), isA(LocationFactory.class)))
                .thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID + 1)).thenReturn(10L);
        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.loadTrack(TRACK_ID + 1);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();
    }

    //TODO
    /**
     * Tests track points table update with large track sampling.
     */
//    @Test
//    public void testTrackPointsTableUpdate_largeTrackSampling() {
//        ArgumentCaptor<ContentObserver> contentObserverCapture = ArgumentCaptor.forClass(ContentObserver.class);
//        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_BASE_URI), contentObserverCapture.capture());
//
//        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(200L);
//        AndroidMock.expectLastCall().anyTimes();
//        FixedSizeLocationIterator locationIterator1 = new FixedSizeLocationIterator(1, 200, 4, 25, 71, 120);
//        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator1);
//        FixedSizeLocationIterator locationIterator2 = new FixedSizeLocationIterator(1, 200, 4, 25, 71, 120);
//        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator2);
//
//        trackDataListener1.clearTrackPoints();
//        locationIterator1.expectSampledLocationsDelivered(trackDataListener1, 4, false);
//        trackDataListener1.onNewTrackPointsDone();
//        trackDataListener2.clearTrackPoints();
//        locationIterator2.expectSampledLocationsDelivered(trackDataListener2, 4, true);
//        trackDataListener2.onNewTrackPointsDone();
//        replay();
//
//        trackDataHub.start();
//        trackDataHub.loadTrack(TRACK_ID);
//        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
//        trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE, TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE));
//        verifyAndReset();
//    }

    /**
     * Tests track points table update with resampling.
     */
    @Test
    public void testTrackPointsTableUpdate_resampling() {
        ArgumentCaptor<ContentObserver> observerCapture = ArgumentCaptor.forClass(ContentObserver.class);
        dataSource.registerContentObserver(eq(TrackPointsColumns.CONTENT_URI), observerCapture.capture());

        // Deliver 30 points (no sampling happens)
        FixedSizeLocationIterator locationIterator = new FixedSizeLocationIterator(1, 30, 5);
        when(contentProviderUtils.getTrackPointLocationIterator(
                eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(30L);

        trackDataListener1.clearTrackPoints();
        locationIterator.expectLocationsDelivered(trackDataListener1);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        trackDataHub.start();
        trackDataHub.loadTrack(TRACK_ID);
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE));
        verifyAndReset();

        // Now deliver 30 more (incrementally sampled)
        ContentObserver observer = observerCapture.getValue();
        locationIterator = new FixedSizeLocationIterator(31, 30);
        when(contentProviderUtils.getTrackPointLocationIterator(eq(TRACK_ID), eq(31L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(60L);
        locationIterator.expectSampledLocationsDelivered(trackDataListener1, 2, false);
        trackDataListener1.onNewTrackPointsDone();
        replay();

        observer.onChange(false);
        verifyAndReset();

        // Now another 30 (triggers resampling)
        locationIterator = new FixedSizeLocationIterator(1, 90);
        when(contentProviderUtils.getTrackPointLocationIterator(
                eq(TRACK_ID), eq(0L), eq(false), isA(LocationFactory.class))).thenReturn(locationIterator);
        when(contentProviderUtils.getLastTrackPointId(TRACK_ID)).thenReturn(90L);
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
    @Test
    public void testPreferencesChange() throws Exception {
        // Register two listeners
        PreferencesUtils.setString(context, R.string.stats_rate_key, PreferencesUtils.STATS_RATE_DEFAULT);
        PreferencesUtils.setString(context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
        PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT);
        PreferencesUtils.setInt(context, R.string.recording_distance_interval_key, PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);

        dataSource.registerOnSharedPreferenceChangeListener(preferenceChangeListenerCapture.capture());
        when(trackDataListener1.onMetricUnitsChanged(true)).thenReturn(false);
        when(trackDataListener1.onReportSpeedChanged(true)).thenReturn(false);
        when(trackDataListener1.onRecordingGpsAccuracy(PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT)).thenReturn(false);
        when(trackDataListener1.onRecordingDistanceIntervalChanged(PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT)).thenReturn(false);

        when(trackDataListener2.onMetricUnitsChanged(true)).thenReturn(false);
        when(trackDataListener2.onReportSpeedChanged(true)).thenReturn(false);
        when(trackDataListener2.onRecordingGpsAccuracy(PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT)).thenReturn(false);
        when(trackDataListener2.onRecordingDistanceIntervalChanged(PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT)).thenReturn(false);
        replay();

        trackDataHub.start();
        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.PREFERENCE));
        trackDataHub.registerTrackDataListener(trackDataListener2, EnumSet.of(TrackDataType.PREFERENCE));
        verifyAndReset();

        // Change report speed to false
        when(trackDataListener1.onReportSpeedChanged(false)).thenReturn(false);
        when(trackDataListener2.onReportSpeedChanged(false)).thenReturn(false);
        replay();

        PreferencesUtils.setString(context, R.string.stats_rate_key, context.getString(R.string.stats_rate_pace));
        OnSharedPreferenceChangeListener listener = preferenceChangeListenerCapture.getValue();
        listener.onSharedPreferenceChanged(sharedPreferences, PreferencesUtils.getKey(context, R.string.stats_rate_key));
        verifyAndReset();

        // Change metric units to false
        when(trackDataListener1.onMetricUnitsChanged(false)).thenReturn(false);
        when(trackDataListener2.onMetricUnitsChanged(false)).thenReturn(false);
        replay();

        String imperialUnits = context.getString(R.string.stats_units_imperial);
        PreferencesUtils.setString(context, R.string.stats_units_key, imperialUnits);
        listener.onSharedPreferenceChanged(sharedPreferences, PreferencesUtils.getKey(context, R.string.stats_units_key));
        verifyAndReset();
    }

    /**
     * Replays mocks.
     */
    private void replay() {
//        AndroidMock.replay(contentProviderUtils, dataSource, trackDataListener1, trackDataListener2);
    }

    /**
     * Verifies and resets mocks.
     */
    private void verifyAndReset() {
        verify(contentProviderUtils);
        verify(dataSource);
        verify(trackDataListener1);
        verify(trackDataListener2);
        reset(contentProviderUtils, dataSource, trackDataListener1, trackDataListener2);
    }

    //TODO
    /**
     * Tests the method {@link TrackDataHub#start()}.
     */
//    @Test
//    public void testRegisterTracksTableListener() {
//        ArgumentCaptor<ContentObserver> observerCapture = ArgumentCaptor.forClass(ContentObserver.class);
//        dataSource.registerContentObserver(eq(TracksColumns.CONTENT_BASE_URI), observerCapture.capture());
//        Track track = TrackStubUtils.createTrack(1);
//
//        when(contentProviderUtils.getTrack(capture(new Capture<Long>())).thenReturn(track);
//        // Make the track id is unique.
//        PreferencesUtils.setLong(context, R.string.recording_track_id_key, System.currentTimeMillis());
//        trackDataListener1.onTrackUpdated(track);
//        replay();
//        trackDataHub.start();
//        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.TRACKS_TABLE));
//        verifyAndReset();
//    }

    //TODO
    /**
     * Tests the method {@link TrackDataHub#start()}.
     */
//    @Test
//    public void testRegisterWaypointsTableListener() {
//        ArgumentCaptor<ContentObserver> observerCapture = ArgumentCaptor.forClass(ContentObserver.class);
//        dataSource.registerContentObserver(eq(WaypointsColumns.CONTENT_BASE_URI), observerCapture.capture());
//        when(contentProviderUtils.getWaypointCursor(
//                capture(new Capture<Long>()), capture(new Capture<Long>()),
//                capture(new Capture<Integer>()))).thenReturn(null);
//        trackDataListener1.clearWaypoints();
//        trackDataListener1.onNewWaypointsDone();
//        replay();
//        trackDataHub.start();
//        trackDataHub.registerTrackDataListener(trackDataListener1, EnumSet.of(TrackDataType.WAYPOINTS_TABLE));
//        verifyAndReset();
//    }

    /**
     * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
     * the key is R.string.min_required_accuracy_key.
     */
    @Test
    public void testNotifyPreferenceChanged_minRequiredAccuracy() {
        int value = 1;
        PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, value);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.recording_gps_accuracy_key));
        Assert.assertEquals(value, trackDataHub.getRecordingGpsAccuracy());
        PreferencesUtils.setInt(context, R.string.recording_gps_accuracy_key, value + 1);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.recording_gps_accuracy_key));
        Assert.assertEquals(value + 1, trackDataHub.getRecordingGpsAccuracy());
    }

    /**
     * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
     * the key is R.string.metric_units_key.
     */
    @Test
    public void testNotifyPreferenceChanged_metricUnitsNoNotify() {
        String imperialUnits = context.getString(R.string.stats_units_imperial);

        PreferencesUtils.setString(context, R.string.stats_units_key, imperialUnits);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.stats_units_key));
        Assert.assertFalse(trackDataHub.isMetricUnits());
        PreferencesUtils.setString(context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.stats_units_key));
        Assert.assertTrue(trackDataHub.isMetricUnits());
    }

    /**
     * Tests the method {@link TrackDataHub#notifyPreferenceChanged(String)} when
     * the key is R.string.metric_units_key.
     */
    @Test
    public void testNotifyPreferenceChanged_reportSpeedNoNotify() {
        String value = context.getString(R.string.stats_rate_pace);
        PreferencesUtils.setString(context, R.string.stats_rate_key, value);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.stats_rate_key));
        Assert.assertFalse(trackDataHub.isReportSpeed());

        value = context.getString(R.string.stats_rate_speed);
        PreferencesUtils.setString(context, R.string.stats_rate_key, value);
        trackDataHub.notifyPreferenceChanged(PreferencesUtils.getKey(context, R.string.stats_rate_key));
        Assert.assertTrue(trackDataHub.isReportSpeed());
    }

    //TODO
    /**
     * Fixed size cursor answer.
     *
     * @author Jimmy Shih
     */
    private static class FixedSizeCursorAnswer {
        private final int size;

        public FixedSizeCursorAnswer(int size) {
            this.size = size;
        }
//
//        @Override
//        public Cursor answer() throws Throwable {
//            MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID});
//            for (long i = 0; i < size; i++) {
//                cursor.addRow(new Object[]{i});
//            }
//            return cursor;
//        }
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
}
