/*
 * Copyright 2008 Google Inc.
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
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.ExtremityMonitor;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Visualization of the chart.
 *
 * @author Sandor Dornbush
 * @author Leif Hendrik Wilden
 */
public class ChartView extends View {
  private static final int MIN_ZOOM_LEVEL = 1;

  /*
   * Scrolling logic:
   */
  private final Scroller scroller;
  private VelocityTracker velocityTracker = null;
  /** Position of the last motion event */
  private float lastMotionX;

  /*
   * Zoom logic:
   */
  private int zoomLevel = 1;
  private int maxZoomLevel = 10;

  private static final int MAX_INTERVALS = 5;

  /*
   * Borders, margins, dimensions (in pixels):
   */
  private int leftBorder = -1;

  /**
   * Unscaled top border of the chart.
   */
  private static final int TOP_BORDER = 15;

  /**
   * Device scaled top border of the chart.
   */
  private int topBorder;

  /**
   * Unscaled bottom border of the chart.
   */
  private static final float BOTTOM_BORDER = 40;

  /**
   * Device scaled bottom border of the chart.
   */
  private int bottomBorder;

  private static final int RIGHT_BORDER = 17;

  /** Space to leave for drawing the unit labels */
  private static final int UNIT_BORDER = 15;
  private static final int FONT_HEIGHT = 10;

  private int w = 0;
  private int h = 0;
  private int effectiveWidth = 0;
  private int effectiveHeight = 0;

  /*
   * Ranges (in data units):
   */
  private double maxX = 1;

  /**
   * The various series.
   */
  public static final int ELEVATION_SERIES = 0;
  public static final int SPEED_SERIES = 1;
  public static final int POWER_SERIES = 2;
  public static final int CADENCE_SERIES = 3;
  public static final int HEART_RATE_SERIES = 4;
  public static final int NUM_SERIES = 5;
  private ChartValueSeries[] series;

  private final ExtremityMonitor xMonitor = new ExtremityMonitor();
  private static final NumberFormat X_FORMAT = new DecimalFormat("###,###");
  private static final NumberFormat X_SHORT_FORMAT = new DecimalFormat("#.0");

  /*
   * Paints etc. used when drawing the chart:
   */
  private final Paint borderPaint = new Paint();
  private final Paint labelPaint = new Paint();
  private final Paint gridPaint = new Paint();
  private final Paint gridBarPaint = new Paint();
  private final Paint clearPaint = new Paint();
  private final Drawable pointer;
  private final Drawable statsMarker;
  private final Drawable waypointMarker;
  private final int markerWidth, markerHeight;

  /**
   * The chart data stored as an array of double arrays. Each one dimensional
   * array is composed of [x, y].
   */
  private final ArrayList<double[]> data = new ArrayList<double[]>();

  /**
   * List of way points to be displayed.
   */
  private final ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();

  private boolean metricUnits = true;
  private boolean showPointer = false;

  /** Display chart versus distance or time */
  public enum Mode {
    BY_DISTANCE, BY_TIME
  }

  private Mode mode = Mode.BY_DISTANCE;

  public ChartView(Context context) {
    super(context);

    setUpChartValueSeries(context);

    labelPaint.setStyle(Style.STROKE);
    labelPaint.setColor(context.getResources().getColor(R.color.black));
    labelPaint.setAntiAlias(true);

    borderPaint.setStyle(Style.STROKE);
    borderPaint.setColor(context.getResources().getColor(R.color.black));
    borderPaint.setAntiAlias(true);

    gridPaint.setStyle(Style.STROKE);
    gridPaint.setColor(context.getResources().getColor(R.color.gray));
    gridPaint.setAntiAlias(false);

    gridBarPaint.set(gridPaint);
    gridBarPaint.setPathEffect(new DashPathEffect(new float[] {3, 2}, 0));

    clearPaint.setStyle(Style.FILL);
    clearPaint.setColor(context.getResources().getColor(R.color.white));
    clearPaint.setAntiAlias(false);

    pointer = context.getResources().getDrawable(R.drawable.arrow_180);
    pointer.setBounds(0, 0,
        pointer.getIntrinsicWidth(), pointer.getIntrinsicHeight());

    statsMarker = getResources().getDrawable(R.drawable.ylw_pushpin);
    markerWidth = statsMarker.getIntrinsicWidth();
    markerHeight = statsMarker.getIntrinsicHeight();
    statsMarker.setBounds(0, 0, markerWidth, markerHeight);

    waypointMarker = getResources().getDrawable(R.drawable.blue_pushpin);
    waypointMarker.setBounds(0, 0, markerWidth, markerHeight);

    scroller = new Scroller(context);
    setFocusable(true);
    setClickable(true);
    updateDimensions();
  }

