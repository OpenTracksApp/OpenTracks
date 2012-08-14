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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.maps.SingleColorTrackPathPainter;
import com.google.android.maps.MapView;

import android.graphics.Canvas;
import android.graphics.Path;
import android.location.Location;
import android.test.AndroidTestCase;

/**
 * Tests {@link MapOverlay}.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 */
public class MapOverlayTest extends AndroidTestCase {
  private Canvas canvas;
  private MockMyTracksOverlay mockMyTracksOverlay;
  private MapView mapView;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    canvas = new Canvas();
    mockMyTracksOverlay = new MockMyTracksOverlay(getContext());

    // Enable drawing.
    mockMyTracksOverlay.setTrackDrawingEnabled(true);

    // Set a TrackPathPainter with a MockPath.
    mockMyTracksOverlay.setTrackPathPainter(new SingleColorTrackPathPainter(getContext()) {
        @Override
      public Path newPath() {
        return new MockPath();
      }
    });

    mapView = null;
  }

  /**
   * Tests {@link MapOverlay#addLocation(Location)}.
   */
  public void testAddLocation() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    mockMyTracksOverlay.addLocation(location);
    assertEquals(1, mockMyTracksOverlay.getNumLocations());
    assertEquals(0, mockMyTracksOverlay.getNumWaypoints());

    location.setLatitude(20);
    location.setLongitude(30);
    mockMyTracksOverlay.addLocation(location);
    assertEquals(2, mockMyTracksOverlay.getNumLocations());
    assertEquals(0, mockMyTracksOverlay.getNumWaypoints());
    assertFalse(mockMyTracksOverlay.getTrackPathPainter().hasPath());

    // Draw and make sure that we don't lose any point.
    mockMyTracksOverlay.draw(canvas, mapView, false);
    assertEquals(2, mockMyTracksOverlay.getNumLocations());
    assertEquals(0, mockMyTracksOverlay.getNumWaypoints());
    assertTrue(mockMyTracksOverlay.getTrackPathPainter().hasPath());
    SingleColorTrackPathPainter trackPathPainter = (SingleColorTrackPathPainter) mockMyTracksOverlay
        .getTrackPathPainter();
    MockPath path = (MockPath) trackPathPainter.getPath();
    assertEquals(2, path.getTotalPoints());

    mockMyTracksOverlay.draw(canvas, mapView, true);
    assertEquals(2, mockMyTracksOverlay.getNumLocations());
    assertEquals(0, mockMyTracksOverlay.getNumWaypoints());
    assertTrue(mockMyTracksOverlay.getTrackPathPainter().hasPath());
  }

  /**
   * Tests {@link MapOverlay#clearPoints()}.
   */
  public void testClearPoints() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    mockMyTracksOverlay.addLocation(location);
    assertEquals(1, mockMyTracksOverlay.getNumLocations());
    mockMyTracksOverlay.clearPoints();
    assertEquals(0, mockMyTracksOverlay.getNumLocations());

    // Test after drawing on canvas
    final int locations = 100;
    for (int i = 0; i < locations; ++i) {
      mockMyTracksOverlay.addLocation(location);
    }
    assertEquals(locations, mockMyTracksOverlay.getNumLocations());
    mockMyTracksOverlay.draw(canvas, mapView, false);
    mockMyTracksOverlay.draw(canvas, mapView, true);
    mockMyTracksOverlay.clearPoints();
    assertEquals(0, mockMyTracksOverlay.getNumLocations());
  }

  /**
   * Tests {@link MapOverlay#addWaypoint(Waypoint)}.
   */
  public void testAddWaypoint() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    Waypoint waypoint = new Waypoint();
    waypoint.setLocation(location);
    mockMyTracksOverlay.addWaypoint(waypoint);
    assertEquals(1, mockMyTracksOverlay.getNumWaypoints());
    assertEquals(0, mockMyTracksOverlay.getNumLocations());
    assertFalse(mockMyTracksOverlay.getTrackPathPainter().hasPath());

    final int waypoints = 10;
    for (int i = 0; i < waypoints; ++i) {
      waypoint = new Waypoint();
      waypoint.setLocation(location);
      mockMyTracksOverlay.addWaypoint(waypoint);
    }
    assertEquals(1 + waypoints, mockMyTracksOverlay.getNumWaypoints());
    assertEquals(0, mockMyTracksOverlay.getNumLocations());
    assertFalse(mockMyTracksOverlay.getTrackPathPainter().hasPath());
  }

  /**
   * Tests {@link MapOverlay#clearWaypoints()}.
   */
  public void testClearWaypoints() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    Waypoint waypoint = new Waypoint();
    waypoint.setLocation(location);
    mockMyTracksOverlay.addWaypoint(waypoint);
    assertEquals(1, mockMyTracksOverlay.getNumWaypoints());
    mockMyTracksOverlay.clearWaypoints();
    assertEquals(0, mockMyTracksOverlay.getNumWaypoints());
  }

  /**
   * Tests {@link MapOverlay#draw(Canvas, MapView, boolean)}.
   */
  public void testDrawing() {
    Location location = new Location("gps");
    location.setLatitude(10);
    for (int i = 0; i < 40; ++i) {
      location.setLongitude(20 + i);
      Waypoint waypoint = new Waypoint();
      waypoint.setLocation(location);
      mockMyTracksOverlay.addWaypoint(waypoint);
    }
    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      mockMyTracksOverlay.addLocation(location);
    }

    // Shadow.
    mockMyTracksOverlay.draw(canvas, mapView, true);
    // We don't expect to do anything if
    assertFalse(mockMyTracksOverlay.getTrackPathPainter().hasPath());
    assertEquals(40, mockMyTracksOverlay.getNumWaypoints());
    assertEquals(100, mockMyTracksOverlay.getNumLocations());

    // No shadow.
    mockMyTracksOverlay.draw(canvas, mapView, false);
    assertTrue(mockMyTracksOverlay.getTrackPathPainter().hasPath());
    SingleColorTrackPathPainter trackPathPainter = (SingleColorTrackPathPainter) mockMyTracksOverlay
        .getTrackPathPainter();
    MockPath path = (MockPath) trackPathPainter.getPath();
    assertEquals(40, mockMyTracksOverlay.getNumWaypoints());
    assertEquals(100, mockMyTracksOverlay.getNumLocations());
    assertEquals(100, path.getTotalPoints());
  }
}
