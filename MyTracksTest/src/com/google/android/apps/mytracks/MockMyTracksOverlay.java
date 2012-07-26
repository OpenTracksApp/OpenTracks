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
 * A mock {@link MapOverlay} for testing.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 * 
 */
public class MockMyTracksOverlay extends MapOverlay {
   
  private Projection projection;
  
  public MockMyTracksOverlay(Context context) {
    super(context);
    projection = new MockProjection();
  }
  
  @Override
  public Projection getMapProjection(MapView mapView) {
    return projection;
  }
  
  @Override
  public Rect getMapViewRect(MapView mapView) {
    return new Rect(0, 0, (int) (100 * 1E6), (int) (100 * 1E6));
  }
}