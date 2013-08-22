// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.stats;

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
  private static final float MOVING_SPEED = 11.1f;

  private TripStatisticsUpdater tripStatisticsUpdater = null;

  @Override
  protected void setUp() throws Exception {
    tripStatisticsUpdater = new TripStatisticsUpdater(System.currentTimeMillis());
  }

  /**
   * Sends some moving and waiting locations and then checks the statistics.
   */
  public void testAddLocationSimple() {
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

    // Time:0 ~ 99; Location:0 ~ 99
    addMoveLocations(100, startTime, tripStatistics, 0, 0);
    // Time:100 ~ 199; Location:99
    addWaitLocations(100, startTime, tripStatistics, 100, 99);
    // Time:200 ~ 299; Location:100 ~ 199
    addMoveLocations(100, startTime, tripStatistics, 200, 100);
    // Time:300 ~ 399; Location:199
    addWaitLocations(100, startTime, tripStatistics, 300, 199);
    // Time:400 ~ 499; Location:200 ~ 299
    addMoveLocations(100, startTime, tripStatistics, 400, 200);
    // Time:500 ~ 599; Location:299
    addWaitLocations(100, startTime, tripStatistics, 500, 299);
    // Time:600 ~ 699; Location:300 ~ 399
    addMoveLocations(100, startTime, tripStatistics, 600, 300);
  }

  /**
   * Sends some disordered locations and checks the statistics. In some
   * situation, especially when signal is not good, MyTracks may receive such
   * data.
   */
  public void testAddLocation_disorderedLocatiions() {
    long startTime = 1000;
    tripStatisticsUpdater = new TripStatisticsUpdater(startTime);
    TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();

    addLocations(5, startTime, tripStatistics, 0, 0);
    addLocations(5, startTime, tripStatistics, 5, 0);
    addLocations(5, startTime, tripStatistics, 10, -5);
    addLocations(5, startTime, tripStatistics, 15, 5);
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateElevation(double)} with constant
   * elevations.
   */
  public void testElevationSimple() throws Exception {
    for (double elevation = 0; elevation < 1000; elevation += 10) {
      tripStatisticsUpdater = new TripStatisticsUpdater(System.currentTimeMillis());
      for (int i = 0; i < 100; i++) {
        tripStatisticsUpdater.updateElevation(elevation);
        assertEquals(elevation, tripStatisticsUpdater.getSmoothedElevation());

        if (i >= TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR) {
          TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();
          assertEquals(elevation, tripStatistics.getMinElevation());
          assertEquals(elevation, tripStatistics.getMaxElevation());
          assertEquals(0.0, tripStatistics.getTotalElevationGain());
        }
      }
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, Double)} with
   * elevation gain.
   */
  public void testElevationGain() throws Exception {
    for (double i = 0; i < 1000; i++) {
      tripStatisticsUpdater.updateElevation(i);
      assertEquals(i, tripStatisticsUpdater.getSmoothedElevation(),
          TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);

      if (i >= TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR) {
        TripStatistics data = tripStatisticsUpdater.getTripStatistics();
        assertEquals(12.0, data.getMinElevation());
        assertEquals(
            i, data.getMaxElevation(), TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);
        assertEquals(
            i, data.getTotalElevationGain(), TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR);
      }
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, Double)} with grade
   * of 1 and -1.
   */
  public void testGradeSimple() throws Exception {
    for (double i = 0; i < 1000; i++) {
      tripStatisticsUpdater.updateGrade(100, Double.valueOf(100));
      if (i >= TripStatisticsUpdater.RUN_SMOOTHING_FACTOR
          + TripStatisticsUpdater.GRADE_SMOOTHING_FACTOR) {
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMaxGrade());
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMinGrade());
      }
    }
    for (double i = 0; i < 1000; i++) {
      tripStatisticsUpdater.updateGrade(100, Double.valueOf(-100));
      if (i >= TripStatisticsUpdater.GRADE_SMOOTHING_FACTOR
          && i >= TripStatisticsUpdater.RUN_SMOOTHING_FACTOR) {
        assertEquals(1.0, tripStatisticsUpdater.getTripStatistics().getMaxGrade());
        // add 0.1 delta since changing min grade from 1 to -1
        assertEquals(-1.0, tripStatisticsUpdater.getTripStatistics().getMinGrade(), 0.1);
      }
    }
  }

  /**
   * Tests {@link TripStatisticsUpdater#updateGrade(double, Double)} with
   * distance of 1. The grade should get ignored.
   */
  public void testGradeIgnoreShort() throws Exception {
    for (double i = 0; i < 100; i++) {
      /*
       * The value of the elevation does not matter. This is just to fill the
       * elevation buffer.
       */
      tripStatisticsUpdater.updateElevation(i);
      tripStatisticsUpdater.updateGrade(1, Double.valueOf(100));
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
      if (i >= TripStatisticsUpdater.SPEED_SMOOTHING_FACTOR) {
        assertEquals(speed, tripStatisticsUpdater.getTripStatistics().getMaxSpeed());
      }
    }
  }

  /**
   * Sends some locations which keeping moving and checks the statistics.
   * 
   * @param points number of locations
   * @param startTime start time of this track
   * @param tripStatistics the TripStatistics object
   * @param timeOffset offset to start time
   * @param locationOffset location offset to start
   */
  private void addMoveLocations(int points, long startTime, TripStatistics tripStatistics,
      int timeOffset, int locationOffset) {
    for (int i = 0; i < points; i++) {
      // Going up by 1 meter each time.
      // Moving by .001 degree latitude (111 meters).
      // Each time slice is 10 seconds.
      Location location = getLocation(i + locationOffset, (i + locationOffset) * .001, MOVING_SPEED,
          startTime + (timeOffset + i) * TEN_SECONDS);
      tripStatisticsUpdater.addLocation(
          location, PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);
      tripStatistics = tripStatisticsUpdater.getTripStatistics();

      assertEquals((timeOffset + i) * TEN_SECONDS, tripStatistics.getTotalTime());
      assertEquals((locationOffset + i) * TEN_SECONDS, tripStatistics.getMovingTime());
      assertEquals(i + locationOffset, tripStatisticsUpdater.getSmoothedElevation(),
          TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);
      if (i + locationOffset >= TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR) {
        assertEquals(12.0, tripStatistics.getMinElevation());
        assertEquals(i + locationOffset, tripStatistics.getMaxElevation(),
            TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);
        assertEquals(i + locationOffset, tripStatistics.getTotalElevationGain(),
            TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR);
      }

      if (i + locationOffset >= TripStatisticsUpdater.SPEED_SMOOTHING_FACTOR) {
        assertEquals(MOVING_SPEED, tripStatistics.getMaxSpeed(), 0.1);
      }

      // If there are only moving locations in the track.
      if (locationOffset == 0 && (i + locationOffset) >= TripStatisticsUpdater.RUN_SMOOTHING_FACTOR
          + TripStatisticsUpdater.GRADE_SMOOTHING_FACTOR) {
        // 1 m / 111 m = .009
        assertEquals(0.009, tripStatistics.getMinGrade(), 0.0001);
        assertEquals(0.009, tripStatistics.getMaxGrade(), 0.0001);
      }
      assertEquals((i + locationOffset) * 111.0, tripStatistics.getTotalDistance(),
          (i + locationOffset) * 111.0 * 0.01);
    }
  }

  /**
   * Sends some locations which are not moving and checks the statistics.
   * 
   * @param points number of locations
   * @param startTime start time of this track
   * @param tripStatistics the TripStatistics object
   * @param timeOffset offset to start time
   * @param locationOffset location offset to start
   */
  private void addWaitLocations(int points, long startTime, TripStatistics tripStatistics,
      int timeOffset, int locationOffset) {
    for (int i = 0; i < points; i++) {
      Location location = getLocation(
          locationOffset, locationOffset * .001, 0, startTime + (i + timeOffset) * TEN_SECONDS);
      tripStatisticsUpdater.addLocation(
          location, PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);

      tripStatistics = tripStatisticsUpdater.getTripStatistics();
      assertEquals((i + timeOffset) * TEN_SECONDS, tripStatistics.getTotalTime());
      assertEquals((locationOffset) * TEN_SECONDS, tripStatistics.getMovingTime());
      assertEquals(locationOffset, tripStatisticsUpdater.getSmoothedElevation(),
          TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(12.0, tripStatistics.getMinElevation());
      assertEquals(locationOffset, tripStatistics.getMaxElevation(),
          TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR / 2);
      assertEquals(locationOffset, tripStatistics.getTotalElevationGain(),
          TripStatisticsUpdater.ELEVATION_SMOOTHING_FACTOR);
      if (locationOffset >= TripStatisticsUpdater.SPEED_SMOOTHING_FACTOR) {
        assertEquals(MOVING_SPEED, tripStatistics.getMaxSpeed(), 0.1);
      }
      assertEquals(MOVING_SPEED, tripStatistics.getMaxSpeed(), 0.1);
      assertEquals(
          locationOffset * 111.0, tripStatistics.getTotalDistance(), locationOffset * 111.0 * 0.01);
    }
  }

  /**
   * Sends some locations which are moving and checks some simple policy.
   * 
   * @param points number of locations
   * @param startTime start time of this track
   * @param tripStatistics the TripStatistics object
   * @param timeOffset offset to start time
   * @param locationOffset location offset to start
   */
  private void addLocations(int points, long startTime, TripStatistics tripStatistics,
      int timeOffset, int locationOffset) {
    for (int i = 0; i < points; i++) {
      // 99999 means a speed should bigger than given speed.
      Location location = getLocation(i + locationOffset, (i + locationOffset) * .001, 99999,
          startTime + (timeOffset + i) * TEN_SECONDS);
      tripStatisticsUpdater.addLocation(
          location, PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT);
      tripStatistics = tripStatisticsUpdater.getTripStatistics();

      assertTrue(tripStatistics.getMovingTime() <= tripStatistics.getTotalTime());
      assertTrue(tripStatistics.getAverageSpeed() <= tripStatistics.getAverageMovingSpeed());
      assertTrue(tripStatistics.getAverageMovingSpeed() <= tripStatistics.getMaxSpeed());
      assertTrue(tripStatistics.getStopTime() >= tripStatistics.getStartTime());
    }
  }

  /**
   * Creates a location and returns it.
   * 
   * @param altitude altitude of location
   * @param latitude latitude of location
   * @param speed speed of location
   * @param time time of location
   */
  private Location getLocation(double altitude, double latitude, float speed, long time) {
    Location location = new Location("test");
    location.setAccuracy(1.0f);
    location.setLongitude(45.0);
    location.setAltitude(altitude);
    location.setLatitude(latitude);
    location.setSpeed(speed);
    location.setTime(time);
    return location;
  }
}
