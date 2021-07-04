package de.dennisguse.opentracks.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.databinding.StatisticsRecordingBinding;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
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
public class StatisticsRecordingFragment extends Fragment {

    private static final String TAG = StatisticsRecordingFragment.class.getSimpleName();

    public static Fragment newInstance() {
        return new StatisticsRecordingFragment();
    }

    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private TrackRecordingService.RecordingData recordingData = TrackRecordingService.NOT_RECORDING;
    private TrackPoint latestTrackPoint;

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

        if (PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key) && recordingData != null) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), recordingData.getTrackCategory());
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
            getActivity().runOnUiThread(this::updateUI);
        }
    };

    private final Runnable bindChangedCallback = new Runnable() {
        @Override
        public void run() {
            TrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
            if (service == null) {
                Log.w(TAG, "could not get TrackRecordingService");
                return;
            }

            service.getRecordingDataObservable()
                    .observe(StatisticsRecordingFragment.this, recordingData -> onRecordingDataChanged(recordingData));
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(bindChangedCallback);

        sensorsAdapter = new SensorsAdapter(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordingBinding.inflate(inflater, container, false);
        viewBinding.statsActivityTypeIcon.setOnClickListener(v -> ((TrackRecordingActivity) getActivity()).chooseActivityType(recordingData.getTrackCategory()));

        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        sensorsRecyclerView.setAdapter(sensorsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        trackRecordingServiceConnection.startConnection(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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

    private void updateUI() {
        if (isResumed()) {
            updateStats();
            setLocationValues();
            updateSensorDataUI();
        }
    }

    /**
     * Tries to fetch most recent {@link SensorDataSet} from {@link de.dennisguse.opentracks.services.TrackRecordingService}.
     * Also sets altitude gain and loss.
     */
    private void updateSensorDataUI() {
        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
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
        }

        {
            Pair<String, String> parts = StringUtils.getAltitudeParts(getContext(), recordingData.getTrackStatistics().getTotalAltitudeGain(), preferenceMetricUnits);
            viewBinding.statsAltitudeGainValue.setText(parts.first);
            viewBinding.statsAltitudeGainUnit.setText(parts.second);
        }
        {
            Pair<String, String> parts = StringUtils.getAltitudeParts(getContext(), recordingData.getTrackStatistics().getTotalAltitudeLoss(), preferenceMetricUnits);
            viewBinding.statsAltitudeLossValue.setText(parts.first);
            viewBinding.statsAltitudeLossUnit.setText(parts.second);
        }
    }

    private void updateStats() {
        // Set activity type
        String trackIconValue = TrackIconUtils.getIconValue(getContext(), recordingData.getTrackCategory());
        viewBinding.statsActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(trackIconValue));

        // Set time
        viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(recordingData.getTrackStatistics().getMovingTime()));
        updateTotalTime();

        // Set average speed/pace
        {
            Speed speed = recordingData.getTrackStatistics().getAverageSpeed();
            viewBinding.statsAverageSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            Speed speed = recordingData.getTrackStatistics().getMaxSpeed();

            viewBinding.statsMaxSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            Speed speed = recordingData.getTrackStatistics().getAverageMovingSpeed();

            viewBinding.statsMovingSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set altitude gain and loss
        viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);
    }

    private void updateTotalTime() {
        Duration totalTime = recordingData.getTrackStatistics().getTotalTime();
        viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(totalTime));
    }

    private void setLocationValues() {
        // Set speed/pace
        Speed speed = latestTrackPoint != null && latestTrackPoint.hasSpeed() ? latestTrackPoint.getSpeed() : null;

        SensorDataSet sensorDataSet = recordingData.getSensorDataSet();
        if (sensorDataSet != null && sensorDataSet.getCyclingDistanceSpeed() != null) {
            SensorDataCycling.DistanceSpeed data = sensorDataSet.getCyclingDistanceSpeed();
            if (data.hasValue() && data.isRecent()) {
                speed = data.getValue().speed;
            }
        }

        setSpeed(speed);

        // Set distance
        {
            Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), recordingData.getTrackStatistics().getTotalDistance(), preferenceMetricUnits);

            viewBinding.statsDistanceValue.setText(parts.first);
            viewBinding.statsDistanceUnit.setText(parts.second);
        }
        // Set altitude
        viewBinding.statsAltitudeGroup.setVisibility(preferenceShowAltitude ? View.VISIBLE : View.GONE);

        if (preferenceShowAltitude) {
            // Current altitude
            Float altitude = null;
            int labelId = R.string.value_unknown;
            if (latestTrackPoint != null && latestTrackPoint.hasAltitude()) {
                altitude = (float) latestTrackPoint.getAltitude().toM();
                labelId = latestTrackPoint.getAltitude().getLabelId();
            }

            Pair<String, String> parts = StringUtils.getAltitudeParts(getContext(), altitude, preferenceMetricUnits);
            viewBinding.statsAltitudeCurrentValue.setText(parts.first);
            viewBinding.statsAltitudeCurrentUnit.setText(parts.second);

            viewBinding.statsAltitudeCurrentLabelEgm.setText(labelId);
        }

        // Set coordinate
        viewBinding.statsCoordinateGroup.setVisibility(preferenceShowCoordinate ? View.VISIBLE : View.GONE);
        if (preferenceShowCoordinate) {
            String latitudeText = getContext().getString(R.string.value_unknown);
            String longitudeText = getContext().getString(R.string.value_unknown);
            if (latestTrackPoint != null && latestTrackPoint.hasLocation()) {
                latitudeText = StringUtils.formatCoordinate(latestTrackPoint.getLatitude());
                longitudeText = StringUtils.formatCoordinate(latestTrackPoint.getLongitude());
            }
            viewBinding.statsLatitudeValue.setText(latitudeText);
            viewBinding.statsLongitudeValue.setText(longitudeText);
        }
    }

    private void setSpeed(Speed speed) {
        viewBinding.statsSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_speed : R.string.stats_pace);

        Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
        viewBinding.statsSpeedValue.setText(parts.first);
        viewBinding.statsSpeedUnit.setText(parts.second);
    }

    private void onRecordingDataChanged(TrackRecordingService.RecordingData recordingData) {
        String oldCategory = this.recordingData.getTrackCategory();
        String newCategory = recordingData.getTrackCategory();
        this.recordingData = recordingData;

        if (!oldCategory.equals(newCategory)) {
            sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.stats_rate_key));
        }

        latestTrackPoint = recordingData.getLatestTrackPoint();
        if (latestTrackPoint != null && latestTrackPoint.hasLocation() && !latestTrackPoint.isRecent()) {
            latestTrackPoint = null;
        }

        updateUI();
    }
}
