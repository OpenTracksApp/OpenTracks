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
package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationFactory;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.services.TrackRecordingServiceTest.MockContext;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A unit test for {@link MyTracksProviderUtilsImpl}.
 *
 * @author Bartlomiej Niechwiej
 */
public class MyTracksProviderUtilsImplTest extends AndroidTestCase {
  private Context context;
  private MyTracksProviderUtils providerUtils;
  
  private static final String tracksNamePrefix = "test name";    
  private static final String tracksCategory = "test category"; 

  @Override
  protected void setUp() throws Exception {
    
   
    super.setUp();
    
    MockContentResolver mockContentResolver = new MockContentResolver();
    RenamingDelegatingContext targetContext = new RenamingDelegatingContext(
        getContext(), getContext(), "test.");
    context = new MockContext(mockContentResolver, targetContext);
    MyTracksProvider provider = new MyTracksProvider();
    provider.attachInfo(context, null);
    mockContentResolver.addProvider(MyTracksProviderUtils.AUTHORITY, provider);
    setContext(context);

    providerUtils = MyTracksProviderUtils.Factory.get(context);
  }

  public void testLocationIterator_noPoints() {
    testIterator(1, 0, 1, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }

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
    assertEquals(15, counter.get());
  }
  
  public void testLocationIterator_nullFactory() {
    try {
      testIterator(1, 15, 4, false, null);
      fail("Expecting IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testLocationIterator_noBatchAscending() {
    testIterator(1, 50, 100, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    testIterator(2, 50, 50, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }
  
  public void testLocationIterator_noBatchDescending() {
    testIterator(1, 50, 100, true, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    testIterator(2, 50, 50, true, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }
 
  public void testLocationIterator_batchAscending() {
    testIterator(1, 50, 11, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    testIterator(2, 50, 25, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }
 
  public void testLocationIterator_batchDescending() {
    testIterator(1, 50, 11, true, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    testIterator(2, 50, 25, true, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }
  
  public void testLocationIterator_largeTrack() {
    testIterator(1, 20000, 2000, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
  }

  private List<Location> testIterator(long trackId, int numPoints, int batchSize,
      boolean descending, LocationFactory locationFactory) {
    long lastPointId = initializeTrack(trackId, numPoints);
    ((MyTracksProviderUtilsImpl) providerUtils).setDefaultCursorBatchSize(batchSize);
    List<Location> locations = new ArrayList<Location>(numPoints);
    LocationIterator it = providerUtils.getLocationIterator(trackId, -1, descending, locationFactory);
    try {
      while (it.hasNext()) {
        Location loc = it.next();
        assertNotNull(loc);
        locations.add(loc);
        // Make sure the IDs are returned in the right order.
        assertEquals(descending ? lastPointId - locations.size() + 1
            : lastPointId - numPoints + locations.size(), it.getLocationId());
      }
      assertEquals(numPoints, locations.size());
    } finally {
      it.close();
    }
    return locations;
  }
  
  private long initializeTrack(long id, int numPoints) {
    Track track = new Track();
    track.setId(id);
    track.setName("Test: " + id);
    track.setNumberOfPoints(numPoints);
    providerUtils.insertTrack(track);
    track = providerUtils.getTrack(id);
    assertNotNull(track);
    
    Location[] locations = new Location[numPoints];
    for (int i = 0; i < numPoints; ++i) {
      Location loc = new Location("test");
      loc.setLatitude(37.0 + (double) i / 10000.0);
      loc.setLongitude(57.0 - (double) i / 10000.0);
      loc.setAccuracy((float) i / 100.0f);
      loc.setAltitude(i * 2.5);
      locations[i] = loc;
    }
    providerUtils.bulkInsertTrackPoints(locations, numPoints, id);
    
    // Load all inserted locations. 
    long lastPointId = -1;
    int counter = 0;
    LocationIterator it = providerUtils.getLocationIterator(id, -1, false,
        MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
    try {
      while (it.hasNext()) {
        it.next();
        lastPointId = it.getLocationId();
        counter++;
      }
    } finally {
      it.close();
    }

    assertTrue(numPoints == 0 || lastPointId > 0);
    assertEquals(numPoints, track.getNumberOfPoints());
    assertEquals(numPoints, counter);
    
    return lastPointId;
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getAllTracks()}
   */
  public void testGetAllTracks() {
    int initialTrackNumber = providerUtils.getAllTracks().size();
    long trackId = System.currentTimeMillis();
    providerUtils.insertTrack(simulaTrack(trackId, 0));
    List<Track> allTracks = providerUtils.getAllTracks();
    assertEquals(initialTrackNumber + 1, allTracks.size());
    assertEquals(trackId, allTracks.get(allTracks.size() - 1).getId());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getTrack(long)}
   */
  public void testGetTrack() {
    long trackId = System.currentTimeMillis();
    providerUtils.insertTrack(simulaTrack(trackId, 0));
    assertNotNull(providerUtils.getTrack(trackId));
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getLastTrack()}
   */
  public void testGetLastTrack() {
    long trackId = System.currentTimeMillis();
    providerUtils.insertTrack(simulaTrack(trackId, 0));
    assertEquals(trackId, providerUtils.getLastTrack().getId());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getLastTrackId()}
   */
  public void testGetLastTrackId() {
    long trackId = System.currentTimeMillis();
    providerUtils.insertTrack(simulaTrack(trackId, 0));
    assertEquals(trackId, providerUtils.getLastTrackId());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#trackExists(long)}
   */
  public void testTrackExists() {
    long trackId = System.currentTimeMillis();
    providerUtils.insertTrack(simulaTrack(trackId, 0));
    assertTrue(providerUtils.trackExists(trackId));
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#updateTrack(Track)}
   */
  public void testUpdateTrack() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 0);
    String nameOld = "name1";
    String nameNew = "name2";
    track.setName(nameOld);
    providerUtils.insertTrack(track);
    assertEquals(nameOld, providerUtils.getTrack(trackId).getName()); 
    track.setName(nameNew);
    providerUtils.updateTrack(track);
    assertEquals(nameNew, providerUtils.getTrack(trackId).getName()); 
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#deleteAllTracks()}
   */
  public void testDeleteAllTracks() {
    // Insert track, point and way point at first.
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    providerUtils.bulkInsertTrackPoints(track.getLocations().toArray(new Location[0]), track
        .getLocations().size(), trackId);
    Waypoint waypoint = new Waypoint();
    providerUtils.insertWaypoint(waypoint );
    ContentResolver contentResolver = context.getContentResolver();
    Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null,
        TracksColumns._ID);
    assertTrue(tracksCursor.getCount() > 0);
    Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null,
        null, TrackPointsColumns._ID);
    assertTrue(tracksPointsCursor.getCount() > 0);
    Cursor waypointCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null,
        null, WaypointsColumns._ID);
    assertTrue(waypointCursor.getCount() > 0);
    // Delete all.
    providerUtils.deleteAllTracks();
    // Check whether all have been deleted. 
    tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null,
        TracksColumns._ID);
    assertTrue(tracksCursor.getCount() == 0);
    tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null,
        null, TrackPointsColumns._ID);
    assertTrue(tracksPointsCursor.getCount() == 0);
    waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null,
        null, WaypointsColumns._ID);
    assertTrue(waypointCursor.getCount() == 0);
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#deleteTrack(long)}.
   */
  public void testDeleteTrack() {
    // Delete all data at first.
    providerUtils.deleteAllTracks();
    //assertEquals(0, waypointCursor.getCount());
    // Insert three tracks, points of two tracks and way point of one track.
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    
    providerUtils.insertTrack(track);
    providerUtils.insertTrack(simulaTrack(trackId + 1, 10));
    providerUtils.insertTrack(simulaTrack(trackId + 2, 10));
    
    providerUtils.bulkInsertTrackPoints(track.getLocations().toArray(new Location[0]), track
        .getLocations().size(), trackId);
    providerUtils.bulkInsertTrackPoints(track.getLocations().toArray(new Location[0]), track
        .getLocations().size(), trackId + 1);
    
    Waypoint waypoint = new Waypoint();
    waypoint.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint);
    
    ContentResolver contentResolver = context.getContentResolver();
    Cursor tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null,
        TracksColumns._ID);
    assertEquals(3, tracksCursor.getCount());
    Cursor tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null,
        null, TrackPointsColumns._ID);
    assertEquals(20, tracksPointsCursor.getCount());
    Cursor waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null,
        null, WaypointsColumns._ID);
    assertEquals(1, waypointCursor.getCount());
    // Delete one track.
    providerUtils.deleteTrack(trackId);
    // Check whether all data of a track has been deleted. 
    tracksCursor = contentResolver.query(TracksColumns.CONTENT_URI, null, null, null,
        TracksColumns._ID);
    assertEquals(2, tracksCursor.getCount());
    tracksPointsCursor = contentResolver.query(TrackPointsColumns.CONTENT_URI, null, null,
        null, TrackPointsColumns._ID);
    assertEquals(20, tracksPointsCursor.getCount());
    waypointCursor = contentResolver.query(WaypointsColumns.CONTENT_URI, null, null,
        null, WaypointsColumns._ID);
    assertEquals(0, waypointCursor.getCount());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#createTrack(Cursor)}.
   */
  @UsesMocks(Cursor.class)
  public void testCreateTrack() {
    Cursor cursorMock = AndroidMock.createNiceMock(Cursor.class);
    int startIndex = 2;
    int index = startIndex;
    AndroidMock.expect(cursorMock.getColumnIndexOrThrow(TracksColumns._ID)).andReturn(index++);
    AndroidMock.expect(cursorMock.getColumnIndexOrThrow(TracksColumns.NAME)).andReturn(index++);
    index = startIndex;
    // Id
    AndroidMock.expect(cursorMock.isNull(index++)).andReturn(false);
    System.out.println("index:"+index);
    // Name
    AndroidMock.expect(cursorMock.isNull(index++)).andReturn(false);
    long trackId = System.currentTimeMillis();
    index = startIndex;
    // Id
    AndroidMock.expect(cursorMock.getLong(index++)).andReturn(trackId);
    // Name
    String name = tracksNamePrefix + Long.toString(trackId);
    AndroidMock.expect(cursorMock.getString(index++)).andReturn(name);
    AndroidMock.replay(cursorMock);
    Track track = providerUtils.createTrack(cursorMock);
    assertEquals(trackId, track.getId());
    assertEquals(name, track.getName());
    AndroidMock.verify(cursorMock);
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#createContentValues(Track)}.
   */
  @UsesMocks(TripStatistics.class)
  public void testCreateContentValues() {
    // ID
    long trackId = System.currentTimeMillis();
    // Name
    String name = tracksNamePrefix + Long.toString(trackId);
    Track track = simulaTrack(trackId, 10);
    track.setName(name);
    track.setCategory(tracksCategory);
    TripStatistics tripStatistics = AndroidMock.createNiceMock(TripStatistics.class);
    // Bottom
    int bottom = 22;
    // AverageSpeed
    double averageSpeed = 1.11;
    AndroidMock.expect(tripStatistics.getBottom()).andReturn(bottom);
    AndroidMock.expect(tripStatistics.getAverageSpeed()).andReturn(averageSpeed);
    track.setTripStatistics(tripStatistics);
    AndroidMock.replay(tripStatistics);
    
    ContentValues contentValues = providerUtils.createContentValues(track);
    assertEquals(trackId, contentValues.get(TracksColumns._ID));
    assertEquals(name, contentValues.get(TracksColumns.NAME)); 
    assertEquals(bottom, contentValues.get(TracksColumns.MINLAT));
    assertEquals(averageSpeed, contentValues.get(TracksColumns.AVGSPEED)); 
    AndroidMock.verify(tripStatistics);
  }

  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getFirstWaypoint(long)}.
   */
  public void testGetFirstWaypoint() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint1 = new Waypoint();
    waypoint1.setDescription("Desc1");
    waypoint1.setTrackId(trackId);
    Waypoint waypoint2 = new Waypoint();
    waypoint2.setDescription("Desc2");
    waypoint2.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint1);
    providerUtils.insertWaypoint(waypoint2);
    
    assertNull(providerUtils.getFirstWaypoint(-1));
    Waypoint wayPoint = providerUtils.getFirstWaypoint(trackId);
    assertEquals("Desc1", wayPoint.getDescription());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getFirstWaypointId(long)}.
   */
  public void testGetFirstWaypointId() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint1 = new Waypoint();
    waypoint1.setId(11L);
    waypoint1.setTrackId(trackId);
    Waypoint waypoint2 = new Waypoint();
    waypoint2.setId(22L);
    waypoint2.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint1);
    providerUtils.insertWaypoint(waypoint2);
    
    assertEquals(-1L, providerUtils.getFirstWaypointId(-1));
    assertEquals(1L, providerUtils.getFirstWaypointId(trackId));
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getLastWaypointId(long)}.
   */
  public void testGetKLastWaypointId() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint1 = new Waypoint();
    waypoint1.setId(11L);
    waypoint1.setTrackId(trackId);
    Waypoint waypoint2 = new Waypoint();
    waypoint2.setId(22L);
    waypoint2.setTrackId(trackId);
    Waypoint waypoint3 = new Waypoint();
    waypoint3.setId(33L);
    waypoint3.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint1);
    providerUtils.insertWaypoint(waypoint2);
    providerUtils.insertWaypoint(waypoint3);
    
