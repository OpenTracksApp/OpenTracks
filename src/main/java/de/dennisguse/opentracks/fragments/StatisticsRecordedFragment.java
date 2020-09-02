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

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

/**
 * A fragment to display track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatisticsRecordedFragment extends Fragment {

    private static final String TRACK_ID_KEY = "trackId";

    private TrackStatistics trackStatistics;
    private String category = "";
    private Track track;

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
        return inflater.inflate(R.layout.statistics_recorded, container, false);
    }

    private TextView totalTimeValueView;
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

    public static StatisticsRecordedFragment newInstance(Track.Id trackId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(TRACK_ID_KEY, trackId);

        StatisticsRecordedFragment fragment = new StatisticsRecordedFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Track.Id trackId = getArguments().getParcelable(TRACK_ID_KEY);
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(getContext());
        track = contentProviderUtils.getTrack(trackId);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Spinner activityTypeIcon = getView().findViewById(R.id.stats_activity_type_icon);
        activityTypeIcon.setAdapter(TrackIconUtils.getIconSpinnerAdapter(getActivity(), ""));
        activityTypeIcon.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                ((TrackRecordedActivity) getActivity()).chooseActivityType(category);
            }
            return true;
        });
        activityTypeIcon.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                ((TrackRecordedActivity) getActivity()).chooseActivityType(category);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);

        loadStatistics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);

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
    }

    public void loadStatistics() {
        if (isResumed()) {
            getActivity().runOnUiThread(() -> {
                if (isResumed()) {
                    trackStatistics = track != null ? track.getTrackStatistics() : null;
                    category = track != null ? track.getCategory() : "";
                    updateUI();
                }
            });
        }
    }

    private void updateUI() {
        String trackIconValue = TrackIconUtils.getIconValue(getContext(), category);

        boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
        boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext(), category);

        // Set total distance
        {
            double totalDistance = trackStatistics == null ? Double.NaN : trackStatistics.getTotalDistance();
            Pair<String, String> parts = StringUtils.getDistanceParts(getContext(), totalDistance, metricUnits);

            distanceValue.setText(parts.first);
            distanceUnit.setText(parts.second);
        }

        // Set activity type
        {
            activityLabel.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);

            activitySpinner.setVisibility(trackIconValue != null ? View.VISIBLE : View.GONE);
            activitySpinner.setEnabled(false);
            if (trackIconValue != null) {
                TrackIconUtils.setIconSpinner(activitySpinner, trackIconValue);
            }
        }

        // Set time and start datetime
        if (trackStatistics != null) {
            movingTimeValue.setText(StringUtils.formatElapsedTime(trackStatistics.getMovingTime()));
            totalTimeValueView.setText(StringUtils.formatElapsedTime(trackStatistics.getTotalTime()));
        }

        // Set average speed/pace
        {
            double speed = trackStatistics != null ? trackStatistics.getAverageSpeed() : Double.NaN;
            speedAvgLabel.setText(reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedAvgValue.setText(parts.first);
            speedAvgUnit.setText(parts.second);
        }

        // Set max speed/pace
        {
            double speed = trackStatistics == null ? Double.NaN : trackStatistics.getMaxSpeed();

            speedMaxLabel.setText(reportSpeed ? R.string.stats_max_speed : R.string.stats_fastest_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMaxValue.setText(parts.first);
            speedMaxUnit.setText(parts.second);
        }

        // Set moving speed/pace
        {
            double speed = trackStatistics != null ? trackStatistics.getAverageMovingSpeed() : Double.NaN;

            speedMovingLabel.setText(reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);

            Pair<String, String> parts = StringUtils.getSpeedParts(getContext(), speed, metricUnits, reportSpeed);
            speedMovingValue.setText(parts.first);
            speedMovingUnit.setText(parts.second);
        }
    }
}
