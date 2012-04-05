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

import com.google.android.apps.mytracks.ColoredPath;
import com.google.android.apps.mytracks.MapOverlay.CachedLocation;
import com.google.android.apps.mytracks.TrackStubUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import java.util.List;

/**
 * Tests for the {@link DynamicSpeedTrackPathPainter}.
 * 
 * @author Youtao Liu
 */
public class DynamicSpeedTrackPathPainterTest extends TrackPathPainterTestCase {

  private DynamicSpeedTrackPathPainter dynamicSpeedTrackPathPainter;
  private TrackPathDescriptor trackPathDescriptor;
  // This number must bigger than 10 to meets the requirement of test.
  private static final int NUMBER_OF_LOCATIONS = 100;
  private static final int NUMBER_OF_SEGMENTS = 4;
  private static final int LOCATIONS_PER_SEGMENT = 25;
  // The maximum speed which is considered slow.
  private static final int SLOW_SPEED = 30;
  // The maximum speed which is considered normal.
  private static final int NORMAL_SPEED = 50;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    initialTrackPathDescriptorMock();
    dynamicSpeedTrackPathPainter = new DynamicSpeedTrackPathPainter(getContext(),
        trackPathDescriptor);
  }

  /**
   * Tests the method
   * {@link DynamicSpeedTrackPathPainter#updatePath(com.google.android.maps.Projection, android.graphics.Rect, int, Boolean, java.util.List)}
   * when all locations are invalid.
   */
  public void testUpdatePath_AllInvalidLocation() {
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS, INVALID_LATITUDE, -1);
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
    List<CachedLocation> points = createCachedLocations(NUMBER_OF_LOCATIONS,
        TrackStubUtils.INITIAL_LATITUDE, -1);

    // Gets a number as the start index of points.
    int startLocationIdx = NUMBER_OF_LOCATIONS / 2;
    dynamicSpeedTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, true, points);

    AndroidMock.verify(trackPathDescriptor);
    assertEquals(NUMBER_OF_LOCATIONS - startLocationIdx, dynamicSpeedTrackPathPainter
        .getColoredPaths().size());
  }

  /**
   * Tests the
   * {@link DynamicSpeedTrackPathPainter#updatePath(com.google.android.maps.Projection, android.graphics.Rect, int, Boolean, java.util.List)}
   * when all locations are valid. This test setups 4 segments with 25 points
   * each. The first segment has slow speed, the second segment has normal
   * speed, the third segment has fast speed, and the fourth segment has slow
   * speed.
   */
  public void testUpdatePath_CheckColoredPath() {
    // Gets the slow speed. Divide SLOW_SPEED by 2 to make it smaller than
    // SLOW_SPEED.
    int slowSpeed = (int) (SLOW_SPEED / (2 * UnitConversions.MS_TO_KMH));
    // Gets the normal speed. Makes it smaller than SLOW_SPEED and bigger than
    // NORMAL_SPEED.
    int normalSpeed = (int) ((SLOW_SPEED + NORMAL_SPEED) / (2 * UnitConversions.MS_TO_KMH));
    // Gets the fast speed. Multiply it by 2 to make it bigger than
    // NORMAL_SPEED.
    int fastSpeed = (int) (NORMAL_SPEED * 2 / UnitConversions.MS_TO_KMH);

    // Get a number of startLocationIdx. And divide NUMBER_OF_LOCATIONS by 8 to
    // make sure it is less than numberOfFirstThreeSegments.
    int startLocationIdx = NUMBER_OF_LOCATIONS / NUMBER_OF_SEGMENTS / 2;

    List<CachedLocation> points = createCachedLocations(LOCATIONS_PER_SEGMENT,
        TrackStubUtils.INITIAL_LATITUDE, slowSpeed);
    points.addAll(createCachedLocations(LOCATIONS_PER_SEGMENT, TrackStubUtils.INITIAL_LATITUDE,
        normalSpeed));
    points.addAll(createCachedLocations(LOCATIONS_PER_SEGMENT, TrackStubUtils.INITIAL_LATITUDE,
        fastSpeed));
    points.addAll(createCachedLocations(LOCATIONS_PER_SEGMENT, TrackStubUtils.INITIAL_LATITUDE,
        slowSpeed));

    dynamicSpeedTrackPathPainter.updatePath(myTracksOverlay.getMapProjection(mockView),
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, true, points);

    AndroidMock.verify(trackPathDescriptor);
    List<ColoredPath> coloredPath = dynamicSpeedTrackPathPainter.getColoredPaths();
    assertEquals(NUMBER_OF_LOCATIONS - startLocationIdx, coloredPath.size());

    // Checks different speeds with different color in the coloredPath.
    for (int i = 0; i < NUMBER_OF_LOCATIONS - startLocationIdx; i++) {
      if (i < LOCATIONS_PER_SEGMENT - startLocationIdx) {
        // Slow.
        assertEquals(getContext().getResources().getColor(R.color.slow_path), coloredPath.get(i)
            .getPathPaint().getColor());
      } else if (i < LOCATIONS_PER_SEGMENT * 2 - startLocationIdx) {
        // Normal.
        assertEquals(getContext().getResources().getColor(R.color.normal_path), coloredPath.get(i)
            .getPathPaint().getColor());
      } else if (i < LOCATIONS_PER_SEGMENT * 3 - startLocationIdx) {
        // Fast.
        assertEquals(getContext().getResources().getColor(R.color.fast_path), coloredPath.get(i)
            .getPathPaint().getColor());
      } else {
        // Slow.
        assertEquals(getContext().getResources().getColor(R.color.slow_path), coloredPath.get(i)
            .getPathPaint().getColor());
      }
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
  }

}
