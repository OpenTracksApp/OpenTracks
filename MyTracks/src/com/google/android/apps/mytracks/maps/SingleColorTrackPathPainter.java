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
import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.List;


/**
 * A path painter that not variates the path colors.
 *
 *  @author Vangelis S.
 */
public class SingleColorTrackPathPainter implements TrackPathPainter {
  private final Paint selectedTrackPaint;
  private Path path;
  
  public SingleColorTrackPathPainter(Context context) {
    selectedTrackPaint = TrackPathUtilities.getPaint(R.color.red, context);
  }

  @Override
  public void drawTrack(Canvas canvas) {
    canvas.drawPath(path, selectedTrackPaint);
  }
  
  @Override
  public void updatePath(Projection projection, Rect viewRect, int startLocationIdx,
      Boolean alwaysVisible, List<CachedLocation> points) {
    // Whether to start a new segment on new valid and visible point.
    boolean newSegment = startLocationIdx <= 0 || !points.get(startLocationIdx - 1).valid; 
    boolean lastVisible = !newSegment;
    final Point pt = new Point();
    // Loop over track points.
    int numPoints = points.size();
    path = newPath();
    path.incReserve(numPoints);
    
    for (int i = startLocationIdx; i < numPoints ; ++i) {
      CachedLocation loc = points.get(i);
      
      // Check if valid, if not then indicate a new segment.
      if (!loc.valid) {
        newSegment = true;
        continue;
      }
      
      final GeoPoint geoPoint = loc.geoPoint;
      // Check if this breaks the existing segment.
      boolean visible = alwaysVisible 
          || viewRect.contains(geoPoint.getLongitudeE6(), geoPoint.getLatitudeE6());
      if (!visible && !lastVisible) {
        // This is a point outside view not connected to a visible one.
        newSegment = true;
      }
      lastVisible = visible;
      
      // Either move to beginning of a new segment or continue the old one.
      projection.toPixels(geoPoint, pt);
      if (newSegment) {
        path.moveTo(pt.x, pt.y);
        newSegment = false;
      } else {
        path.lineTo(pt.x, pt.y);
      }
    }
  }
  
  @Override
  public void clear() {
    path = null;
  }

  @Override
  public boolean needsRedraw() {
    return false;
  }
  
  @Override
  public Path getLastPath() {
    return path;
  }
  
  // Visible for testing
  public Path newPath() {
    return new Path();
  }
}