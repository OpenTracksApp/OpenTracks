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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.MyTracksUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;

import java.util.ArrayList;

/**
 * A map overlay that displays a "MyLocation" arrow, an error circle, the
 * currently recording track and optionally a selected track.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksOverlay extends Overlay {

  private final Drawable arrow[] = new Drawable[18];
  private final int arrowWidth, arrowHeight;
  private final Drawable statsMarker;
  private final Drawable waypointMarker;
  private final Drawable startMarker;
  private final Drawable endMarker;
  private final int markerWidth, markerHeight;
  private final Paint selectedTrackPaint;
  private final Paint errorCirclePaint;
  private final Context context;
  private final ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();

  private Track selectedTrack;
  private int lastHeading = 0;
  private Location myLocation;
  private boolean showEndMarker = true;

  public MyTracksOverlay(Context context) {
    this.context = context;

    // TODO: Can we use a FrameAnimation or similar here rather
    // than individual resources for each arrow direction?

    arrow[0] = context.getResources().getDrawable(R.drawable.arrow_0);
    arrow[1] = context.getResources().getDrawable(R.drawable.arrow_20);
    arrow[2] = context.getResources().getDrawable(R.drawable.arrow_40);
    arrow[3] = context.getResources().getDrawable(R.drawable.arrow_60);
    arrow[4] = context.getResources().getDrawable(R.drawable.arrow_80);
    arrow[5] = context.getResources().getDrawable(R.drawable.arrow_100);
    arrow[6] = context.getResources().getDrawable(R.drawable.arrow_120);
    arrow[7] = context.getResources().getDrawable(R.drawable.arrow_140);
    arrow[8] = context.getResources().getDrawable(R.drawable.arrow_160);
    arrow[9] = context.getResources().getDrawable(R.drawable.arrow_180);
    arrow[10] = context.getResources().getDrawable(R.drawable.arrow_200);
    arrow[11] = context.getResources().getDrawable(R.drawable.arrow_220);
    arrow[12] = context.getResources().getDrawable(R.drawable.arrow_240);
    arrow[13] = context.getResources().getDrawable(R.drawable.arrow_260);
    arrow[14] = context.getResources().getDrawable(R.drawable.arrow_280);
    arrow[15] = context.getResources().getDrawable(R.drawable.arrow_300);
    arrow[16] = context.getResources().getDrawable(R.drawable.arrow_320);
    arrow[17] = context.getResources().getDrawable(R.drawable.arrow_340);
    arrowWidth = arrow[lastHeading].getIntrinsicWidth();
    arrowHeight = arrow[lastHeading].getIntrinsicHeight();
    for (int i = 0; i <= 17; i++) {
      arrow[i].setBounds(0, 0, arrowWidth, arrowHeight);
    }

    statsMarker = context.getResources().getDrawable(R.drawable.ylw_pushpin);
    markerWidth = statsMarker.getIntrinsicWidth();
    markerHeight = statsMarker.getIntrinsicHeight();
    statsMarker.setBounds(0, 0, markerWidth, markerHeight);

    startMarker = context.getResources().getDrawable(R.drawable.green_dot);
    startMarker.setBounds(0, 0, markerWidth, markerHeight);

    endMarker = context.getResources().getDrawable(R.drawable.red_dot);
    endMarker.setBounds(0, 0, markerWidth, markerHeight);

    waypointMarker =
        context.getResources().getDrawable(R.drawable.blue_pushpin);
    waypointMarker.setBounds(0, 0, markerWidth, markerHeight);

    selectedTrackPaint = new Paint();
    selectedTrackPaint.setColor(context.getResources().getColor(R.color.red));
    selectedTrackPaint.setStrokeWidth(3);
    selectedTrackPaint.setStyle(Paint.Style.STROKE);
    selectedTrackPaint.setAntiAlias(true);

    errorCirclePaint = new Paint();
    errorCirclePaint.setColor(context.getResources().getColor(R.color.blue));
    errorCirclePaint.setStyle(Paint.Style.STROKE);
    errorCirclePaint.setStrokeWidth(3);
    errorCirclePaint.setAlpha(127);
    errorCirclePaint.setAntiAlias(true);
  }

  public void setSelectedTrack(Track track) {
    selectedTrack = track;
  }

  public Track getSelectedTrack() {
    return selectedTrack;
  }

  public void addWaypoint(Waypoint wpt) {
    waypoints.add(wpt);
  }

  public void clearWaypoints() {
    waypoints.clear();
  }

  public void setShowEndMarker(boolean showEndMarker) {
    this.showEndMarker = showEndMarker;
  }

  @Override
  public void draw(Canvas canvas, MapView mapView, boolean shadow) {

    if (shadow) {
      return;
    }

    // Draw the selected track:
    drawTrack(canvas, mapView, selectedTrack, true);

    // Draw the waypoints:
    ArrayList<Waypoint> currentWaypoints = waypoints;
    for (int i = 1; i < currentWaypoints.size(); i++) {
      Location loc = currentWaypoints.get(i).getLocation();
      if (loc == null) {
        continue;
      }
      GeoPoint geoPoint = MyTracksUtils.getGeoPoint(loc);
      Point pt = new Point();
      mapView.getProjection().toPixels(geoPoint, pt);
      canvas.save();
      canvas.translate(pt.x - (markerWidth / 2) + 3, pt.y - (markerHeight));
      if (currentWaypoints.get(i).getType() == Waypoint.TYPE_STATISTICS) {
        statsMarker.draw(canvas);
      } else {
        waypointMarker.draw(canvas);
      }
      canvas.restore();
    }

    // Draw the arrow icon:
    if (myLocation == null) {
      return;
    }
    GeoPoint geoPoint = new GeoPoint(
        (int) (myLocation.getLatitude() * 1E6),
        (int) (myLocation.getLongitude() * 1E6));
    Point pt = new Point();
    mapView.getProjection().toPixels(geoPoint, pt);
    canvas.save();
    canvas.translate(pt.x - (arrowWidth / 2), pt.y - (arrowHeight / 2));
    arrow[lastHeading].draw(canvas);
    canvas.restore();

    // Draw the error circle:
    float radius =
        mapView.getProjection().metersToEquatorPixels(myLocation.getAccuracy());
    canvas.drawCircle(pt.x, pt.y, radius, errorCirclePaint);
  }

  public void drawTrack(Canvas canvas, MapView mapView, Track track,
      boolean selected) {

    if (track == null) {
      return;
    }
    ArrayList<Location> points = track.getLocations();
    if (points.size() < 2) {
      return;
    }

    // Get the current viewing window:
    int w = mapView.getLongitudeSpan();
    int h = mapView.getLatitudeSpan();
    int cx = mapView.getMapCenter().getLongitudeE6();
    int cy = mapView.getMapCenter().getLatitudeE6();
    Rect rect = new Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);

    Point pt = new Point();
    GeoPoint geoPoint;
    Location loc;
    Location lastValidLocation = null;
    Path path;
    boolean wasInside;

    // Do as much allocation and preparation outside the loop over track
    // points:
    wasInside = false;
    int locLon = 0, locLat = 0, minLon, maxLon, minLat, maxLat;
    int lastLocLon = (int) (points.get(0).getLongitude() * 1E6);
    int lastLocLat = (int) (points.get(0).getLatitude() * 1E6);

    // Line decimation for dummies:
    // Skip di additional points, where di depends on zoom level:
    int di = 0;
    int dl = 17 - mapView.getZoomLevel();
    if (dl > 0) {
      di += (dl * dl);
    }

    // Loop over track points:
    path = new Path();
    for (int i = 1; i < points.size(); i++) {

      loc = points.get(i);
      if (loc == null) {
        continue;
      }
      locLon = (int) (loc.getLongitude() * 1E6);
      locLat = (int) (loc.getLatitude() * 1E6);

      // Skip to every n-th point (depends on zoom level, see above):
      for (int j = 0; j < di && i < points.size() - 1; j++) {
        // TODO Check the thread synchronization.
        // There is no reason that points.get(i + 1) should be null but users
        // have reported it causing null pointer exceptions.
        if ((locLat > 90E6) ||
            (lastLocLat > 90E6) ||
            (points.get(i + 1) == null) ||
            (points.get(i + 1).getLatitude() > 90)) {
          break;
        }
        i++;
        loc = points.get(i);
        locLon = (int) (loc.getLongitude() * 1E6);
        locLat = (int) (loc.getLatitude() * 1E6);
      }

      // Draw a line segment if it's inside the viewing window:
      if (locLat < 90E6 && lastLocLat < 90E6) {
        lastValidLocation = loc;
        minLon = Math.min(locLon, lastLocLon);
        maxLon = Math.max(locLon, lastLocLon);
        minLat = Math.min(locLat, lastLocLat);
        maxLat = Math.max(locLat, lastLocLat);
        if (rect.intersects(minLon, minLat, maxLon, maxLat)) {
          if (!wasInside) {
            geoPoint = new GeoPoint(lastLocLat, lastLocLon);
            mapView.getProjection().toPixels(geoPoint, pt);
            path.moveTo(pt.x, pt.y);
          }
          geoPoint = new GeoPoint(locLat, locLon);
          mapView.getProjection().toPixels(geoPoint, pt);
          path.lineTo(pt.x, pt.y);
          wasInside = rect.contains(locLon, locLat);
        }
      } else {
        wasInside = false;
      }
      lastLocLon = locLon;
      lastLocLat = locLat;
    }
    canvas.drawPath(path, selectedTrackPaint);

    // Draw the "End" marker:
    Location currentLastValidLocation = lastValidLocation;
    if (showEndMarker && currentLastValidLocation != null) {
      canvas.save();
      geoPoint = MyTracksUtils.getGeoPoint(currentLastValidLocation);
      mapView.getProjection().toPixels(geoPoint, pt);
      canvas.translate(pt.x - (markerWidth / 2), pt.y - markerHeight);
      endMarker.draw(canvas);
      canvas.restore();
    }

    // Draw the "Start" marker:
    for (int i = 0; i < points.size(); i++) {
      loc = points.get(i);
      if (loc.getLatitude() < 90) {
        locLon = (int) (loc.getLongitude() * 1E6);
        locLat = (int) (loc.getLatitude() * 1E6);
        geoPoint = new GeoPoint(locLat, locLon);
        mapView.getProjection().toPixels(geoPoint, pt);
        canvas.save();
        canvas.translate(pt.x - (markerWidth / 2), pt.y - markerHeight);
        startMarker.draw(canvas);
        canvas.restore();
        break;
      }
    }
  }

  @Override
  public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
      long when) {
    draw(canvas, mapView, shadow);
    return false;
  }

  /**
   * Sets the pointer location (will be drawn on next invalidate).
   */
  public void setMyLocation(Location myLocation) {
    this.myLocation = myLocation;
  }

  /**
   * Sets the pointer heading in degrees (will be drawn on next invalidate).
   *
   * @return true if the visible heading changed (i.e. a redraw of pointer is
   *         potentially necessary)
   */
  public boolean setHeading(float heading) {
    int newhdg = Math.round(-heading / 360 * 18 + 180);
    while (newhdg < 0)
      newhdg += 18;
    while (newhdg > 17)
      newhdg -= 18;
    if (newhdg != lastHeading) {
      lastHeading = newhdg;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onTap(GeoPoint p, MapView mapView) {
    if (p.equals(mapView.getMapCenter())) {
      // There is (unfortunately) no good way to determine whether the tap was
      // caused by an actual tap on the screen or the track ball. If the
      // location is equal to the map center,then it was a track ball press with
      // very high likelihood.
      return false;
    }

    final Location tapLocation = MyTracksUtils.getLocation(p);
    double dmin = Double.MAX_VALUE;
    Waypoint waypoint = null;
    for (int i = 0; i < waypoints.size(); i++) {
      final Location waypointLocation = waypoints.get(i).getLocation();
      if (waypointLocation == null) {
        continue;
      }
      final double d = waypointLocation.distanceTo(tapLocation);
      if (d < dmin) {
        dmin = d;
        waypoint = waypoints.get(i);
      }
    }

    if (waypoint != null
        && dmin < 15000000 / Math.pow(2, mapView.getZoomLevel())) {
      Intent intent = new Intent(context, MyTracksWaypointDetails.class);
      intent.putExtra("waypointid", waypoint.getId());
      context.startActivity(intent);
      return true;
    }
    return super.onTap(p, mapView);
  }
}
