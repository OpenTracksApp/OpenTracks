package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.annotation.Nullable;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

public class AnnouncementUtils {

    private AnnouncementUtils() {
    }

    public static String getAnnouncement(Context context, TrackStatistics trackStatistics, String category, @Nullable IntervalStatistics.Interval currentInterval) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(context);
        boolean reportSpeed = PreferencesUtils.isReportSpeed(context, category);
        double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        double distancePerTime = trackStatistics.getAverageMovingSpeed() * UnitConversions.MPS_TO_KMH;
        double currentDistancePerTime = currentInterval != null ? currentInterval.getSpeed_ms() * UnitConversions.MPS_TO_KMH : 0;

        if (distance == 0) {
            return context.getString(R.string.voice_total_distance_zero);
        }

        if (!metricUnits) {
            distance *= UnitConversions.KM_TO_MI;
            distancePerTime *= UnitConversions.KM_TO_MI;
            currentDistancePerTime *= UnitConversions.KM_TO_MI;
        }

        String rate;
        String currentRate;
        String currentRateMsg;
        if (reportSpeed) {
            int speedId = metricUnits ? R.plurals.voiceSpeedKilometersPerHour : R.plurals.voiceSpeedMilesPerHour;
            rate = context.getResources().getQuantityString(speedId, getQuantityCount(distancePerTime), distancePerTime);

            currentRate = context.getResources().getQuantityString(speedId, getQuantityCount(currentDistancePerTime), currentDistancePerTime);
            currentRateMsg = context.getString(R.string.voice_speed_lap, currentRate);
        } else {
            double timePerDistance = distancePerTime == 0 ? 0.0 : 1 / distancePerTime;
            int paceId = metricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
            Duration time = Duration.ofMillis((long) (timePerDistance * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS));
            rate = context.getString(paceId, getAnnounceTime(context, time));

            double currentTimePerDistance = currentDistancePerTime == 0 ? 0.0 : 1 / currentDistancePerTime;
            Duration currentTime = Duration.ofMillis((long) (currentTimePerDistance * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS));
            currentRate = context.getString(paceId, getAnnounceTime(context, currentTime));
            currentRateMsg = context.getString(R.string.voice_pace_lap, currentRate);
        }

        int totalDistanceId = metricUnits ? R.plurals.voiceTotalDistanceKilometers : R.plurals.voiceTotalDistanceMiles;
        String totalDistance = context.getResources().getQuantityString(totalDistanceId, getQuantityCount(distance), distance);

        currentRateMsg = currentInterval == null ? "" : " " + currentRateMsg;

        return context.getString(R.string.voice_template, totalDistance, getAnnounceTime(context, trackStatistics.getMovingTime()), rate) + currentRateMsg;
    }

    //TODO We might need to localize this using strings.xml if order is relevant.
    private static String getAnnounceTime(Context context, Duration duration) {
        String result = "";

        int hours = (int) (duration.getSeconds() / (60 * 60));
        int minutes = (int) (duration.getSeconds() / 60);
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

