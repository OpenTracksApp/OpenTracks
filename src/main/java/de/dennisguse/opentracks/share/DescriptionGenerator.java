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

import android.content.Context;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Generates descriptions for tracks and markers.
 *
 * @author Jimmy Shih
 */
public class DescriptionGenerator {

    private static final String HTML_LINE_BREAK = "<br>";
    private static final String HTML_PARAGRAPH_SEPARATOR = "<p>";
    private static final String TEXT_LINE_BREAK = "\n";
    private static final String TEXT_PARAGRAPH_SEPARATOR = "\n\n";

    private final Context context;

    public DescriptionGenerator(Context context) {
        this.context = context;
    }

    /**
     * Generates a track description.
     *
     * @param track the track
     * @param html  true to output html, false to output plain text
     */
    public String generateTrackDescription(Track track, boolean html) {
        String paragraphSeparator = html ? HTML_PARAGRAPH_SEPARATOR : TEXT_PARAGRAPH_SEPARATOR;
        String lineBreak = html ? HTML_LINE_BREAK : TEXT_LINE_BREAK;
        StringBuilder builder = new StringBuilder();

        // Created by
        String creator = html
                ? "<a href='" + context.getString(R.string.app_web_url) + "'>" + context.getString(R.string.app_name) + "</a>"
                : context.getString(R.string.app_name);
        builder.append(creator);

        builder.append(paragraphSeparator);

        writeString(track.getName(), builder, R.string.generic_name_line, lineBreak);
        writeString(track.getActivityTypeLocalized(), builder, R.string.description_activity_type, lineBreak);
        writeString(track.getDescription(), builder, R.string.generic_description_line, lineBreak);
        builder.append(generateTrackStatisticsDescription(track.getTrackStatistics(), html));

        return builder.toString();
    }

    /**
     * Writes a string to a string builder.
     *
     * @param text      the string
     * @param builder   the string builder
     * @param resId     the resource id containing one string placeholder
     * @param lineBreak the line break
     */
    private void writeString(String text, StringBuilder builder, int resId, String lineBreak) {
        if (text == null || text.length() == 0) {
            text = context.getString(R.string.value_unknown);
        }
        builder.append(context.getString(resId, text));
        builder.append(lineBreak);
    }

    /**
     * Generates a description for a {@link TrackStatistics}.
     *
     * @param stats the track statistics
     * @param html  true to use "<br>" for line break instead of "\n"
     */
    private String generateTrackStatisticsDescription(TrackStatistics stats, boolean html) {
        String lineBreak = html ? HTML_LINE_BREAK : TEXT_LINE_BREAK;
        StringBuilder builder = new StringBuilder();

        // Total distance
        writeDistance(stats.getTotalDistance(), builder, R.string.description_total_distance, lineBreak);

        // Total time
        writeTime(stats.getTotalTime(), builder, R.string.description_total_time, lineBreak);

        // Moving time
        writeTime(stats.getMovingTime(), builder, R.string.description_moving_time, lineBreak);

        // Average speed
        writeSpeed(stats.getAverageSpeed(), builder, R.string.description_average_speed, lineBreak);

        // Average moving speed
        writeSpeed(stats.getAverageMovingSpeed(), builder, R.string.description_average_moving_speed, lineBreak);

        // Max speed
        writeSpeed(stats.getMaxSpeed(), builder, R.string.description_max_speed, lineBreak);

        // Average pace
        writePace(stats.getAverageSpeed(), builder, R.string.description_average_pace_in_minute, lineBreak);

        // Average moving pace
        writePace(stats.getAverageMovingSpeed(), builder, R.string.description_average_moving_pace_in_minute, lineBreak);

        // Fastest pace
        writePace(stats.getMaxSpeed(), builder, R.string.description_fastest_pace_in_minute, lineBreak);

        // Max altitude
        if (stats.hasAltitudeMax()) {
            writeAltitude(stats.getMaxAltitude(), builder, R.string.description_max_altitude, lineBreak);
        }

        // Min altitude
        if (stats.hasAltitudeMin()) {
            writeAltitude(stats.getMinAltitude(), builder, R.string.description_min_altitude, lineBreak);
        }

        // Altitude gain
        if (stats.hasTotalAltitudeGain()) {
            writeAltitude(stats.getTotalAltitudeGain(), builder, R.string.description_altitude_gain, lineBreak);
        }

        // Altitude loss
        if (stats.hasTotalAltitudeLoss()) {
            writeAltitude(stats.getTotalAltitudeLoss(), builder, R.string.description_altitude_loss, lineBreak);
        }

        // Recorded time
        builder.append(context.getString(R.string.description_recorded_time, StringUtils.formatDateTimeWithOffset(OffsetDateTime.ofInstant(stats.getStartTime(), ZoneId.systemDefault()))));
        builder.append(lineBreak);

        return builder.toString();
    }

