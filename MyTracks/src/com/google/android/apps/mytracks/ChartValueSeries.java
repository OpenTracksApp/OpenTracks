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
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

import java.text.NumberFormat;

/**
 * This class encapsulates the meta data for one series of chart values.
 *
 * @author Sandor Dornbush
 */
public class ChartValueSeries {

  private static final float STROKE_WIDTH = 2f;

  private final ExtremityMonitor extremityMonitor;
  private final NumberFormat numberFormat;
  private final Path path;
  private final Paint fillPaint;
  private final Paint strokePaint;
  private final Paint labelPaint;
  private final YAxisDimension markingInterval;
  private String title;

  private boolean enabled = true;
  private int interval = 1;
  private int effectiveMin = 0;
  private int effectiveMax = 1;

  /**
   * This class to calculates the y axis dimension, interval, effective min, and
   * effective max.
   */
  public static class YAxisDimension {
    private final int numberOfIntervals;
    private final int absoluteMin;
    private final int absoluteMax;
    private final int[] intervalValues;

    /**
     * Constructor.
     *
     * @param numberOfIntervals the number of intervals
     * @param absoluteMin the absolute minimum value
     * @param absoluteMax the absolute maximum value
     * @param intervalValues the list of interval values
     */
    public YAxisDimension(
        int numberOfIntervals, int absoluteMin, int absoluteMax, int[] intervalValues) {
      this.numberOfIntervals = numberOfIntervals;
      this.absoluteMin = absoluteMin;
      this.absoluteMax = absoluteMax;
      this.intervalValues = intervalValues;
    }

    /**
     * Gets the y axis dimension. Returns an array of int[3], containing interval, effectiveMin, 
     * and effectiveMax. 
     * 
     * @param min the min value
     * @param max the max value
     */
    public int[] getYAxisDimension(double min, double max) {
      min = Math.max(min, absoluteMin);
      max = Math.min(max, absoluteMax);
      int interval = 0;
      int effectiveMin = 0;
      for (int i = 0; i < intervalValues.length; i++) {
        interval = intervalValues[i];
        effectiveMin = getEffetiveMin(min, interval);
        double targetInterval = (max - effectiveMin) / numberOfIntervals;
        if (interval >= targetInterval) {
          break;
        }
      }
      int effectiveMax = getEffectiveMax(max, interval);
      return new int[] { interval, effectiveMin, effectiveMax };
    }

    /**
     * Gets the effective min value.
     * 
     * @param min the min value
     * @param interval the interval
     */
    private int getEffetiveMin(double min, int interval) {
      int value = (int) (min / interval) * interval;
      // value > min if min is negative
      if (value > min) {
        return value - interval;
      }
      return value;
    }

    /**
     * Gets the effective max value
     * 
     * @param max the max value
     * @param interval the interval
     */
    private int getEffectiveMax(double max, int interval) {
      return ((int) (max / interval)) * interval + interval;
    }
  }

  /**
   * Constructs a new chart value series.
   *
   * @param context the context
   * @param fillColor the fill color
   * @param strokeColor the stroke color
   * @param yAxisDimension the marking interval
   * @param titleId the title id
   */
  public ChartValueSeries(Context context, int fillColor, int strokeColor,
      YAxisDimension yAxisDimension, int titleId) {
    extremityMonitor = new ExtremityMonitor();
    numberFormat = NumberFormat.getIntegerInstance();
    path = new Path();
    fillPaint = new Paint();
    fillPaint.setStyle(Style.FILL);
    fillPaint.setColor(context.getResources().getColor(fillColor));
    fillPaint.setAntiAlias(true);
    if (strokeColor != -1) {
      strokePaint = new Paint();
      strokePaint.setStyle(Style.STROKE);
      strokePaint.setColor(context.getResources().getColor(strokeColor));
      strokePaint.setAntiAlias(true);
      // Make a copy of the stroke paint with the default thickness
      labelPaint = new Paint(strokePaint);
      strokePaint.setStrokeWidth(STROKE_WIDTH);
    } else {
      strokePaint = null;
      labelPaint = fillPaint;
    }
    this.markingInterval = yAxisDimension;
    title = context.getString(titleId);
  }

  /**
   * Returns true if the series has data.
   */
  public boolean hasData() {
    return extremityMonitor.hasData();
  }
  
  /**
   * Updates the series with a new value.
   *
   * @param value the new value
   */
  public void update(double value) {
    extremityMonitor.update(value);
  }
  
  /**
   * Gets the number format for the series.
   */
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }
  
  /**
   * Gets the path.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Gets the label paint.
   */
  public Paint getLabelPaint() {
    return labelPaint;
  }
  
  /**
   * Gets the title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title.
   *
   * @param title the title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns true if the series is enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets the enabled value.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  /**
   * Draws the path on canvas.
   *
   * @param canvas the canvas
   */
  public void drawPath(Canvas canvas) {
    canvas.drawPath(path, fillPaint);
    if (strokePaint != null) {
      canvas.drawPath(path, strokePaint);
    }
  }

  /**
   * Updates the y axis dimension.
   */
  public void updateDimension() {
    boolean hasData = extremityMonitor.hasData();
    double min = hasData ? extremityMonitor.getMin() : 0.0;
    double max = hasData ? extremityMonitor.getMax() : 1.0;
    int[] dimension = markingInterval.getYAxisDimension(min, max);
    interval = dimension[0];
    effectiveMin = dimension[1];
    effectiveMax = dimension[2];
  }

  /**
   * Gets the y axis interval value.
   */
  public int getInterval() {
    return interval;
  }

  /**
   * Gets the minimum value.
   */
  public int getMin() {
    return effectiveMin;
  }

  /**
   * Gets the maximum value.
   */
  @VisibleForTesting
  int getMax() {
    return effectiveMax;
  }

  /**
   * Gets the maximum label length.
   */
  public int getMaxLabelLength() {
    return Math.max(numberFormat.format(getMin()).length(), numberFormat.format(getMax()).length());
  }
}
