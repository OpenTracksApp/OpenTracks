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
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackPointUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to display track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordingFragment extends Fragment implements TrackDataListener {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    private static final long UI_UPDATE_INTERVAL = UnitConversions.ONE_SECOND_MS;

    private TrackDataHub trackDataHub;
    private Handler handlerUpdateUI;

    private TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection();

    private TrackPoint lastTrackPoint;
    private TrackStatistics lastTrackStatistics;

    private String category = "";
    @Deprecated //TODO This should be handled somewhere else; not in the UI.
    private int recordingGpsAccuracy;

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
                if (isResumed()) {
                    getActivity().runOnUiThread(() -> {
                        if (isResumed()) {
                            updateUI();
                        }
                    });
                }
            }
            if (PreferencesUtils.isKey(getContext(), R.string.recording_track_id_key, key)) {
                recordingGpsAccuracy = PreferencesUtils.getRecordingGPSAccuracy(getContext());
                if (PreferencesUtils.getRecordingTrackId(getContext()).isValid()) {
                    // A recording track id has been set -> Resumes track and starts timer.
                    resumeTrackDataHub();
                    trackRecordingServiceConnection.startConnection(getContext());

                    handlerUpdateUI.post(updateUIeachSecond);
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.statistics_recording, container, false);
    }

    /* Views */
    private View sensorHorizontalLine;
    private Group heartRateGroup;
    private TextView heartRateValueView;
    private TextView heartRateSensorView;
    private Group cadenceGroup;
    private TextView cadenceValueView;
    private TextView cadenceSensorView;

    private TextView totalTimeValueView;
    private final Runnable updateUIeachSecond = new Runnable() {
        public void run() {
            if (isResumed() && isSelectedTrackRecording()) {
                if (!isSelectedTrackPaused() && lastTrackStatistics != null) {
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
    private Group elevationGroup;
    private TextView speedLabel;
    private TextView speedValue;
    private TextView speedUnit;
    private TextView elevationValue;
    private TextView elevationUnit;
    private Group coordinateGroup;
    private TextView latitudeValue;
    private TextView longitudeValue;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sensorHorizontalLine = view.findViewById(R.id.stats_sensor_horizontal_line);

        heartRateGroup = view.findViewById(R.id.stats_sensor_heart_rate_group);
        heartRateValueView = view.findViewById(R.id.stats_sensor_heart_rate_value);
        heartRateSensorView = view.findViewById(R.id.stats_sensor_heart_rate_sensor_value);

        cadenceGroup = view.findViewById(R.id.stats_sensor_cadence_group);
        cadenceValueView = view.findViewById(R.id.stats_sensor_cadence_value);
        cadenceSensorView = view.findViewById(R.id.stats_sensor_cadence_sensor_value);

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

        elevationGroup = view.findViewById(R.id.stats_elevation_current_group);

        speedLabel = view.findViewById(R.id.stats_speed_label);
        speedValue = view.findViewById(R.id.stats_speed_value);
        speedUnit = view.findViewById(R.id.stats_speed_unit);

        elevationValue = view.findViewById(R.id.stats_elevation_current_value);
        elevationUnit = view.findViewById(R.id.stats_elevation_current_unit);

        coordinateGroup = view.findViewById(R.id.stats_coordinate_group);

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
        activityTypeIcon.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                ((TrackRecordingActivity) getActivity()).chooseActivityType(category);
            }
            return true;
        });
        activityTypeIcon.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                ((TrackRecordingActivity) getActivity()).chooseActivityType(category);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);
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
        trackRecordingServiceConnection.unbind(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sensorHorizontalLine = null;

        heartRateGroup = null;
        heartRateValueView = null;
        heartRateSensorView = null;
        cadenceGroup = null;
        cadenceValueView = null;
        cadenceSensorView = null;

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

        elevationGroup = null;

        speedLabel = null;
        speedValue = null;
        speedUnit = null;

        elevationValue = null;
        elevationUnit = null;

        coordinateGroup = null;

        latitudeValue = null;
        longitudeValue = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
    }

    @Override
    public void onTrackUpdated(final Track track) {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    lastTrackStatistics = track != null ? track.getTrackStatistics() : null;
                    category = track != null ? track.getCategory() : "";
                    updateUI();
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
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    if (!isSelectedTrackRecording() || isSelectedTrackPaused()) {
                        lastTrackPoint = null;
                    }

                    TrackPoint trackPoint = lastTrackPoint; //NOTE: There seems to be a race condition; just fix the symptom for now.
                    if (trackPoint != null) {
                        boolean hasFix = !LocationUtils.isLocationOld(trackPoint.getLocation());
                        boolean hasGoodFix = TrackPointUtils.fulfillsAccuracy(trackPoint, recordingGpsAccuracy);

                        if (!hasFix || !hasGoodFix) {
                            lastTrackPoint = null;
                        }
                    }
                    setLocationValues();
                }
            });
        }
    }

    @Override
    public void clearMarkers() {
        // We don't care.
    }

    @Override
    public void onNewMarker(Marker marker) {
        // We don't care.
    }

    @Override
    public void onNewMarkersDone() {
        // We don't care.
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackRecordingActivity) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, true, false, true, true);
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
     * Tries to fetch most recent {@link SensorDataSet} from {@link de.dennisguse.opentracks.services.TrackRecordingService}.
     */
    private void updateSensorDataUI() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

        SensorDataSet sensorDataSet = null;
        if (trackRecordingService == null) {
            Log.d(TAG, "Cannot get the track recording service.");
        } else {
            sensorDataSet = trackRecordingService.getSensorData();
        }

        setHeartRateSensorData(sensorDataSet);
        setCadenceSensorData(sensorDataSet);
        setSpeedSensorData(sensorDataSet, isSelectedTrackRecording());
    }

    private void setHeartRateSensorData(SensorDataSet sensorDataSet) {
        int isVisible = View.VISIBLE;
        if (PreferencesUtils.isBluetoothHeartRateSensorAddressNone(getContext())) {
            isVisible = View.GONE;
        }
        heartRateGroup.setVisibility(isVisible);
        setVisibilitySensorHorizontalLine();

        String sensorValue = getContext().getString(R.string.value_unknown);
        String sensorName = getContext().getString(R.string.value_unknown);
        if (sensorDataSet != null && sensorDataSet.getHeartRate() != null) {
            SensorDataHeartRate data = sensorDataSet.getHeartRate();

            sensorName = data.getSensorName();
            if (data.hasHeartRate_bpm() && data.isRecent()) {
                sensorValue = StringUtils.formatDecimal(data.getHeartRate_bpm(), 0);
            }
        }

        heartRateSensorView.setText(sensorName);
        heartRateValueView.setText(sensorValue);
    }

    private void setCadenceSensorData(SensorDataSet sensorDataSet) {
        int isVisible = View.VISIBLE;
        if (PreferencesUtils.isBluetoothCyclingCadenceSensorAddressNone(getContext())) {
            isVisible = View.GONE;
        }
        cadenceGroup.setVisibility(isVisible);
        setVisibilitySensorHorizontalLine();

        String sensorValue = getContext().getString(R.string.value_unknown);
        String sensorName = getContext().getString(R.string.value_unknown);
        if (sensorDataSet != null && sensorDataSet.getCyclingCadence() != null) {
            SensorDataCycling.Cadence data = sensorDataSet.getCyclingCadence();
            sensorName = data.getSensorName();

            if (data.hasCadence_rpm() && data.isRecent()) {
                sensorValue = StringUtils.formatDecimal(data.getCadence_rpm(), 0);
            }
        }

        cadenceSensorView.setText(sensorName);
        cadenceValueView.setText(sensorValue);
    }

    /**
     * If cadence and hear rate groups are invisible then sensor horizontal line hast to be invisible too.
     */
    private void setVisibilitySensorHorizontalLine() {
        if (cadenceGroup.getVisibility() != View.VISIBLE && heartRateGroup.getVisibility() != View.VISIBLE) {
            sensorHorizontalLine.setVisibility(View.GONE);
        }

    }

    private void setSpeedSensorData(SensorDataSet sensorDataSet, boolean isRecording) {
        if (isRecording) {
            if (sensorDataSet != null && sensorDataSet.getCyclingSpeed() != null) {
                SensorDataCycling.Speed data = sensorDataSet.getCyclingSpeed();

                if (data.hasSpeed_mps() && data.isRecent()) {
                    setSpeed(data.getSpeed_mps());
                }
            }
        }
    }

    private void updateStats() {
        String trackIconValue = TrackIconUtils.getIconValue(getContext(), category);

        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext(), category);
        boolean isRecording = isSelectedTrackRecording();

        // Set total distance
        {
            double totalDistance = lastTrackStatistics == null ? Double.NaN : lastTrackStatistics.getTotalDistance();
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
        if (lastTrackStatistics != null) {
            movingTimeValue.setText(StringUtils.formatElapsedTime(lastTrackStatistics.getMovingTime()));
            updateTotalTime();
        }

        // Set average speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageSpeed() : Double.NaN;
            speedAvgLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedAvgValue.setText(parts.first);
            speedAvgUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = lastTrackStatistics == null ? Double.NaN : lastTrackStatistics.getMaxSpeed();

            speedMaxLabel.setText(reportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMaxValue.setText(parts.first);
            speedMaxUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageMovingSpeed() : Double.NaN;

            speedMovingLabel.setText(reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMovingValue.setText(parts.first);
            speedMovingUnit.setText(parts.second);
        }

        // Make elevation visible?
        {
            boolean showElevation = PreferencesUtils.isShowStatsElevation(getContext());
            elevationGroup.setVisibility(showElevation ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTotalTime() {
        long totalTime;
        if (isSelectedTrackRecording()) {
            totalTime = calculateTotalTime();
        } else {
            totalTime = lastTrackStatistics.getTotalTime();
        }
        totalTimeValueView.setText(StringUtils.formatElapsedTime(totalTime));
    }

    /**
     * Return time from service.
     * If service isn't bound then use lastTrackStatistics for calculate it.
     */
    private long calculateTotalTime() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        if (trackRecordingService != null) {
            return trackRecordingService.getTotalTime();
        } else {
            return System.currentTimeMillis() - lastTrackStatistics.getStopTime_ms() + lastTrackStatistics.getTotalTime();
        }
    }

    private void setLocationValues() {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());

        // Set speed/pace
        double speed = lastTrackPoint != null && lastTrackPoint.hasSpeed() ? lastTrackPoint.getSpeed() : Double.NaN;
        setSpeed(speed);

        // Set elevation
        boolean showElevation = PreferencesUtils.isShowStatsElevation(getContext());
        elevationGroup.setVisibility(showElevation ? View.VISIBLE : View.GONE);

        if (showElevation) {
            double altitude = lastTrackPoint != null && lastTrackPoint.hasAltitude() ? lastTrackPoint.getAltitude() : Double.NaN;
            Pair<String, String> parts = StringUtils.formatElevation(getContext(), altitude, metricUnits);

            elevationValue.setText(parts.first);
            elevationUnit.setText(parts.second);
        }

        // Set coordinate
        boolean showCoordinate = PreferencesUtils.isStatsShowCoordinate(getContext());

        coordinateGroup.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
        if (showCoordinate) {
            double latitude = lastTrackPoint != null ? lastTrackPoint.getLatitude() : Double.NaN;
            String latitudeText = Double.isNaN(latitude) || Double.isInfinite(latitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(latitude);
            latitudeValue.setText(latitudeText);

            double longitude = lastTrackPoint != null ? lastTrackPoint.getLongitude() : Double.NaN;
            String longitudeText = Double.isNaN(longitude) || Double.isInfinite(longitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(longitude);
            longitudeValue.setText(longitudeText);
        }
    }

    private void setSpeed(double speed) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext(), category);

        speedLabel.setText(reportSpeed ? R.string.stats_speed : R.string.stats_pace);

        Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
        speedValue.setText(parts.first);
        speedUnit.setText(parts.second);
    }
}
