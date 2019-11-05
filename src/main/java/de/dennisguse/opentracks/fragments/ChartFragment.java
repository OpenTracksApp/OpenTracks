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

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ZoomControls;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.EnumSet;

import de.dennisguse.opentracks.ChartView;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.content.SensorDataSetLocation;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.TrackDataType;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.stats.TripStatisticsUpdater;
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

    public static final String CHART_FRAGMENT_TAG = "chartFragment";

    private final ArrayList<double[]> pendingPoints = new ArrayList<>();

    private TrackDataHub trackDataHub;

    // Stats gathered from the received data
    private TripStatisticsUpdater tripStatisticsUpdater;
    private long startTime;

    private boolean metricUnits = true;
    private boolean reportSpeed = true;
    private int recordingDistanceInterval = PreferencesUtils.RECORDING_DISTANCE_INTERVAL_DEFAULT;

    // Modes of operation
    private boolean chartByDistance = true;
    private boolean[] chartShow = new boolean[]{true, true, true, true, true, true};

    // UI elements
    private ChartView chartView;
    private ZoomControls zoomControls;

    /**
     * A runnable that will enable/disable zoom controls and orange pointer as appropriate and redraw.
     */
    private final Runnable updateChart = new Runnable() {
        @Override
        public void run() {
            if (!isResumed() || trackDataHub == null) {
                return;
            }

            zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
            zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
            chartView.setShowPointer(isSelectedTrackRecording());
            chartView.invalidate();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Create a chartView here to store data thus won't need to reload all the data on every onStart or onResume.
         */
        chartView = new ChartView(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chart, container, false);
        zoomControls = view.findViewById(R.id.chart_zoom_controls);
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomIn();
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomOut();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        ViewGroup layout = getActivity().findViewById(R.id.chart_view_layout);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.addView(chartView, layoutParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
        checkChartSettings();
        getActivity().runOnUiThread(updateChart);
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
    }

    @Override
    public void onStop() {
        super.onStop();
        ViewGroup layout = getActivity().findViewById(R.id.chart_view_layout);
        layout.removeView(chartView);
    }

    @Override
    public void onTrackUpdated(Track track) {
        if (isResumed()) {
            if (track == null || track.getTripStatistics() == null) {
                startTime = -1L;
                return;
            }
            startTime = track.getTripStatistics().getStartTime();
        }
    }

    @Override
    public void clearTrackPoints() {
        if (isResumed()) {
            tripStatisticsUpdater = startTime != -1L ? new TripStatisticsUpdater(startTime) : null;
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
    public void onSampledInTrackPoint(Location location) {
        if (isResumed()) {
            double[] data = new double[ChartView.NUM_SERIES + 1];
            fillDataPoint(location, data);
            pendingPoints.add(data);
        }
    }

    @Override
    public void onSampledOutTrackPoint(Location location) {
        if (isResumed()) {
            fillDataPoint(location, null);
        }
    }

    @Override
    public void onSegmentSplit(Location location) {
        if (isResumed()) {
            fillDataPoint(location, null);
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

    @Override
    public boolean onMetricUnitsChanged(boolean metric) {
        if (isResumed()) {
            if (metricUnits == metric) {
                return false;
            }
            metricUnits = metric;
            chartView.setMetricUnits(metricUnits);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        chartView.requestLayout();
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean onReportSpeedChanged(boolean speed) {
        if (isResumed()) {
            if (reportSpeed == speed) {
                return false;
            }
            reportSpeed = speed;
            chartView.setReportSpeed(reportSpeed);
            boolean chartShowSpeed = PreferencesUtils.shouldChartShowSpeed(getActivity());
            setSeriesEnabled(ChartView.SPEED_SERIES, chartShowSpeed && reportSpeed);
            setSeriesEnabled(ChartView.PACE_SERIES, chartShowSpeed && !reportSpeed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        chartView.requestLayout();
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean onRecordingGpsAccuracy(int minRequiredAccuracy) {
        // We don't care.
        return false;
    }

    @Override
    public boolean onRecordingDistanceIntervalChanged(int value) {
        if (isResumed()) {
            if (recordingDistanceInterval == value) {
                return false;
            }
            recordingDistanceInterval = value;
            return true;
        }
        return false;
    }

    /**
     * Checks the chart settings.
     */
    private void checkChartSettings() {
        boolean needUpdate = false;
        if (chartByDistance != PreferencesUtils.isChartByDistance(getActivity())) {
            chartByDistance = !chartByDistance;
            chartView.setChartByDistance(chartByDistance);
            reloadTrackDataHub();
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.ELEVATION_SERIES, PreferencesUtils.shouldChartShowElevation(getActivity()))) {
            needUpdate = true;
        }

        boolean chartShowSpeed = PreferencesUtils.shouldChartShowSpeed(getActivity());
        if (setSeriesEnabled(ChartView.SPEED_SERIES, chartShowSpeed && reportSpeed)) {
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.PACE_SERIES, chartShowSpeed && !reportSpeed)) {
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.POWER_SERIES, PreferencesUtils.shouldChartShowPower(getActivity()))) {
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.CADENCE_SERIES, PreferencesUtils.shouldChartShowCadence(getActivity()))) {
            needUpdate = true;
        }
        if (setSeriesEnabled(ChartView.HEART_RATE_SERIES, PreferencesUtils.shouldChartShowHeartRate(getActivity()))) {
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
        trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.TRACKS_TABLE,
                TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
                TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE, TrackDataType.PREFERENCE));
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

    private void zoomIn() {
        chartView.zoomIn();
        zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
        zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
    }

    private void zoomOut() {
        chartView.zoomOut();
        zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
        zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
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
     * Given a location, fill in a data point, an array of double[]. <br>
     * data[0] = time/distance <br>
     * data[1] = elevation <br>
     * data[2] = speed <br>
     * data[3] = pace <br>
     * data[4] = heart rate <br>
     * data[5] = cadence <br>
     * data[6] = power <br>
     *
     * @param location the location
     * @param data     the data point to fill in, can be null
     */
    @VisibleForTesting
    void fillDataPoint(Location location, double[] data) {
        double timeOrDistance = Double.NaN;
        double elevation = Double.NaN;
        double speed = Double.NaN;
        double pace = Double.NaN;

        if (tripStatisticsUpdater != null) {
            tripStatisticsUpdater.addLocation(location, recordingDistanceInterval);
            TripStatistics tripStatistics = tripStatisticsUpdater.getTripStatistics();
            if (chartByDistance) {
                double distance = tripStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
                if (!metricUnits) {
                    distance *= UnitConversions.KM_TO_MI;
                }
                timeOrDistance = distance;
            } else {
                timeOrDistance = tripStatistics.getTotalTime();
            }

            elevation = tripStatisticsUpdater.getSmoothedElevation();
            if (!metricUnits) {
                elevation *= UnitConversions.M_TO_FT;
            }

            speed = tripStatisticsUpdater.getSmoothedSpeed() * UnitConversions.MS_TO_KMH;
            if (!metricUnits) {
                speed *= UnitConversions.KM_TO_MI;
            }
            pace = speed == 0 ? 0.0 : 60.0 / speed;
        }

        double heartRate = Double.NaN;
        double cadence = Double.NaN;
        double power = Double.NaN;
        if (location instanceof SensorDataSetLocation && ((SensorDataSetLocation) location).getSensorDataSet() != null) {
            SensorDataSet sensorDataSet = ((SensorDataSetLocation) location).getSensorDataSet();
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
    void setTripStatisticsUpdater(long time) {
        tripStatisticsUpdater = new TripStatisticsUpdater(time);
    }

    @VisibleForTesting
    void setChartView(ChartView view) {
        chartView = view;
    }

    @VisibleForTesting
    void setMetricUnits(boolean value) {
        metricUnits = value;
    }

    @VisibleForTesting
    void setReportSpeed(boolean value) {
        reportSpeed = value;
    }

    @VisibleForTesting
    void setChartByDistance(boolean value) {
        chartByDistance = value;
    }
}
