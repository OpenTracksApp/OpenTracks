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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.adapters.SensorsAdapter;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.viewmodels.SensorDataModel;

/**
 * A fragment to display track statistics to the user for a currently recording {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordingFragment extends Fragment implements TrackDataListener {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    private static final Duration UI_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private TrackDataHub trackDataHub;

    private Handler handlerUpdateUI;

    private TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection();
    private TrackPoint lastTrackPoint;

    @Nullable // Lazily loaded.
    private Track track;

    private StatisticsRecordingBinding viewBinding;
    private SensorsAdapter sensorsAdapter;

    private SharedPreferences sharedPreferences;
    private boolean preferenceMetricUnits;
    private boolean preferenceReportSpeed;
    private boolean preferenceShowAltitude;
    private boolean preferenceShowCoordinate;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
            updateUInecessary = true;
            preferenceMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key) && track != null) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), track.getCategory());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_show_grade_altitude_key, key)) {
            updateUInecessary = true;
            preferenceShowAltitude = PreferencesUtils.isShowStatsAltitude(sharedPreferences, getContext());
        }

        if (PreferencesUtils.isKey(getContext(), R.string.stats_show_coordinate_key, key)) {
            updateUInecessary = true;
            preferenceShowCoordinate = PreferencesUtils.isStatsShowCoordinate(sharedPreferences, getContext());
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    updateUI();
                }
            });
        }
    };

    private final Runnable updateUIeachSecond = new Runnable() {
        public void run() {
            if (isResumed() && isSelectedTrackRecording()) {
                if (!isSelectedTrackPaused() && track != null) {
                    synchronized (this) {
                        if (lastTrackPoint != null && lastTrackPoint.hasLocation() && !lastTrackPoint.isRecent()) {
                            lastTrackPoint = null;
                            setLocationValues();
                        }
                    }
                    updateTotalTime();
                    updateSensorDataUI();
                }

                handlerUpdateUI.postDelayed(this, UI_UPDATE_INTERVAL.toMillis());
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handlerUpdateUI = new Handler();

        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());

        sensorsAdapter = new SensorsAdapter(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);
        viewBinding.statsActivityTypeIcon.setOnClickListener(v -> ((TrackRecordingActivity) getActivity()).chooseActivityType(track != null ? track.getCategory() : "")); //TODO "" would be useless as Track was not yet loaded.

        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        sensorsRecyclerView.setAdapter(sensorsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        trackRecordingServiceConnection.startConnection(getContext());

        updateUIeachSecond.run();
        handlerUpdateUI.post(updateUIeachSecond);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

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
        sharedPreferences = null;
    }

    @Override
    public void onTrackUpdated(final Track track) {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    if ((this.track == null && track != null) || (this.track != null && track != null && !this.track.getCategory().equals(track.getCategory()))) {
                        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.stats_rate_key));
                    }
                    this.track = track;
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
    public void onNewTrackPointsDone(@NonNull TrackPoint newLastTrackPoint, @NonNull TrackStatistics trackStatistics) {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    synchronized (this) {
                        this.lastTrackPoint = newLastTrackPoint;

                        if (!isSelectedTrackRecording() || isSelectedTrackPaused()) {
                            this.lastTrackPoint = null;
                        }

                        if (this.lastTrackPoint != null && this.lastTrackPoint.hasLocation() && !this.lastTrackPoint.isRecent()) {
                            this.lastTrackPoint = null;
                        }
                        setLocationValues();
                    }
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
     * Also sets altitude gain and loss.
     */
    private void updateSensorDataUI() {
        TrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();

        if (trackRecordingService == null) {
            Log.d(TAG, "Cannot get the track recording service.");
        } else {
            SensorDataSet sensorDataSet = trackRecordingService.getSensorDataSet();
            if (sensorDataSet != null) {
                List<SensorDataModel> sensorDataList = new ArrayList<>();
                if (sensorDataSet.getHeartRate() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getHeartRate()));
                }
                if (sensorDataSet.getCyclingCadence() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getCyclingCadence()));
                }
                if (sensorDataSet.getCyclingPower() != null) {
                    sensorDataList.add(new SensorDataModel(sensorDataSet.getCyclingPower()));
                }
                sensorsAdapter.swapData(sensorDataList);
                setSpeedSensorData(sensorDataSet);
            }
            //TODO Check if we can get the total altitude gain and loss via trackStatistics instead of doing some computation in the UI layer.
            setTotalAltitudeGain(trackRecordingService.getAltitudeGain_m());
            setTotalAltitudeLoss(trackRecordingService.getAltitudeLoss_m());
        }
    }

    // Set altitude gain
    private void setTotalAltitudeGain(Float altitudeGain_m) {
        Float totalAltitudeGain = altitudeGain_m;

        if (track != null && track.getTrackStatistics().hasTotalAltitudeGain()) {
            if (altitudeGain_m == null) {
                totalAltitudeGain = 0f;
            }

            totalAltitudeGain += track.getTrackStatistics().getTotalAltitudeGain();
        }

        Pair<String, String> parts = StringUtils.formatAltitude(getContext(), totalAltitudeGain, preferenceMetricUnits);
        viewBinding.statsAltitudeGainValue.setText(parts.first);
        viewBinding.statsAltitudeGainUnit.setText(parts.second);
    }

    // Set altitude loss
    private void setTotalAltitudeLoss(Float altitudeLoss_m) {
        Float totalAltitudeLoss = altitudeLoss_m;

        if (track != null && track.getTrackStatistics().hasTotalAltitudeLoss()) {
            if (altitudeLoss_m == null) {
                totalAltitudeLoss = track.getTrackStatistics().getTotalAltitudeLoss();
            } else {
                totalAltitudeLoss += track.getTrackStatistics().getTotalAltitudeLoss();
            }
        }

        Pair<String, String> parts = StringUtils.formatAltitude(getContext(), totalAltitudeLoss, preferenceMetricUnits);
        viewBinding.statsAltitudeLossValue.setText(parts.first);
        viewBinding.statsAltitudeLossUnit.setText(parts.second);
    }

    private void setSpeedSensorData(SensorDataSet sensorDataSet) {
        if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null) {
            SensorDataCycling.DistanceSpeed data = sensorDataSet.getCyclingDistanceSpeed();

            if (data.hasValue() && data.isRecent()) {
                setTotalDistance(data.getValue().distanceOverall);
                setSpeed(data.getValue().speed);
            }
            if (data.hasValue() && data.isRecent()) {
                setSpeed(data.getValue().speed);
            }
        }
    }

    private void updateStats() {

        setTotalDistance(Distance.of(0)); //TODO Why?

        // Set activity type
        if (track != null) {
            String trackIconValue = TrackIconUtils.getIconValue(getContext(), track.getCategory());
            viewBinding.statsActivityTypeIcon.setEnabled(isSelectedTrackRecording());
            viewBinding.statsActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(trackIconValue));
        }

        // Set time
        if (track != null) {
            viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(track.getTrackStatistics().getMovingTime()));
            updateTotalTime();
        }

        // Set average speed/pace
        {
            Speed speed = track != null ? track.getTrackStatistics().getAverageSpeed() : null;
            viewBinding.statsAverageSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            Speed speed = track != null ? track.getTrackStatistics().getMaxSpeed() : null;

            viewBinding.statsMaxSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            Speed speed = track != null ? track.getTrackStatistics().getAverageMovingSpeed() : null;

            viewBinding.statsMovingSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set altitude gain and loss
        {
            viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTotalTime() {
        Duration totalTime = track.getTrackStatistics().getTotalTime();
        if (isSelectedTrackRecording()) {
            TrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
            if (trackRecordingService != null) {
                totalTime = trackRecordingService.getTotalTime();
            }
        }

        viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(totalTime));
    }

    private void setLocationValues() {
        // Set speed/pace
        Speed speed = lastTrackPoint != null && lastTrackPoint.hasSpeed() ? lastTrackPoint.getSpeed() : null;
        setSpeed(speed);

        // Set altitude
        viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);

        if (preferenceShowAltitude) {
            // Current altitude
            Float altitude = lastTrackPoint != null && lastTrackPoint.hasAltitude() ? (float) lastTrackPoint.getAltitude() : null;
            Pair<String, String> parts = StringUtils.formatAltitude(getContext(), altitude, preferenceMetricUnits);
            viewBinding.statsAltitudeCurrentValue.setText(parts.first);
            viewBinding.statsAltitudeCurrentUnit.setText(parts.second);
        }

        // Set coordinate
        viewBinding.statsCoordinateGroup.setVisibility(preferenceShowCoordinate ? View.VISIBLE : View.GONE);
        if (preferenceShowCoordinate) {
            String latitudeText = getContext().getString(R.string.value_unknown);
            String longitudeText = getContext().getString(R.string.value_unknown);
            if (lastTrackPoint != null && lastTrackPoint.hasLocation()) {
                latitudeText = StringUtils.formatCoordinate(lastTrackPoint.getLatitude());
                longitudeText = StringUtils.formatCoordinate(lastTrackPoint.getLongitude());
            }
            viewBinding.statsLatitudeValue.setText(latitudeText);
            viewBinding.statsLongitudeValue.setText(longitudeText);
        }
    }

    private void setTotalDistance(Distance sensorDistanceSinceLastTrackpoint) {
        Distance totalDistance = track != null ? (track.getTrackStatistics().getTotalDistance().plus(sensorDistanceSinceLastTrackpoint)) : Distance.invalid();
        Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), totalDistance, preferenceMetricUnits);

        viewBinding.statsDistanceValue.setText(parts.first);
        viewBinding.statsDistanceUnit.setText(parts.second);
    }

    private void setSpeed(Speed speed) {
        viewBinding.statsSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_speed : R.string.stats_pace);

        Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
        viewBinding.statsSpeedValue.setText(parts.first);
        viewBinding.statsSpeedUnit.setText(parts.second);
    }
}
