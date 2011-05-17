// Copyright 2009 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import com.google.android.maps.GeoPoint;

/**
 * A rectangle in geographical space.
 */
class GeoRect {
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
