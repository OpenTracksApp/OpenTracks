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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.adapters.SensorsAdapter;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.databinding.StatisticsRecordedBinding;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.viewmodels.SensorDataModel;

/**
 * A fragment to display track statistics to the user for a recorded {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordedFragment extends Fragment {

    private static final String TRACK_ID_KEY = "trackId";

    public static StatisticsRecordedFragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        StatisticsRecordedFragment fragment = new StatisticsRecordedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private TrackStatistics trackStatistics;
    private SensorStatistics sensorStatistics;
    private SensorsAdapter sensorsAdapter;
    private String category = "";
    private Track.Id trackId;
    private ContentProviderUtils contentProviderUtils;

    private StatisticsRecordedBinding viewBinding;

    private boolean preferenceMetricUnits;
    private boolean preferenceReportSpeed;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (key == null || PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
            updateUInecessary = true;
            preferenceMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
        }

        if (key == null || PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), category);
        }

        if (key != null && updateUInecessary && isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    updateUI();
                }
            });
        }
    };


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordedBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sensorsAdapter = new SensorsAdapter(getContext());
        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        sensorsRecyclerView.setAdapter(sensorsAdapter);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getArguments().getParcelable(TRACK_ID_KEY);
        contentProviderUtils = new ContentProviderUtils(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        loadStatistics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;

        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);
    }

    public void loadStatistics() {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    Track track = contentProviderUtils.getTrack(trackId);
                    trackStatistics = track != null ? track.getTrackStatistics() : null;
                    sensorStatistics = contentProviderUtils.getSensorStats(trackId);
                    category = track != null ? track.getCategory() : "";
                    updateUI();
                    updateSensorUI();
                }
            });
        }
    }

    private void updateUI() {
        // Set total distance
        {
            double totalDistance = trackStatistics == null ? Double.NaN : trackStatistics.getTotalDistance();
            Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), totalDistance, preferenceMetricUnits);

            viewBinding.statsDistanceValue.setText(parts.first);
            viewBinding.statsDistanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            String trackIconValue = TrackIconUtils.getIconValue(getContext(), category);
            viewBinding.statsActivityTypeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), TrackIconUtils.getIconDrawable(trackIconValue)));
        }

        // Set time and start datetime
        if (trackStatistics != null) {
            viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getMovingTime()));
            viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getTotalTime()));
        }

        // Set average speed/pace
        {
            double speed = trackStatistics != null ? trackStatistics.getAverageSpeed() : Double.NaN;
            viewBinding.statsAverageSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = trackStatistics == null ? Double.NaN : trackStatistics.getMaxSpeed();

            viewBinding.statsMaxSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = trackStatistics != null ? trackStatistics.getAverageMovingSpeed() : Double.NaN;

            viewBinding.statsMovingSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, preferenceMetricUnits, preferenceReportSpeed);
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set elevation gain and loss
        {
            // Make elevation visible?
            boolean showElevation = PreferencesUtils.isShowStatsElevation(getContext());
            viewBinding.statsElevationGroup.setVisibility(showElevation ? View.VISIBLE : View.GONE);

            Float elevationGain_m = trackStatistics != null ? trackStatistics.getTotalElevationGain() : null;
            Float elevationLoss_m = trackStatistics != null ? trackStatistics.getTotalElevationLoss() : null;

            Pair<String, String> parts;

            parts = StringUtils.formatElevation(getContext(), elevationGain_m, preferenceMetricUnits);
            viewBinding.statsElevationGainValue.setText(parts.first);
            viewBinding.statsElevationGainUnit.setText(parts.second);

            parts = StringUtils.formatElevation(getContext(), elevationLoss_m, preferenceMetricUnits);
            viewBinding.statsElevationLossValue.setText(parts.first);
            viewBinding.statsElevationLossUnit.setText(parts.second);
        }
    }

    private void updateSensorUI() {
        if (sensorStatistics == null) {
            return;
        }

        List<SensorDataModel> sensorDataList = new ArrayList<>();
        if (sensorStatistics.hasHeartRate()) {
            sensorDataList.add(new SensorDataModel(R.string.sensor_state_heart_rate_max, R.string.sensor_unit_beats_per_minute, sensorStatistics.getMaxHeartRate()));
            sensorDataList.add(new SensorDataModel(R.string.sensor_state_heart_rate_avg, R.string.sensor_unit_beats_per_minute, sensorStatistics.getAvgHeartRate()));
        }
        if (sensorStatistics.hasCadence()) {
            sensorDataList.add(new SensorDataModel(R.string.sensor_state_cadence_max, R.string.sensor_unit_rounds_per_minute, sensorStatistics.getMaxCadence()));
            sensorDataList.add(new SensorDataModel(R.string.sensor_state_cadence_avg, R.string.sensor_unit_rounds_per_minute, sensorStatistics.getAvgCadence()));
        }
        if (sensorStatistics.hasPower()) {
            sensorDataList.add(new SensorDataModel(R.string.sensor_state_power_avg, R.string.sensor_unit_power, sensorStatistics.getAvgPower()));
        }
        if (sensorDataList.size() > 0) {
            sensorsAdapter.swapData(sensorDataList);
        }
    }
}
