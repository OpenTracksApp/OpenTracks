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

package de.dennisguse.opentracks.chart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.dennisguse.opentracks.MarkerDetailActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.stats.ExtremityMonitor;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.MarkerUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.ThemeUtils;

/**
 * Visualization of the chart.
 * Provides support for zooming (via pinch), scrolling, flinging, and selecting shown markers (single touch).
 *
 * @author Sandor Dornbush
 * @author Leif Hendrik Wilden
 */
public class ChartView extends View {

    static final int Y_AXIS_INTERVALS = 5;

    private static final int TARGET_X_AXIS_INTERVALS = 4;

    private static final int MIN_ZOOM_LEVEL = 1;
    private static final int MAX_ZOOM_LEVEL = 10;

    private static final NumberFormat X_NUMBER_FORMAT = NumberFormat.getIntegerInstance();
    private static final NumberFormat X_FRACTION_FORMAT = NumberFormat.getNumberInstance();
    private static final int BORDER = 8;
    private static final int SPACER = 4;
    private static final int Y_AXIS_OFFSET = 16;

    //TODO Determine from actual size of the used drawable
    private static final float MARKER_X_ANCHOR = 13f / 48f;

    static {
        X_FRACTION_FORMAT.setMaximumFractionDigits(1);
        X_FRACTION_FORMAT.setMinimumFractionDigits(1);
    }

    private final List<ChartValueSeries> seriesList = new ArrayList<>();
    private final ChartValueSeries speedSeries;
    private final ChartValueSeries paceSeries;

    private final List<ChartPoint> chartPoints = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private final ExtremityMonitor xExtremityMonitor = new ExtremityMonitor();
    private final int backgroundColor;
    private final Paint axisPaint;
    private final Paint xAxisMarkerPaint;
    private final Paint gridPaint;
    private final Paint markerPaint;
    private final Drawable pointer;
    private final Drawable markerPin;
    private final int markerWidth;
    private final int markerHeight;
    private final Scroller scroller;
    private double maxX = 1.0;
    private int zoomLevel = 1;

    private int leftBorder = BORDER;
    private int topBorder = BORDER;
    private int bottomBorder = BORDER;
    private int rightBorder = BORDER;
    private int spacer = SPACER;
    private int yAxisOffset = Y_AXIS_OFFSET;

    private int width = 0;
    private int height = 0;
    private int effectiveWidth = 0;
    private int effectiveHeight = 0;

    private boolean chartByDistance = false;
    private boolean metricUnits = true;
    private boolean reportSpeed = true;
    private boolean showPointer = false;

