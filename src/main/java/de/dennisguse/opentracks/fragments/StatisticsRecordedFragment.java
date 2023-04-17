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
import android.util.Log;
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

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.StatisticsRecordedBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

/**
 * A fragment to display track statistics to the user for a recorded {@link Track}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordedFragment extends Fragment {

    //private static final String TAG = StatisticsRecordedFragment.class.getSimpleName();

    private static final String TRACK_ID_KEY = "trackId";

    public static StatisticsRecordedFragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        StatisticsRecordedFragment fragment = new StatisticsRecordedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private SensorStatistics sensorStatistics;

    private Track.Id trackId;
    @Nullable // Lazily loaded.
    private Track track;

    private ContentProviderUtils contentProviderUtils;

    private StatisticsRecordedBinding viewBinding;

    private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();
    private boolean preferenceReportSpeed;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        boolean updateUInecessary = false;

        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            updateUInecessary = true;
            unitSystem = PreferencesUtils.getUnitSystem();
        }

        if (PreferencesUtils.isKey(R.string.stats_rate_key, key) && track != null) {
            updateUInecessary = true;
            preferenceReportSpeed = PreferencesUtils.isReportSpeed(track.getCategory());
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getArguments().getParcelable(TRACK_ID_KEY);
        contentProviderUtils = new ContentProviderUtils(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = StatisticsRecordedBinding.inflate(inflater, container, false);

        RecyclerView sensorsRecyclerView = viewBinding.statsSensorsRecyclerView;
        sensorsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//        sensorsRecyclerView.setAdapter(sensorsAdapter);

        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        loadStatistics();
    }

    @Override
    public void onPause() {
        super.onPause();

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    public void loadStatistics() {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    Track track = contentProviderUtils.getTrack(trackId);
                    if (track == null) {
                        Log.e(StatisticsRecordedFragment.class.getSimpleName(), "track cannot be null");
                        getActivity().finish();
                        return;
                    }

                    sensorStatistics = contentProviderUtils.getSensorStats(trackId);

                    boolean prefsChanged = this.track == null || (!this.track.getCategory().equals(track.getCategory()));
                    this.track = track;
                    if (prefsChanged) {
                        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, getString(R.string.stats_rate_key));
                    }

                    loadTrackDescription(track);
                    updateUI();
                    updateSensorUI();

                    ((TrackRecordedActivity) getActivity()).startPostponedEnterTransitionWith(viewBinding.statsActivityTypeIcon);
                }
            });
        }
    }

    private void loadTrackDescription(@NonNull Track track) {
        viewBinding.statsNameValue.setText(track.getName());
        viewBinding.statsDescriptionValue.setText(track.getDescription());
        viewBinding.statsStartDatetimeValue.setText(StringUtils.formatDateTimeWithOffsetIfDifferent(track.getStartTime()));
    }

    private void updateUI() {
        TrackStatistics trackStatistics = track.getTrackStatistics();

        // Set total distance
        {
            Pair<String, String> parts = DistanceFormatter.Builder()
                    .setUnit(unitSystem)
                    .build(getContext()).getDistanceParts(trackStatistics.getTotalDistance());

            viewBinding.statsDistanceValue.setText(parts.first);
            viewBinding.statsDistanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            String trackIconValue = TrackIconUtils.getIconValue(getContext(), track.getCategory());
            viewBinding.statsActivityTypeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), TrackIconUtils.getIconDrawable(trackIconValue)));
        }

        // Set time and start datetime
        {
            viewBinding.statsMovingTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getMovingTime()));
            viewBinding.statsTotalTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getTotalTime()));
        }

        SpeedFormatter formatter = SpeedFormatter.getBuilder().setUnit(unitSystem).setReportSpeedOrPace(preferenceReportSpeed).build(getContext());
        // Set average speed/pace
        {
            viewBinding.statsAverageSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getAverageSpeed());
            viewBinding.statsAverageSpeedValue.setText(parts.first);
            viewBinding.statsAverageSpeedUnit.setText(parts.second);
        }


        // Set max speed/pace
        {
            viewBinding.statsMaxSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getMaxSpeed());
            viewBinding.statsMaxSpeedValue.setText(parts.first);
            viewBinding.statsMaxSpeedUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            viewBinding.statsMovingSpeedLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = formatter.getSpeedParts(trackStatistics.getAverageMovingSpeed());
            viewBinding.statsMovingSpeedValue.setText(parts.first);
            viewBinding.statsMovingSpeedUnit.setText(parts.second);
        }

        // Set altitude gain and loss
        {
            Float altitudeGain = trackStatistics.getTotalAltitudeGain();
            Float altitudeLoss = trackStatistics.getTotalAltitudeLoss();

            Pair<String, String> parts;

            parts = StringUtils.getAltitudeParts(getContext(), altitudeGain, unitSystem);
            viewBinding.statsAltitudeGainValue.setText(parts.first);
            viewBinding.statsAltitudeGainUnit.setText(parts.second);

            parts = StringUtils.getAltitudeParts(getContext(), altitudeLoss, unitSystem);
            viewBinding.statsAltitudeLossValue.setText(parts.first);
            viewBinding.statsAltitudeLossUnit.setText(parts.second);

            boolean show = altitudeGain != null && altitudeLoss != null;
            viewBinding.statsAltitudeGroup.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Decide whether to show an additional view for pace/speed
        boolean showFastestAvgSpeed = PreferencesUtils.shouldShouldFastestAndAvgSpeed();
        if (showFastestAvgSpeed) {
            viewBinding.statsSpeedGroup2.setVisibility(View.VISIBLE);
        } else {
            viewBinding.statsSpeedGroup2.setVisibility(View.GONE);
        }

        SpeedFormatter formatter2 = SpeedFormatter.getBuilder().setUnit(unitSystem).setReportSpeedOrPace(!preferenceReportSpeed).build(getContext());
        // Set average pace/speed
        {
            viewBinding.statsAveragePaceLabel.setText(preferenceReportSpeed ? R.string.stats_average_pace : R.string.stats_average_speed);

            Pair<String, String> parts = formatter2.getSpeedParts(trackStatistics.getAverageSpeed());
            viewBinding.statsAveragePaceValue.setText(parts.first);
            viewBinding.statsAveragePaceUnit.setText(parts.second);
        }

        // Set max pace/speed
        {
            viewBinding.statsMaxPaceLabel.setText(preferenceReportSpeed ? R.string.stats_fastest_pace : R.string.stats_max_speed);

            Pair<String, String> parts = formatter2.getSpeedParts(trackStatistics.getMaxSpeed());
            viewBinding.statsMaxPaceValue.setText(parts.first);
            viewBinding.statsMaxPaceUnit.setText(parts.second);
        }

        // Set moving pace/speed
        {
            viewBinding.statsMovingPaceLabel.setText(preferenceReportSpeed ? R.string.stats_average_moving_pace : R.string.stats_average_moving_speed);

            Pair<String, String> parts = formatter2.getSpeedParts(trackStatistics.getAverageMovingSpeed());
            viewBinding.statsMovingPaceValue.setText(parts.first);
            viewBinding.statsMovingPaceUnit.setText(parts.second);
        }
    }

    private void updateSensorUI() {
        if (sensorStatistics == null) {
        }
        //TODO
    }
}