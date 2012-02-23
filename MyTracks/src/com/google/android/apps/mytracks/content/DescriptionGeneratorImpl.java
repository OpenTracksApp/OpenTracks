/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ChartURLGenerator;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.util.Pair;

import java.util.Vector;

/**
 * An implementation of {@link DescriptionGenerator} for My Tracks.
 *
 * @author Jimmy Shih
 */
public class DescriptionGeneratorImpl implements DescriptionGenerator {

  private static final String HTML_LINE_BREAK = "<br>";
  private static final String TEXT_LINE_BREAK = "\n";

  private Context context;

  public DescriptionGeneratorImpl(Context context) {
    this.context = context;
  }

  @Override
  public String generateTrackDescription(
      Track track, Vector<Double> distances, Vector<Double> elevations) {
    StringBuilder builder = new StringBuilder();

    // Created by
    String url = context.getString(R.string.my_tracks_web_url);
    builder.append(context.getString(
        R.string.send_google_by_my_tracks, "<a href='http://" + url + "'>", "</a>"));
    builder.append("<p>");

    builder.append(generateTripStatisticsDescription(track.getStatistics(), true));

    // Activity type
    String trackCategory = track.getCategory();
    String category = trackCategory != null && trackCategory.length() > 0 ? trackCategory
        : context.getString(R.string.value_unknown);
    builder.append(context.getString(R.string.description_activity_type, category));
    builder.append(HTML_LINE_BREAK);

    // Elevation chart
    if (distances != null && elevations != null) {
      builder.append("<img border=\"0\" src=\""
          + ChartURLGenerator.getChartUrl(distances, elevations, track, context) + "\"/>");
      builder.append(HTML_LINE_BREAK);
    }
    return builder.toString();
  }

  @Override
  public String generateWaypointDescription(Waypoint waypoint) {
    return generateTripStatisticsDescription(waypoint.getStatistics(), false);
  }

  /**
   * Generates a description for a {@link TripStatistics}.
   *
   * @param stats the trip statistics
   * @param html true to use "<br>" for line break instead of "\n"
   */
  private String generateTripStatisticsDescription(TripStatistics stats, boolean html) {
    String lineBreak = html ? HTML_LINE_BREAK : TEXT_LINE_BREAK;
    StringBuilder builder = new StringBuilder();

    // Total distance
    writeDistance(
        stats.getTotalDistance(), builder, R.string.description_total_distance, lineBreak);

    // Total time
    writeTime(stats.getTotalTime(), builder, R.string.description_total_time, lineBreak);

    // Moving time
    writeTime(stats.getMovingTime(), builder, R.string.description_moving_time, lineBreak);

    // Average speed
    Pair<Double, Double> averageSpeed = writeSpeed(
        stats.getAverageSpeed(), builder, R.string.description_average_speed, lineBreak);

    // Average moving speed
    Pair<Double, Double> averageMovingSpeed = writeSpeed(stats.getAverageMovingSpeed(), builder,
        R.string.description_average_moving_speed, lineBreak);

    // Max speed
    Pair<Double, Double> maxSpeed = writeSpeed(
        stats.getMaxSpeed(), builder, R.string.description_max_speed, lineBreak);

    // Average pace
    writePace(averageSpeed, builder, R.string.description_average_pace, lineBreak);

    // Average moving pace
    writePace(averageMovingSpeed, builder, R.string.description_average_moving_pace, lineBreak);

    // Min pace
    writePace(maxSpeed, builder, R.string.description_min_pace, lineBreak);

    // Max elevation
    writeElevation(stats.getMaxElevation(), builder, R.string.description_max_elevation, lineBreak);

    // Min elevation
    writeElevation(stats.getMinElevation(), builder, R.string.description_min_elevation, lineBreak);

    // Elevation gain
    writeElevation(
        stats.getTotalElevationGain(), builder, R.string.description_elevation_gain, lineBreak);

    // Max grade
    writeGrade(stats.getMaxGrade(), builder, R.string.description_max_grade, lineBreak);

    // Min grade
    writeGrade(stats.getMinGrade(), builder, R.string.description_min_grade, lineBreak);

    // Recorded time
    builder.append(
        context.getString(R.string.description_recorded_time,
            StringUtils.formatDateTime(context, stats.getStartTime())));
    builder.append(lineBreak);

    return builder.toString();
  }

