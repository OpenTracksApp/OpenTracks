/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.stats;

import junit.framework.TestCase;

/**
 * Tests for {@link TripStatistics}.
 * This only tests non-trivial pieces of that class.
 *
 * @author Rodrigo Damazio
 */
public class TripStatisticsTest extends TestCase {

  private TripStatistics statistics;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    statistics = new TripStatistics();
  }

  public void testSetBounds() {
    // This is not a trivial setter, conversion happens in it
    statistics.setBounds(12345, -34567, 56789, -98765);
    assertEquals(12345, statistics.getLeft());
    assertEquals(-34567, statistics.getTop());
    assertEquals(56789, statistics.getRight());
    assertEquals(-98765, statistics.getBottom());
  }

  public void testMerge() {
    TripStatistics statistics2 = new TripStatistics();
    statistics.setStartTime(1000L);  // Resulting start time
    statistics.setStopTime(2500L);
    statistics2.setStartTime(3000L);
    statistics2.setStopTime(4000L);  // Resulting stop time
    statistics.setTotalTime(1500L);
    statistics2.setTotalTime(1000L);  // Result: 1500+1000
    statistics.setMovingTime(700L);
    statistics2.setMovingTime(600L);  // Result: 700+600
    statistics.setTotalDistance(750.0);
    statistics2.setTotalDistance(350.0);  // Result: 750+350
    statistics.setTotalElevationGain(50.0);
    statistics2.setTotalElevationGain(850.0);  // Result: 850+50
    statistics.setMaxSpeed(60.0);  // Resulting max speed
    statistics2.setMaxSpeed(30.0);
    statistics.setMaxElevation(1250.0);
    statistics.setMinElevation(1200.0);  // Resulting min elevation
    statistics2.setMaxElevation(3575.0);  // Resulting max elevation
    statistics2.setMinElevation(2800.0);
    statistics.setMaxGrade(15.0);
    statistics.setMinGrade(-25.0);  // Resulting min grade
    statistics2.setMaxGrade(35.0);  // Resulting max grade
    statistics2.setMinGrade(0.0);

    // Resulting bounds: -10000, 35000, 30000, -40000
    statistics.setBounds(-10000, 20000, 30000, -40000);
    statistics2.setBounds(-5000, 35000, 0, 20000);

    statistics.merge(statistics2);

    assertEquals(1000L, statistics.getStartTime());
    assertEquals(4000L, statistics.getStopTime());
    assertEquals(2500L, statistics.getTotalTime());
    assertEquals(1300L, statistics.getMovingTime());
    assertEquals(1100.0, statistics.getTotalDistance());
    assertEquals(900.0, statistics.getTotalElevationGain());
    assertEquals(60.0, statistics.getMaxSpeed());
    assertEquals(-10000, statistics.getLeft());
    assertEquals(30000, statistics.getRight());
    assertEquals(35000, statistics.getTop());
    assertEquals(-40000, statistics.getBottom());
    assertEquals(1200.0, statistics.getMinElevation());
    assertEquals(3575.0, statistics.getMaxElevation());
    assertEquals(-25.0, statistics.getMinGrade());
    assertEquals(35.0, statistics.getMaxGrade());
  }

  public void testGetAverageSpeed() {
    statistics.setTotalDistance(1000.0);
    statistics.setTotalTime(50000);  // in milliseconds
    assertEquals(20.0, statistics.getAverageSpeed());
  }

  public void testGetAverageMovingSpeed() {
    statistics.setTotalDistance(1000.0);
    statistics.setMovingTime(20000);  // in milliseconds
    assertEquals(50.0, statistics.getAverageMovingSpeed());
  }
}