  private void setUpChartValueSeries(Context context) {
    series = new ChartValueSeries[NUM_SERIES];

    // Create the value series.
    series[ELEVATION_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.elevation_fill,
                             R.color.elevation_border,
                             new ZoomSettings(MAX_INTERVALS,
                                 new int[] {5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000}),
                             R.string.elevation);

    series[SPEED_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.speed_fill,
                             R.color.speed_border,
                             new ZoomSettings(MAX_INTERVALS, 0, Integer.MIN_VALUE,
                                 new int[] {1, 5, 10, 20, 50}),
                             R.string.speed);
    series[POWER_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.power_fill,
                             R.color.power_border,
                             new ZoomSettings(MAX_INTERVALS, 0, 1000, new int[] {5, 50, 100, 200}),
                             R.string.power);
    series[CADENCE_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.cadence_fill,
                             R.color.cadence_border,
                             new ZoomSettings(MAX_INTERVALS, 0, Integer.MIN_VALUE,
                                 new int[] {5, 10, 25, 50}),
                             R.string.cadence);
    series[HEART_RATE_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.heartrate_fill,
                             R.color.heartrate_border,
                             new ZoomSettings(MAX_INTERVALS, 0, Integer.MIN_VALUE,
                                 new int[] {25, 50}),
                             R.string.heart_rate);
  }

  public void clearWaypoints() {
    waypoints.clear();
  }

  public void addWaypoint(Waypoint waypoint) {
    waypoints.add(waypoint);
  }

  /**
   * Determines whether the pointer icon is shown on the last data point.
   */
  public void setShowPointer(boolean showPointer) {
    this.showPointer = showPointer;
  }

  /**
   * Sets whether metric units are used or not.
   */
  public void setMetricUnits(boolean metricUnits) {
    this.metricUnits = metricUnits;
  }

  public void setReportSpeed(boolean reportSpeed, Context c) {
    series[SPEED_SERIES].setTitle(c.getString(reportSpeed
                                              ? R.string.speed
                                              : R.string.pace_label));
  }

  private void addDataPointInternal(double[] theData) {
    xMonitor.update(theData[0]);
    int min = Math.min(series.length, theData.length - 1);
    for (int i = 1; i <= min; i++) {
      if (!Double.isNaN(theData[i])) {
        series[i - 1].update(theData[i]);
      }
    }
    // Fill in the extra's if needed.
    for (int i = theData.length; i < series.length; i++) {
      if (series[i].hasData()) {
        series[i].update(0);
      }
    }
  }

  /**
   * Adds multiple data points to the chart.
   *
   * @param theData an array list of data points to be added
   */
  public void addDataPoints(ArrayList<double[]> theData) {
    synchronized (data) {
      data.addAll(theData);
      for (int i = 0; i < theData.size(); i++) {
        double d[] = theData.get(i);
        addDataPointInternal(d);
      }
      updateDimensions();
      setUpPath();
    }
  }

  /**
   * Clears all data.
   */
  public void reset() {
    synchronized (data) {
      data.clear();
      xMonitor.reset();
      zoomLevel = 1;
      updateDimensions();
    }
  }

  public void resetScroll() {
    scrollTo(0, 0);
  }

  /**
   * @return true if the chart can be zoomed into.
   */
  public boolean canZoomIn() {
    return zoomLevel < maxZoomLevel;
  }

  /**
   * @return true if the chart can be zoomed out
   */
  public boolean canZoomOut() {
    return zoomLevel > MIN_ZOOM_LEVEL;
  }

  /**
   * Zooms in one level (factor 2).
   */
  public void zoomIn() {
    if (canZoomIn()) {
      zoomLevel++;
      setUpPath();
      invalidate();
    }
  }

