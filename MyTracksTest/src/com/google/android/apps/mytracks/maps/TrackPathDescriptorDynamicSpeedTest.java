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

import android.location.Location;

/**
 * Tests for the MyTracks track path descriptors and painters.
 * 
 * @author Vangelis S.
 */
public class TrackPathDescriptorDynamicSpeedTest extends TrackPathPainterTestCase {
  
  public void testDynamicSpeedTrackPathDescriptor() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    
    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    TrackPathPainter painter = new DynamicSpeedTrackPathPainter(
        getContext(), new DynamicSpeedTrackPathDescriptor(getContext()));
    myTracksOverlay.setTrackPathPainter(painter);
    
    int startLocationIdx = 0;

    assertNotNull(painter);
    painter.updatePath(myTracksOverlay.getMapProjection(mockView), 
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx,
        myTracksOverlay.getPoints());
    assertNotNull(myTracksOverlay.getTrackPathPainter().hasPath());
    painter.drawPath(canvas);    
  }
}