    /**
     * Writes distance.
     *
     * @param distance  distance in meters
     * @param builder   StringBuilder to append distance
     * @param resId     resource id of distance string
     * @param lineBreak line break string
     */
    @VisibleForTesting
    void writeDistance(Distance distance, StringBuilder builder, int resId, String lineBreak) {
        double distanceInKm = distance.toKM();
        double distanceInMi = distance.toMI();
        builder.append(context.getString(resId, distanceInKm, distanceInMi));
        builder.append(lineBreak);
    }

    /**
     * Writes time.
     *
     * @param time      time in milliseconds.
     * @param builder   StringBuilder to append time
     * @param resId     resource id of time string
     * @param lineBreak line break string
     */
    @VisibleForTesting
    void writeTime(Duration time, StringBuilder builder, int resId, String lineBreak) {
        builder.append(context.getString(resId, StringUtils.formatElapsedTime(time)));
        builder.append(lineBreak);
    }

    /**
     * Writes speed.
     *
     * @param speed     speed in meters per second
     * @param builder   StringBuilder to append speed
     * @param resId     resource id of speed string
     * @param lineBreak line break string
     */
    @VisibleForTesting
    void writeSpeed(Speed speed, StringBuilder builder, int resId, String lineBreak) {
        builder.append(context.getString(resId, speed.toKMH(), speed.toMPH()));
        builder.append(lineBreak);
    }

    /**
     * @param builder   StringBuilder to append pace
     * @param resId     resource id of pace string
     * @param lineBreak line break string
     */
    @VisibleForTesting
    void writePace(Speed speed, StringBuilder builder, int resId, String lineBreak) {
        Pair<String, String> paceInMetrics = SpeedFormatter.Builder().setUnit(UnitSystem.METRIC).setReportSpeedOrPace(false).build(context).getSpeedParts(speed);
        Pair<String, String> paceInImperial = SpeedFormatter.Builder().setUnit(UnitSystem.IMPERIAL_FEET).setReportSpeedOrPace(false).build(context).getSpeedParts(speed);

        String formattedPaceMetrics = paceInMetrics.first != null ? paceInMetrics.first : context.getString(R.string.value_unknown);
        String formattedPaceImperial = paceInImperial.first != null ? paceInImperial.first : context.getString(R.string.value_unknown);

        builder.append(context.getString(resId, formattedPaceMetrics, formattedPaceImperial));
        builder.append(lineBreak);
    }

    /**
     * @param altitudeM altitude_m in meters
     * @param builder    StringBuilder to append
     * @param resId      resource id of altitude string
     * @param lineBreak  line break string
     */
    @VisibleForTesting
    void writeAltitude(double altitudeM, StringBuilder builder, int resId, String lineBreak) {
        long altitudeInM = Math.round(altitudeM);
        long altitudeInFt = Math.round(Distance.of(altitudeM).toFT());
        builder.append(context.getString(resId, altitudeInM, altitudeInFt));
        builder.append(lineBreak);
    }
}
