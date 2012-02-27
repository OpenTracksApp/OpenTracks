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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.jayway.android.robotium.solo.Solo;

import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ZoomControls;

/**
 * Tests {@link ChartActivity}.
 * 
 * @author Youtao Liu
 */
public class ChartActivityTest extends ActivityInstrumentationTestCase2<ChartActivity> {

  private Instrumentation instrumentation;
  private ChartActivity chartActivity;
  private Solo solo;
  private View zoomIn;
  private View zoomOut;
  private int currentZoomLevel;

  private final String LOCATION_PROVIDER = "gps";
  private final double INITIAL_LONGTITUDE = 22;
  private final double INITIAL_LATITUDE = 22;
  private final double INITIAL_ALTITUDE = 22;
  private final float INITIAL_ACCURACY = 5;
  private final float INITIAL_SPEED = 10;
  private final float INITIAL_BEARING = 3.0f;
  // 10 is same with the default value in ChartView
  private final int MAX_ZOOM_LEVEL = 10;
  private final int MIN_ZOOM_LEVEL = 1;
  // The ratio from meter/second to kilometer/hour, the conversion is 60 * 60 /
  // 1000 = 3.6.
  private final double METER_PER_SECOND_TO_KILOMETER_PER_HOUR = 3.6;
  private final double KILOMETER_TO_METER = 1000.0;
  private final double HOURS_PER_UNIT = 60;

