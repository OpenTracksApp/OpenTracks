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

package de.dennisguse.opentracks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.LocaleRule;
import de.dennisguse.opentracks.TimezoneRule;

/**
 * Tests for {@link StringUtils}.
 *
 * @author Rodrigo Damazio
 */
@RunWith(AndroidJUnit4.class)
public class StringUtilsTest {

    @Rule
    public final LocaleRule mLocaleRule = new LocaleRule(Locale.ENGLISH);

    @Rule
    public TimezoneRule timezoneRule = new TimezoneRule(TimeZone.getTimeZone("Europe/Berlin"));

    private final Context context = ApplicationProvider.getApplicationContext();

    /**
     * Tests {@link StringUtils#formatElapsedTime(Duration)}.
     */
    @Test
    public void testformatElapsedTime() {
        assertEquals("00:01", StringUtils.formatElapsedTime(Duration.ofMillis(1000)));
        assertEquals("00:10", StringUtils.formatElapsedTime(Duration.ofMillis(10000)));
        assertEquals("01:00", StringUtils.formatElapsedTime(Duration.ofMillis(60000)));
        assertEquals("10:00", StringUtils.formatElapsedTime(Duration.ofMillis(600000)));
        assertEquals("1:00:00", StringUtils.formatElapsedTime(Duration.ofMillis(3600000)));
        assertEquals("10:00:00", StringUtils.formatElapsedTime(Duration.ofMillis(36000000)));
        assertEquals("100:00:00", StringUtils.formatElapsedTime(Duration.ofMillis(360000000)));
    }

