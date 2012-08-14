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

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

/**
 * A mock {@link Path}. Records calls to {@link Path#lineTo(float, float)} and
 * {@link Path#moveTo(float, float)}.
 * 
 * @author Bartlomiej Niechwiej
 * @author Vangelis S.
 */
public class MockPath extends Path {

  // A list of disjointed path segments.
  private final List<List<PointF>> segments = new LinkedList<List<PointF>>();
  
  // The total number of points in this path.
  private int totalPoints;
  private List<PointF> currentSegment;

  @Override
  public void lineTo(float x, float y) {
    super.lineTo(x, y);
    Assert.assertNotNull(currentSegment);
    currentSegment.add(new PointF(x, y));
    totalPoints++;
  }

  @Override
  public void moveTo(float x, float y) {
    super.moveTo(x, y);
    currentSegment = new ArrayList<PointF>(Arrays.asList(new PointF(x, y)));
    segments.add(currentSegment);
    totalPoints++;
  }

  /**
   * Gets the total number of points in this path.
   */
  public int getTotalPoints() {
    return totalPoints;
  }
  
}