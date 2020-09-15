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

package de.dennisguse.opentracks.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;

import androidx.annotation.NonNull;

import java.text.NumberFormat;

import de.dennisguse.opentracks.stats.ExtremityMonitor;

/**
 * This class encapsulates the meta data for one series of the chart values.
 *
 * @author Sandor Dornbush
 */
abstract class ChartValueSeries {

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
    private final ExtremityMonitor extremityMonitor = new ExtremityMonitor();
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
    private final Path path = new Path();

    private int interval = 1;
    private int minMarkerValue = 0;
    private int maxMarkerValue = interval * ChartView.Y_AXIS_INTERVALS;
    private boolean enabled = true;

    /**
     * Constructor.
     *
     * @param context         the context
     * @param absoluteMin     the absolute min value
     * @param absoluteMax     the absolute max value
     * @param intervalValues  the list of interval values
     * @param metricTitleId   the metric title id
     * @param imperialTitleId the imperial title id
     * @param fillColor       the fill color
     * @param strokeColor     the stroke color
     */
    ChartValueSeries(Context context, int absoluteMin, int absoluteMax, int[] intervalValues, int metricTitleId, int imperialTitleId, int fillColor, int strokeColor) {
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
        titlePaint.setStyle(Style.FILL_AND_STROKE);

        markerPaint = new Paint(strokePaint);
        markerPaint.setTextSize(ChartView.SMALL_TEXT_SIZE * scale);
        markerPaint.setTextAlign(Align.RIGHT);
        markerPaint.setStyle(Style.FILL_AND_STROKE);

        // Set stroke paint thickness
        strokePaint.setStrokeWidth(STROKE_WIDTH);
    }

    /**
     * Returns true if the series is enabled.
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the series enabled value.
     *
     * @param enabled true to enable
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns true if the series has data.
     */
    boolean hasData() {
        return extremityMonitor.hasData();
    }

    /**
     * Updates the series with a new {@link ChartPoint}.
     */
    void update(ChartPoint chartPoint) {
        if (isChartPointValid(chartPoint)) {
            extremityMonitor.update(extractDataFromChartPoint(chartPoint));
        }
    }

    abstract double extractDataFromChartPoint(@NonNull ChartPoint chartPoint);

    boolean isChartPointValid(@NonNull ChartPoint chartPoint) {
        return !Double.isNaN(extractDataFromChartPoint(chartPoint));
    }

    protected abstract boolean drawIfChartPointHasNoData();

    Path getPath() {
        return path;
    }

    void drawPath(Canvas canvas) {
        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    /**
     * Updates the y axis dimension.
     */
    void updateDimension() {
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
        for (int intervalValue : intervalValues) {
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
     * @param min           the min series value
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
    int getInterval() {
        return interval;
    }

    /**
     * Gets the min marker value.
     */
    int getMinMarkerValue() {
        return minMarkerValue;
    }

    /**
     * Gets the max marker value.
     */
    int getMaxMarkerValue() {
        return maxMarkerValue;
    }

    /**
     * Gets the title id.
     */
    int getTitleId(boolean metricUnits) {
        return metricUnits ? metricTitleId : imperialTitleId;
    }

    /**
     * Gets the title paint.
     */
    Paint getTitlePaint() {
        return titlePaint;
    }

    /**
     * Gets the marker paint.
     */
    Paint getMarkerPaint() {
        return markerPaint;
    }

    /**
     * Gets the largest marker.
     */
    String getLargestMarker() {
        String minMarker = numberFormat.format(getMinMarkerValue());
        String maxMarker = numberFormat.format(getMaxMarkerValue());
        return minMarker.length() >= maxMarker.length() ? minMarker : maxMarker;
    }

    /**
     * Formats a marker value.
     *
     * @param value the value
     */
    String formatMarker(int value) {
        return numberFormat.format(value);
    }
}
