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

package de.dennisguse.opentracks.fragments;

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
import de.dennisguse.opentracks.TrackActivityDataHubInterface;
import de.dennisguse.opentracks.chart.ChartPoint;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.databinding.ChartBinding;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * A fragment to display track chart to the user.
 * ChartFragment uses a {@link TrackStatisticsUpdater} internally and recomputes the {@link TrackStatistics} from the beginning.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class ChartFragment extends Fragment implements TrackDataListener {

    private static final String KEY_CHART_VIEW_BY_DISTANCE_KEY = "chartViewByDistance";

    public static ChartFragment newInstance(boolean chartByDistance) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_CHART_VIEW_BY_DISTANCE_KEY, chartByDistance);

        ChartFragment chartFragment = new ChartFragment();
        chartFragment.setArguments(bundle);
        return chartFragment;
    }

    private SharedPreferences sharedPreferences;

    private TrackDataHub trackDataHub;

    // Stats gathered from the received data
    private final List<ChartPoint> pendingPoints = new ArrayList<>();
    private String category = "";

    // Modes of operation
    private boolean chartByDistance;

    private ChartBinding viewBinding;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
                boolean metricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, getContext());
                if (metricUnits != viewBinding.chartView.getMetricUnits()) {
                    viewBinding.chartView.setMetricUnits(metricUnits);
                    runOnUiThread(() -> {
                        if (isResumed()) {
                            viewBinding.chartView.requestLayout();
                        }
                    });
                }
            }
            if (PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
                boolean reportSpeed = PreferencesUtils.isReportSpeed(sharedPreferences, getContext(), category);
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
        sharedPreferences = PreferencesUtils.getSharedPreferences(getContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);

        checkChartSettings();
        getActivity().runOnUiThread(updateChart);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences = null;
    }

    @Override
    public void onTrackUpdated(Track track) {
        if (isResumed()) {
            if (track == null) {
                category = "";
                return;
            }

            category = track.getCategory();
            boolean reportSpeed = PreferencesUtils.isReportSpeed(PreferencesUtils.getSharedPreferences(getContext()), getContext(), category);
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

    public void onSampledInTrackPoint(@NonNull TrackPoint trackPoint, @NonNull TrackStatistics trackStatistics, Speed smoothedSpeed, double smoothedAltitude_m) {
        if (isResumed()) {
            ChartPoint point = new ChartPoint(trackStatistics, trackPoint, smoothedSpeed, smoothedAltitude_m, chartByDistance, viewBinding.chartView.getMetricUnits());
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
        trackDataHub = ((TrackActivityDataHubInterface) getActivity()).getTrackDataHub();
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
