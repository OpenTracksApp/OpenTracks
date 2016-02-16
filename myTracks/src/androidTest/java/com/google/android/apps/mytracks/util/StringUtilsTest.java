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

package com.google.android.apps.mytracks.util;

import android.test.AndroidTestCase;

import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Tests for {@link StringUtils}.
 *
 * @author Rodrigo Damazio
 */
public class StringUtilsTest extends AndroidTestCase {

  /**
   * Tests {@link StringUtils#formatDateTimeIso8601(long)}.
   */
  public void testFormatDateTimeIso8601() {
    assertEquals("1970-01-01T00:00:12.345Z", StringUtils.formatDateTimeIso8601(12345));
  }

  /**
   * Tests {@link StringUtils#formatElapsedTime(long)}.
   */
  public void testformatElapsedTime() {
    // 1 second
    assertEquals("00:01", StringUtils.formatElapsedTime(1000));
    // 10 seconds
    assertEquals("00:10", StringUtils.formatElapsedTime(10000));
    // 1 minute
    assertEquals("01:00", StringUtils.formatElapsedTime(60000));
    // 10 minutes
    assertEquals("10:00", StringUtils.formatElapsedTime(600000));
    // 1 hour
    assertEquals("1:00:00", StringUtils.formatElapsedTime(3600000));
    // 10 hours
    assertEquals("10:00:00", StringUtils.formatElapsedTime(36000000));
    // 100 hours
    assertEquals("100:00:00", StringUtils.formatElapsedTime(360000000));
  }

  /**
   * Tests {@link StringUtils#formatElapsedTimeWithHour(long)}.
   */
  public void testformatElapsedTimeWithHour() {
    // 1 second
    assertEquals("0:00:01", StringUtils.formatElapsedTimeWithHour(1000));
    // 10 seconds
    assertEquals("0:00:10", StringUtils.formatElapsedTimeWithHour(10000));
    // 1 minute
    assertEquals("0:01:00", StringUtils.formatElapsedTimeWithHour(60000));
    // 10 minutes
    assertEquals("0:10:00", StringUtils.formatElapsedTimeWithHour(600000));
    // 1 hour
    assertEquals("1:00:00", StringUtils.formatElapsedTimeWithHour(3600000));
    // 10 hours
    assertEquals("10:00:00", StringUtils.formatElapsedTimeWithHour(36000000));
    // 100 hours
    assertEquals("100:00:00", StringUtils.formatElapsedTimeWithHour(360000000));
  }

  /**
   * Tests {@link StringUtils#formatDistance(android.content.Context, double,
   * boolean)}.
   */
  public void testFormatDistance() {
    // A large number in metric
    assertEquals("5.00 km", StringUtils.formatDistance(getContext(), 5000, true));
    // A large number in imperial
    assertEquals("3.11 mi", StringUtils.formatDistance(getContext(), 5000, false));
    // A small number in metric
    assertEquals("100.00 m", StringUtils.formatDistance(getContext(), 100, true));
    // A small number in imperial
    assertEquals("328.08 ft", StringUtils.formatDistance(getContext(), 100, false));
  }

  /**
   * Tests {@link StringUtils#formatCData(String)}.
   */
  public void testFormatCData() {
    assertEquals("<![CDATA[hello]]>", StringUtils.formatCData("hello"));
    assertEquals("<![CDATA[hello]]]]><![CDATA[>there]]>", StringUtils.formatCData("hello]]>there"));
  }
  
  /**
   * Tests {@link StringUtils#getTime(String)}.
   */
  public void testGetTime() {
    assertGetTime("2010-05-04T03:02:01", 2010, 5, 4, 3, 2, 1, 0);
    assertGetTime("2010-05-04T03:02:01Z", 2010, 5, 4, 3, 2, 1, 0);
  }

  /**
   * Tests {@link StringUtils#getTime(String)} with fractional seconds.
   */
  public void testGetTime_fractional() {
    assertGetTime("2010-05-04T03:02:01.3", 2010, 5, 4, 3, 2, 1, 300);
    assertGetTime("2010-05-04T03:02:01.35", 2010, 5, 4, 3, 2, 1, 350);
    assertGetTime("2010-05-04T03:02:01.352Z", 2010, 5, 4, 3, 2, 1, 352);
    assertGetTime("2010-05-04T03:02:01.3525Z", 2010, 5, 4, 3, 2, 1, 352);
  }

  /**
   * Tests {@link StringUtils#getTime(String)} with time zone.
   */
  public void testGetTime_timezone() {
    assertGetTime("2010-05-04T03:02:01Z", 2010, 5, 4, 3, 2, 1, 0);
    assertGetTime("2010-05-04T03:02:01+00:00", 2010, 5, 4, 3, 2, 1, 0);
    assertGetTime("2010-05-04T03:02:01-00:00", 2010, 5, 4, 3, 2, 1, 0);
    assertGetTime("2010-05-04T03:02:01+01:00", 2010, 5, 4, 2, 2, 1, 0);
    assertGetTime("2010-05-04T03:02:01+10:30", 2010, 5, 3, 16, 32, 1, 0);
    assertGetTime("2010-05-04T03:02:01-09:30", 2010, 5, 4, 12, 32, 1, 0);
    assertGetTime("2010-05-04T03:02:01-05:00", 2010, 5, 4, 8, 2, 1, 0);
  }

  /**
   * Tests {@link StringUtils#getTime(String)} with fractional seconds and time
   * zone.
   */
  public void testGetTime_fractionalAndTimezone() {
    assertGetTime("2010-05-04T03:02:01.352Z", 2010, 5, 4, 3, 2, 1, 352);
    assertGetTime("2010-05-04T03:02:01.47+00:00", 2010, 5, 4, 3, 2, 1, 470);
    assertGetTime("2010-05-04T03:02:01.5791+03:00", 2010, 5, 4, 0, 2, 1, 579);
    assertGetTime("2010-05-04T03:02:01.8-05:30", 2010, 5, 4, 8, 32, 1, 800);
  }

  /**
   * Asserts the {@link StringUtils#getTime(String)} returns the expected
   * values.
   *
   * @param xmlDateTime the xml date time string
   * @param year the expected year
   * @param month the expected month
   * @param day the expected day
   * @param hour the expected hour
   * @param minute the expected minute
   * @param second the expected second
   * @param millisecond the expected milliseconds
   */
  private void assertGetTime(String xmlDateTime, int year, int month, int day, int hour, int minute,
      int second, int millisecond) {
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.set(year, month - 1, day, hour, minute, second);
    calendar.set(GregorianCalendar.MILLISECOND, millisecond);
    assertEquals(calendar.getTimeInMillis(), StringUtils.getTime(xmlDateTime));
  }

  /**
   * Tests {@link StringUtils#getTimeParts(long)} with a positive number.
   */
  public void testGetTimeParts_postive() {
    int parts[] = StringUtils.getTimeParts(61000);
    assertEquals(1, parts[0]);
    assertEquals(1, parts[1]);
    assertEquals(0, parts[2]);
  }

  /**
   * Tests {@link StringUtils#getTimeParts(long)} with a negative number.
   */
  public void testGetTimeParts_negative() {
    int parts[] = StringUtils.getTimeParts(-61000);
    assertEquals(-1, parts[0]);
    assertEquals(-1, parts[1]);
    assertEquals(0, parts[2]);
  }
}
