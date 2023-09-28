/*
 * Copyright 2009 Google Inc.
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

package de.dennisguse.opentracks.chart;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.TrackDataHub;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.databinding.ChartBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;

/**
 * A fragment to display track chart to the user.
 * ChartFragment uses a {@link TrackStatisticsUpdater} internally and recomputes the {@link TrackStatistics} from the beginning.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class ChartFragment extends Fragment implements TrackDataHub.Listener {

    private static final String KEY_CHART_VIEW_BY_DISTANCE_KEY = "chartViewByDistance";

    public static ChartFragment newInstance(boolean chartByDistance) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_CHART_VIEW_BY_DISTANCE_KEY, chartByDistance);

        ChartFragment chartFragment = new ChartFragment();
        chartFragment.setArguments(bundle);
        return chartFragment;
    }

    private TrackDataHub trackDataHub;

    // Stats gathered from the received data
    private final List<ChartPoint> pendingPoints = new ArrayList<>();
    private String activityTypeLocalized = "";

    // Modes of operation
    private boolean chartByDistance;

    private ChartBinding viewBinding;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
                UnitSystem unitSystem = PreferencesUtils.getUnitSystem();
                if (unitSystem != viewBinding.chartView.getUnitSystem()) {
                    viewBinding.chartView.setUnitSystem(unitSystem);
                    runOnUiThread(() -> {
                        if (isResumed()) {
                            viewBinding.chartView.requestLayout();
                        }
                    });
                }
            }
            if (PreferencesUtils.isKey(R.string.stats_rate_key, key)) {
                boolean reportSpeed = PreferencesUtils.isReportSpeed(activityTypeLocalized);
                if (reportSpeed != viewBinding.chartView.getReportSpeed()) {
                    viewBinding.chartView.setReportSpeed(reportSpeed);
                    viewBinding.chartView.applyReportSpeed();

                    runOnUiThread(() -> {
                        if (isResumed()) {
                            viewBinding.chartView.requestLayout();
                        }
                    });
                }
            }
        }
    };

    /**
     * A runnable that will setFrequency the orange pointer as appropriate and redraw.
     */
    private final Runnable updateChart = new Runnable() {
        @Override
        public void run() {
            if (!isResumed()) {
                return;
            }

            viewBinding.chartView.setShowPointer(isSelectedTrackRecording());
            viewBinding.chartView.invalidate();
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chartByDistance = getArguments().getBoolean(KEY_CHART_VIEW_BY_DISTANCE_KEY, true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = ChartBinding.inflate(inflater, container, false);
        viewBinding.chartView.setChartByDistance(chartByDistance);
        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();

        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        checkChartSettings();
        getActivity().runOnUiThread(updateChart);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onTrackUpdated(Track track) {
        if (isResumed()) {
            if (track == null) {
                activityTypeLocalized = "";
                return;
            }

            activityTypeLocalized = track.getActivityTypeLocalized();
            boolean reportSpeed = PreferencesUtils.isReportSpeed(activityTypeLocalized);
            if (reportSpeed != viewBinding.chartView.getReportSpeed()) {
                viewBinding.chartView.setReportSpeed(reportSpeed);
                viewBinding.chartView.applyReportSpeed();
            }
        }
    }

    @Override
    public void clearTrackPoints() {
        if (isResumed()) {
            pendingPoints.clear();
            viewBinding.chartView.reset();
            runOnUiThread(() -> {
                if (isResumed()) {
                    viewBinding.chartView.resetScroll();
                }
            });
        }
    }

    public void onSampledInTrackPoint(@NonNull TrackPoint trackPoint, @NonNull TrackStatistics trackStatistics) {
        if (isResumed()) {
            ChartPoint point = ChartPoint.create(trackStatistics, trackPoint, trackPoint.getSpeed(), chartByDistance, viewBinding.chartView.getUnitSystem());
            pendingPoints.add(point);
        }
    }

    @Override
    public void onNewTrackPointsDone() {
        if (isResumed()) {
            //Avoid ConcurrentModificationException exception
            viewBinding.chartView.addChartPoints(Collections.unmodifiableList(pendingPoints));
            pendingPoints.clear();
            runOnUiThread(updateChart);
        }
    }

    @Override
    public void clearMarkers() {
        if (isResumed()) {
            viewBinding.chartView.clearMarker();
        }
    }

    @Override
    public void onNewMarker(@NonNull Marker marker) {
        if (isResumed()) {
            viewBinding.chartView.addMarker(marker);
        }
    }

    @Override
    public void onNewMarkersDone() {
        if (isResumed()) {
            runOnUiThread(updateChart);
        }
    }

    /**
     * Checks the chart settings.
     */
    private void checkChartSettings() {
        boolean needUpdate = viewBinding.chartView.applyReportSpeed();
        if (needUpdate) {
            viewBinding.chartView.postInvalidate();
        }
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackDataHubInterface) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this);
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
    @Deprecated
    //TODO Should not be dynamic but instead set while instantiating, i.e., newFragment().
    private synchronized boolean isSelectedTrackRecording() {
        return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
    }

    /**
     * Runs a runnable on the UI thread if possible.
     *
     * @param runnable the runnable
     */
    private void runOnUiThread(Runnable runnable) {
        Activity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            fragmentActivity.runOnUiThread(runnable);
        }
    }
}
