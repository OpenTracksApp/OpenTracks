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

package com.google.android.apps.mytracks;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import android.graphics.Point;

/**
 * A mock {@link Projection}. Acts as an identity matrix.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 * 
 */
public class MockProjection implements Projection {

  @Override
  public Point toPixels(GeoPoint geoPoint, Point point) {
    return point;
  }

  @Override
  public float metersToEquatorPixels(float meters) {
    return meters;
  }

  @Override
  public GeoPoint fromPixels(int x, int y) {
    return new GeoPoint(y, x);
  }
}