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
        List<StatsData> statsDataList = layout.getFields().stream().filter(Layout.Field::isVisible).map(i -> StatsDataBuilder.build(context, recordingData, i, metricUnits)).filter(Objects::nonNull).collect(Collectors.toList());
        statsDataList.addAll(getSensorStatsDataIfNeeded(context, statsDataList, recordingData));
        return statsDataList;
    }

    private static StatsData build(@NonNull Context context, @NonNull TrackRecordingService.RecordingData recordingData, @NonNull Layout.Field field, boolean metricUnits) {
        StatsData data = null;
        TrackPoint latestTrackPoint;
        SensorDataSet sensorDataSet;

        switch (field.getTitle()) {
            case "total_time":
                data = new StatsData(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getTotalTime()), context.getString(R.string.stats_total_time), field.isPrimary());
                break;
            case "moving_time":
                data = new StatsData(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getMovingTime()), context.getString(R.string.stats_moving_time), field.isPrimary());
                break;
            case "distance":
                data = new StatsData(StringUtils.getDistanceParts(context, recordingData.getTrackStatistics().getTotalDistance(), metricUnits), context.getString(R.string.stats_distance), field.isPrimary());
                break;
            case "speed":
            case "pace":
                boolean reportSpeed = field.getTitle().equals("speed");
                String title = field.getTitle().equals("speed") ? context.getString(R.string.stats_speed) : context.getString(R.string.stats_pace);
                latestTrackPoint = recordingData.getLatestTrackPoint();
                Speed speed = latestTrackPoint != null && latestTrackPoint.hasSpeed() ? latestTrackPoint.getSpeed() : null;

                sensorDataSet = recordingData.getSensorDataSet();
                if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null && sensorDataSet.getCyclingDistanceSpeed().hasValue() && sensorDataSet.getCyclingDistanceSpeed().isRecent()) {
                    data = new StatsData(
                            StringUtils.getSpeedParts(context, sensorDataSet.getCyclingDistanceSpeed().getValue().getSpeed(), metricUnits, reportSpeed),
                            title,
                            context.getString(R.string.description_speed_source_sensor, sensorDataSet.getCyclingDistanceSpeed().getSensorNameOrAddress()),
                            field.isPrimary());
                } else {
                    data = new StatsData(
                            StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed),
                            title,
                            context.getString(R.string.description_speed_source_gps),
                            field.isPrimary());
                }
                break;
            case "average_moving_speed":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, true), context.getString(R.string.stats_average_moving_speed), field.isPrimary());
                break;
            case "average_speed":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, true), context.getString(R.string.stats_average_speed), field.isPrimary());
                break;
            case "max_speed":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true), context.getString(R.string.stats_max_speed), field.isPrimary());
                break;
            case "average_moving_pace":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageMovingSpeed(), metricUnits, false), context.getString(R.string.stats_average_moving_pace), field.isPrimary());
                break;
            case "average_pace":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getAverageSpeed(), metricUnits, false), context.getString(R.string.stats_average_pace), field.isPrimary());
                break;
            case "fastest_pace":
                data = new StatsData(StringUtils.getSpeedParts(context, recordingData.getTrackStatistics().getMaxSpeed(), metricUnits, true), context.getString(R.string.stats_fastest_pace), field.isPrimary());
                break;
            case "altitude":
                latestTrackPoint = recordingData.getLatestTrackPoint();
                Float altitude = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? (float) latestTrackPoint.getAltitude().toM() : null;
                String altitudeSource = latestTrackPoint != null && latestTrackPoint.hasAltitude() ? context.getString(latestTrackPoint.getAltitude().getLabelId()) : null;
                data = new StatsData(StringUtils.getAltitudeParts(context, altitude, metricUnits), context.getString(R.string.stats_altitude), altitudeSource, field.isPrimary());
                break;
            case "gain":
                data = new StatsData(StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeGain(), metricUnits), context.getString(R.string.stats_gain), field.isPrimary());
                break;
            case "loss":
                data = new StatsData(StringUtils.getAltitudeParts(context, recordingData.getTrackStatistics().getTotalAltitudeLoss(), metricUnits), context.getString(R.string.stats_loss), field.isPrimary());
                break;
            case "coordinates":
                latestTrackPoint = recordingData.getLatestTrackPoint();
                if (latestTrackPoint != null && latestTrackPoint.hasLocation()) {
                    data = new StatsData(StringUtils.formatCoordinate(latestTrackPoint.getLatitude(), latestTrackPoint.getLongitude()), context.getString(R.string.stats_coordinates), field.isPrimary()).setLong();
                } else {
                    data = new StatsData("-", context.getString(R.string.stats_coordinates), field.isPrimary()).setLong();
                }
                break;
            case "heart_rate":
                sensorDataSet = recordingData.getSensorDataSet();
                if (sensorDataSet != null && sensorDataSet.getHeartRate() != null && sensorDataSet.getHeartRate().hasValue() && sensorDataSet.getHeartRate().isRecent()) {
                    data = new StatsData(
                            StringUtils.formatDecimal(sensorDataSet.getHeartRate().getValue(), 0),
                            context.getString(R.string.sensor_unit_beats_per_minute),
                            context.getString(R.string.stats_sensors_heart_rate),
                            sensorDataSet.getHeartRate().getSensorNameOrAddress(),
                            field.isPrimary());
                } else {
                    data = new StatsData("-", context.getString(R.string.sensor_unit_beats_per_minute), context.getString(R.string.stats_sensors_heart_rate), "-", field.isPrimary());
                }
                break;
            case "cadence":
                sensorDataSet = recordingData.getSensorDataSet();
                if (sensorDataSet != null && sensorDataSet.getCyclingCadence() != null && sensorDataSet.getCyclingCadence().hasValue() && sensorDataSet.getCyclingCadence().isRecent()) {
                    data = new StatsData(
                            StringUtils.formatDecimal(sensorDataSet.getCyclingCadence().getValue(), 0),
                            context.getString(R.string.sensor_unit_rounds_per_minute),
                            context.getString(R.string.stats_sensors_cadence),
                            sensorDataSet.getCyclingCadence().getSensorNameOrAddress(),
                            field.isPrimary());
                } else {
                    data = new StatsData("-", context.getString(R.string.sensor_unit_rounds_per_minute), context.getString(R.string.stats_sensors_cadence), "-", field.isPrimary());
                }
                break;
            case "power":
                sensorDataSet = recordingData.getSensorDataSet();
                if (sensorDataSet != null && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue() && sensorDataSet.getCyclingPower().isRecent()) {
                    data = new StatsData(
                            StringUtils.formatDecimal(sensorDataSet.getCyclingPower().getValue(), 0),
                            context.getString(R.string.sensor_unit_power),
                            context.getString(R.string.stats_sensors_power),
                            sensorDataSet.getCyclingPower().getSensorNameOrAddress(),
                            field.isPrimary());
                } else {
                    data = new StatsData("-", context.getString(R.string.sensor_unit_power), context.getString(R.string.stats_sensors_power), "-", field.isPrimary());
                }
                break;
        }

        return data;
    }

    /**
     * Builds a list of StatsData with sensors connected but not in statsDataList.
     *
     * @param context       the Context object.
     * @param statsDataList list of StatsData already added by the user.
     * @param recordingData the RecordingData object.
     * @return              list of StatsData with sensor information not present in statsDataList.
     */
    private static List<StatsData> getSensorStatsDataIfNeeded(Context context, List<StatsData> statsDataList, TrackRecordingService.RecordingData recordingData) {
        List<StatsData> sensorDataList = new ArrayList<>();
        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        if (statsDataList.stream().noneMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_heart_rate))) && sensorDataSet != null && sensorDataSet.getHeartRate() != null && sensorDataSet.getHeartRate().hasValue() && sensorDataSet.getHeartRate().isRecent()) {
            sensorDataList.add(new StatsData(
                    StringUtils.formatDecimal(sensorDataSet.getHeartRate().getValue(), 0),
                    context.getString(R.string.sensor_unit_beats_per_minute),
                    context.getString(R.string.stats_sensors_heart_rate),
                    sensorDataSet.getHeartRate().getSensorNameOrAddress(),
                    true));
        }
        if (statsDataList.stream().noneMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_cadence))) && sensorDataSet != null && sensorDataSet.getCyclingCadence() != null && sensorDataSet.getCyclingCadence().hasValue() && sensorDataSet.getCyclingCadence().isRecent()) {
            sensorDataList.add(new StatsData(
                    StringUtils.formatDecimal(sensorDataSet.getCyclingCadence().getValue(), 0),
                    context.getString(R.string.sensor_unit_rounds_per_minute),
                    context.getString(R.string.stats_sensors_cadence),
                    sensorDataSet.getCyclingCadence().getSensorNameOrAddress(),
                    true));
        }
        if (statsDataList.stream().noneMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_power))) && sensorDataSet != null && sensorDataSet.getCyclingPower() != null && sensorDataSet.getCyclingPower().hasValue() && sensorDataSet.getCyclingPower().isRecent()) {
            sensorDataList.add(new StatsData(
                    StringUtils.formatDecimal(sensorDataSet.getCyclingPower().getValue(), 0),
                    context.getString(R.string.sensor_unit_power),
                    context.getString(R.string.stats_sensors_power),
                    sensorDataSet.getCyclingPower().getSensorNameOrAddress(),
                    true));
        }

        return sensorDataList;
    }

    public static List<StatsData> fromSensorStatistics(@NonNull Context context, @NonNull SensorStatistics sensorStatistics) {
        List<StatsData> sensorDataList = new ArrayList<>();

        if (sensorStatistics.hasHeartRate()) {
            sensorDataList.add(
                    new StatsData(
                            new Pair<>(
                                    StringUtils.formatDecimal(sensorStatistics.getMaxHeartRate(), 0),
                                    context.getString(R.string.sensor_unit_beats_per_minute)),
                            context.getString(R.string.sensor_state_heart_rate_max),
                            true
                    )
            );
            sensorDataList.add(
                    new StatsData(
                            new Pair<>(
                                    StringUtils.formatDecimal(sensorStatistics.getAvgHeartRate(), 0),
                                    context.getString(R.string.sensor_unit_beats_per_minute)),
                            context.getString(R.string.sensor_state_heart_rate_avg),
                            true
                    )
            );
        }
        if (sensorStatistics.hasCadence()) {
            sensorDataList.add(
                    new StatsData(
                            new Pair<>(
                                    StringUtils.formatDecimal(sensorStatistics.getMaxCadence(), 0),
                                    context.getString(R.string.sensor_unit_rounds_per_minute)),
                            context.getString(R.string.sensor_state_cadence_max),
                            true
                    )
            );
            sensorDataList.add(
                    new StatsData(
                            new Pair<>(
                                    StringUtils.formatDecimal(sensorStatistics.getAvgCadence(), 0),
                                    context.getString(R.string.sensor_unit_rounds_per_minute)),
                            context.getString(R.string.sensor_state_cadence_avg),
                            true
                    )
            );
        }
        if (sensorStatistics.hasPower()) {
            sensorDataList.add(
                    new StatsData(
                            new Pair<>(
                                    StringUtils.formatDecimal(sensorStatistics.getAvgPower(), 0),
                                    context.getString(R.string.sensor_unit_power)),
                            context.getString(R.string.sensor_state_power_avg),
                            true
                    )
            );
        }

        return sensorDataList;
    }
}
