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

import com.google.android.apps.mytracks.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.services.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
  private StatsUtilities utils;
  private UIUpdateThread thread;

  /**
   * The start time of the selected track.
   */
  private long startTime = -1;

  /**
   * If true, the statistics for the current segment are shown, otherwise
   * for the full track.
   */
  private boolean showCurrentSegment = false;

  private TrackDataHub dataHub;

  /**
   * A runnable for posting to the UI thread. Will update the total time field.
   */
  private final Runnable updateResults = new Runnable() {
    public void run() {
      if (dataHub.isRecordingSelected()) {
        utils.setTime(R.id.total_time_register,
            System.currentTimeMillis() - startTime);
      }
    }
  };

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
      while (MyTracks.getInstance().isRecording()) {
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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    dataHub = MyTracks.getInstance().getDataHub();
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
  protected void onStart() {
    dataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.TRACK_UPDATES,
        ListenerDataType.WAYPOINT_UPDATES,
        ListenerDataType.LOCATION_UPDATES,
        ListenerDataType.COMPASS_UPDATES,
        ListenerDataType.DISPLAY_PREFERENCES));

    super.onStart();
  }

  @Override
  protected void onStop() {
    dataHub.unregisterTrackDataListener(this);

    if (thread != null) {
      thread.interrupt();
      thread = null;
    }

    super.onStop();
  }

  @Override
  public boolean onUnitsChanged(boolean metric) {
    utils.setMetricUnits(metric);
    updateLabels();

    return true;  // Reload data
  }

  @Override
  public boolean onReportSpeedChanged(boolean displaySpeed) {
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuItem currentSegment = menu.add(0,
        Constants.MENU_CURRENT_SEGMENT, 0, R.string.current_segment);
    currentSegment.setIcon(R.drawable.ic_menu_lastsegment);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem item = menu.findItem(Constants.MENU_CURRENT_SEGMENT);
    if (item != null) {
      item.setTitle(showCurrentSegment
          ? getString(R.string.current_track)
          : getString(R.string.current_segment));
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case Constants.MENU_CURRENT_SEGMENT:
        showCurrentSegment = !showCurrentSegment;
        // TODO: Re-read only the data that interests us
        return true;
    }
    return super.onOptionsItemSelected(item);
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

    if (track == null || track.getStatistics() == null) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          utils.setAllToUnknown();
        }
      });
    }
  }

  @Override
  public void onCurrentLocationChanged(Location loc) {
    if (dataHub.isRecordingSelected()) {
      showLocation(loc);
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
        showUnknownLocation();
        break;
    }
  }

  @Override
  public void onTrackUpdated(final Track track) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        startTime = track.getStatistics().getStartTime();
        if (!dataHub.isRecordingSelected()) {
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
