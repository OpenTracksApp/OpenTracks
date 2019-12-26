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

package de.dennisguse.opentracks.content;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Tests for {@link DescriptionGenerator}.
 *
 * @author Jimmy Shih
 */
@RunWith(AndroidJUnit4.class)
public class DescriptionGeneratorTest {

    private static final long START_TIME = 1288721514000L;
    private DescriptionGenerator descriptionGenerator;

    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        descriptionGenerator = new DescriptionGenerator(ApplicationProvider.getApplicationContext());
    }

    /**
     * Tests {@link DescriptionGenerator#generateTrackDescription(Track, boolean)}.
     */
    @Test
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
        String expected = //"Created by"
                "<a href='https://github.com/dennisguse/opentracks'>OpenTracks</a><p>"
                + "Name: -<br>"
                + "Activity type: hiking<br>"
                + "Description: -<br>"
                + "Total distance: 20.00 km (12.4 mi)<br>"
                + "Total time: 10:00<br>"
                + "Moving time: 05:00<br>"
                + "Average speed: 120.00 km/h (74.6 mi/h)<br>"
                + "Average moving speed: 240.00 km/h (149.1 mi/h)<br>"
                + "Max speed: 360.00 km/h (223.7 mi/h)<br>"
                + "Average pace: 0:30 min/km (0:48 min/mi)<br>"
                + "Average moving pace: 0:15 min/km (0:24 min/mi)<br>"
                + "Fastest pace: 0:10 min/km (0:16 min/mi)<br>"
                + "Max elevation: 550 m (1804 ft)<br>"
                + "Min elevation: -500 m (-1640 ft)<br>"
                + "Elevation gain: 6000 m (19685 ft)<br>"
                + "Max grade: 42 %<br>"
                + "Min grade: 11 %<br>"
                + "Recorded: " + StringUtils.formatDateTime(context, START_TIME) + "<br>";

        Assert.assertEquals(expected, descriptionGenerator.generateTrackDescription(track, true));
    }


    /**
     * Tests {@link DescriptionGenerator#writeDistance(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteDistance() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeDistance(1100, builder, R.string.description_total_distance, "<br>");
        Assert.assertEquals("Total distance: 1.10 km (0.7 mi)<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeTime(long, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteTime() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeTime(1000, builder, R.string.description_total_time, "<br>");
        Assert.assertEquals("Total time: 00:01<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeSpeed(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteSpeed() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeSpeed(1.1, builder, R.string.description_average_speed, "\n");
        Assert.assertEquals("Average speed: 3.96 km/h (2.5 mi/h)\n", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeElevation(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteElevation() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeElevation(4.2, builder, R.string.description_min_elevation, "<br>");
        Assert.assertEquals("Min elevation: 4 m (14 ft)<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writePace(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWritePace() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writePace(1.1, builder, R.string.description_average_pace_in_minute, "\n");
        Assert.assertEquals("Average pace: 15:09 min/km (24:23 min/mi)\n", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeGrade(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteGrade() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeGrade(.042, builder, R.string.description_max_grade, "<br>");
        Assert.assertEquals("Max grade: 4 %<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeGrade(double, StringBuilder, int, String)} with a NaN.
     */
    @Test
    public void testWriteGrade_nan() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeGrade(Double.NaN, builder, R.string.description_max_grade, "<br>");
        Assert.assertEquals("Max grade: 0 %<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeGrade(double, StringBuilder, int, String)} with an infinite number.
     */
    @Test
    public void testWriteGrade_infinite() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeGrade(Double.POSITIVE_INFINITY, builder, R.string.description_max_grade, "<br>");
        Assert.assertEquals("Max grade: 0 %<br>", builder.toString());
    }
}
