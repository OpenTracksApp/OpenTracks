/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.util;

import android.app.Activity;
import android.location.Location;
import android.util.Pair;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.stats.TripStatistics;

/**
 * Utilities for updating the statistics UI labels and values.
 *
 * @author Jimmy Shih
 */
public class StatsUtils {

    private static final String GRADE_PERCENTAGE = "%";
    private static final String GRADE_FORMAT = "%1$d";

    private StatsUtils() {
    }

    /**
     * Sets the location values.
     *
     * @param activity    the activity for finding views
     * @param location    the location
     * @param isRecording true if recording
     */
    public static void setLocationValues(Activity activity, Location location, boolean isRecording) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(activity);
        boolean reportSpeed = PreferencesUtils.isReportSpeed(activity);

        // Set speed/pace
        View speedContainer = activity.findViewById(R.id.stats_speed);
        speedContainer.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            TextView speedLabel = activity.findViewById(R.id.stats_speed_label);
            speedLabel.setText(reportSpeed ? R.string.stats_speed : R.string.stats_pace);

            double speed = location != null && location.hasSpeed() ? location.getSpeed() : Double.NaN;
            Pair<String, String> parts = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);

            TextView speedValue = activity.findViewById(R.id.stats_speed_value);
            speedValue.setText(parts.first);

            TextView speedUnit = activity.findViewById(R.id.stats_speed_unit);
            speedUnit.setText(parts.second);
        }

        // Set elevation
        boolean showGradeElevation = PreferencesUtils.getBoolean(activity, R.string.stats_show_grade_elevation_key, PreferencesUtils.STATS_SHOW_ELEVATION_DEFAULT) && isRecording;
        View elevationContainer = activity.findViewById(R.id.stats_elevation);
        elevationContainer.setVisibility(showGradeElevation ? View.VISIBLE : View.GONE);

        if (showGradeElevation) {
            double altitude = location != null && location.hasAltitude() ? location.getAltitude() : Double.NaN;
            Pair<String, String> parts = StringUtils.formatElevation(activity, altitude, metricUnits);

            TextView elevationValue = activity.findViewById(R.id.stats_elevation_current_value);
            elevationValue.setText(parts.first);
            TextView elevationUnit = activity.findViewById(R.id.stats_elevation_current_unit);
            elevationUnit.setText(parts.second);
        }

        // Set coordinate
        boolean showCoordinate = isRecording && PreferencesUtils.getBoolean(activity, R.string.stats_show_coordinate_key, PreferencesUtils.STATS_SHOW_COORDINATE_DEFAULT);

        View coordinateSeparator = activity.findViewById(R.id.stats_coordinate_separator);
        coordinateSeparator.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);

        View coordinateContainer = activity.findViewById(R.id.stats_coordinate_container);
        coordinateContainer.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
        if (showCoordinate) {
            double latitude = location != null ? location.getLatitude() : Double.NaN;
            String latitudeText = Double.isNaN(latitude) || Double.isInfinite(latitude) ? activity.getString(R.string.value_unknown) : StringUtils.formatCoordinate(latitude);
            TextView latitudeValue = activity.findViewById(R.id.stats_latitude_value);
            latitudeValue.setText(latitudeText);

            double longitude = location != null ? location.getLongitude() : Double.NaN;
            String longitudeText = Double.isNaN(longitude) || Double.isInfinite(longitude) ? activity.getString(R.string.value_unknown) : StringUtils.formatCoordinate(longitude);
            TextView longitudeValue = activity.findViewById(R.id.stats_longitude_value);
            longitudeValue.setText(longitudeText);
        }
    }

    public static void setSensorData(Activity activity, SensorDataSet sensorDataSet, boolean isRecording) {
        // heart rate
        int isVisible = View.VISIBLE;
        if (!isRecording || PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT.equals(PreferencesUtils.getString(activity, R.string.settings_sensor_bluetooth_sensor_key, PreferencesUtils.BLUETOOTH_SENSOR_DEFAULT))) {
            isVisible = View.INVISIBLE;
        }
        activity.findViewById(R.id.stats_sensor_container).setVisibility(isVisible);

        if (isRecording) {
            TextView heartRateValue = activity.findViewById(R.id.stats_sensor_heart_rate_value);
            TextView heartRateSensor = activity.findViewById(R.id.stats_sensor_heart_rate_sensor_value);
            String heartRate = activity.getString(R.string.value_unknown);
            String sensor = activity.getString(R.string.value_unknown);
            if (sensorDataSet != null && sensorDataSet.isRecent(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS)) {
                sensor = sensorDataSet.getSensorName();
                if (sensorDataSet.hasHeartRate()) {
                    heartRate = StringUtils.formatDecimal(sensorDataSet.getHeartRate(), 0);
                }
            }

            heartRateValue.setText(heartRate);
            heartRateSensor.setText(sensor);
        }
    }

    /**
     * Sets the total time value.
     *
     * @param activity  the activity
     * @param totalTime the total time
     */
    public static void setTotalTimeValue(Activity activity, long totalTime) {
        TextView totalTimeValue = activity.findViewById(R.id.stats_total_time_value);
        totalTimeValue.setText(StringUtils.formatElapsedTime(totalTime));
    }

    /**
     * Sets the trip statistics values.
     *
     * @param activity       the activity for finding views
     * @param tripStatistics the trip statistics
     * @param trackIconValue the track icon value or null to hide the track icon spinner
     */
    public static void setTripStatisticsValues(Activity activity, TripStatistics tripStatistics, String trackIconValue) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(activity);
        boolean reportSpeed = PreferencesUtils.isReportSpeed(activity);

        // Set total distance
        {
            double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
            Pair<String, String> parts = StringUtils.getDistanceParts(activity, totalDistance, metricUnits);

            TextView distanceValue = activity.findViewById(R.id.stats_distance_value);
            distanceValue.setText(parts.first);

            TextView distanceUnit = activity.findViewById(R.id.stats_distance_unit);
            distanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            activity.findViewById(R.id.stats_activity_type_label).setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);

            Spinner spinner = activity.findViewById(R.id.stats_activity_type_icon);
            spinner.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);
            if (trackIconValue != null) {
                TrackIconUtils.setIconSpinner(spinner, trackIconValue);
            }
        }

        // Set time
        if (tripStatistics != null) {
            setTotalTimeValue(activity, tripStatistics.getTotalTime());

            TextView movingTimeValue = activity.findViewById(R.id.stats_moving_time_value);
            movingTimeValue.setText(StringUtils.formatElapsedTime(tripStatistics.getMovingTime()));
        }

        // Set average speed/pace
        {
            double speed = tripStatistics != null ? tripStatistics.getAverageSpeed() : Double.NaN;
            TextView speedLabel = activity.findViewById(R.id.stats_average_speed_label);
            speedLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);
            TextView speedValue = activity.findViewById(R.id.stats_average_speed_value);
            speedValue.setText(parts.first);
            TextView speedUnit = activity.findViewById(R.id.stats_average_speed_unit);
            speedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = tripStatistics == null ? Double.NaN : tripStatistics.getMaxSpeed();

            TextView speedLabel = activity.findViewById(R.id.stats_max_speed_label);
            speedLabel.setText(reportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);
            TextView speedValue = activity.findViewById(R.id.stats_max_speed_value);
            speedValue.setText(parts.first);
            TextView speedUnit = activity.findViewById(R.id.stats_max_speed_unit);
            speedUnit.setText(parts.second);
        }

        // Set average speed/pace
        {
            double speed = tripStatistics != null ? tripStatistics.getAverageSpeed() : Double.NaN;

            TextView speedLabel = activity.findViewById(R.id.stats_average_speed_label);
            speedLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);
            TextView speedValue = activity.findViewById(R.id.stats_average_speed_value);
            speedValue.setText(parts.first);
            TextView speedUnit = activity.findViewById(R.id.stats_average_speed_unit);
            speedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = tripStatistics != null ? tripStatistics.getAverageMovingSpeed() : Double.NaN;

            TextView speedLabel = activity.findViewById(R.id.stats_moving_speed_label);
            speedLabel.setText(reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(activity, speed, metricUnits, reportSpeed);
            TextView speedValue = activity.findViewById(R.id.stats_moving_speed_value);
            speedValue.setText(parts.first);
            TextView speedUnit = activity.findViewById(R.id.stats_moving_speed_unit);
            speedUnit.setText(parts.second);
        }


        // Set elevation
        {
            boolean showElevation = PreferencesUtils.getBoolean(activity, R.string.stats_show_grade_elevation_key, PreferencesUtils.STATS_SHOW_ELEVATION_DEFAULT);
            View gradeElevationSeparator = activity.findViewById(R.id.stats_elevation_separator);
            gradeElevationSeparator.setVisibility(showElevation ? View.VISIBLE : View.GONE);

            View gradeElevationContainer = activity.findViewById(R.id.stats_elevation_container);
            gradeElevationContainer.setVisibility(showElevation ? View.VISIBLE : View.GONE);

            if (showElevation) {
                {
                    double elevation = tripStatistics == null ? Double.NaN : tripStatistics.getMinElevation();
                    Pair<String, String> parts = StringUtils.formatElevation(activity, elevation, metricUnits);

                    TextView elevationValue = activity.findViewById(R.id.stats_elevation_min_value);
                    elevationValue.setText(parts.first);
                    TextView elevationUnit = activity.findViewById(R.id.stats_elevation_min_unit);
                    elevationUnit.setText(parts.second);
                }

                {
                    double elevation = tripStatistics == null ? Double.NaN : tripStatistics.getMaxElevation();
                    Pair<String, String> parts = StringUtils.formatElevation(activity, elevation, metricUnits);

                    TextView elevationValue = activity.findViewById(R.id.stats_elevation_max_value);
                    elevationValue.setText(parts.first);
                    TextView elevationUnit = activity.findViewById(R.id.stats_elevation_max_unit);
                    elevationUnit.setText(parts.second);
                }
            }
        }
    }
}