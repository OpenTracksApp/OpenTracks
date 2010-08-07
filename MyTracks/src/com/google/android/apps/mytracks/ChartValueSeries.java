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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.stats.ExtremityMonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.text.DecimalFormat;

/**
 * This class encasulates meta data about one series of values for a chart.
 *
 * @author Sandor Dornbush
 */
public class ChartValueSeries {

  private final ExtremityMonitor monitor = new ExtremityMonitor();
  private final DecimalFormat format;
  private final Path path = new Path();
  private final Paint fillPaint;
  private final Paint strokePaint;
  private final int rounding;

  private String title;
  private double min = 0;
  private double max = 1;
  private int effectiveMax = 0;
  private int effectiveMin = 0;
  private double spread = 0;
  private boolean enabled = true;

  /**
   * Constructs a new chart value series.
   *
   * @param context The context for the chart
   * @param formatString The format of the decimal format for this series
   * @param fill The paint for filling the chart
   * @param stroke The paint for stroking the outside the chart, optional
   * @param rounding The factor to round the values by
   */
  public ChartValueSeries(Context context,
                          String formatString,
                          Paint fill,
                          Paint stroke,
                          int rounding,
                          int titleId) {
    this.format = new DecimalFormat(formatString);
    this.fillPaint = fill;
    this.strokePaint = stroke;
    this.rounding = rounding;
    this.title = context.getString(titleId);
  }

  /**
   * Draws the path of the chart
   */
  public void drawPath(Canvas c) {
    c.drawPath(path, fillPaint);
    if (strokePaint != null) {
      c.drawPath(path, strokePaint);
    }
  }

  /**
   * Resets this series
   */
  public void reset() {
    monitor.reset();
  }

  /**
   * Updates this series with a new value
   */
  public void update(double d) {
    monitor.update(d);
  }

  /**
   * @return The interval between markers
   */
  public int getInterval() {
    // Try to find 5 even looking intervals.
    int interval = (int) (spread / 5);
    int minInterval = rounding / 4;
    if (interval < minInterval) {
      // We won't be able to find 5 even intervals.
      return minInterval;
    } else if (interval < rounding) {
      // The desired interval is less than the rounding value.
      // We will have less than 5 intervals.
      return rounding;
    } else {
      // Round the interval.
      return (interval / rounding) * rounding;
    }
  }

  /**
   * Determines what the min and max of the chart will be.
   * This will round down and up the min and max respectively.
   */
  public void updateDimension() {
    if (monitor.getMax() == Double.NEGATIVE_INFINITY) {
      min = 0;
      max = 1;
    } else {
      min = monitor.getMin();
      max = monitor.getMax();
    }
    // Round it up.
    effectiveMax = ((int) (max / rounding)) * rounding + rounding;
    // Round it down.
    effectiveMin = ((int) (min / rounding)) * rounding;
    if (min < 0) {
      effectiveMin -= rounding;
    }
    spread = effectiveMax - effectiveMin;
  }

  /**
   * @return The length of the longest string from the series
   */
  public int getMaxLabelLength() {
    String minS = format.format(effectiveMin);
    String maxS = format.format(effectiveMax);
    return Math.max(minS.length(), maxS.length());
  }

  /**
   * @return The rounded down minimum value
   */
  public int getMin() {
    return effectiveMin;
  }

  /**
   * @return The rounded up maximum value
   */
  public int getMax() {
    return effectiveMax;
  }

  /**
   * @return The difference between the min and max values in the series
   */
  public double getSpread() {
    return spread;
  }

  /**
   * @return The format for the decimal format for this series
   */
  DecimalFormat getFormat() {
    return format;
  }

  /**
   * @return The path for this series
   */
  Path getPath() {
    return path;
  }

  /**
   * @return The paint for this series
   */
  Paint getPaint() {
    return (strokePaint == null)
        ? fillPaint
        : strokePaint;
  }

  /**
   * @return The title of the series
   */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return is this series enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets the series enabled flag.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
