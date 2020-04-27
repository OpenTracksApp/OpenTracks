package de.dennisguse.opentracks.util;

import android.content.Context;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class AnnouncementUtils {

    private AnnouncementUtils() {}

    public static String getAnnouncement(Context context, TrackStatistics trackStatistics, String category) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(context);
        boolean reportSpeed = PreferencesUtils.isReportSpeed(context, category);
        double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        double distancePerTime = trackStatistics.getAverageMovingSpeed() * UnitConversions.MS_TO_KMH;

        if (distance == 0) {
            return context.getString(R.string.voice_total_distance_zero);
        }

        if (!metricUnits) {
            distance *= UnitConversions.KM_TO_MI;
            distancePerTime *= UnitConversions.KM_TO_MI;
        }

        String rate;
        if (reportSpeed) {
            int speedId = metricUnits ? R.plurals.voiceSpeedKilometersPerHour : R.plurals.voiceSpeedMilesPerHour;
            rate = context.getResources().getQuantityString(speedId, getQuantityCount(distancePerTime), distancePerTime);
        } else {
            double timePerDistance = distancePerTime == 0 ? 0.0 : 1 / distancePerTime;
            int paceId = metricUnits ? R.string.voice_pace_per_kilometer : R.string.voice_pace_per_mile;
            long time = Math.round(timePerDistance * UnitConversions.HR_TO_MIN * UnitConversions.MIN_TO_S * UnitConversions.S_TO_MS);
            rate = context.getString(paceId, getAnnounceTime(context, time));
        }

        int totalDistanceId = metricUnits ? R.plurals.voiceTotalDistanceKilometers : R.plurals.voiceTotalDistanceMiles;
        String totalDistance = context.getResources().getQuantityString(totalDistanceId, getQuantityCount(distance), distance);

        return context.getString(R.string.voice_template, totalDistance, getAnnounceTime(context, trackStatistics.getMovingTime()), rate);
    }

    private static String getAnnounceTime(Context context, long time) {
        int[] parts = StringUtils.getTimeParts(time);
        String seconds = context.getResources()
                .getQuantityString(R.plurals.voiceSeconds, parts[0], parts[0]);
        String minutes = context.getResources()
                .getQuantityString(R.plurals.voiceMinutes, parts[1], parts[1]);
        String hours = context.getResources()
                .getQuantityString(R.plurals.voiceHours, parts[2], parts[2]);
        StringBuilder sb = new StringBuilder();
        if (parts[2] != 0) {
            sb.append(hours);
            sb.append(" ");
        }
        sb.append(minutes);
        sb.append(" ");
        sb.append(seconds);
        return sb.toString();
    }

    /**
     * Gets the plural count to be used by getQuantityString.
     * getQuantityString only supports integer quantities, not a double quantity like "2.2".
     * <p>
     * As a temporary workaround, we convert a double quantity to an integer quantity.
     * If the double quantity is exactly 0, 1, or 2, then we can return these integer quantities.
     * Otherwise, we cast the double quantity to an integer quantity.
     * However, we need to make sure that if the casted value is 0, 1, or 2, we don't return those, instead, return the next biggest integer 3.
     *
     * @param d the double value
     */
    private static int getQuantityCount(double d) {
        if (d == 0) {
            return 0;
        } else if (d == 1) {
            return 1;
        } else if (d == 2) {
            return 2;
        } else {
            //TODO This seems weird; why not use Math.round(d) or Math.ceil()?
            int count = (int) d;
            return Math.max(count, 3);
        }
    }
}

