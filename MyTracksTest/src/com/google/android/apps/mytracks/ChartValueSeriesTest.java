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

import com.google.android.apps.mytracks.ChartValueSeries.ZoomSettings;
import com.google.android.maps.mytracks.R;

import android.graphics.Paint.Style;
import android.test.AndroidTestCase;

/**
 * @author Sandor Dornbush
 */
public class ChartValueSeriesTest extends AndroidTestCase {
  private ChartValueSeries series;
 
  @Override
  protected void setUp() throws Exception {
    series = new ChartValueSeries(getContext(),
        "###,###",
        R.color.elevation_fill,
        R.color.elevation_border,
        new ZoomSettings(5, new int[] {100}),
        R.string.elevation);
  }

  public void testInitialConditions() {
    assertEquals(0, series.getInterval());
    assertEquals(1, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(0, series.getMax());
    assertEquals(0.0, series.getSpread());
    assertEquals(Style.STROKE, series.getPaint().getStyle());
    assertEquals(getContext().getString(R.string.elevation),
        series.getTitle());
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
    assertEquals(100.0, series.getSpread());
  }

  public void testBigUpdates() {
    series.update(0);
    series.update(901);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(1000, series.getMax());
    assertEquals(1000.0, series.getSpread());
  }

  public void testNotZeroBasedUpdates() {
    series.update(500);
    series.update(1401);
    series.updateDimension();
    assertEquals(100, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(500, series.getMin());
    assertEquals(1500, series.getMax());
    assertEquals(1000.0, series.getSpread());
  }

  public void testZoomSettings_invalidArgs() {
    try {
      new ZoomSettings(0, new int[] {10, 50, 100});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK.
    }
    try {
      new ZoomSettings(1, null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK.
    }
    try {
      new ZoomSettings(1, new int[] {});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK.
    }
    try {
      new ZoomSettings(1, new int[] {1, 3, 2});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK.
    }
  }
  
  public void testZoomSettings_minAligned() {
    ZoomSettings settings = new ZoomSettings(5, new int[] {10, 50, 100});
    assertEquals(10, settings.calculateInterval(0, 15));
    assertEquals(10, settings.calculateInterval(0, 50));
    assertEquals(50, settings.calculateInterval(0, 111));
    assertEquals(50, settings.calculateInterval(0, 250));
    assertEquals(100, settings.calculateInterval(0, 251));
    assertEquals(100, settings.calculateInterval(0, 10000));
  }

  public void testZoomSettings_minNotAligned() {
    ZoomSettings settings = new ZoomSettings(5, new int[] {10, 50, 100});
    assertEquals(50, settings.calculateInterval(5, 55));
    assertEquals(10, settings.calculateInterval(10, 60));
    assertEquals(50, settings.calculateInterval(7, 250));
    assertEquals(100, settings.calculateInterval(7, 257));
    assertEquals(100, settings.calculateInterval(11, 10000));
    
    // A regression test.
    settings = new ZoomSettings(5, new int[] {5, 10, 20});
    assertEquals(10, settings.calculateInterval(-37.14, -11.89));
  }
}
