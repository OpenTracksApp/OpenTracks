// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.stats;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;

import android.location.Location;

import junit.framework.TestCase;

/**
 * Tests {@link TripStatisticsUpdater}.
 * 
 * @author Sandor Dornbush
 */
public class TripStatisticsUpdaterTest extends TestCase {

  private static final long ONE_SECOND = 1000;
  private static final long TEN_SECONDS = 10 * ONE_SECOND;

  private TripStatisticsUpdater tripStatisticsUpdater = null;

  @Override
  protected void setUp() throws Exception {
    tripStatisticsUpdater = new TripStatisticsUpdater(System.currentTimeMillis());
  }

  public void testAddLocationSimple() throws Exception {
    long startTime = 1000;
    tripStatisticsUpdater = new TripStatisticsUpdater(startTime);
    TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();

    assertEquals(0.0, tripStatisticsUpdater.getSmoothedElevation());
    assertEquals(Double.POSITIVE_INFINITY, tripStatistics.getMinElevation());
    assertEquals(Double.NEGATIVE_INFINITY, tripStatistics.getMaxElevation());
    assertEquals(0.0, tripStatistics.getMaxSpeed());
    assertEquals(Double.POSITIVE_INFINITY, tripStatistics.getMinGrade());
    assertEquals(Double.NEGATIVE_INFINITY, tripStatistics.getMaxGrade());
    assertEquals(0.0, tripStatistics.getTotalElevationGain());
    assertEquals(0, tripStatistics.getMovingTime());
    assertEquals(0.0, tripStatistics.getTotalDistance());
    
    float speed = 11.1f;
    for (int i = 0; i < 100; i++) {
      Location location = new Location("test");
      location.setAccuracy(1.0f);
      location.setLongitude(45.0);

      // Going up by 1 meter each time.
      location.setAltitude(i);

      // Moving by .001 degree latitude (111 meters)
      location.setLatitude(i * .001);

      location.setSpeed(speed);

      // Each time slice is 10 seconds.
      location.setTime(startTime + i * TEN_SECONDS);
      tripStatisticsUpdater.addLocation(location, PreferencesUtils.MIN_RECORDING_DISTANCE_DEFAULT);

      tripStatistics = tripStatisticsUpdater.getTripStatistics();
      assertEquals(i * TEN_SECONDS, tripStatistics.getTotalTime());
      assertEquals(i * TEN_SECONDS, tripStatistics.getMovingTime());
      assertEquals(i, tripStatisticsUpdater.getSmoothedElevation(),
          Constants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(0.0, tripStatistics.getMinElevation());
      assertEquals(i, tripStatistics.getMaxElevation(), Constants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(i, tripStatistics.getTotalElevationGain(), Constants.ELEVATION_SMOOTHING_FACTOR);

      if (i >= Constants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(speed, tripStatistics.getMaxSpeed(), 0.1);
      }
      if (i >= Constants.DISTANCE_SMOOTHING_FACTOR && i >= Constants.ELEVATION_SMOOTHING_FACTOR) {
        // 1 m / 111 m = .009
        assertEquals(0.009, tripStatistics.getMinGrade(), 0.0001);
        assertEquals(0.009, tripStatistics.getMaxGrade(), 0.0001);
      }
      assertEquals(i * 111.0, tripStatistics.getTotalDistance(), 100);
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateElevation(double)} with constant
   * elevations.
   */
  public void testElevationSimple() throws Exception {
    for (double elevation = 0; elevation < 1000; elevation += 10) {
      tripStatisticsUpdater = new TripStatisticsUpdater(System.currentTimeMillis());
      for (int i = 0; i < 100; i++) {
        assertEquals(0.0, tripStatisticsUpdater.updateElevation(elevation));
        assertEquals(elevation, tripStatisticsUpdater.getSmoothedElevation());

        TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();
        assertEquals(elevation, tripStatistics.getMinElevation());
        assertEquals(elevation, tripStatistics.getMaxElevation());
        assertEquals(0.0, tripStatistics.getTotalElevationGain());
      }
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, double)} with
   * elevation gain.
   */
  public void testElevationGain() throws Exception {
    for (double i = 0; i < 1000; i++) {
      double expectedGain;
      if (i < Constants.ELEVATION_SMOOTHING_FACTOR - 1) {
        expectedGain = 0;
      } else if (i < Constants.ELEVATION_SMOOTHING_FACTOR) {
        expectedGain = 0.5;
      } else {
        expectedGain = 1.0;
      }
      assertEquals(expectedGain, tripStatisticsUpdater.updateElevation(i));
      assertEquals(i, tripStatisticsUpdater.getSmoothedElevation(),
          Constants.ELEVATION_SMOOTHING_FACTOR / 2);

      TripStatistics data = tripStatisticsUpdater.getTripStatistics();
      assertEquals(0.0, data.getMinElevation());
      assertEquals(i, data.getMaxElevation(), Constants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(i, data.getTotalElevationGain(), Constants.ELEVATION_SMOOTHING_FACTOR);
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, double)} with grade
   * of 1 and -1.
   */
  public void testGradeSimple() throws Exception {
    for (double i = 0; i < 1000; i++) {
      /*
       * The value of the elevation does not matter. This is just to fill the
       * elevation buffer.
       */
      tripStatisticsUpdater.updateElevation(i);
      tripStatisticsUpdater.updateGrade(100, 100);
      if (i >= Constants.GRADE_SMOOTHING_FACTOR && i >= Constants.ELEVATION_SMOOTHING_FACTOR) {
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMaxGrade());
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMinGrade());
      }
    }
    for (double i = 0; i < 1000; i++) {
      /*
       * The value of the elevation does not matter. This is just to fill the
       * elevation buffer.
       */
      tripStatisticsUpdater.updateElevation(i);
      tripStatisticsUpdater.updateGrade(100, -100);
      if (i >= Constants.GRADE_SMOOTHING_FACTOR && i >= Constants.ELEVATION_SMOOTHING_FACTOR) {
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMaxGrade());
        assertEquals(-1.0, tripStatisticsUpdater.getTripStatistics().getMinGrade());
      }
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, double)} with
   * distance of 1. The grade should get ignored.
   */
  public void testGradeIgnoreShort() throws Exception {
    for (double i = 0; i < 100; i++) {
      /*
       * The value of the elevation does not matter. This is just to fill the
       * elevation buffer.
       */
      tripStatisticsUpdater.updateElevation(i);
      tripStatisticsUpdater.updateGrade(1, 100);
      assertEquals(
          Double.NEGATIVE_INFINITY, tripStatisticsUpdater.getTripStatistics().getMaxGrade());
      assertEquals(
          Double.POSITIVE_INFINITY, tripStatisticsUpdater.getTripStatistics().getMinGrade());
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateSpeed(long, double, long, double)}
   * with speed of zero.
   */
  public void testUpdateSpeedIncludeZero() {
    for (int i = 0; i < 1000; i++) {
      tripStatisticsUpdater.updateSpeed(i + ONE_SECOND, 0.0, i, 4.0);
      assertEquals(0.0, tripStatisticsUpdater.getTripStatistics().getMaxSpeed());
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateSpeed(long, double, long, double)}
   * with the error code 128. The speed should get ignored.
   */
  public void testUpdateSpeedIngoreErrorCode() {
    long time = 12344000;
    tripStatisticsUpdater.updateSpeed(time + ONE_SECOND, 128.0, time, 0.0);
    assertEquals(0.0, tripStatisticsUpdater.getTripStatistics().getMaxSpeed());
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateSpeed(long, double, long, double)}
   * with a large speed change. The speed should get ignored.
   */
  public void testUpdateSpeedIngoreLargeAcceleration() {
    long time = 12344000;
    tripStatisticsUpdater.updateSpeed(time + ONE_SECOND, 100.0, time, 1.0);
    assertEquals(0.0, tripStatisticsUpdater.getTripStatistics().getMaxSpeed());
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateSpeed(long, double, long, double)}
   * with constant speed.
   */
  public void testUpdateSpeed() {
    double speed = 4.0;
    for (int i = 0; i < 1000; i++) {
      tripStatisticsUpdater.updateSpeed(i + ONE_SECOND, speed, i, speed);
      if (i >= Constants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(speed, tripStatisticsUpdater.getTripStatistics().getMaxSpeed());
      }
    }
  }
}
