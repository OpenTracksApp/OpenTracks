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
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
  private static final SimpleDateFormat ISO_8601_BASE = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
  private static final Pattern ISO_8601_EXTRAS = Pattern.compile(
      "^(\\.\\d+)?(?:Z|([+-])(\\d{2}):(\\d{2}))?$");
  static {
    ISO_8601_DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    ISO_8601_BASE.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private StringUtils() {}

  /**
   * Formats the date and time based on user's phone date/time preferences.
   * 
   * @param context the context
   * @param time the time in milliseconds
   */
  public static String formatDateTime(Context context, long time) {
    return DateUtils.formatDateTime(
        context, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE) + " "
        + DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME).toString();
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
    /*
     * Temporary workaround for DateUtils.formatElapsedTime(time / 1000). In API
     * level 17, it returns strings like "1:0:00" instead of "1:00:00", which
     * breaks several unit tests.
     */
    if (time < 0) {
      return "-";
    }
    long hours = 0;
    long minutes = 0;
    long seconds = 0;
    long elapsedSeconds = time / 1000;

    if (elapsedSeconds >= 3600) {
      hours = elapsedSeconds / 3600;
      elapsedSeconds -= hours * 3600;
    }
    if (elapsedSeconds >= 60) {
      minutes = elapsedSeconds / 60;
      elapsedSeconds -= minutes * 60;
    }
    seconds = elapsedSeconds;

    if (hours > 0) {
      return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
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
   * @param metricUnits true to use metric units. False to use imperial units
   */
  public static String formatDistance(Context context, double distance, boolean metricUnits) {
    if (Double.isNaN(distance) || Double.isInfinite(distance)) {
      return context.getString(R.string.value_unknown);
    }
    if (metricUnits) {
      if (distance > 500.0) {
        distance *= UnitConversions.M_TO_KM;
        return context.getString(R.string.value_float_kilometer, distance);
      } else {
        return context.getString(R.string.value_float_meter, distance);
      }
    } else {
      if (distance * UnitConversions.M_TO_MI > 0.5) {
        distance *= UnitConversions.M_TO_MI;
        return context.getString(R.string.value_float_mile, distance);
      } else {
        distance *= UnitConversions.M_TO_FT;
        return context.getString(R.string.value_float_feet, distance);
      }
    }
  }

  /**
   * Gets the distance in an array of two strings. The first string is the
   * distance. The second string is the unit. The first string is null if the
   * distance is invalid.
   * 
   * @param context the context
   * @param distance the distance
   * @param metricUnits true to use metric unit
   */
  public static String[] getDistanceParts(Context context, double distance, boolean metricUnits) {
    String[] result = new String[2];
    if (Double.isNaN(distance) || Double.isInfinite(distance)) {
      result[0] = null;
      result[1] = context.getString(metricUnits ? R.string.unit_meter : R.string.unit_feet);
      return result;
    }

    int unitId;
    if (metricUnits) {
      if (distance > 500.0) {
        distance *= UnitConversions.M_TO_KM;
        unitId = R.string.unit_kilometer;
      } else {
        unitId = R.string.unit_meter;
      }
    } else {
      if (distance * UnitConversions.M_TO_MI > 0.5) {
        distance *= UnitConversions.M_TO_MI;
        unitId = R.string.unit_mile;
      } else {
        distance *= UnitConversions.M_TO_FT;
        unitId = R.string.unit_feet;
      }
    }
    result[0] = String.format("%.2f", distance);
    result[1] = context.getString(unitId);
    return result;
  }

  /**
   * Gets the speed in an array of two strings. The first string is the speed.
   * The second string is the unit. The first string is null if speed is
   * invalid.
   * 
   * @param context the context
   * @param speed the speed
   * @param metricUnits true to use metric unit
   * @param reportSpeed true to report speed
   */
  public static String[] getSpeedParts(
      Context context, double speed, boolean metricUnits, boolean reportSpeed) {
    String[] result = new String[2];
    int unitId;
    if (metricUnits) {
      unitId = reportSpeed ? R.string.unit_kilometer_per_hour : R.string.unit_minute_per_kilometer;
    } else {
      unitId = reportSpeed ? R.string.unit_mile_per_hour : R.string.unit_minute_per_mile;
    }
    result[1] = context.getString(unitId);
    if (Double.isNaN(speed) || Double.isInfinite(speed)) {
      result[0] = null;
      return result;
    }
    speed *= UnitConversions.MS_TO_KMH;
    if (!metricUnits) {
      speed *= UnitConversions.KM_TO_MI;
    }
    if (reportSpeed) {
      result[0] = String.format("%.2f", speed);
    } else {
      // convert from hours to minutes
      double pace = speed == 0 ? 0.0 : 60.0 / speed;
      int minutes = (int) pace;
      int seconds = (int) Math.round((pace - minutes) * 60.0);
      result[0] = String.format("%d:%02d", minutes, seconds);  
    }
    return result;
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
          + " (at position " + position.getErrorIndex() + ")");
    }

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

  /**
   * Gets the html.
   * 
   * @param context the context
   * @param resId the string resource id
   * @param formatArgs the string resource ids of the format arguments
   */
  public static Spanned getHtml(Context context, int resId, Object... formatArgs) {
    Object[] args = new Object[formatArgs.length];
    for (int i = 0; i < formatArgs.length; i++) {
      String url = context.getString((Integer) formatArgs[i]);
      args[i] = " <a href='" + url + "'>" + url + "</a> ";
    }
    return Html.fromHtml(context.getString(resId, args));
  }

  /**
   * Gets the frequency display options.
   * 
   * @param context the context
   * @param metricUnits true to display in metric units
   */
  public static String[] getFrequencyOptions(Context context, boolean metricUnits) {
    String[] values = context.getResources().getStringArray(R.array.frequency_values);
    String[] options = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      int value = Integer.parseInt(values[i]);
      if (value == PreferencesUtils.FREQUENCY_OFF) {
        options[i] = context.getString(R.string.value_off);
      } else if (value < 0) {
        options[i] = context.getString(
            metricUnits ? R.string.value_integer_kilometer : R.string.value_integer_mile,
            Math.abs(value));
      } else {
        options[i] = context.getString(R.string.value_integer_minute, value);
      }
    }
    return options;
  }
}
