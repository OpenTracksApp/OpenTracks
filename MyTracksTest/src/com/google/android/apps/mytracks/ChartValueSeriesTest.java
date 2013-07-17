/*
 * Copyright 2010 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.test.AndroidTestCase;

/**
 * Tests {@link ChartValueSeries}.
 * 
 * @author Sandor Dornbush
 */
public class ChartValueSeriesTest extends AndroidTestCase {
  private ChartValueSeries series;

  @Override
  protected void setUp() throws Exception {
    series = new ChartValueSeries(getContext(),
        Integer.MIN_VALUE,
        Integer.MAX_VALUE,
        new int[] {100, 1000 },
        R.string.description_elevation_metric,
        R.string.description_elevation_imperial,
        R.color.chart_elevation_fill,
        R.color.chart_elevation_border);
  }

  public void testInitialConditions() {
    assertEquals(1, series.getInterval());
    assertEquals(0, series.getMinMarkerValue());
    assertEquals(5, series.getMaxMarkerValue());
    assertTrue(series.isEnabled());
  }

  public void testEnabled() {
    series.setEnabled(false);
    assertFalse(series.isEnabled());
  }

  public void testSmallUpdates() {
    series.update(0);
    series.update(10);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(0, series.getMinMarkerValue());
    assertEquals(500, series.getMaxMarkerValue());
  }

  public void testBigUpdates() {
    series.update(0);
    series.update(901);
    series.updateDimension();
    assertEquals(1000, series.getInterval());
    assertEquals(0, series.getMinMarkerValue());
    assertEquals(5000, series.getMaxMarkerValue());
  }

  public void testNotZeroBasedUpdates() {
    series.update(220);
    series.update(250);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(200, series.getMinMarkerValue());
    assertEquals(700, series.getMaxMarkerValue());
  }
}
