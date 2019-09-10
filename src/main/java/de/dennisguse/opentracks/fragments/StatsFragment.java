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

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.EnumSet;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackDetailActivity;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.content.TrackDataHub;
import de.dennisguse.opentracks.content.TrackDataListener;
import de.dennisguse.opentracks.content.TrackDataType;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.ITrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.services.sensors.RemoteSensorManager;
import de.dennisguse.opentracks.stats.TripStatistics;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StatsUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to display track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatsFragment extends Fragment implements TrackDataListener {

    public static final String STATS_FRAGMENT_TAG = "statsFragment";

    private TrackDataHub trackDataHub;
    private Handler handlerUpdateUI;

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    private Location lastLocation = null;
    private TripStatistics lastTripStatistics = null;
    private SensorDataSet sensorDataSet = null;

    private final Runnable updateUIeachSecond = new Runnable() {
        public void run() {
            if (isResumed() && isSelectedTrackRecording()) {
                if (!isSelectedTrackPaused() && lastTripStatistics != null) {
                    StatsUtils.setTotalTimeValue(getActivity(), System.currentTimeMillis() - lastTripStatistics.getStopTime() + lastTripStatistics.getTotalTime());
                    updateSensorDataUI();
                }

                handlerUpdateUI.postDelayed(this, UnitConversions.ONE_SECOND);
            }
        }
    };
    private String category = "";
    private int recordingGpsAccuracy = PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        handlerUpdateUI = new Handler();
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(getContext(), null);
        TrackRecordingServiceConnection.startConnection(getContext(), trackRecordingServiceConnection);

        Spinner activityTypeIcon = getView().findViewById(R.id.stats_activity_type_icon);
        activityTypeIcon.setAdapter(TrackIconUtils.getIconSpinnerAdapter(getActivity(), ""));
        activityTypeIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TrackDetailActivity) getActivity()).chooseActivityType(category);
                }
                return true;
            }
        });
        activityTypeIcon.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    ((TrackDetailActivity) getActivity()).chooseActivityType(category);
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTrackDataHub();
        updateUi(getActivity());
        if (isSelectedTrackRecording()) {
            handlerUpdateUI.post(updateUIeachSecond);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseTrackDataHub();
        handlerUpdateUI.removeCallbacks(updateUIeachSecond);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind();
    }

    @Override
    public void onTrackUpdated(final Track track) {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        lastTripStatistics = track != null ? track.getTripStatistics() : null;
                        category = track != null ? track.getCategory() : "";
                        updateUi(getActivity());
                    }
                }
            });
        }
    }

    @Override
    public void clearTrackPoints() {
        lastLocation = null;
    }

    @Override
    public void onSampledInTrackPoint(Location location) {
        lastLocation = location;
    }

    @Override
    public void onSampledOutTrackPoint(Location location) {
        lastLocation = location;
    }

    @Override
    public void onSegmentSplit(Location location) {
        // We don't care.
    }

    @Override
    public void onNewTrackPointsDone() {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        if (!isSelectedTrackRecording() || isSelectedTrackPaused()) {
                            lastLocation = null;
                        }

                        if (lastLocation != null) {
                            boolean hasFix = !LocationUtils.isLocationOld(lastLocation);
                            boolean hasGoodFix = lastLocation.hasAccuracy() && lastLocation.getAccuracy() < recordingGpsAccuracy;

                            if (!hasFix || !hasGoodFix) {
                                lastLocation = null;
                            }
                        }
                        StatsUtils.setLocationValues(getActivity(), getActivity(), null, lastLocation, isSelectedTrackRecording());
                    }
                }
            });
        }
    }

    @Override
    public void clearWaypoints() {
        // We don't care.
    }

    @Override
    public void onNewWaypoint(Waypoint wpt) {
        // We don't care.
    }

    @Override
    public void onNewWaypointsDone() {
        // We don't care.
    }

    @Override
    public boolean onMetricUnitsChanged(final boolean metric) {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        updateUi(getActivity());
                    }
                }
            });
        }
        return true;
    }

    @Override
    public boolean onReportSpeedChanged(final boolean speed) {
        if (isResumed()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        updateUi(getActivity());
                    }
                }
            });
        }
        return true;
    }

    @Override
    public boolean onRecordingGpsAccuracy(int newValue) {
        recordingGpsAccuracy = newValue;
        return false;
    }

    @Override
    public boolean onRecordingDistanceIntervalChanged(int minRecordingDistance) {
        // We don't care.
        return false;
    }

    /**
     * Resumes the trackDataHub. Needs to be synchronized because trackDataHub can
     * be accessed by multiple threads.
     */
    private synchronized void resumeTrackDataHub() {
        trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
        trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.TRACKS_TABLE,
                TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE, TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE,
                TrackDataType.PREFERENCE));
    }

    /**
     * Pauses the trackDataHub. Needs to be synchronized because trackDataHub can
     * be accessed by multiple threads.
     */
    private synchronized void pauseTrackDataHub() {
        trackDataHub.unregisterTrackDataListener(this);
        trackDataHub = null;
    }

    /**
     * Returns true if the selected track is recording. Needs to be synchronized
     * because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackRecording() {
        return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
    }

    /**
     * Returns true if the selected track is paused. Needs to be synchronized
     * because trackDataHub can be accessed by multiple threads.
     */
    private synchronized boolean isSelectedTrackPaused() {
        return trackDataHub != null && trackDataHub.isSelectedTrackPaused();
    }

    /**
     * Tries to fetch most recent {@link SensorDataSet} {@link RemoteSensorManager}.
     */
    private void updateSensorDataUI() {
        ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
        if (trackRecordingService == null) {
            Log.d(STATS_FRAGMENT_TAG, "Cannot get the track recording service.");
            sensorDataSet = null;
        } else {
            //TODO sensorState = trackRecordingService.getSensorState();
            sensorDataSet = trackRecordingService.getSensorData();
        }

        StatsUtils.setSensorData(getActivity(), getActivity(), sensorDataSet, isSelectedTrackRecording());
    }

    /**
     * Updates the UI.
     */
    private void updateUi(FragmentActivity activity) {
        String trackIconValue = TrackIconUtils.getIconValue(activity, category);
        StatsUtils.setTripStatisticsValues(activity, activity, null, lastTripStatistics, trackIconValue);
        StatsUtils.setLocationValues(activity, activity, null, lastLocation, isSelectedTrackRecording());
        updateSensorDataUI();
    }
}
