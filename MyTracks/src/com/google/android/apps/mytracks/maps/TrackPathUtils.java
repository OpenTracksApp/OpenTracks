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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * Various utility functions for track path painting.
 * 
 * @author Vangelis S.
 */
public class TrackPathUtils {

  private TrackPathUtils() {}

  /**
   * Add a path.
   * 
   * @param googleMap the google map
   * @param paths the existing paths
   * @param points the path points
   * @param color the path color
   * @param append true to append to the last path
   */
  public static void addPath(GoogleMap googleMap, ArrayList<Polyline> paths,
      ArrayList<LatLng> points, int color, boolean append) {
    if (points.size() == 0) {
      return;
    }
    if (append && paths.size() != 0) {
      Polyline lastPolyline = paths.get(paths.size() - 1);
      ArrayList<LatLng> pathPoints = new ArrayList<LatLng>();
      pathPoints.addAll(lastPolyline.getPoints());
      pathPoints.addAll(points);
      lastPolyline.setPoints(pathPoints);
    } else {
      PolylineOptions polylineOptions = new PolylineOptions().addAll(points).width(5).color(color);
      Polyline polyline = googleMap.addPolyline(polylineOptions);
      paths.add(polyline);
    }
    points.clear();
  }
}