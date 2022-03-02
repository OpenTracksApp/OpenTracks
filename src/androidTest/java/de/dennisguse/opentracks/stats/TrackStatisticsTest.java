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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;

/**
 * Tests for {@link TrackStatistics}.
 * This only tests non-trivial pieces of that class.
 *
 * @author Rodrigo Damazio
 */
@RunWith(AndroidJUnit4.class)
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

        assertNull(statistics.getTotalAltitudeGain());
        assertNull(statistics.getTotalAltitudeLoss());
        assertEquals(Double.NEGATIVE_INFINITY, statistics.getMaxAltitude(), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, statistics.getMinAltitude(), 0.0);
        assertEquals(0.0, statistics.getMaxSpeed().toMPS(), 0.0);
        assertEquals(0.0, statistics.getAverageSpeed().toMPS(), 0.0);
        assertEquals(0.0, statistics.getAverageMovingSpeed().toMPS(), 0.0);
        assertNull(statistics.getAverageHeartRate());
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
        statistics.setTotalDistance(Distance.of(750.0));
        statistics2.setTotalDistance(Distance.of(350.0));  // Result: 750+350
        statistics.setTotalAltitudeGain(50.0f);
        statistics2.setTotalAltitudeGain(850.0f);  // Result: 850+50
        statistics.setMaxSpeed(Speed.of(60.0));  // Resulting max speed
        statistics2.setMaxSpeed(Speed.of(30.0));
        statistics.setMaxAltitude(1250.0);
        statistics.setMinAltitude(1200.0);  // Resulting min altitude
        statistics2.setMaxAltitude(3575.0);  // Resulting max altitude
        statistics2.setMinAltitude(2800.0);
        statistics.setAverageHeartRate(HeartRate.of(100f));
        statistics2.setAverageHeartRate(HeartRate.of(200f));

        // when
        statistics.merge(statistics2);

        // then
        assertEquals(Instant.ofEpochMilli(1000), statistics.getStartTime());
        assertEquals(Instant.ofEpochMilli(4000), statistics.getStopTime());
        assertEquals(Duration.ofMillis(2500), statistics.getTotalTime());
        assertEquals(Duration.ofMillis(1300), statistics.getMovingTime());
        assertEquals(1100.0, statistics.getTotalDistance().toM(), 0.001);
        assertEquals(900.0, statistics.getTotalAltitudeGain(), 0.001);
        assertEquals(Speed.of(statistics.getTotalDistance(), statistics.getMovingTime()).toMPS(), statistics.getMaxSpeed().toMPS(), 0.001);
        assertEquals(1200.0, statistics.getMinAltitude(), 0.001);
        assertEquals(3575.0, statistics.getMaxAltitude(), 0.001);
        assertEquals(150.0, statistics.getAverageHeartRate().getBPM(), 0.001);
    }

    @Test
    public void testGetAverageSpeed() {
        statistics.setTotalDistance(Distance.of(1000.0));
        statistics.setTotalTime(Duration.ofMillis(50000));
        assertEquals(20.0, statistics.getAverageSpeed().toMPS(), 0.001);
    }

    @Test
    public void testGetAverageMovingSpeed() {
        statistics.setTotalDistance(Distance.of(1000.0));
        statistics.setMovingTime(Duration.ofMillis(20000));
        assertEquals(50.0, statistics.getAverageMovingSpeed().toMPS(), 0.001);
    }
}
