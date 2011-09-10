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

import com.google.android.apps.mytracks.ColoredPath;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A path painter that varies the path colors based on fixed speeds or average speed margin
 * depending of the TrackPathDescriptor passed to its constructor.
 * 
 *  @author Vangelis S.
 */
public class  DynamicSpeedTrackPathPainter implements TrackPathPainter {
  private final Paint selectedTrackPaintSlow;
  private final Paint selectedTrackPaintMedium;
  private final Paint selectedTrackPaintFast;
  private final List<ColoredPath> coloredPaths;
  private final TrackPathDescriptor trackPathDescriptor;
  private int slowSpeed;
  private int normalSpeed;
  
  public DynamicSpeedTrackPathPainter (Context context, TrackPathDescriptor trackPathDescriptor) {
    this.trackPathDescriptor = trackPathDescriptor;
    
    selectedTrackPaintSlow = TrackPathUtilities.getPaint(R.color.slow_path, context);
    selectedTrackPaintMedium = TrackPathUtilities.getPaint(R.color.normal_path, context);
    selectedTrackPaintFast = TrackPathUtilities.getPaint(R.color.fast_path, context);
    
    this.coloredPaths = new ArrayList<ColoredPath>();
  }
  
  @Override
  public void drawTrack(Canvas canvas) {
    for(int i = 0; i < coloredPaths.size(); ++i) {
      ColoredPath coloredPath = coloredPaths.get(i);
      canvas.drawPath(coloredPath.getPath(), coloredPath.getPathPaint());
    }
  }
  
  @Override
  public void updatePath(Projection projection, Rect viewRect, int startLocationIdx, 
      Boolean alwaysVisible, List<CachedLocation> points) {
    // Whether to start a new segment on new valid and visible point.
    boolean newSegment = startLocationIdx <= 0 || !points.get(startLocationIdx - 1).valid; 
    boolean lastVisible = !newSegment;
    final Point pt = new Point();
    
    clear();
    
    slowSpeed = trackPathDescriptor.getSlowSpeed();
    normalSpeed = trackPathDescriptor.getNormalSpeed();
    
    // Loop over track points.
    for (int i = startLocationIdx; i < points.size(); ++i) {
      CachedLocation loc = points.get(i);
      
      // Check if valid, if not then indicate a new segment.
      if (!loc.valid) {
        newSegment = true;
        continue;
      }
      
      final GeoPoint geoPoint = loc.geoPoint;
      // Check if this breaks the existing segment.
      boolean visible = alwaysVisible || viewRect.contains(
          geoPoint.getLongitudeE6(), geoPoint.getLatitudeE6());
      if (!visible && !lastVisible) {
        // This is a point outside view not connected to a visible one.
        newSegment = true;
      }
      lastVisible = visible;
      
      // Either move to beginning of a new segment or continue the old one.
      if (newSegment) {
        projection.toPixels(geoPoint, pt);
        newSegment = false;
      } else {
        ColoredPath coloredPath;
        if(loc.speed <= slowSpeed) {
          coloredPath = new ColoredPath(selectedTrackPaintSlow);
        }
        else if(loc.speed <= normalSpeed) {
          coloredPath = new ColoredPath(selectedTrackPaintMedium);
        } else {
          coloredPath = new ColoredPath(selectedTrackPaintFast);
        }
        coloredPath.getPath().moveTo(pt.x, pt.y);
        projection.toPixels(geoPoint, pt);
        coloredPath.getPath().lineTo(pt.x, pt.y);
        coloredPaths.add(coloredPath);
      }
    }
  }
  
  @Override
  public void clear()
  {
    coloredPaths.clear();
  }

  @Override
  public boolean needsRedraw() {
    return trackPathDescriptor.needsRedraw();
  }
  
  @Override
  public Path getLastPath() {
    Path path = new Path();
    for(int i = 0; i < coloredPaths.size(); ++i) {
      path.addPath(coloredPaths.get(i).getPath());
    }
    return path;
  }
}