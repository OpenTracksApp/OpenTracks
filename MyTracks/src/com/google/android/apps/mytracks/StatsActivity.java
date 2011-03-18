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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtilsImpl;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.services.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity that displays track statistics to the user.
 *
 * @author Sandor Dornbush
 */
public class StatsActivity extends Activity
    implements OnSharedPreferenceChangeListener {

  private StatsUtilities utils;
  private UIUpdateThread thread;

  private ContentObserver observer;

  /**
   * The id of the currently selected track.
   */
  private long selectedTrackId = -1;

  /**
   * The id of the currently recording track.
   */
  private long recordingTrackId = -1;

  /**
   * The start time of the selected track.
   */
  private long startTime = -1;

  /**
   * True if distances should be displayed in metric units (from shared
   * preferences).
   */
  private boolean metricUnits = true;

  /**
   * True if pace should be displayed as dist/time (from shared preferences).
   */
  private boolean displaySpeed = true;

  /**
   * true if activity has resumed and is on top
   */
  private boolean activityOnTop = false;

  /**
   * If true, the statistics for the current segment are shown, otherwise
   * for the full track.
   */
  private boolean showCurrentSegment = false;

  private MyTracksProviderUtils providerUtils;

  /**
   * A runnable for posting to the UI thread. Will update the total time field.
   */
  private final Runnable updateResults = new Runnable() {
    public void run() {
      updateTotalTime();
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
        long sleeptime = 1000;
        runOnUiThread(updateResults);
        try {
          Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
          Log.w(TAG,
              "StatsActivity: Caught exception on sleep.", e);
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

    utils = new StatsUtilities(this);
    providerUtils = new MyTracksProviderUtilsImpl(getContentResolver());
    
    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(ApiFeatures.getInstance()).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.stats);

    Handler contentHandler = new Handler();
    observer = new ContentObserver(contentHandler) {
      @Override
      public void onChange(boolean selfChange) {
        Log.d(TAG, "StatsActivity: ContentObserver.onChange");
        restoreStats();
        super.onChange(selfChange);
      }
    };

    ScrollView sv = ((ScrollView) findViewById(R.id.scrolly));
    sv.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);

    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      selectedTrackId = preferences.getLong(
          getString(R.string.selected_track_key), -1);
      recordingTrackId = preferences.getLong(
          getString(R.string.recording_track_key), -1);
      metricUnits = preferences.getBoolean(
          getString(R.string.metric_units_key), true);
      displaySpeed =
        preferences.getBoolean(getString(R.string.report_speed_key), true);
      checkLiveTrack();
      restoreStats();
      showUnknownLocation();
      preferences.registerOnSharedPreferenceChangeListener(this);
    }
    utils.setMetricUnits(metricUnits);
    utils.setReportSpeed(displaySpeed);
    utils.updateUnits();
    utils.setSpeedLabel(R.id.speed_label, R.string.speed, R.string.pace_label);
    utils.setSpeedLabels();

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    if (metrics.heightPixels > 600) {
      ((TextView) findViewById(R.id.speed_register)).setTextSize(80.0f);
    }
  }

  @Override
  protected void onPause() {
    unregisterLocationListener();
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
    getContentResolver().unregisterContentObserver(observer);
    activityOnTop = false;
    super.onPause();
  }

  @Override
  protected void onResume() {
    activityOnTop = true;
    checkLiveTrack();
    restoreStats();
    showUnknownLocation();
    super.onResume();
  }

  @Override
  public void onSharedPreferenceChanged(
      final SharedPreferences sharedPreferences, final String key) {
    Log.d(TAG,
        "StatsActivity: onSharedPreferences changed " + key);
    if (key != null) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (key.equals(getString(R.string.selected_track_key))) {
            selectedTrackId =
                sharedPreferences.getLong(
                    getString(R.string.selected_track_key),
                    -1);
            checkLiveTrack();
            restoreStats();
            showUnknownLocation();
          } else if (key.equals(getString(R.string.recording_track_key))) {
            recordingTrackId =
                sharedPreferences.getLong(
                    getString(R.string.recording_track_key),
                    -1);
            checkLiveTrack();
            restoreStats();
            showUnknownLocation();
          } else if (key.equals(getString(R.string.metric_units_key))) {
            metricUnits =
                sharedPreferences.getBoolean(
                    getString(R.string.metric_units_key), true);
            utils.setMetricUnits(metricUnits);
            utils.updateUnits();
            restoreStats();
          } else if (key.equals(getString(R.string.report_speed_key))) {
            displaySpeed =
                sharedPreferences.getBoolean(
                    getString(R.string.report_speed_key),
                    true);
            utils.setReportSpeed(displaySpeed);
            utils.updateUnits();
            utils.setSpeedLabel(
                R.id.speed_label, R.string.speed, R.string.pace_label);
            Log.w(TAG, "Setting speed labels");
            utils.setSpeedLabels();
            restoreStats();
          }
        }
      });
    }
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
        restoreStats();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private final LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location l) {
      if (selectedTrackIsRecording()) {
        showLocation(l);
      }
    }
  
    @Override
    public void onProviderDisabled(String provider) {
      // Do nothing
    }
  
    @Override
    public void onProviderEnabled(String provider) {
      // Do nothing
    }
  
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      // Do nothing
    }
  };

  /**
   * Registers to receive location updates from the GPS location provider.
   */
  private void registerLocationListener() {
    LocationManager locationManager =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    if (locationManager != null) {
      LocationProvider gpsProvider =
          locationManager.getProvider(Constants.GPS_PROVIDER);
      if (gpsProvider == null) {
        Toast.makeText(this, getString(R.string.error_no_gps_location_provider),
            Toast.LENGTH_LONG).show();
        return;
      } else {
        Log.d(TAG, "StatsActivity: Using location provider "
            + gpsProvider.getName());
      }
      locationManager.requestLocationUpdates(gpsProvider.getName(),
         0/*minTime*/, 0/*minDist*/, locationListener);
    }
  }

  /**
   * Unregisters all location listener.
   */
  private void unregisterLocationListener() {
    LocationManager locationManager =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    if (locationManager != null) {
      locationManager.removeUpdates(locationListener);
    }
  }

  /**
   * @return true if the selected track is the currently recording track
   */
  private boolean selectedTrackIsRecording() {
    return MyTracks.getInstance().isRecording()
        && selectedTrackId == recordingTrackId;
  }

  /**
   * Reads values for selected tracks from provider and update the UI.
   */
  private void restoreStats() {
    if (selectedTrackId < 0) {
      utils.setAllToUnknown();
      return;
    }

    Track track = providerUtils.getTrack(selectedTrackId);
    if (track == null || track.getStatistics() == null) {
      utils.setAllToUnknown();
      return;
    }

    startTime = track.getStatistics().getStartTime();
    if (!selectedTrackIsRecording()) {
      utils.setTime(R.id.total_time_register,
          track.getStatistics().getTotalTime());
    }
    utils.setAllStats(track.getStatistics());
  }

  /**
   * Checks if this activity needs to update live track data or not.
   * If so, make sure that:
   * a) a thread keeps updating the total time
   * b) a location listener is registered
   * c) a content observer is registered
   * Otherwise unregister listeners, observers, and kill update thread.
   */
  private void checkLiveTrack() {
    final boolean isRecording = selectedTrackIsRecording();
    final boolean startThread =
        (thread == null) && isRecording && activityOnTop;
    final boolean killThread =
        (thread != null) && (!isRecording || !activityOnTop);
    if (startThread) {
      thread = new UIUpdateThread();
      thread.start();
      getContentResolver().registerContentObserver(
          TracksColumns.CONTENT_URI, false, observer);
      getContentResolver().registerContentObserver(
          WaypointsColumns.CONTENT_URI, false, observer);
      registerLocationListener();
    } else if (killThread) {
      thread.interrupt();
      thread = null;
      getContentResolver().unregisterContentObserver(observer);
      unregisterLocationListener();
    }
  }

  public void updateTotalTime() {
    if (selectedTrackIsRecording()) {
      utils.setTime(R.id.total_time_register,
          System.currentTimeMillis() - startTime);
    }
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
}
