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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.TrackDataType;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.EnumSet;

/**
 * A fragment to display track statistics to the user.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatsFragment extends Fragment implements TrackDataListener {

  public static final String STATS_FRAGMENT_TAG = "statsFragment";

  private static final String TAG = StatsFragment.class.getSimpleName();

  private TrackDataHub trackDataHub;
  private UiUpdateThread uiUpdateThread;

  private Location lastLocation = null;
  private TripStatistics lastTripStatistics = null;

  // A runnable to update the total time field.
  private final Runnable updateTotalTime = new Runnable() {
    public void run() {
      FragmentActivity activity = getActivity();
      if (activity != null && lastTripStatistics != null && isSelectedTrackRecording()
          && !isSelectedTrackPaused()) {
        StatsUtils.setTotalTimeValue(activity, System.currentTimeMillis()
            - lastTripStatistics.getStopTime() + lastTripStatistics.getTotalTime());
      }
    }
  };

  /**
   * A thread that updates the total time field every second.
   */
  private class UiUpdateThread extends Thread {
    @Override
    public void run() {
      Log.d(TAG, "UI update thread started");
      while (isSelectedTrackRecording()) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
          activity.runOnUiThread(updateTotalTime);
        }
        try {
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
          Log.d(TAG, "UI update thread caught exception", e);
          break;
        }
      }
      Log.d(TAG, "UI update thread finished");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.stats, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    updateUi(getActivity());
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeTrackDataHub();
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
    if (uiUpdateThread != null) {
      uiUpdateThread.interrupt();
      uiUpdateThread = null;
    }
  }

  @Override
  public void onLocationStateChanged(LocationState state) {
    if (isResumed() && state != LocationState.GOOD_FIX) {
      final FragmentActivity activity = getActivity();
      activity.runOnUiThread(new Runnable() {
          @Override
        public void run() {
          lastLocation = null;
          StatsUtils.setLocationValues(activity, lastLocation, true);
        }
      });
    }
  }

  @Override
  public void onLocationChanged(final Location location) {
    if (isResumed()) {
      final FragmentActivity activity = getActivity();
      activity.runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (isSelectedTrackRecording() && !isSelectedTrackPaused()) {
            lastLocation = location;
            StatsUtils.setLocationValues(activity, location, true);
          } else {
            if (lastLocation != null) {
              lastLocation = null;
              StatsUtils.setLocationValues(activity, lastLocation, true);
            }
          }
        }
      });
    }
  }

  @Override
  public void onHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public void onSelectedTrackChanged(Track track) {
    if (isResumed()) {
      if (uiUpdateThread != null) {
        uiUpdateThread.interrupt();
        uiUpdateThread = null;
      }
      if (isSelectedTrackRecording()) {
        uiUpdateThread = new UiUpdateThread();
        uiUpdateThread.start();
      }
    }
  }

  @Override
  public void onTrackUpdated(final Track track) {
    if (isResumed()) {
      final FragmentActivity activity = getActivity();
      activity.runOnUiThread(new Runnable() {
          @Override
        public void run() {
          lastTripStatistics = track != null ? track.getTripStatistics() : null;
          updateUi(activity);
        }
      });
    }
  }

  @Override
  public void clearTrackPoints() {
    // We don't care.
  }

  @Override
  public void onSampledInTrackPoint(Location location) {
    // We don't care.
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    // We don't care.
  }

  @Override
  public void onSegmentSplit(Location location) {
    // We don't care.
  }

  @Override
  public void onNewTrackPointsDone() {
    // We don't care.
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
      final FragmentActivity activity = getActivity();
      activity.runOnUiThread(new Runnable() {
          @Override
        public void run() {
          updateUi(activity);
        }
      });
    }
    return true;
  }

  @Override
  public boolean onReportSpeedChanged(final boolean speed) {
    if (isResumed()) {
      final FragmentActivity activity = getActivity();
      activity.runOnUiThread(new Runnable() {
          @Override
        public void run() {
          updateUi(activity);
        }
      });
    }
    return true;
  }

  @Override
  public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because trackDataHub can
   * be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.SELECTED_TRACK,
        TrackDataType.TRACKS_TABLE, TrackDataType.LOCATION, TrackDataType.PREFERENCE));
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
   * Updates the UI.
   */
  private void updateUi(FragmentActivity activity) {
    StatsUtils.setTripStatisticsValues(activity, lastTripStatistics);
    StatsUtils.setLocationValues(activity, lastLocation, true);
  }
}
