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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;

import java.util.Vector;

/**
 * This class will generate google chart server url's.
 * 
 * @author Sandor Dornbush
 */
public class ChartURLGenerator {

  private static final String CHARTS_BASE_URL = "http://chart.apis.google.com/chart?";

  private ChartURLGenerator() {}

  /**
   * Gets a chart of a track.
   * 
   * @param distances An array of distance measurements
   * @param elevations A matching array of elevation measurements
   * @param track The track for this chart
   * @param context The current appplication context
   */
  public static String getChartUrl(
      Vector<Double> distances, Vector<Double> elevations, Track track, Context context) {
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);
    return getChartUrl(
        distances, elevations, track, context.getString(R.string.stats_elevation), metricUnits);
  }

  /**
   * Gets a chart of a track. This form is for testing without contexts.
   * 
   * @param distances An array of distance measurements
   * @param elevations A matching array of elevation measurements
   * @param track The track for this chart
   * @param title The title for the chart
   * @param metricUnits Should the data be displayed in metric units
   */
  @VisibleForTesting
  static String getChartUrl(Vector<Double> distances, Vector<Double> elevations, Track track,
      String title, boolean metricUnits) {
    if (distances == null || elevations == null || track == null) {
      return null;
    }

    if (distances.size() != elevations.size()) {
      return null;
    }

    // Round it up.
    TripStatistics tripStatistics = track.getTripStatistics();
    double effectiveMaxY = tripStatistics.getMaxElevation();
    if (!metricUnits) {
      effectiveMaxY *= UnitConversions.M_TO_FT;
    }
    effectiveMaxY = ((int) (effectiveMaxY / 100)) * 100 + 100;

    // Round it down.
    double effectiveMinY = tripStatistics.getMinElevation();
    if (!metricUnits) {
      effectiveMinY *= UnitConversions.M_TO_FT;
    }
    effectiveMinY = ((int) (effectiveMinY / 100)) * 100;
    if (tripStatistics.getMinElevation() < 0) {
      effectiveMinY -= 100;
    }

    double ySpread = effectiveMaxY - effectiveMinY;

    StringBuilder sb = new StringBuilder(CHARTS_BASE_URL);
    sb.append("&chs=600x350");
    sb.append("&cht=lxy");

    // Title
    sb.append("&chtt=");
    sb.append(title);

    // Labels
    sb.append("&chxt=x,y");
    double totalDistance = tripStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
    if (!metricUnits) {
      totalDistance *= UnitConversions.KM_TO_MI;
    }
    int xInterval = ((int) (totalDistance / 6));
    int yInterval = ((int) (ySpread / 600)) * 100;
    if (yInterval < 100) {
      yInterval = 25;
    }
    // Range
    sb.append("&chxr=0,0,");
    sb.append((int) totalDistance);
    sb.append(',');
    sb.append(xInterval);

    sb.append("|1,");
    sb.append(effectiveMinY);
    sb.append(',');
    sb.append(effectiveMaxY);
    sb.append(',');
    sb.append(yInterval);

    // Line color
    sb.append("&chco=009A00");

    // Fill
    sb.append("&chm=B,00AA00,0,0,0");

    // Grid lines
    double desiredGrids = ySpread / yInterval;
    sb.append("&chg=100000,");
    sb.append(100.0 / desiredGrids);
    sb.append(",1,0");

    // Data
    sb.append("&chd=e:");
    for (int i = 0; i < distances.size(); i++) {
      int normalized = (int) (getNormalizedDistance(distances.elementAt(i), track) * 4095);
      sb.append(ChartsExtendedEncoder.getEncodedValue(normalized));
    }

    sb.append(ChartsExtendedEncoder.getSeparator());
    for (int i = 0; i < elevations.size(); i++) {
      double value = elevations.elementAt(i);
      if (!metricUnits) {
        value *= UnitConversions.M_TO_FT;
      }
      int normalized = (int) (getNormalizedElevation(value, effectiveMinY, ySpread) * 4095);
      sb.append(ChartsExtendedEncoder.getEncodedValue(normalized));
    }
    return sb.toString();
  }

  private static double getNormalizedDistance(double value, Track track) {
    return value / track.getTripStatistics().getTotalDistance();
  }

  private static double getNormalizedElevation(double value, double effectiveMinY, double ySpread) {
    return (value - effectiveMinY) / ySpread;
  }
}