  /**
   * Zooms out one level (factor 2).
   */
  public void zoomOut() {
    if (canZoomOut()) {
      zoomLevel--;
      scroller.abortAnimation();
      int scrollX = getScrollX();
      if (scrollX > effectiveWidth * (zoomLevel - 1)) {
        scrollX = effectiveWidth * (zoomLevel - 1);
        scrollTo(scrollX, 0);
      }
      setUpPath();
      invalidate();
    }
  }

  /**
   * Initiates flinging.
   *
   * @param velocityX start velocity (pixels per second)
   */
  public void fling(int velocityX) {
    scroller.fling(getScrollX(), 0, velocityX, 0, 0,
        effectiveWidth * (zoomLevel - 1), 0, 0);
    invalidate();
  }

  /**
   * Scrolls the view horizontally by the given amount.
   *
   * @param deltaX number of pixels to scroll
   */
  public void scrollBy(int deltaX) {
    int scrollX = getScrollX() + deltaX;
    if (scrollX < 0) {
      scrollX = 0;
    }
    int available = effectiveWidth * (zoomLevel - 1);
    if (scrollX > available) {
      scrollX = available;
    }
    scrollTo(scrollX, 0);
  }

  /**
   * @return the current display mode (by distance, by time)
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Sets the display mode (by distance, by time).
   * It is expected that after the mode change, data will be reloaded.
   */
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  private int getWaypointX(Waypoint waypoint) {
    return (mode == Mode.BY_DISTANCE)
        ? getX(metricUnits
               ? waypoint.getLength() / 1000.0
               : waypoint.getLength() * UnitConversions.KM_TO_MI / 1000.0)
        : getX(waypoint.getDuration());
  }

  /**
   * Called by the parent to indicate that the mScrollX/Y values need to be
   * updated. Triggers a redraw during flinging.
   */
  @Override
  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      int oldX = getScrollX();
      int x = scroller.getCurrX();
      scrollTo(x, 0);
      if (oldX != x) {
        onScrollChanged(x, 0, oldX, 0);
        postInvalidate();
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain();
    }
    velocityTracker.addMovement(event);
    final int action = event.getAction();
    final float x = event.getX();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        /*
         * If being flinged and user touches, stop the fling. isFinished will be
         * false if being flinged.
         */
        if (!scroller.isFinished()) {
          scroller.abortAnimation();
        }
        // Remember where the motion event started
        lastMotionX = x;
        break;
      case MotionEvent.ACTION_MOVE:
        // Scroll to follow the motion event
        final int deltaX = (int) (lastMotionX - x);
        lastMotionX = x;
        if (deltaX < 0) {
          if (getScrollX() > 0) {
            scrollBy(deltaX);
          }
        } else if (deltaX > 0) {
          final int availableToScroll =
              effectiveWidth * (zoomLevel - 1) - getScrollX();
          if (availableToScroll > 0) {
            scrollBy(Math.min(availableToScroll, deltaX));
          }
        }
        break;
      case MotionEvent.ACTION_UP:
        // Check if top area with waypoint markers was touched and find the
        // touched marker if any:
        if (event.getY() < 100) {
          int dmin = Integer.MAX_VALUE;
          Waypoint nearestWaypoint = null;
          for (int i = 0; i < waypoints.size(); i++) {
            final Waypoint waypoint = waypoints.get(i);
            final int d = Math.abs(getWaypointX(waypoint) - (int) event.getX()
                - getScrollX());
            if (d < dmin) {
              dmin = d;
              nearestWaypoint = waypoint;
            }
          }
          if (nearestWaypoint != null && dmin < 100) {
            Intent intent =
                new Intent(getContext(), WaypointDetails.class);
            intent.putExtra(WaypointDetails.WAYPOINT_ID_EXTRA, nearestWaypoint.getId());
            getContext().startActivity(intent);
            return true;
          }
        }

