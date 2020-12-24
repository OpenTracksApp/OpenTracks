package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.adapters.SensorsAdapter;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorData;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.TrackRecordingServiceInterface;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to display track statistics to the user for a currently recording {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
//TODO isRecording should not be relevant anymore as we now have StatisticRecordedFragment.
//TODO During updateUI(): do not call PreferenceUtils (it is slow) rather use sharedPreferenceChangeListener.
public class StatisticsRecordingFragment extends Fragment implements TrackDataListener {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    private static final long UI_UPDATE_INTERVAL = UnitConversions.ONE_SECOND_MS;

    private TrackDataHub trackDataHub;
    private Handler handlerUpdateUI;

    private TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection();

    private TrackPoint lastTrackPoint;
    private TrackStatistics lastTrackStatistics;

    private String category = "";

    private StatisticsRecordingBinding viewBinding;

    private SensorsAdapter sensorsAdapter;

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (preferences, key) -> {
        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key) || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            if (isResumed()) {
                getActivity().runOnUiThread(() -> {
                    if (isResumed()) {
                        updateUI();
                    }
                });
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        handlerUpdateUI = new Handler();


        viewBinding.statsActivityTypeIcon.setOnClickListener(v -> ((TrackRecordingActivity) getActivity()).chooseActivityType(category));

        sensorsAdapter = new SensorsAdapter(getContext());
        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        sensorsRecyclerView.setAdapter(sensorsAdapter);
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
        viewBinding = null;
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

                        if (!hasFix) {
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
     * Also sets elevation gain and loss.
     */
    private void updateSensorDataUI() {
        TrackRecordingServiceInterface trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

        if (trackRecordingService == null) {
            Log.d(TAG, "Cannot get the track recording service.");
        } else {
            SensorDataSet sensorDataSet = trackRecordingService.getSensorData();
            if (sensorDataSet != null) {
                List<Pair<Integer, SensorData>> sensorDataList = new ArrayList<>();
                if (sensorDataSet.getHeartRate() != null) {
                    sensorDataList.add(new Pair<>(SensorsAdapter.HEART_RATE_TYPE, sensorDataSet.getHeartRate()));
                }
                if (sensorDataSet.getCyclingCadence() != null) {
                    sensorDataList.add(new Pair<>(SensorsAdapter.CADENCE_TYPE, sensorDataSet.getCyclingCadence()));
                }
                if(sensorDataSet.getCyclingPower() != null) {
                    sensorDataList.add(new Pair<>(SensorsAdapter.POWER_TYPE, sensorDataSet.getCyclingPower()));
                }
                sensorsAdapter.swapData(sensorDataList);
                setSpeedSensorData(sensorDataSet);
            }
            //TODO Check if we can distribute the total elevation gain and loss via trackStatistics instead of doing some computation in the UI layer.
            setTotalElevationGain(trackRecordingService.getElevationGain_m());
            setTotalElevationLoss(trackRecordingService.getElevationLoss_m());
        }
    }

    // Set elevation gain
    private void setTotalElevationGain(Float elevationGain_m) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());

        Float totalElevationGain = elevationGain_m;

        if (lastTrackStatistics != null && lastTrackStatistics.hasTotalElevationGain()) {
            if (elevationGain_m == null) {
                totalElevationGain = lastTrackStatistics.getTotalElevationGain();
            } else {
                totalElevationGain += lastTrackStatistics.getTotalElevationGain();
            }
        }

        Pair<String, String> parts = StringUtils.formatElevation(getContext(), totalElevationGain, metricUnits);
        viewBinding.statsElevationGainValue.setText(parts.first);
        viewBinding.statsElevationGainUnit.setText(parts.second);
    }

    // Set elevation loss
    private void setTotalElevationLoss(Float elevationLoss_m) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());

        Float totalElevationLoss = elevationLoss_m;

        if (lastTrackStatistics != null && lastTrackStatistics.hasTotalElevationLoss()) {
            if (elevationLoss_m == null) {
                totalElevationLoss = lastTrackStatistics.getTotalElevationLoss();
            } else {
                totalElevationLoss += lastTrackStatistics.getTotalElevationLoss();
            }
        }

        Pair<String, String> parts = StringUtils.formatElevation(getContext(), totalElevationLoss, metricUnits);
        viewBinding.statsElevationLossValue.setText(parts.first);
        viewBinding.statsElevationLossUnit.setText(parts.second);
    }

    private void setSpeedSensorData(SensorDataSet sensorDataSet) {
        if (sensorDataSet != null && sensorDataSet.getCyclingSpeed() != null) {
            SensorDataCycling.Speed data = sensorDataSet.getCyclingSpeed();

            if (data.hasSpeed_mps() && data.isRecent()) {
                setSpeed(data.getSpeed_mps());
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

            viewBinding.statsDistanceValue.setText(parts.first);
            viewBinding.statsDistanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            viewBinding.statsActivityTypeIcon.setEnabled(isRecording);
            viewBinding.statsActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(trackIconValue));
        }

        // Set time
        if (lastTrackStatistics != null) {
            viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(lastTrackStatistics.getMovingTime()));
            updateTotalTime();
        }

        // Set average speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageSpeed() : Double.NaN;
            viewBinding.statsAverageSpeedLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = lastTrackStatistics == null ? Double.NaN : lastTrackStatistics.getMaxSpeed();

            viewBinding.statsMaxSpeedLabel.setText(reportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = lastTrackStatistics != null ? lastTrackStatistics.getAverageMovingSpeed() : Double.NaN;

            viewBinding.statsMovingSpeedLabel.setText(reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set elevation gain and loss
        {
            // Make elevation visible?
            boolean showElevation = PreferencesUtils.isShowStatsElevation(getContext());
            viewBinding.statsElevationGroup.setVisibility(showElevation ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTotalTime() {
        long totalTime;
        if (isSelectedTrackRecording()) {
            totalTime = calculateTotalTime();
        } else {
            totalTime = lastTrackStatistics.getTotalTime();
        }
        viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(totalTime));
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
        viewBinding.statsElevationGroup.setVisibility(showElevation ? View.VISIBLE : View.GONE);

        if (showElevation) {
            // Current elevation
            Float altitude = lastTrackPoint != null && lastTrackPoint.hasAltitude() ? (float) lastTrackPoint.getAltitude() : null;
            Pair<String, String> parts = StringUtils.formatElevation(getContext(), altitude, metricUnits);
            viewBinding.statsElevationCurrentValue.setText(parts.first);
            viewBinding.statsElevationCurrentUnit.setText(parts.second);
        }

        // Set coordinate
        boolean showCoordinate = PreferencesUtils.isStatsShowCoordinate(getContext());

        viewBinding.statsCoordinateGroup.setVisibility(showCoordinate ? View.VISIBLE : View.GONE);
        if (showCoordinate) {
            double latitude = lastTrackPoint != null ? lastTrackPoint.getLatitude() : Double.NaN;
            String latitudeText = Double.isNaN(latitude) || Double.isInfinite(latitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(latitude);
            viewBinding.statsLatitudeValue.setText(latitudeText);

            double longitude = lastTrackPoint != null ? lastTrackPoint.getLongitude() : Double.NaN;
            String longitudeText = Double.isNaN(longitude) || Double.isInfinite(longitude) ? getContext().getString(R.string.value_unknown) : StringUtils.formatCoordinate(longitude);
            viewBinding.statsLongitudeValue.setText(longitudeText);
        }
    }

    private void setSpeed(double speed) {
        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext(), category);

        viewBinding.statsSpeedLabel.setText(reportSpeed ? R.string.stats_speed : R.string.stats_pace);

        Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
        viewBinding.statsSpeedValue.setText(parts.first);
        viewBinding.statsSpeedUnit.setText(parts.second);
    }
}
