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
package de.dennisguse.opentracks.content.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.UUIDUtils;

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
    private static final String TEST_NAME_NEW = "Test Name new!";

    private final Context context = ApplicationProvider.getApplicationContext();
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
        testIterator(new Track.Id(1), 0, 1, false);
    }

    @Test
    public void testLocationIterator_noBatchAscending() {
        testIterator(new Track.Id(1), 50, 100, false);
        testIterator(new Track.Id(2), 50, 50, false);
    }

    @Test
    public void testLocationIterator_noBatchDescending() {
        testIterator(new Track.Id(1), 50, 100, true);
        testIterator(new Track.Id(2), 50, 50, true);
    }

    @Test
    public void testLocationIterator_batchAscending() {
        testIterator(new Track.Id(1), 50, 11, false);
        testIterator(new Track.Id(2), 50, 25, false);
    }

    @Test
    public void testLocationIterator_batchDescending() {
        testIterator(new Track.Id(1), 50, 11, true);
        testIterator(new Track.Id(2), 50, 25, true);
    }

    @Test
    public void testLocationIterator_largeTrack() {
        testIterator(new Track.Id(1), 20000, 2000, false);
    }

    private void testIterator(Track.Id trackId, int numPoints, int batchSize, boolean descending) {
        long lastPointId = initializeTrack(trackId, numPoints);
        contentProviderUtils.setDefaultCursorBatchSize(batchSize);
        List<TrackPoint> locations = new ArrayList<>(numPoints);
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(trackId, -1L, descending)) {
            while (it.hasNext()) {
                TrackPoint loc = it.next();
                Assert.assertNotNull(loc);
                locations.add(loc);
                // Make sure the IDs are returned in the right order.
                Assert.assertEquals(descending ? lastPointId - locations.size() + 1
                        : lastPointId - numPoints + locations.size(), it.getTrackPointId());
            }
            Assert.assertEquals(numPoints, locations.size());
        }
    }

    private long initializeTrack(Track.Id id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id);
        contentProviderUtils.insertTrack(track);
        track = contentProviderUtils.getTrack(id);
        Assert.assertNotNull(track);

        TrackPoint[] trackPoints = new TrackPoint[numPoints];
        for (int i = 0; i < numPoints; ++i) {
            Location loc = new Location("test");
            loc.setLatitude(37.0 + (double) i / 10000.0);
            loc.setLongitude(57.0 - (double) i / 10000.0);
            loc.setAccuracy((float) i / 100.0f);
            loc.setAltitude(i * 2.5);
            trackPoints[i] = new TrackPoint(loc);
        }
        contentProviderUtils.bulkInsertTrackPoint(trackPoints, id);

        // Load all inserted trackPoints.
        long lastPointId = -1;
        int counter = 0;
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(id, -1L, false)) {
            while (it.hasNext()) {
                it.next();
                lastPointId = it.getTrackPointId();
                counter++;
            }
        }

        Assert.assertTrue(numPoints == 0 || lastPointId > 0);
        Assert.assertEquals(numPoints, counter);

        return lastPointId;
    }

    /**
     * Tests the method {@link ContentProviderUtils#createTrack(Cursor)}.
     */
    @Test
    public void testCreateTrack() {
        long trackId = System.currentTimeMillis();

        int columnIndex = 1;
        // Id
        when(cursorMock.getColumnIndexOrThrow(TracksColumns._ID)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getLong(columnIndex)).thenReturn(trackId);

        //Uuid
        columnIndex++;
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.UUID)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getBlob(columnIndex)).thenReturn(UUIDUtils.toBytes(UUID.randomUUID()));

        // Name
        columnIndex++;
        String name = NAME_PREFIX + trackId;
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.NAME)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getString(columnIndex)).thenReturn(name);

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
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Waypoint waypoint = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        contentProviderUtils.insertWaypoint(waypoint);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(1, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(10, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        // Delete all.
        contentProviderUtils.deleteAllTracks(context);
        // Check whether all have been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(0, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(0, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(0, waypointCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteAllTracks(Context)}
     */
    @Test
    public void testDeleteAllTracks_withWaypointAndPhoto() throws IOException {
        // Insert track, points and waypoint with photo at first.
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Waypoint waypoint = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        contentProviderUtils.insertWaypoint(waypoint);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(1, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(10, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        // Check waypoint has photo and it's in the external storage.
        Assert.assertTrue(waypoint.hasPhoto());
        File dir = FileUtils.getPhotoDir(context, trackId);
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);
        Assert.assertTrue(dir.exists());
        // Delete all.
        contentProviderUtils.deleteAllTracks(context);
        // Check whether all have been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(0, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(0, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(0, waypointCursor.getCount());
        Assert.assertFalse(dir.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteTrack(Context, Track.Id)}.
     */
    @Test
    public void testDeleteTrack() {
        // Insert three tracks, points of two tracks and way point of one track.
        long random = System.currentTimeMillis();
        Track.Id trackId1 = new Track.Id(random);
        Track.Id trackId2 = new Track.Id(random + 1);
        Track.Id trackId3 = new Track.Id(random + 2);

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId1, 0);

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId2, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId3, 10);

        Waypoint waypoint = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId2));
        waypoint.setTrackId(trackId1);
        contentProviderUtils.insertWaypoint(waypoint);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(3, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(20, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        // Delete one track.
        contentProviderUtils.deleteTrack(context, trackId1);
        // Check whether all data of a track has been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(2, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(20, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(0, waypointCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteTrack(Context, Track.Id)}.
     */
    @Test
    public void testDeleteTrack_withWaypointPhoto() throws IOException {
        // Insert three tracks.
        long random = System.currentTimeMillis();
        Track.Id trackId1 = new Track.Id(random);
        Track.Id trackId2 = new Track.Id(random + 1);
        Track.Id trackId3 = new Track.Id(random + 2);

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId1, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId2, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId3, 10);

        // Insert a waypoint in tracks trackId and trackId + 1.
        TrackPoint trackPoint1 = contentProviderUtils.getLastValidTrackPoint(trackId1);
        Waypoint waypoint1 = TestDataUtil.createWaypointWithPhoto(context, trackId1, trackPoint1.getLocation());
        contentProviderUtils.insertWaypoint(waypoint1);
        File dir1 = FileUtils.getPhotoDir(context, trackId1);

        TrackPoint trackPoint2 = contentProviderUtils.getLastValidTrackPoint(trackId2);
        Waypoint waypoint2 = TestDataUtil.createWaypointWithPhoto(context, trackId2, trackPoint2.getLocation());
        contentProviderUtils.insertWaypoint(waypoint2);
        File dir2 = FileUtils.getPhotoDir(context, trackId2);

        // Check.
        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(3, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(30, tracksPointsCursor.getCount());
        Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(2, waypointCursor.getCount());
        Assert.assertTrue(waypoint1.hasPhoto());
        Assert.assertTrue(dir1.isDirectory());
        Assert.assertEquals(1, dir1.list().length);
        Assert.assertTrue(dir1.exists());
        Assert.assertTrue(dir2.isDirectory());
        Assert.assertEquals(1, dir2.list().length);
        Assert.assertTrue(dir2.exists());
        // Delete one track.
        contentProviderUtils.deleteTrack(context, trackId1);
        // Check whether all data of a track has been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        Assert.assertEquals(2, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        Assert.assertEquals(20, tracksPointsCursor.getCount());
        waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null, null, WaypointsColumns._ID);
        Assert.assertEquals(1, waypointCursor.getCount());
        Assert.assertFalse(dir1.exists());
        Assert.assertTrue(dir2.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getAllTracks()}
     */
    @Test
    public void testGetAllTracks() {
        // given
        int initialTrackNumber = contentProviderUtils.getAllTracks().size();
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        contentProviderUtils.insertTrack(TestDataUtil.createTrack(trackId));

        // when
        List<Track> allTracks = contentProviderUtils.getAllTracks();

        // then
        Assert.assertEquals(initialTrackNumber + 1, allTracks.size());
        Assert.assertEquals(trackId, allTracks.get(allTracks.size() - 1).getId());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getLastTrack()}
     */
    @Test
    public void testGetLastTrack() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        contentProviderUtils.insertTrack(TestDataUtil.createTrack(trackId));
        Assert.assertEquals(trackId, contentProviderUtils.getLastTrack().getId());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrack(Track.Id)}
     */
    @Test
    public void testGetTrack_by_id() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        contentProviderUtils.insertTrack(TestDataUtil.createTrack(trackId));

        // when / then
        Assert.assertNotNull(contentProviderUtils.getTrack(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrack(Track.Id)}
     */
    @Test
    public void testGetTrack_by_uuid() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrack(trackId);
        contentProviderUtils.insertTrack(track);

        // when / then
        Assert.assertNotNull(contentProviderUtils.getTrack(track.getUuid()));
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateTrack(Track)}
     */
    @Test
    public void testUpdateTrack() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrack(trackId);
        String nameOld = "name1";
        String nameNew = "name2";
        track.setName(nameOld);

        // when / then
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
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);

        // Bottom
        long startTime = 1000L;
        // AverageSpeed
        TrackStatistics statistics = new TrackStatistics();
        statistics.setStartTime_ms(startTime);
        statistics.setStopTime_ms(2500L);
        statistics.setTotalTime(1500L);
        statistics.setMovingTime(700L);
        statistics.setTotalDistance(750.0);
        statistics.setTotalElevationGain(50.0);
        statistics.setMaxSpeed(60.0);
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);

        track.first.setTrackStatistics(statistics);
        contentProviderUtils.insertTrack(track.first);

        Waypoint waypoint = new Waypoint(track.second[0]);
        waypoint.setDescription(TEST_DESC);
        contentProviderUtils.insertWaypoint(waypoint);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(contentResolverMock);

        Waypoint.Id waypointId = new Waypoint.Id(System.currentTimeMillis());
        waypoint.setId(waypointId);
        ContentValues contentValues = contentProviderUtils.createContentValues(waypoint);
        Assert.assertEquals(waypointId, contentValues.get(WaypointsColumns._ID));
        Assert.assertEquals((int) (TestDataUtil.INITIAL_LONGITUDE * 1000000), contentValues.get(WaypointsColumns.LONGITUDE));
        Assert.assertEquals(TEST_DESC, contentValues.get(WaypointsColumns.DESCRIPTION));
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
        Assert.assertEquals(id, waypoint.getId().getId());
        Assert.assertEquals(name, waypoint.getName());
        Assert.assertEquals(trackId, waypoint.getTrackId().getId());
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteWaypoint(Context, Waypoint.Id)}
     * when there is only one waypoint in the track.
     */
    @Test
    public void testDeleteWaypoint_onlyOneWayPoint() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        Waypoint waypoint1 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint1.setDescription(TEST_DESC);
        waypoint1.setTrackId(trackId);
        contentProviderUtils.insertWaypoint(waypoint1);

        // Check insert was done.
        Assert.assertEquals(contentProviderUtils.getWaypointCount(trackId), 1);

        // Get waypoint id that needs to delete.
        Waypoint.Id waypoint1Id = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint1)));

        // Delete
        contentProviderUtils.deleteWaypoint(context, waypoint1Id);

        Assert.assertNull(contentProviderUtils.getWaypoint(waypoint1Id));
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteWaypoint(Context, Waypoint.Id)}
     * when there is only one waypoint in the track.
     */
    @Test
    public void testDeleteWaypoint_onlyOneWayPointWithPhotoUrl() throws IOException {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Waypoint waypoint1 = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        contentProviderUtils.insertWaypoint(waypoint1);

        // Check insert was done.
        Assert.assertEquals(contentProviderUtils.getWaypointCount(trackId), 1);

        // Get waypoint id that needs to delete.
        Waypoint.Id waypoint1Id = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint1)));

        // Check waypoint has photo and it's in the external storage.
        Assert.assertTrue(waypoint1.hasPhoto());
        File dir = FileUtils.getPhotoDir(context, trackId);
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);
        Assert.assertTrue(dir.exists());

        // Delete
        contentProviderUtils.deleteWaypoint(context, waypoint1Id);

        // Check waypoint doesn't exists and photo folder was deleted.
        Assert.assertNull(contentProviderUtils.getWaypoint(waypoint1Id));
        Assert.assertFalse(dir.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteWaypoint(Context, Waypoint.Id)} when there is more than one waypoint in the track.
     */
    @Test
    public void testDeleteWaypoint_hasNextWayPoint() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

