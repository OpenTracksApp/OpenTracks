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
    StringBuffer buffer = new StringBuffer();

    // Created by
    String url = context.getString(R.string.my_tracks_web_url);
    buffer.append(context.getString(
        R.string.send_google_by_my_tracks, "<a href='http://" + url + "'>", "</a>"));
    buffer.append("<p>");

    buffer.append(generateTripStatisticsDescription(track.getStatistics(), true));

    // Activity type
    String trackCategory = track.getCategory();
    String category = trackCategory != null && trackCategory.length() > 0 ? trackCategory
        : context.getString(R.string.value_unknown);
    buffer.append(context.getString(R.string.description_activity_type, category));
    buffer.append(HTML_LINE_BREAK);

    // Elevation chart
    if (distances != null && elevations != null) {
      buffer.append("<img border=\"0\" src=\""
          + ChartURLGenerator.getChartUrl(distances, elevations, track, context) + "\"/>");
    }
    return buffer.toString();
  }

  @Override
  public String generateWaypointDescription(Waypoint waypoint) {
    return generateTripStatisticsDescription(waypoint.getStatistics(), false);
  }

  /**
   * Generates a description for a {@link TripStatistics}.
   *
   * @param stats the trip statistics
   * @param html true to use <br> for line break instead of \n
   */
  private String generateTripStatisticsDescription(TripStatistics stats, boolean html) {
    String lineBreak = html ? HTML_LINE_BREAK : TEXT_LINE_BREAK;
    StringBuffer buffer = new StringBuffer();

    // Total distance
    writeDistance(stats.getTotalDistance(), buffer, R.string.description_total_distance);
    buffer.append(lineBreak);

    // Total time
    writeTime(stats.getTotalTime(), buffer, R.string.description_total_time);
    buffer.append(lineBreak);

    // Moving time
    writeTime(stats.getMovingTime(), buffer, R.string.description_moving_time);
    buffer.append(lineBreak);

    // Average speed
    Pair<Double, Double> averageSpeed = writeSpeed(
        stats.getAverageSpeed(), buffer, R.string.description_average_speed);
    buffer.append(lineBreak);

    // Average moving speed
    Pair<Double, Double> averageMovingSpeed = writeSpeed(
        stats.getAverageMovingSpeed(), buffer, R.string.description_average_moving_speed);
    buffer.append(lineBreak);

    // Max speed
    Pair<Double, Double> maxSpeed = writeSpeed(
        stats.getMaxSpeed(), buffer, R.string.description_max_speed);
    buffer.append(lineBreak);

    // Average pace
    writePace(averageSpeed, buffer, R.string.description_average_pace);
    buffer.append(lineBreak);

    // Average moving pace
    writePace(averageMovingSpeed, buffer, R.string.description_average_moving_pace);
    buffer.append(lineBreak);

    // Min pace
    writePace(maxSpeed, buffer, R.string.description_min_pace);
    buffer.append(lineBreak);

    // Max elevation
    writeElevation(stats.getMaxElevation(), buffer, R.string.description_max_elevation);
    buffer.append(lineBreak);

    // Min elevation
    writeElevation(stats.getMinElevation(), buffer, R.string.description_min_elevation);
    buffer.append(lineBreak);

    // Elevation gain
    writeElevation(stats.getTotalElevationGain(), buffer, R.string.description_elevation_gain);
    buffer.append(lineBreak);

    // Max grade
    writeGrade(stats.getMaxGrade(), buffer, R.string.description_max_grade);
    buffer.append(lineBreak);

    // Min grade
    writeGrade(stats.getMinGrade(), buffer, R.string.description_min_grade);
    buffer.append(lineBreak);

    // Recorded time
    buffer.append(
        context.getString(R.string.description_recorded_time,
            StringUtils.formatDateTime(context, stats.getStartTime())));
    buffer.append(lineBreak);

    return buffer.toString();
  }

  /**
   * Writes distance.
   *
   * @param distance distance in meters
   * @param buffer buffer to write distance
   * @param resId resource id of distance string
   */
  private void writeDistance(double distance, StringBuffer buffer, int resId) {
    double distanceInKm = distance * UnitConversions.M_TO_KM;
    double distanceInMi = distanceInKm * UnitConversions.KM_TO_MI;
    buffer.append(context.getString(resId, distanceInKm, distanceInMi));
  }

  /**
   * Writes time.
   *
   * @param time time in milliseconds.
   * @param buffer buffer to write time
   * @param resId resource id of time string
   */
  private void writeTime(long time, StringBuffer buffer, int resId) {
    buffer.append(context.getString(resId, StringUtils.formatElapsedTime(time)));
  }

  /**
   * Writes speed.
   *
   * @param speed speed in meters per second
   * @param buffer buffer to write speed
   * @param resId resource id of speed string
   *@return a pair of speed, first in kilometers per hour, second in miles per
   *         hour.
   */
  private Pair<Double, Double> writeSpeed(double speed, StringBuffer buffer, int resId) {
    double speedInKmHr = speed * UnitConversions.MS_TO_KMH;
    double speedInMiHr = speedInKmHr * UnitConversions.KM_TO_MI;
    buffer.append(context.getString(resId, speedInKmHr, speedInMiHr));
    return Pair.create(speedInKmHr, speedInMiHr);
  }

  /**
   * Writes pace.
   *
   * @param speed a pair of speed, first in kilometers per hour, second in miles
   *          per hour
   * @param buffer buffer to write pace
   * @param resId resource id of pace string
   */
  private void writePace(Pair<Double, Double> speed, StringBuffer buffer, int resId) {
    double paceInMinKm = getPace(speed.first);
    double paceInMinMi = getPace(speed.second);
    buffer.append(context.getString(resId, paceInMinKm, paceInMinMi));
  }

  /**
   * Writes elevation.
   *
   * @param elevation elevation in meters
   * @param buffer buffer to write elevation
   * @param resId resource id of elevation string
   */
  private void writeElevation(double elevation, StringBuffer buffer, int resId) {
    long elevationInM = Math.round(elevation);
    long elevationInFt = Math.round(elevation * UnitConversions.M_TO_FT);
    buffer.append(context.getString(resId, elevationInM, elevationInFt));
  }

  /**
   * Writes grade.
   *
   * @param grade grade in fraction
   * @param buffer buffer to write grade
   * @param resId resource id grade string
   */
  private void writeGrade(double grade, StringBuffer buffer, int resId) {
    long gradeInPercent = Double.isNaN(grade) || Double.isInfinite(grade) 
        ? 0L : Math.round(grade * 100);
    buffer.append(context.getString(resId, gradeInPercent));
  }

  /**
   * Gets pace (in minutes) from speed.
   *
   * @param speed speed in hours
   */
  private double getPace(double speed) {
    return speed == 0 ? 0.0 : 60.0 / speed; // convert from hours to minutes
  }
}
