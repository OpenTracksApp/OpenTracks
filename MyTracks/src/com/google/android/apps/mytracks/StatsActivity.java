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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.EnumSet;

/**
 * An activity that displays track statistics to the user.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class StatsActivity extends Activity implements TrackDataListener {
  /**
   * A runnable for posting to the UI thread. Will update the total time field.
   */
  private final Runnable updateResults = new Runnable() {
    public void run() {
      if (dataHub != null && dataHub.isRecordingSelected()) {
        utils.setTime(R.id.total_time_register,
            System.currentTimeMillis() - startTime);
      }
    }
  };

  private StatsUtilities utils;
  private UIUpdateThread thread;

  /**
   * The start time of the selected track.
   */
  private long startTime = -1;

  private TrackDataHub dataHub;
  private SharedPreferences preferences;

  /**
   * A thread that updates the total time field every second.
   */
  private class UIUpdateThread extends Thread {

    public UIUpdateThread() {
      super();
      Log.i(TAG, "Created UI update thread");
    }

    @Override
    public void run() {
      Log.i(TAG, "Started UI update thread");
      while (ServiceUtils.isRecording(StatsActivity.this, null, preferences)) {
        runOnUiThread(updateResults);
        try {
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
          Log.w(TAG, "StatsActivity: Caught exception on sleep.", e);
          break;
        }
      }
      Log.w(TAG, "UIUpdateThread finished.");
    }
  }

  /** Called when the activity is first created. */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    preferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    utils = new StatsUtilities(this);

    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(ApiFeatures.getInstance()).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.stats);

    ScrollView sv = ((ScrollView) findViewById(R.id.scrolly));
    sv.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);

    showUnknownLocation();

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    if (metrics.heightPixels > 600) {
      ((TextView) findViewById(R.id.speed_register)).setTextSize(80.0f);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    dataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    dataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.TRACK_UPDATES,
        ListenerDataType.LOCATION_UPDATES,
        ListenerDataType.DISPLAY_PREFERENCES));
  }

  @Override
  protected void onPause() {
    dataHub.unregisterTrackDataListener(this);
    dataHub = null;

    if (thread != null) {
      thread.interrupt();
      thread = null;
    }

    super.onStop();
  }

  @Override
  public boolean onUnitsChanged(boolean metric) {
    // Ignore if unchanged.
    if (metric == utils.isMetricUnits()) return false;

    utils.setMetricUnits(metric);
    updateLabels();

    return true;  // Reload data
  }

  @Override
  public boolean onReportSpeedChanged(boolean displaySpeed) {
    // Ignore if unchanged.
    if (displaySpeed == utils.isReportSpeed()) return false;

    utils.setReportSpeed(displaySpeed);
    updateLabels();

    return true;  // Reload data
  }

  private void updateLabels() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        utils.updateUnits();
        utils.setSpeedLabel(R.id.speed_label, R.string.speed, R.string.pace_label);
        utils.setSpeedLabels();
      }
    });
  }

  /**
   * Updates the given location fields (latitude, longitude, altitude) and all
   * other fields.
   *
   * @param l may be null (will set location fields to unknown)
   */
  private void showLocation(Location l) {
    utils.setAltitude(R.id.elevation_register, l.getAltitude());
    utils.setLatLong(R.id.latitude_register, l.getLatitude());
    utils.setLatLong(R.id.longitude_register, l.getLongitude());
    utils.setSpeed(R.id.speed_register, l.getSpeed() * 3.6);
  }

  private void showUnknownLocation() {
    utils.setUnknown(R.id.elevation_register);
    utils.setUnknown(R.id.latitude_register);
    utils.setUnknown(R.id.longitude_register);
    utils.setUnknown(R.id.speed_register);
  }

  @Override
  public void onSelectedTrackChanged(Track track, boolean isRecording) {
    /*
     * Checks if this activity needs to update live track data or not.
     * If so, make sure that:
     * a) a thread keeps updating the total time
     * b) a location listener is registered
     * c) a content observer is registered
     * Otherwise unregister listeners, observers, and kill update thread.
     */
    final boolean startThread = (thread == null) && isRecording;
    final boolean killThread = (thread != null) && (!isRecording);
    if (startThread) {
      thread = new UIUpdateThread();
      thread.start();
    } else if (killThread) {
      thread.interrupt();
      thread = null;
    }
  }

  @Override
  public void onCurrentLocationChanged(final Location loc) {
    TrackDataHub localDataHub = dataHub;
    if (localDataHub != null && localDataHub.isRecordingSelected()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (loc != null) {
            showLocation(loc);
          } else {
            showUnknownLocation();
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
  public void onProviderStateChange(ProviderState state) {
    switch (state) {
      case DISABLED:
      case NO_FIX:
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            showUnknownLocation();
          }
        });
        break;
    }
  }

  @Override
  public void onTrackUpdated(final Track track) {
    TrackDataHub localDataHub = dataHub;
    final boolean recordingSelected = localDataHub != null && localDataHub.isRecordingSelected();
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (track == null || track.getStatistics() == null) {
          utils.setAllToUnknown();
          return;
        }

        startTime = track.getStatistics().getStartTime();
        if (!recordingSelected) {
          utils.setTime(R.id.total_time_register,
              track.getStatistics().getTotalTime());
          showUnknownLocation();
        }
        utils.setAllStats(track.getStatistics());
      }
    });
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
  public void clearTrackPoints() {
    // We don't care.
  }

  @Override
  public void onNewTrackPoint(Location loc) {
    // We don't care.
  }

  @Override
  public void onSegmentSplit() {
    // We don't care.
  }

  @Override
  public void onSampledOutTrackPoint(Location loc) {
    // We don't care.
  }

  @Override
  public void onNewTrackPointsDone() {
    // We don't care.
  }
}
