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
package de.dennisguse.opentracks.util;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import de.dennisguse.opentracks.R;

/**
 * Various string manipulation methods.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils {

    private static final String TAG = StringUtils.class.getSimpleName();

    private static final String COORDINATE_DEGREE = "\u00B0";

    private StringUtils() {
    }

    /**
     * Formats the date and time_ms based on user's phone date/time_ms preferences.
     *
     * @param context the context
     * @param time_ms the time_ms in milliseconds
     */
    public static String formatDateTime(Context context, long time_ms) {
        return DateUtils.formatDateTime(context, time_ms, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE)
                + " " + DateUtils.formatDateTime(context, time_ms, DateUtils.FORMAT_SHOW_TIME);
    }

    /**
     * Formats the time using the ISO 8601 date time_ms format with fractional seconds in UTC time zone.
     *
     * @param time_ms the time in milliseconds
     */
    public static String formatDateTimeIso8601(long time_ms) {
        return Instant.ofEpochMilli(time_ms).toString();
    }

    /**
     * Formats the elapsed timed in the form "MM:SS" or "H:MM:SS".
     *
     * @param time_ms the time in milliseconds
     */
    public static String formatElapsedTime(long time_ms) {
        return DateUtils.formatElapsedTime((long) (time_ms * UnitConversions.MS_TO_S));
    }

    /**
     * Formats the elapsed time in the form "H:MM:SS".
     *
     * @param time_ms the time in milliseconds
     */
    public static String formatElapsedTimeWithHour(long time_ms) {
        String value = formatElapsedTime(time_ms);
        return TextUtils.split(value, ":").length == 2 ? "0:" + value : value;
    }

    /**
     * Formats the distance in meters.
     *
     * @param context     the context
     * @param distance_m  the distance_m
     * @param metricUnits true to use metric units. False to use imperial units
     */
    public static String formatDistance(Context context, double distance_m, boolean metricUnits) {
        if (Double.isNaN(distance_m) || Double.isInfinite(distance_m)) {
            return context.getString(R.string.value_unknown);
        }

        if (metricUnits) {
            if (distance_m > 500.0) {
                distance_m *= UnitConversions.M_TO_KM;
                return context.getString(R.string.value_float_kilometer, distance_m);
            } else {
                return context.getString(R.string.value_float_meter, distance_m);
            }
        } else {
            if (distance_m * UnitConversions.M_TO_MI > 0.5) {
                distance_m *= UnitConversions.M_TO_MI;
                return context.getString(R.string.value_float_mile, distance_m);
            } else {
                distance_m *= UnitConversions.M_TO_FT;
                return context.getString(R.string.value_float_feet, distance_m);
            }
        }
    }

    public static String formatSpeed(Context context, double speed_mps, boolean metricUnits, boolean reportSpeed) {
        if (Double.isNaN(speed_mps) || Double.isInfinite(speed_mps)) {
            speed_mps = 0;
        }

        double speed = speed_mps * UnitConversions.M_TO_KM; //KM p sec
        if (!metricUnits) {
            speed *= UnitConversions.KM_TO_MI;
        }

        if (reportSpeed) {
            if (metricUnits) {
                return context.getString(R.string.value_float_kilometer_hour, speed * UnitConversions.S_TO_HR);
            } else {
                return context.getString(R.string.value_float_mile_hour, speed * UnitConversions.S_TO_HR);
            }
        } else {
            int pace = speed == 0 ? 0 : (int) Math.round(1 / speed); //sec / [KM | MI]
            int minutes = pace / 60;
            int seconds = pace % 60;
            if (metricUnits) {
                return context.getString(R.string.value_pace_kilometer, minutes, seconds);
            } else {
                return context.getString(R.string.value_pace_mile, minutes, seconds);
            }
        }
    }

    private static String formatDecimal(double value) {
        return StringUtils.formatDecimal(value, 2);
    }

    /**
     * Format a decimal number while removing trailing zeros of the decimal part (if present).
     */
    public static String formatDecimal(double value, int decimalPlaces) {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(decimalPlaces);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return df.format(value);
    }

    /**
     * Formats a coordinate
     *
     * @param coordinate the coordinate
     */
    public static String formatCoordinate(double coordinate) {
        return Location.convert(coordinate, Location.FORMAT_DEGREES) + COORDINATE_DEGREE;
    }

    /**
     * Get the formatted distance with unit.
     *
     * @param context     the context
     * @param distance_m  the distance
     * @param metricUnits true to use metric unit
     * @return the formatted distance (or null) and it's unit as {@link Pair}
     */
    public static Pair<String, String> getDistanceParts(Context context, double distance_m, boolean metricUnits) {
        if (Double.isNaN(distance_m) || Double.isInfinite(distance_m)) {
            return new Pair<>(null, context.getString(metricUnits ? R.string.unit_meter : R.string.unit_feet));
        }

        int unitId;
        if (metricUnits) {
            if (distance_m > 500.0) {
                distance_m *= UnitConversions.M_TO_KM;
                unitId = R.string.unit_kilometer;
            } else {
                unitId = R.string.unit_meter;
            }
        } else {
            if (distance_m * UnitConversions.M_TO_MI > 0.5) {
                distance_m *= UnitConversions.M_TO_MI;
                unitId = R.string.unit_mile;
            } else {
                distance_m *= UnitConversions.M_TO_FT;
                unitId = R.string.unit_feet;
            }
        }
        return new Pair<>(formatDecimal(distance_m), context.getString(unitId));
    }

    /**
     * Gets the formatted speed with unit.
     *
     * @param context     the context
     * @param speed_mps   the speed
     * @param metricUnits true to use metric unit
     * @param reportSpeed true to report speed; false for pace
     * @return the formatted speed (or null) and it's unit as {@link Pair}
     */
    public static Pair<String, String> getSpeedParts(Context context, double speed_mps, boolean metricUnits, boolean reportSpeed) {
        int unitId;
        if (metricUnits) {
            unitId = reportSpeed ? R.string.unit_kilometer_per_hour : R.string.unit_minute_per_kilometer;
        } else {
            unitId = reportSpeed ? R.string.unit_mile_per_hour : R.string.unit_minute_per_mile;
        }
        String unitString = context.getString(unitId);


        if (Double.isNaN(speed_mps) || Double.isInfinite(speed_mps)) {
            speed_mps = 0;
        }

        double speed = speed_mps * UnitConversions.M_TO_KM; //KM p sec
        if (!metricUnits) {
            speed *= UnitConversions.KM_TO_MI;
        }

        if (reportSpeed) {
            return new Pair<>(StringUtils.formatDecimal(speed * UnitConversions.S_TO_HR, 1), unitString);
        }

        int pace = speed == 0 ? 0 : (int) Math.round(1 / speed); //sec / [KM | MI]

        int minutes = pace / 60;
        int seconds = pace % 60;
        return new Pair<>(String.format(Locale.US, "%d:%02d", minutes, seconds), unitString);
    }

    /**
     * Gets a string for category.
     *
     * @param category the category
     */
    public static String getCategory(String category) {
        if (category == null || category.length() == 0) {
            return null;
        }
        return "[" + category + "]";
    }

    /**
     * Gets a string for category and description.
     *
     * @param category    the category
     * @param description the description
     */
    static String getCategoryDescription(String category, String description) {
        if (category == null || category.length() == 0) {
            return description;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(category).append("]");
        if (description != null && description.length() != 0) {
            builder.append(" ").append(description);
        }
        return builder.toString();
    }

    /**
     * Formats the given text as a XML CDATA element.
     * This includes adding the starting and ending CDATA tags.
     * NOTE: This may result in multiple consecutive CDATA tags.
     *
     * @param text the given text
     */
    public static String formatCData(String text) {
        return "<![CDATA[" + text.replaceAll("]]>", "]]]]><![CDATA[>") + "]]>";
    }

    /**
     * Gets the time, in milliseconds, from an XML date time string (ISO8601) as defined at http://www.w3.org/TR/xmlschema-2/#dateTime
     * Let's be lenient: if timezone information is not provided, UTC will be used.
     *
     * @param xmlDateTime the XML date time string
     */
    public static long parseTime(String xmlDateTime) {
        try {
            TemporalAccessor t = DateTimeFormatter.ISO_DATE_TIME.parseBest(xmlDateTime, ZonedDateTime::from, LocalDateTime::from);
            if (t instanceof LocalDateTime) {
                Log.w(TAG, "Date does not contain timezone information: using UTC.");
                t = ((LocalDateTime) t).atZone(ZoneOffset.UTC);
            }
            return Instant.from(t).toEpochMilli();
        } catch (Exception e) {
            Log.e(TAG, "Invalid XML dateTime value");
            throw e;
        }
    }

    /**
     * Gets the time as an array of three integers.
     * Index 0 contains the number of seconds, index 1 contains the number of minutes, and index 2 contains the number of hours.
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

        long seconds = (long) (time * UnitConversions.MS_TO_S);
        parts[0] = (int) (seconds % 60);
        int minutes = (int) (seconds / 60);
        parts[1] = minutes % 60;
        parts[2] = minutes / 60;
        return parts;
    }

    /**
     * Gets the frequency display options.
     *
     * @param context     the context
     * @param metricUnits true to display in metric units
     */
    public static String[] getFrequencyOptions(Context context, boolean metricUnits) {
        String[] values = context.getResources().getStringArray(R.array.frequency_values);
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            int value = Integer.parseInt(values[i]);
            if (context.getString(R.string.frequency_off).equals(values[i])) {
                options[i] = context.getString(R.string.value_off);
            } else if (value < 0) {
                options[i] = context.getString(metricUnits ? R.string.value_integer_kilometer : R.string.value_integer_mile, Math.abs(value));
            } else {
                options[i] = context.getString(R.string.value_integer_minute, value);
            }
        }
        return options;
    }

    /**
     * Sets an elevation_m value.
     *
     * @param context     the context
     * @param elevation_m the elevation_m
     * @param metricUnits true if metric units
     * @return the formatted elevation_m (or null) and it's unit as {@link Pair}
     */
    public static Pair<String, String> formatElevation(Context context, Float elevation_m, boolean metricUnits) {
        String value = context.getString(R.string.value_unknown);
        String unit = context.getString(metricUnits ? R.string.unit_meter : R.string.unit_feet);
        if (elevation_m != null) {
            if (!metricUnits) {
                elevation_m *= (float) UnitConversions.M_TO_FT;
            }
            value = StringUtils.formatDecimal(elevation_m, 0);
        }
        return new Pair<>(value, unit);
    }

    public static String valueInParentheses(String text) {
        return "(" + text + ")";
    }
}
