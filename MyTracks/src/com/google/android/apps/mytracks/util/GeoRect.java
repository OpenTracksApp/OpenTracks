/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mytracks.util;

import com.google.android.maps.GeoPoint;

/**
 * A rectangle in geographical space.
 */
public class GeoRect {
  public int top;
  public int left;
  public int bottom;
  public int right;

  public GeoRect() {
    top = 0;
    left = 0;
    bottom = 0;
    right = 0;
  }

  public GeoRect(GeoPoint center, int latSpan, int longSpan) {
    top = center.getLatitudeE6() - latSpan / 2;
    left = center.getLongitudeE6() - longSpan / 2;
    bottom = center.getLatitudeE6() + latSpan / 2;
    right = center.getLongitudeE6() + longSpan / 2;
  }

  public GeoPoint getCenter() {
    return new GeoPoint(top / 2 + bottom / 2, left / 2 + right / 2);
  }

  public int getLatSpan() {
    return bottom - top;
  }

  public int getLongSpan() {
    return right - left;
  }

  public boolean contains(GeoPoint geoPoint) {
    if (geoPoint.getLatitudeE6() >= top
        && geoPoint.getLatitudeE6() <= bottom
        && geoPoint.getLongitudeE6() >= left
        && geoPoint.getLongitudeE6() <= right) {
      return true;
    }
    return false;
  }
}