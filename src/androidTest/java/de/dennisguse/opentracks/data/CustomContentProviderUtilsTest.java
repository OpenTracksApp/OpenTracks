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
package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.TestSensorDataUtil;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.FileUtils;

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
        testIterator(new Track.Id(1), 0);
    }

    @Test
    public void testLocationIterator_noAscending() {
        testIterator(new Track.Id(1), 50);
        testIterator(new Track.Id(2), 50);
    }

    @Test
    public void testLocationIterator_largeTrack() {
        testIterator(new Track.Id(1), 20000 / 2);
    }

    private void testIterator(Track.Id trackId, int numPoints) {
        TrackPoint.Id lastPointId = initializeTrack(trackId, numPoints);
        List<TrackPoint> locations = new ArrayList<>(numPoints);
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            while (it.hasNext()) {
                TrackPoint trackPoint = it.next();
                assertNotNull(trackPoint);
                locations.add(trackPoint);
                // Make sure the IDs are returned in the right order.
                assertEquals(lastPointId.id() - numPoints + locations.size(), trackPoint.getId().id());
            }
            assertEquals(numPoints, locations.size());
        }
    }

    private TrackPoint.Id initializeTrack(Track.Id id, int numPoints) {
        Track track = new Track();
        track.setId(id);
        track.setName("Test: " + id.id());
        contentProviderUtils.insertTrack(track);
        track = contentProviderUtils.getTrack(id);
        assertNotNull(track);

        List<TrackPoint> trackPoints = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; ++i) {
            trackPoints.add(new TrackPoint(TrackPoint.Type.TRACKPOINT,
                    new Position(
                            Instant.ofEpochMilli(i),
                            37.0 + (double) i / 10000.0,
                            57.0 - (double) i / 10000.0,
                            Distance.of(i / 100.0f),
                            Altitude.WGS84.of(i * 2.5),
                            null,
                            null,
                            null)));
        }
        contentProviderUtils.bulkInsertTrackPoint(trackPoints, id);

        // Load all inserted trackPoints.
        TrackPoint.Id lastPointId = null;
        int counter = 0;
        try (TrackPointIterator it = contentProviderUtils.getTrackPointLocationIterator(id, null)) {
            while (it.hasNext()) {
                TrackPoint trackPoint = it.next();
                lastPointId = trackPoint.getId();
                counter++;
            }
        }

        assertTrue(numPoints == 0 || lastPointId.id() > 0);
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
        when(cursorMock.getLong(columnIndex)).thenReturn(trackId.id());

        //Uuid
        columnIndex++;
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.UUID)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getBlob(columnIndex)).thenReturn(UUIDUtils.toBytes(UUID.randomUUID()));

        // Name
        columnIndex++;
        String name = NAME_PREFIX + trackId.id();
        when(cursorMock.getColumnIndexOrThrow(TracksColumns.NAME)).thenReturn(columnIndex);
        when(cursorMock.isNull(columnIndex)).thenReturn(false);
        when(cursorMock.getString(columnIndex)).thenReturn(name);

        Track track = ContentProviderUtils.createTrack(cursorMock);
        assertEquals(trackId, track.getId());
        assertEquals(name, track.getName());
    }

    private void assertCount(int trackCount, int trackPointCount, int markerCount) {
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null, TracksColumns._ID)) {
            assertEquals(trackCount, tracksCursor.getCount());
        }
        try (Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, null, null, null, TrackPointsColumns._ID)) {
            assertEquals(trackPointCount, tracksPointsCursor.getCount());
        }
        try (Cursor markerCursor = contentResolver.query(MarkerColumns.CONTENT_URI, null, null, null, MarkerColumns._ID)) {
            assertEquals(markerCount, markerCursor.getCount());
        }
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

        assertCount(1, 10, 1);

        // when
        contentProviderUtils.deleteAllTracks(context);

        // then
        assertCount(0, 0, 0);
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

        assertCount(1, 10, 1);
        assertTrue(marker.hasPhoto());
        File dir = FileUtils.getPhotoDir(context, trackId);
        assertTrue(dir.isDirectory());
        assertEquals(1, dir.list().length);
        assertTrue(dir.exists());

        // when
        contentProviderUtils.deleteAllTracks(context);

        // then
        assertCount(0, 0, 0);
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

        assertCount(3, 30, 1);

        // when
        contentProviderUtils.deleteTrack(context, trackId1);

        // then
        assertCount(2, 20, 0);
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
        assertCount(3, 30, 2);
        assertTrue(marker1.hasPhoto());
        assertTrue(dir1.isDirectory());
        assertEquals(1, dir1.list().length);
        assertTrue(dir1.exists());
        assertTrue(dir2.isDirectory());
        assertEquals(1, dir2.list().length);
        assertTrue(dir2.exists());

        // when
        contentProviderUtils.deleteTrack(context, trackId1);

        // then
        assertCount(2, 20, 1);
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
        statistics.setTotalDistance(Distance.of(750.0));
        statistics.setTotalAltitudeGain(50.0f);
        statistics.setMaxSpeed(Speed.of(60.0));
        statistics.setMaxAltitude(1250.0);
        statistics.setMinAltitude(1200.0);

        track.first.setTrackStatistics(statistics);
        contentProviderUtils.insertTrack(track.first);

        Marker marker = new Marker(trackId, track.second.get(0));
        marker.setDescription(TEST_DESC);
        contentProviderUtils.insertMarker(marker);

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(contentResolverMock);

        Marker.Id markerId = new Marker.Id(System.currentTimeMillis());
        marker.setId(markerId);
        ContentValues contentValues = contentProviderUtils.createContentValues(marker);
        assertEquals(markerId.id(), contentValues.get(MarkerColumns._ID));
        assertEquals((int) (TestDataUtil.INITIAL_LONGITUDE * 1000000), contentValues.get(MarkerColumns.LONGITUDE));
        assertEquals(TEST_DESC, contentValues.get(MarkerColumns.DESCRIPTION));
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
        assertEquals(contentProviderUtils.getMarkers(trackId).size(), 1);

        // Get marker id that needs to delete.
        Marker.Id marker1Id = contentProviderUtils.insertMarker(marker1);

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
        assertEquals(contentProviderUtils.getMarkers(trackId).size(), 1);

        // Get marker id that needs to delete.
        Marker.Id marker1Id = contentProviderUtils.insertMarker(marker1);

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
//        statistics.setTotalAltitudeGain(50.0);
//        statistics.setMaxSpeed(60.0);
//        statistics.setMaxAltitude(1250.0);
//        statistics.setMinAltitude(1200.0);
//
//        track.setTrackStatistics(statistics);
//        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track);

        // Insert at first.
        Marker marker1 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker1.setDescription(MOCK_DESC);
        Marker.Id marker1Id = contentProviderUtils.insertMarker(marker1);

        Marker marker2 = new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId));
        marker2.setDescription(MOCK_DESC);
        Marker.Id marker2Id = contentProviderUtils.insertMarker(marker2);

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

        assertEquals(Integer.valueOf(4), contentProviderUtils.getNextMarkerNumber(trackId));
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
        Marker.Id markerId = contentProviderUtils.insertMarker(marker);

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
        Marker.Id markerId = contentProviderUtils.insertMarker(marker);

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
        Marker.Id markerId = contentProviderUtils.insertMarker(marker);

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.id());
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
        Marker.Id markerId = contentProviderUtils.insertMarker(marker);

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.id());
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
        Marker.Id markerId = contentProviderUtils.insertMarker(marker);
        contentProviderUtils.insertMarker(otherMarker);

        File dir = new File(FileUtils.getPhotoDir(context), "" + trackId.id());
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

        // when
        contentProviderUtils.bulkInsertTrackPoint(track.second, trackId);
        try (Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, null)) {
            // then
            assertEquals(20, cursor.getCount());
        }

        // when
        contentProviderUtils.bulkInsertTrackPoint(track.second.subList(0, 8), trackId);
        try (Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, null)) {
            // then
            assertEquals(28, cursor.getCount());
        }
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
        when(cursorMock.getFloat(6)).thenReturn(75f);

        // when
        TrackPoint trackPoint = contentProviderUtils.createTrackPoint(cursorMock);

        // then
        assertEquals(latitude, trackPoint.getPosition().latitude(), 0.01);
        assertEquals(longitude, trackPoint.getPosition().longitude(), 0.01);
        assertEquals(time, trackPoint.getTime().toEpochMilli());
        assertEquals(speed, trackPoint.getSpeed().toMPS(), 0.01);
        assertEquals(HeartRate.of(75f), trackPoint.getHeartRate());
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
        // when
        try (Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, null)) {
            // then
            assertEquals(11, cursor.getCount());
        }
    }

    @Test
    public void testInsertAndLoadTrackPoint() {
        // given
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 10);

        TrackPoint trackPoint = TestDataUtil.createTrackPoint(5);
        trackPoint.setHeartRate(1F);
        trackPoint.setCadence(2F);
        trackPoint.setPower(3F);

        // when
        contentProviderUtils.insertTrackPoint(trackPoint, trackId);

        // then
        List<TrackPoint> trackPoints = TestDataUtil.getTrackPoints(contentProviderUtils, trackId);
        assertTrue(trackPoints.get(10).hasHeartRate());
        assertEquals(trackPoint.getHeartRate(), trackPoints.get(10).getHeartRate());
        assertEquals(trackPoint.getCadence(), trackPoints.get(10).getCadence());
        assertEquals(trackPoint.getPower(), trackPoints.get(10).getPower());
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
        try (Cursor cursor = contentProviderUtils.getTrackPointCursor(trackId, trackpointIds.get(8))) {
            // then
            assertEquals(2, cursor.getCount());
        }
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
            assertEquals(startTrackPointId.id() + i, trackPoint.getId().id());

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
        trackPoint.setCadence(null);
        trackPoint.setHeartRate(null);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0f);
        assertTrue(sensorStatistics.hasCadence());
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0f);
        assertTrue(sensorStatistics.hasPower());
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0f);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0f);
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
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0f);
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
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0f);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0f);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0f);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0f);
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0f);
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0f);
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
        assertEquals(sensorStatistics.avgHeartRate().getBPM(), stats.avgHr, 0.01f);
        assertEquals(sensorStatistics.maxHeartRate().getBPM(), stats.maxHr, 0.01f);
        assertEquals(sensorStatistics.avgCadence().getRPM(), stats.avgCadence, 0.01f);
        assertEquals(sensorStatistics.maxCadence().getRPM(), stats.maxCadence, 0.01f);
        assertEquals(sensorStatistics.avgPower().getW(), stats.avgPower, 0.01f);
    }

    @Test
    public void testGetSensorStats_veryLongActivity12h() {
        testGetSensorStats_randomData(43200 / 6, false);
    }

    @Test
    public void testGetSensorStats_withSeveralRandomStartSegments() {
        testGetSensorStats_randomData(5000, true);
    }
}
