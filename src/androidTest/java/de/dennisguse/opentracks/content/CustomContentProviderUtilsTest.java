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
package de.dennisguse.opentracks.content;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.stats.TripStatistics;

import static org.mockito.Mockito.when;

/**
 * A unit test for {@link ContentProviderUtils}.
 *
 * @author Bartlomiej Niechwiej
 * @author Youtao Liu
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomContentProviderUtilsTest {
    private static final String NAME_PREFIX = "test name";
    private static final String MOCK_DESC = "Mock Next Waypoint Desc!";
    private static final String TEST_DESC = "Test Desc!";
    private static final String TEST_DESC_NEW = "Test Desc new!";
    private static final double INITIAL_LATITUDE = 37.0;
    private static final double INITIAL_LONGITUDE = -57.0;
    private static final double ALTITUDE_INTERVAL = 2.5;

    private Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    @Mock
    private Cursor cursorMock;

    @Mock
    private ContentResolver contentResolverMock;

    @Before
    public void setUp() {
        contentProviderUtils = new ContentProviderUtils(context);
        contentProviderUtils.deleteAllTracks(context);
    }

    @Test
    public void testLocationIterator_noPoints() {
        testIterator(1, 0, 1, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    @Test
    public void testLocationIterator_customFactory() {
        final Location location = new Location("test_location");
        final AtomicInteger counter = new AtomicInteger();
        testIterator(1, 15, 4, false, new LocationFactory() {
            @Override
            public Location createLocation() {
                counter.incrementAndGet();
                return location;
            }
        });
        // Make sure we were called exactly as many times as we had track points.
        Assert.assertEquals(15, counter.get());
    }

    @Test
    public void testLocationIterator_nullFactory() {
        try {
            testIterator(1, 15, 4, false, null);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }

    @Test
    public void testLocationIterator_noBatchAscending() {
        testIterator(1, 50, 100, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
        testIterator(2, 50, 50, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    @Test
    public void testLocationIterator_noBatchDescending() {
        testIterator(1, 50, 100, true, LocationFactory.DEFAULT_LOCATION_FACTORY);
        testIterator(2, 50, 50, true, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    @Test
    public void testLocationIterator_batchAscending() {
        testIterator(1, 50, 11, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
        testIterator(2, 50, 25, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    @Test
    public void testLocationIterator_batchDescending() {
        testIterator(1, 50, 11, true, LocationFactory.DEFAULT_LOCATION_FACTORY);
        testIterator(2, 50, 25, true, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    @Test
    public void testLocationIterator_largeTrack() {
        testIterator(1, 20000, 2000, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
    }

    private List<Location> testIterator(long trackId, int numPoints, int batchSize, boolean descending, LocationFactory locationFactory) {
        long lastPointId = initializeTrack(trackId, numPoints);
        ((ContentProviderUtils) contentProviderUtils).setDefaultCursorBatchSize(batchSize);
        List<Location> locations = new ArrayList<Location>(numPoints);
        try (LocationIterator it = contentProviderUtils.getTrackPointLocationIterator(trackId, -1L, descending, locationFactory)) {
            while (it.hasNext()) {
                Location loc = it.next();
                Assert.assertNotNull(loc);
                locations.add(loc);
                // Make sure the IDs are returned in the right order.
                Assert.assertEquals(descending ? lastPointId - locations.size() + 1
                        : lastPointId - numPoints + locations.size(), it.getLocationId());
            }
            Assert.assertEquals(numPoints, locations.size());
        }
        return locations;
    }

    private long initializeTrack(long id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id);
        track.setNumberOfPoints(numPoints);
        contentProviderUtils.insertTrack(track);
        track = contentProviderUtils.getTrack(id);
        Assert.assertNotNull(track);

        Location[] locations = new Location[numPoints];
        for (int i = 0; i < numPoints; ++i) {
            Location loc = new Location("test");
            loc.setLatitude(37.0 + (double) i / 10000.0);
            loc.setLongitude(57.0 - (double) i / 10000.0);
            loc.setAccuracy((float) i / 100.0f);
            loc.setAltitude(i * 2.5);
            locations[i] = loc;
        }
        contentProviderUtils.bulkInsertTrackPoint(locations, numPoints, id);

        // Load all inserted locations.
        long lastPointId = -1;
        int counter = 0;
        try (LocationIterator it = contentProviderUtils.getTrackPointLocationIterator(id, -1L, false, LocationFactory.DEFAULT_LOCATION_FACTORY)) {
            while (it.hasNext()) {
                it.next();
                lastPointId = it.getLocationId();
                counter++;
            }
        }

        Assert.assertTrue(numPoints == 0 || lastPointId > 0);
        Assert.assertEquals(numPoints, track.getNumberOfPoints());
        Assert.assertEquals(numPoints, counter);

        return lastPointId;
    }

    /**
     * Tests the method {@link ContentProviderUtils#createTrack(Cursor)}.
     */
    @Test
    public void testCreateTrack() {
        int startColumnIndex = 1;
        int columnIndex = startColumnIndex;
        when(cursorMock.getColumnIndexOrThrow(TracksColumns._ID)).thenReturn(columnIndex++);
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.NAME)).thenReturn(columnIndex++);
        columnIndex = startColumnIndex;
        // Id
        when(cursorMock.isNull(columnIndex++)).thenReturn(false);
        // Name
        when(cursorMock.isNull(columnIndex++)).thenReturn(false);
        long trackId = System.currentTimeMillis();
        columnIndex = startColumnIndex;
        // Id
        when(cursorMock.getLong(columnIndex++)).thenReturn(trackId);
        // Name
        String name = NAME_PREFIX + trackId;
        when(cursorMock.getString(columnIndex++)).thenReturn(name);

        Track track = contentProviderUtils.createTrack(cursorMock);
        Assert.assertEquals(trackId, track.getId());
        Assert.assertEquals(name, track.getName());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteAllTracks(Context)}
     */
    @Test
    public void testDeleteAllTracks() {
        // Insert track, points and waypoint at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);
        Waypoint waypoint = new Waypoint();
        contentProviderUtils.insertWaypoint(waypoint);
        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(1, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(10, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        // Delete all.
        contentProviderUtils.deleteAllTracks(context);
        // Check whether all have been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(0, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(0, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(0, waypointCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteTrack(Context, long)}.
     */
    @Test
    public void testDeleteTrack() {
        // Insert three tracks, points of two tracks and way point of one track.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);

        contentProviderUtils.insertTrack(track);
        insertTrackWithLocations(getTrack(trackId + 1, 10));
        insertTrackWithLocations(getTrack(trackId + 2, 10));

        Waypoint waypoint = new Waypoint();
        waypoint.setTrackId(trackId);
        contentProviderUtils.insertWaypoint(waypoint);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(3, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(20, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        // Delete one track.
        contentProviderUtils.deleteTrack(context, trackId);
        // Check whether all data of a track has been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(2, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(20, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(0, waypointCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getAllTracks()}
     */
    @Test
    public void testGetAllTracks() {
        int initialTrackNumber = contentProviderUtils.getAllTracks().size();
        long trackId = System.currentTimeMillis();
        contentProviderUtils.insertTrack(getTrack(trackId, 0));
        List<Track> allTracks = contentProviderUtils.getAllTracks();
        Assert.assertEquals(initialTrackNumber + 1, allTracks.size());
        Assert.assertEquals(trackId, allTracks.get(allTracks.size() - 1).getId());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getLastTrack()}
     */
    @Test
    public void testGetLastTrack() {
        long trackId = System.currentTimeMillis();
        contentProviderUtils.insertTrack(getTrack(trackId, 0));
        Assert.assertEquals(trackId, contentProviderUtils.getLastTrack().getId());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrack(long)}
     */
    @Test
    public void testGetTrack() {
        long trackId = System.currentTimeMillis();
        contentProviderUtils.insertTrack(getTrack(trackId, 0));
        Assert.assertNotNull(contentProviderUtils.getTrack(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateTrack(Track)}
     */
    @Test
    public void testUpdateTrack() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 0);
        String nameOld = "name1";
        String nameNew = "name2";
        track.setName(nameOld);
        contentProviderUtils.insertTrack(track);
        Assert.assertEquals(nameOld, contentProviderUtils.getTrack(trackId).getName());
        track.setName(nameNew);
        contentProviderUtils.updateTrack(track);
        Assert.assertEquals(nameNew, contentProviderUtils.getTrack(trackId).getName());
    }

    /**
     * Tests the method {@link ContentProviderUtils#createContentValues(Waypoint)}.
     */
    @Test
    public void testCreateContentValues_waypoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);
        // Bottom
        long startTime = 1000L;
        // AverageSpeed
        double minGrade = -20.11;
        TripStatistics statistics = new TripStatistics();
        statistics.setStartTime(startTime);
        statistics.setStopTime(2500L);
        statistics.setTotalTime(1500L);
        statistics.setMovingTime(700L);
        statistics.setTotalDistance(750.0);
        statistics.setTotalElevationGain(50.0);
        statistics.setMaxSpeed(60.0);
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);
        statistics.setMaxGrade(15.0);
        statistics.setMinGrade(minGrade);
        statistics.setBounds(-10000, 20000, 30000, -40000);
        // Insert at first.
        Waypoint waypoint = new Waypoint();
        waypoint.setDescription(TEST_DESC);
        waypoint.setTripStatistics(statistics);

        Location location = new Location("test");
        location.setLatitude(22);
        location.setLongitude(22);
        location.setAccuracy((float) 1 / 100.0f);
        location.setAltitude(2.5);
        waypoint.setLocation(location);
        contentProviderUtils.insertWaypoint(waypoint);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(contentResolverMock);

        long waypointId = System.currentTimeMillis();
        waypoint.setId(waypointId);
        ContentValues contentValues = contentProviderUtils.createContentValues(waypoint);
        Assert.assertEquals(waypointId, contentValues.get(WaypointsColumns._ID));
        Assert.assertEquals(22 * 1000000, contentValues.get(WaypointsColumns.LONGITUDE));
        Assert.assertEquals(TEST_DESC, contentValues.get(WaypointsColumns.DESCRIPTION));
        Assert.assertEquals(startTime, contentValues.get(WaypointsColumns.STARTTIME));
        Assert.assertEquals(minGrade, contentValues.get(WaypointsColumns.MINGRADE));
    }

    /**
     * Tests the method {@link ContentProviderUtils#createWaypoint(Cursor)}.
     */
    @Test
    public void testCreateWaypoint() {
        int startColumnIndex = 1;
        int columnIndex = startColumnIndex;
        when(cursorMock.getColumnIndexOrThrow(WaypointsColumns._ID)).thenReturn(columnIndex++);
        when(cursorMock.getColumnIndexOrThrow(WaypointsColumns.NAME)).thenReturn(columnIndex++);
        when(cursorMock.getColumnIndexOrThrow(WaypointsColumns.TRACKID)).thenReturn(columnIndex++);
        columnIndex = startColumnIndex;
        // Id
        when(cursorMock.isNull(columnIndex++)).thenReturn(false);
        // Name
        when(cursorMock.isNull(columnIndex++)).thenReturn(false);
        // trackIdIndex
        when(cursorMock.isNull(columnIndex++)).thenReturn(false);
        long id = System.currentTimeMillis();
        columnIndex = startColumnIndex;
        // Id
        when(cursorMock.getLong(columnIndex++)).thenReturn(id);
        // Name
        String name = NAME_PREFIX + id;
        when(cursorMock.getString(columnIndex++)).thenReturn(name);
        // trackIdIndex
        long trackId = 11L;
        when(cursorMock.getLong(columnIndex++)).thenReturn(trackId);

        Waypoint waypoint = contentProviderUtils.createWaypoint(cursorMock);
        Assert.assertEquals(id, waypoint.getId());
        Assert.assertEquals(name, waypoint.getName());
        Assert.assertEquals(trackId, waypoint.getTrackId());
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteWaypoint(Context, long)}
     * when there is only one waypoint in the track.
     */
    @Test
    public void testDeleteWaypoint_onlyOneWayPoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        // Insert at first.
        Waypoint waypoint1 = new Waypoint();
        waypoint1.setDescription(TEST_DESC);
        waypoint1.setTrackId(trackId);
        contentProviderUtils.insertWaypoint(waypoint1);

        // Delete
        contentProviderUtils.deleteWaypoint(context, 1);

        Assert.assertNull(contentProviderUtils.getWaypoint(1));
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteWaypoint(Context, long)}
     * when there is more than one waypoint in the track.
     */
    @Test
    public void testDeleteWaypoint_hasNextWayPoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        TripStatistics statistics = new TripStatistics();
        statistics.setStartTime(1000L);
        statistics.setStopTime(2500L);
        statistics.setTotalTime(1500L);
        statistics.setMovingTime(700L);
        statistics.setTotalDistance(750.0);
        statistics.setTotalElevationGain(50.0);
        statistics.setMaxSpeed(60.0);
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);
        statistics.setMaxGrade(15.0);
        statistics.setMinGrade(-25.0);
        statistics.setBounds(-10000, 20000, 30000, -40000);

        // Insert at first.
        Waypoint waypoint1 = new Waypoint();
        waypoint1.setDescription(MOCK_DESC);
        waypoint1.setTrackId(trackId);
        waypoint1.setTripStatistics(statistics);
        long waypoint1Id = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint1));

        Waypoint waypoint2 = new Waypoint();
        waypoint2.setDescription(MOCK_DESC);
        waypoint2.setTrackId(trackId);
        waypoint2.setTripStatistics(statistics);
        long waypoint2Id = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint2));

        // Delete
        Assert.assertNotNull(contentProviderUtils.getWaypoint(waypoint1Id));
        contentProviderUtils.deleteWaypoint(context, waypoint1Id);
        Assert.assertNull(contentProviderUtils.getWaypoint(waypoint1Id));

        Assert.assertEquals(MOCK_DESC, contentProviderUtils.getWaypoint(waypoint2Id).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getFirstWaypointId(long)}.
     */
    @Test
    public void testGetFirstWaypointId() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        Waypoint waypoint1 = new Waypoint();
        waypoint1.setTrackId(trackId);
        long waypoint1id = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint1));

        Waypoint waypoint2 = new Waypoint();
        waypoint2.setTrackId(trackId);
        long waypoint2id = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint2));

        Assert.assertEquals(-1L, contentProviderUtils.getFirstWaypointId(-1));
        Assert.assertEquals(waypoint1id, contentProviderUtils.getFirstWaypointId(trackId));
        Assert.assertEquals(waypoint2id, contentProviderUtils.getLastWaypoint(trackId).getId());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getNextWaypointNumber(long)}.
     */
    @Test
    public void testGetNextWaypointNumber() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        Waypoint waypoint1 = new Waypoint();
        waypoint1.setTrackId(trackId);
        Waypoint waypoint2 = new Waypoint();
        waypoint2.setTrackId(trackId);
        Waypoint waypoint3 = new Waypoint();
        waypoint3.setTrackId(trackId);
        Waypoint waypoint4 = new Waypoint();
        waypoint4.setTrackId(trackId);
        contentProviderUtils.insertWaypoint(waypoint1);
        contentProviderUtils.insertWaypoint(waypoint2);
        contentProviderUtils.insertWaypoint(waypoint3);
        contentProviderUtils.insertWaypoint(waypoint4);

        Assert.assertEquals(4, contentProviderUtils.getNextWaypointNumber(trackId));
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#getLastWaypoint(long)}.
     */
    @Test
    public void testGetLastWaypoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        Waypoint waypoint1 = new Waypoint();
        waypoint1.setTrackId(trackId);
        waypoint1.setDescription("Desc1");
        Waypoint waypoint2 = new Waypoint();
        waypoint2.setTrackId(trackId);
        waypoint2.setDescription("Desc2");
        Waypoint waypoint3 = new Waypoint();
        waypoint3.setTrackId(trackId);
        waypoint3.setDescription("Desc3");
        contentProviderUtils.insertWaypoint(waypoint1);
        contentProviderUtils.insertWaypoint(waypoint2);
        contentProviderUtils.insertWaypoint(waypoint3);

        Assert.assertEquals("Desc3", contentProviderUtils.getLastWaypoint(trackId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#insertWaypoint(Waypoint)} and
     * {@link ContentProviderUtils#getWaypoint(long)}.
     */
    @Test
    public void testInsertAndGetWaypoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        Waypoint waypoint = new Waypoint();
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        long waypointId = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint));

        Assert.assertEquals(TEST_DESC, contentProviderUtils.getWaypoint(waypointId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateWaypoint(Waypoint)}.
     */
    @Test
    public void testUpdateWaypoint() {
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);
        // Insert at first.
        Waypoint waypoint = new Waypoint();
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        long waypointId = ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint));

        // Update
        waypoint = contentProviderUtils.getWaypoint(waypointId);
        waypoint.setDescription(TEST_DESC_NEW);
        contentProviderUtils.updateWaypoint(waypoint);

        Assert.assertEquals(TEST_DESC_NEW, contentProviderUtils.getWaypoint(waypointId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#bulkInsertTrackPoint(Location[], int, long)}.
     */
    @Test
    public void testBulkInsertTrackPoint() {
        // Insert track, point at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);

        contentProviderUtils.bulkInsertTrackPoint(track.getLocations().toArray(new Location[0]), -1, trackId);
        Assert.assertEquals(20, contentProviderUtils.getTrackPointCursor(trackId, -1L, 1000, false).getCount());
        contentProviderUtils.bulkInsertTrackPoint(track.getLocations().toArray(new Location[0]), 8, trackId);
        Assert.assertEquals(28, contentProviderUtils.getTrackPointCursor(trackId, -1L, 1000, false).getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#createTrackPoint(Cursor)}.
     */
    @Test
    public void testCreateTrackPoint() {
        // Set index.
        int index = 1;
        // Id
        when(cursorMock.getColumnIndex(TrackPointsColumns._ID)).thenReturn(index++);
        // Longitude
        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE)).thenReturn(index++);
        // Latitude
        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE)).thenReturn(index++);
        // Time
        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.TIME))
                .thenReturn(index++);
        // Speed
        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.SPEED)).thenReturn(index++);
        // Sensor
        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_HEARTRATE)).thenReturn(index++);

        // Set return value of isNull().
        index = 2;
        // Longitude
        when(cursorMock.isNull(index++)).thenReturn(false);
        // Latitude
        when(cursorMock.isNull(index++)).thenReturn(false);
        // Time
        when(cursorMock.isNull(index++)).thenReturn(false);
        // Speed
        when(cursorMock.isNull(index++)).thenReturn(false);
        // Sensor
        when(cursorMock.isNull(index++)).thenReturn(false);

        // Set return value of isNull().
        index = 2;
        // Longitude
        int longitude = 11;
        when(cursorMock.getInt(index++)).thenReturn(longitude * 1000000);
        // Latitude.
        int latitude = 22;
        when(cursorMock.getInt(index++)).thenReturn(latitude * 1000000);
        // Time
        long time = System.currentTimeMillis();
        when(cursorMock.getLong(index++)).thenReturn(time);
        // Speed
        float speed = 2.2f;
        when(cursorMock.getFloat(index++)).thenReturn(speed);
        // Sensor
        byte[] sensor = "Sensor state".getBytes();
        when(cursorMock.getBlob(index++)).thenReturn(sensor);

        Location location = contentProviderUtils.createTrackPoint(cursorMock);
        Assert.assertEquals((double) longitude, location.getLongitude(), 0.01);
        Assert.assertEquals((double) latitude, location.getLatitude(), 0.01);
        Assert.assertEquals(time, location.getTime(), 0.01);
        Assert.assertEquals(speed, location.getSpeed(), 0.01);
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#insertTrackPoint(Location, long)}.
     */
    @Test
    public void testInsertTrackPoint() {
        // Insert track, point at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);

        contentProviderUtils.insertTrackPoint(createLocation(22), trackId);
        Assert.assertEquals(11, contentProviderUtils.getTrackPointCursor(trackId, -1L, 1000, false).getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getFirstTrackPointId(long)}.
     */
    @Test
    public void testGetFirstTrackPointId() {
        // Insert track, point at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);

        Assert.assertNotEquals(-1L, contentProviderUtils.getFirstTrackPointId(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#getLastTrackPointId(long)}.
     */
    @Test
    public void testGetLastTrackPointId() {
        // Insert track, point at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);

        Assert.assertTrue(contentProviderUtils.getFirstTrackPointId(trackId) < contentProviderUtils.getLastTrackPointId(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#getLastValidTrackPoint(long)}.
     */
    @Test
    public void testGetLastValidTrackPoint() {
        // Insert track, points at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        insertTrackWithLocations(track);

        Location lastLocation = contentProviderUtils.getLastValidTrackPoint(trackId);
        checkLocation(9, lastLocation);
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointCursor(long, long, int, boolean)} in descending.
     */
    @Test
    public void testGetTrackPointCursor_desc() {
        // Insert track, points at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        long[] trackpointIds = new long[track.getNumberOfPoints()];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.getLocations().get(i), track.getId()));
        }

        Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds[1], 5, true);
        Assert.assertEquals(2, cursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointCursor(long, long, int, boolean)} in ascending.
     */
    @Test
    public void testGetTrackPointCursor_asc() {
        // Insert track, points at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        long[] trackpointIds = new long[track.getNumberOfPoints()];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.getLocations().get(i), track.getId()));
        }

        Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds[8], 5, false);
        Assert.assertEquals(2, cursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointLocationIterator(long, long, boolean, LocationFactory)} in descending.
     */
    @Test
    public void testGetTrackPointLocationIterator_desc() {
        // Insert track, points at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        long[] trackpointIds = new long[track.getNumberOfPoints()];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.getLocations().get(i), track.getId()));
        }

        long startTrackPointId = trackpointIds[9];

        LocationIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, startTrackPointId, true, LocationFactory.DEFAULT_LOCATION_FACTORY);
        for (int i = 0; i < trackpointIds.length; i++) {
            Assert.assertTrue(locationIterator.hasNext());
            Location location = locationIterator.next();
            Assert.assertEquals(startTrackPointId - i, locationIterator.getLocationId());
            checkLocation((trackpointIds.length - 1) - i, location);
        }
        Assert.assertFalse(locationIterator.hasNext());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointLocationIterator(long, long, boolean, LocationFactory)} in ascending.
     */
    @Test
    public void testGetTrackPointLocationIterator_asc() {
        // Insert track, point at first.
        long trackId = System.currentTimeMillis();
        Track track = getTrack(trackId, 10);
        contentProviderUtils.insertTrack(track);

        long[] trackpointIds = new long[track.getNumberOfPoints()];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.getLocations().get(i), track.getId()));
        }

        long startTrackPointId = trackpointIds[0];

        LocationIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, startTrackPointId, false, LocationFactory.DEFAULT_LOCATION_FACTORY);
        for (int i = 0; i < trackpointIds.length; i++) {
            Assert.assertTrue(locationIterator.hasNext());
            Location location = locationIterator.next();
            Assert.assertEquals(startTrackPointId + i, locationIterator.getLocationId());

            checkLocation(i, location);
        }
        Assert.assertFalse(locationIterator.hasNext());
    }

    /**
     * Simulates a track which is used for testing.
     *
     * @param id        the id of the track
     * @param numPoints the location number in the track
     * @return the simulated track
     */
    public static Track getTrack(long id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id);
        track.setNumberOfPoints(numPoints);
        for (int i = 0; i < numPoints; i++) {
            track.addLocation(createLocation(i));
        }
        return track;
    }

    /**
     * Creates a location.
     *
     * @param i the index to set the value of location.
     * @return created location
     */
    private static Location createLocation(int i) {
        Location loc = new Location("test");
        loc.setLatitude(INITIAL_LATITUDE + (double) i / 10000.0);
        loc.setLongitude(INITIAL_LONGITUDE - (double) i / 10000.0);
        loc.setAccuracy((float) i / 100.0f);
        loc.setAltitude(i * ALTITUDE_INTERVAL);
        return loc;
    }

    /**
     * Checks the value of a location.
     *
     * @param i        the index of this location which created in the method {@link CustomContentProviderUtilsTest#getTrack(long, int)}
     * @param location the location to be checked
     */
    private void checkLocation(int i, Location location) {
        Assert.assertEquals(INITIAL_LATITUDE + (double) i / 10000.0, location.getLatitude(), 0.01);
        Assert.assertEquals(INITIAL_LONGITUDE - (double) i / 10000.0, location.getLongitude(), 0.01);
        Assert.assertEquals((float) i / 100.0f, location.getAccuracy(), 0.01);
        Assert.assertEquals(i * ALTITUDE_INTERVAL, location.getAltitude(), 0.01);
    }

    /**
     * Inserts a track with locations into the database.
     *
     * @param track track to be inserted
     */
    private void insertTrackWithLocations(Track track) {
        contentProviderUtils.insertTrack(track);
        contentProviderUtils.bulkInsertTrackPoint(track.getLocations().toArray(new Location[0]), track.getLocations().size(), track.getId());
    }
}
