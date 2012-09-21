/*
 * Copyright 2008 Google Inc.
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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.maps.TrackPathPainter;
import com.google.android.apps.mytracks.maps.TrackPathPainterFactory;
import com.google.android.apps.mytracks.maps.TrackPathUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A map overlay that displays my location arrow, error circle, and track info.
 * 
 * @author Leif Hendrik Wilden
 */
public class MapOverlay extends Overlay {

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (PreferencesUtils.getKey(context, R.string.track_color_mode_key).equals(key)) {
            trackPathPainter = TrackPathPainterFactory.getTrackPathPainter(context);
          }
        }
      };

  private final Context context;
  private final List<Waypoint> waypoints;
  private final List<CachedLocation> points;
  private final BlockingQueue<CachedLocation> pendingPoints;
  private final Drawable[] arrows;
  private final int arrowWidth;
  private final int arrowHeight;
  private final Drawable statsMarker;
  private final Drawable waypointMarker;
  private final Drawable startMarker;
  private final Drawable endMarker;
  private final int markerWidth;
  private final int markerHeight;
  private final Paint errorCirclePaint;
  private TrackPathPainter trackPathPainter;

  private boolean trackDrawingEnabled;
  private boolean showEndMarker = true;
  private int headingIndex = 0;
  private Location myLocation;

  private GeoPoint lastReferencePoint;
  private Rect lastViewRect;

  /**
   * A pre-processed {@link Location} to speed up drawing.
   * 
   * @author Jimmy Shih
   */
  public static class CachedLocation {

    private final boolean valid;
    private final GeoPoint geoPoint;
    private final int speed;

    /**
     * Constructor for an invalid cached location.
     */
    public CachedLocation() {
      this.valid = false;
      this.geoPoint = null;
      this.speed = -1;
    }

    /**
     * Constructor for a potentially valid cached location.
     */
    public CachedLocation(Location location) {
      this.valid = LocationUtils.isValidLocation(location);
      this.geoPoint = valid ? LocationUtils.getGeoPoint(location) : null;
      this.speed = (int) Math.floor(location.getSpeed() * UnitConversions.MS_TO_KMH);
    }

    /**
     * Returns true if the location is valid.
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Gets the {@link GeoPoint}.
     */
    public GeoPoint getGeoPoint() {
      return geoPoint;
    }

    /**
     * Gets the speed in kilometers per hour.
     */
    public int getSpeed() {
      return speed;
    }
  };

  public MapOverlay(Context context) {
    this.context = context;
    this.waypoints = new ArrayList<Waypoint>();
    this.points = new ArrayList<CachedLocation>(1024);
    this.pendingPoints = new ArrayBlockingQueue<CachedLocation>(
        Constants.MAX_DISPLAYED_TRACK_POINTS, true);

    /*
     * TODO: Use animation rather than individual resources for each arrow
     * direction.
     */
    final Resources resources = context.getResources();
    arrows = new Drawable[] { resources.getDrawable(R.drawable.arrow_0),
        resources.getDrawable(R.drawable.arrow_20), resources.getDrawable(R.drawable.arrow_40),
        resources.getDrawable(R.drawable.arrow_60), resources.getDrawable(R.drawable.arrow_80),
        resources.getDrawable(R.drawable.arrow_100), resources.getDrawable(R.drawable.arrow_120),
        resources.getDrawable(R.drawable.arrow_140), resources.getDrawable(R.drawable.arrow_160),
        resources.getDrawable(R.drawable.arrow_180), resources.getDrawable(R.drawable.arrow_200),
        resources.getDrawable(R.drawable.arrow_220), resources.getDrawable(R.drawable.arrow_240),
        resources.getDrawable(R.drawable.arrow_260), resources.getDrawable(R.drawable.arrow_280),
        resources.getDrawable(R.drawable.arrow_300), resources.getDrawable(R.drawable.arrow_320),
        resources.getDrawable(R.drawable.arrow_340) };
    arrowWidth = arrows[headingIndex].getIntrinsicWidth();
    arrowHeight = arrows[headingIndex].getIntrinsicHeight();
    for (Drawable arrow : arrows) {
      arrow.setBounds(0, 0, arrowWidth, arrowHeight);
    }

    statsMarker = resources.getDrawable(R.drawable.yellow_pushpin);
    markerWidth = statsMarker.getIntrinsicWidth();
    markerHeight = statsMarker.getIntrinsicHeight();
    statsMarker.setBounds(0, 0, markerWidth, markerHeight);

    waypointMarker = resources.getDrawable(R.drawable.blue_pushpin);
    waypointMarker.setBounds(0, 0, markerWidth, markerHeight);

    startMarker = resources.getDrawable(R.drawable.green_dot);
    startMarker.setBounds(0, 0, markerWidth, markerHeight);

    endMarker = resources.getDrawable(R.drawable.red_dot);
    endMarker.setBounds(0, 0, markerWidth, markerHeight);

    errorCirclePaint = TrackPathUtils.getPaint(context, R.color.blue);
    errorCirclePaint.setAlpha(127);

    trackPathPainter = TrackPathPainterFactory.getTrackPathPainter(context);

    context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  /**
   * Add a location to the map overlay.
   * <p>
   * NOTE: This method doesn't take ownership of the given location, so it is
   * safe to reuse the same location while calling this method.
   * 
   * @param location the location
   */
  public void addLocation(Location location) {
    // Queue up in the pendingPoints until it's merged with points.
    if (!pendingPoints.offer(new CachedLocation(location))) {
      Log.e(TAG, "Unable to add to pendingPoints.");
    }
  }

  /**
   * Adds a segment split to the map overlay.
   */
  public void addSegmentSplit() {
    // Queue up in the pendingPoints until it's merged with points.
    if (!pendingPoints.offer(new CachedLocation())) {
      Log.e(TAG, "Unable to add to pendingPoints");
    }
  }

  /**
   * Clears the locations.
   */
  public void clearPoints() {
    synchronized (points) {
      points.clear();
      pendingPoints.clear();
      trackPathPainter.clearPath();
      lastReferencePoint = null;
      lastViewRect = null;
    }
  }

  /**
   * Adds a waypoint to the map overlay.
   * 
   * @param waypoint the waypoint
   */
  public void addWaypoint(Waypoint waypoint) {
    // Note: We don't cache waypoints because it's not worth the effort.
    if (waypoint != null && waypoint.getLocation() != null) {
      synchronized (waypoints) {
        waypoints.add(waypoint);
      }
    }
  }

  /**
   * Clears the waypoints.
   */
  public void clearWaypoints() {
    synchronized (waypoints) {
      waypoints.clear();
    }
  }

  /**
   * Sets whether to draw the track or not.
   * 
   * @param trackDrawingEnabled true to draw track
   */
  public void setTrackDrawingEnabled(boolean trackDrawingEnabled) {
    this.trackDrawingEnabled = trackDrawingEnabled;
  }

  /**
   * Sets whether to draw the end maker or not.
   * 
   * @param showEndMarker true to draw end marker
   */
  public void setShowEndMarker(boolean showEndMarker) {
    this.showEndMarker = showEndMarker;
  }

  /**
   * Sets my location.
   * 
   * @param myLocation my location
   */
  public void setMyLocation(Location myLocation) {
    this.myLocation = myLocation;
  }

  /**
   * Sets the heading.
   * 
   * @param heading the heading
   * @return true if the visible heading has changed.
   */
  public boolean setHeading(float heading) {
    /*
     * Use -heading because the arrow images are counter-clockwise rather than
     * clockwise.
     */
    int index = Math.round(-heading / 360 * 18);
    while (index < 0) {
      index += 18;
    }
    while (index > 17) {
      index -= 18;
    }
    if (index != headingIndex) {
      headingIndex = index;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    if (shadow) {
      return;
    }

    // It's safe to keep projection within a single draw operation
    Projection projection = getMapProjection(mapView);
    if (projection == null) {
      Log.w(TAG, "No projection, unable to draw.");
      return;
    }

    if (trackDrawingEnabled) {
      // Get the current viewing Rect
      Rect viewRect = getMapViewRect(mapView);

      // Draw the selected track
      drawTrack(canvas, projection, viewRect);

      // Draw the start and end markers
      drawMarkers(canvas, projection);

      // Draw the waypoints
      drawWaypoints(canvas, projection);
    }

    // Draw the current location
    drawMyLocation(canvas, projection);
  }

  @Override
  public boolean onTap(GeoPoint geoPoint, MapView mapView) {
    if (geoPoint.equals(mapView.getMapCenter())) {
      /*
       * There is (unfortunately) no good way to determine whether the tap was
       * caused by an actual tap on the screen or the track ball. If the
       * location is equal to the map center,then it was a track ball press with
       * very high likelihood.
       */
      return false;
    }
    final Location tapLocation = LocationUtils.getLocation(geoPoint);
    double minDistance = Double.MAX_VALUE;
    Waypoint waypoint = null;
    synchronized (waypoints) {
      for (int i = 0; i < waypoints.size(); i++) {
        final Location waypointLocation = waypoints.get(i).getLocation();
        if (waypointLocation == null) {
          continue;
        }
        final double distance = waypointLocation.distanceTo(tapLocation);
        if (distance < minDistance) {
          minDistance = distance;
          waypoint = waypoints.get(i);
        }
      }
    }

    if (waypoint != null && minDistance < 15000000 / Math.pow(2, mapView.getZoomLevel())) {
      Intent intent = IntentUtils.newIntent(context, MarkerDetailActivity.class)
          .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, waypoint.getId());
      context.startActivity(intent);
      return true;
    }
    return super.onTap(geoPoint, mapView);
  }

  /**
   * Gets the points.
   */
  @VisibleForTesting
  public List<CachedLocation> getPoints() {
    return points;
  }

  /**
   * Gets the track path painter.
   */
  @VisibleForTesting
  public TrackPathPainter getTrackPathPainter() {
    return trackPathPainter;
  }

  /**
   * Sets the track path painter.
   * 
   * @param trackPathPainter the track path painter
   */
  @VisibleForTesting
  public void setTrackPathPainter(TrackPathPainter trackPathPainter) {
    this.trackPathPainter = trackPathPainter;
  }

  /**
   * Gets the map view projection.
   * 
   * @param mapView the map view
   */
  @VisibleForTesting
  protected Projection getMapProjection(MapView mapView) {
    return mapView.getProjection();
  }

  /**
   * Gets the map view Rect.
   * 
   * @param mapView the map view
   */
  @VisibleForTesting
  protected Rect getMapViewRect(MapView mapView) {
    int width = mapView.getLongitudeSpan();
    int height = mapView.getLatitudeSpan();
    int centerX = mapView.getMapCenter().getLongitudeE6();
    int centerY = mapView.getMapCenter().getLatitudeE6();
    return new Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height
        / 2);
  }

  /**
   * Gets number of locations.
   */
  @VisibleForTesting
  int getNumLocations() {
    synchronized (points) {
      return points.size() + pendingPoints.size();
    }
  }

  /**
   * Gets number of waypoints.
   */
  @VisibleForTesting
  int getNumWaypoints() {
    synchronized (waypoints) {
      return waypoints.size();
    }
  }

  /**
   * Draws the track.
   * 
   * @param canvas the canvas
   * @param projection the projection
   * @param viewRect the view rect
   */
  private void drawTrack(Canvas canvas, Projection projection, Rect viewRect) {
    boolean draw;

    synchronized (points) {
      // Merge the pending points with the list of cached locations.
      GeoPoint referencePoint = projection.fromPixels(0, 0);
      int newPoints = pendingPoints.drainTo(points);
      boolean newProjection = !viewRect.equals(lastViewRect)
          || !referencePoint.equals(lastReferencePoint);
      // Call updateState first to trigger its side effects.
      boolean currentPathValid = !trackPathPainter.updateState() && !newProjection
          && trackPathPainter.hasPath();
      if (newPoints == 0 && currentPathValid) {
        // No need to update
        draw = true;
      } else {
        int numPoints = points.size();
        if (numPoints < 2) {
          // Not enough points to draw a path
          draw = false;
        } else if (currentPathValid) {
          // Incremental update of the path
          draw = true;
          trackPathPainter.updatePath(projection, viewRect, numPoints - newPoints, points);
        } else {
          // Reload the path
          draw = true;
          trackPathPainter.clearPath();
          trackPathPainter.updatePath(projection, viewRect, 0, points);
        }
      }
      lastReferencePoint = referencePoint;
      lastViewRect = viewRect;
    }
    if (draw) {
      trackPathPainter.drawPath(canvas);
    }
  }

  /**
   * Draws the start and end markers.
   * 
   * @param canvas the canvas
   * @param projection the projection
   */
  private void drawMarkers(Canvas canvas, Projection projection) {
    int offsetY = (int) (markerHeight * Constants.MARKER_Y_OFFSET_PERCENTAGE);
    // Draw the end marker
    if (showEndMarker) {
      for (int i = points.size() - 1; i >= 0; i--) {
        if (points.get(i).valid) {
          drawElement(
              canvas, projection, points.get(i).geoPoint, endMarker, -markerWidth / 2, -offsetY);
          break;
        }
      }
    }

    // Draw the start marker
    for (int i = 0; i < points.size(); i++) {
      if (points.get(i).valid) {
        drawElement(
            canvas, projection, points.get(i).geoPoint, startMarker, -markerWidth / 2, -offsetY);
        break;
      }
    }
  }

  /**
   * Draws the waypoints.
   * 
   * @param canvas the canvas
   * @param projection the projection
   */
  private void drawWaypoints(Canvas canvas, Projection projection) {
    synchronized (waypoints) {
      int offsetX = (int) (markerWidth * Constants.WAYPOINT_X_OFFSET_PERCENTAGE);
      for (Waypoint waypoint : waypoints) {
        Location location = waypoint.getLocation();
        Drawable drawable = waypoint.getType() == Waypoint.TYPE_STATISTICS ? statsMarker
            : waypointMarker;
        drawElement(canvas, projection, LocationUtils.getGeoPoint(location), drawable, -offsetX,
            -markerHeight);
      }
    }
  }

  /**
   * Draws my location.
   * 
   * @param canvas the canvas
   * @param projection the projection
   */
  private void drawMyLocation(Canvas canvas, Projection projection) {
    if (myLocation == null) {
      return;
    }
    Point point = drawElement(canvas, projection, LocationUtils.getGeoPoint(myLocation),
        arrows[headingIndex], -(arrowWidth / 2), -(arrowHeight / 2));
    // Draw the error circle
    float radius = projection.metersToEquatorPixels(myLocation.getAccuracy());
    canvas.drawCircle(point.x, point.y, radius, errorCirclePaint);
  }

  /**
   * Draws an element.
   * 
   * @param canvas the canvas
   * @param projection the projection
   * @param geoPoint the geo point
   * @param drawable the drawable
   * @param offsetX the x offset
   * @param offsetY the y offset
   * @return the point of the drawing.
   */
  private Point drawElement(Canvas canvas, Projection projection, GeoPoint geoPoint,
      Drawable drawable, int offsetX, int offsetY) {
    Point point = new Point();
    projection.toPixels(geoPoint, point);
    canvas.save();
    canvas.translate(point.x + offsetX, point.y + offsetY);
    drawable.draw(canvas);
    canvas.restore();
    return point;
  }
}