    private final GestureDetectorCompat detectorScrollFlingTab = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceX) > 0) {
                int availableToScroll = effectiveWidth * (zoomLevel - 1) - getScrollX();
                if (availableToScroll > 0) {
                    scrollBy(Math.min(availableToScroll, (int) distanceX));
                }
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            // Check if the y event is within markerHeight of the marker center
            if (Math.abs(event.getY() - topBorder - spacer - markerHeight / 2f) < markerHeight) {
                int minDistance = Integer.MAX_VALUE;
                Marker nearestMarker = null;
                synchronized (markers) {
                    for (Marker marker : markers) {
                        int distance = Math.abs(getX(getMarkerXValue(marker)) - (int) event.getX() - getScrollX());
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestMarker = marker;
                        }
                    }
                }
                if (nearestMarker != null && minDistance < markerWidth) {
                    Intent intent = IntentUtils.newIntent(getContext(), MarkerDetailActivity.class)
                            .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, nearestMarker.getId());
                    getContext().startActivity(intent);
                    return true;
                }
            }

            return false;
        }
    });

    private final ScaleGestureDetector detectorZoom = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            if (scaleFactor >= 1.1f) {
                zoomIn();
                return true;
            } else if (scaleFactor <= 0.9) {
                zoomOut();
                return true;
            }
            return false;
        }
    });

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        int fontSizeSmall = ThemeUtils.getFontSizeSmallInPx(context);
        int fontSizeMedium = ThemeUtils.getFontSizeMediumInPx(context);

        seriesList.add(new ChartValueSeries(context,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new int[]{5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000},
                R.string.description_altitude_metric,
                R.string.description_altitude_imperial,
                R.color.chart_altitude_fill,
                R.color.chart_altitude_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getAltitude();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return true;
            }
        });

        speedSeries = new ChartValueSeries(context,
                0,
                Integer.MAX_VALUE,
                new int[]{1, 5, 10, 20, 50, 100},
                R.string.description_speed_metric,
                R.string.description_speed_imperial,
                R.color.chart_speed_fill,
                R.color.chart_speed_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getSpeed();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return reportSpeed;
            }
        };
        seriesList.add(speedSeries);

        paceSeries = new ChartValueSeries(context,
                0,
                Integer.MAX_VALUE,
                new int[]{1, 2, 5, 10, 15, 20, 30, 60, 120},
                R.string.description_pace_metric,
                R.string.description_pace_imperial,
                R.color.chart_pace_fill,
                R.color.chart_pace_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getPace();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return !reportSpeed;
            }
        };
        seriesList.add(paceSeries);

        seriesList.add(new ChartValueSeries(context,
                0,
                Integer.MAX_VALUE,
                new int[]{25, 50},
                R.string.description_sensor_heart_rate,
                R.string.description_sensor_heart_rate,
                R.color.chart_heart_rate_fill,
                R.color.chart_heart_rate_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getHeartRate();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return false;
            }
        });

        seriesList.add(new ChartValueSeries(context,
                0,
                Integer.MAX_VALUE,
                new int[]{5, 10, 25, 50},
                R.string.description_sensor_cadence,
                R.string.description_sensor_cadence,
                R.color.chart_cadence_fill,
                R.color.chart_cadence_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getCadence();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return false;
            }
        });
        seriesList.add(new ChartValueSeries(context,
                0,
                1000,
                new int[]{5, 50, 100, 200},
                R.string.description_sensor_power,
                R.string.description_sensor_power,
                R.color.chart_power_fill,
                R.color.chart_power_border,
                fontSizeSmall,
                fontSizeMedium) {
            @Override
            protected Double extractDataFromChartPoint(@NonNull ChartPoint chartPoint) {
                return chartPoint.getPower();
            }

            @Override
            protected boolean drawIfChartPointHasNoData() {
                return false;
            }
        });

        backgroundColor = ThemeUtils.getBackgroundColor(context);

        axisPaint = new Paint();
        axisPaint.setStyle(Style.FILL_AND_STROKE);
        axisPaint.setColor(ThemeUtils.getTextColorPrimary(context));
        axisPaint.setAntiAlias(true);
        axisPaint.setTextSize(fontSizeSmall);

        xAxisMarkerPaint = new Paint(axisPaint);
        xAxisMarkerPaint.setTextAlign(Align.CENTER);

        gridPaint = new Paint();
        gridPaint.setStyle(Style.STROKE);
        gridPaint.setColor(ThemeUtils.getTextColorSecondary(context));
        gridPaint.setAntiAlias(false);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{3, 2}, 0));

        markerPaint = new Paint();
        markerPaint.setStyle(Style.STROKE);
        markerPaint.setAntiAlias(false);

        pointer = ContextCompat.getDrawable(context, R.drawable.ic_logo_color_24dp);
        pointer.setBounds(0, 0, pointer.getIntrinsicWidth(), pointer.getIntrinsicHeight());

        markerPin = MarkerUtils.getDefaultPhoto(context);
        markerWidth = markerPin.getIntrinsicWidth();
        markerHeight = markerPin.getIntrinsicHeight();
        markerPin.setBounds(0, 0, markerWidth, markerHeight);

        scroller = new Scroller(context);
        setFocusable(true);
        setClickable(true);
        updateDimensions();

        // either speedSeries or paceSeries should be enabled.
        speedSeries.setEnabled(reportSpeed);
        paceSeries.setEnabled(!reportSpeed);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return true;
    }

    public void setChartByDistance(boolean chartByDistance) {
        this.chartByDistance = chartByDistance;
    }

    public boolean getMetricUnits() {
        return metricUnits;
    }

    /**
     * Sets metric units.
     *
     * @param value true to use metric units
     */
    public void setMetricUnits(boolean value) {
        metricUnits = value;
    }

    public boolean getReportSpeed() {
        return reportSpeed;
    }

    /**
     * Sets report speed.
     *
     * @param value report speed (true) or pace (false)
     */
    public void setReportSpeed(boolean value) {
        reportSpeed = value;
    }

    public boolean applyReportSpeed() {
        if (reportSpeed) {
            if (!speedSeries.isEnabled()) {
                speedSeries.setEnabled(true);
                paceSeries.setEnabled(false);
                return true;
            }
        } else {
            if (!paceSeries.isEnabled()) {
                speedSeries.setEnabled(false);
                paceSeries.setEnabled(true);
                return true;
            }
        }

        return false;
    }

    public void setShowPointer(boolean value) {
        showPointer = value;
    }

    public void addChartPoints(List<ChartPoint> dataPoints) {
        synchronized (chartPoints) {
            chartPoints.addAll(dataPoints);
            for (ChartPoint dataPoint : dataPoints) {
                xExtremityMonitor.update(dataPoint.getTimeOrDistance());
                for (ChartValueSeries i : seriesList) {
                    i.update(dataPoint);
                }
            }
            updateDimensions();
            updateSeries();
        }
    }

    /**
     * Clears all data.
     */
    public void reset() {
        synchronized (chartPoints) {
            chartPoints.clear();
            xExtremityMonitor.reset();
            zoomLevel = 1;
            updateDimensions();
        }
    }

    /**
     * Resets scroll.
     * To be called on the UI thread.
     */
    public void resetScroll() {
        scrollTo(0, 0);
    }

    public void addMarker(Marker marker) {
        synchronized (markers) {
            markers.add(marker);
        }
    }

    public void clearMarker() {
        synchronized (markers) {
            markers.clear();
        }
    }

    private boolean canZoomIn() {
        return zoomLevel < MAX_ZOOM_LEVEL;
    }

    private boolean canZoomOut() {
        return zoomLevel > MIN_ZOOM_LEVEL;
    }

    private void zoomIn() {
        if (canZoomIn()) {
            zoomLevel++;
            updateSeries();
            invalidate();
        }
    }

    private void zoomOut() {
        if (canZoomOut()) {
            zoomLevel--;
            scroller.abortAnimation();
            int scrollX = getScrollX();
            int maxWidth = effectiveWidth * (zoomLevel - 1);
            if (scrollX > maxWidth) {
                scrollX = maxWidth;
                scrollTo(scrollX, 0);
            }
            updateSeries();
            invalidate();
        }
    }

    /**
     * Initiates flinging.
     *
     * @param velocityX velocity of fling in pixels per second
     */
    private void fling(int velocityX) {
        int maxWidth = effectiveWidth * (zoomLevel - 1);
        scroller.fling(getScrollX(), 0, velocityX, 0, 0, maxWidth, 0, 0);
        invalidate();
    }

    /**
     * Handle parent's view disallow touch event.
     *
     * @param disallow Does disallow parent touch event?
     */
    private void requestDisallowInterceptTouchEventInParent(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    /**
     * Scrolls the view horizontally by a given amount.
     *
     * @param deltaX the number of pixels to scroll
     */
    private void scrollBy(int deltaX) {
        int scrollX = getScrollX() + deltaX;
        if (scrollX <= 0) {
            scrollX = 0;
        }

        int maxWidth = effectiveWidth * (zoomLevel - 1);
        if (scrollX >= maxWidth) {
            scrollX = maxWidth;
        }

        scrollTo(scrollX, 0);
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
        boolean isZoom = detectorZoom.onTouchEvent(event);
        boolean isScrollTab = detectorScrollFlingTab.onTouchEvent(event);

        // ChartView handles zoom gestures (more than one pointer) and all gestures when zoomed itself
        requestDisallowInterceptTouchEventInParent(event.getPointerCount() != 1 || zoomLevel != MIN_ZOOM_LEVEL);

        return isZoom || isScrollTab;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateEffectiveDimensionsIfChanged(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (chartPoints) {
            canvas.save();

            canvas.drawColor(backgroundColor);

            canvas.save();

            clipToGraphArea(canvas);
            drawDataSeries(canvas);
            drawMarker(canvas);
            drawGrid(canvas);

            canvas.restore();

            drawSeriesTitles(canvas);
            drawXAxis(canvas);
            drawYAxis(canvas);

            canvas.restore();

            if (showPointer) {
                drawPointer(canvas);
            }
        }
    }

    /**
     * Clips a canvas to the graph area.
     *
     * @param canvas the canvas
     */
    private void clipToGraphArea(Canvas canvas) {
        int x = getScrollX() + leftBorder;
        int y = topBorder;
        canvas.clipRect(x, y, x + effectiveWidth, y + effectiveHeight);
    }

    /**
     * Draws the data series.
     *
     * @param canvas the canvas
     */
    private void drawDataSeries(Canvas canvas) {
        for (ChartValueSeries chartValueSeries : seriesList) {
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData()) {
                chartValueSeries.drawPath(canvas);
            }
        }
    }

    private void drawMarker(Canvas canvas) {
        synchronized (markers) {
            for (Marker marker : markers) {
                double xValue = getMarkerXValue(marker);
                if (xValue > maxX) {
                    continue;
                }
                canvas.save();
                float x = getX(getMarkerXValue(marker));
                canvas.drawLine(x, topBorder + spacer + markerHeight / 2, x, topBorder + effectiveHeight, markerPaint);
                canvas.translate(x - (markerWidth * MARKER_X_ANCHOR), topBorder + spacer);

                markerPin.draw(canvas);
                canvas.restore();
            }
        }
    }

    /**
     * Draws the grid.
     *
     * @param canvas the canvas
     */
    private void drawGrid(Canvas canvas) {
        // X axis grid
        List<Double> xAxisMarkerPositions = getXAxisMarkerPositions(getXAxisInterval());
        for (double position : xAxisMarkerPositions) {
            int x = getX(position);
            canvas.drawLine(x, topBorder, x, topBorder + effectiveHeight, gridPaint);
        }
        // Y axis grid
        float rightEdge = getX(maxX);
        for (int i = 0; i <= Y_AXIS_INTERVALS; i++) {
            double percentage = (double) i / Y_AXIS_INTERVALS;
            int range = effectiveHeight - 2 * yAxisOffset;
            int y = topBorder + yAxisOffset + (int) (percentage * range);
            canvas.drawLine(leftBorder, y, rightEdge, y, gridPaint);
        }
    }

    /**
     * Draws series titles.
     *
     * @param canvas the canvas
     */
    private void drawSeriesTitles(Canvas canvas) {
        int[] titleDimensions = getTitleDimensions();
        int lines = titleDimensions[0];
        int lineHeight = titleDimensions[1];
        int count = 0;
        for (ChartValueSeries chartValueSeries : seriesList) {
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData() || allowIfEmpty(chartValueSeries)) {
                count++;
                String title = getContext().getString(chartValueSeries.getTitleId(metricUnits));
                Paint paint = chartValueSeries.getTitlePaint();
                int x = (int) (0.5 * width) + getScrollX();
                int y = topBorder - spacer - (lines - count) * (lineHeight + spacer);
                canvas.drawText(title, x, y, paint);
            }
        }
    }

    /**
     * Gets the title dimensions.
     * Returns an array of 2 integers, first element is the number of lines and the second element is the line height.
     */
    private int[] getTitleDimensions() {
        int lines = 0;
        int lineHeight = 0;
        for (ChartValueSeries chartValueSeries : seriesList) {
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData() || allowIfEmpty(chartValueSeries)) {
                lines++;
                String title = getContext().getString(chartValueSeries.getTitleId(metricUnits));
                Rect rect = getRect(chartValueSeries.getTitlePaint(), title);
                if (rect.height() > lineHeight) {
                    lineHeight = rect.height();
                }
            }
        }
        return new int[]{lines, lineHeight};
    }

    /**
     * Draws the x axis.
     *
     * @param canvas the canvas
     */
    private void drawXAxis(Canvas canvas) {
        int x = getScrollX() + leftBorder;
        int y = topBorder + effectiveHeight;
        canvas.drawLine(x, y, x + effectiveWidth, y, axisPaint);
        String label = getXAxisLabel();
        Rect rect = getRect(axisPaint, label);
        int yOffset = rect.height() / 2;
        canvas.drawText(label, x + effectiveWidth + spacer, y + yOffset, axisPaint);

        double interval = getXAxisInterval();
        NumberFormat numberFormat = interval < 1 ? X_FRACTION_FORMAT : X_NUMBER_FORMAT;

        for (double markerPosition : getXAxisMarkerPositions(interval)) {
            drawXAxisMarker(canvas, markerPosition, numberFormat, spacer + yOffset);
        }
    }

    private String getXAxisLabel() {
        Context context = getContext();
        if (chartByDistance) {
            return metricUnits ? context.getString(R.string.unit_kilometer) : context.getString(R.string.unit_mile);
        } else {
            return context.getString(R.string.description_time);
        }
    }

    /**
     * Draws a x axis marker.
     *
     * @param canvas       canvas
     * @param value        value
     * @param numberFormat the number format
     * @param spacing      the spacing between x axis and marker
     */
    private void drawXAxisMarker(Canvas canvas, double value, NumberFormat numberFormat, int spacing) {
        String marker = chartByDistance ? numberFormat.format(value) : StringUtils.formatElapsedTime((Duration.ofMillis((long) value)));
        Rect rect = getRect(xAxisMarkerPaint, marker);
        canvas.drawText(marker, getX(value), topBorder + effectiveHeight + spacing + rect.height(), xAxisMarkerPaint);
    }

    private double getXAxisInterval() {
        double interval = maxX / zoomLevel / TARGET_X_AXIS_INTERVALS;
        if (interval < 1) {
            interval = .5;
        } else if (interval < 5) {
            interval = 2;
        } else if (interval < 10) {
            interval = 5;
        } else {
            interval = (interval / 10) * 10;
        }
        return interval;
    }

    private List<Double> getXAxisMarkerPositions(double interval) {
        List<Double> markers = new ArrayList<>();
        markers.add(0d);
        for (int i = 1; i * interval < maxX; i++) {
            markers.add(i * interval);
        }

        if (markers.size() < 2) {
            markers.add(maxX);
        }
        return markers;
    }

    /**
     * Draws the y axis.
     *
     * @param canvas the canvas
     */
    private void drawYAxis(Canvas canvas) {
        int x = getScrollX() + leftBorder;
        int y = topBorder;
        canvas.drawLine(x, y, x, y + effectiveHeight, axisPaint);

        //TODO
        int markerXPosition = x - spacer;
        for (int i = 0; i < seriesList.size(); i++) {
            int index = seriesList.size() - 1 - i;
            ChartValueSeries chartValueSeries = seriesList.get(index);
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData() || allowIfEmpty(chartValueSeries)) {
                markerXPosition -= drawYAxisMarkers(chartValueSeries, canvas, markerXPosition) + spacer;
            }
        }
    }

    /**
     * Draws the y axis markers for a chart value series.
     *
     * @param chartValueSeries the chart value series
     * @param canvas           the canvas
     * @param xPosition        the right most x position
     * @return the maximum marker width.
     */
    private float drawYAxisMarkers(ChartValueSeries chartValueSeries, Canvas canvas, int xPosition) {
        int interval = chartValueSeries.getInterval();
        float maxMarkerWidth = 0;
        for (int i = 0; i <= Y_AXIS_INTERVALS; i++) {
            maxMarkerWidth = Math.max(maxMarkerWidth, drawYAxisMarker(chartValueSeries, canvas, xPosition,
                    i * interval + chartValueSeries.getMinMarkerValue()));
        }
        return maxMarkerWidth;
    }

    /**
     * Draws a y axis marker.
     *
     * @param chartValueSeries the chart value series
     * @param canvas           the canvas
     * @param xPosition        the right most x position
     * @param yValue           the y value
     * @return the marker width.
     */
    private float drawYAxisMarker(ChartValueSeries chartValueSeries, Canvas canvas, int xPosition, int yValue) {
        String marker = chartValueSeries.formatMarker(yValue);
        Paint paint = chartValueSeries.getMarkerPaint();
        Rect rect = getRect(paint, marker);
        int yPosition = getY(chartValueSeries, yValue) + (rect.height() / 2);
        canvas.drawText(marker, xPosition, yPosition, paint);
        return paint.measureText(marker);
    }

    /**
     * Draws the current pointer.
     *
     * @param canvas the canvas
     */
    private void drawPointer(Canvas canvas) {
        ChartValueSeries firstChartValueSeries = null;
        for (ChartValueSeries chartValueSeries : seriesList) {
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData()) {
                firstChartValueSeries = chartValueSeries;
                break;
            }
        }
        if (firstChartValueSeries != null && chartPoints.size() > 0) {
            int dx = getX(maxX) - pointer.getIntrinsicWidth() / 2;
            double value = firstChartValueSeries.extractDataFromChartPoint(chartPoints.get(chartPoints.size() - 1));
            int dy = getY(firstChartValueSeries, value) - pointer.getIntrinsicHeight();
            canvas.translate(dx, dy);
            pointer.draw(canvas);
        }
    }

    /**
     * The path needs to be updated any time after the data or the dimensions change.
     */
    private void updateSeries() {
        synchronized (chartPoints) {
            seriesList.stream().forEach(this::updateSerie);
        }
    }

    private void updateSerie(ChartValueSeries series) {
        final int yCorner = topBorder + effectiveHeight;
        final Path path = series.getPath();

        boolean drawFirstPoint = false;
        path.rewind();

        Iterator<ChartPoint> iterator = chartPoints.iterator();
        while (iterator.hasNext()) {
            ChartPoint point = iterator.next();
            if (!series.isChartPointValid(point)) {
                continue;
            }

            double value = series.extractDataFromChartPoint(point);
            int x = getX(point.getTimeOrDistance());
            int y = getY(series, value);

            // start from lower left corner
            if (!drawFirstPoint) {
                path.moveTo(x, yCorner);
                drawFirstPoint = true;
            }

            // draw graph
            path.lineTo(x, y);

            // last point: move to lower right
            if (!iterator.hasNext()) {
                path.lineTo(x, yCorner);
            }
        }

        // back to lower left corner
        path.close();
    }

    /**
     * Updates the chart dimensions.
     */
    private void updateDimensions() {
        maxX = xExtremityMonitor.hasData() ? xExtremityMonitor.getMax() : 1.0;
        for (ChartValueSeries chartValueSeries : seriesList) {
            chartValueSeries.updateDimension();
        }
        float density = getResources().getDisplayMetrics().density;
        spacer = (int) (density * SPACER);
        yAxisOffset = (int) (density * Y_AXIS_OFFSET);

        int markerLength = 0;
        for (ChartValueSeries chartValueSeries : seriesList) {
            if (chartValueSeries.isEnabled() && chartValueSeries.hasData() || allowIfEmpty(chartValueSeries)) {
                Rect rect = getRect(chartValueSeries.getMarkerPaint(), chartValueSeries.getLargestMarker());
                markerLength += rect.width() + spacer;
            }
        }

        leftBorder = (int) (density * BORDER + markerLength);
        int[] titleDimensions = getTitleDimensions();
        topBorder = (int) (density * BORDER + titleDimensions[0] * (titleDimensions[1] + spacer));
        Rect xAxisLabelRect = getRect(axisPaint, getXAxisLabel());
        // border + x axis marker + spacer + .5 x axis label
        bottomBorder = (int) (density * BORDER + getRect(xAxisMarkerPaint, "1").height() + spacer + (xAxisLabelRect.height() / 2));
        rightBorder = (int) (density * BORDER + xAxisLabelRect.width() + spacer);
        updateEffectiveDimensions();
    }

    /**
     * Updates the effective dimensions.
     */
    private void updateEffectiveDimensions() {
        effectiveWidth = Math.max(0, width - leftBorder - rightBorder);
        effectiveHeight = Math.max(0, height - topBorder - bottomBorder);
    }

    /**
     * Updates the effective dimensions if changed.
     *
     * @param newWidth  the new width
     * @param newHeight the new height
     */
    private void updateEffectiveDimensionsIfChanged(int newWidth, int newHeight) {
        if (width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            updateEffectiveDimensions();
            updateSeries();
        }
    }

    /**
     * Gets the x position for a value.
     *
     * @param value the value
     */
    private int getX(double value) {
        if (value > maxX) {
            value = maxX;
        }
        double percentage = value / maxX;
        return leftBorder + (int) (percentage * effectiveWidth * zoomLevel);
    }

    /**
     * Gets the y position for a value in a chart value series
     *
     * @param chartValueSeries the chart value series
     * @param value            the value
     */
    private int getY(ChartValueSeries chartValueSeries, double value) {
        int effectiveSpread = chartValueSeries.getInterval() * Y_AXIS_INTERVALS;
        double percentage = (value - chartValueSeries.getMinMarkerValue()) / effectiveSpread;
        int rangeHeight = effectiveHeight - 2 * yAxisOffset;
        return topBorder + yAxisOffset + (int) ((1 - percentage) * rangeHeight);
    }

    private double getMarkerXValue(Marker marker) {
        if (chartByDistance) {
            return marker.getLength().toKM_Miles(metricUnits);
        } else {
            return marker.getDuration().toMillis();
        }
    }

    /**
     * Gets a paint's Rect for a string.
     *
     * @param paint  the paint
     * @param string the string
     */
    private Rect getRect(Paint paint, String string) {
        Rect rect = new Rect();
        paint.getTextBounds(string, 0, string.length(), rect);
        return rect;
    }

    /**
     * Returns true if the index is allowed when the chartData is empty.
     */
    private boolean allowIfEmpty(ChartValueSeries chartValueSeries) {
        if (!chartPoints.isEmpty()) {
            return false;
        }

        return chartValueSeries.drawIfChartPointHasNoData();
    }
}
