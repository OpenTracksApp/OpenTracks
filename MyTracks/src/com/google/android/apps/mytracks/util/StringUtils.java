/*
 * Copyright 2008 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various string manipulation methods.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils {

  private StringUtils() {}

  /**
   * Formats the date and time based on user's phone date/time preferences.
   *
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatDateTime(Context context, long time) {
    DateFormat dateFormatter = android.text.format.DateFormat.getDateFormat(context);
    return dateFormatter.format(new Date(time)) + " " + formatTime(context, time);
  }

  /**
   * Formats the time based on user's phone date/time preferences.
   *
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatTime(Context context, long time) {
    DateFormat timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
    return timeFormatter.format(new Date(time));
  }

  /**
   * Formats the elapsed timed in the form "MM:SS" or "H:MM:SS".
   *
   * @param time the time in milliseconds
   */
  public static String formatElapsedTime(long time) {
    return DateUtils.formatElapsedTime(time / 1000);
  }

  /**
   * Formats the elapsed time and total distance.
   *
   * @param context the current context
   * @param totalDistance the total distance in meters
   * @param totalTime the total time in milliseconds
   * @param metric whether to use metric units
   * @return the formatted string
   */
  public static String formatTimeDistance(Context context, double totalDistance, long totalTime, boolean metric) {
    String distanceUnit;
    if (metric) {
      if (totalDistance > 2000.0) {
        totalDistance *= UnitConversions.M_TO_KM;
        distanceUnit = context.getString(R.string.unit_kilometer);
      } else {
        distanceUnit = context.getString(R.string.unit_meter);
      }
    } else {
      if (totalDistance * UnitConversions.M_TO_MI > 2) {
        totalDistance *= UnitConversions.M_TO_MI;
        distanceUnit = context.getString(R.string.unit_mile);
      } else {
        totalDistance *= UnitConversions.M_TO_FT;
        distanceUnit = context.getString(R.string.unit_feet);
      }
    }
    return String.format("%s  %.2f %s",
        formatElapsedTime(totalTime),
        totalDistance,
        distanceUnit);
  }

  private static final NumberFormat SINGLE_DECIMAL_PLACE_FORMAT = NumberFormat.getNumberInstance();

  static {
    SINGLE_DECIMAL_PLACE_FORMAT.setMaximumFractionDigits(1);
    SINGLE_DECIMAL_PLACE_FORMAT.setMinimumFractionDigits(1);
  }

  /**
   * Formats a double precision number as decimal number with a single decimal
   * place.
   *
   * @param number A double precision number
   * @return A string representation of a decimal number, derived from the input
   *         double, with a single decimal place
   */
  public static final String formatSingleDecimalPlace(double number) {
    return SINGLE_DECIMAL_PLACE_FORMAT.format(number);
  }

  /**
   * Formats the given text as a CDATA element to be used in a XML file. This
   * includes adding the starting and ending CDATA tags. Please notice that this
   * may result in multiple consecutive CDATA tags.
   *
   * @param unescaped the unescaped text to be formatted
   * @return the formatted text, inside one or more CDATA tags
   */
  public static String stringAsCData(String unescaped) {
    // "]]>" needs to be broken into multiple CDATA segments, like:
    // "Foo]]>Bar" becomes "<![CDATA[Foo]]]]><![CDATA[>Bar]]>"
    // (the end of the first CDATA has the "]]", the other has ">")
    String escaped = unescaped.replaceAll("]]>", "]]]]><![CDATA[>");
    return "<![CDATA[" + escaped + "]]>";
  }

  private static final SimpleDateFormat BASE_XML_DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  static {
    BASE_XML_DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "UTC"));
  }

  /**
   * Formats the time to the XML date time format.
   * 
   * @param time time in milliseconds
   */
  public static final String formatXmlDateTime(long time) {
    return BASE_XML_DATE_FORMAT.format(new Date(time));
  }

  private static final Pattern XML_DATE_EXTRAS_PATTERN =
      Pattern.compile("^(\\.\\d+)?(?:Z|([+-])(\\d{2}):(\\d{2}))?$");

  /**
   * Parses an XML dateTime element as defined by the XML standard.
   *
   * @see <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">dateTime</a>
   */
  public static long parseXmlDateTime(String xmlTime) {
    // Parse the base date (fixed format)
    ParsePosition position = new ParsePosition(0);
    Date date = BASE_XML_DATE_FORMAT.parse(xmlTime, position);
    if (date == null) {
      throw new IllegalArgumentException("Invalid XML dateTime value: '" + xmlTime
          + "' (at position " + position.getErrorIndex() + ")");
    }

    // Parse the extras
    Matcher matcher =
        XML_DATE_EXTRAS_PATTERN.matcher(xmlTime.substring(position.getIndex()));
    if (!matcher.matches()) {
      // This will match even an empty string as all groups are optional,
      // so a non-match means some other garbage was there
      throw new IllegalArgumentException("Invalid XML dateTime value: " + xmlTime);
    }

    long time = date.getTime();

    // Account for fractional seconds
    String fractional = matcher.group(1);
    if (fractional != null) {
      // Regex ensures fractional part is in (0,1(
      float fractionalSeconds = Float.parseFloat(fractional);
      long fractionalMillis = (long) (fractionalSeconds * 1000.0f);
      time += fractionalMillis;
    }

    // Account for timezones
    String sign = matcher.group(2);
    String offsetHoursStr = matcher.group(3);
    String offsetMinsStr = matcher.group(4);
    if (sign != null && offsetHoursStr != null && offsetMinsStr != null) {
      // Regex ensures sign is + or -
      boolean plusSign = sign.equals("+");
      int offsetHours = Integer.parseInt(offsetHoursStr);
      int offsetMins = Integer.parseInt(offsetMinsStr);

      // Regex ensures values are >= 0
      if (offsetHours > 14 || offsetMins > 59) {
        throw new IllegalArgumentException("Bad timezone in " + xmlTime);
      }

      long totalOffsetMillis = (offsetMins + offsetHours * 60L) * 60000L;

      // Make time go back to UTC
      if (plusSign) {
        time -= totalOffsetMillis;
      } else {
        time += totalOffsetMillis;
      }
    }

    return time;
  }

  /**
   * Gets the time as an array of parts.
   */
  public static int[] getTimeParts(long time) {
    if (time < 0) {
      int[] parts = getTimeParts(time * -1);
      parts[0] *= -1;
      parts[1] *= -1;
      parts[2] *= -1;
      return parts;
    }
    int[] parts = new int[3];

    long seconds = time / 1000;
    parts[0] = (int) (seconds % 60);
    int tmp = (int) (seconds / 60);
    parts[1] = tmp % 60;
    parts[2] = tmp / 60;

    return parts;
  }
}
