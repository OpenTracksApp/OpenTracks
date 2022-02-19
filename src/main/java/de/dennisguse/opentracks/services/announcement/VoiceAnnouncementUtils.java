package de.dennisguse.opentracks.services.announcement;

import static de.dennisguse.opentracks.settings.PreferencesUtils.shouldVoiceAnnounceHeartRate;

import android.content.Context;

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

    static String getAnnouncement(
            Context context,
            TrackStatistics trackStatistics,
            boolean isMetricUnits,
            boolean isReportSpeed,
            @Nullable IntervalStatistics.Interval currentInterval,
            @Nullable SensorStatistics sensorStatistics
    ) {
        Distance distance = trackStatistics.getTotalDistance();
        Speed distancePerTime = trackStatistics.getAverageMovingSpeed();
        Speed currentDistancePerTime = currentInterval != null ? currentInterval.getSpeed() : null;

        int totalDistanceId = isMetricUnits ? R.plurals.voiceTotalDistanceKilometers : R.plurals.voiceTotalDistanceMiles;
        double distanceInUnit = distance.toKM_Miles(isMetricUnits);
        String totalDistance = context.getResources().getQuantityString(totalDistanceId, getQuantityCount(distanceInUnit), distanceInUnit);
        if (distance.isZero()) {
            return totalDistance;
        }

        String rate;
        String currentRate;
        String currentRateMsg;
        String heartRateMsg = "";
        if (isReportSpeed) {
            int speedId = isMetricUnits ? R.plurals.voiceSpeedKilometersPerHour : R.plurals.voiceSpeedMilesPerHour;
            double speedInUnit = distancePerTime.to(isMetricUnits);
            rate = context.getResources().getQuantityString(speedId, getQuantityCount(speedInUnit), speedInUnit);

            double currentDistancePerTimeInUnit = currentDistancePerTime != null ? currentDistancePerTime.to(isMetricUnits) : 0;
            currentRate = context.getResources().getQuantityString(speedId, getQuantityCount(currentDistancePerTimeInUnit), currentDistancePerTimeInUnit);
            currentRateMsg = context.getString(R.string.voice_speed_lap, currentRate);
        } else {
            Duration time = distancePerTime.toPace(isMetricUnits);

            int paceId = isMetricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
            rate = context.getString(paceId, getAnnounceTime(context, time));

            Duration currentTime = currentDistancePerTime != null ? currentDistancePerTime.toPace(isMetricUnits) : Duration.ofMillis(0);
            currentRate = context.getString(paceId, getAnnounceTime(context, currentTime));
            currentRateMsg = context.getString(R.string.voice_pace_lap, currentRate);
        }

        currentRateMsg = currentInterval == null ? "" : " " + currentRateMsg;

        if (shouldVoiceAnnounceHeartRate()) {
            if (sensorStatistics != null && sensorStatistics.hasHeartRate()) {
                heartRateMsg = context.getString(R.string.average_heart_rate, Math.round(sensorStatistics.getAvgHeartRate().getBPM()));
            }

            if (currentInterval != null && currentInterval.hasAverageHeartRate()) {
                if (!heartRateMsg.isEmpty()) {
                    heartRateMsg += " ";
                }
                heartRateMsg += context.getString(R.string.current_heart_rate, Math.round(currentInterval.getAverageHeartRate().getBPM()));
            }
        }
        heartRateMsg = heartRateMsg.isEmpty() ? "" : " " + heartRateMsg;

        return context.getString(R.string.voice_template, totalDistance, getAnnounceTime(context, trackStatistics.getMovingTime()), rate) + currentRateMsg + heartRateMsg;
    }

    //TODO We might need to localize this using strings.xml if order is relevant.
    private static String getAnnounceTime(Context context, Duration duration) {
        String result = "";

        int hours = (int) (duration.toHours());
        int minutes = (int) (duration.toMinutes() % 60);
        int seconds = (int) (duration.getSeconds() % 60);

        if (hours != 0) {
            String hoursText = context.getResources()
                    .getQuantityString(R.plurals.voiceHours, hours, hours);
            result += hoursText + " ";
        }
        String minutesText = context.getResources()
                .getQuantityString(R.plurals.voiceMinutes, minutes, minutes);
        String secondsText = context.getResources()
                .getQuantityString(R.plurals.voiceSeconds, seconds, seconds);

        return result + minutesText + " " + secondsText;
    }

    static int getQuantityCount(double d) {
        return (int) d;
    }
}

