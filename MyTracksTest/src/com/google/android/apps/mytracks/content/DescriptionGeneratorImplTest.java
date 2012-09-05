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
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.test.AndroidTestCase;
import android.util.Pair;

/**
 * Tests for {@link DescriptionGeneratorImpl}.
 *
 * @author Jimmy Shih
 */
public class DescriptionGeneratorImplTest extends AndroidTestCase {

  private static final long START_TIME = 1288721514000L;
  private DescriptionGeneratorImpl descriptionGenerator;

  @Override
  protected void setUp() throws Exception {
    descriptionGenerator = new DescriptionGeneratorImpl(getContext());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#generateTrackDescription(Track,
   * java.util.Vector, java.util.Vector, boolean)}.
   */
  public void testGenerateTrackDescription() {
    Track track = new Track();
    TripStatistics stats = new TripStatistics();
    stats.setTotalDistance(20000);
    stats.setTotalTime(600000);
    stats.setMovingTime(300000);
    stats.setMaxSpeed(100);
    stats.setMaxElevation(550);
    stats.setMinElevation(-500);
    stats.setTotalElevationGain(6000);
    stats.setMaxGrade(0.42);
    stats.setMinGrade(0.11);
    stats.setStartTime(START_TIME);
    track.setTripStatistics(stats);
    track.setCategory("hiking");
    String expected = "Created by" 
      + " <a href='http://www.google.com/mobile/mytracks'>My Tracks</a> on Android.<p>" 
      + "Name: -<br>"
      + "Activity type: hiking<br>"
      + "Description: -<br>"
      + "Total distance: 20.00 km (12.4 mi)<br>"
      + "Total time: 10:00<br>"
      + "Moving time: 05:00<br>"
      + "Average speed: 120.00 km/h (74.6 mi/h)<br>"
      + "Average moving speed: 240.00 km/h (149.1 mi/h)<br>"
      + "Max speed: 360.00 km/h (223.7 mi/h)<br>"
      + "Average pace: 0.50 min/km (0.8 min/mi)<br>"
      + "Average moving pace: 0.25 min/km (0.4 min/mi)<br>"
      + "Fastest pace: 0.17 min/km (0.3 min/mi)<br>"
      + "Max elevation: 550 m (1804 ft)<br>"
      + "Min elevation: -500 m (-1640 ft)<br>"
      + "Elevation gain: 6000 m (19685 ft)<br>"
      + "Max grade: 42 %<br>"
      + "Min grade: 11 %<br>"
      + "Recorded: " + StringUtils.formatDateTime(getContext(), START_TIME) + "<br>";
    assertEquals(expected, descriptionGenerator.generateTrackDescription(track, null, null, true));
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#generateWaypointDescription(TripStatistics)}.
   */
  public void testGenerateWaypointDescription() {
    Waypoint waypoint = new Waypoint();
    TripStatistics stats = new TripStatistics();
    stats.setTotalDistance(20000);
    stats.setTotalTime(600000);
    stats.setMovingTime(300000);
    stats.setMaxSpeed(100);
    stats.setMaxElevation(550);
    stats.setMinElevation(-500);
    stats.setTotalElevationGain(6000);
    stats.setMaxGrade(0.42);
    stats.setMinGrade(0.11);
    stats.setStartTime(START_TIME);
    waypoint.setTripStatistics(stats);
    String expected = "Total distance: 20.00 km (12.4 mi)\n"
      + "Total time: 10:00\n"
      + "Moving time: 05:00\n"
      + "Average speed: 120.00 km/h (74.6 mi/h)\n"
      + "Average moving speed: 240.00 km/h (149.1 mi/h)\n"
      + "Max speed: 360.00 km/h (223.7 mi/h)\n"
      + "Average pace: 0.50 min/km (0.8 min/mi)\n"
      + "Average moving pace: 0.25 min/km (0.4 min/mi)\n"
      + "Fastest pace: 0.17 min/km (0.3 min/mi)\n"
      + "Max elevation: 550 m (1804 ft)\n"
      + "Min elevation: -500 m (-1640 ft)\n"
      + "Elevation gain: 6000 m (19685 ft)\n"
      + "Max grade: 42 %\n"
      + "Min grade: 11 %\n"
      + "Recorded: " + StringUtils.formatDateTime(getContext(), START_TIME) + "\n";
    assertEquals(
        expected, descriptionGenerator.generateWaypointDescription(waypoint.getTripStatistics()));
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeDistance(double, StringBuilder,
   * int, String)}.
   */
  public void testWriteDistance() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeDistance(1100, builder, R.string.description_total_distance, "<br>");
    assertEquals("Total distance: 1.10 km (0.7 mi)<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeTime(long, StringBuilder, int,
   * String)}.
   */
  public void testWriteTime() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeTime(1000, builder, R.string.description_total_time, "<br>");
    assertEquals("Total time: 00:01<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeSpeed(double, StringBuilder,
   * int, String)}.
   */
  public void testWriteSpeed() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeSpeed(1.1, builder, R.string.description_average_speed, "\n");
    assertEquals("Average speed: 3.96 km/h (2.5 mi/h)\n", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeElevation(double, StringBuilder,
   * int, String)}.
   */
  public void testWriteElevation() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeElevation(4.2, builder, R.string.description_min_elevation, "<br>");
    assertEquals("Min elevation: 4 m (14 ft)<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writePace(Pair, StringBuilder, int,
   * String)}.
   */
  public void testWritePace() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writePace(
        new Pair<Double, Double>(1.1, 2.2), builder, R.string.description_average_pace, "\n");
    assertEquals("Average pace: 54.55 min/km (27.3 min/mi)\n", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeGrade(double, StringBuilder,
   * int, String)}.
   */
  public void testWriteGrade() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeGrade(.042, builder, R.string.description_max_grade, "<br>");
    assertEquals("Max grade: 4 %<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeGrade(double, StringBuilder,
   * int, String)} with a NaN.
   */
  public void testWriteGrade_nan() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeGrade(Double.NaN, builder, R.string.description_max_grade, "<br>");
    assertEquals("Max grade: 0 %<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#writeGrade(double, StringBuilder,
   * int, String)} with an infinite number.
   */
  public void testWriteGrade_infinite() {
    StringBuilder builder = new StringBuilder();
    descriptionGenerator.writeGrade(
        Double.POSITIVE_INFINITY, builder, R.string.description_max_grade, "<br>");
    assertEquals("Max grade: 0 %<br>", builder.toString());
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#getPace(double)}.
   */
  public void testGetPace() {
    assertEquals(12.0, descriptionGenerator.getPace(5));
  }

  /**
   * Tests {@link DescriptionGeneratorImpl#getPace(double)} with zero speed.
   */
  public void testGetPace_zero() {
    assertEquals(0.0, descriptionGenerator.getPace(0));
  }
}