//        Track track = TestDataUtil.createTrackAndInsert(trackId, 10);
//
//        TrackStatistics statistics = new TrackStatistics();
//        statistics.setStartTime_ms(1000L);
//        statistics.setStopTime_ms(2500L);
//        statistics.setTotalTime(1500L);
//        statistics.setMovingTime(700L);
//        statistics.setTotalDistance(750.0);
//        statistics.setTotalElevationGain(50.0);
//        statistics.setMaxSpeed(60.0);
//        statistics.setMaxElevation(1250.0);
//        statistics.setMinElevation(1200.0);
//
//        track.setTrackStatistics(statistics);
//        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track);

        // Insert at first.
        Waypoint waypoint1 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint1.setDescription(MOCK_DESC);
        waypoint1.setTrackId(trackId);
        Waypoint.Id waypoint1Id = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint1)));

        Waypoint waypoint2 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint2.setDescription(MOCK_DESC);
        waypoint2.setTrackId(trackId);
        Waypoint.Id waypoint2Id = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint2)));

        // Delete
        Assert.assertNotNull(contentProviderUtils.getWaypoint(waypoint1Id));
        contentProviderUtils.deleteWaypoint(context, waypoint1Id);
        Assert.assertNull(contentProviderUtils.getWaypoint(waypoint1Id));

        Assert.assertEquals(MOCK_DESC, contentProviderUtils.getWaypoint(waypoint2Id).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getNextWaypointNumber(Track.Id)}.
     */
    @Test
    public void testGetNextWaypointNumber() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Waypoint waypoint1 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint1.setTrackId(trackId);
        Waypoint waypoint2 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint2.setTrackId(trackId);
        Waypoint waypoint3 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint3.setTrackId(trackId);
        Waypoint waypoint4 = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint4.setTrackId(trackId);
        contentProviderUtils.insertWaypoint(waypoint1);
        contentProviderUtils.insertWaypoint(waypoint2);
        contentProviderUtils.insertWaypoint(waypoint3);
        contentProviderUtils.insertWaypoint(waypoint4);

        Assert.assertEquals(4, contentProviderUtils.getNextWaypointNumber(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#insertWaypoint(Waypoint)} and
     * {@link ContentProviderUtils#getWaypoint(Waypoint.Id)}.
     */
    @Test
    public void testInsertAndGetWaypoint() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Waypoint waypoint = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        Waypoint.Id waypointId = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint)));

        Assert.assertEquals(TEST_DESC, contentProviderUtils.getWaypoint(waypointId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateWaypoint(Context, Waypoint)}.
     */
    @Test
    public void testUpdateWaypoint() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        Waypoint waypoint = new Waypoint(contentProviderUtils.getLastValidTrackPoint(trackId));
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        Waypoint.Id waypointId = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint)));

        // Update
        waypoint = contentProviderUtils.getWaypoint(waypointId);
        waypoint.setDescription(TEST_DESC_NEW);
        contentProviderUtils.updateWaypoint(context, waypoint);

        Assert.assertEquals(TEST_DESC_NEW, contentProviderUtils.getWaypoint(waypointId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateWaypoint(Context, Waypoint)}.
     */
    @Test
    public void testUpdateWaypoint_withPhoto() throws IOException {
        // tests after update waypoint with photo the photo remains in the storage.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Waypoint waypoint = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        Waypoint.Id waypointId = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint)));

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId);
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);

        // Update
        waypoint = contentProviderUtils.getWaypoint(waypointId);
        waypoint.setName(TEST_NAME_NEW);
        waypoint.setDescription(TEST_DESC_NEW);
        contentProviderUtils.updateWaypoint(context, waypoint);

        Assert.assertEquals(TEST_NAME_NEW, contentProviderUtils.getWaypoint(waypointId).getName());
        Assert.assertEquals(TEST_DESC_NEW, contentProviderUtils.getWaypoint(waypointId).getDescription());
        Assert.assertTrue(waypoint.hasPhoto());
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateWaypoint(Context, Waypoint)}.
     */
    @Test
    public void testUpdateWaypoint_delPhotoAndDir() throws IOException {
        // tests after update waypoint if user deletes the photo then file photo is deleted from the storage. Also empty directory is deleted.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Waypoint waypoint = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        Waypoint.Id waypointId = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint)));

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId);
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);

        // Update
        waypoint = contentProviderUtils.getWaypoint(waypointId);
        waypoint.setName(TEST_NAME_NEW);
        waypoint.setDescription(TEST_DESC_NEW);
        waypoint.setPhotoUrl(null);
        contentProviderUtils.updateWaypoint(context, waypoint);

        Assert.assertEquals(TEST_NAME_NEW, contentProviderUtils.getWaypoint(waypointId).getName());
        Assert.assertEquals(TEST_DESC_NEW, contentProviderUtils.getWaypoint(waypointId).getDescription());
        Assert.assertFalse(waypoint.hasPhoto());
        Assert.assertFalse(dir.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateWaypoint(Context, Waypoint)}.
     */
    @Test
    public void testUpdateWaypoint_delPhotoNotDir() throws IOException {
        // tests after update waypoint if user deletes the photo then file photo is deleted from the storage. Directory remains if there are more photos from other waypoints.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert two waypoints with photos.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Waypoint waypoint = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        waypoint.setDescription(TEST_DESC);
        waypoint.setTrackId(trackId);
        Waypoint otherWaypoint = TestDataUtil.createWaypointWithPhoto(context, trackId, trackPoint.getLocation());
        otherWaypoint.setDescription(TEST_DESC);
        otherWaypoint.setTrackId(trackId);
        Waypoint.Id waypointId = new Waypoint.Id(ContentUris.parseId(contentProviderUtils.insertWaypoint(waypoint)));
        contentProviderUtils.insertWaypoint(otherWaypoint);

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId);
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(2, dir.list().length);

        // Update one waypoint deleting photo.
        waypoint = contentProviderUtils.getWaypoint(waypointId);
        waypoint.setPhotoUrl(null);
        contentProviderUtils.updateWaypoint(context, waypoint);

        Assert.assertEquals(TEST_DESC, contentProviderUtils.getWaypoint(waypointId).getDescription());
        Assert.assertFalse(waypoint.hasPhoto());
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertEquals(1, dir.list().length);
    }

    /**
     * Tests the method {@link ContentProviderUtils#bulkInsertTrackPoint(TrackPoint[], Track.Id)}.
     */
    @Test
    public void testBulkInsertTrackPoint() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track.first, track.second);

        // when / then
        contentProviderUtils.bulkInsertTrackPoint(track.second, trackId);
        Assert.assertEquals(20, contentProviderUtils.getTrackPointCursor(trackId, -1L, 1000, false).getCount());
        contentProviderUtils.bulkInsertTrackPoint(Arrays.copyOfRange(track.second, 0, 8), trackId);
        Assert.assertEquals(28, contentProviderUtils.getTrackPointCursor(trackId, -1L, 1000, false).getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#createTrackPoint(Cursor)}.
     */
    //TODO incomplete
    @Test
    public void testCreateTrackPoint() {
        // given
        when(cursorMock.getColumnIndex(TrackPointsColumns._ID)).thenReturn(1);

        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.LONGITUDE)).thenReturn(2);
        when(cursorMock.isNull(2)).thenReturn(false);
        int longitude = 11;
        when(cursorMock.getInt(2)).thenReturn(longitude * 1000000);

        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.LATITUDE)).thenReturn(3);
        when(cursorMock.isNull(3)).thenReturn(false);
        int latitude = 22;
        when(cursorMock.getInt(3)).thenReturn(latitude * 1000000);

        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.TIME)).thenReturn(4);
        when(cursorMock.isNull(4)).thenReturn(false);
        long time = System.currentTimeMillis();
        when(cursorMock.getLong(4)).thenReturn(time);

        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.SPEED)).thenReturn(5);
        when(cursorMock.isNull(5)).thenReturn(false);
        float speed = 2.2f;
        when(cursorMock.getFloat(5)).thenReturn(speed);

        when(cursorMock.getColumnIndexOrThrow(TrackPointsColumns.SENSOR_HEARTRATE)).thenReturn(6);
        when(cursorMock.isNull(6)).thenReturn(false);

        // when
        TrackPoint trackPoint = contentProviderUtils.createTrackPoint(cursorMock);

        // then
        Assert.assertEquals(longitude, trackPoint.getLongitude(), 0.01);
        Assert.assertEquals(latitude, trackPoint.getLatitude(), 0.01);
        Assert.assertEquals(time, trackPoint.getTime(), 0.01);
        Assert.assertEquals(speed, trackPoint.getSpeed(), 0.01);
        Assert.assertFalse(trackPoint.hasHeartRate());
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#insertTrackPoint(TrackPoint, Track.Id)}.
     */
    @Test
    public void testInsertTrackPoint() {
        // Insert track, point at first.
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        contentProviderUtils.insertTrackPoint(TestDataUtil.createTrackPoint(22), trackId);
        Assert.assertEquals(11, contentProviderUtils.getTrackPoints(trackId).size());
    }

    @Test
    public void testInsertAndLoadTrackPoint() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        TrackPoint trackPoint = TestDataUtil.createTrackPoint(5);
        trackPoint.setHeartRate_bpm(1F);
        trackPoint.setCyclingCadence_rpm(2F);
        trackPoint.setPower(3F);

        // when
        contentProviderUtils.insertTrackPoint(trackPoint, trackId);

        // then
        List<TrackPoint> trackPoints = contentProviderUtils.getTrackPoints(trackId);
        Assert.assertTrue(trackPoints.get(10).hasHeartRate());
        Assert.assertEquals(trackPoint.getHeartRate_bpm(), trackPoints.get(10).getHeartRate_bpm(), 0.01);
        Assert.assertEquals(trackPoint.getCyclingCadence_rpm(), trackPoints.get(10).getCyclingCadence_rpm(), 0.01);
        Assert.assertEquals(trackPoint.getPower(), trackPoints.get(10).getPower(), 0.01);
    }

    /**
     * Tests the method {@link ContentProviderUtils#getLastValidTrackPoint(Track.Id)}.
     */
    @Test
    public void testGetLastValidTrackPoint() {
        // Insert track, points at first.
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        TrackPoint lastTrackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        checkLocation(9, lastTrackPoint.getLocation());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointCursor(Track.Id, long, int, boolean)} in descending.
     */
    @Test
    public void testGetTrackPointCursor_desc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        long[] trackpointIds = new long[track.second.length];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.second[i], track.first.getId()));
        }

        // when
        Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds[1], 5, true);

        // then
        Assert.assertEquals(2, cursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointCursor(Track.Id, long, int, boolean)} in ascending.
     */
    @Test
    public void testGetTrackPointCursor_asc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        long[] trackpointIds = new long[track.second.length];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.second[i], track.first.getId()));
        }

        // when
        Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds[8], 5, false);

        // then
        Assert.assertEquals(2, cursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointLocationIterator(Track.Id, long, boolean)} in descending.
     */
    @Test
    public void testGetTrackPointLocationIterator_desc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        long[] trackpointIds = new long[track.second.length];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.second[i], track.first.getId()));
        }

        long startTrackPointId = trackpointIds[9];
        // when
        TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, startTrackPointId, true);

        // then
        for (int i = 0; i < trackpointIds.length; i++) {
            Assert.assertTrue(trackPointIterator.hasNext());
            TrackPoint trackPoint = trackPointIterator.next();
            Assert.assertEquals(startTrackPointId - i, trackPointIterator.getTrackPointId());
            checkLocation((trackpointIds.length - 1) - i, trackPoint.getLocation());
        }
        Assert.assertFalse(trackPointIterator.hasNext());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTrackPointLocationIterator(Track.Id, long, boolean)} in ascending.
     */
    @Test
    public void testGetTrackPointLocationIterator_asc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, TrackPoint[]> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        long[] trackpointIds = new long[track.second.length];
        for (int i = 0; i < trackpointIds.length; i++) {
            trackpointIds[i] = ContentUris.parseId(contentProviderUtils.insertTrackPoint(track.second[i], track.first.getId()));
        }

        long startTrackPointId = trackpointIds[0];

        // when
        TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, startTrackPointId, false);

        // then
        for (int i = 0; i < trackpointIds.length; i++) {
            Assert.assertTrue(trackPointIterator.hasNext());
            TrackPoint trackPoint = trackPointIterator.next();
            Assert.assertEquals(startTrackPointId + i, trackPointIterator.getTrackPointId());

            checkLocation(i, trackPoint.getLocation());
        }
        Assert.assertFalse(trackPointIterator.hasNext());
    }

    /**
     * Checks the value of a location.
     *
     * @param i        the index of this location which created in the method {@link TestDataUtil#createTrack(Track.Id, int)}
     * @param location the location to be checked
     */
    private void checkLocation(int i, Location location) {
        Assert.assertEquals(TestDataUtil.INITIAL_LATITUDE + (double) i / 10000.0, location.getLatitude(), 0.01);
        Assert.assertEquals(TestDataUtil.INITIAL_LONGITUDE - (double) i / 10000.0, location.getLongitude(), 0.01);
        Assert.assertEquals((float) i / 100.0f, location.getAccuracy(), 0.01);
        Assert.assertEquals(i * TestDataUtil.ALTITUDE_INTERVAL, location.getAltitude(), 0.01);
    }

    @Test
    public void testFormatIdListForUri() {
        Assert.assertEquals("", ContentProviderUtils.formatIdListForUri());
        Assert.assertEquals("12", ContentProviderUtils.formatIdListForUri(new Track.Id(12)));
        Assert.assertEquals("42,43,44", ContentProviderUtils.formatIdListForUri(new Track.Id(42), new Track.Id(43), new Track.Id(44)));
    }
}
