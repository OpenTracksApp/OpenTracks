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
package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.ColoredPath;
import com.google.android.apps.mytracks.MapOverlay.CachedLocation;
import com.google.android.apps.mytracks.TrackStubUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.location.Location;

import java.util.List;

/**
 * Tests for the {@link DynamicSpeedTrackPathPainter}.
 * 
 * @author Youtao Liu
 */
public class DynamicSpeedTrackPathPainterTest extends TrackPathPainterTestCase {
  private DynamicSpeedTrackPathPainter dynamicSpeedTrackPathPainter;
  private TrackPathDescriptor trackPathDescriptor;
  private static final int NUMBER_OF_LOCATIONS = 100;
  private static final int SLOW_SPEED = 30;
  private static final int NORMAL_SPEED = 50;

  /**
   * Tests the method
   * {@link DynamicSpeedTrackPathPainter#updatePath(com.google.android.maps.Projection, android.graphics.Rect, int, Boolean, java.util.List)}
   * when all locations are invalid.
   */
  public void testUpdatePath_AllInvalidLocation() {
    initialTrackPathDescriptorMock();
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS, false, -1);
    for (int i = 0; i < 100; ++i) {
      Location location = TrackStubUtils.createMyTracksLocation(INVALID_LATITUDE,
          TrackStubUtils.INITIAL_LONGITUDE, TrackStubUtils.INITIAL_ALTITUDE);
      CachedLocation cachedLocation = new CachedLocation(location);
      points.add(cachedLocation);
    }
    dynamicSpeedTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), 1, true, points);
    AndroidMock.verify(trackPathDescriptor);
    // Should be zero for there is no valid locations.
    assertEquals(0, dynamicSpeedTrackPathPainter.getColoredPaths().size());

  }

  /**
   * Tests the
   * {@link DynamicSpeedTrackPathPainter#updatePath(com.google.android.maps.Projection, android.graphics.Rect, int, Boolean, java.util.List)}
   * when all locations are valid.
   */
  public void testUpdatePath_AllValidLocation() {
    initialTrackPathDescriptorMock();
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS, true, -1);

    // Gets a random number from 1 to numberOfLocations.
    int startLocationIdx = (int) (1 + (NUMBER_OF_LOCATIONS - 1) * Math.random());

    dynamicSpeedTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, true, points);

    AndroidMock.verify(trackPathDescriptor);
    assertEquals(NUMBER_OF_LOCATIONS - startLocationIdx, dynamicSpeedTrackPathPainter
        .getColoredPaths().size());

  }

  /**
   * Tests the
   * {@link DynamicSpeedTrackPathPainter#updatePath(com.google.android.maps.Projection, android.graphics.Rect, int, Boolean, java.util.List)}
   * when all locations are valid.
   */
  public void testUpdatePath_CheckColoredPath() {
    initialTrackPathDescriptorMock();
    // Gets the slow speed for location.
    int slowSpeed = (int) (SLOW_SPEED / (2 * UnitConversions.MS_TO_KMH));
    // Gets the normal speed for location.
    int normalSpeed = (int) ((SLOW_SPEED + NORMAL_SPEED) / (2 * UnitConversions.MS_TO_KMH));
    // Gets the fast speed for location.
    int fastSpeed = (int) (NORMAL_SPEED * 3 / UnitConversions.MS_TO_KMH);

    // Get a number of startLocationIdx. And makes sure is less than numberOfFirstThreeSegments.
    int startLocationIdx = NUMBER_OF_LOCATIONS / 8;
    int numberOfFirstThreeSegments = NUMBER_OF_LOCATIONS / 4;
    int numberOfLastSegment = NUMBER_OF_LOCATIONS - numberOfFirstThreeSegments * 3;

    List<CachedLocation> points = createCachedLocations(numberOfFirstThreeSegments, true, slowSpeed);
    points.addAll(createCachedLocations(numberOfFirstThreeSegments, true, normalSpeed));
    points.addAll(createCachedLocations(numberOfFirstThreeSegments, true, fastSpeed));
    points.addAll(createCachedLocations(numberOfLastSegment, true, slowSpeed));

    dynamicSpeedTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, true, points);

    AndroidMock.verify(trackPathDescriptor);
    List<ColoredPath> coloredPath = dynamicSpeedTrackPathPainter.getColoredPaths();
    assertEquals(NUMBER_OF_LOCATIONS - startLocationIdx, coloredPath.size());

    // Checks different speeds with different color in the coloredPath.
    int i = 0;
    for (; i < numberOfFirstThreeSegments - startLocationIdx; i++) {
      assertEquals(getContext().getResources().getColor(R.color.slow_path), coloredPath.get(i)
          .getPathPaint().getColor());
    }
    for (; i < numberOfFirstThreeSegments; i++) {
      assertEquals(getContext().getResources().getColor(R.color.normal_path), coloredPath.get(i)
          .getPathPaint().getColor());
    }
    for (; i < numberOfFirstThreeSegments; i++) {
      assertEquals(getContext().getResources().getColor(R.color.fast_path), coloredPath.get(i)
          .getPathPaint().getColor());
    }
    for (; i < numberOfLastSegment; i++) {
      assertEquals(getContext().getResources().getColor(R.color.slow_path), coloredPath.get(i)
          .getPathPaint().getColor());
    }
  }

  /**
   * Initials a mocked TrackPathDescriptor object.
   */
  @UsesMocks(TrackPathDescriptor.class)
  private void initialTrackPathDescriptorMock() {
    trackPathDescriptor = AndroidMock.createMock(TrackPathDescriptor.class);
    AndroidMock.expect(trackPathDescriptor.getSlowSpeed()).andReturn(SLOW_SPEED);
    AndroidMock.expect(trackPathDescriptor.getNormalSpeed()).andReturn(NORMAL_SPEED);
    AndroidMock.replay(trackPathDescriptor);

    dynamicSpeedTrackPathPainter = new DynamicSpeedTrackPathPainter(getContext(),
        trackPathDescriptor);
  }

  
}
