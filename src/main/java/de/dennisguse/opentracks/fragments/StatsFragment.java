/*
 * Copyright 2008 Google Inc.
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

package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.EnumSet;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.TrackDataType;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to display track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatsFragment extends Fragment implements TrackDataListener {

    private static final String STATS_FRAGMENT_TAG = StatsFragment.class.getSimpleName();

    private static final long UI_UPDATE_INTERVAL = UnitConversions.ONE_SECOND_MS;

    private TrackDataHub trackDataHub;
    private Handler handlerUpdateUI;

    //TODO Initialize immediately and remove in onDestroy()
    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private TrackPoint lastTrackPoint = null;
    private TripStatistics lastTripStatistics = null;

    private String category = "";
    @Deprecated //TODO This should be handled somewhere else; not in the UI.
    private int recordingGpsAccuracy;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
                if (isResumed()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isResumed()) {
                                updateUI();
                            }
                        }
                    });
                }
            }
            if (PreferencesUtils.isKey(getContext(), R.string.recording_track_id_key, key)) {
                recordingGpsAccuracy = PreferencesUtils.getRecordingGPSAccuracy(getContext());
                if (PreferencesUtils.getRecordingTrackId(getContext()) != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
                    // A recording track id has been set -> Resumes track and starts timer.
                    resumeTrackDataHub();
                    if (trackRecordingServiceConnection == null) {
                        trackRecordingServiceConnection = new TrackRecordingServiceConnection(getContext(), null);
                    }
                    trackRecordingServiceConnection.startConnection(getContext());

                    handlerUpdateUI.post(updateUIeachSecond);
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stats, container, false);
    }

    /* Views */
    private View sensorContainerView;
    private TextView heartRateValueView;
    private TextView heartRateSensorView;

    private TextView totalTimeValueView;
    private final Runnable updateUIeachSecond = new Runnable() {
        public void run() {
            if (isResumed() && isSelectedTrackRecording()) {
                if (!isSelectedTrackPaused() && lastTripStatistics != null) {
                    updateTotalTime();
                    updateSensorDataUI();
                }

                handlerUpdateUI.postDelayed(this, UI_UPDATE_INTERVAL);
            }
        }
    };
    private TextView distanceValue;
    private TextView distanceUnit;
    private View activityLabel;
    private Spinner activitySpinner;
    private TextView movingTimeValue;
    private TextView speedAvgLabel;
    private TextView speedAvgValue;
    private TextView speedAvgUnit;
    private TextView speedMaxLabel;
    private TextView speedMaxValue;
    private TextView speedMaxUnit;
    private TextView speedMovingLabel;
    private TextView speedMovingValue;
    private TextView speedMovingUnit;
    private View gradeElevationSeparator;
    private View gradeElevationContainer;
    private View speedContainer;
    private TextView speedLabel;
    private TextView speedValue;
    private TextView speedUnit;
    private TextView elevationValue;
    private TextView elevationUnit;
    private View coordinateSeparator;
    private View coordinateContainer;
    private TextView latitudeValue;
    private TextView longitudeValue;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorContainerView = view.findViewById(R.id.stats_sensor_container);
        heartRateValueView = view.findViewById(R.id.stats_sensor_heart_rate_value);
        heartRateSensorView = view.findViewById(R.id.stats_sensor_heart_rate_sensor_value);

        totalTimeValueView = view.findViewById(R.id.stats_total_time_value);

        distanceValue = view.findViewById(R.id.stats_distance_value);
        distanceUnit = view.findViewById(R.id.stats_distance_unit);

        activityLabel = view.findViewById(R.id.stats_activity_type_label);
        activitySpinner = view.findViewById(R.id.stats_activity_type_icon);

        movingTimeValue = view.findViewById(R.id.stats_moving_time_value);

        speedAvgLabel = view.findViewById(R.id.stats_average_speed_label);
        speedAvgValue = view.findViewById(R.id.stats_average_speed_value);
        speedAvgUnit = view.findViewById(R.id.stats_average_speed_unit);

        speedMaxLabel = view.findViewById(R.id.stats_max_speed_label);
        speedMaxValue = view.findViewById(R.id.stats_max_speed_value);
        speedMaxUnit = view.findViewById(R.id.stats_max_speed_unit);

        speedMovingLabel = view.findViewById(R.id.stats_moving_speed_label);
        speedMovingValue = view.findViewById(R.id.stats_moving_speed_value);
        speedMovingUnit = view.findViewById(R.id.stats_moving_speed_unit);

        gradeElevationSeparator = view.findViewById(R.id.stats_elevation_separator);
        gradeElevationContainer = view.findViewById(R.id.stats_elevation_container);

        speedContainer = view.findViewById(R.id.stats_speed);
        speedLabel = view.findViewById(R.id.stats_speed_label);
        speedValue = view.findViewById(R.id.stats_speed_value);
        speedUnit = view.findViewById(R.id.stats_speed_unit);

        elevationValue = view.findViewById(R.id.stats_elevation_current_value);
        elevationUnit = view.findViewById(R.id.stats_elevation_current_unit);

        coordinateSeparator = view.findViewById(R.id.stats_coordinate_separator);
        coordinateContainer = view.findViewById(R.id.stats_coordinate_container);

        latitudeValue = view.findViewById(R.id.stats_latitude_value);
        longitudeValue = view.findViewById(R.id.stats_longitude_value);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        recordingGpsAccuracy = Integer.parseInt(getContext().getResources().getString(R.string.recording_gps_accuracy_default));

        handlerUpdateUI = new Handler();

        Spinner activityTypeIcon = getView().findViewById(R.id.stats_activity_type_icon);
        activityTypeIcon.setAdapter(TrackIconUtils.getIconSpinnerAdapter(getActivity(), ""));
        activityTypeIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TrackDetailActivity) getActivity()).chooseActivityType(category);
                }
                return true;
            }
        });
        activityTypeIcon.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    ((TrackDetailActivity) getActivity()).chooseActivityType(category);
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection(getContext(), null);
        trackRecordingServiceConnection.startConnection(getContext());

        handlerUpdateUI.post(updateUIeachSecond);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);

        handlerUpdateUI.removeCallbacks(updateUIeachSecond);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (trackRecordingServiceConnection != null) {
            trackRecordingServiceConnection.unbind(getContext());
        }
        trackRecordingServiceConnection = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sensorContainerView = null;
        heartRateValueView = null;
        heartRateSensorView = null;

        totalTimeValueView = null;

        distanceValue = null;
        distanceUnit = null;

        activityLabel = null;
        activitySpinner = null;

        movingTimeValue = null;

        speedAvgLabel = null;
        speedAvgValue = null;
        speedAvgUnit = null;

        speedMaxLabel = null;
        speedMaxValue = null;
        speedMaxUnit = null;

        speedMovingLabel = null;
        speedMovingValue = null;
        speedMovingUnit = null;

        gradeElevationSeparator = null;
        gradeElevationContainer = null;

        speedContainer = null;
        speedLabel = null;
        speedValue = null;
        speedUnit = null;

        elevationValue = null;
        elevationUnit = null;

        coordinateSeparator = null;
        coordinateContainer = null;

        latitudeValue = null;
        longitudeValue = null;
    }

    @Override
    public void onTrackUpdated(final Track track) {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        lastTripStatistics = track != null ? track.getTripStatistics() : null;
                        category = track != null ? track.getCategory() : "";
                        updateUI();
                    }
                }
            });
        }
    }

    @Override
    public void clearTrackPoints() {
        lastTrackPoint = null;
    }

    @Override
    public void onSampledInTrackPoint(TrackPoint trackPoint) {
        lastTrackPoint = trackPoint;
    }

    @Override
    public void onSampledOutTrackPoint(TrackPoint trackPoint) {
        lastTrackPoint = trackPoint;
    }

    @Override
    public void onNewTrackPointsDone() {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        if (!isSelectedTrackRecording() || isSelectedTrackPaused()) {
                            lastTrackPoint = null;
                        }

                        if (lastTrackPoint != null) {
                            boolean hasFix = !LocationUtils.isLocationOld(lastTrackPoint.getLocation());
                            boolean hasGoodFix = lastTrackPoint.hasAccuracy() && lastTrackPoint.getAccuracy() < recordingGpsAccuracy;

                            if (!hasFix || !hasGoodFix) {
                                lastTrackPoint = null;
                            }
                        }
                        setLocationValues();
                    }
                }
            });
        }
    }

    @Override
    public void clearWaypoints() {
        // We don't care.
    }

    @Override
    public void onNewWaypoint(Waypoint wpt) {
        // We don't care.
    }

    @Override
    public void onNewWaypointsDone() {
        // We don't care.
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.TRACKS_TABLE,
                TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE, TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE));
    }

    /**
     * Pauses the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void pauseTrackDataHub() {
        trackDataHub.unregisterTrackDataListener(this);
        trackDataHub = null;
    }

    /**
     * Returns true if the selected track is recording.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackRecording() {
        return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
    }

    /**
     * Returns true if the selected track is paused.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackPaused() {
        return trackDataHub != null && trackDataHub.isSelectedTrackPaused();
    }

    private void updateUI() {
        updateStats();
        setLocationValues();
        updateSensorDataUI();
    }

    /**
     * Tries to fetch most recent {@link SensorDataSet} {@link de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager}.
     */
    private void updateSensorDataUI() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

        SensorDataSet sensorDataSet = null;
        if (trackRecordingService == null) {
            Log.d(STATS_FRAGMENT_TAG, "Cannot get the track recording service.");
        } else {
            //TODO sensorState = trackRecordingService.getSensorState();
            sensorDataSet = trackRecordingService.getSensorData();
        }

        setHeartRateSensorData(sensorDataSet, isSelectedTrackRecording());
    }

    private void setHeartRateSensorData(SensorDataSet sensorDataSet, boolean isRecording) {
        // heart rate
        int isVisible = View.VISIBLE;
        if (!isRecording || PreferencesUtils.isBluetoothHeartRateSensorAddressDefault(getContext())) {
            isVisible = View.INVISIBLE;
        }
        sensorContainerView.setVisibility(isVisible);

        if (isRecording) {
            String heartRate = getContext().getString(R.string.value_unknown);
            String sensorName = getContext().getString(R.string.value_unknown);
            if (sensorDataSet != null && sensorDataSet.isRecent(BluetoothRemoteSensorManager.MAX_SENSOR_DATE_SET_AGE_MS)) {
                sensorName = sensorDataSet.getSensorName();
                if (sensorDataSet.hasHeartRate()) {
                    heartRate = StringUtils.formatDecimal(sensorDataSet.getHeartRate(), 0);
                }
            }

            heartRateSensorView.setText(sensorName);
            heartRateValueView.setText(heartRate);
        }
    }

    private void updateStats() {
        String trackIconValue = TrackIconUtils.getIconValue(getContext(), category);

        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext());
        boolean isRecording = isSelectedTrackRecording();

        // Set total distance
        {
            double totalDistance = lastTripStatistics == null ? Double.NaN : lastTripStatistics.getTotalDistance();
            Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), totalDistance, metricUnits);

            distanceValue.setText(parts.first);
            distanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            activityLabel.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);

            activitySpinner.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);
            activitySpinner.setEnabled(isRecording);
            if (trackIconValue != null) {
                TrackIconUtils.setIconSpinner(activitySpinner, trackIconValue);
            }
        }

        // Set time
        if (lastTripStatistics != null) {
            movingTimeValue.setText(StringUtils.formatElapsedTime(lastTripStatistics.getMovingTime()));
            updateTotalTime();
        }

        // Set average speed/pace
        {
            double speed = lastTripStatistics != null ? lastTripStatistics.getAverageSpeed() : Double.NaN;
            speedAvgLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedAvgValue.setText(parts.first);
            speedAvgUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = lastTripStatistics == null ? Double.NaN : lastTripStatistics.getMaxSpeed();

            speedMaxLabel.setText(reportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMaxValue.setText(parts.first);
            speedMaxUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = lastTripStatistics != null ? lastTripStatistics.getAverageMovingSpeed() : Double.NaN;

            speedMovingLabel.setText(reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMovingValue.setText(parts.first);
            speedMovingUnit.setText(parts.second);
        }

        // Make elevation visible?
        {
            boolean showElevation = PreferencesUtils.isShowStatsGradeElevation(getContext());
            gradeElevationSeparator.setVisibility(showElevation ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTotalTime() {
        long totalTime;
        if (isSelectedTrackRecording()) {
            totalTime = calculateTotalTime();
        } else {
            totalTime = lastTripStatistics.getTotalTime();
        }
        totalTimeValueView.setText(StringUtils.formatElapsedTime(totalTime));
    }

    /**
     * Return time from service.
     * If service isn't bound then use lastTripStatistics for calculate it.
     */
    private long calculateTotalTime() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        if (trackRecordingService != null) {
            return trackRecordingService.getTotalTime();
        } else {
            return System.currentTimeMillis() - lastTripStatistics.getStopTime() + lastTripStatistics.getTotalTime();
        }
    }

    private void setLocationValues() {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext());
        boolean isRecording = isSelectedTrackRecording();

        // Set speed/pace
        speedContainer.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        if (isRecording) {
            speedLabel.setText(reportSpeed ? R.string.stats_speed : R.string.stats_pace);

            double speed = lastTrackPoint != null && lastTrackPoint.hasSpeed() ? lastTrackPoint.getSpeed() : Double.NaN;
            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedValue.setText(parts.first);
            speedUnit.setText(parts.second);
        }

        // Set elevation
        boolean showGradeElevation = isRecording && PreferencesUtils.isShowStatsGradeElevation(getContext());
        gradeElevationContainer.setVisibility(showGradeElevation ? View.VISIBLE : View.GONE);

        if (showGradeElevation) {
            double altitude = lastTrackPoint != null && lastTrackPoint.hasAltitude() ? lastTrackPoint.getAltitude() : Double.NaN;
            Pair<String, String> parts = StringUtils.formatElevation(getContext(), altitude, metricUnits);

            elevationValue.setText(parts.first);
            elevationUnit.setText(parts.second);
        }

        // Set coordinate
        boolean showCoordinate = isRecording && PreferencesUtils.isStatsShowCoordinate(getContext());

        coordinateSeparator.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
        coordinateContainer.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
        if (showCoordinate) {
            double latitude = lastTrackPoint != null ? lastTrackPoint.getLatitude() : Double.NaN;
            String latitudeText = Double.isNaN(latitude) || Double.isInfinite(latitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(latitude);
            latitudeValue.setText(latitudeText);

            double longitude = lastTrackPoint != null ? lastTrackPoint.getLongitude() : Double.NaN;
            String longitudeText = Double.isNaN(longitude) || Double.isInfinite(longitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(longitude);
            longitudeValue.setText(longitudeText);
        }
    }
}
