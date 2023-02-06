package de.dennisguse.opentracks.services.announcement;

import static android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceAverageHeartRate;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceAverageSpeedPace;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceLapHeartRate;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceLapSpeedPace;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceMovingTime;
import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceTotalDistance;

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
        Distance totalDistance = trackStatistics.getTotalDistance();
        Speed averageMovingSpeed = trackStatistics.getAverageMovingSpeed();
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
            case NAUTICAL_IMPERIAL:
                perUnitStringId = R.string.voice_per_nautical_mile;
                distanceId = R.plurals.voiceDistanceNauticalMiles;
                speedId = R.plurals.voiceSpeedMKnots;
                unitDistanceTTS = "nautical mile";
                unitSpeedTTS = "knots";
                break;
            default:
                throw new UnsupportedOperationException("Not implemented");
        }

        double distanceInUnit = totalDistance.toKM_Miles(unitSystem);

        if (shouldVoiceAnnounceTotalDistance()) {
            builder.append(context.getString(R.string.total_distance));
            // Units should always be english singular for TTS.
            // See https://developer.android.com/reference/android/text/style/TtsSpan?hl=en#TYPE_MEASURE
            appendDecimalUnit(builder, context.getResources().getQuantityString(distanceId, getQuantityCount(distanceInUnit), distanceInUnit), distanceInUnit, 1, unitDistanceTTS);
            // Punctuation helps introduce natural pauses in TTS
            builder.append(".");
        }
        if (totalDistance.isZero()) {
            return builder;
        }

        // Announce time
        Duration movingTime = trackStatistics.getMovingTime();
        if (shouldVoiceAnnounceMovingTime() && !movingTime.isZero()) {
            appendDuration(context, builder, movingTime);
            builder.append(".");
        }

        if (isReportSpeed) {
            if (shouldVoiceAnnounceAverageSpeedPace()) {
                double speedInUnit = averageMovingSpeed.to(unitSystem);
                builder.append(" ")
                        .append(context.getString(R.string.speed));
                appendDecimalUnit(builder, context.getResources().getQuantityString(speedId, getQuantityCount(speedInUnit), speedInUnit), speedInUnit, 1, unitSpeedTTS);
                builder.append(".");
            }
            if (shouldVoiceAnnounceLapSpeedPace() && currentDistancePerTime != null) {
                double currentDistancePerTimeInUnit = currentDistancePerTime.to(unitSystem);
                if (currentDistancePerTimeInUnit > 0) {
                    builder.append(" ")
                            .append(context.getString(R.string.lap_speed));
                    appendDecimalUnit(builder, context.getResources().getQuantityString(speedId, getQuantityCount(currentDistancePerTimeInUnit), currentDistancePerTimeInUnit), currentDistancePerTimeInUnit, 1, unitSpeedTTS);
                    builder.append(".");
                }
            }
        } else {
            if (shouldVoiceAnnounceAverageSpeedPace()) {
                Duration time = averageMovingSpeed.toPace(unitSystem);
                builder.append(" ")
                        .append(context.getString(R.string.pace));
                appendDuration(context, builder, time);
                builder.append(" ")
                        .append(context.getString(perUnitStringId))
                        .append(".");
            }

            if (shouldVoiceAnnounceLapSpeedPace() && currentDistancePerTime != null) {
                Duration currentTime = currentDistancePerTime.toPace(unitSystem);
                builder.append(" ")
                        .append(context.getString(R.string.lap_time));
                appendDuration(context, builder, currentTime);
                builder.append(" ")
                        .append(context.getString(perUnitStringId))
                        .append(".");
            }
        }

        if (shouldVoiceAnnounceAverageHeartRate() && sensorStatistics != null && sensorStatistics.hasHeartRate()) {
            int averageHeartRate = Math.round(sensorStatistics.getAvgHeartRate().getBPM());

            builder.append(" ")
                    .append(context.getString(R.string.average_heart_rate));
            appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, averageHeartRate), averageHeartRate);
            builder.append(".");
        }
        if (shouldVoiceAnnounceLapHeartRate() && currentInterval != null && currentInterval.hasAverageHeartRate()) {
            int currentHeartRate = Math.round(currentInterval.getAverageHeartRate().getBPM());

            builder.append(" ")
                    .append(context.getString(R.string.current_heart_rate));
            appendCardinal(builder, context.getString(R.string.sensor_state_heart_rate_value, currentHeartRate), currentHeartRate);
            builder.append(".");
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
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceHours, hours, hours), hours, 0, "hour");
        }
        if (minutes > 0) {
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceMinutes, minutes, minutes), minutes, 0, "minute");
        }
        if (seconds > 0 || duration.isZero()) {
            appendDecimalUnit(builder, context.getResources().getQuantityString(R.plurals.voiceSeconds, seconds, seconds), seconds, 0, "second");
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

