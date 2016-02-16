/*
 * Copyright 2012 Google Inc.
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
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.maps.mytracks.R;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * A single color track path.
 * 
 * @author Jimmy Shih
 */
public class SingleColorTrackPath implements TrackPath {

  final int color;
  
  public SingleColorTrackPath(Context context) {
    color = context.getResources().getColor(R.color.track_color_fast);
  }

  @Override
  public boolean updateState(TripStatistics tripStatistics) {
    return false;
  }

  @Override
  public void updatePath(GoogleMap googleMap, ArrayList<Polyline> paths, int startIndex,
      List<CachedLocation> locations) {
    if (googleMap == null) {
      return;
    }
    if (startIndex >= locations.size()) {
      return;
    }

    boolean newSegment = startIndex == 0 || !locations.get(startIndex - 1).isValid();
    ArrayList<LatLng> lastSegmentPoints = new ArrayList<LatLng>();
    boolean useLastPolyline = true;
    for (int i = startIndex; i < locations.size(); i++) {
      CachedLocation cachedLocation = locations.get(i);

      // If not valid, start a new segment
      if (!cachedLocation.isValid()) {
        newSegment = true;
        continue;
      }
      LatLng latLng = cachedLocation.getLatLng();
      if (newSegment) {
        TrackPathUtils.addPath(googleMap, paths, lastSegmentPoints, color, useLastPolyline);
        useLastPolyline = false;
        newSegment = false;
      }
      lastSegmentPoints.add(latLng);
    }
    TrackPathUtils.addPath(googleMap, paths, lastSegmentPoints, color, useLastPolyline);
  }
}