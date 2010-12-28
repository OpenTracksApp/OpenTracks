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

import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.ExtremityMonitor;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
  private final int minZoomLevel = 1;
  private int maxZoomLevel = 5;

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
  private static final float BOTTOM_BORDER = 100;
  
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
  private final NumberFormat xFormat = new DecimalFormat("###,###");
  private final NumberFormat xShortFormat = new DecimalFormat("#.0");

  /*
   * Paints etc. used when drawing the histogram:
   */
  private final Paint borderPaint = new Paint();
  private final Paint labelPaint = new Paint();
  private final Paint gridPaint = new Paint();
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

  private boolean yLabelMask[] = null;

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

  public void setUpChartValueSeries(Context context) {
    series = new ChartValueSeries[NUM_SERIES];

    // Create the value series
    series[ELEVATION_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.green,
                             -1,
                             100,
                             R.string.elevation);

    series[SPEED_SERIES] =
        new ChartValueSeries(context,
                             "###,###.0",
                             R.color.blue_transparent,
                             R.color.blue,
                             5,
                             R.string.speed);
    series[POWER_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.power_fill,
                             R.color.power_border,
                             5,
                             R.string.power);
    series[POWER_SERIES].setAbsoluteMax(1500);
    series[CADENCE_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.cadence_fill,
                             R.color.cadence_border,
                             5,
                             R.string.cadence);
    series[HEART_RATE_SERIES] =
        new ChartValueSeries(context,
                             "###,###",
                             R.color.heartrate_fill,
                             R.color.heartrate_border,
                             5,
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

  /**
   * Gets the data that is displayed by the chart.
   *
   * @return an array list with data points
   */
  public ArrayList<double[]> getData() {
    return data;
  }

  /**
   * Sets the data that is to be displayed by the chart.
   *
   * @param theData an array list of data points
   */
  public synchronized void setDataPoints(ArrayList<double[]> theData) {
    scrollTo(0, 0);
    zoomLevel = 1;
    data.clear();
    xMonitor.reset();
    for (ChartValueSeries cvs : series) {
      cvs.reset();
    }
    addDataPoints(theData);
  }

  /**
   * Adds a new data point to the chart.
   *
   * @param theData a data point
   */
  public synchronized void addDataPoint(double[] theData) {
    data.add(theData);
    addDataPointInternal(theData);
    updateDimensions();
    setupPath();
  }

  private void addDataPointInternal(double[] theData) {
    xMonitor.update(theData[0]);
    int min = Math.min(series.length, theData.length - 1);
    for (int i = 0; i < min; i++) {
      if (!Double.isNaN(theData[i + 1])) {
        series[i].update(theData[i + 1]);
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
  public synchronized void addDataPoints(ArrayList<double[]> theData) {
    data.addAll(theData);
    for (int i = 0; i < theData.size(); i++) {
      double d[] = theData.get(i);
      addDataPointInternal(d);
    }
    updateDimensions();
    setupPath();
  }

  /**
   * Clears all data.
   * Call this only from the UI thread!
   */
  public synchronized void reset() {
    data.clear();
    zoomLevel = 1;
    scrollTo(0, 0);
    updateDimensions();
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
    return zoomLevel > minZoomLevel;
  }

  /**
   * Zooms in one level (factor 2).
   */
  public void zoomIn() {
    if (canZoomIn()) {
      zoomLevel++;
      setupPath();
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
      setupPath();
      invalidate();
    }
  }

  /**
   * @return the current zoom level (1 equals to showing all data points)
   */
  public int getZoomLevel() {
    return zoomLevel;
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
   * Sets the scroll position of the chart. This will trigger a redraw.
   */
  @Override
  public void scrollTo(int x, int y) {
    super.scrollTo(x, y);
  }

  /**
   * @return the current display mode (by distance, by time)
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Sets the display mode (by distance, by time).
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
                new Intent(getContext(), MyTracksWaypointDetails.class);
            intent.putExtra("waypointid", nearestWaypoint.getId());
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
  protected synchronized void onDraw(Canvas c) {
    if (w != c.getWidth() || h != c.getHeight()) {
      // Dimensions have changed (for example due to orientation change):
      w = c.getWidth();
      h = c.getHeight();
      effectiveWidth = Math.max(0, w - leftBorder - RIGHT_BORDER);
      effectiveHeight = Math.max(0, h - topBorder - bottomBorder);
      setupPath();
    }
    c.save();
    c.drawColor(Color.WHITE);
    if (data.size() < 1) {
      drawXAxis(c);
      drawYAxis(c);
      c.restore();
      return;
    }

    // Draw the data series
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        cvs.drawPath(c);
      }
    }
    drawGrid(c);

    // Draw the waypoints:
    for (int i = 1; i < waypoints.size(); i++) {
      final Waypoint waypoint = waypoints.get(i);
      if (waypoint.getLocation() == null) {
        continue;
      }
      c.save();

      final float x = getWaypointX(waypoint);
      c.drawLine(x, h - bottomBorder, x, topBorder, gridPaint);
      c.translate(x - (markerWidth / 2), markerHeight);
      if (waypoints.get(i).getType() == Waypoint.TYPE_STATISTICS) {
        statsMarker.draw(c);
      } else {
        waypointMarker.draw(c);
      }
      c.restore();
    }

    // Draw the axis and labels:
    drawXLabels(c);
    drawXAxis(c);
    drawSeriesTitles(c);

    yLabelMask = new boolean[effectiveHeight / FONT_HEIGHT + 1];
    c.translate(getScrollX(), 0);
    drawYAxis(c);
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled() && cvs.hasData()) {
        drawYLabels(cvs, c);
      }
    }
    c.restore();
    if (showPointer && data.size() > 0) {
      c.translate(getX(maxX) - pointer.getIntrinsicWidth() / 2,
                  (getY(series[0], data.get(data.size() - 1)[1])
                   - pointer.getIntrinsicHeight() / 2 - 12));
      pointer.draw(c);
    }
  }

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
        int y = (int) (w * zoomLevel * ((double) (++j) / sections));
        c.drawText(cvs.getTitle(), y, topBorder, cvs.getLabelPaint());
      }
    }
  }

  /**
   * Sets up the path that is used to draw the histogram in onDraw(). The path
   * needs to be updated any time after the data or histogram dimensions change.
   */
  private synchronized void setupPath() {
    for (ChartValueSeries cvs : series) {
      cvs.getPath().reset();
    }
    if (data.size() < 1) {
      return;
    }

    // All of the data points to the respective series.
    for (int i = 0; i < data.size(); i++) {
      double[] d = data.get(i);
      int min = Math.min(series.length, d.length - 1);

      for (int j = 0; j < min; j++) {
        ChartValueSeries cvs = series[j];
        Path path = cvs.getPath();
        path.lineTo(getX(d[0]), getY(cvs, d[j + 1]));
      }
    }

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
   * Find the index of the first point which has a series populated.
   * @param seriesIndex The index of the value series to search for.
   * @return The index in the first data for the point in the series that has series
   *         index value populated or -1 if none is found.
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
   * Update the histogram dimensions.
   */
  private void updateDimensions() {
    maxX = xMonitor.getMax();
    if (data.size() <= 1) {
      maxX = 1;
    }
    for (ChartValueSeries cvs : series) {
      cvs.updateDimension();
    }
    int maxLength = 0;
    for (ChartValueSeries cvs : series) {
      maxLength = Math.max(maxLength,
                           cvs.getMaxLabelLength());
    }
    leftBorder = 4 + 7 * maxLength;
    effectiveWidth = w - leftBorder - RIGHT_BORDER;
    float density = getContext().getResources().getDisplayMetrics().density;
    bottomBorder = (int) (density * BOTTOM_BORDER);
    topBorder = (int) (density * TOP_BORDER);
    effectiveHeight = h - topBorder - bottomBorder;
  }

  private int getX(double distance) {
    return leftBorder + (int) ((distance * effectiveWidth / maxX) * zoomLevel);
  }

  private int getY(ChartValueSeries cvs, double y) {
    return topBorder + effectiveHeight
        - (int) ((y - cvs.getMin()) * effectiveHeight / cvs.getSpread());
  }

  private void drawXLabels(Canvas c) {
    int numLabels = 0;
    double interval = (int) (maxX / zoomLevel / 4);
    boolean shortFormat = false;
    if (interval < 1) {
      interval = .5;
      shortFormat = true;
    } else {
      if (interval < 5) {
        interval = 2;
      } else if (interval < 10) {
        interval = 5;
      } else {
        interval = (interval / 10) * 10;
      }
    }
    drawXLabel(c, 0, shortFormat);
    numLabels++;
    for (int i = 1; i * interval < maxX; i++) {
      drawXLabel(c, i * interval, shortFormat);
      numLabels++;
    }
    if (numLabels < 2) {
      drawXLabel(c, (int) maxX, shortFormat);
    }
  }

  private void drawYLabels(ChartValueSeries cvs, Canvas c) {
    int numLabels = 0;
    drawYLabel(cvs, c, 0);
    final int y0 = getY(cvs, 0);
    numLabels++;
    int interval = cvs.getInterval();
    for (int i = 0; i * interval < cvs.getSpread(); i++) {
      final int y = i * interval + cvs.getMin();
      final int yi = getY(cvs, y);
      if (Math.abs(y0 - yi) > 15) {
        drawYLabel(cvs, c, y);
        numLabels++;
      }
    }
    if (numLabels < 3) {
      drawYLabel(cvs, c, cvs.getMax());
    }
  }

  private void drawXLabel(Canvas c, double x, boolean shortFormat) {
    String s =
        (mode == Mode.BY_DISTANCE)
            ? (shortFormat ? xShortFormat.format(x) : xFormat.format(x))
            : StringUtils.formatTime((long) x);
    c.drawText(s,
               getX(x),
               effectiveHeight + UNIT_BORDER + topBorder,
               labelPaint);
  }

  private void drawYLabel(ChartValueSeries cvs, Canvas c, int y) {
    int desiredY =
        (int) ((y - cvs.getMin()) * effectiveHeight / cvs.getSpread());

    // Make sure we don't write one label on top of another.
    int slot = desiredY / FONT_HEIGHT;
    if (slot < 0) {
      return;
    }
    if (yLabelMask[slot]) {
      desiredY = ++slot * FONT_HEIGHT;
    }
    desiredY = topBorder + effectiveHeight + 5 - desiredY;
    if (slot < yLabelMask.length) {
      yLabelMask[slot] = true;
    }
    c.drawText(cvs.getFormat().format(y),
               2, desiredY,
               cvs.getLabelPaint());
  }

  private void drawXAxis(Canvas canvas) {
    float rightEdge = getX(maxX);
    canvas.drawLine(leftBorder, effectiveHeight + topBorder, rightEdge,
        effectiveHeight + topBorder, borderPaint);
    Context c = getContext();
    String s =
        (mode == Mode.BY_DISTANCE)
            ? (metricUnits ? c.getString(R.string.kilometer)
            : c.getString(R.string.mile)) : (c.getString(R.string.min));
    canvas.drawText(s,
                    rightEdge,
                    effectiveHeight + .2f * UNIT_BORDER + topBorder,
                    labelPaint);
  }

  private void drawYAxis(Canvas canvas) {
    canvas.drawLine(leftBorder, UNIT_BORDER + topBorder,
                    leftBorder, effectiveHeight + topBorder,
                    borderPaint);
    canvas.drawRect(0, 0,
                    leftBorder - 1, effectiveHeight + topBorder + 1,
                    clearPaint);
    Context c = getContext();
    // TODO: This should really show units for all series.
    String s = metricUnits
        ? c.getString(R.string.meter)
        : c.getString(R.string.feet);
    canvas.drawText(s, leftBorder - UNIT_BORDER * .2f,
        UNIT_BORDER * .8f + topBorder, labelPaint);
  }

  private synchronized void drawGrid(Canvas c) {
    if (data.size() < 1) {
      return;
    }
    for (ChartValueSeries cvs : series) {
      if (cvs.isEnabled()) {
        int interval = cvs.getInterval();
        for (int i = 1; i * interval < cvs.getSpread(); i++) {
          int y = getY(cvs, i * interval + cvs.getMin());
          c.drawLine(getX(data.get(0)[0]), y,
                     getX(data.get(data.size() - 1)[0]), y,
                     cvs.getPaint());
        }
      }
    }
  }

  /**
   * Gets one of the chart value series.
   * The index should be one of the following values:
   *   ELEVATION_SERIES or SPEED_SERIES
   *
   * @param index of the value series
   * @return The chart series at the index
   */
  public ChartValueSeries getChartValueSeries(int index) {
    return series[index];
  }
}