  /**
   * Writes distance.
   *
   * @param distance distance in meters
   * @param builder StringBuilder to append distance
   * @param resId resource id of distance string
   * @param lineBreak line break string
   */
  @VisibleForTesting
  void writeDistance(double distance, StringBuilder builder, int resId, String lineBreak) {
    double distanceInKm = distance * UnitConversions.M_TO_KM;
    double distanceInMi = distanceInKm * UnitConversions.KM_TO_MI;
    builder.append(context.getString(resId, distanceInKm, distanceInMi));
    builder.append(lineBreak);
  }

  /**
   * Writes time.
   *
   * @param time time in milliseconds.
   * @param builder StringBuilder to append time
   * @param resId resource id of time string
   * @param lineBreak line break string
   */
  @VisibleForTesting
  void writeTime(long time, StringBuilder builder, int resId, String lineBreak) {
    builder.append(context.getString(resId, StringUtils.formatElapsedTime(time)));
    builder.append(lineBreak);
  }

  /**
   * Writes speed.
   *
   * @param speed speed in meters per second
   * @param builder StringBuilder to append speed
   * @param resId resource id of speed string
   * @param lineBreak line break string
   * @return a pair of speed, first in kilometers per hour, second in miles per
   *         hour.
   */
  @VisibleForTesting
  Pair<Double, Double> writeSpeed(
      double speed, StringBuilder builder, int resId, String lineBreak) {
    double speedInKmHr = speed * UnitConversions.MS_TO_KMH;
    double speedInMiHr = speedInKmHr * UnitConversions.KM_TO_MI;
    builder.append(context.getString(resId, speedInKmHr, speedInMiHr));
    builder.append(lineBreak);
    return Pair.create(speedInKmHr, speedInMiHr);
  }

  /**
   * Writes pace.
   *
   * @param speed a pair of speed, first in kilometers per hour, second in miles
   *          per hour
   * @param builder StringBuilder to append pace
   * @param resId resource id of pace string
   * @param lineBreak line break string
   */
  @VisibleForTesting
  void writePace(
      Pair<Double, Double> speed, StringBuilder builder, int resId, String lineBreak) {
    double paceInMinKm = getPace(speed.first);
    double paceInMinMi = getPace(speed.second);
    builder.append(context.getString(resId, paceInMinKm, paceInMinMi));
    builder.append(lineBreak);
  }

  /**
   * Writes elevation.
   *
   * @param elevation elevation in meters
   * @param builder StringBuilder to append elevation
   * @param resId resource id of elevation string
   * @param lineBreak line break string
   */
  @VisibleForTesting
  void writeElevation(
      double elevation, StringBuilder builder, int resId, String lineBreak) {
    long elevationInM = Math.round(elevation);
    long elevationInFt = Math.round(elevation * UnitConversions.M_TO_FT);
    builder.append(context.getString(resId, elevationInM, elevationInFt));
    builder.append(lineBreak);
  }

  /**
   * Writes grade.
   *
   * @param grade grade in fraction
   * @param builder StringBuilder to append grade
   * @param resId resource id grade string
   * @param lineBreak line break string
   */
  @VisibleForTesting
  void writeGrade(double grade, StringBuilder builder, int resId, String lineBreak) {
    long gradeInPercent = Double.isNaN(grade) || Double.isInfinite(grade) ? 0L
        : Math.round(grade * 100);
    builder.append(context.getString(resId, gradeInPercent));
    builder.append(lineBreak);
  }

  /**
   * Gets pace (in minutes) from speed.
   *
   * @param speed speed in hours
   */
  @VisibleForTesting
  double getPace(double speed) {
    return speed == 0 ? 0.0 : 60.0 / speed; // convert from hours to minutes
  }
}
