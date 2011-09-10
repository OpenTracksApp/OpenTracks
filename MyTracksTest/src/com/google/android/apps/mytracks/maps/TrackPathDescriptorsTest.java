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
package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.MockMyTracksOverlay;
import com.google.android.maps.MapView;

import android.graphics.Canvas;
import android.location.Location;
import android.test.AndroidTestCase;

/**
 * Tests for the MyTracks map overlay.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 */
public class TrackPathDescriptorsTest extends AndroidTestCase {
  private Canvas canvas;
  private MockMyTracksOverlay myTracksOverlay;
  private MapView mockView;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    canvas = new Canvas();
    myTracksOverlay = new MockMyTracksOverlay(getContext());
    // Enable drawing.
    myTracksOverlay.setTrackDrawingEnabled(true);
    mockView = null;
  }

  public void testSimpeColorTrackPathPainter() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);

    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    TrackPathPainter painter = new SingleColorTrackPathPainter(getContext());
    int startLocationIdx = 0;
    Boolean alwaysVisible = true;
    
    painter.updatePath(myTracksOverlay.getMapProjection(mockView), 
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, alwaysVisible,
        myTracksOverlay.getPoints());
    painter.drawTrack(canvas);
  }
  
  public void testFixedSpeedTrackPathDescriptor() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);

    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    TrackPathPainter painter = new DynamicSpeedTrackPathPainter(getContext(), new FixedSpeedTrackPathDescriptor(getContext()));
    int startLocationIdx = 0;
    Boolean alwaysVisible = true;
    
    painter.updatePath(myTracksOverlay.getMapProjection(mockView), 
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, alwaysVisible,
        myTracksOverlay.getPoints());
    painter.drawTrack(canvas);
  }
  
  public void testDynamicSpeedTrackPathDescriptor() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    
    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    TrackPathPainter painter = new DynamicSpeedTrackPathPainter(getContext(), new DynamicSpeedTrackPathDescriptor(getContext()));
    int startLocationIdx = 0;
    Boolean alwaysVisible = true;
    
    painter.updatePath(myTracksOverlay.getMapProjection(mockView), 
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, alwaysVisible,
        myTracksOverlay.getPoints());
    painter.drawTrack(canvas);
  }
}
