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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.chart.ChartView;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to display track chart to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class ChartFragment extends Fragment implements TrackDataListener {

    private static final String KEY_CHART_VIEW_BY_DISTANCE_KEY = "chartViewByDistance";

    public static Fragment newInstance(boolean chartByDistance) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_CHART_VIEW_BY_DISTANCE_KEY, chartByDistance);

        ChartFragment chartFragment = new ChartFragment();
        chartFragment.setArguments(bundle);
        return chartFragment;
    }

    private final List<double[]> pendingPoints = new ArrayList<>();

    private TrackDataHub trackDataHub;

    // Stats gathered from the received data
    private TrackStatisticsUpdater trackStatisticsUpdater;
    private long startTime;

    private int recordingDistanceInterval;

    // Modes of operation
    private boolean chartByDistance;
    private final boolean[] chartShow = new boolean[]{true, true, true, true, true, true};

    // UI elements
    private ChartView chartView;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (PreferencesUtils.isKey(getContext(), R.string.stats_units_key, key)) {
                boolean metricUnits = PreferencesUtils.isMetricUnits(getContext());
                if (metricUnits != chartView.getMetricUnits()) {
                    chartView.setMetricUnits(metricUnits);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isResumed()) {
                                chartView.requestLayout();
                            }
                        }
                    });
                }
            }
            if (PreferencesUtils.isKey(getContext(), R.string.stats_rate_key, key)) {
                boolean reportSpeed = PreferencesUtils.isReportSpeed(getContext());
                if (reportSpeed != chartView.getReportSpeed()) {
                    chartView.setReportSpeed(reportSpeed);
                    setSeriesEnabled(ChartView.SPEED_SERIES, reportSpeed);
                    setSeriesEnabled(ChartView.PACE_SERIES, !reportSpeed);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isResumed()) {
                                chartView.requestLayout();
                            }
                        }
                    });
                }
            }

            if (PreferencesUtils.isKey(getContext(), R.string.recording_distance_interval_key, key)) {
                recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(getContext());
            }
        }
    };

    /**
     * A runnable that will set the orange pointer as appropriate and redraw.
     */
    private final Runnable updateChart = new Runnable() {
        @Override
        public void run() {
            if (!isResumed() || trackDataHub == null) {
                return;
            }

            chartView.setShowPointer(isSelectedTrackRecording());
            chartView.invalidate();
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chartByDistance = getArguments().getBoolean(KEY_CHART_VIEW_BY_DISTANCE_KEY, true);

        recordingDistanceInterval = PreferencesUtils.getRecordingDistanceIntervalDefault(getContext());

        // Create a chartView here to store data thus won't need to reload all the data on every onStart or onResume.
        chartView = new ChartView(getContext(), chartByDistance);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chart, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        ViewGroup layout = getView().findViewById(R.id.chart_view_layout);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.addView(chartView, layoutParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
        PreferencesUtils.register(getContext(), sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

        checkChartSettings();
        getActivity().runOnUiThread(updateChart);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        PreferencesUtils.unregister(getContext(), sharedPreferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        ViewGroup layout = getView().findViewById(R.id.chart_view_layout);
        layout.removeView(chartView);
    }

    @Override
    public void onTrackUpdated(Track track) {
        if (isResumed()) {
            if (track == null || track.getTrackStatistics() == null) {
                startTime = -1L;
                return;
            }
            startTime = track.getTrackStatistics().getStartTime_ms();
        }
    }

    @Override
    public void clearTrackPoints() {
        if (isResumed()) {
            trackStatisticsUpdater = startTime != -1L ? new TrackStatisticsUpdater(startTime) : null;
            pendingPoints.clear();
            chartView.reset();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        chartView.resetScroll();
                    }
                }
            });
        }
    }

    @Override
    public void onSampledInTrackPoint(TrackPoint trackPoint) {
        if (isResumed()) {
            double[] data = new double[ChartView.NUM_SERIES + 1];
            fillDataPoint(trackPoint, data);
            pendingPoints.add(data);
        }
    }

    @Override
    public void onSampledOutTrackPoint(TrackPoint trackPoint) {
        if (isResumed()) {
            fillDataPoint(trackPoint, null);
        }
    }

    @Override
    public void onNewTrackPointsDone() {
        if (isResumed()) {
            chartView.addDataPoints(pendingPoints);
            pendingPoints.clear();
            runOnUiThread(updateChart);
        }
    }

    @Override
    public void clearWaypoints() {
        if (isResumed()) {
            chartView.clearWaypoints();
        }
    }

    @Override
    public void onNewWaypoint(Waypoint waypoint) {
        if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
            chartView.addWaypoint(waypoint);
        }
    }

    @Override
    public void onNewWaypointsDone() {
        if (isResumed()) {
            runOnUiThread(updateChart);
        }
    }

    /**
     * Checks the chart settings.
     */
    private void checkChartSettings() {
        boolean needUpdate = false;

        if (setSeriesEnabled(ChartView.SPEED_SERIES, chartView.getReportSpeed())) {
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.PACE_SERIES, !chartView.getReportSpeed())) {
            needUpdate = true;
        }

        if (needUpdate) {
            chartView.postInvalidate();
        }
    }

    /**
     * Sets the series enabled value.
     *
     * @param index the series index
     * @param value the value
     * @return true if changed
     */
    private boolean setSeriesEnabled(int index, boolean value) {
        if (chartShow[index] != value) {
            chartShow[index] = value;
            chartView.setChartValueSeriesEnabled(index, value);
            return true;
        }

        return false;
    }

    /**
     * Resumes the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, true, true, true, true);
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
     * Reloads the trackDataHub.
     * Needs to be synchronized because trackDataHub can be accessed by multiple threads.
     */
    private synchronized void reloadTrackDataHub() {
        if (trackDataHub != null) {
            trackDataHub.reloadDataForListener(this);
        }
    }

    /**
     * Runs a runnable on the UI thread if possible.
     *
     * @param runnable the runnable
     */
    private void runOnUiThread(Runnable runnable) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            fragmentActivity.runOnUiThread(runnable);
        }
    }

    /**
     * Given a trackPoint, fill in a data point, an array of double[]. <br>
     * data[0] = time/distance <br>
     * data[1] = elevation <br>
     * data[2] = speed <br>
     * data[3] = pace <br>
     * data[4] = heart rate <br>
     * data[5] = cadence <br>
     * data[6] = power <br>
     *
     * @param trackPoint the trackPoint
     * @param data       the data point to fill in, can be null
     */
    @VisibleForTesting
    void fillDataPoint(@NonNull TrackPoint trackPoint, double[] data) {
        double timeOrDistance = Double.NaN;
        double elevation = Double.NaN;
        double speed = Double.NaN;
        double pace = Double.NaN;

        if (trackStatisticsUpdater != null) {
            trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
            TrackStatistics trackStatistics = trackStatisticsUpdater.getTrackStatistics();
            if (chartByDistance) {
                double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
                if (!chartView.getMetricUnits()) {
                    distance *= UnitConversions.KM_TO_MI;
                }
                timeOrDistance = distance;
            } else {
                timeOrDistance = trackStatistics.getTotalTime();
            }

            elevation = trackStatisticsUpdater.getSmoothedElevation();
            if (!chartView.getMetricUnits()) {
                elevation *= UnitConversions.M_TO_FT;
            }

            speed = trackStatisticsUpdater.getSmoothedSpeed() * UnitConversions.MS_TO_KMH;
            if (!chartView.getMetricUnits()) {
                speed *= UnitConversions.KM_TO_MI;
            }
            pace = speed == 0 ? 0.0 : 60.0 / speed;
        }

        double heartRate = Double.NaN;
        double cadence = Double.NaN;
        double power = Double.NaN;
        if (trackPoint.getSensorDataSet() != null) {
            SensorDataSet sensorDataSet = trackPoint.getSensorDataSet();
            if (sensorDataSet.hasHeartRate()) {
                heartRate = sensorDataSet.getHeartRate();
            }
            if (sensorDataSet.hasCadence()) {
                cadence = sensorDataSet.getCadence();
            }
            if (sensorDataSet.hasPower()) {
                power = sensorDataSet.getPower();
            }
        }

        //TODO: Is related to ChartView.ELEVATION_SERIES etc.
        if (data != null) {
            data[0] = timeOrDistance;
            data[1] = elevation;
            data[2] = speed;
            data[3] = pace;
            data[4] = heartRate;
            data[5] = cadence;
            data[6] = power;
        }
    }

    @VisibleForTesting
    void setTrackStatisticsUpdater(long time) {
        trackStatisticsUpdater = new TrackStatisticsUpdater(time);
    }

    @VisibleForTesting
    void setChartView(ChartView view) {
        chartView = view;
    }

    @VisibleForTesting
    void setMetricUnits(boolean value) {
        chartView.setMetricUnits(value);
    }

    @VisibleForTesting
    void setReportSpeed(boolean value) {
        chartView.setReportSpeed(value);
    }

    @VisibleForTesting
    void setChartByDistance(boolean value) {
        chartByDistance = value;
    }
}
