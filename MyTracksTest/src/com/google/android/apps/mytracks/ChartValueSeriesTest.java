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

import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.test.AndroidTestCase;

/**
 * @author Sandor Dornbush
 */
public class ChartValueSeriesTest extends AndroidTestCase {
  private ChartValueSeries series;
  private Paint fillPaint1;
 
  @Override
  protected void setUp() throws Exception {
    fillPaint1 = new Paint();
    fillPaint1.setStyle(Style.FILL);
    fillPaint1.setColor(getContext().getResources().getColor(R.color.green));
    fillPaint1.setAntiAlias(true);
    series = new ChartValueSeries(getContext(),
        "###,###",
        fillPaint1,
        null,
        100,
        R.string.elevation);
  }

  public void testInitialConditions() {
    assertEquals(25, series.getInterval());
    assertEquals(1, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(0, series.getMax());
    assertEquals(0.0, series.getSpread());
    assertEquals(fillPaint1, series.getPaint());
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
    assertEquals(25, series.getInterval());
    assertEquals(3, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(100, series.getMax());
    assertEquals(100.0, series.getSpread());
  }

  public void testBigUpdates() {
    series.update(0);
    series.update(901);
    series.updateDimension();
    assertEquals(200, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(0, series.getMin());
    assertEquals(1000, series.getMax());
    assertEquals(1000.0, series.getSpread());
  }

  public void testNotZeroBasedUpdates() {
    series.update(500);
    series.update(1401);
    series.updateDimension();
    assertEquals(200, series.getInterval());
    assertEquals(5, series.getMaxLabelLength());
    assertEquals(500, series.getMin());
    assertEquals(1500, series.getMax());
    assertEquals(1000.0, series.getSpread());
  }
}
