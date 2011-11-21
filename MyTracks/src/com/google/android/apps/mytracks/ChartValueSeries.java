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
import android.graphics.Paint.Style;
import android.graphics.Path;

import java.text.DecimalFormat;

/**
 * This class encapsulates meta data about one series of values for a chart.
 *
 * @author Sandor Dornbush
 */
public class ChartValueSeries {

  private final ExtremityMonitor monitor = new ExtremityMonitor();
  private final DecimalFormat format;
  private final Path path = new Path();
  private final Paint fillPaint;
  private final Paint strokePaint;
  private final Paint labelPaint;
  private final ZoomSettings zoomSettings;

  private String title;
  private double min;
  private double max = 1.0;
  private int effectiveMax;
  private int effectiveMin;
  private double spread;
  private int interval;
  private boolean enabled = true;

  /**
   * This class controls how effective min/max values of a {@link ChartValueSeries} are calculated.
   */
  public static class ZoomSettings {
    private int intervals;
    private final int absoluteMin;
    private final int absoluteMax;
    private final int[] zoomLevels;

    public ZoomSettings(int intervals, int[] zoomLevels) {
      this.intervals = intervals;
      this.absoluteMin = Integer.MAX_VALUE;
      this.absoluteMax = Integer.MIN_VALUE;
      this.zoomLevels = zoomLevels;
      checkArgs();
    }

    public ZoomSettings(int intervals, int absoluteMin, int absoluteMax, int[] zoomLevels) {
      this.intervals = intervals;
      this.absoluteMin = absoluteMin;
      this.absoluteMax = absoluteMax;
      this.zoomLevels = zoomLevels;
      checkArgs();
    }

    private void checkArgs() {
      if (intervals <= 0 || zoomLevels == null || zoomLevels.length == 0) {
        throw new IllegalArgumentException("Expecing positive intervals and non-empty zoom levels");
      }
      for (int i = 1; i < zoomLevels.length; ++i) {
        if (zoomLevels[i] <= zoomLevels[i - 1]) {
          throw new IllegalArgumentException("Expecting zoom levels in ascending order");
        }
      }
    }

    public int getIntervals() {
      return intervals;
    }

    public int getAbsoluteMin() {
      return absoluteMin;
    }

    public int getAbsoluteMax() {
      return absoluteMax;
    }

    public int[] getZoomLevels() {
      return zoomLevels;
    }

    /**
     * Calculates the interval between markings given the min and max values.
     * This function attempts to find the smallest zoom level that fits [min,max] after rounding
     * it to the current zoom level.
     *
     * @param min the minimum value in the series
     * @param max the maximum value in the series
     * @return the calculated interval for the given range
     */
    public int calculateInterval(double min, double max) {
      min = Math.min(min, absoluteMin);
      max = Math.max(max, absoluteMax);
      for (int i = 0; i < zoomLevels.length; ++i) {
        int zoomLevel = zoomLevels[i];
        int roundedMin = (int)(min / zoomLevel) * zoomLevel;
        if (roundedMin > min) {
          roundedMin -= zoomLevel;
        }
        double interval = (max - roundedMin) / intervals;
        if (zoomLevel >= interval) {
          return zoomLevel;
        }
      }
      return zoomLevels[zoomLevels.length - 1];
    }
  }

  /**
   * Constructs a new chart value series.
   *
   * @param context The context for the chart
   * @param formatString The format of the decimal format for this series
   * @param fillColor The paint for filling the chart
   * @param strokeColor The paint for stroking the outside the chart, optional
   * @param zoomSettings The settings related to zooming
   * @param titleId The title ID
   *
   * TODO: Get rid of Context and inject appropriate values instead.
   */
  public ChartValueSeries(Context context, String formatString, int fillColor, int strokeColor,
      ZoomSettings zoomSettings, int titleId) {
    this.format = new DecimalFormat(formatString);
    fillPaint = new Paint();
    fillPaint.setStyle(Style.FILL);
    fillPaint.setColor(context.getResources().getColor(fillColor));
    fillPaint.setAntiAlias(true);
    if (strokeColor != -1) {
      strokePaint = new Paint();
      strokePaint.setStyle(Style.STROKE);
      strokePaint.setColor(context.getResources().getColor(strokeColor));
      strokePaint.setAntiAlias(true);
      // Make a copy of the stroke paint with the default thickness.
      labelPaint = new Paint(strokePaint);
      strokePaint.setStrokeWidth(2f);
    } else {
      strokePaint = null;
      labelPaint = fillPaint;
    }
    this.zoomSettings = zoomSettings;
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
    return interval;
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
    min = Math.min(min, zoomSettings.getAbsoluteMin());
    max = Math.max(max, zoomSettings.getAbsoluteMax());

    this.interval = zoomSettings.calculateInterval(min, max);
    // Round it up.
    effectiveMax = ((int) (max / interval)) * interval + interval;
    // Round it down.
    effectiveMin = ((int) (min / interval)) * interval;
    if (min < 0) {
      effectiveMin -= interval;
    }
    spread = effectiveMax - effectiveMin;
  }

  /**
   * @return The length of the longest string from the series
   */
  public int getMaxLabelLength() {
    String minS = format.format(effectiveMin);
    String maxS = format.format(getMax());
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
    return strokePaint == null ? fillPaint : strokePaint;
  }

  public Paint getLabelPaint() {
    return labelPaint;
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

  public boolean hasData() {
    return monitor.hasData();
  }
}
