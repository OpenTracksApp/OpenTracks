package de.dennisguse.opentracks.services.announcement;

import static android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceHeartRate;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TtsSpan;

import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.intervals.IntervalStatistics;

class VoiceAnnouncementUtils {

    private VoiceAnnouncementUtils() {
    }

    static Spannable getAnnouncement(
            Context context,
            TrackStatistics trackStatistics,
            boolean isMetricUnits,
            boolean isReportSpeed,
            @Nullable IntervalStatistics.Interval currentInterval,
            @Nullable SensorStatistics sensorStatistics
    ) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Distance distance = trackStatistics.getTotalDistance();
        Speed distancePerTime = trackStatistics.getAverageMovingSpeed();
        Speed currentDistancePerTime = currentInterval != null ? currentInterval.getSpeed() : null;
        int perUnitStringId = isMetricUnits ? R.string.voice_per_kilometer : R.string.voice_per_mile;

        double distanceInUnit = distance.toKM_Miles(isMetricUnits);
        int distanceId = isMetricUnits ? R.plurals.voiceDistanceKilometers : R.plurals.voiceDistanceMiles;

        builder.append(context.getString(R.string.total_distance));
        appendDecimalUnit(
                builder,
                // This string is not actually spoken
                context.getResources().getQuantityString(distanceId, getQuantityCount(distanceInUnit), distanceInUnit),
                String.format("%.2f", distanceInUnit),
                // Units should always be english singular
                isMetricUnits ? "kilometer" : "mile"
        );
        // Punctuation helps introduce natural pauses in TTS
        builder.append(",");
        if (distance.isZero()) {
            return builder;
        }

        // Announce time
        Duration movingTime = trackStatistics.getMovingTime();
        if (!movingTime.isZero()) {
            appendDuration(
                    context,
                    builder,
                    movingTime
            );
            builder.append(",");
        }

        if (isReportSpeed) {
            int speedId = isMetricUnits ? R.plurals.voiceSpeedKilometersPerHour : R.plurals.voiceSpeedMilesPerHour;
            double speedInUnit = distancePerTime.to(isMetricUnits);

            builder.append(" ").append(context.getString(R.string.speed));
            appendDecimalUnit(
                    builder,
                    context.getResources().getQuantityString(speedId, getQuantityCount(speedInUnit), speedInUnit),
                    String.format("%.1f", speedInUnit),
                    isMetricUnits ? "kilometer per hour" : "mile per hour"
            );
            builder.append(",");

            if (currentDistancePerTime != null) {
                double currentDistancePerTimeInUnit = currentDistancePerTime.to(isMetricUnits);

                if (currentDistancePerTimeInUnit > 0) {

                    builder.append(" ").append(context.getString(R.string.lap_speed));
                    appendDecimalUnit(
                            builder,
                            context.getResources().getQuantityString(speedId, getQuantityCount(currentDistancePerTimeInUnit), currentDistancePerTimeInUnit),
                            String.format("%.1f", currentDistancePerTimeInUnit),
                            isMetricUnits ? "kilometer per hour" : "mile per hour"
                    );
                    builder.append(",");
                }
            }
        } else {
            Duration time = distancePerTime.toPace(isMetricUnits);
            builder.append(" ").append(context.getString(R.string.pace));
            appendDuration(
                    context,
                    builder,
                    time
            );
            builder.append(" ").append(context.getString(perUnitStringId))
                    .append(",");

            Duration currentTime = currentDistancePerTime != null ? currentDistancePerTime.toPace(isMetricUnits) : Duration.ofMillis(0);
            if (!currentTime.isZero()) {
                builder.append(" ").append(context.getString(R.string.lap_time));
                appendDuration(
                        context,
                        builder,
                        currentTime
                );
                builder.append(" ").append(context.getString(perUnitStringId))
                        .append(",");
            }
        }

        if (shouldVoiceAnnounceHeartRate()) {
            if (sensorStatistics != null && sensorStatistics.hasHeartRate()) {
                int averageHeartRate = Math.round(sensorStatistics.getAvgHeartRate().getBPM());

                builder.append(" ").append(context.getString(R.string.average_heart_rate));
                appendCardinal(
                        builder,
                        context.getString(R.string.sensor_state_heart_rate_value, averageHeartRate),
                        averageHeartRate
                );
                builder.append(",");
            }

            if (currentInterval != null && currentInterval.hasAverageHeartRate()) {
                int currentHeartRate = Math.round(currentInterval.getAverageHeartRate().getBPM());

                builder.append(" ").append(context.getString(R.string.current_heart_rate));
                appendCardinal(
                        builder,
                        context.getString(R.string.sensor_state_heart_rate_value, currentHeartRate),
                        currentHeartRate
                );
                builder.append(",");
            }
        }

        return builder;
    }

    static int getQuantityCount(double d) {
        return (int) d;
    }

    private static void appendDuration(
            Context context,
            SpannableStringBuilder builder,
            Duration duration
    ) {
        int hours = (int) (duration.toHours());
        int minutes = (int) (duration.toMinutes() % 60);
        int seconds = (int) (duration.getSeconds() % 60);

        if (hours > 0) {
            appendDecimalUnit(
                    builder,
                    context.getResources()
                            .getQuantityString(R.plurals.voiceHours, hours, hours),
                    String.format("%1$d", hours),
                    "hour"
            );
        }
        if (minutes > 0) {
            appendDecimalUnit(
                    builder,
                    context.getResources()
                            .getQuantityString(R.plurals.voiceMinutes, minutes, minutes),
                    String.format("%1$d", minutes),
                    "minute"
            );
        }
        if (seconds > 0 || duration.isZero()) {
            appendDecimalUnit(
                    builder,
                    context.getResources()
                            .getQuantityString(R.plurals.voiceSeconds, seconds, seconds),
                    String.format("%1$d", seconds),
                    "second"
            );
        }
    }

    /**
     * Speaks as: 98.14 [UNIT] - ninety eight point one four [UNIT with correct plural form]
     */
    private static void appendDecimalUnit(
            SpannableStringBuilder builder,
            String localizedText,
            String decimalValue,
            String unit
    ) {
        int decimalPoint = decimalValue.indexOf(".");

        TtsSpan.MeasureBuilder measureBuilder = new TtsSpan.MeasureBuilder()
                .setUnit(unit);

        if (decimalPoint == -1) {
            measureBuilder.setNumber(decimalValue);
        } else {
            measureBuilder.setIntegerPart(decimalValue.substring(0, decimalPoint))
                    .setFractionalPart(decimalValue.substring(decimalPoint + 1));
        }

        builder.append(" ")
                .append(
                        localizedText,
                        measureBuilder.build(),
                        SPAN_INCLUSIVE_EXCLUSIVE
                );
    }

    /**
     * Speaks as: 98 - ninety eight
     */
    private static void appendCardinal(
            SpannableStringBuilder builder,
            String localizedText,
            long number
    ) {
        builder.append(" ")
                .append(
                        localizedText,
                        new TtsSpan.CardinalBuilder()
                                .setNumber(number)
                                .build(),
                        SPAN_INCLUSIVE_EXCLUSIVE
                );
    }
}

