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
import android.graphics.Rect;

import java.util.List;

/**
 * An interface for classes which paint the track path.
 * 
 * @author Vangelis S.
 */
public interface TrackPathPainter {

  /**
   * Returns true if has path.
   */
  public boolean hasPath();

  /**
   * Updates state. Returns true if the state is updated.
   */
  public boolean updateState();

  /**
   * Updates the path. Creates a new path if necessary
   * 
   * @param projection the projection
   * @param viewRect the view rectangle
   * @param startIndex the start index
   * @param points the points
   */
  public void updatePath(
      Projection projection, Rect viewRect, int startIndex, List<CachedLocation> points);

  /**
   * Clears the path.
   */
  public void clearPath();

  /**
   * Draws the path.
   * 
   * @param canvas the canvas
   */
  public void drawPath(Canvas canvas);
}
