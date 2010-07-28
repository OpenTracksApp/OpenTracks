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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.DescriptionGenerator;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.Vector;

/**
 * Various string manipulation methods.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils implements DescriptionGenerator {

  private final Context context;

  /**
   * Formats a number of milliseconds as a string.
   *
   * @param time - A period of time in milliseconds.
   * @return A string of the format M:SS, MM:SS or HH:MM:SS
   */
  public static String formatTime(long time) {
    return formatTimeInternal(time, false);
  }

  /**
   * Formats a number of milliseconds as a string. To be used when we need the
   * hours to be shown even when it is zero, e.g. exporting data to a
   * spreadsheet.
   *
   * @param time - A period of time in milliseconds
   * @return A string of the format HH:MM:SS even if time is less than 1 hour
   */
  public static String formatTimeAlwaysShowingHours(long time) {
    return formatTimeInternal(time, true);
  }

  /**
   * Formats the given text as a CDATA element to be used in a XML file. This
   * includes adding the starting and ending CDATA tags. Please notice that this
   * may result in multiple consecutive CDATA tags.
   *
   * @param unescaped the unescaped text to be formatted
   * @return the formatted text, inside one or more CDATA tags
   */
  public static String stringAsCData(String unescaped) {
    // "]]>" needs to be broken into multiple CDATA segments, like:
    // "Foo]]>Bar" becomes "<![CDATA[Foo]]]]><![CDATA[>Bar]]>"
    // (the end of the first CDATA has the "]]", the other has ">")
    String escaped = unescaped.replaceAll("]]>", "]]]]><![CDATA[>");
    return "<![CDATA[" + escaped + "]]>";
  }

  /**
   * Formats a number of milliseconds as a string.
   *
   * @param time - A period of time in milliseconds
   * @param alwaysShowHours - Whether to display 00 hours if time is less than 1
   *        hour
   * @return A string of the format HH:MM:SS
   */
  private static String formatTimeInternal(long time, boolean alwaysShowHours) {
    int[] parts = getTimeParts(time);
    StringBuilder builder = new StringBuilder();
    if (parts[2] > 0 || alwaysShowHours) {
      builder.append(parts[2]);
      builder.append(':');
      if (parts[1] <= 9) {
        builder.append("0");
      }
    }

    builder.append(parts[1]);
    builder.append(':');
    if (parts[0] <= 9) {
      builder.append("0");
    }
    builder.append(parts[0]);

    return builder.toString();
  }

  /**
   * Gets the time as an array of parts.
   */
  public static int[] getTimeParts(long time) {
    if (time < 0) {
      int[] parts = getTimeParts(time * -1);
      parts[0] *= -1;
      parts[1] *= -1;
      parts[2] *= -1;
      return parts;
    }
    int[] parts = new int[3];

    long seconds = time / 1000;
    parts[0] = (int) (seconds % 60);
    int tmp = (int) (seconds / 60);
    parts[1] = tmp % 60;
    parts[2] = tmp / 60;

    return parts;
  }

  public StringUtils(Context context) {
    this.context = context;
  }

  public String formatTimeLong(long time) {
    int[] parts = getTimeParts(time);
    String secLabel =
        context.getString(parts[0] == 1 ? R.string.second : R.string.seconds);
    String minLabel =
        context.getString(parts[1] == 1 ? R.string.minute : R.string.minutes);
    String hourLabel =
        context.getString(parts[2] == 1 ? R.string.hour : R.string.hours);

    StringBuilder sb = new StringBuilder();
    if (parts[2] != 0) {
      sb.append(parts[2]);
      sb.append(" ");
      sb.append(hourLabel);
      sb.append(" ");
      sb.append(parts[1]);
      sb.append(minLabel);
    } else {
      sb.append(parts[1]);
      sb.append(" ");
      sb.append(minLabel);
      sb.append(" ");
      sb.append(parts[0]);
      sb.append(secLabel);
    }
    return sb.toString();
  }

  /**
   * Generates a description for a track (with information about the
   * statistics).
   *
   * @param track the track
   * @return a track description
   */
  public String generateTrackDescription(Track track, Vector<Double> distances,
      Vector<Double> elevations) {
    boolean displaySpeed = true;
    SharedPreferences preferences =
        context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      displaySpeed =
          preferences.getBoolean(MyTracksSettings.REPORT_SPEED, true);
    }

    TripStatistics trackStats = track.getStatistics();
    final double distanceInKm = trackStats.getTotalDistance() / 1000;
    final double distanceInMiles = distanceInKm * UnitConversions.KM_TO_MI;
    final long minElevationInMeters = Math.round(trackStats.getMinElevation());
    final long minElevationInFeet =
        Math.round(trackStats.getMinElevation() * UnitConversions.M_TO_FT);
    final long maxElevationInMeters = Math.round(trackStats.getMaxElevation());
    final long maxElevationInFeet =
        Math.round(trackStats.getMaxElevation() * UnitConversions.M_TO_FT);
    final long elevationGainInMeters =
        Math.round(trackStats.getTotalElevationGain());
    final long elevationGainInFeet = Math.round(
        trackStats.getTotalElevationGain() * UnitConversions.M_TO_FT);

    long minGrade = 0;
    long maxGrade = 0;
    double trackMaxGrade = trackStats.getMaxGrade();
    double trackMinGrade = trackStats.getMinGrade();
    if (!Double.isNaN(trackMaxGrade)
        && !Double.isInfinite(trackMaxGrade)) {
      maxGrade = Math.round(trackMaxGrade * 100);
    }
    if (!Double.isNaN(trackMinGrade) && !Double.isInfinite(trackMinGrade)) {
      minGrade = Math.round(trackMinGrade * 100);
    }

    String category = context.getString(R.string.unknown);
    String trackCategory = track.getCategory();
    if (trackCategory != null && trackCategory.length() > 0) {
      category = trackCategory;
    }

    String averageSpeed =
        getSpeedString(trackStats.getAverageSpeed(),
            R.string.average_speed_label,
            R.string.average_pace_label,
            displaySpeed);

    String averageMovingSpeed =
        getSpeedString(trackStats.getAverageMovingSpeed(),
            R.string.average_moving_speed_label,
            R.string.average_moving_pace_label,
            displaySpeed);

    String maxSpeed =
        getSpeedString(trackStats.getMaxSpeed(),
            R.string.max_speed_label,
            R.string.min_pace_label,
            displaySpeed);

    return String.format("%s<p>"
        + "%s: %.2f %s (%.1f %s)<br>"
        + "%s: %s<br>"
        + "%s: %s<br>"
        + "%s %s %s"
        + "%s: %d %s (%d %s)<br>"
        + "%s: %d %s (%d %s)<br>"
        + "%s: %d %s (%d %s)<br>"
        + "%s: %d %%<br>"
        + "%s: %d %%<br>"
        + "%s: %tc<br>"
        + "%s: %s<br>"
        + "<img border=\"0\" src=\"%s\"/>",

        // Line 1
        context.getString(R.string.my_map_link),

        // Line 2
        context.getString(R.string.total_distance_label),
        distanceInKm, context.getString(R.string.kilometer),
        distanceInMiles, context.getString(R.string.mile),

        // Line 3
        context.getString(R.string.total_time_label),
        StringUtils.formatTime(trackStats.getTotalTime()),

        // Line 4
        context.getString(R.string.moving_time_label),
        StringUtils.formatTime(trackStats.getMovingTime()),

        // Line 5
        averageSpeed, averageMovingSpeed, maxSpeed,

        // Line 6
        context.getString(R.string.min_elevation_label),
        minElevationInMeters, context.getString(R.string.meter),
        minElevationInFeet, context.getString(R.string.feet),

        // Line 7
        context.getString(R.string.max_elevation_label),
        maxElevationInMeters, context.getString(R.string.meter),
        maxElevationInFeet, context.getString(R.string.feet),

        // Line 8
        context.getString(R.string.elevation_gain_label),
        elevationGainInMeters, context.getString(R.string.meter),
        elevationGainInFeet, context.getString(R.string.feet),

        // Line 9
        context.getString(R.string.max_grade_label), maxGrade,

        // Line 10
        context.getString(R.string.min_grade_label), minGrade,

        // Line 11
        context.getString(R.string.recorded_date),
        new Date(trackStats.getStartTime()),

        // Line 12
        context.getString(R.string.category), category,

        // Line 13
        ChartURLGenerator.getChartUrl(distances, elevations, track, context));
  }

  private String getSpeedString(double speed, int speedLabel, int paceLabel,
      boolean displaySpeed) {
    double speedInKph = speed * 3.6;
    double speedInMph = speedInKph * UnitConversions.KMH_TO_MPH;
    if (displaySpeed) {
      return String.format("%s: %.2f %s (%.1f %s)<br>",
          context.getString(speedLabel),
          speedInKph, context.getString(R.string.kilometer_per_hour),
          speedInMph, context.getString(R.string.mile_per_hour));
    } else {
      double paceInKm;
      double paceInMi;
      if (speed == 0) {
        paceInKm = 0.0;
        paceInMi = 0.0;
      } else {
        paceInKm = 60.0 / speedInKph;
        paceInMi = 60.0 / speedInMph;
      }
      return String.format("%s: %.2f %s (%.1f %s)<br>",
          context.getString(paceLabel),
          paceInKm, context.getString(R.string.min_per_kilometer),
          paceInMi, context.getString(R.string.min_per_mile));
    }
  }

  /**
   * Generates a description for a waypoint (with information about the
   * statistics).
   *
   * @return a track description
   */
  public String generateWaypointDescription(Waypoint waypoint) {
    TripStatistics stats = waypoint.getStatistics();

    final double distanceInKm = stats.getTotalDistance() / 1000;
    final double distanceInMiles = distanceInKm * UnitConversions.KM_TO_MI;
    final double averageSpeedInKmh = stats.getAverageSpeed() * 3.6;
    final double averageSpeedInMph =
        averageSpeedInKmh * UnitConversions.KMH_TO_MPH;
    final double movingSpeedInKmh = stats.getAverageMovingSpeed() * 3.6;
    final double movingSpeedInMph =
        movingSpeedInKmh * UnitConversions.KMH_TO_MPH;
    final double maxSpeedInKmh = stats.getMaxSpeed() * 3.6;
    final double maxSpeedInMph = maxSpeedInKmh * UnitConversions.KMH_TO_MPH;
    final long minElevationInMeters = Math.round(stats.getMinElevation());
    final long minElevationInFeet =
        Math.round(stats.getMinElevation() * UnitConversions.M_TO_FT);
    final long maxElevationInMeters = Math.round(stats.getMaxElevation());
    final long maxElevationInFeet =
        Math.round(stats.getMaxElevation() * UnitConversions.M_TO_FT);
    final long elevationGainInMeters =
        Math.round(stats.getTotalElevationGain());
    final long elevationGainInFeet = Math.round(
        stats.getTotalElevationGain() * UnitConversions.M_TO_FT);
    long theMinGrade = 0;
    long theMaxGrade = 0;
    double maxGrade = stats.getMaxGrade();
    double minGrade = stats.getMinGrade();
    if (!Double.isNaN(maxGrade) &&
        !Double.isInfinite(maxGrade)) {
      theMaxGrade = Math.round(maxGrade * 100);
    }
    if (!Double.isNaN(minGrade) &&
        !Double.isInfinite(minGrade)) {
      theMinGrade = Math.round(minGrade * 100);
    }
    final String percent = "%";

    return String.format(
        "%s: %.2f %s (%.1f %s)\n"
        + "%s: %s\n"
        + "%s: %s\n"
        + "%s: %.2f %s (%.1f %s)\n"
        + "%s: %.2f %s (%.1f %s)\n"
        + "%s: %.2f %s (%.1f %s)\n"
        + "%s: %d %s (%d %s)\n"
        + "%s: %d %s (%d %s)\n"
        + "%s: %d %s (%d %s)\n"
        + "%s: %d %s\n"
        + "%s: %d %s\n",
        context.getString(R.string.distance_label),
            distanceInKm, context.getString(R.string.kilometer),
            distanceInMiles, context.getString(R.string.mile),
        context.getString(R.string.time_label),
            StringUtils.formatTime(stats.getTotalTime()),
        context.getString(R.string.moving_time_label),
            StringUtils.formatTime(stats.getMovingTime()),
        context.getString(R.string.average_speed_label),
            averageSpeedInKmh, context.getString(R.string.kilometer_per_hour),
            averageSpeedInMph, context.getString(R.string.mile_per_hour),
        context.getString(R.string.average_moving_speed_label),
            movingSpeedInKmh, context.getString(R.string.kilometer_per_hour),
            movingSpeedInMph, context.getString(R.string.mile_per_hour),
        context.getString(R.string.max_speed_label),
            maxSpeedInKmh, context.getString(R.string.kilometer_per_hour),
            maxSpeedInMph, context.getString(R.string.mile_per_hour),
        context.getString(R.string.min_elevation_label),
            minElevationInMeters, context.getString(R.string.meter),
            minElevationInFeet, context.getString(R.string.feet),
        context.getString(R.string.max_elevation_label),
            maxElevationInMeters, context.getString(R.string.meter),
            maxElevationInFeet, context.getString(R.string.feet),
        context.getString(R.string.elevation_gain_label),
            elevationGainInMeters, context.getString(R.string.meter),
            elevationGainInFeet, context.getString(R.string.feet),
        context.getString(R.string.max_grade_label),
            theMaxGrade, percent,
        context.getString(R.string.min_grade_label),
            theMinGrade, percent);
  }
}
