package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.Layout;
import de.dennisguse.opentracks.util.StringUtils;

public class StatisticDataBuilder {

    public static List<StatisticData> fromRecordingData(@NonNull Context context, @NonNull RecordingData recordingData, @NonNull Layout layout, boolean metricUnits) {
        List<StatisticData> statisticDataList = layout.getFields().stream()
                .filter(DataField::isVisible)
                .map(field -> build(context, recordingData, field.getKey(), field.isPrimary(), metricUnits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        statisticDataList.addAll(getSensorStatsDataIfNeeded(context, recordingData, statisticDataList, metricUnits));
        return statisticDataList;
    }

    private static StatisticData build(@NonNull Context context, @NonNull RecordingData recordingData, @NonNull String fieldKey, boolean isPrimary, boolean metricUnits) {
        final TrackPoint latestTrackPoint = recordingData.getLatestTrackPoint();
        final SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        final TrackStatistics trackStatistics = recordingData.getTrackStatistics();

        String title = null;
        String description = null;
        Pair<String, String> valueAndUnit = null;
        boolean isWide = false;

        final String sensorUnknown = context.getString(R.string.value_unknown);

        if (fieldKey.equals(context.getString(R.string.stats_custom_layout_total_time_key))) {
            valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(trackStatistics.getTotalTime()), null);
            title = context.getString(R.string.stats_total_time);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_moving_time_key))) {
            valueAndUnit = new Pair<>(StringUtils.formatElapsedTime(trackStatistics.getMovingTime()), null);
            title = context.getString(R.string.stats_moving_time);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_distance_key))) {
            valueAndUnit = StringUtils.getDistanceParts(context, trackStatistics.getTotalDistance(), metricUnits);
            title = context.getString(R.string.stats_distance);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_speed_key)) || fieldKey.equals(context.getString(R.string.stats_custom_layout_pace_key))) {
            boolean reportSpeed = fieldKey.equals("speed");
            title = reportSpeed ? context.getString(R.string.stats_speed) : context.getString(R.string.stats_pace);

            if (sensorDataSet != null && sensorDataSet.getSpeed() != null) {
                valueAndUnit = StringUtils.getSpeedParts(context, sensorDataSet.getSpeed().first, metricUnits, reportSpeed);
                description = sensorDataSet.getSpeed().second;
            } else {
                Speed speed = latestTrackPoint != null && latestTrackPoint.hasSpeed() ? latestTrackPoint.getSpeed() : null;
                valueAndUnit = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
                description = context.getString(R.string.description_speed_source_gps);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_moving_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getAverageMovingSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_average_moving_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getAverageSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_average_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_max_speed_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getMaxSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_max_speed);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_moving_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getAverageMovingSpeed(), metricUnits, false);
            title = context.getString(R.string.stats_average_moving_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_average_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getAverageSpeed(), metricUnits, false);
            title = context.getString(R.string.stats_average_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_fastest_pace_key))) {
            valueAndUnit = StringUtils.getSpeedParts(context, trackStatistics.getMaxSpeed(), metricUnits, true);
            title = context.getString(R.string.stats_fastest_pace);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_altitude_key))) {
            Float altitude = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? (float) latestTrackPoint.getAltitude().toM() : null;
            title = context.getString(R.string.stats_altitude);
            description = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? context.getString(latestTrackPoint.getAltitude().getLabelId()) : null;
            valueAndUnit = StringUtils.getAltitudeParts(context, altitude, metricUnits);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_gain_key))) {
            valueAndUnit = StringUtils.getAltitudeChangeParts(context, trackStatistics.getTotalAltitudeGain(), metricUnits);
            title = context.getString(R.string.stats_gain);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_loss_key))) {
            valueAndUnit = StringUtils.getAltitudeChangeParts(context, trackStatistics.getTotalAltitudeLoss(), metricUnits);
            title = context.getString(R.string.stats_loss);
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_coordinates_key))) {
            title = context.getString(R.string.stats_coordinates);
            isWide = true;
            if (latestTrackPoint != null && latestTrackPoint.hasLocation()) {
                valueAndUnit = new Pair<>(StringUtils.formatCoordinate(context, latestTrackPoint.getLatitude(), latestTrackPoint.getLongitude()), null);
            } else {
                valueAndUnit = new Pair<>(context.getString(R.string.value_unknown), null);
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_heart_rate_key))) {
            title = context.getString(R.string.stats_sensors_heart_rate);
            if (sensorDataSet != null && sensorDataSet.getHeartRate() != null) {
                valueAndUnit = StringUtils.getHeartRateParts(context, sensorDataSet.getHeartRate().first);
                description = sensorDataSet.getHeartRate().second;
            } else {
                valueAndUnit = StringUtils.getHeartRateParts(context, null);
                description = sensorUnknown;
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_cadence_key))) {
            title = context.getString(R.string.stats_sensors_cadence);

            Cadence cadence = null;
            if (sensorDataSet != null) {
                Pair<Cadence, String> cadenceData = sensorDataSet.getCadence();
                if (cadenceData != null) {
                    cadence = cadenceData.first;
                    description = cadenceData.second;
                }
            }

            valueAndUnit = StringUtils.getCadenceParts(context, cadence);
            if (description == null) {
                description = sensorUnknown;
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_power_key))) {
            title = context.getString(R.string.stats_sensors_power);
            if (sensorDataSet != null && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue()) {
                valueAndUnit = StringUtils.getPowerParts(context, sensorDataSet.getCyclingPower().getValue());
                description = sensorDataSet.getCyclingPower().getSensorNameOrAddress();
            } else {
                valueAndUnit = StringUtils.getPowerParts(context, null);
                description = sensorUnknown;
            }
        } else if (fieldKey.equals(context.getString(R.string.stats_custom_layout_clock_key))) {
            title = context.getString(R.string.stats_clock);
            valueAndUnit = new Pair<>(null, null);
        }

        if (valueAndUnit == null) {
            return null;
        }

        return new StatisticData(new DataField(fieldKey, title, true, isPrimary, isWide), valueAndUnit, description);
    }

    /**
     * Builds a list of StatisticData with sensors configured but not in statisticDataList.
     */
    private static List<StatisticData> getSensorStatsDataIfNeeded(Context context, RecordingData recordingData, List<StatisticData> statisticDataList, boolean metricUnits) {
        List<StatisticData> sensorDataList = new ArrayList<>();
        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        if (sensorDataSet == null) {
            return sensorDataList;
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_heart_rate))) && sensorDataSet.getHeartRate() != null) {
            sensorDataList.add(build(context, recordingData, "heart_rate", true, metricUnits));
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_cadence))) && sensorDataSet.getCadence() != null) {
            sensorDataList.add(build(context, recordingData, "cadence", true, metricUnits));
        }
        if (statisticDataList.stream().noneMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_power))) && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue()) {
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
