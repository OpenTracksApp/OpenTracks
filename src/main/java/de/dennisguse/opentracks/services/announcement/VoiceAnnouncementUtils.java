package de.dennisguse.opentracks.services.announcement;

import static android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceAverageHeartRate;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceAverageSpeedPace;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceLapHeartRate;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceLapSpeedPace;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceMovingTime;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceTotalDistance;

import android.content.Context;
import android.icu.text.MessageFormat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TtsSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.Map;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.intervals.IntervalStatistics;

class VoiceAnnouncementUtils {

    private VoiceAnnouncementUtils() {
    }

    static Spannable createIdle(Context context) {
        return new SpannableStringBuilder()
                .append(context.getString(R.string.voiceIdle));
    }

    static Spannable createStatistics(Context context, TrackStatistics trackStatistics, UnitSystem unitSystem, boolean isReportSpeed, @Nullable IntervalStatistics.Interval currentInterval, @Nullable SensorStatistics sensorStatistics) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Distance totalDistance = trackStatistics.getTotalDistance();
        Speed averageMovingSpeed = trackStatistics.getAverageMovingSpeed();
        Speed intervalSpeed = currentInterval != null ? currentInterval.getSpeed() : null;

        if (shouldVoiceAnnounceTotalDistance()) {
            builder.append(context.getString(R.string.total_distance)).append(" ");
            builder.append(String.format( "%.1f", totalDistance.toKM_Miles(unitSystem) ));
            // Punctuation helps introduce natural pauses in TTS
            builder.append(". ");
        }
        if (totalDistance.isZero()) {
            return builder;
        }

        // Announce time
        Duration movingTime = trackStatistics.getMovingTime();
        if (shouldVoiceAnnounceMovingTime() && !movingTime.isZero()) {
            builder.append(context.getString(R.string.moving_time)).append(" ");
            appendDuration(context, builder, movingTime);
            builder.append(". ");
        }

        if (isReportSpeed) {
            if (shouldVoiceAnnounceAverageSpeedPace()) {
                builder.append(context.getString(R.string.average_speed)).append(" ");
                builder.append(String.format( "%.1f", averageMovingSpeed.to(unitSystem) ));
                builder.append(". ");
            }
            if (shouldVoiceAnnounceLapSpeedPace() && intervalSpeed != null) {
                double currentDistancePerTimeInUnit = intervalSpeed.to(unitSystem);
                if (currentDistancePerTimeInUnit > 0) {
                    builder.append(context.getString(R.string.speed)).append(" ");
                    builder.append(String.format( "%.1f", intervalSpeed.to(unitSystem) ));
                    builder.append(". ");
                }
            }
        } else {
            if (shouldVoiceAnnounceAverageSpeedPace()) {
                Duration time = averageMovingSpeed.toPace(unitSystem);
                builder.append(context.getString(R.string.average_pace)).append(" ");
                appendDuration(context, builder, time);
                builder.append(". ");
            }

            if (shouldVoiceAnnounceLapSpeedPace() && intervalSpeed != null) {
                Duration time = intervalSpeed.toPace(unitSystem);
                builder.append(context.getString(R.string.pace)).append(" ");
                appendDuration(context, builder, time);
                builder.append(". ");
            }
        }

        if (shouldVoiceAnnounceAverageHeartRate() && sensorStatistics != null && sensorStatistics.hasHeartRate()) {
            int averageHeartRate = Math.round(sensorStatistics.avgHeartRate().getBPM());

            builder.append(context.getString(R.string.average_heart_rate)).append(" ");
            appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, averageHeartRate), averageHeartRate);
            builder.append(". ");
        }
        if (shouldVoiceAnnounceLapHeartRate() && currentInterval != null && currentInterval.hasAverageHeartRate()) {
            int currentHeartRate = Math.round(currentInterval.getAverageHeartRate().getBPM());

            builder.append(context.getString(R.string.heart_rate)).append(" ");
            appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, currentHeartRate), currentHeartRate);
            builder.append(".");
        }

        return builder;
    }

    private static void appendDuration(@NonNull Context context, @NonNull SpannableStringBuilder builder, @NonNull Duration duration) {
        int hours = (int) (duration.toHours());
        int minutes = (int) (duration.toMinutes() % 60);
        int seconds = (int) (duration.getSeconds() % 60);

        if (hours > 0) {
            String template = context.getResources().getString(R.string.voiceHoursPlural);
            appendDecimalUnit(builder, MessageFormat.format(template, Map.of("n", hours)), hours, 0, "hour");
        }
        if (minutes > 0) {
            String template = context.getResources().getString(R.string.voiceMinutesPlural);
            appendDecimalUnit(builder, MessageFormat.format(template, Map.of("n", minutes)), minutes, 0, "minute");
        }
        if (seconds > 0 || duration.isZero()) {
            String template = context.getResources().getString(R.string.voiceSecondsPlural);
            appendDecimalUnit(builder, MessageFormat.format(template, Map.of("n", seconds)), seconds, 0, "second");
        }
    }

    /**
     * Speaks as: 98.14 [UNIT] - ninety eight point one four [UNIT with correct plural form]
     *
     * @param number The number to speak
     * @param precision The number of decimal places to announce
     */
    private static void appendDecimalUnit(@NonNull SpannableStringBuilder builder, @NonNull String localizedText, double number, int precision, @NonNull String unit) {
        TtsSpan.MeasureBuilder measureBuilder = new TtsSpan.MeasureBuilder()
                .setUnit(unit);

        if (precision == 0) {
            measureBuilder.setNumber((long)number);
        } else {
            // Round before extracting integral and decimal parts
            double roundedNumber = Math.round(Math.pow(10, precision) * number) / Math.pow(10.0, precision);
            long integerPart = (long) roundedNumber;
            // Extract the decimal part
            String fractionalPart = String.format("%." + precision + "f", (roundedNumber - integerPart)).substring(2);
            measureBuilder.setIntegerPart(integerPart)
                    .setFractionalPart(fractionalPart);
        }

        builder.append(" ")
                .append(localizedText, measureBuilder.build(), SPAN_INCLUSIVE_EXCLUSIVE);
    }

    /**
     * Speaks as: 98 - ninety eight
     */
    private static void appendCardinal(@NonNull SpannableStringBuilder builder, @NonNull String localizedText, long number) {
        builder.append(" ")
                .append(localizedText, new TtsSpan.CardinalBuilder().setNumber(number).build(), SPAN_INCLUSIVE_EXCLUSIVE);
    }
}

