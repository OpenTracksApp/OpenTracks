package de.dennisguse.opentracks.services.announcement;

import static android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceHeartRate;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TtsSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;

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

    static Spannable getAnnouncement(Context context, TrackStatistics trackStatistics, UnitSystem unitSystem, boolean isReportSpeed, @Nullable IntervalStatistics.Interval currentInterval, @Nullable SensorStatistics sensorStatistics) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Distance distance = trackStatistics.getTotalDistance();
        Speed distancePerTime = trackStatistics.getAverageMovingSpeed();
        Speed currentDistancePerTime = currentInterval != null ? currentInterval.getSpeed() : null;

        int perUnitStringId;
        int distanceId;
        int speedId;
        String unitDistanceTTS;
        String unitSpeedTTS;
        switch (unitSystem) {
            case METRIC:
                perUnitStringId = R.string.voice_per_kilometer;
                distanceId = R.plurals.voiceDistanceKilometers;
                speedId = R.plurals.voiceSpeedKilometersPerHour;
                unitDistanceTTS = "kilometer";
                unitSpeedTTS = "kilometer per hour";
                break;
            case IMPERIAL:
                perUnitStringId = R.string.voice_per_mile;
                distanceId = R.plurals.voiceDistanceMiles;
                speedId = R.plurals.voiceSpeedMilesPerHour;
                unitDistanceTTS = "mile";
                unitSpeedTTS = "mile per hour";
                break;
            default:
                throw new RuntimeException("Not implemented");
        }

        double distanceInUnit = distance.toKM_Miles(unitSystem);

        builder.append(context.getString(R.string.total_distance));
        long distanceIntegerPart = (long) distanceInUnit;
        // Extract the decimal part
        String distanceFractionalPart = String.format("%.2f", (distanceInUnit - distanceIntegerPart)).substring(2);
        // Units should always be english singular for TTS.
        // See https://developer.android.com/reference/android/text/style/TtsSpan?hl=en#TYPE_MEASURE
        appendDecimalUnit(builder, context.getResources().getQuantityString(distanceId, getQuantityCount(distanceInUnit), distanceInUnit), distanceIntegerPart, distanceFractionalPart, unitDistanceTTS);
        // Punctuation helps introduce natural pauses in TTS
        builder.append(".");
        if (distance.isZero()) {
            return builder;
        }

        // Announce time
        Duration movingTime = trackStatistics.getMovingTime();
        if (!movingTime.isZero()) {
            appendDuration(context, builder, movingTime);
            builder.append(".");
        }

        if (isReportSpeed) {
            double speedInUnit = distancePerTime.to(unitSystem);

            builder.append(" ")
                    .append(context.getString(R.string.speed));
            long speedIntegerPart = (long) speedInUnit;
            // Extract the decimal part
            String speedFractionalPart = String.format("%.1f", (speedInUnit - speedIntegerPart)).substring(2);
            appendDecimalUnit(builder, context.getResources().getQuantityString(speedId, getQuantityCount(speedInUnit), speedInUnit), speedIntegerPart, speedFractionalPart, unitSpeedTTS);
            builder.append(".");

            if (currentDistancePerTime != null) {
                double currentDistancePerTimeInUnit = currentDistancePerTime.to(unitSystem);

                if (currentDistancePerTimeInUnit > 0) {

                    builder.append(" ")
                            .append(context.getString(R.string.lap_speed));
                    long currentDistanceIntegerPart = (long) currentDistancePerTimeInUnit;
                    // Extract the decimal part
                    String currentDistanceFractionalPart = String.format("%.1f", (currentDistancePerTimeInUnit - currentDistanceIntegerPart)).substring(2);
                    appendDecimalUnit(builder, context.getResources().getQuantityString(speedId, getQuantityCount(currentDistancePerTimeInUnit), currentDistancePerTimeInUnit), currentDistanceIntegerPart, currentDistanceFractionalPart, unitSpeedTTS);
                    builder.append(".");
                }
            }
        } else {
            Duration time = distancePerTime.toPace(unitSystem);
            builder.append(" ")
                    .append(context.getString(R.string.pace));
            appendDuration(context, builder, time);
            builder.append(" ")
                    .append(context.getString(perUnitStringId))
                    .append(".");

            Duration currentTime = currentDistancePerTime != null ? currentDistancePerTime.toPace(unitSystem) : Duration.ofMillis(0);
            if (!currentTime.isZero()) {
                builder.append(" ")
                        .append(context.getString(R.string.lap_time));
                appendDuration(context, builder, currentTime);
                builder.append(" ")
                        .append(context.getString(perUnitStringId))
                        .append(".");
            }
        }

        if (shouldVoiceAnnounceHeartRate()) {
            if (sensorStatistics != null && sensorStatistics.hasHeartRate()) {
                int averageHeartRate = Math.round(sensorStatistics.getAvgHeartRate().getBPM());

                builder.append(" ")
                        .append(context.getString(R.string.average_heart_rate));
                appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, averageHeartRate), averageHeartRate);
                builder.append(".");
            }

            if (currentInterval != null && currentInterval.hasAverageHeartRate()) {
                int currentHeartRate = Math.round(currentInterval.getAverageHeartRate().getBPM());

                builder.append(" ")
                        .append(context.getString(R.string.current_heart_rate));
                appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, currentHeartRate), currentHeartRate);
                builder.append(".");
            }
        }

        return builder;
    }

    static int getQuantityCount(double d) {
        return (int) d;
    }

    private static void appendDuration(@NonNull Context context, @NonNull SpannableStringBuilder builder, @NonNull Duration duration) {
        int hours = (int) (duration.toHours());
        int minutes = (int) (duration.toMinutes() % 60);
        int seconds = (int) (duration.getSeconds() % 60);

        if (hours > 0) {
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceHours, hours, hours), hours, null, "hour");
        }
        if (minutes > 0) {
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceMinutes, minutes, minutes), minutes, null, "minute");
        }
        if (seconds > 0 || duration.isZero()) {
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceSeconds, seconds, seconds), seconds, null, "second");
        }
    }

    /**
     * Speaks as: 98.14 [UNIT] - ninety eight point one four [UNIT with correct plural form]
     */
    private static void appendDecimalUnit(@NonNull SpannableStringBuilder builder, @NonNull String localizedText, long integerPart, @Nullable String fractionalPart, @NonNull String unit) {
        TtsSpan.MeasureBuilder measureBuilder = new TtsSpan.MeasureBuilder()
                .setUnit(unit);

        if (fractionalPart == null) {
            measureBuilder.setNumber(integerPart);
        } else {
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

