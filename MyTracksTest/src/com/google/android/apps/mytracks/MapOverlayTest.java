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
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests for the MyTracks map overlay.
 * 
 * @author Bartlomiej Niechwiej
 */
public class MapOverlayTest extends AndroidTestCase {
  private Canvas canvas;
  private MockMyTracksOverlay myTracksOverlay;
  private MapView mockView;
  private Projection mockProjection;
  
  /**
   * A mock version of {@code MapOverlay} that does not use
   * {@class MapView}. 
   */
  private class MockMyTracksOverlay extends MapOverlay {
    public MockMyTracksOverlay(Context context) {
      super(context);
    }
    @Override
    Projection getMapProjection(MapView mapView) {
      return mockProjection;
    }
    @Override
    Rect getMapViewRect(MapView mapView) {
      return new Rect(0, 0, 100, 100);
    }
    @Override
    Path newPath() {
      return new MockPath();
    }
  }
  
  /**
   * A mock class that intercepts {@code Path}'s and records calls to
   * {@code #moveTo()} and {@code #lineTo()}.
   */
  private static class MockPath extends Path {
    /** A list of disjoined path segments. */
    public final List<List<PointF>> segments = new LinkedList<List<PointF>>();
    /** The total number of points in this path. */
    public int totalPoints;
    
    private List<PointF> currentSegment;

    @Override
    public void lineTo(float x, float y) {
      super.lineTo(x, y);
      assertNotNull(currentSegment);
      currentSegment.add(new PointF(x, y));
      totalPoints++;
    }
    @Override
    public void moveTo(float x, float y) {
      super.moveTo(x, y);
      segments.add(currentSegment =
          new ArrayList<PointF>(Arrays.asList(new PointF(x, y))));
      totalPoints++;
    }
  }

  /**
   * A mock {@code Projection} that acts as the identity matrix.
   */
  private static class MockProjection implements Projection {
    @Override
    public Point toPixels(GeoPoint in, Point out) {
      return out;
    }
    @Override
    public float metersToEquatorPixels(float meters) {
      return meters;
    }
    @Override
    public GeoPoint fromPixels(int x, int y) {
      return new GeoPoint(y, x);
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    canvas = new Canvas();
    myTracksOverlay = new MockMyTracksOverlay(getContext());
    // Enable drawing.
    myTracksOverlay.setTrackDrawingEnabled(true);
    mockView = null;
    mockProjection = new MockProjection();
  }

  public void testAddLocation() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    myTracksOverlay.addLocation(location);
    assertEquals(1, myTracksOverlay.getNumLocations());
    assertEquals(0, myTracksOverlay.getNumWaypoints());
    
    location.setLatitude(20);
    location.setLongitude(30);
    myTracksOverlay.addLocation(location);
    assertEquals(2, myTracksOverlay.getNumLocations());
    assertEquals(0, myTracksOverlay.getNumWaypoints());
    assertNull(myTracksOverlay.getLastPath());
    
    // Draw and make sure that we don't lose any point.
    myTracksOverlay.draw(canvas, mockView, false);
    assertEquals(2, myTracksOverlay.getNumLocations());
    assertEquals(0, myTracksOverlay.getNumWaypoints());
    assertNotNull(myTracksOverlay.getLastPath());
    assertEquals(2, ((MockPath)myTracksOverlay.getLastPath()).totalPoints);
    
    myTracksOverlay.draw(canvas, mockView, true);
    assertEquals(2, myTracksOverlay.getNumLocations());
    assertEquals(0, myTracksOverlay.getNumWaypoints());
    assertNotNull(myTracksOverlay.getLastPath());
  }
  
  public void testClearPoints() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    myTracksOverlay.addLocation(location);
    assertEquals(1, myTracksOverlay.getNumLocations());
    myTracksOverlay.clearPoints();
    assertEquals(0, myTracksOverlay.getNumLocations());

    // Same after drawing on canvas.
    final int locations = 100;
    for (int i = 0; i < locations; ++i) {
      myTracksOverlay.addLocation(location);
    }
    assertEquals(locations, myTracksOverlay.getNumLocations());
    myTracksOverlay.draw(canvas, mockView, false);
    myTracksOverlay.draw(canvas, mockView, true);
    myTracksOverlay.clearPoints();
    assertEquals(0, myTracksOverlay.getNumLocations());
  }

  public void testAddWaypoint() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    Waypoint waypoint = new Waypoint();
    waypoint.setLocation(location);
    myTracksOverlay.addWaypoint(waypoint);
    assertEquals(1, myTracksOverlay.getNumWaypoints());
    assertEquals(0, myTracksOverlay.getNumLocations());
    assertNull(myTracksOverlay.getLastPath());
    
    final int waypoints = 10;
    for (int i = 0; i < waypoints; ++i) {
      waypoint = new Waypoint();
      waypoint.setLocation(location);
      myTracksOverlay.addWaypoint(waypoint);
    }
    assertEquals(1 + waypoints, myTracksOverlay.getNumWaypoints());
    assertEquals(0, myTracksOverlay.getNumLocations());
    assertNull(myTracksOverlay.getLastPath());
  }

  public void testClearWaypoints() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    location.setLongitude(20);
    Waypoint waypoint = new Waypoint();
    waypoint.setLocation(location);
    myTracksOverlay.addWaypoint(waypoint);
    assertEquals(1, myTracksOverlay.getNumWaypoints());
    myTracksOverlay.clearWaypoints();
    assertEquals(0, myTracksOverlay.getNumWaypoints());
  }
  
  public void testDrawing() {
    Location location = new Location("gps");
    location.setLatitude(10);
    for (int i = 0; i < 40; ++i) {
      location.setLongitude(20 + i);
      Waypoint waypoint = new Waypoint();
      waypoint.setLocation(location);
      myTracksOverlay.addWaypoint(waypoint);
    }
    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    // Shadow.
    myTracksOverlay.draw(canvas, mockView, true);
    // We don't expect to do anything if  
    assertNull(myTracksOverlay.getLastPath());
    assertEquals(40, myTracksOverlay.getNumWaypoints());
    assertEquals(100, myTracksOverlay.getNumLocations());

    // No shadow.
    myTracksOverlay.draw(canvas, mockView, false);
    assertNotNull(myTracksOverlay.getLastPath());
    assertTrue(myTracksOverlay.getLastPath() instanceof MockPath);
    MockPath path = (MockPath) myTracksOverlay.getLastPath();
    assertEquals(40, myTracksOverlay.getNumWaypoints());
    assertEquals(100, myTracksOverlay.getNumLocations());
    assertEquals(100, path.totalPoints);
    // TODO: Check the points from the path (and the segments).
  }
}