        VelocityTracker myVelocityTracker = velocityTracker;
        myVelocityTracker.computeCurrentVelocity(1000);
        int initialVelocity = (int) myVelocityTracker.getXVelocity();
        if (Math.abs(initialVelocity) >
            ViewConfiguration.getMinimumFlingVelocity()) {
          fling(-initialVelocity);
        }
        if (velocityTracker != null) {
          velocityTracker.recycle();
          velocityTracker = null;
        }
        break;
    }
    return true;
  }

  @Override
  protected void onDraw(Canvas c) {
    synchronized (data) {
      updateEffectiveDimensionsIfChanged(c);

      // Keep original state.
      c.save();

      c.drawColor(Color.WHITE);

      if (data.isEmpty()) {
        // No data, draw only axes
        drawXAxis(c);
        drawYAxis(c);
        c.restore();
        return;
      }

      // Clip to graph drawing space
      c.save();
      clipToGraphSpace(c);

      // Draw the grid and the data on it.
      drawGrid(c);
      drawDataSeries(c);
      drawWaypoints(c);

      // Go back to full canvas drawing.
      c.restore();

      // Draw the axes and their labels.
      drawAxesAndLabels(c);

      // Go back to original state.
      c.restore();

      // Draw the pointer
      if (showPointer) {
        drawPointer(c);
      }
    }
  }

  /** Clips the given canvas to the area where the graph lines should be drawn. */
  private void clipToGraphSpace(Canvas c) {
    c.clipRect(leftBorder + 1 + getScrollX(), topBorder + 1,
        w - RIGHT_BORDER + getScrollX() - 1, h - bottomBorder - 1);
  }

  /** Draws the axes and their labels into th e given canvas. */
  private void drawAxesAndLabels(Canvas c) {
    drawXLabels(c);
    drawXAxis(c);
    drawSeriesTitles(c);

    c.translate(getScrollX(), 0);
    drawYAxis(c);
    float density = getContext().getResources().getDisplayMetrics().density;
    final int spacer = (int) (5 * density);
    int x = leftBorder - spacer;
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        x -= drawYLabels(cvs, c, x) + spacer;
      }
    }
  }

  /** Draws the current pointer into the given canvas. */
  private void drawPointer(Canvas c) {
    c.translate(getX(maxX) - pointer.getIntrinsicWidth() / 2,
                getY(series[0], data.get(data.size() - 1)[1])
                - pointer.getIntrinsicHeight() / 2 - 12);
    pointer.draw(c);
  }

  /** Draws the waypoints into the given canvas. */
  private void drawWaypoints(Canvas c) {
    for (int i = 1; i < waypoints.size(); i++) {
      final Waypoint waypoint = waypoints.get(i);
      if (waypoint.getLocation() == null) {
        continue;
      }
      c.save();

      final float x = getWaypointX(waypoint);
      c.drawLine(x, h - bottomBorder, x, topBorder, gridPaint);
      c.translate(x - (float) markerWidth / 2.0f, (float) markerHeight);
      if (waypoints.get(i).getType() == Waypoint.TYPE_STATISTICS) {
        statsMarker.draw(c);
      } else {
        waypointMarker.draw(c);
      }
      c.restore();
    }
  }

  /** Draws the data series into the given canvas. */
  private void drawDataSeries(Canvas c) {
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        cvs.drawPath(c);
      }
    }
  }

  /** Draws the colored titles for the data series. */
  private void drawSeriesTitles(Canvas c) {
    int sections = 1;
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        sections++;
      }
    }
    int j = 0;
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        int x = (int) (w * (double) ++j / sections) + getScrollX();
        c.drawText(cvs.getTitle(), x, topBorder, cvs.getLabelPaint());
      }
    }
  }

  /**
   * Sets up the path that is used to draw the chart in onDraw(). The path
   * needs to be updated any time after the data or histogram dimensions change.
   */
  private void setUpPath() {
    synchronized (data) {
      for (ChartValueSeries cvs : series) {
        cvs.getPath().reset();
      }

      if (!data.isEmpty()) {
        drawPaths();
        closePaths();
      }
    }
  }

  /** Actually draws the data points as a path. */
  private void drawPaths() {
    // All of the data points to the respective series.
    // TODO: Come up with a better sampling than Math.max(1, (maxZoomLevel - zoomLevel + 1) / 2);
    int sampling = 1;
    for (int i = 0; i < data.size(); i += sampling) {
      double[] d = data.get(i);
      int min = Math.min(series.length, d.length - 1);
      for (int j = 0; j < min; ++j) {
        ChartValueSeries cvs = series[j];
        Path path = cvs.getPath();
        int x = getX(d[0]);
        int y = getY(cvs, d[j + 1]);
        if (i == 0) {
          path.moveTo(x, y);
        } else {
          path.lineTo(x, y);
        }
      }
    }
  }

  /** Closes the drawn path so it looks like a solid graph. */
  private void closePaths() {
    // Close the path.
    int yCorner = topBorder + effectiveHeight;
    int xCorner = getX(data.get(0)[0]);
    int min = series.length;
    for (int j = 0; j < min; j++) {
      ChartValueSeries cvs = series[j];
      Path path = cvs.getPath();
      int first = getFirstPointPopulatedIndex(j + 1);
      if (first != -1) {
        // Bottom right corner
        path.lineTo(getX(data.get(data.size() - 1)[0]), yCorner);
        // Bottom left corner
        path.lineTo(xCorner, yCorner);
        // Top right corner
        path.lineTo(xCorner, getY(cvs, data.get(first)[j + 1]));
      }
    }
  }

  /**
   * Finds the index of the first point which has a series populated.
   *
   * @param seriesIndex The index of the value series to search for
   * @return The index in the first data for the point in the series that has series
   *         index value populated or -1 if none is found
   */
  private int getFirstPointPopulatedIndex(int seriesIndex) {
    for (int i = 0; i < data.size(); i++) {
      if (data.get(i).length > seriesIndex) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Updates the chart dimensions.
   */
  private void updateDimensions() {
    maxX = xMonitor.getMax();
    if (data.size() <= 1) {
      maxX = 1;
    }
    for (ChartValueSeries cvs : series) {
      cvs.updateDimension();
    }
    // TODO: This is totally broken. Make sure that we calculate based on measureText for each
    // grid line, as the labels may vary across intervals.
    int maxLength = 0;
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        maxLength += cvs.getMaxLabelLength();
      }
    }
    float density = getContext().getResources().getDisplayMetrics().density;
    maxLength = Math.max(maxLength, 1);
    leftBorder = (int) (density * (4 + 8 * maxLength));
    bottomBorder = (int) (density * BOTTOM_BORDER);
    topBorder = (int) (density * TOP_BORDER);
    updateEffectiveDimensions();
  }

  /** Updates the effective dimensions where the graph will be drawn. */
  private void updateEffectiveDimensions() {
    effectiveWidth = Math.max(0, w - leftBorder - RIGHT_BORDER);
    effectiveHeight = Math.max(0, h - topBorder - bottomBorder);
  }

  /**
   * Updates the effective dimensions where the graph will be drawn, only if the
   * dimensions of the given canvas have changed since the last call.
   */
  private void updateEffectiveDimensionsIfChanged(Canvas c) {
    if (w != c.getWidth() || h != c.getHeight()) {
      // Dimensions have changed (for example due to orientation change).
      w = c.getWidth();
      h = c.getHeight();
      updateEffectiveDimensions();
      setUpPath();
    }
  }

  private int getX(double distance) {
    return leftBorder + (int) ((distance * effectiveWidth / maxX) * zoomLevel);
  }

  private int getY(ChartValueSeries cvs, double y) {
    int effectiveSpread = cvs.getInterval() * MAX_INTERVALS;
    return topBorder + effectiveHeight
        - (int) ((y - cvs.getMin()) * effectiveHeight / effectiveSpread);
  }

  /** Draws the labels on the X axis into the given canvas. */
  private void drawXLabels(Canvas c) {
    double interval = (int) (maxX / zoomLevel / 4);
    boolean shortFormat = false;
    if (interval < 1) {
      interval = .5;
      shortFormat = true;
    } else if (interval < 5) {
      interval = 2;
    } else if (interval < 10) {
      interval = 5;
    } else {
      interval = (interval / 10) * 10;
    }
    drawXLabel(c, 0, shortFormat);
    int numLabels = 1;
    for (int i = 1; i * interval < maxX; i++) {
      drawXLabel(c, i * interval, shortFormat);
      numLabels++;
    }
    if (numLabels < 2) {
      drawXLabel(c, (int) maxX, shortFormat);
    }
  }

  /** Draws the labels on the Y axis into the given canvas. */
  private float drawYLabels(ChartValueSeries cvs, Canvas c, int x) {
    int interval = cvs.getInterval();
    float maxTextWidth = 0;
    for (int i = 0; i < MAX_INTERVALS; ++i) {
      maxTextWidth = Math.max(maxTextWidth, drawYLabel(cvs, c, x, i * interval + cvs.getMin()));
    }
    return maxTextWidth;
  }

  /** Draws a single label on the X axis. */
  private void drawXLabel(Canvas c, double x, boolean shortFormat) {
    if (x < 0) {
      return;
    }
    String s =
        (mode == Mode.BY_DISTANCE)
            ? (shortFormat ? X_SHORT_FORMAT.format(x) : X_FORMAT.format(x))
            : StringUtils.formatTime((long) x);
    c.drawText(s,
               getX(x),
               effectiveHeight + UNIT_BORDER + topBorder,
               labelPaint);
  }

  /** Draws a single label on the Y axis. */
  private float drawYLabel(ChartValueSeries cvs, Canvas c, int x, int y) {
    int desiredY = (int) ((y - cvs.getMin()) * effectiveHeight /
        (cvs.getInterval() * MAX_INTERVALS));
    desiredY = topBorder + effectiveHeight + FONT_HEIGHT / 2 - desiredY - 1;
    Paint p = new Paint(cvs.getLabelPaint());
    p.setTextAlign(Align.RIGHT);
    String text = cvs.getFormat().format(y);
    c.drawText(text, x, desiredY, p);
    return p.measureText(text);
  }

  /** Draws the actual X axis line and its label. */
  private void drawXAxis(Canvas canvas) {
    float rightEdge = getX(maxX);
    final int y = effectiveHeight + topBorder;
    canvas.drawLine(leftBorder, y, rightEdge, y, borderPaint);
    Context c = getContext();
    String s = mode == Mode.BY_DISTANCE
        ? (metricUnits ? c.getString(R.string.kilometer) : c.getString(R.string.mile))
        : c.getString(R.string.min);
    canvas.drawText(s, rightEdge, effectiveHeight + .2f * UNIT_BORDER + topBorder, labelPaint);
  }

  /** Draws the actual Y axis line and its label. */
  private void drawYAxis(Canvas canvas) {
    canvas.drawRect(0, 0,
        leftBorder - 1, effectiveHeight + topBorder + UNIT_BORDER + 1,
        clearPaint);
    canvas.drawLine(leftBorder, UNIT_BORDER + topBorder,
                    leftBorder, effectiveHeight + topBorder,
                    borderPaint);
    for (int i = 1; i < MAX_INTERVALS; ++i) {
      int y = i * effectiveHeight / MAX_INTERVALS + topBorder;
      canvas.drawLine(leftBorder - 5, y, leftBorder, y, gridPaint);
    }

    Context c = getContext();
    // TODO: This should really show units for all series.
    String s = metricUnits ? c.getString(R.string.meter) : c.getString(R.string.feet);
    canvas.drawText(s, leftBorder - UNIT_BORDER * .2f, UNIT_BORDER * .8f + topBorder, labelPaint);
  }

  /** Draws the grid for the graph. */
  private void drawGrid(Canvas c) {
    float rightEdge = getX(maxX);
    for (int i = 1; i < MAX_INTERVALS; ++i) {
      int y = i * effectiveHeight / MAX_INTERVALS + topBorder;
      c.drawLine(leftBorder, y, rightEdge, y, gridBarPaint);
    }
  }

  /**
   * Returns whether a given time series is enabled for drawing.
   *
   * @param index the time series, one of {@link #ELEVATION_SERIES},
   *        {@link #SPEED_SERIES}, {@link #POWER_SERIES}, etc.
   * @return true if drawn, false otherwise
   */
  public boolean isChartValueSeriesEnabled(int index) {
    return series[index].isEnabled();
  }

  /**
   * Sets whether a given time series will be enabled for drawing.
   *
   * @param index the time series, one of {@link #ELEVATION_SERIES},
   *        {@link #SPEED_SERIES}, {@link #POWER_SERIES}, etc.
   */
  public void setChartValueSeriesEnabled(int index, boolean enabled) {
    series[index].setEnabled(enabled);
  }
}
