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
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various string manipulation methods.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils {

  private static final SimpleDateFormat ISO_8601_DATE_TIME_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final SimpleDateFormat ISO_8601_BASE = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss");
  private static final Pattern ISO_8601_EXTRAS = Pattern.compile(
      "^(\\.\\d+)?(?:Z|([+-])(\\d{2}):(\\d{2}))?$");

  static {
    ISO_8601_DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    ISO_8601_BASE.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private StringUtils() {}

  /**
   * Formats the time based on user's phone date/time preferences.
   *
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatTime(Context context, long time) {
    return android.text.format.DateFormat.getTimeFormat(context).format(time);
  }

  /**
   * Formats the date and time based on user's phone date/time preferences.
   *
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatDateTime(Context context, long time) {
    return android.text.format.DateFormat.getDateFormat(context).format(time) + " "
        + formatTime(context, time);
  }

  /**
   * Formats the time using the ISO 8601 date time format with fractional
   * seconds in UTC time zone.
   *
   * @param time the time in milliseconds
   */
  public static String formatDateTimeIso8601(long time) {
    return ISO_8601_DATE_TIME_FORMAT.format(time);
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
   * Formats the elapsed time in the form "H:MM:SS".
   *
   * @param time the time in milliseconds
   */
  public static String formatElapsedTimeWithHour(long time) {
    String value = formatElapsedTime(time);
    return TextUtils.split(value, ":").length == 2 ? "0:" + value : value;
  }

  /**
   * Formats the distance.
   *
   * @param context the context
   * @param distance the distance in meters
   * @param metric true to use metric. False to use imperial
   */
  public static String formatDistance(Context context, double distance, boolean metric) {
    if (metric) {
      if (distance > 2000.0) {
        distance *= UnitConversions.M_TO_KM;
        return context.getString(R.string.value_float_kilometer, distance);
      } else {
        return context.getString(R.string.value_float_meter, distance);        
      }
    } else {
      if (distance * UnitConversions.M_TO_MI > 2) {
        distance *= UnitConversions.M_TO_MI;
        return context.getString(R.string.value_float_mile, distance);        
      } else {
        distance *= UnitConversions.M_TO_FT;
        return context.getString(R.string.value_float_feet, distance);
      }
    }
  }
  
  /**
   * Formats the speed.
   * 
   * @param context the context
   * @param speed the speed in meters per second
   * @param metric true to use metric. False to use imperial
   * @param reportSpeed true to report as speed. False to report as pace
   */
  public static String formatSpeed(
      Context context, double speed, boolean metric, boolean reportSpeed) {
    if (Double.isNaN(speed) || Double.isInfinite(speed)) {
      return context.getString(R.string.value_unknown);
    }
    if (metric) {
      speed = speed * UnitConversions.MS_TO_KMH;
      if (reportSpeed) {
        return context.getString(R.string.value_float_kilometer_hour, speed);
      } else {
        double paceInMinute = speed == 0 ? 0.0 : 60 / speed;
        return context.getString(R.string.value_float_minute_kilometer, paceInMinute);
      }
    } else {
      speed = speed * UnitConversions.MS_TO_KMH * UnitConversions.KM_TO_MI;
      if (reportSpeed) {
        return context.getString(R.string.value_float_mile_hour, speed);
      } else {
        double paceInMinute = speed == 0 ? 0.0 : 60 / speed;
        return context.getString(R.string.value_float_minute_mile, paceInMinute);
      }
    }
  }
  
  /**
   * Formats the elapsed time and distance.
   *
   * @param context the context
   * @param elapsedTime the elapsed time in milliseconds
   * @param distance the distance in meters
   * @param metric true to use metric. False to use imperial
   */
  public static String formatTimeDistance(
      Context context, long elapsedTime, double distance, boolean metric) {
    return formatElapsedTime(elapsedTime) + " " + formatDistance(context, distance, metric);
  }

  /**
   * Formats the given text as a XML CDATA element. This includes adding the
   * starting and ending CDATA tags. Please notice that this may result in
   * multiple consecutive CDATA tags.
   *
   * @param text the given text
   */
  public static String formatCData(String text) {
    return "<![CDATA[" + text.replaceAll("]]>", "]]]]><![CDATA[>") + "]]>";
  }

  /**
   * Gets the time, in milliseconds, from an XML date time string as defined at
   * http://www.w3.org/TR/xmlschema-2/#dateTime
   *
   * @param xmlDateTime the XML date time string
   */
  public static long getTime(String xmlDateTime) {
    // Parse the date time base
    ParsePosition position = new ParsePosition(0);
    Date date = ISO_8601_BASE.parse(xmlDateTime, position);
    if (date == null) {
      throw new IllegalArgumentException("Invalid XML dateTime value: " + xmlDateTime 
          + " (at position " + position.getErrorIndex() + ")"); }

    // Parse the date time extras
    Matcher matcher = ISO_8601_EXTRAS.matcher(xmlDateTime.substring(position.getIndex()));
    if (!matcher.matches()) {
      // This will match even an empty string as all groups are optional. Thus a
      // non-match means invalid content.
      throw new IllegalArgumentException("Invalid XML dateTime value: " + xmlDateTime);
    }

    long time = date.getTime();

    // Account for fractional seconds
    String fractional = matcher.group(1);
    if (fractional != null) {
      // Regex ensures fractional part is in (0,1)
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
        throw new IllegalArgumentException("Bad timezone: " + xmlDateTime);
      }

      long totalOffsetMillis = (offsetMins + offsetHours * 60L) * 60000L;

      // Convert to UTC
      if (plusSign) {
        time -= totalOffsetMillis;
      } else {
        time += totalOffsetMillis;
      }
    }
    return time;
  }

  /**
   * Gets the time as an array of three integers. Index 0 contains the number of
   * seconds, index 1 contains the number of minutes, and index 2 contains the
   * number of hours.
   *
   * @param time the time in milliseconds
   * @return an array of 3 elements.
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
    int minutes = (int) (seconds / 60);
    parts[1] = minutes % 60;
    parts[2] = minutes / 60;
    return parts;
  }
}
