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
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;

import java.text.NumberFormat;

/**
 * This class encapsulates the meta data for one series of the chart values.
 * 
 * @author Sandor Dornbush
 */
public class ChartValueSeries {

  private static final float STROKE_WIDTH = 2f;

  private final int absoluteMin;
  private final int absoluteMax;
  private final int[] intervalValues;
  private final int metricTitleId;
  private final int imperialTitleId;
  private final Paint fillPaint;
  private final Paint strokePaint;
  private final Paint titlePaint;
  private final Paint markerPaint;
  private final ExtremityMonitor extremityMonitor;
  private final NumberFormat numberFormat;
  private final Path path;
  
  private int interval = 1;
  private int minMarkerValue = 0;
  private int maxMarkerValue = interval * ChartView.Y_AXIS_INTERVALS;
  private boolean enabled = true;

  /**
   * Constructor.
   * 
   * @param context the context
   * @param absoluteMin the absolute min value
   * @param absoluteMax the absolute max value
   * @param intervalValues the list of interval values
   * @param metricTitleId the metric title id
   * @param imperialTitleId the imperial title id
   * @param fillColor the fill color
   * @param strokeColor the stroke color
   */
  public ChartValueSeries(Context context, int absoluteMin, int absoluteMax, int[] intervalValues,
      int metricTitleId, int imperialTitleId, int fillColor, int strokeColor) {
    this.absoluteMin = absoluteMin;
    this.absoluteMax = absoluteMax;
    this.intervalValues = intervalValues;
    this.metricTitleId = metricTitleId;
    this.imperialTitleId = imperialTitleId;

    fillPaint = new Paint();
    fillPaint.setStyle(Style.FILL);
    fillPaint.setColor(context.getResources().getColor(fillColor));
    fillPaint.setAntiAlias(true);

    strokePaint = new Paint();
    strokePaint.setStyle(Style.STROKE);
    strokePaint.setColor(context.getResources().getColor(strokeColor));
    strokePaint.setAntiAlias(true);

    float scale = context.getResources().getDisplayMetrics().density;
    
    // Make copies of the stroke paint with the default thickness
    titlePaint = new Paint(strokePaint);
    titlePaint.setTextSize(ChartView.MEDIUM_TEXT_SIZE * scale);
    titlePaint.setTextAlign(Align.CENTER);

    markerPaint = new Paint(strokePaint);
    markerPaint.setTextSize(ChartView.SMALL_TEXT_SIZE * scale);
    markerPaint.setTextAlign(Align.RIGHT);

    // Set stroke paint thickness
    strokePaint.setStrokeWidth(STROKE_WIDTH);

    extremityMonitor = new ExtremityMonitor();
    numberFormat = NumberFormat.getIntegerInstance();
    path = new Path();
  }

  /**
   * Sets the series enabled value.
   * 
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns true if the series is enabled.
   */
  public boolean isEnabled() {
    return enabled;
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
   * Gets the path.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Draws the path on canvas.
   * 
   * @param canvas the canvas
   */
  public void drawPath(Canvas canvas) {
    canvas.drawPath(path, fillPaint);
    canvas.drawPath(path, strokePaint);
  }

  /**
   * Updates the y axis dimension.
   */
  public void updateDimension() {
    double min = hasData() ? extremityMonitor.getMin() : 0.0;
    double max = hasData() ? extremityMonitor.getMax() : 1.0;
    min = Math.max(min, absoluteMin);
    max = Math.min(max, absoluteMax);
    interval = getInterval(min, max);
    minMarkerValue = getMinMarkerValue(min, interval);
    maxMarkerValue = minMarkerValue + interval * ChartView.Y_AXIS_INTERVALS;
  }

  /**
   * Gets the interval value.
   * 
   * @param min the min value
   * @param max the max value
   */
  private int getInterval(double min, double max) {
    for (int i = 0; i < intervalValues.length; i++) {
      int intervalValue = intervalValues[i];
      int minValue = getMinMarkerValue(min, intervalValue);
      double targetInterval = (max - minValue) / ChartView.Y_AXIS_INTERVALS;
      if (intervalValue >= targetInterval) {
        return intervalValue;
      }
    }
    // Return the largest interval
    return intervalValues[intervalValues.length - 1];
  }

  /**
   * Gets the min marker value.
   * 
   * @param min the min series value
   * @param intervalValue the interval value
   */
  private int getMinMarkerValue(double min, int intervalValue) {
    // Round down to the nearest intervalValue
    int value = ((int) (min / intervalValue)) * intervalValue;
    // value > min if min is negative
    if (value > min) {
      return value - intervalValue;
    }
    return value;
  }

  /**
   * Gets the interval value.
   */
  public int getInterval() {
    return interval;
  }

  /**
   * Gets the min marker value.
   */
  public int getMinMarkerValue() {
    return minMarkerValue;
  }

  /**
   * Gets the max marker value.
   */
  @VisibleForTesting
  int getMaxMarkerValue() {
    return maxMarkerValue;
  }

  /**
   * Gets the title id.
   */
  public int getTitleId(boolean metricUnits) {
    return metricUnits ? metricTitleId : imperialTitleId;
  }

  /**
   * Gets the title paint.
   */
  public Paint getTitlePaint() {
    return titlePaint;
  }

  /**
   * Gets the marker paint.
   */
  public Paint getMarkerPaint() {
    return markerPaint;
  }

  /**
   * Gets the largest marker.
   */
  public String getLargestMarker() {
    String minMarker = numberFormat.format(getMinMarkerValue());
    String maxMarker = numberFormat.format(getMaxMarkerValue());
    return minMarker.length() >= maxMarker.length() ? minMarker : maxMarker;
  }

  /**
   * Formats a marker value.
   * 
   * @param value the value
   */
  public String formatMarker(int value) {
    return numberFormat.format(value);
  }
}
