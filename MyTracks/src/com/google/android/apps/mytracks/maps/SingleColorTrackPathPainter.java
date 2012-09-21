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
import com.google.common.annotations.VisibleForTesting;

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
 * @author Vangelis S.
 */
public class SingleColorTrackPathPainter implements TrackPathPainter {
  private final Paint selectedTrackPaint;
  private Path path;

  public SingleColorTrackPathPainter(Context context) {
    selectedTrackPaint = TrackPathUtils.getPaint(context, R.color.red);
  }

  @Override
  public boolean hasPath() {
    return path != null;
  }

  @Override
  public boolean updateState() {
    return false;
  }

  @Override
  public void updatePath(
      Projection projection, Rect viewRect, int startIndex, List<CachedLocation> points) {
    if (!hasPath()) {
      path = newPath();
    }
    updatePath(projection, viewRect, startIndex, points, path);
  }

  @Override
  public void clearPath() {
    path = null;
  }

  @Override
  public void drawPath(Canvas canvas) {
    if (path != null) {
      canvas.drawPath(path, selectedTrackPaint);
    }
  }

  /**
   * Updates the path.
   * 
   * @param projection the projection
   * @param viewRect the view rectangle
   * @param startIndex the start index
   * @param points the points
   * @param pathToUpdate the path to update
   */
  @VisibleForTesting
  void updatePath(Projection projection, Rect viewRect, int startIndex, List<CachedLocation> points,
      Path pathToUpdate) {
    pathToUpdate.incReserve(points.size() - startIndex);

    boolean hasLastPoint = startIndex != 0 && points.get(startIndex - 1).isValid();
    boolean newSegment = !hasLastPoint;
    // Assume if last point exists, it is visible
    boolean lastPointVisible = hasLastPoint;
    Point point = new Point();

    for (int i = startIndex; i < points.size(); i++) {
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
      projection.toPixels(geoPoint, point);
      if (newSegment) {
        pathToUpdate.moveTo(point.x, point.y);
        newSegment = false;
      } else {
        pathToUpdate.lineTo(point.x, point.y);
      }
    }
  }

  /**
   * Creates a new path.
   */
  @VisibleForTesting
  protected Path newPath() {
    return new Path();
  }

  /**
   * Gets the path.
   */
  @VisibleForTesting
  public Path getPath() {
    return path;
  }

}