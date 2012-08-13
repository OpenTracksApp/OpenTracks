// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.stats;

import com.google.android.apps.mytracks.Constants;

import android.location.Location;

import junit.framework.TestCase;

/**
 * Test the the function of the TripStatisticsBuilder class.
 * 
 * @author Sandor Dornbush
 */
public class TripStatisticsBuilderTest extends TestCase {

  private TripStatisticsBuilder builder = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    builder = new TripStatisticsBuilder(System.currentTimeMillis());
  }

  public void testAddLocationSimple() throws Exception {
    builder = new TripStatisticsBuilder(1000);
    TripStatistics tripStatistics = builder.getTripStatistics();

    assertEquals(0.0, builder.getSmoothedElevation());
    assertEquals(Double.POSITIVE_INFINITY, tripStatistics.getMinElevation());
    assertEquals(Double.NEGATIVE_INFINITY, tripStatistics.getMaxElevation());
    assertEquals(0.0, tripStatistics.getMaxSpeed());
    assertEquals(Double.POSITIVE_INFINITY, tripStatistics.getMinGrade());
    assertEquals(Double.NEGATIVE_INFINITY, tripStatistics.getMaxGrade());
    assertEquals(0.0, tripStatistics.getTotalElevationGain());
    assertEquals(0, tripStatistics.getMovingTime());
    assertEquals(0.0, tripStatistics.getTotalDistance());
    Location lastLocation = null;
    for (int i = 0; i < 100; i++) {
      Location location = new Location("test");
      location.setAccuracy(1.0f);
      location.setLongitude(45.0);

      // Going up by 1 meter each time.
      location.setAltitude(i);
      location.setLatitude(i * .001); // Moving by .001 degree latitude (111 meters)
      location.setSpeed(11.1f);
      // Each time slice is 10 seconds.
      long time = 1000 + 10000 * i;
      location.setTime(time);
      builder.addLocation(location, lastLocation);

      tripStatistics = builder.getTripStatistics();
      assertEquals(10000 * i, tripStatistics.getTotalTime());
      assertEquals(10000 * i, tripStatistics.getMovingTime());
      assertEquals(i, builder.getSmoothedElevation(), Constants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(0.0, tripStatistics.getMinElevation());
      assertEquals(i, tripStatistics.getMaxElevation(), Constants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(i, tripStatistics.getTotalElevationGain(), Constants.ELEVATION_SMOOTHING_FACTOR);

      if (i > Constants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(11.1f, tripStatistics.getMaxSpeed(), 0.1);
      }
      if (i > Constants.DISTANCE_SMOOTHING_FACTOR && i > Constants.ELEVATION_SMOOTHING_FACTOR) {
        // 1 m / 111 m = .009
        assertEquals(0.009, tripStatistics.getMinGrade(), 0.0001);
        assertEquals(0.009, tripStatistics.getMaxGrade(), 0.0001);
      }
      assertEquals(111.0 * i, tripStatistics.getTotalDistance(), 100);
      lastLocation = location;
    }
  }

  /**
   * Test that elevation works if the user is stable.
   */
  public void testElevationSimple() throws Exception {
    for (double elevation = 0; elevation < 1000; elevation += 10) {
      builder = new TripStatisticsBuilder(System.currentTimeMillis());
      for (int j = 0; j < 100; j++) {
        assertEquals(0.0, builder.updateElevation(elevation));
        assertEquals(elevation, builder.getSmoothedElevation());
        TripStatistics data = builder.getTripStatistics();
        assertEquals(elevation, data.getMinElevation());
        assertEquals(elevation, data.getMaxElevation());
        assertEquals(0.0, data.getTotalElevationGain());
      }
    }
  }

  public void testElevationGain() throws Exception {
    for (double i = 0; i < 1000; i++) {
      double expectedGain;
      if (i < (Constants.ELEVATION_SMOOTHING_FACTOR - 1)) {
        expectedGain = 0;
      } else if (i < Constants.ELEVATION_SMOOTHING_FACTOR) {
        expectedGain = 0.5;
      } else {
        expectedGain = 1.0;
      }
      assertEquals(expectedGain, builder.updateElevation(i));
      assertEquals(i, builder.getSmoothedElevation(), 20);
      TripStatistics data = builder.getTripStatistics();
      assertEquals(0.0, data.getMinElevation(), 0.0);
      assertEquals(i, data.getMaxElevation(), Constants.ELEVATION_SMOOTHING_FACTOR);
      assertEquals(i, data.getTotalElevationGain(), Constants.ELEVATION_SMOOTHING_FACTOR);
    }
  }

  public void testGradeSimple() throws Exception {
    for (double i = 0; i < 1000; i++) {
      // The value of the elevation does not matter. This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(100, 100);
      if ((i > Constants.GRADE_SMOOTHING_FACTOR) && (i > Constants.ELEVATION_SMOOTHING_FACTOR)) {
        assertEquals(1.0, builder.getTripStatistics().getMaxGrade());
        assertEquals(1.0, builder.getTripStatistics().getMinGrade());
      }
    }
    for (double i = 0; i < 1000; i++) {
      // The value of the elevation does not matter. This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(100, -100);
      if ((i > Constants.GRADE_SMOOTHING_FACTOR) && (i > Constants.ELEVATION_SMOOTHING_FACTOR)) {
        assertEquals(1.0, builder.getTripStatistics().getMaxGrade());
        assertEquals(-1.0, builder.getTripStatistics().getMinGrade());
      }
    }
  }

  public void testGradeIgnoreShort() throws Exception {
    for (double i = 0; i < 100; i++) {
      // The value of the elevation does not matter. This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(1, 100);
      assertEquals(Double.NEGATIVE_INFINITY, builder.getTripStatistics().getMaxGrade());
      assertEquals(Double.POSITIVE_INFINITY, builder.getTripStatistics().getMinGrade());
    }
  }

  public void testUpdateSpeedIncludeZero() {
    for (int i = 0; i < 1000; i++) {
      builder.updateSpeed(i + 1000, 0.0, i, 4.0);
      assertEquals(0.0, builder.getTripStatistics().getMaxSpeed());
    }
  }

  public void testUpdateSpeedIngoreErrorCode() {
    builder.updateSpeed(12345000, 128.0, 12344000, 0.0);
    assertEquals(0.0, builder.getTripStatistics().getMaxSpeed());
  }

  public void testUpdateSpeedIngoreLargeAcceleration() {
    builder.updateSpeed(12345000, 100.0, 12344000, 1.0);
    assertEquals(0.0, builder.getTripStatistics().getMaxSpeed());
  }

  public void testUpdateSpeed() {
    for (int i = 0; i < 1000; i++) {
      builder.updateSpeed(i + 1000, 4.0, i, 4.0);
      if (i > Constants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(4.0, builder.getTripStatistics().getMaxSpeed());
      }
    }
  }
}
