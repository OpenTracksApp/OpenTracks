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

package de.dennisguse.opentracks.share;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

import de.dennisguse.opentracks.LocaleRule;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Tests for {@link DescriptionGenerator}.
 *
 * @author Jimmy Shih
 */
@RunWith(AndroidJUnit4.class)
public class DescriptionGeneratorTest {

    @Rule
    public final LocaleRule mLocaleRule = new LocaleRule(Locale.ENGLISH);

    private static final Instant START_TIME = Instant.ofEpochMilli(1288721514000L);
    private DescriptionGenerator descriptionGenerator;

    private final Context context = ApplicationProvider.getApplicationContext();

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
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofMillis(600000));
        stats.setMovingTime(Duration.ofMillis(300000));
        stats.setMaxSpeed(Speed.of(100));
        stats.setMaxAltitude(550);
        stats.setMinAltitude(-500);
        stats.setTotalAltitudeGain(6000f);
        stats.setTotalAltitudeLoss(6000f);
        stats.setStartTime(START_TIME);
        track.setTrackStatistics(stats);
        track.setActivityTypeLocalized("hiking");
        String expected = //"Created by"
                "<a href='https://codeberg.org/OpenTracksApp/OpenTracks'>OpenTracks (Debug)</a><p>"
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
                        + "Elevation loss: 6000 m (19685 ft)<br>"
                        + "Recorded: " + StringUtils.formatDateTimeWithOffset(OffsetDateTime.ofInstant(START_TIME, ZoneId.systemDefault())) + "<br>";

        assertEquals(expected, descriptionGenerator.generateTrackDescription(track, true));
    }

    @Test
    public void testGenerateTrackDescriptionWithoutMaxMinAltitude() {
        Track track = new Track();
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofMillis(600000));
        stats.setMovingTime(Duration.ofMillis(300000));
        stats.setMaxSpeed(Speed.of(100));
        stats.setMaxAltitude(Double.POSITIVE_INFINITY);
        stats.setMinAltitude(Double.NEGATIVE_INFINITY);
        stats.setTotalAltitudeGain(6000f);
        stats.setTotalAltitudeLoss(6000f);
        stats.setStartTime(START_TIME);
        track.setTrackStatistics(stats);
        track.setActivityTypeLocalized("hiking");
        String expected = //"Created by"
                "<a href='https://codeberg.org/OpenTracksApp/OpenTracks'>OpenTracks (Debug)</a><p>"
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
                        + "Elevation gain: 6000 m (19685 ft)<br>"
                        + "Elevation loss: 6000 m (19685 ft)<br>"
                        + "Recorded: " + StringUtils.formatDateTimeWithOffset(OffsetDateTime.ofInstant(START_TIME, ZoneId.systemDefault())) + "<br>";

        assertEquals(expected, descriptionGenerator.generateTrackDescription(track, true));
    }


    /**
     * Tests {@link DescriptionGenerator#writeDistance(Distance, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteDistance() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeDistance(Distance.of(1100), builder, R.string.description_total_distance, "<br>");
        assertEquals("Total distance: 1.10 km (0.7 mi)<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeTime(Duration, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteTime() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeTime(Duration.ofMillis(1000), builder, R.string.description_total_time, "<br>");
        assertEquals("Total time: 00:01<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeSpeed(Speed, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteSpeed() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeSpeed(Speed.of(1.1), builder, R.string.description_average_speed, "\n");
        assertEquals("Average speed: 3.96 km/h (2.5 mi/h)\n", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writeAltitude(double, StringBuilder, int, String)}.
     */
    @Test
    public void testWriteAltitude() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writeAltitude(4.2, builder, R.string.description_min_altitude, "<br>");
        assertEquals("Min elevation: 4 m (14 ft)<br>", builder.toString());
    }

    /**
     * Tests {@link DescriptionGenerator#writePace(Speed, StringBuilder, int, String)}.
     */
    @Test
    public void testWritePace() {
        StringBuilder builder = new StringBuilder();
        descriptionGenerator.writePace(Speed.of(1.1), builder, R.string.description_average_pace_in_minute, "\n");
        assertEquals("Average pace: 15:09 min/km (24:23 min/mi)\n", builder.toString());
    }
}
