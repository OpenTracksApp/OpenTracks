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

import static java.time.temporal.ChronoUnit.DAYS;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.settings.UnitSystem;

/**
 * Various string manipulation methods.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StringUtils {

    private static final String TAG = StringUtils.class.getSimpleName();
    public static final String EMPTY = "";

    private StringUtils() {
    }

    /**
     * Formats the date and time with the offset (using default Locale format).
     */
    public static String formatDateTimeWithOffset(OffsetDateTime odt) {
        return odt.toZonedDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
    }

    public static String formatDateTimeWithOffsetIfDifferent(OffsetDateTime odt) {
        if (!odt.getOffset().equals(OffsetDateTime.now().getOffset())) {
            return odt.toZonedDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
        }
        return odt.toZonedDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
    }

    /**
     * Formats the date relative to today date.
     */
    public static String formatDateTodayRelative(Context context, OffsetDateTime odt) {
        LocalDate today = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate();
        LocalDate ld = odt.toLocalDate();
        long daysBetween = DAYS.between(ld, today);

        if (daysBetween == 0) {
            // Today
            return context.getString(R.string.generic_today);
        } else if (daysBetween == 1) {
            // Yesterday
            return context.getString(R.string.generic_yesterday);
        } else if (daysBetween < 7) {
            // Name of the week day
            return ld.format(DateTimeFormatter.ofPattern("EEEE"));
        } else if (today.getYear() == ld.getYear()) {
            // Short date without year
            return ld.format(DateTimeFormatter.ofPattern("d MMM"));
        } else {
            // Short date with year
            return ld.format(DateTimeFormatter.ofPattern("d MMM y"));
        }
    }

    /**
     * Formats the time using the ISO 8601 date time format with fractional seconds.
     */
    public static String formatDateTimeIso8601(@NonNull Instant time, ZoneOffset zoneOffset) {
        return time
                .atOffset(zoneOffset)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Formats the elapsed timed in the form "MM:SS" or "H:MM:SS".
     */
    public static String formatElapsedTime(@NonNull Duration time) {
        return DateUtils.formatElapsedTime(time.getSeconds());
    }

    /**
     * Formats the elapsed time in the form "H:MM:SS".
     */
    public static String formatElapsedTimeWithHour(@NonNull Duration time) {
        String value = formatElapsedTime(time);
        return TextUtils.split(value, ":").length == 2 ? "0:" + value : value;
    }


    /**
     * Format a decimal number while removing trailing zeros of the decimal part (if present).
     */
    public static String formatDecimal(double value, int decimalPlaces) {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(decimalPlaces);
        df.setMaximumFractionDigits(decimalPlaces);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return df.format(value);
    }

    public static String formatCoordinate(Context context, Position position) {
        return context.getString(R.string.location_latitude_longitude, Location.convert(position.latitude(), Location.FORMAT_DEGREES), Location.convert(position.longitude(), Location.FORMAT_DEGREES));
    }

    public static Pair<String, String> getHeartRateParts(Context context, HeartRate heartrate) {
        String value = context.getString(R.string.value_unknown);
        if (heartrate != null) {
            value = StringUtils.formatDecimal(heartrate.getBPM(), 0);
        }

        return new Pair<>(value, context.getString(R.string.sensor_unit_beats_per_minute));
    }

    public static Pair<String, String> getCadenceParts(Context context, Cadence cadence) {
        String value = context.getString(R.string.value_unknown);
        if (cadence != null) {
            value = StringUtils.formatDecimal(cadence.getRPM(), 0);
        }

        return new Pair<>(value, context.getString(R.string.sensor_unit_rounds_per_minute));
    }

    public static Pair<String, String> getPowerParts(Context context, Power power) {
        String value = context.getString(R.string.value_unknown);
        if (power != null) {
            value = StringUtils.formatDecimal(power.getW(), 0);
        }

        return new Pair<>(value, context.getString(R.string.sensor_unit_power));
    }

    @Deprecated //TODO Move to strings.xml
    public static String getCategory(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }
        return "[" + category + "]";
    }

    @Deprecated //TODO use separate UI elements rather than concatenating strings.
    public static String getCategoryDescription(String category, String description) {
        if (category == null || category.isEmpty()) {
            return description;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getCategory(category));
        if (description != null && !description.isEmpty()) {
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
     * Gets the time, in milliseconds, from an XML date time string (ISO8601) as defined at <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">...</a>
     * Let's be lenient: if timezone information is not provided, UTC will be used.
     *
     * @param xmlDateTime the XML date time string
     */
    public static OffsetDateTime parseTime(String xmlDateTime) {
        try {
            TemporalAccessor t = DateTimeFormatter.ISO_DATE_TIME.parseBest(xmlDateTime, ZonedDateTime::from, LocalDateTime::from);
            if (t instanceof LocalDateTime localDateTime) {
                Log.w(TAG, "Date does not contain timezone information: using UTC.");
                t = localDateTime.atZone(ZoneOffset.UTC);
            }
            return OffsetDateTime.from(t);
        } catch (Exception e) {
            Log.e(TAG, "Invalid XML dateTime value");
            throw e;
        }
    }

    /**
     * @return the formatted altitude_m (or null) and it's unit as {@link Pair}
     */
    //TODO altitude_m should be double or a value object
    public static Pair<String, String> getAltitudeParts(Context context, Float altitude_m, UnitSystem unitSystem) {
        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(0)
                .setThreshold(Double.MAX_VALUE)
                .setUnit(unitSystem)
                .build(context);

        Distance distance = altitude_m != null ? Distance.of(altitude_m) : Distance.of((Double) null);
        return formatter.getDistanceParts(distance);
    }

    public static String formatAltitude(Context context, Float altitude_m, UnitSystem unitSystem) {
        Pair<String, String> altitudeParts = getAltitudeParts(context, altitude_m, unitSystem);

        return context.getString(R.string.altitude_with_unit, altitudeParts.first, altitudeParts.second);
    }

    public static String valueInParentheses(String text) {
        return "(" + text + ")";
    }
}
