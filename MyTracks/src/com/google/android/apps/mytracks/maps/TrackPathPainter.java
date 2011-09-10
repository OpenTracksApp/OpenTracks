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

import com.google.android.apps.mytracks.MapOverlay.CachedLocation;
import com.google.android.maps.Projection;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.List;

/**
 * An interface for classes which paint the track path. 
 *
 * @author Vangelis S.
 */
public interface TrackPathPainter {
	
  /**
   * Clears the related data.
   */
  void clear();
  
  /**
   * Draws the path to the canvas.
   * @param canvas The Canvas to draw upon
   */
  void drawTrack(Canvas canvas);
  
  /**
   * Updates the path.
   * @param projection The Canvas to draw upon.
   * @param viewRect The Path to be drawn.   
   * @param startLocationIdx The start point from where update the path.
   * @param alwaysVisible Flag for alwaysvisible.
   * @param points The list of points used to update the path.
   */
  void updatePath(Projection projection, Rect viewRect, int startLocationIdx,
      Boolean alwaysVisible, List<CachedLocation> points);
  
  /**
   * @return True if the path needs to be updated.
   */
  boolean needsRedraw();
  
  /**
   * @return The path being used currently.
   */
  Path getLastPath();
}



