/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.MapOverlay.CachedLocation;
import com.google.android.apps.mytracks.TrackStubUtils;
import com.google.android.maps.Projection;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.graphics.Path;
import android.graphics.Rect;

import java.util.List;

/**
 * Tests for the {@link SingleColorTrackPathPainter}.
 * 
 * @author Youtao Liu
 */
public class SingleColorTrackPathPainterTest extends TrackPathPainterTestCase {

  private static final int NUMBER_OF_LOCATIONS = 10;
  private SingleColorTrackPathPainter singleColorTrackPathPainter;
  private Path pathMock;


  /**
   * Initials a mocked TrackPathDescriptor object and
   * singleColorTrackPathPainter.
   */
  @Override
  @UsesMocks(Path.class)
  protected void setUp() throws Exception {
    super.setUp();

    singleColorTrackPathPainter = new SingleColorTrackPathPainter(getContext());
    pathMock = AndroidMock.createStrictMock(Path.class);
  }

  /**
   * Tests the
   * {@link SingleColorTrackPathPainter#updatePath(Projection, Rect, int, List)}
   * method when all locations are valid.
   */
  public void testUpdatePath_AllValidLocation() {
    // Gets a number as the start index of points.
    int startLocationIdx = NUMBER_OF_LOCATIONS / 2;
    
    pathMock.incReserve(NUMBER_OF_LOCATIONS - startLocationIdx);
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS,
        TrackStubUtils.INITIAL_LATITUDE, -1);

    for (int i = startLocationIdx; i < NUMBER_OF_LOCATIONS; i++) {
      pathMock.lineTo(0, 0);
    }

    AndroidMock.replay(pathMock);
    singleColorTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, points, pathMock);
    AndroidMock.verify(pathMock);
  }

  /**
   * Tests the
   * {@link SingleColorTrackPathPainter#updatePath(Projection, Rect, int, List)}
   * method when all locations are invalid.
   */
  public void testUpdatePath_AllInvalidLocation() {
    int startLocationIdx = NUMBER_OF_LOCATIONS / 2;
    pathMock.incReserve(NUMBER_OF_LOCATIONS - startLocationIdx);
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS, INVALID_LATITUDE, -1);
    // Gets a random number from 1 to numberOfLocations.
    AndroidMock.replay(pathMock);
    singleColorTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, points, pathMock);
    AndroidMock.verify(pathMock);
  }

  /**
   * Tests the
   * {@link SingleColorTrackPathPainter#updatePath(Projection, Rect, int, List)}
   * method when there are three segments.
   */
  public void testUpdatePath_ThreeSegments() {
    // First segment.
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS,
        TrackStubUtils.INITIAL_LATITUDE, -1);
    points.addAll(createCachedLocations(1, INVALID_LATITUDE, -1));
    // Second segment.
    points.addAll(createCachedLocations(NUMBER_OF_LOCATIONS, TrackStubUtils.INITIAL_LATITUDE, -1));
    points.addAll(createCachedLocations(1, INVALID_LATITUDE, -1));
    // Third segment.
    points.addAll(createCachedLocations(NUMBER_OF_LOCATIONS, TrackStubUtils.INITIAL_LATITUDE, -1));
    // Gets a random number from 1 to numberOfLocations.
    int startLocationIdx = NUMBER_OF_LOCATIONS / 2;
    pathMock.incReserve(NUMBER_OF_LOCATIONS *3 + 1 +1 - startLocationIdx);
    for (int i = 0; i < NUMBER_OF_LOCATIONS - startLocationIdx; i++) {
      pathMock.lineTo(0, 0);
    }
    pathMock.moveTo(0, 0);
    for (int i = 0; i < NUMBER_OF_LOCATIONS - 1; i++) {
      pathMock.lineTo(0, 0);
    }
    pathMock.moveTo(0, 0);
    for (int i = 0; i < NUMBER_OF_LOCATIONS - 1; i++) {
      pathMock.lineTo(0, 0);
    }

    AndroidMock.replay(pathMock);
    singleColorTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, points, pathMock);
    AndroidMock.verify(pathMock);
  }
}