    /**
     * Tests {@link StringUtils#formatElapsedTimeWithHour(Duration)}.
     */
    @Test
    public void testformatElapsedTimeWithHour() {
        assertEquals("0:00:01", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(1000)));
        assertEquals("0:00:10", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(10000)));
        assertEquals("0:01:00", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(60000)));
        assertEquals("0:10:00", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(600000)));
        assertEquals("1:00:00", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(3600000)));
        assertEquals("10:00:00", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(36000000)));
        assertEquals("100:00:00", StringUtils.formatElapsedTimeWithHour(Duration.ofMillis(360000000)));
    }

    /**
     * Tests {@link StringUtils#formatCData(String)}.
     */
    @Test
    public void testFormatCData() {
        assertEquals("<![CDATA[hello]]>", StringUtils.formatCData("hello"));
        assertEquals("<![CDATA[hello]]]]><![CDATA[>there]]>", StringUtils.formatCData("hello]]>there"));
    }

    @Test
    public void testParseTime() {
        assertEquals(Instant.ofEpochMilli(352), StringUtils.parseTime("1970-01-01T00:00:00.352").toInstant());
        assertEquals(Instant.ofEpochMilli(352), StringUtils.parseTime("1970-01-01T00:00:00.352Z").toInstant());
        assertEquals(Instant.ofEpochMilli(352), StringUtils.parseTime("1970-01-01T00:00:00.352+00:00").toInstant());

        assertEquals(Instant.ofEpochMilli(352), StringUtils.parseTime("1970-01-01T01:00:00.352+01:00").toInstant());
        assertEquals(Instant.ofEpochMilli(352).plus(Duration.ofHours(1)), StringUtils.parseTime("1970-01-01T00:00:00.352-01:00").toInstant());
    }

    @Test
    public void testFormatDecimal() {
        assertEquals("0", StringUtils.formatDecimal(0.0, 0));
        assertEquals("0", StringUtils.formatDecimal(0.1, 0));
        assertEquals("1", StringUtils.formatDecimal(1.1, 0));
        assertEquals("10", StringUtils.formatDecimal(10, 0));
        assertEquals("10", StringUtils.formatDecimal(10.1, 0));
        assertEquals("-0", StringUtils.formatDecimal(-0.1, 0));

        assertEquals("0.00", StringUtils.formatDecimal(0.0, 2));
        assertEquals("0.10", StringUtils.formatDecimal(0.1, 2));
        assertEquals("1.10", StringUtils.formatDecimal(1.1, 2));
        assertEquals("10.00", StringUtils.formatDecimal(10, 2));
        assertEquals("10.10", StringUtils.formatDecimal(10.1, 2));
        assertEquals("10.11", StringUtils.formatDecimal(10.111, 2));
        assertEquals("-0.10", StringUtils.formatDecimal(-0.1, 2));

        assertEquals("1.0", StringUtils.formatDecimal(0.99, 1));
    }

    @Test
    public void testFormatDateTodayRelative() {
        // given
        ArrayList<String> shortDays = Arrays.stream(DayOfWeek.values()).map(d -> d.getDisplayName(TextStyle.FULL, Locale.getDefault())).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> shortMonths = Arrays.stream(Month.values()).map(m -> m.getDisplayName(TextStyle.SHORT, Locale.getDefault())).collect(Collectors.toCollection(ArrayList::new));

        LocalDate today = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayName = today.minusDays(2);
        LocalDate thisYear = today.minusDays(15);
        LocalDate aYearAgo = today.minusDays(400);

        int offsetFromLocale = OffsetDateTime.now().getOffset().getTotalSeconds() - 28800; // -08:00

        OffsetDateTime todayRecordedOdt = OffsetDateTime.of(LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 20, 0, 0), ZoneOffset.ofTotalSeconds(offsetFromLocale));
        OffsetDateTime yesterdayRecordedOdt = OffsetDateTime.of(LocalDateTime.of(yesterday.getYear(), yesterday.getMonth(), yesterday.getDayOfMonth(), 1, 0, 0), ZoneOffset.ofTotalSeconds(offsetFromLocale));
        OffsetDateTime dayNameRecordedOdt = OffsetDateTime.of(LocalDateTime.of(dayName.getYear(), dayName.getMonth(), dayName.getDayOfMonth(), 7, 0, 0), ZoneOffset.ofTotalSeconds(offsetFromLocale));
        OffsetDateTime thisYearRecordedOdt = OffsetDateTime.of(LocalDateTime.of(thisYear.getYear(), thisYear.getMonth(), thisYear.getDayOfMonth(), 12, 0, 0), ZoneOffset.ofTotalSeconds(offsetFromLocale));
        OffsetDateTime aYearAgoRecordedOdt = OffsetDateTime.of(LocalDateTime.of(aYearAgo.getYear(), aYearAgo.getMonth(), aYearAgo.getDayOfMonth(), 17, 0, 0), ZoneOffset.ofTotalSeconds(offsetFromLocale));

        // when
        String formatToday = StringUtils.formatDateTodayRelative(context, todayRecordedOdt);
        String formatYesterday = StringUtils.formatDateTodayRelative(context, yesterdayRecordedOdt);
        String formatDayName = StringUtils.formatDateTodayRelative(context, dayNameRecordedOdt);
        String formatThisYear = StringUtils.formatDateTodayRelative(context, thisYearRecordedOdt);
        String formatAYearAgo = StringUtils.formatDateTodayRelative(context, aYearAgoRecordedOdt);

        // then
        assertEquals("Today", formatToday);
        assertEquals("Yesterday", formatYesterday);
        assertTrue(shortDays.contains(formatDayName)); // Something like Friday
        if (today.getYear() != thisYear.getYear()) {
            assertTrue(shortMonths.stream().anyMatch(fty -> formatThisYear.matches("\\d+ " + fty + " \\d{4}"))); // Something like 14 Dec 2021
        } else {
            assertTrue(shortMonths.stream().anyMatch(fty -> formatThisYear.matches("\\d+ " + fty))); // Something like 14 Dec
        }
        assertTrue(shortMonths.stream().anyMatch(fty -> formatAYearAgo.matches("\\d+ " + fty + " \\d{4}"))); // Something like 14 Dec 2021
    }
}
