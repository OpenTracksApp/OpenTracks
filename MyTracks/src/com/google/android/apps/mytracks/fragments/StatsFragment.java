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

import com.google.android.apps.mytracks.MyTracksApplication;
import com.google.android.apps.mytracks.StatsUtilities;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import java.util.EnumSet;

/**
 * A fragment to display track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatsFragment extends Fragment implements TrackDataListener {

  private static final String TAG = StatsFragment.class.getSimpleName();

  private StatsUtilities statsUtilities;
  private TrackDataHub trackDataHub;
  private UiUpdateThread uiUpdateThread;

  // The start time of the current track.
  private long startTime = -1L;

  // A runnable to update the total time field.
  private final Runnable updateTotalTime = new Runnable() {
    public void run() {
      if (isRecording()) {
        statsUtilities.setTime(R.id.total_time_register, System.currentTimeMillis() - startTime);
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
      while (ServiceUtils.isRecording(getActivity(), null)) {
        getActivity().runOnUiThread(updateTotalTime);
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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    statsUtilities = new StatsUtilities(getActivity());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.stats, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    ScrollView scrollView = (ScrollView) getActivity().findViewById(R.id.scrolly);
    scrollView.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);
    updateLabels();
    setLocationUnknown();
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
  public void onProviderStateChange(ProviderState state) {
    if (state == ProviderState.DISABLED || state == ProviderState.NO_FIX) {
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setLocationUnknown();
        }
      });
    }
  }

  @Override
  public void onCurrentLocationChanged(final Location location) {
    if (isRecording()) {
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (location != null) {
            setLocation(location);
          } else {
            setLocationUnknown();
          }
        }
      });
    }
  }

  @Override
  public void onCurrentHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public void onSelectedTrackChanged(Track track, boolean isRecording) {
    if (uiUpdateThread == null && isRecording) {
      uiUpdateThread = new UiUpdateThread();
      uiUpdateThread.start();
    } else if (uiUpdateThread != null && !isRecording) {
      uiUpdateThread.interrupt();
      uiUpdateThread = null;
    }
  }

  @Override
  public void onTrackUpdated(final Track track) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (track == null || track.getStatistics() == null) {
          statsUtilities.setAllToUnknown();
          return;
        }

        startTime = track.getStatistics().getStartTime();
        if (!isRecording()) {
          statsUtilities.setTime(R.id.total_time_register, track.getStatistics().getTotalTime());
          setLocationUnknown();
        }
        statsUtilities.setAllStats(track.getStatistics());
      }
    });
  }

  @Override
  public void clearTrackPoints() {
    // We don't care.
  }

  @Override
  public void onNewTrackPoint(Location loc) {
    // We don't care.
  }

  @Override
  public void onSampledOutTrackPoint(Location loc) {
    // We don't care.
  }

  @Override
  public void onSegmentSplit() {
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
  public boolean onUnitsChanged(boolean metric) {
    if (statsUtilities.isMetricUnits() == metric) {
      return false;
    }
    statsUtilities.setMetricUnits(metric);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        updateLabels();
      }
    });
    return true;
  }

  @Override
  public boolean onReportSpeedChanged(boolean speed) {
    if (statsUtilities.isReportSpeed() == speed) {
      return false;
    }
    statsUtilities.setReportSpeed(speed);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        updateLabels();
      }
    });
    return true;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because trackDataHub can
   * be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((MyTracksApplication) getActivity().getApplication()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.TRACK_UPDATES,
        ListenerDataType.LOCATION_UPDATES,
        ListenerDataType.DISPLAY_PREFERENCES));
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
   * Returns true if recording. Needs to be synchronized because trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized boolean isRecording() {
    return trackDataHub != null && trackDataHub.isRecordingSelected();
  }

  /**
   * Updates the labels.
   */
  private void updateLabels() {
    statsUtilities.updateUnits();
    statsUtilities.setSpeedLabel(R.id.speed_label, R.string.stat_speed, R.string.stat_pace);
    statsUtilities.setSpeedLabels();
  }

  /**
   * Sets the current location.
   *
   * @param location the current location
   */
  private void setLocation(Location location) {
    statsUtilities.setAltitude(R.id.elevation_register, location.getAltitude());
    statsUtilities.setLatLong(R.id.latitude_register, location.getLatitude());
    statsUtilities.setLatLong(R.id.longitude_register, location.getLongitude());
    statsUtilities.setSpeed(R.id.speed_register, location.getSpeed() * UnitConversions.MS_TO_KMH);
  }

  /**
   * Sets the current location to unknown.
   */
  private void setLocationUnknown() {
    statsUtilities.setUnknown(R.id.elevation_register);
    statsUtilities.setUnknown(R.id.latitude_register);
    statsUtilities.setUnknown(R.id.longitude_register);
    statsUtilities.setUnknown(R.id.speed_register);
  }
}
