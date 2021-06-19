package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.DataField;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.util.StringUtils;

public class StatisticDataBuilder {

    public static List<StatisticData> fromRecordingData(@NonNull Context context, @NonNull TrackRecordingService.RecordingData recordingData, @NonNull Layout layout, boolean metricUnits) {
        List<StatisticData> statisticDataList = layout.getFields().stream()
                .filter(DataField::isVisible)
                .map(field -> build(context, recordingData, field.getKey(), field.isPrimary(), metricUnits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        statisticDataList.addAll(getSensorStatsDataIfNeeded(context, recordingData, statisticDataList, metricUnits));
        return statisticDataList;
    }

    private static StatisticData build(@NonNull Context context, @NonNull TrackRecordingService.RecordingData recordingData, @NonNull String fieldKey, boolean isPrimary, boolean metricUnits) {
        final TrackPoint latestTrackPoint = recordingData.getLatestTrackPoint();
        final SensorDataSet sensorDataSet = recordingData.getSensorDataSet();

        String title = null;
        String description = null;
        Pair<String, String> valueAndUnit = null;
        boolean isWide = false;
        if (fieldKey.equals(context.getString(R.string.stats_custom_layout_total_time_key))) {
            valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getTotalTime()), null);
            title = context.getString(R.string.stats_total_time);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_moving_time_key))) {
            valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getMovingTime()), null);
            title = context.getString(R.string.stats_moving_time);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_distance_key))) {
            valueAndUnit = StringUtils.getDistanceParts(context, recordingData.getTrackStatistics().getTotalDistance(), metricUnits);
            title = context.getString(R.string.stats_distance);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_speed_key)) ||  fieldKey.equals(context.getString(R.string.stats_custom_layout_pace_key))) {
            boolean reportSpeed = fieldKey.equals("speed");
            title = fieldKey.equals("speed") ? context.getString(R.string.stats_speed) : context.getString(R.string.stats_pace);
            Speed speed = latestTrackPoint != null && latestTrackPoint.hasSpeed() ? latestTrackPoint.getSpeed() : null;
            if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null && sensorDataSet.getCyclingDistanceSpeed().hasValue() && sensorDataSet.getCyclingDistanceSpeed().isRecent()) {
                valueAndUnit = StringUtils.getSpeedParts(context, sensorDataSet.getCyclingDistanceSpeed().getValue().getSpeed(), metricUnits, reportSpeed);
                description = context.getString(R.string.description_speed_source_sensor, sensorDataSet.getCyclingDistanceSpeed().getSensorNameOrAddress());
            } else {
                valueAndUnit = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
                description = context.getString(R.string.description_speed_source_gps);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_moving_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_average_moving_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_average_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_max_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_max_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_moving_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, false);
            title = context.getString(R.string.stats_average_moving_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, false);
            title = context.getString(R.string.stats_average_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_fastest_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_fastest_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_altitude_key))) {
            Float altitude = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? (float) latestTrackPoint.getAltitude().toM() : null;
            title = context.getString(R.string.stats_altitude);
            description = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? context.getString(latestTrackPoint.getAltitude().getLabelId()) : null;
            valueAndUnit = StringUtils.getAltitudeParts(context, altitude, metricUnits);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_gain_key))) {
            valueAndUnit = StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeGain(), metricUnits);
            title = context.getString(R.string.stats_gain);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_loss_key))) {
            valueAndUnit = StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeLoss(), metricUnits);
            title = context.getString(R.string.stats_loss);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_coordinates_key))) {
            title = context.getString(R.string.stats_coordinates);
            isWide = true;
            if (latestTrackPoint != null && latestTrackPoint.hasLocation()) {
                valueAndUnit = new Pair<>(StringUtils.formatCoordinate(context, latestTrackPoint.getLatitude(), latestTrackPoint.getLongitude()), null);
            } else {
                valueAndUnit = new Pair<>(context.getString(R.string.value_none), null);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_heart_rate_key))) {
            title = context.getString(R.string.stats_sensors_heart_rate);
            if (sensorDataSet != null && sensorDataSet.getHeartRate() != null && sensorDataSet.getHeartRate().hasValue() && sensorDataSet.getHeartRate().isRecent()) {
                valueAndUnit = StringUtils.getHeartRateParts(context, sensorDataSet.getHeartRate().getValue());
                description = sensorDataSet.getHeartRate().getSensorNameOrAddress();
            } else {
                valueAndUnit = StringUtils.getHeartRateParts(context, null);
                description = context.getString(R.string.value_none);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_cadence_key))) {
            title = context.getString(R.string.stats_sensors_cadence);
            if (sensorDataSet != null && sensorDataSet.getCyclingCadence() != null && sensorDataSet.getCyclingCadence().hasValue() && sensorDataSet.getCyclingCadence().isRecent()) {
                valueAndUnit = StringUtils.getCadenceParts(context, sensorDataSet.getCyclingCadence().getValue());
                description = sensorDataSet.getCyclingCadence().getSensorNameOrAddress();
            } else {
                valueAndUnit = StringUtils.getCadenceParts(context, null);
                description = context.getString(R.string.value_none);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_power_key))) {
            title = context.getString(R.string.stats_sensors_power);
            if (sensorDataSet != null && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue() && sensorDataSet.getCyclingPower().isRecent()) {
                valueAndUnit = StringUtils.getPowerParts(context, sensorDataSet.getCyclingPower().getValue());
                description = sensorDataSet.getCyclingPower().getSensorNameOrAddress();
            } else {
                valueAndUnit = StringUtils.getPowerParts(context, null);
                description = context.getString(R.string.value_none);
            }
        }

        if (valueAndUnit == null) {
            return null;
        }

        return new StatisticData(new DataField(fieldKey, title, true, isPrimary, isWide), valueAndUnit, description);
    }

    /**
     * Builds a list of StatisticData with sensors connected but not in statisticDataList.
     */
    private static List<StatisticData> getSensorStatsDataIfNeeded(Context context, TrackRecordingService.RecordingData recordingData, List<StatisticData> statisticDataList, boolean metricUnits) {
        List<StatisticData> sensorDataList = new ArrayList<>();
        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        if (sensorDataSet == null) {
            return sensorDataList;
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_heart_rate))) && sensorDataSet.getHeartRate() != null && sensorDataSet.getHeartRate().hasValue() && sensorDataSet.getHeartRate().isRecent()) {
            sensorDataList.add(build(context, recordingData, "heart_rate", true, metricUnits));
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_cadence))) && sensorDataSet.getCyclingCadence() != null && sensorDataSet.getCyclingCadence().hasValue() && sensorDataSet.getCyclingCadence().isRecent()) {
            sensorDataList.add(build(context, recordingData, "cadence", true, metricUnits));
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_power))) && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue() && sensorDataSet.getCyclingPower().isRecent()) {
            sensorDataList.add(build(context, recordingData, "power", true, metricUnits));
        }

        return sensorDataList;
    }

    public static List<StatisticData> fromSensorStatistics(@NonNull Context context, @NonNull SensorStatistics sensorStatistics) {
        List<StatisticData> sensorDataList = new ArrayList<>();

        if (sensorStatistics.hasHeartRate()) {
            sensorDataList.add(
                    new StatisticData(
                            new DataField(context.getString(R.string.stats_custom_layout_heart_rate_key), context.getString(R.string.sensor_state_heart_rate_max), true, true, false),
                            StringUtils.getHeartRateParts(context, sensorStatistics.getMaxHeartRate()),
                            null
                    )
            );
            sensorDataList.add(
                    new StatisticData(
                            new DataField(context.getString(R.string.stats_custom_layout_average_heart_rate_key), context.getString(R.string.sensor_state_heart_rate_avg), true, true, false),
                            StringUtils.getHeartRateParts(context, sensorStatistics.getAvgHeartRate()),
                            null
                    )
            );
        }
        if (sensorStatistics.hasCadence()) {
            sensorDataList.add(
                    new StatisticData(
                            new DataField(context.getString(R.string.stats_custom_layout_cadence_key), context.getString(R.string.sensor_state_cadence_max), true, true, false),
                            StringUtils.getCadenceParts(context, sensorStatistics.getMaxCadence()),
                            null
                    )
            );
            sensorDataList.add(
                    new StatisticData(
                            new DataField(context.getString(R.string.stats_custom_layout_average_cadence_key), context.getString(R.string.sensor_state_cadence_avg), true, true, false),
                            StringUtils.getCadenceParts(context, sensorStatistics.getAvgCadence()),
                            null
                    )
            );
        }
        if (sensorStatistics.hasPower()) {
            sensorDataList.add(
                    new StatisticData(
                            new DataField(context.getString(R.string.stats_custom_layout_power_key), context.getString(R.string.sensor_state_power_avg), true, true, false),
                            StringUtils.getPowerParts(context, sensorStatistics.getAvgPower()),
                            null
                    )
            );
        }

        return sensorDataList;
    }
}
