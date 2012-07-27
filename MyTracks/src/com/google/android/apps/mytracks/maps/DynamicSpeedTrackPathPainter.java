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
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * A path painter that varies the path colors based on fixed speeds or average
 * speed margin depending of the TrackPathDescriptor passed to its constructor.
 * 
 * @author Vangelis S.
 */
public class DynamicSpeedTrackPathPainter implements TrackPathPainter {
  private final TrackPathDescriptor trackPathDescriptor;
  private final Paint slowPaint;
  private final Paint mediumPaint;
  private final Paint fastPaint;
  private final List<ColoredPath> coloredPaths;

  public DynamicSpeedTrackPathPainter(Context context, TrackPathDescriptor trackPathDescriptor) {
    this.trackPathDescriptor = trackPathDescriptor;
    slowPaint = TrackPathUtils.getPaint(context, R.color.slow_path);
    mediumPaint = TrackPathUtils.getPaint(context, R.color.normal_path);
    fastPaint = TrackPathUtils.getPaint(context, R.color.fast_path);
    coloredPaths = new ArrayList<ColoredPath>();
  }

  @Override
  public boolean hasPath() {
    return !coloredPaths.isEmpty();
  }

  @Override
  public boolean updateState() {
    return trackPathDescriptor.updateState();
  }

  @Override
  public void updatePath(
      Projection projection, Rect viewRect, int startIndex, List<CachedLocation> points) {
    boolean hasLastPoint = startIndex != 0 && points.get(startIndex -1).isValid();
    Point point = new Point();
    if (hasLastPoint) {
      GeoPoint geoPoint = points.get(startIndex -1).getGeoPoint();
      projection.toPixels(geoPoint, point);
    }
    boolean newSegment = !hasLastPoint;
    // Assume if last point exists, it is visible
    boolean lastPointVisible = hasLastPoint;
    int slowSpeed = trackPathDescriptor.getSlowSpeed();
    int normalSpeed = trackPathDescriptor.getNormalSpeed();

    for (int i = startIndex; i < points.size(); ++i) {
      CachedLocation cachedLocation = points.get(i);

      // If not valid, start a new segment
      if (!cachedLocation.isValid()) {
        newSegment = true;
        continue;
      }

      GeoPoint geoPoint = cachedLocation.getGeoPoint();
      // Check if this breaks the existing segment.
      boolean pointVisible = viewRect.contains(geoPoint.getLongitudeE6(), geoPoint.getLatitudeE6());
      if (!pointVisible && !lastPointVisible) {
        // This point and the last point are both outside visible area.
        newSegment = true;
      }
      lastPointVisible = pointVisible;

      // Either update point or draw a line from the last point
      if (newSegment) {
        projection.toPixels(geoPoint, point);
        newSegment = false;
      } else {
        ColoredPath coloredPath;
        if (cachedLocation.getSpeed() <= slowSpeed) {
          coloredPath = new ColoredPath(slowPaint);
        } else if (cachedLocation.getSpeed() <= normalSpeed) {
          coloredPath = new ColoredPath(mediumPaint);
        } else {
          coloredPath = new ColoredPath(fastPaint);
        }
        coloredPath.getPath().moveTo(point.x, point.y);
        projection.toPixels(geoPoint, point);
        coloredPath.getPath().lineTo(point.x, point.y);
        coloredPaths.add(coloredPath);
      }
    }
  }

  @Override
  public void clearPath() {
    coloredPaths.clear();
  }

  @Override
  public void drawPath(Canvas canvas) {
    for (int i = 0; i < coloredPaths.size(); i++) {
      ColoredPath coloredPath = coloredPaths.get(i);
      canvas.drawPath(coloredPath.getPath(), coloredPath.getPathPaint());
    }
  }

  /**
   * Gets the colored paths.
   */
  @VisibleForTesting
  List<ColoredPath> getColoredPaths() {
    return coloredPaths;
  }
}