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

import com.google.android.apps.mytracks.ChartValueSeries.YAxisDimension;
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
    YAxisDimension yAxisDimension = new YAxisDimension(
        5, Integer.MIN_VALUE, Integer.MAX_VALUE, new int[] { 100 });
    series = new ChartValueSeries(getContext(), R.color.elevation_fill, R.color.elevation_border,
        yAxisDimension, R.string.stats_elevation);
  }

  public void testInitialConditions() {
    assertEquals(1, series.getInterval());
    assertEquals(1, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(1, series.getMax());
    assertEquals(getContext().getString(R.string.stats_elevation), series.getTitle());
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
    assertEquals(3, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(100, series.getMax());
  }

  public void testBigUpdates() {
    series.update(0);
    series.update(901);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(1000, series.getMax());
  }

  public void testNotZeroBasedUpdates() {
    series.update(500);
    series.update(1401);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(500, series.getMin());
    assertEquals(1500, series.getMax());
  }

  public void testYAxisDimension_minAligned() {
    YAxisDimension yAxisDimension = new YAxisDimension(
        5, Integer.MIN_VALUE, Integer.MAX_VALUE, new int[] { 10, 50, 100 });
    assertEquals(10, yAxisDimension.getYAxisDimension(0, 15)[0]);
    assertEquals(10, yAxisDimension.getYAxisDimension(0, 50)[0]);
    assertEquals(50, yAxisDimension.getYAxisDimension(0, 111)[0]);
    assertEquals(50, yAxisDimension.getYAxisDimension(0, 250)[0]);
    assertEquals(100, yAxisDimension.getYAxisDimension(0, 251)[0]);
    assertEquals(100, yAxisDimension.getYAxisDimension(0, 10000)[0]);
  }

  public void testYAxisDimension_minNotAligned() {
    YAxisDimension settings = new YAxisDimension(
        5, Integer.MIN_VALUE, Integer.MAX_VALUE, new int[] { 10, 50, 100 });
    assertEquals(50, settings.getYAxisDimension(5, 55)[0]);
    assertEquals(10, settings.getYAxisDimension(10, 60)[0]);
    assertEquals(50, settings.getYAxisDimension(7, 250)[0]);
    assertEquals(100, settings.getYAxisDimension(7, 257)[0]);
    assertEquals(100, settings.getYAxisDimension(11, 10000)[0]);
  }
}
