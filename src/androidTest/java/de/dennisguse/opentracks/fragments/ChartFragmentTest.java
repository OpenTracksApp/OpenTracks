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

package de.dennisguse.opentracks.fragments;

import android.location.Location;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.TrackStubUtils;
import de.dennisguse.opentracks.chart.ChartView;
import de.dennisguse.opentracks.content.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Tests {@link ChartFragment}.
 *
 * @author Youtao Liu
 */
@RunWith(AndroidJUnit4.class)
public class ChartFragmentTest {

    private static final double HOURS_PER_UNIT = 60.0;

    private ChartFragment chartFragment;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    @Before
    public void setUp() {
        boolean chartByDistance = false;
        chartFragment = new ChartFragment(chartByDistance) {
        };
        chartFragment.setChartView(new ChartView(ApplicationProvider.getApplicationContext(), chartByDistance));
        chartFragment.setTripStatisticsUpdater(TrackStubUtils.INITIAL_TIME);
    }

    /**
     * Tests the logic to get the incorrect values of sensor in {@link ChartFragment#fillDataPoint(Location, double[])}.
     */
    @Test
    public void testFillDataPoint_sensorIncorrect() {
        TrackPoint sensorDataSetLocation = TrackStubUtils.createSensorDataSetLocation();

        // No input.
        double[] point = fillDataPointTestHelper(sensorDataSetLocation);
        Assert.assertEquals(Float.NaN, point[ChartView.HEART_RATE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.CADENCE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.POWER_SERIES + 1], 0.01);

        // Input incorrect state.
        // Creates SensorData.
        SensorDataSet sensorDataSet = new SensorDataSet(SensorDataSet.DATA_UNAVAILABLE, SensorDataSet.DATA_UNAVAILABLE);
        sensorDataSetLocation.setSensorDataSet(sensorDataSet);
        // Test.
        point = fillDataPointTestHelper(sensorDataSetLocation);
        Assert.assertEquals(Float.NaN, point[ChartView.HEART_RATE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.CADENCE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.POWER_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to get the correct values of sensor in {@link ChartFragment#fillDataPoint(Location, double[])}.
     */
    @Test
    public void testFillDataPoint_sensorCorrect() {
        TrackPoint sensorDataSetLocation = TrackStubUtils.createSensorDataSetLocation();
        // No input.
        double[] point = fillDataPointTestHelper(sensorDataSetLocation);
        Assert.assertEquals(Float.NaN, point[ChartView.HEART_RATE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.CADENCE_SERIES + 1], 0.01);
        Assert.assertEquals(Float.NaN, point[ChartView.POWER_SERIES + 1], 0.01);

        // Creates SensorData.
        SensorDataSet sensorDataSet = new SensorDataSet(100, 101, 102);

        // Creates SensorDataSet.
        sensorDataSetLocation.setSensorDataSet(sensorDataSet);
        // Test.
        point = fillDataPointTestHelper(sensorDataSetLocation);
        Assert.assertEquals(100.0, point[ChartView.HEART_RATE_SERIES + 1], 0.01);
        Assert.assertEquals(101.0, point[ChartView.CADENCE_SERIES + 1], 0.01);
        Assert.assertEquals(102.0, point[ChartView.POWER_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to get the value of metric Distance in {@link ChartFragment#fillDataPoint(Location, double[])}.
     */
    @Test
    public void testFillDataPoint_distanceMetric() {
        // By distance.
        chartFragment.setChartByDistance(true);
        // Resets last location and writes first location.
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[0], 0.01);

        // The second is a same location, just different time.
        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals(0.0, point[0], 0.01);

        // The third location is a new location, and use metric.
        TrackPoint sensorDataSetLocation3 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation3.setLatitude(23);
        point = fillDataPointTestHelper(sensorDataSetLocation3);

        // Computes the distance between Latitude 22 and 23.
        float[] results = new float[4];
        Location.distanceBetween(sensorDataSetLocation2.getLatitude(), sensorDataSetLocation2.getLongitude(),
                sensorDataSetLocation3.getLatitude(), sensorDataSetLocation3.getLongitude(), results);
        double distance1 = results[0] * UnitConversions.M_TO_KM;
        Assert.assertEquals(distance1, point[0], 0.01);

        // The fourth location is a new location, and use metric.
        TrackPoint sensorDataSetLocation4 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation4.setLatitude(24);
        point = fillDataPointTestHelper(sensorDataSetLocation4);

        // Computes the distance between Latitude 23 and 24.
        Location.distanceBetween(sensorDataSetLocation3.getLatitude(), sensorDataSetLocation3.getLongitude(),
                sensorDataSetLocation4.getLatitude(), sensorDataSetLocation4.getLongitude(), results);
        double distance2 = results[0] * UnitConversions.M_TO_KM;
        Assert.assertEquals((distance1 + distance2), point[0], 0.01);
    }

    /**
     * Tests the logic to get the value of imperial Distance in {@link ChartFragment#fillDataPoint(Location, double[])}.
     */
    @Test
    public void testFillDataPoint_distanceImperial() {
        // By distance.
        chartFragment.setChartByDistance(true);
        // Setups to use imperial.
        chartFragment.setMetricUnits(false);

        // The first is a same location, just different time.
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[0], 0.01);

        // The second location is a new location, and use imperial.
        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation2.setLatitude(23);
        point = fillDataPointTestHelper(sensorDataSetLocation2);

        /*
         * Computes the distance between Latitude 22 and 23.
         * And for we set using * imperial, the distance should be multiplied by UnitConversions.KM_TO_MI.
         */
        float[] results = new float[4];
        Location.distanceBetween(sensorDataSetLocation1.getLatitude(), sensorDataSetLocation1.getLongitude(), sensorDataSetLocation2.getLatitude(), sensorDataSetLocation2.getLongitude(), results);
        double distance1 = results[0] * UnitConversions.M_TO_KM * UnitConversions.KM_TO_MI;
        Assert.assertEquals(distance1, point[0], 0.01);

        // The third location is a new location, and use imperial.
        TrackPoint sensorDataSetLocation3 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation3.setLatitude(24);
        point = fillDataPointTestHelper(sensorDataSetLocation3);

        /*
         * Computes the distance between Latitude 23 and 24.
         * And for we set using * imperial, the distance should be multiplied by UnitConversions.KM_TO_MI.
         */
        Location.distanceBetween(sensorDataSetLocation2.getLatitude(), sensorDataSetLocation2.getLongitude(), sensorDataSetLocation3.getLatitude(), sensorDataSetLocation3.getLongitude(), results);
        double distance2 = results[0] * UnitConversions.M_TO_KM * UnitConversions.KM_TO_MI;
        Assert.assertEquals(distance1 + distance2, point[0], 0.01);
    }

    /**
     * Tests the logic to get the values of time in {@link ChartFragment#fillDataPoint(Location, double[])}.
     */
    @Test
    public void testFillDataPoint_time() {
        // By time
        chartFragment.setChartByDistance(false);
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[0], 0.01);
        long timeSpan = 222;
        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation2.setTime(sensorDataSetLocation1.getTime() + timeSpan);
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals((double) timeSpan, point[0], 0.01);
    }

    /**
     * Tests the logic to get the value of elevation in {@link ChartFragment#fillDataPoint(Location, double[])} by one and two points.
     */
    @Test
    public void testFillDataPoint_elevation() {
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();

        /*
         * At first, clear old points of elevation, so give true to the second parameter.
         * Then only one value INITIAL_ALTITUDE in buffer.
         */
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(TrackStubUtils.INITIAL_ALTITUDE, point[ChartView.ELEVATION_SERIES + 1], 0.01);

        /*
         * Send another value to buffer, now there are two values, INITIAL_ALTITUDE and INITIAL_ALTITUDE * 2.
         */
        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation2.setAltitude(TrackStubUtils.INITIAL_ALTITUDE * 2);
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals((TrackStubUtils.INITIAL_ALTITUDE + TrackStubUtils.INITIAL_ALTITUDE * 2) / 2.0,
                point[ChartView.ELEVATION_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to get the value of speed in {@link ChartFragment#fillDataPoint(Location, double[])}.
     * In this test, firstly remove all points in memory, and then fill in two points one by one.
     * The speed values of these points are 129, 130.
     */
    @Test
    public void testFillDataPoint_speed() {
        /*
         * At first, clear old points of speed, so give true to the second parameter.
         * It will not be filled in to the speed buffer.
         */
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation1.setSpeed(128.5f);
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[ChartView.SPEED_SERIES + 1], 0.01);

        /*
         * Tests the logic when both metricUnits and reportSpeed are true.
         * This location will be filled into speed buffer.
         */
        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();

        /*
         * Add a time span here to make sure the second point is valid, the value 222 here is doesn't matter.
         */
        sensorDataSetLocation2.setTime(sensorDataSetLocation1.getTime() + 222);
        sensorDataSetLocation2.setSpeed(130);
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals(130.0 * UnitConversions.MS_TO_KMH, point[ChartView.SPEED_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to compute speed when use Imperial.
     */
    @Test
    public void testFillDataPoint_speedImperial() {
        // Setups to use imperial.
        chartFragment.setMetricUnits(false);

        // First data point is not added to the speed buffer
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation1.setSpeed(100.0f);
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[ChartView.SPEED_SERIES + 1], 0.01);

        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();

        /*
         * Add a time span here to make sure the second point and the speed is valid.
         * Speed is valid if: speedDifference > Constants.MAX_ACCELERATION * timeDifference speedDifference = 102 -100 timeDifference = 222
         */
        sensorDataSetLocation2.setTime(sensorDataSetLocation2.getTime() + 222);
        sensorDataSetLocation2.setSpeed(102);
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals(102.0 * UnitConversions.MS_TO_KMH * UnitConversions.KM_TO_MI, point[ChartView.SPEED_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to get pace value when reportSpeed is false.
     */
    @Test
    public void testFillDataPoint_pace_nonZeroSpeed() {
        // Setups reportSpeed to false.
        chartFragment.setReportSpeed(false);

        // First data point is not added to the speed buffer
        TrackPoint sensorDataSetLocation1 = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation1.setSpeed(100.0f);
        double[] point = fillDataPointTestHelper(sensorDataSetLocation1);
        Assert.assertEquals(0.0, point[ChartView.SPEED_SERIES + 1], 0.01);

        TrackPoint sensorDataSetLocation2 = TrackStubUtils.createSensorDataSetLocation();

        /*
         * Add a time span here to make sure the second point and the speed is valid.
         * Speed is valid if: speedDifference > Constants.MAX_ACCELERATION * timeDifference speedDifference = 102 -100 timeDifference = 222
         */
        sensorDataSetLocation2.setTime(sensorDataSetLocation2.getTime() + 222);
        sensorDataSetLocation2.setSpeed(102);
        point = fillDataPointTestHelper(sensorDataSetLocation2);
        Assert.assertEquals(HOURS_PER_UNIT / (102.0 * UnitConversions.MS_TO_KMH), point[ChartView.PACE_SERIES + 1], 0.01);
    }

    /**
     * Tests the logic to get pace value when reportSpeed is false and average speed is zero.
     */
    @Test
    public void testFillDataPoint_pace_zeroSpeed() {
        // Setups reportSpeed to false.
        chartFragment.setReportSpeed(false);
        TrackPoint sensorDataSetLocation = TrackStubUtils.createSensorDataSetLocation();
        sensorDataSetLocation.setSpeed(0);
        double[] point = fillDataPointTestHelper(sensorDataSetLocation);
        Assert.assertEquals(0.0, point[ChartView.PACE_SERIES + 1], 0.01);
    }

    /**
     * Helper method to test fillDataPoint.
     *
     * @param location location to fill
     * @return data of this location
     */
    private double[] fillDataPointTestHelper(Location location) {
        double[] point = new double[ChartView.NUM_SERIES + 1];
        chartFragment.fillDataPoint(location, point);
        return point;
    }
}
