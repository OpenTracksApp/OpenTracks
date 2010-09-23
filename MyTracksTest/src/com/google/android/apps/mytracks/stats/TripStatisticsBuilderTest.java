// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.stats;

import com.google.android.apps.mytracks.MyTracksConstants;

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
    builder = new TripStatisticsBuilder();
    builder.resume();
  }

  public void testAddLocationSimple() throws Exception {
    builder = new TripStatisticsBuilder();
    builder.resumeAt(1000);
    TripStatistics stats = builder.getStatistics();

    assertEquals(0.0, builder.getSmoothedElevation());
    assertEquals(Double.POSITIVE_INFINITY, stats.getMinElevation());
    assertEquals(Double.NEGATIVE_INFINITY, stats.getMaxElevation());
    assertEquals(0.0, stats.getMaxSpeed());
    assertEquals(Double.POSITIVE_INFINITY, stats.getMinGrade());
    assertEquals(Double.NEGATIVE_INFINITY, stats.getMaxGrade());
    assertEquals(0.0, stats.getTotalElevationGain());
    assertEquals(0, stats.getMovingTime());
    assertEquals(0.0, stats.getTotalDistance());
    for (int i = 0; i < 100; i++) {
      Location l = new Location("test");
      l.setAccuracy(1.0f);
      l.setLongitude(45.0);

      // Going up by 5 meters each time.
      l.setAltitude(i);
      // Moving by .1% of a degree latitude.
      l.setLatitude(i * .001);
      l.setSpeed(11.1f);
      // Each time slice is 10 seconds.
      long time = 1000 + 10000 * i;
      l.setTime(time);
      boolean moving = builder.addLocation(l, time);
      assertEquals((i != 0), moving);

      stats = builder.getStatistics();
      assertEquals(10000 * i, stats.getTotalTime());
      assertEquals(10000 * i, stats.getMovingTime());
      assertEquals(i, builder.getSmoothedElevation(),
                   MyTracksConstants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(0.0, stats.getMinElevation());
      assertEquals(i, stats.getMaxElevation(),
                   MyTracksConstants.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(i, stats.getTotalElevationGain(),
                   MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);

      if (i > MyTracksConstants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(11.1f, stats.getMaxSpeed(), 0.1);
      }
      if ((i > MyTracksConstants.GRADE_SMOOTHING_FACTOR)
          && (i > MyTracksConstants.ELEVATION_SMOOTHING_FACTOR)) {
        assertEquals(0.009, stats.getMinGrade(), 0.0001);
        assertEquals(0.009, stats.getMaxGrade(), 0.0001);
      }
      // 1 degree = 111 km
      // 1 timeslice = 0.001 degree = 111 m
      assertEquals(111.0 * i, stats.getTotalDistance(), 100);
    }
  }

  /**
   * Test that elevation works if the user is stable.
   */
  public void testElevationSimple() throws Exception {
    for (double elevation = 0; elevation < 1000; elevation += 10) {
      builder = new TripStatisticsBuilder();
      builder.resume();
      for (int j = 0; j < 100; j++) {
        assertEquals(0.0, builder.updateElevation(elevation));
        assertEquals(elevation, builder.getSmoothedElevation());
        TripStatistics data = builder.getStatistics();
        assertEquals(elevation, data.getMinElevation());
        assertEquals(elevation, data.getMaxElevation());
        assertEquals(0.0, data.getTotalElevationGain());
      }
    }
  }

  public void testElevationGain() throws Exception {
    for (double i = 0; i < 1000; i++) {
      double expectedGain;
      if (i < (MyTracksConstants.ELEVATION_SMOOTHING_FACTOR - 1)) {
        expectedGain = 0;
      } else if (i < MyTracksConstants.ELEVATION_SMOOTHING_FACTOR) {
        expectedGain = 0.5;
      } else {
        expectedGain = 1.0;
      }
      assertEquals(expectedGain,
                   builder.updateElevation(i));
      assertEquals(i, builder.getSmoothedElevation(), 20);
      TripStatistics data = builder.getStatistics();
      assertEquals(0.0, data.getMinElevation(), 0.0);
      assertEquals(i, data.getMaxElevation(),
                   MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);
      assertEquals(i, data.getTotalElevationGain(),
                   MyTracksConstants.ELEVATION_SMOOTHING_FACTOR);
    }
  }

  public void testGradeSimple() throws Exception {
    for (double i = 0; i < 1000; i++) {
      // The value of the elevation does not matter.  This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(100, 100);
      if ((i > MyTracksConstants.GRADE_SMOOTHING_FACTOR)
          && (i > MyTracksConstants.ELEVATION_SMOOTHING_FACTOR)) {
        assertEquals(1.0, builder.getStatistics().getMaxGrade());
        assertEquals(1.0, builder.getStatistics().getMinGrade());
      }
    }
    for (double i = 0; i < 1000; i++) {
      // The value of the elevation does not matter.  This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(100, -100);
      if ((i > MyTracksConstants.GRADE_SMOOTHING_FACTOR)
          && (i > MyTracksConstants.ELEVATION_SMOOTHING_FACTOR)) {
        assertEquals(1.0, builder.getStatistics().getMaxGrade());
        assertEquals(-1.0, builder.getStatistics().getMinGrade());
      }
    }
  }

  public void testGradeIgnoreShort() throws Exception {
    for (double i = 0; i < 100; i++) {
      // The value of the elevation does not matter.  This is just to fill the
      // buffer.
      builder.updateElevation(i);
      builder.updateGrade(1, 100);
      assertEquals(Double.NEGATIVE_INFINITY, builder.getStatistics().getMaxGrade());
      assertEquals(Double.POSITIVE_INFINITY, builder.getStatistics().getMinGrade());
    }
  }

  public void testUpdateSpeedIncludeZero() {
    for (int i = 0; i < 1000; i++) {
      builder.updateSpeed(i + 1000, 0.0, i, 4.0);
      assertEquals(0.0, builder.getStatistics().getMaxSpeed());
      assertEquals((i + 1) * 1000, builder.getStatistics().getMovingTime());
    }
  }

  public void testUpdateSpeedIngoreErrorCode() {
    builder.updateSpeed(12345000, 128.0, 12344000, 0.0);
    assertEquals(0.0, builder.getStatistics().getMaxSpeed());
    assertEquals(1000, builder.getStatistics().getMovingTime());
  }

  public void testUpdateSpeedIngoreLargeAcceleration() {
    builder.updateSpeed(12345000, 100.0, 12344000, 1.0);
    assertEquals(0.0, builder.getStatistics().getMaxSpeed());
    assertEquals(1000, builder.getStatistics().getMovingTime());
  }

  public void testUpdateSpeed() {
    for (int i = 0; i < 1000; i++) {
      builder.updateSpeed(i + 1000, 4.0, i, 4.0);
      assertEquals((i + 1) * 1000, builder.getStatistics().getMovingTime());
      if (i > MyTracksConstants.SPEED_SMOOTHING_FACTOR) {
        assertEquals(4.0, builder.getStatistics().getMaxSpeed());
      }
    }
  }
}
