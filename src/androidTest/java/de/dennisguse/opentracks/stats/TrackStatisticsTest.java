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
package de.dennisguse.opentracks.stats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link TrackStatistics}.
 * This only tests non-trivial pieces of that class.
 *
 * @author Rodrigo Damazio
 */
@RunWith(JUnit4.class)
public class TrackStatisticsTest {

    private TrackStatistics statistics;

    @Before
    public void setUp() {
        statistics = new TrackStatistics();
    }

    @Test
    public void testMerge_no_data() {
        // given
        TrackStatistics statistics2 = new TrackStatistics();

        // when
        statistics.merge(statistics2);

        // then
        assertNull(statistics.getStartTime());
        assertNull(statistics.getStopTime());
        assertEquals(Duration.ofSeconds(0), statistics.getMovingTime());
        assertEquals(Duration.ofSeconds(0), statistics.getTotalTime());

        assertNull(statistics.getTotalElevationGain());
        assertNull(statistics.getTotalElevationLoss());
        assertEquals(Double.NEGATIVE_INFINITY, statistics.getMaxElevation(), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, statistics.getMinElevation(), 0.0);
        assertEquals(0.0, statistics.getMaxSpeed(), 0.0);
        assertEquals(0.0, statistics.getAverageSpeed(), 0.0);
        assertEquals(0.0, statistics.getAverageMovingSpeed(), 0.0);
    }

    @Test
    public void testMerge() {
        // given
        TrackStatistics statistics2 = new TrackStatistics();
        statistics.setStartTime(Instant.ofEpochMilli(1000));  // Resulting start time
        statistics.setStopTime(Instant.ofEpochMilli(2500));
        statistics2.setStartTime(Instant.ofEpochMilli(3000));
        statistics2.setStopTime(Instant.ofEpochMilli(4000));  // Resulting stop time
        statistics.setTotalTime(Duration.ofMillis(1500));
        statistics2.setTotalTime(Duration.ofMillis(1000));  // Result: 1500+1000
        statistics.setMovingTime(Duration.ofMillis(700));
        statistics2.setMovingTime(Duration.ofMillis(600));  // Result: 700+600
        statistics.setTotalDistance(750.0);
        statistics2.setTotalDistance(350.0);  // Result: 750+350
        statistics.setTotalElevationGain(50.0f);
        statistics2.setTotalElevationGain(850.0f);  // Result: 850+50
        statistics.setMaxSpeed(60.0);  // Resulting max speed
        statistics2.setMaxSpeed(30.0);
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);  // Resulting min elevation
        statistics2.setMaxElevation(3575.0);  // Resulting max elevation
        statistics2.setMinElevation(2800.0);

        // when
        statistics.merge(statistics2);

        // then
        assertEquals(Instant.ofEpochMilli(1000), statistics.getStartTime());
        assertEquals(Instant.ofEpochMilli(4000), statistics.getStopTime());
        assertEquals(Duration.ofMillis(2500), statistics.getTotalTime());
        assertEquals(Duration.ofMillis(1300), statistics.getMovingTime());
        assertEquals(1100.0, statistics.getTotalDistance(), 0.001);
        assertEquals(900.0, statistics.getTotalElevationGain(), 0.001);
        assertEquals(statistics.getTotalDistance() / statistics.getMovingTime().getSeconds(), statistics.getMaxSpeed(), 0.001);
        assertEquals(1200.0, statistics.getMinElevation(), 0.001);
        assertEquals(3575.0, statistics.getMaxElevation(), 0.001);
    }

    @Test
    public void testGetAverageSpeed() {
        statistics.setTotalDistance(1000.0);
        statistics.setTotalTime(Duration.ofMillis(50000));
        assertEquals(20.0, statistics.getAverageSpeed(), 0.001);
    }

    @Test
    public void testGetAverageMovingSpeed() {
        statistics.setTotalDistance(1000.0);
        statistics.setMovingTime(Duration.ofMillis(20000));
        assertEquals(50.0, statistics.getAverageMovingSpeed(), 0.001);
    }
}
