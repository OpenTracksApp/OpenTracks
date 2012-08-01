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
package com.google.android.apps.mytracks;

import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import android.content.Context;
import android.graphics.Rect;

/**
 * Elements for Tests for the MyTracks map overlay.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 * 
 * A mock version of {@code MapOverlay} that does not use
 * {@class MapView}. 
 */
public class MockMyTracksOverlay extends MapOverlay {
   
  private Projection mockProjection;
  
  public MockMyTracksOverlay(Context context) {
    super(context);
    mockProjection = new MockProjection();
  }
  
  @Override
  public Projection getMapProjection(MapView mapView) {
    return mockProjection;
  }
  
  @Override
  public Rect getMapViewRect(MapView mapView) {
    return new Rect(0, 0, 100, 100);
  }
}