  public ChartActivityTest() {
    super(ChartActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    chartActivity = getActivity();
  }

  /**
   * Tests {@link ChartActivity#zoomIn()} and {@link ChartActivity#zoomOut()}.
   */
  public void testZoomInAndZoomOut() {
    currentZoomLevel = chartActivity.getChartView().getZoomLevel();
    chartActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        ZoomControls zoomControls = (ZoomControls) chartActivity.findViewById(R.id.elevation_zoom);
        zoomIn = zoomControls.getChildAt(0);
        zoomOut = zoomControls.getChildAt(1);
        // Invoke following two methods method to initial the display of
        // ZoomControls.
        zoomControls.setIsZoomInEnabled(chartActivity.getChartView().canZoomIn());
        zoomControls.setIsZoomOutEnabled(chartActivity.getChartView().canZoomOut());
        // Click zoomIn button to disable.
        for (int i = currentZoomLevel; i > MIN_ZOOM_LEVEL; i--) {
          zoomIn.performClick();
        }
      }
    });
    instrumentation.waitForIdleSync();
    assertEquals(false, zoomIn.isEnabled());
    assertEquals(true, zoomOut.isEnabled());
    chartActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Click to the second max zoom level
        for (int i = MIN_ZOOM_LEVEL; i < MAX_ZOOM_LEVEL - 1; i++) {
          zoomOut.performClick();
          assertEquals(true, zoomIn.isEnabled());
          assertEquals(true, zoomOut.isEnabled());
        }
        zoomOut.performClick();
      }
    });
    instrumentation.waitForIdleSync();
    assertEquals(true, zoomIn.isEnabled());
    assertEquals(false, zoomOut.isEnabled());
  }

  /**
   * There are two parts in this test. Tests
   * {@link ChartActivity#OnCreateDialog()} which includes the logic to create
   * {@link ChartSettingsDialog}.
   */
  public void testCreateSettingDialog() {
    solo = new Solo(instrumentation, chartActivity);
    // Part1, tests {@link ChartActivity#onCreateOptionsMenu()}. Check if
    // optional menu is created.
    assertNull(chartActivity.getChartSettingsMenuItem());
    sendKeys(KeyEvent.KEYCODE_MENU);
    assertNotNull(chartActivity.getChartSettingsMenuItem());

    // Part2, tests {@link ChartActivity#onOptionsItemSelected()}. Clicks on the
    // "Chart settings", and then verify that the dialog contains the
    // "By distance" text.
    solo.clickOnText(chartActivity.getString(R.string.menu_chart_view_chart_settings));
    instrumentation.waitForIdleSync();
    assertTrue(solo.searchText(chartActivity.getString(R.string.chart_settings_by_distance)));
  }

  /**
   * Tests the logic to get the incorrect values of sensor in {@link
   * ChartActivity#fillDataPoint(Location location, double result[])}.
   */
  public void testFillDataPoint_sensorIncorrect() {
    MyTracksLocation myTracksLocation = getMyTracksLocation();
    // No input.
    double[] point = fillDataPointTestHelper(myTracksLocation, false);
    assertEquals(Double.NaN, point[3]);
    assertEquals(Double.NaN, point[4]);
    assertEquals(Double.NaN, point[5]);

    // Input incorrect state.
    // Creates SensorData.
    Sensor.SensorData.Builder powerData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.NONE);
    Sensor.SensorData.Builder cadenceData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.NONE);
    Sensor.SensorData.Builder heartRateData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.NONE);
    // Creates SensorDataSet.
    SensorDataSet sensorDataSet = myTracksLocation.getSensorDataSet();
    sensorDataSet = sensorDataSet.toBuilder().setPower(powerData).setCadence(cadenceData)
        .setHeartRate(heartRateData).build();
    myTracksLocation.setSensorData(sensorDataSet);
    // Test.
    point = fillDataPointTestHelper(myTracksLocation, false);
    assertEquals(Double.NaN, point[3]);
    assertEquals(Double.NaN, point[4]);
    assertEquals(Double.NaN, point[5]);
  }

  /**
   * Tests the logic to get the correct values of sensor in {@link
   * ChartActivity#fillDataPoint(Location location, double result[])}.
   */
  public void testFillDataPoint_sensorCorrect() {
    MyTracksLocation myTracksLocation = getMyTracksLocation();
    // No input.
    double[] point = fillDataPointTestHelper(myTracksLocation, false);
    assertEquals(Double.NaN, point[3]);
    assertEquals(Double.NaN, point[4]);
    assertEquals(Double.NaN, point[5]);

    // Creates SensorData.
    Sensor.SensorData.Builder powerData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.SENDING);
    Sensor.SensorData.Builder cadenceData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.SENDING);
    Sensor.SensorData.Builder heartRateData = Sensor.SensorData.newBuilder().setValue(20)
        .setState(Sensor.SensorState.SENDING);
    // Creates SensorDataSet.
    SensorDataSet sensorDataSet = myTracksLocation.getSensorDataSet();
    sensorDataSet = sensorDataSet.toBuilder().setPower(powerData).setCadence(cadenceData)
        .setHeartRate(heartRateData).build();
    myTracksLocation.setSensorData(sensorDataSet);
    // Test.
    point = fillDataPointTestHelper(myTracksLocation, false);
    assertEquals(20.0, point[3]);
    assertEquals(20.0, point[4]);
    assertEquals(20.0, point[5]);
  }

  /**
   * Tests the logic to get the value of metric Distance in
   * {@link #fillDataPoint}.
   */
  public void testFillDataPoint_distanceMetric() {
    // By distance.
    chartActivity.getChartView().setMode(ChartView.Mode.BY_DISTANCE);
    // Resets last location and writes first location.
    MyTracksLocation myTracksLocation1 = getMyTracksLocation();
    double[] point = fillDataPointTestHelper(myTracksLocation1, true);
    assertEquals(0.0, point[0]);

    // The second is a same location, just different time.
    MyTracksLocation myTracksLocation2 = getMyTracksLocation();
    point = fillDataPointTestHelper(myTracksLocation2, false);
    assertEquals(0.0, point[0]);

    // The third location is a new location, and use metric.
    MyTracksLocation myTracksLocation3 = getMyTracksLocation();
    myTracksLocation3.setLatitude(23);
    point = fillDataPointTestHelper(myTracksLocation3, false);
    // Computes the distance between Latitude 22 and 23.
    float[] results = new float[4];
    Location.distanceBetween(myTracksLocation2.getLatitude(), myTracksLocation2.getLongitude(),
        myTracksLocation3.getLatitude(), myTracksLocation3.getLongitude(), results);
    double distance1 = results[0];
    assertEquals(distance1 / KILOMETER_TO_METER, point[0]);

    // The fourth location is a new location, and use metric.
    MyTracksLocation myTracksLocation4 = getMyTracksLocation();
    myTracksLocation4.setLatitude(24);
    point = fillDataPointTestHelper(myTracksLocation4, false);
    // Computes the distance between Latitude 23 and 24.

    Location.distanceBetween(myTracksLocation3.getLatitude(), myTracksLocation3.getLongitude(),
        myTracksLocation4.getLatitude(), myTracksLocation4.getLongitude(), results);
    double distance2 = results[0];
    assertEquals((distance1 + distance2) / KILOMETER_TO_METER, point[0]);
  }

  /**
   * Tests the logic to get the value of imperial Distance in
   * {@link #fillDataPoint}.
   */
  public void testFillDataPoint_distanceImperial() {
    // Setups to use imperial.
    chartActivity.onUnitsChanged(false);

    // The first is a same location, just different time.
    MyTracksLocation myTracksLocation1 = getMyTracksLocation();
    double[] point = fillDataPointTestHelper(myTracksLocation1, true);
    assertEquals(0.0, point[0]);

    // The second location is a new location, and use imperial.
    MyTracksLocation myTracksLocation2 = getMyTracksLocation();
    myTracksLocation2.setLatitude(23);
    point = fillDataPointTestHelper(myTracksLocation2, false);
    /*
     * Computes the distance between Latitude 22 and 23. And for we set using
     * imperial, the distance should be multiplied by UnitConversions.KM_TO_MI.
     */
    float[] results = new float[4];
    Location.distanceBetween(myTracksLocation1.getLatitude(), myTracksLocation1.getLongitude(),
        myTracksLocation2.getLatitude(), myTracksLocation2.getLongitude(), results);
    double distance1 = results[0] * UnitConversions.KM_TO_MI;
    assertEquals(distance1 / KILOMETER_TO_METER, point[0]);

    // The third location is a new location, and use imperial.
    MyTracksLocation myTracksLocation3 = getMyTracksLocation();
    myTracksLocation3.setLatitude(24);
    point = fillDataPointTestHelper(myTracksLocation3, false);
    /*
     * Computes the distance between Latitude 23 and 24. And for we set using
     * imperial, the distance should be multiplied by UnitConversions.KM_TO_MI.
     */

    Location.distanceBetween(myTracksLocation2.getLatitude(), myTracksLocation2.getLongitude(),
        myTracksLocation3.getLatitude(), myTracksLocation3.getLongitude(), results);
    double distance2 = results[0] * UnitConversions.KM_TO_MI;
    assertEquals((distance1 + distance2) / KILOMETER_TO_METER, point[0]);
  }

  /**
   * Tests the logic to get the values of time in {@link #fillDataPoint}.
   */
  public void testFillDataPoint_time() {
    // By time
    chartActivity.getChartView().setMode(ChartView.Mode.BY_TIME);
    MyTracksLocation myTracksLocation1 = getMyTracksLocation();
    double[] point = fillDataPointTestHelper(myTracksLocation1, true);
    assertEquals(0.0, point[0]);
    long timeSpan = 222;
    MyTracksLocation myTracksLocation2 = getMyTracksLocation();
    myTracksLocation2.setTime(myTracksLocation1.getTime() + timeSpan);
    point = fillDataPointTestHelper(myTracksLocation2, false);
    assertEquals((double) timeSpan, point[0]);
  }

  /**
   * Tests the logic to get the value of elevation in
   * {@link ChartActivity#fillDataPoint} by one and two points.
   */
  public void testFillDataPoint_elevation() {
    MyTracksLocation myTracksLocation1 = getMyTracksLocation();
    /*
     * At first, clear old points of elevation, so give true to the second
     * parameter. Then only one value INITIALLONGTITUDE in buffer.
     */
    double[] point = fillDataPointTestHelper(myTracksLocation1, true);
    assertEquals(INITIAL_ALTITUDE, point[1]);
    /*
     * Send another value to buffer, now there are two values, INITIALALTITUDE
     * and INITIALALTITUDE * 2.
     */
    MyTracksLocation myTracksLocation2 = getMyTracksLocation();
    myTracksLocation2.setAltitude(INITIAL_ALTITUDE * 2);
    point = fillDataPointTestHelper(myTracksLocation2, false);
    assertEquals((INITIAL_ALTITUDE + INITIAL_ALTITUDE * 2) / 2.0, point[1]);
  }

  /**
   * Tests the logic to get the value of speed in
   * {@link ChartActivity#fillDataPoint}. In this test, firstly remove all
   * points in memory, and then fill in two points one by one. The speed values
   * of these points are 129, 130.
   */
  public void testFillDataPoint_speed() {
    // Set max speed to make the speed of points are valid.
    chartActivity.setTrackMaxSpeed(200.0);
    /*
     * At first, clear old points of speed, so give true to the second
     * parameter. It will not be filled in to the speed buffer.
     */
    MyTracksLocation myTracksLocation1 = getMyTracksLocation();
    myTracksLocation1.setSpeed(129);
    double[] point = fillDataPointTestHelper(myTracksLocation1, true);
    assertEquals(0.0, point[2]);

    /*
     * Tests the logic when both metricUnits and reportSpeed are true.This
     * location will be filled into speed buffer.
     */
    MyTracksLocation myTracksLocation2 = getMyTracksLocation();
    // Add a time span here to make sure the second point is valid, the value
    // 222 here is doesn't matter.
    myTracksLocation2.setTime(myTracksLocation1.getTime() + 222);
    myTracksLocation2.setSpeed(130);
    point = fillDataPointTestHelper(myTracksLocation2, false);
    assertEquals(130.0 * METER_PER_SECOND_TO_KILOMETER_PER_HOUR, point[2]);
  }

  /**
   * Tests the logic to compute speed when use Imperial.
   */
  public void testFillDataPoint_speedImperial() {
    // Setups to use imperial.
    chartActivity.onUnitsChanged(false);
    MyTracksLocation myTracksLocation = getMyTracksLocation();
    myTracksLocation.setSpeed(132);
    double[] point = fillDataPointTestHelper(myTracksLocation, true);
    assertEquals(132.0 * METER_PER_SECOND_TO_KILOMETER_PER_HOUR * UnitConversions.KM_TO_MI,
        point[2]);
  }

  /**
   * Tests the logic to get pace value when reportSpeed is false.
   */
  public void testFillDataPoint_pace_nonZeroSpeed() {
    // Setups reportSpeed to false.
    chartActivity.onReportSpeedChanged(false);
    MyTracksLocation myTracksLocation = getMyTracksLocation();
    myTracksLocation.setSpeed(134);
    double[] point = fillDataPointTestHelper(myTracksLocation, true);
    assertEquals(HOURS_PER_UNIT / (134.0 * METER_PER_SECOND_TO_KILOMETER_PER_HOUR), point[2]);

  }

  /**
   * Tests the logic to get pace value when reportSpeed is false and average
   * speed is zero.
   */
  public void testFillDataPoint_pace_zeroSpeed() {
    // Setups reportSpeed to false.
    chartActivity.onReportSpeedChanged(false);
    MyTracksLocation myTracksLocation = getMyTracksLocation();
    myTracksLocation.setSpeed(0);
    double[] point = fillDataPointTestHelper(myTracksLocation, true);
    assertEquals(Double.NaN, point[2]);
  }

  /**
   * Simulates a MyTracksLocation for test.
   * 
   * @return a simulated location.
   */
  private MyTracksLocation getMyTracksLocation() {
    // Initial Location
    Location loc = new Location(LOCATION_PROVIDER);
    loc.setLongitude(INITIAL_LONGTITUDE);
    loc.setLatitude(INITIAL_LATITUDE);
    loc.setAltitude(INITIAL_ALTITUDE);
    loc.setAccuracy(INITIAL_ACCURACY);
    loc.setSpeed(INITIAL_SPEED);
    loc.setTime(System.currentTimeMillis());
    loc.setBearing(INITIAL_BEARING);
    SensorDataSet sd = SensorDataSet.newBuilder().build();
    MyTracksLocation myTracksLocation = new MyTracksLocation(loc, sd);

    return myTracksLocation;
  }

  /**
   * Helper method to test fillDataPoint.
   * 
   * @param location location to fill
   * @param operation a flag to do some operations
   * @return data of this location
   */
  private double[] fillDataPointTestHelper(Location location, boolean isNeedClear) {
    if (isNeedClear) {
      chartActivity.clearTrackPoints();
    }
    double[] point = new double[6];
    chartActivity.fillDataPoint(location, point);
    return point;
  }

}
