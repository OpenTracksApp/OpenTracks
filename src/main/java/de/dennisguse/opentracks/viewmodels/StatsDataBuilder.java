package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.util.StringUtils;

public class StatsDataBuilder {

    public static List<StatsData> fromRecordingData(@NonNull Context context, @NonNull TrackRecordingService.RecordingData recordingData, @NonNull Layout layout, boolean metricUnits) {
        List<StatsData> statsDataList = layout.getFields().stream()
                .filter(Layout.Field::isVisible)
                .map(field -> build(context, recordingData, field.getTitle(), field.isPrimary(), metricUnits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        statsDataList.addAll(getSensorStatsDataIfNeeded(context, recordingData, statsDataList, metricUnits));
        return statsDataList;
    }

    private static StatsData build(@NonNull Context context, @NonNull TrackRecordingService.RecordingData recordingData, @NonNull String fieldTitle, boolean isPrimary, boolean metricUnits) {
        final TrackPoint latestTrackPoint = recordingData.getLatestTrackPoint();
        final SensorDataSet sensorDataSet = recordingData.getSensorDataSet();

        String mainDescription = null;
        String secondaryDescription = null;
        Pair<String, String> valueAndUnit = null;
        boolean isWide = false;
        switch (fieldTitle) {
            case "total_time":
                valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getTotalTime()), null);
                mainDescription = context.getString(R.string.stats_total_time);
                break;
            case "moving_time":
                valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getMovingTime()), null);
                mainDescription = context.getString(R.string.stats_moving_time);
                break;
            case "distance":
                valueAndUnit = StringUtils.getDistanceParts(context, recordingData.getTrackStatistics().getTotalDistance(), metricUnits);
                mainDescription = context.getString(R.string.stats_distance);
                break;
            case "speed":
            case "pace":
                boolean reportSpeed = fieldTitle.equals("speed");
                mainDescription = fieldTitle.equals("speed") ? context.getString(R.string.stats_speed) : context.getString(R.string.stats_pace);
                Speed speed = latestTrackPoint != null && latestTrackPoint.hasSpeed() ? latestTrackPoint.getSpeed() : null;
                if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null && sensorDataSet.getCyclingDistanceSpeed().hasValue() && sensorDataSet.getCyclingDistanceSpeed().isRecent()) {
                    valueAndUnit = StringUtils.getSpeedParts(context, sensorDataSet.getCyclingDistanceSpeed().getValue().getSpeed(), metricUnits, reportSpeed);
                    secondaryDescription = context.getString(R.string.description_speed_source_sensor, sensorDataSet.getCyclingDistanceSpeed().getSensorNameOrAddress());
                } else {
                    valueAndUnit = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
                    secondaryDescription = context.getString(R.string.description_speed_source_gps);
                }
                break;
            case "average_moving_speed":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, true);
                mainDescription = context.getString(R.string.stats_average_moving_speed);
                break;
            case "average_speed":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, true);
                mainDescription = context.getString(R.string.stats_average_speed);
                break;
            case "max_speed":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true);
                mainDescription = context.getString(R.string.stats_max_speed);
                break;
            case "average_moving_pace":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, false);
                mainDescription = context.getString(R.string.stats_average_moving_pace);
                break;
            case "average_pace":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, false);
                mainDescription = context.getString(R.string.stats_average_pace);
                break;
            case "fastest_pace":
                valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true);
                mainDescription = context.getString(R.string.stats_fastest_pace);
                break;
            case "altitude":
                Float altitude = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? (float) latestTrackPoint.getAltitude().toM() : null;
                mainDescription = context.getString(R.string.stats_altitude);
                secondaryDescription = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? context.getString(latestTrackPoint.getAltitude().getLabelId()) : null;
                valueAndUnit = StringUtils.getAltitudeParts(context, altitude, metricUnits);
                break;
            case "gain":
                valueAndUnit = StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeGain(), metricUnits);
                mainDescription = context.getString(R.string.stats_gain);
                break;
            case "loss":
                valueAndUnit = StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeLoss(), metricUnits);
                mainDescription = context.getString(R.string.stats_loss);
                break;
            case "coordinates":
                mainDescription = context.getString(R.string.stats_coordinates);
                isWide = true;
                if (latestTrackPoint != null && latestTrackPoint.hasLocation()) {
                    valueAndUnit = new Pair<>(StringUtils.formatCoordinate(latestTrackPoint.getLatitude(), latestTrackPoint.getLongitude()), null);
                } else {
                    valueAndUnit = new Pair<>(context.getString(R.string.value_none), null);
                }
                break;
            case "heart_rate":
                mainDescription = context.getString(R.string.stats_sensors_heart_rate);
                if (sensorDataSet != null && sensorDataSet.getHeartRate() != null && sensorDataSet.getHeartRate().hasValue() && sensorDataSet.getHeartRate().isRecent()) {
                    valueAndUnit = StringUtils.getHeartRateParts(context, sensorDataSet.getHeartRate().getValue());
                    secondaryDescription = sensorDataSet.getHeartRate().getSensorNameOrAddress();
                } else {
                    valueAndUnit = StringUtils.getHeartRateParts(context, null);
                    secondaryDescription = context.getString(R.string.value_none);
                }
                break;
            case "cadence":
                if (sensorDataSet != null && sensorDataSet.getCyclingCadence() != null && sensorDataSet.getCyclingCadence().hasValue() && sensorDataSet.getCyclingCadence().isRecent()) {
                    valueAndUnit = StringUtils.getCadenceParts(context, sensorDataSet.getCyclingCadence().getValue());
                    secondaryDescription = sensorDataSet.getCyclingCadence().getSensorNameOrAddress();
                } else {
                    valueAndUnit = StringUtils.getCadenceParts(context, null);
                    secondaryDescription = context.getString(R.string.value_none);

                }
                break;
            case "power":
                if (sensorDataSet != null && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue() && sensorDataSet.getCyclingPower().isRecent()) {
                    valueAndUnit = StringUtils.getPowerParts(context, sensorDataSet.getCyclingPower().getValue());
                    secondaryDescription = sensorDataSet.getCyclingPower().getSensorNameOrAddress();
                } else {
                    valueAndUnit = StringUtils.getPowerParts(context, null);
                    secondaryDescription = context.getString(R.string.value_none);
                }
                break;
        }

        if (valueAndUnit == null) {
            return null;
        }
        return new StatsData(valueAndUnit, mainDescription, secondaryDescription, isPrimary, isWide);
    }

    /**
     * Builds a list of StatsData with sensors connected but not in statsDataList.
     */
    private static List<StatsData> getSensorStatsDataIfNeeded(Context context, TrackRecordingService.RecordingData recordingData, List<StatsData> statsDataList, boolean metricUnits) {
        List<StatsData> sensorDataList = new ArrayList<>();
        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        if (sensorDataSet != null) {
            sensorDataList.add(build(context, recordingData, "heart_rate", true, metricUnits));
            sensorDataList.add(build(context, recordingData, "cadence", true, metricUnits));
            sensorDataList.add(build(context, recordingData, "power", true, metricUnits));
        }

        return sensorDataList;
    }

    public static List<StatsData> fromSensorStatistics(@NonNull Context context, @NonNull SensorStatistics sensorStatistics) {
        List<StatsData> sensorDataList = new ArrayList<>();

        if (sensorStatistics.hasHeartRate()) {
            sensorDataList.add(
                    new StatsData(
                            StringUtils.getHeartRateParts(context, sensorStatistics.getMaxHeartRate()),
                            context.getString(R.string.sensor_state_heart_rate_max),
                            null,
                            true,
                            false
                    )
            );
            sensorDataList.add(
                    new StatsData(
                            StringUtils.getHeartRateParts(context, sensorStatistics.getAvgHeartRate()),
                            context.getString(R.string.sensor_state_heart_rate_avg),
                            null,
                            true,
                            false
                    )
            );
        }
        if (sensorStatistics.hasCadence()) {
            sensorDataList.add(
                    new StatsData(
                            StringUtils.getCadenceParts(context, sensorStatistics.getMaxCadence()),
                            context.getString(R.string.sensor_state_cadence_max),
                            null,
                            true,
                            false
                    )
            );
            sensorDataList.add(
                    new StatsData(
                            StringUtils.getCadenceParts(context, sensorStatistics.getAvgCadence()),
                            context.getString(R.string.sensor_state_cadence_avg),
                            null,
                            true,
                            false
                    )
            );
        }
        if (sensorStatistics.hasPower()) {
            sensorDataList.add(
                    new StatsData(
                            StringUtils.getPowerParts(context, sensorStatistics.getAvgPower()),
                            context.getString(R.string.sensor_state_power_avg),
                            null,
                            true,
                            false
                    )
            );
        }

        return sensorDataList;
    }
}
