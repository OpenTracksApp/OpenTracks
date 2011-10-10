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

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

/**
 * Tests for the MyTracks track path painter factory.
 * 
 * @author Vangelis S.
 */
public class TrackPathPainterFactoryTest extends TrackPathPainterTestCase {
  
  public void testTrackPathPainterFactory() throws Exception {
    Location location = new Location("gps");
    location.setLatitude(10);
    
    for (int i = 0; i < 100; ++i) {
      location = new Location("gps");
      location.setLatitude(20 + i / 2);
      location.setLongitude(150 - i);
      myTracksOverlay.addLocation(location);
    }
    
    Context context = getContext();
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    if (prefs == null) {
      return;
    }
    
    testTrackPathPainterFactorySpecific(context, prefs, R.string.track_color_mode_none, 
        SingleColorTrackPathPainter.class);
    testTrackPathPainterFactorySpecific(context, prefs, R.string.track_color_mode_fixed, 
        DynamicSpeedTrackPathPainter.class);
    testTrackPathPainterFactorySpecific(context, prefs, R.string.track_color_mode_dynamic, 
        DynamicSpeedTrackPathPainter.class);
  }
  
  private <T> void testTrackPathPainterFactorySpecific(Context context, SharedPreferences prefs, 
      int track_color_mode, Class <?> c) {
    prefs.edit().putString(context.getString(R.string.track_color_mode_key), 
        context.getString(track_color_mode)).commit();
    
    int startLocationIdx = 0;
    Boolean alwaysVisible = true;
    
    TrackPathPainter painter = TrackPathPainterFactory.getTrackPathPainter(context);
    
    assertNotNull(painter);
    assertTrue(c.isInstance(painter));
    
    painter.updatePath(myTracksOverlay.getMapProjection(mockView), 
        myTracksOverlay.getMapViewRect(mockView), startLocationIdx, alwaysVisible,
        myTracksOverlay.getPoints());
    painter.drawTrack(canvas);
  }
}