    assertEquals(-1L, providerUtils.getLastWaypointId(-1));
    assertEquals(3L, providerUtils.getLastWaypointId(trackId));
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getNextMarkerNumber(long, boolean)}.
   */
  public void testGetNextMarkerNumber() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint1 = new Waypoint();
    waypoint1.setType(Waypoint.TYPE_STATISTICS);
    waypoint1.setTrackId(trackId);
    Waypoint waypoint2 = new Waypoint();
    waypoint2.setType(Waypoint.TYPE_WAYPOINT);
    waypoint2.setTrackId(trackId);
    Waypoint waypoint3 = new Waypoint();
    waypoint3.setType(Waypoint.TYPE_STATISTICS);
    waypoint3.setTrackId(trackId);
    Waypoint waypoint4 = new Waypoint();
    waypoint4.setType(Waypoint.TYPE_WAYPOINT);
    waypoint4.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint1);
    providerUtils.insertWaypoint(waypoint2);
    providerUtils.insertWaypoint(waypoint3);
    providerUtils.insertWaypoint(waypoint4);
    
    assertEquals(2, providerUtils.getNextMarkerNumber(trackId, true));
    assertEquals(3, providerUtils.getNextMarkerNumber(trackId, false));
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#getNextStatisticsWaypointAfter(Waypoint)}.
   */
  public void testGetNextStatisticsWaypointAfter() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint1 = new Waypoint();
    waypoint1.setType(Waypoint.TYPE_STATISTICS);
    waypoint1.setTrackId(trackId);
    Waypoint waypoint2 = new Waypoint();
    waypoint2.setType(Waypoint.TYPE_WAYPOINT);
    waypoint2.setTrackId(trackId);
    Waypoint waypoint3 = new Waypoint();
    waypoint3.setType(Waypoint.TYPE_STATISTICS);
    waypoint3.setTrackId(trackId);
    waypoint3.setDescription("Desc3");
    Waypoint waypoint4 = new Waypoint();
    waypoint4.setType(Waypoint.TYPE_STATISTICS);
    waypoint4.setTrackId(trackId);
    waypoint4.setDescription("Desc4");
    providerUtils.insertWaypoint(waypoint1);
    providerUtils.insertWaypoint(waypoint2);
    providerUtils.insertWaypoint(waypoint3);
    providerUtils.insertWaypoint(waypoint4);
    
    assertEquals("Desc3", providerUtils.getNextStatisticsWaypointAfter(providerUtils.getFirstWaypoint(trackId)).getDescription());
  }
  
  /**
   * Tests the method {@link MyTracksProviderUtilsImpl#insertWaypoint(Waypoint)} and
   * {@link MyTracksProviderUtilsImpl#getWaypoint(long)}.
   */
  public void testInsertAndGetWaypoint() {
    long trackId = System.currentTimeMillis();
    Track track = simulaTrack(trackId, 10);
    providerUtils.insertTrack(track);
    
    Waypoint waypoint = new Waypoint();
    waypoint.setDescription(Long.toString(trackId));
    waypoint.setTrackId(trackId);
    providerUtils.insertWaypoint(waypoint);
    
    assertEquals(Long.toString(trackId), providerUtils.getWaypoint(1).getDescription());
  }

  /**
   * Simulates a track which is used for testing.
   * 
   * @param id the id of the track
   * @param numPoints the location number in the track
   * @return the simulated track
   */
  private Track simulaTrack(long id,int numPoints) {
    Track track = new Track();
    track.setId(id);
    track.setName("Test: " + id);
    track.setNumberOfPoints(numPoints);
    for(int i=0; i < numPoints; i++) {
      Location loc = new Location("test");
      loc.setLatitude(37.0 + (double) i / 10000.0);
      loc.setLongitude(57.0 - (double) i / 10000.0);
      loc.setAccuracy((float) i / 100.0f);
      loc.setAltitude(i * 2.5);
      track.addLocation(loc);
    }
    return track;
  }
}
