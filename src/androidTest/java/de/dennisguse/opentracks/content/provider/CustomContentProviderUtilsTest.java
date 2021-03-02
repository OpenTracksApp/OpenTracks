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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.TestSensorDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    private static final String MOCK_DESC = "Mock Next Marker Desc!";
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
        testIterator(new Track.Id(1), 0, 1);
    }

    @Test
    public void testLocationIterator_noBatchAscending() {
        testIterator(new Track.Id(1), 50, 100);
        testIterator(new Track.Id(2), 50, 50);
    }

    @Test
    public void testLocationIterator_batchAscending() {
        testIterator(new Track.Id(1), 50, 11);
        testIterator(new Track.Id(2), 50, 25);
    }

    @Test
    public void testLocationIterator_largeTrack() {
        testIterator(new Track.Id(1), 20000, 2000);
    }

    private void testIterator(Track.Id trackId, int numPoints, int batchSize) {
        long lastPointId = initializeTrack(trackId, numPoints);
        contentProviderUtils.setDefaultCursorBatchSize(batchSize);
        List<TrackPoint> locations = new ArrayList<>(numPoints);
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            while (it.hasNext()) {
                TrackPoint trackPoint = it.next();
                assertNotNull(trackPoint);
                locations.add(trackPoint);
                // Make sure the IDs are returned in the right order.
                assertEquals(lastPointId - numPoints + locations.size(), trackPoint.getId().getId());
            }
            assertEquals(numPoints, locations.size());
        }
    }

    private long initializeTrack(Track.Id id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id.getId());
        contentProviderUtils.insertTrack(track);
        track = contentProviderUtils.getTrack(id);
        assertNotNull(track);

        List<TrackPoint> trackPoints = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; ++i) {
            Location loc = new Location("test");
            loc.setLatitude(37.0 + (double) i / 10000.0);
            loc.setLongitude(57.0 - (double) i / 10000.0);
            loc.setAccuracy((float) i / 100.0f);
            loc.setAltitude(i * 2.5);
            trackPoints.add(new TrackPoint(loc));
        }
        contentProviderUtils.bulkInsertTrackPoint(trackPoints, id);

        // Load all inserted trackPoints.
        long lastPointId = -1;
        int counter = 0;
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(id, null)) {
            while (it.hasNext()) {
                TrackPoint trackPoint = it.next();
                lastPointId = trackPoint.getId().getId();
                counter++;
            }
        }

        assertTrue(numPoints == 0 || lastPointId > 0);
        assertEquals(numPoints, counter);

        return lastPointId;
    }

    /**
     * Tests the method {@link ContentProviderUtils#createTrack(Cursor)}.
     */
    @Test
    public void testCreateTrack() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());

        int columnIndex = 1;
        // Id
        when(cursorMock.getColumnIndexOrThrow(TracksColumns._ID)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getLong(columnIndex)).thenReturn(trackId.getId());

        //Uuid
        columnIndex++;
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.UUID)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getBlob(columnIndex)).thenReturn(UUIDUtils.toBytes(UUID.randomUUID()));

        // Name
        columnIndex++;
        String name = NAME_PREFIX + trackId.getId();
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.NAME)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getString(columnIndex)).thenReturn(name);

        Track track = ContentProviderUtils.createTrack(cursorMock);
        assertEquals(trackId, track.getId());
        assertEquals(name, track.getName());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteAllTracks(Context)}
     */
    @Test
    public void testDeleteAllTracks() {
        // Insert track, points and marker at first.
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Marker marker = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        contentProviderUtils.insertMarker(marker);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(1, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(10, tracksPointsCursor.getCount());
        Cursor markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(1, markerCursor.getCount());
        // Delete all.
        contentProviderUtils.deleteAllTracks(context);
        // Check whether all have been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(0, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(0, tracksPointsCursor.getCount());
        markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(0, markerCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteAllTracks(Context)}
     */
    @Test
    public void testDeleteAllTracks_withMarkerAndPhoto() throws IOException {
        // Insert track, points and marker with photo at first.
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Marker marker = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        contentProviderUtils.insertMarker(marker);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(1, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(10, tracksPointsCursor.getCount());
        Cursor markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(1, markerCursor.getCount());
        // Check marker has photo and it's in the external storage.
        assertTrue(marker.hasPhoto());
        File dir = FileUtils.getPhotoDir(context, trackId);
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);
        assertTrue(dir.exists());
        // Delete all.
        contentProviderUtils.deleteAllTracks(context);
        // Check whether all have been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(0, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(0, tracksPointsCursor.getCount());
        markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(0, markerCursor.getCount());
        assertFalse(dir.exists());
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

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId1, 10);

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId2, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId3, 10);

        Marker marker = new Marker(trackId1, contentProviderUtils.getLastValidTrackPoint(trackId2));
        contentProviderUtils.insertMarker(marker);

        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(3, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(30, tracksPointsCursor.getCount());
        Cursor markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(1, markerCursor.getCount());
        // Delete one track.
        contentProviderUtils.deleteTrack(context, trackId1);
        // Check whether all data of a track has been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(2, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(20, tracksPointsCursor.getCount());
        markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(0, markerCursor.getCount());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteTrack(Context, Track.Id)}.
     */
    @Test
    public void testDeleteTrack_withMarkerPhoto() throws IOException {
        // Insert three tracks.
        long random = System.currentTimeMillis();
        Track.Id trackId1 = new Track.Id(random);
        Track.Id trackId2 = new Track.Id(random + 1);
        Track.Id trackId3 = new Track.Id(random + 2);

        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId1, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId2, 10);
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId3, 10);

        // Insert a marker in tracks trackId and trackId + 1.
        TrackPoint trackPoint1 = contentProviderUtils.getLastValidTrackPoint(trackId1);
        Marker marker1 = TestDataUtil.createMarkerWithPhoto(context, trackId1, trackPoint1);
        contentProviderUtils.insertMarker(marker1);
        File dir1 = FileUtils.getPhotoDir(context, trackId1);

        TrackPoint trackPoint2 = contentProviderUtils.getLastValidTrackPoint(trackId2);
        Marker marker2 = TestDataUtil.createMarkerWithPhoto(context, trackId2, trackPoint2);
        contentProviderUtils.insertMarker(marker2);
        File dir2 = FileUtils.getPhotoDir(context, trackId2);

        // Check.
        ContentResolver contentResolver = context.getContentResolver();
        Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(3, tracksCursor.getCount());
        Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(30, tracksPointsCursor.getCount());
        Cursor markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(2, markerCursor.getCount());
        assertTrue(marker1.hasPhoto());
        assertTrue(dir1.isDirectory());
        assertEquals(1, dir1.list().length);
        assertTrue(dir1.exists());
        assertTrue(dir2.isDirectory());
        assertEquals(1, dir2.list().length);
        assertTrue(dir2.exists());
        // Delete one track.
        contentProviderUtils.deleteTrack(context, trackId1);
        // Check whether all data of a track has been deleted.
        tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID);
        assertEquals(2, tracksCursor.getCount());
        tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID);
        assertEquals(20, tracksPointsCursor.getCount());
        markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID);
        assertEquals(1, markerCursor.getCount());
        assertFalse(dir1.exists());
        assertTrue(dir2.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getTracks()}
     */
    @Test
    public void testGetAllTracks() {
        // given
        int initialTrackNumber = contentProviderUtils.getTracks().size();
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        contentProviderUtils.insertTrack(TestDataUtil.createTrack(trackId));

        // when
        List<Track> allTracks = contentProviderUtils.getTracks();

        // then
        assertEquals(initialTrackNumber + 1, allTracks.size());
        assertEquals(trackId, allTracks.get(allTracks.size() - 1).getId());
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
        assertNotNull(contentProviderUtils.getTrack(trackId));
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
        assertNotNull(contentProviderUtils.getTrack(track.getUuid()));
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
        assertEquals(nameOld, contentProviderUtils.getTrack(trackId).getName());
        track.setName(nameNew);
        contentProviderUtils.updateTrack(track);
        assertEquals(nameNew, contentProviderUtils.getTrack(trackId).getName());
    }

    /**
     * Tests the method {@link ContentProviderUtils#createContentValues(Marker)}.
     */
    @Test
    public void testCreateContentValues_marker() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, List<TrackPoint>> track = TestDataUtil.createTrack(trackId, 10);

        // AverageSpeed
        TrackStatistics statistics = new TrackStatistics();
        statistics.setStartTime(Instant.ofEpochMilli(1000));
        statistics.setStopTime(Instant.ofEpochMilli(2500));
        statistics.setTotalTime(Duration.ofMillis(1500));
        statistics.setMovingTime(Duration.ofMillis(700));
        statistics.setTotalDistance(750.0);
        statistics.setTotalElevationGain(50.0f);
        statistics.setMaxSpeed(60.0);
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);

        track.first.setTrackStatistics(statistics);
        contentProviderUtils.insertTrack(track.first);

        Marker marker = new Marker(trackId, track.second.get(0));
        marker.setDescription(TEST_DESC);
        contentProviderUtils.insertMarker(marker);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(contentResolverMock);

        Marker.Id markerId = new Marker.Id(System.currentTimeMillis());
        marker.setId(markerId);
        ContentValues contentValues = contentProviderUtils.createContentValues(marker);
        assertEquals(markerId.getId(), contentValues.get(MarkerColumns._ID));
        assertEquals((int) (TestDataUtil.INITIAL_LONGITUDE * 1000000), contentValues.get(MarkerColumns.LONGITUDE));
        assertEquals(TEST_DESC, contentValues.get(MarkerColumns.DESCRIPTION));
    }

    /**
     * Tests the method {@link ContentProviderUtils#createMarker(Cursor)}.
     */
    @Test
    public void testCreateMarker() {
        int startColumnIndex = 1;
        int columnIndex = startColumnIndex;
        when(cursorMock.getColumnIndexOrThrow(MarkerColumns._ID)).thenReturn(columnIndex++);
        when(cursorMock.getColumnIndexOrThrow(MarkerColumns.NAME)).thenReturn(columnIndex++);
        when(cursorMock.getColumnIndexOrThrow(MarkerColumns.TRACKID)).thenReturn(columnIndex++);
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

        Marker marker = contentProviderUtils.createMarker(cursorMock);
        assertEquals(id, marker.getId().getId());
        assertEquals(name, marker.getName());
        assertEquals(trackId, marker.getTrackId().getId());
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteMarker(Context, Marker.Id)}
     * when there is only one marker in the track.
     */
    @Test
    public void testDeleteMarker_onlyOneMarker() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        Marker marker1 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker1.setDescription(TEST_DESC);
        contentProviderUtils.insertMarker(marker1);

        // Check insert was done.
        assertEquals(contentProviderUtils.getMarkerCount(trackId), 1);

        // Get marker id that needs to delete.
        Marker.Id marker1Id = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker1)));

        // Delete
        contentProviderUtils.deleteMarker(context, marker1Id);

        assertNull(contentProviderUtils.getMarker(marker1Id));
    }

    /**
     * Tests the method
     * {@link ContentProviderUtils#deleteMarker(Context, Marker.Id)}
     * when there is only one marker in the track.
     */
    @Test
    public void testDeleteMarker_onlyOneMarkerWithPhotoUrl() throws IOException {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Marker marker1 = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        contentProviderUtils.insertMarker(marker1);

        // Check insert was done.
        assertEquals(contentProviderUtils.getMarkerCount(trackId), 1);

        // Get marker id that needs to delete.
        Marker.Id marker1Id = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker1)));

        // Check marker has photo and it's in the external storage.
        assertTrue(marker1.hasPhoto());
        File dir = FileUtils.getPhotoDir(context, trackId);
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);
        assertTrue(dir.exists());

        // Delete
        contentProviderUtils.deleteMarker(context, marker1Id);

        // Check marker doesn't exists and photo folder was deleted.
        assertNull(contentProviderUtils.getMarker(marker1Id));
        assertFalse(dir.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#deleteMarker(Context, Marker.Id)} when there is more than one marker in the track.
     */
    @Test
    public void testDeleteMarker_hasNextMarker() {
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
        Marker marker1 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker1.setDescription(MOCK_DESC);
        Marker.Id marker1Id = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker1)));

        Marker marker2 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker2.setDescription(MOCK_DESC);
        Marker.Id marker2Id = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker2)));

        // Delete
        assertNotNull(contentProviderUtils.getMarker(marker1Id));
        contentProviderUtils.deleteMarker(context, marker1Id);
        assertNull(contentProviderUtils.getMarker(marker1Id));

        assertEquals(MOCK_DESC, contentProviderUtils.getMarker(marker2Id).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#getNextMarkerNumber(Track.Id)}.
     */
    @Test
    public void testGetNextMarkerNumber() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Marker marker1 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        Marker marker2 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        Marker marker3 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        Marker marker4 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        contentProviderUtils.insertMarker(marker1);
        contentProviderUtils.insertMarker(marker2);
        contentProviderUtils.insertMarker(marker3);
        contentProviderUtils.insertMarker(marker4);

        assertEquals(4, contentProviderUtils.getNextMarkerNumber(trackId));
    }

    /**
     * Tests the method {@link ContentProviderUtils#insertMarker(Marker)} and
     * {@link ContentProviderUtils#getMarker(Marker.Id)}.
     */
    @Test
    public void testInsertAndGetMarker() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        Marker marker = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker.setDescription(TEST_DESC);
        Marker.Id markerId = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker)));

        assertEquals(TEST_DESC, contentProviderUtils.getMarker(markerId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateMarker(Context, Marker)}.
     */
    @Test
    public void testUpdateMarker() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        Marker marker = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker.setDescription(TEST_DESC);
        Marker.Id markerId = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker)));

        // Update
        marker = contentProviderUtils.getMarker(markerId);
        marker.setDescription(TEST_DESC_NEW);
        contentProviderUtils.updateMarker(context, marker);

        assertEquals(TEST_DESC_NEW, contentProviderUtils.getMarker(markerId).getDescription());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateMarker(Context, Marker)}.
     */
    @Test
    public void testUpdateMarker_withPhoto() throws IOException {
        // tests after update marker with photo the photo remains in the storage.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Marker marker = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        marker.setDescription(TEST_DESC);
        Marker.Id markerId = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker)));

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.getId());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);

        // Update
        marker = contentProviderUtils.getMarker(markerId);
        marker.setName(TEST_NAME_NEW);
        marker.setDescription(TEST_DESC_NEW);
        contentProviderUtils.updateMarker(context, marker);

        assertEquals(TEST_NAME_NEW, contentProviderUtils.getMarker(markerId).getName());
        assertEquals(TEST_DESC_NEW, contentProviderUtils.getMarker(markerId).getDescription());
        assertTrue(marker.hasPhoto());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateMarker(Context, Marker)}.
     */
    @Test
    public void testUpdateMarker_delPhotoAndDir() throws IOException {
        // tests after update marker if user deletes the photo then file photo is deleted from the storage. Also empty directory is deleted.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert at first.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Marker marker = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        marker.setDescription(TEST_DESC);
        Marker.Id markerId = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker)));

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.getId());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);

        // Update
        marker = contentProviderUtils.getMarker(markerId);
        marker.setName(TEST_NAME_NEW);
        marker.setDescription(TEST_DESC_NEW);
        marker.setPhotoUrl(null);
        contentProviderUtils.updateMarker(context, marker);

        assertEquals(TEST_NAME_NEW, contentProviderUtils.getMarker(markerId).getName());
        assertEquals(TEST_DESC_NEW, contentProviderUtils.getMarker(markerId).getDescription());
        assertFalse(marker.hasPhoto());
        assertFalse(dir.exists());
    }

    /**
     * Tests the method {@link ContentProviderUtils#updateMarker(Context, Marker)}.
     */
    @Test
    public void testUpdateMarker_delPhotoNotDir() throws IOException {
        // tests after update marker if user deletes the photo then file photo is deleted from the storage. Directory remains if there are more photos from other markers.

        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        // Insert two markers with photos.
        TrackPoint trackPoint = contentProviderUtils.getLastValidTrackPoint(trackId);
        Marker marker = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        marker.setDescription(TEST_DESC);
        Marker otherMarker = TestDataUtil.createMarkerWithPhoto(context, trackId, trackPoint);
        otherMarker.setDescription(TEST_DESC);
        Marker.Id markerId = new Marker.Id(ContentUris.parseId(contentProviderUtils.insertMarker(marker)));
        contentProviderUtils.insertMarker(otherMarker);

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.getId());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(2, dir.list().length);

        // Update one marker deleting photo.
        marker = contentProviderUtils.getMarker(markerId);
        marker.setPhotoUrl(null);
        contentProviderUtils.updateMarker(context, marker);

        assertEquals(TEST_DESC, contentProviderUtils.getMarker(markerId).getDescription());
        assertFalse(marker.hasPhoto());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);
    }

    /**
     * Tests the method {@link ContentProviderUtils#bulkInsertTrackPoint(List, Track.Id)}.
     */
    @Test
    public void testBulkInsertTrackPoint() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, List<TrackPoint>> track = TestDataUtil.createTrack(trackId, 10);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track.first, track.second);

        // when / then
        contentProviderUtils.bulkInsertTrackPoint(track.second, trackId);
        assertEquals(20, contentProviderUtils.getTrackPointCursor(trackId, null, 1000).getCount());
        contentProviderUtils.bulkInsertTrackPoint(track.second.subList(0, 8), trackId);
        assertEquals(28, contentProviderUtils.getTrackPointCursor(trackId, null, 1000).getCount());
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
        assertEquals(longitude, trackPoint.getLongitude(), 0.01);
        assertEquals(latitude, trackPoint.getLatitude(), 0.01);
        assertEquals(time, trackPoint.getTime().toEpochMilli());
        assertEquals(speed, trackPoint.getSpeed(), 0.01);
        assertFalse(trackPoint.hasHeartRate());
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
        assertEquals(11, contentProviderUtils.getTrackPoints(trackId).size());
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
        assertTrue(trackPoints.get(10).hasHeartRate());
        assertEquals(trackPoint.getHeartRate_bpm(), trackPoints.get(10).getHeartRate_bpm(), 0.01);
        assertEquals(trackPoint.getCyclingCadence_rpm(), trackPoints.get(10).getCyclingCadence_rpm(), 0.01);
        assertEquals(trackPoint.getPower(), trackPoints.get(10).getPower(), 0.01);
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

    @Test
    public void testGetTrackPointCursor_asc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, List<TrackPoint>> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        List<TrackPoint.Id> trackpointIds = track.second.stream()
                .map(it -> ContentUris.parseId(contentProviderUtils.insertTrackPoint(it, track.first.getId())))
                .map(TrackPoint.Id::new).collect(Collectors.toList());

        // when
        Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds.get(8), 5);

        // then
        assertEquals(2, cursor.getCount());
    }

    @Test
    public void testGetTrackPointLocationIterator_asc() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Pair<Track, List<TrackPoint>> track = TestDataUtil.createTrack(trackId, 10);
        contentProviderUtils.insertTrack(track.first);

        List<TrackPoint.Id> trackpointIds = track.second.stream()
                .map(it -> ContentUris.parseId(contentProviderUtils.insertTrackPoint(it, track.first.getId())))
                .map(TrackPoint.Id::new).collect(Collectors.toList());

        TrackPoint.Id startTrackPointId = trackpointIds.get(0);

        // when
        TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, trackpointIds.get(0));

        // then
        for (int i = 0; i < trackpointIds.size(); i++) {
            assertTrue(trackPointIterator.hasNext());
            TrackPoint trackPoint = trackPointIterator.next();
            assertEquals(startTrackPointId.getId() + i, trackPoint.getId().getId());

            checkLocation(i, trackPoint.getLocation());
        }
        assertFalse(trackPointIterator.hasNext());
    }

    /**
     * Checks the value of a location.
     *
     * @param i        the index of this location which created in the method {@link TestDataUtil#createTrack(Track.Id, int)}
     * @param location the location to be checked
     */
    private void checkLocation(int i, Location location) {
        assertEquals(TestDataUtil.INITIAL_LATITUDE + (double) i / 10000.0, location.getLatitude(), 0.01);
        assertEquals(TestDataUtil.INITIAL_LONGITUDE - (double) i / 10000.0, location.getLongitude(), 0.01);
        assertEquals((float) i / 100.0f, location.getAccuracy(), 0.01);
        assertEquals(i * TestDataUtil.ALTITUDE_INTERVAL, location.getAltitude(), 0.01);
    }

    @Test
    public void testFormatIdListForUri() {
        assertEquals("", ContentProviderUtils.formatIdListForUri());
        assertEquals("12", ContentProviderUtils.formatIdListForUri(new Track.Id(12)));
        assertEquals("42,43,44", ContentProviderUtils.formatIdListForUri(new Track.Id(42), new Track.Id(43), new Track.Id(44)));
    }

    @Test
    public void testGetSensorStats_noSensorData() {
        // given
        List<TrackPoint> trackPointList = new ArrayList<>();
        TrackPoint trackPoint = TestDataUtil.createTrackPoint(1);
        trackPoint.setType(TrackPoint.Type.TRACKPOINT);
        trackPoint.setPower(null);
        trackPoint.setCyclingCadence_rpm(null);
        trackPoint.setHeartRate_bpm(null);
        trackPointList.add(trackPoint);
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, trackPointList);

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);

        // then
        assertFalse(sensorStatistics.hasHeartRate());
        assertFalse(sensorStatistics.hasCadence());
        assertFalse(sensorStatistics.hasPower());
    }

    @Test
    public void testGetSensorStats_needAtLeastTwoTrackPointsFalse() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     90           300           0
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, 90f, 300f, TrackPoint.Type.TRACKPOINT);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);

        // then
        assertFalse(sensorStatistics.hasHeartRate());
        assertFalse(sensorStatistics.hasCadence());
        assertFalse(sensorStatistics.hasPower());
    }

    @Test
    public void testGetSensorStats_needAtLeastTwoTrackPointsTrue() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     90          300         -1
         * 1               140     90          300         1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, 90f, 300f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(1, ChronoUnit.SECONDS), 140f, 90f, 300f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertTrue(sensorStatistics.hasHeartRate());
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0f);
        assertTrue(sensorStatistics.hasCadence());
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0f);
        assertTrue(sensorStatistics.hasPower());
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0f);
    }

    @Test
    public void testGetSensorStats_onlyHr() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     NULL        NULL        -1
         * 1               140     NULL        NULL        1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, null, null, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(1, ChronoUnit.SECONDS), 140f, null, null, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertTrue(sensorStatistics.hasHeartRate());
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0f);
        assertFalse(sensorStatistics.hasCadence());
        assertFalse(sensorStatistics.hasPower());
    }

    @Test
    public void testGetSensorStats_onlyCadence() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               NULL    90          NULL        -1
         * 1               NULL    90          NULL        1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, null, 90f, null, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(1, ChronoUnit.SECONDS), null, 90f, null, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertFalse(sensorStatistics.hasHeartRate());
        assertTrue(sensorStatistics.hasCadence());
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0f);
        assertFalse(sensorStatistics.hasPower());
    }

    @Test
    public void testGetSensorStats_onlyPower() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               NULL    NULL        300         -1
         * 1               NULL    NULL        300         1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, null, null, 300f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(1, ChronoUnit.SECONDS), null, null, 300f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertFalse(sensorStatistics.hasHeartRate());
        assertFalse(sensorStatistics.hasCadence());
        assertTrue(sensorStatistics.hasPower());
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0f);
    }

    @Test
    public void testGetSensorStats() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     75          250         -1
         * 2               148     80          300         0
         * 1               150     82          325         0
         * 7               160     90          275         0
         * 4               155     85          280         0
         * 1               155     84          295         1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, 75f, 250f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(2, ChronoUnit.SECONDS), 148f, 80f, 300f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(3, ChronoUnit.SECONDS), 150f, 82f, 325f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(10, ChronoUnit.SECONDS), 160f, 90f, 275f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(14, ChronoUnit.SECONDS), 155f, 85f, 280f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(15, ChronoUnit.SECONDS), 155f, 84f, 295f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0f);
    }

    @Test
    public void testGetSensorStats_withManualResume() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     75          250         -1
         * 2               148     80          300         0
         * 1               150     82          325         0
         * 3               174     88          400         0
         * 20              127     54          175         -2
         * 3               160     90          275         0
         * 7               155     85          280         0
         * 3               150     90          267         0
         * 3               170     90          240         0
         * 2               155     84          295         1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, 75f, 250f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(2, ChronoUnit.SECONDS), 148f, 80f, 300f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(3, ChronoUnit.SECONDS), 150f, 82f, 325f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(6, ChronoUnit.SECONDS), 174f, 88f, 400f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(26, ChronoUnit.SECONDS), 127f, 54f, 175f, TrackPoint.Type.SEGMENT_START_MANUAL);
        sensorDataUtil.add(start.plus(29, ChronoUnit.SECONDS), 160f, 90f, 275f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(36, ChronoUnit.SECONDS), 155f, 85f, 280f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(39, ChronoUnit.SECONDS), 150f, 90f, 267f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(42, ChronoUnit.SECONDS), 170f, 90f, 240f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(44, ChronoUnit.SECONDS), 155f, 84f, 295f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0f);
    }

    @Test
    public void testGetSensorStats_withStartAutomatic() {
        // given
        /*
         * time elapsed    hr      cadence     power       track type
         * 0               140     75          250         -1
         * 2               148     80          300         0
         * 1               150     82          325         0
         * 3               174     88          400         0
         * 20              127     54          175         -1
         * 3               160     90          275         0
         * 7               155     85          280         0
         * 3               150     90          267         0
         * 3               170     90          240         0
         * 2               155     84          295         1
         */
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        sensorDataUtil.add(start, 140f, 75f, 250f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(2, ChronoUnit.SECONDS), 148f, 80f, 300f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(3, ChronoUnit.SECONDS), 150f, 82f, 325f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(6, ChronoUnit.SECONDS), 174f, 88f, 400f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(26, ChronoUnit.SECONDS), 127f, 54f, 175f, TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        sensorDataUtil.add(start.plus(29, ChronoUnit.SECONDS), 160f, 90f, 275f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(36, ChronoUnit.SECONDS), 155f, 85f, 280f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(39, ChronoUnit.SECONDS), 150f, 90f, 267f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(42, ChronoUnit.SECONDS), 170f, 90f, 240f, TrackPoint.Type.TRACKPOINT);
        sensorDataUtil.add(start.plus(44, ChronoUnit.SECONDS), 155f, 84f, 295f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0f);
    }

    private void testGetSensorStats_randomData(int totalPoints, boolean withStartSegments) {
        // given
        Instant start = Instant.now();
        TestSensorDataUtil sensorDataUtil = new TestSensorDataUtil();
        Random random = new Random();
        for (int i = 0; i < totalPoints; i++) {
            int randomNum = withStartSegments ? random.nextInt(50) - 2 : 0;
            TrackPoint.Type type = randomNum >= 0 ? TrackPoint.Type.TRACKPOINT : TrackPoint.Type.getById(randomNum);
            float randomHr = random.nextFloat() * (200f - 90f) + 90f;
            float randomCadence = random.nextFloat() * (110f - 40f) + 40f;
            float randomPower = random.nextFloat() * (500f - 100f) + 100f;
            sensorDataUtil.add(start.plus(i, ChronoUnit.SECONDS), randomHr, randomCadence, randomPower, type);
        }
        sensorDataUtil.add(start.plus(totalPoints, ChronoUnit.SECONDS), 155f, 84f, 295f, TrackPoint.Type.SEGMENT_END_MANUAL);

        Track.Id trackId = new Track.Id(start.toEpochMilli());
        Track track = TestDataUtil.createTrack(trackId);
        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track, sensorDataUtil.getTrackPointList());

        // when
        SensorStatistics sensorStatistics = contentProviderUtils.getSensorStats(trackId);
        TestSensorDataUtil.SensorDataStats stats = sensorDataUtil.computeStats();

        // then
        assertEquals(sensorStatistics.getAvgHeartRate(), stats.avgHr, 0.01f);
        assertEquals(sensorStatistics.getMaxHeartRate(), stats.maxHr, 0.01f);
        assertEquals(sensorStatistics.getAvgCadence(), stats.avgCadence, 0.01f);
        assertEquals(sensorStatistics.getMaxCadence(), stats.maxCadence, 0.01f);
        assertEquals(sensorStatistics.getAvgPower(), stats.avgPower, 0.01f);
    }

    @Test
    public void testGetSensorStats_veryLongActivity12h() {
    testGetSensorStats_randomData(43200, false);
    }

    @Test
    public void testGetSensorStats_withSeveralRandomStartSegments() {
        testGetSensorStats_randomData(5000, true);
    }
}
