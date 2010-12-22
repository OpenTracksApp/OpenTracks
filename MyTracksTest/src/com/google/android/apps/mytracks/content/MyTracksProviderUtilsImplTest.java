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

import android.content.Context;
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
}
