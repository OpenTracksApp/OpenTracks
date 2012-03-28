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

import static com.google.android.apps.mytracks.Constants.CHART_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.MAP_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.STATS_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * An activity to show the track detail.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
@SuppressWarnings("deprecation")
public class TrackDetailActivity extends TabActivity implements OnTouchListener {
  
  public static final String TRACK_ID = "track_id";
  public static final String WAYPOINT_ID = "waypoint_id";
  
  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;
  private TrackDataHub trackDataHub;
  private MenuManager menuManager;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private NavControls navControls;
  
  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

  /**
   * Callback when {@linkk TrackRecordingServiceConnection} binding changes.
   */
  private final Runnable serviceBindCallback = new Runnable() {
    @Override
    public void run() {
      synchronized (trackRecordingServiceConnection) {
        ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
        if (startNewTrackRequested && service != null) {
          Log.i(TAG, "Starting recording");
          startNewTrackRequested = false;
          startRecordingNewTrack(service);
        } else if (startNewTrackRequested) {
          Log.w(TAG, "Not yet starting recording");
        }
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    trackDataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    menuManager = new MenuManager(this);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, serviceBindCallback);

    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

    // Show the action bar (or nothing at all).
    ApiAdapterFactory.getApiAdapter().showActionBar(this);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec(MAP_TAB_TAG)
        .setIndicator("Map", res.getDrawable(
            android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MapActivity.class)));
    tabHost.addTab(tabHost.newTabSpec(STATS_TAB_TAG)
        .setIndicator("Stats", res.getDrawable(R.drawable.ic_menu_statistics))
        .setContent(new Intent(this, StatsActivity.class)));
    tabHost.addTab(tabHost.newTabSpec(CHART_TAB_TAG)
        .setIndicator("Chart", res.getDrawable(R.drawable.menu_elevation))
        .setContent(new Intent(this, ChartActivity.class)));

    // Hide the tab widget itself. We'll use overlayed prev/next buttons to
    // switch between the tabs:
    tabHost.getTabWidget().setVisibility(View.GONE);

    RelativeLayout layout = new RelativeLayout(this);
    LayoutParams params =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.setLayoutParams(params);
    navControls =
        new NavControls(this, layout,
            getResources().obtainTypedArray(R.array.left_icons),
            getResources().obtainTypedArray(R.array.right_icons),
            changeTab);
    navControls.show();
    tabHost.addView(layout);
    layout.setOnTouchListener(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    trackDataHub.start();

    // Ensure that service is running and bound if we're supposed to be recording
    if (ServiceUtils.isRecording(
        this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences)) {
      trackRecordingServiceConnection.startAndBind();
    }

    Intent intent = getIntent();
    long trackId = intent.getLongExtra(TRACK_ID, -1L);
    if (trackId != -1L) {
      trackDataHub.loadTrack(trackId);
      return;
    }
    
    long waypointId = intent.getLongExtra(WAYPOINT_ID, -1L);
    if (waypointId != -1L) {
      Waypoint waypoint = myTracksProviderUtils.getWaypoint(waypointId);
      trackId = waypoint.getTrackId();
      
      // Request that the waypoint is shown (now or when the right track is loaded).
      showWaypoint(trackId, waypointId);
  
      // Load the right track, if not loaded already.
      trackDataHub.loadTrack(trackId);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackRecordingServiceConnection.bindIfRunning();
  }

  @Override
  protected void onStop() {
    super.onStop();
    trackDataHub.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return menuManager.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MapActivity map = getMapTab();
    boolean isSatelliteView = map != null ? map.isSatelliteView() : false;

    menuManager.onPrepareOptionsMenu(menu, myTracksProviderUtils.getLastTrack() != null,
        ServiceUtils.isRecording(this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences),
        trackDataHub.isATrackSelected(),
        isSatelliteView,
        getTabHost().getCurrentTabTag());

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return menuManager.onOptionsItemSelected(item)
        ? true
        : super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (ServiceUtils.isRecording(this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences)) {
        try {
          insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        } catch (RemoteException e) {
          Log.e(TAG, "Cannot insert statistics marker.", e);
        } catch (IllegalStateException e) {
          Log.e(TAG, "Cannot insert statistics marker.", e);
        }
        return true;
      }
    }

    return super.onTrackballEvent(event);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, final Intent results) {
    if (requestCode != Constants.SHOW_WAYPOINT) {
      Log.d(TAG, "Warning unhandled request code: " + requestCode);
      return;
    }
    if (results != null) {
      long waypointId = results.getLongExtra(WaypointDetails.WAYPOINT_ID_EXTRA, -1L);
      if (waypointId != -1L) {
        MapActivity map = getMapTab();
        if (map != null) {
          getTabHost().setCurrentTab(0);
          map.showWaypoint(waypointId);
        }
      }
    }
  }

  private void showWaypoint(long trackId, long waypointId) {
    MapActivity map =
        (MapActivity) getLocalActivityManager().getActivity("tab1");
    if (map != null) {
      getTabHost().setCurrentTab(0);
      map.showWaypoint(trackId, waypointId);
    } else {
      Log.e(TAG, "Couldnt' get map tab");
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      navControls.show();
    }
    return false;
  }

  /**
   * Inserts a waypoint marker.
   *
   * TODO: Merge with WaypointsList#insertWaypoint.
   *
   * @return Id of the inserted statistics marker.
   * @throws RemoteException If the call on the service failed.
   */
  private long insertWaypoint(WaypointCreationRequest request) throws RemoteException {
    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
    if (trackRecordingService == null) {
      throw new IllegalStateException("The recording service is not bound.");
    }
    try {
      long waypointId = trackRecordingService.insertWaypoint(request);
      if (waypointId >= 0) {
        Toast.makeText(this, R.string.marker_insert_success, Toast.LENGTH_LONG).show();
      }
      return waypointId;
    } catch (RemoteException e) {
      Toast.makeText(this, R.string.marker_insert_error, Toast.LENGTH_LONG).show();
      throw e;
    }
  }

  private void startRecordingNewTrack(
      ITrackRecordingService trackRecordingService) {
    try {
      long recordingTrackId = trackRecordingService.startNewTrack();
      // Select the recording track.
      trackDataHub.loadTrack(recordingTrackId);
      Toast.makeText(this, getString(R.string.track_record_success), Toast.LENGTH_SHORT).show();
      // TODO: We catch Exception, because after eliminating the service process
      // all exceptions it may throw are no longer wrapped in a RemoteException.
    } catch (Exception e) {
      Toast.makeText(this, getString(R.string.track_record_error), Toast.LENGTH_SHORT).show();
      Log.w(TAG, "Unable to start recording.", e);
    }
  }

  /**
   * Starts the track recording service (if not already running) and binds to
   * it. Starts recording a new track.
   */
  void startRecording() {
    synchronized (trackRecordingServiceConnection) {
      startNewTrackRequested = true;
      trackRecordingServiceConnection.startAndBind();

      // Binding was already requested before, it either already happened
      // (in which case running the callback manually triggers the actual recording start)
      // or it will happen in the future
      // (in which case running the callback now will have no effect).
      serviceBindCallback.run();
    }
  }

  /**
   * Stops the track recording service and unbinds from it. Will display a toast
   * "Stopped recording" and pop up the Track Details activity.
   */
  void stopRecording() {
    // Save the track id as the shared preference will overwrite the recording track id.
    SharedPreferences sharedPreferences = getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    long currentTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1);

    ITrackRecordingService trackRecordingService = trackRecordingServiceConnection.getServiceIfBound();
    if (trackRecordingService != null) {
      try {
        trackRecordingService.endCurrentTrack();
      } catch (Exception e) {
        Log.e(TAG, "Unable to stop recording.", e);
      }
    }

    trackRecordingServiceConnection.stop();

    if (currentTrackId > 0) {
      Intent intent = new Intent(this, TrackEditActivity.class)
          .putExtra(TrackEditActivity.SHOW_CANCEL, false)
          .putExtra(TrackEditActivity.TRACK_ID, currentTrackId);
      startActivity(intent);
    }
  }

  long getSelectedTrackId() {
    return trackDataHub.getSelectedTrackId();
  }

  /**
   * Asks the chart tab to show its settings.
   */
  public void showChartSettings() {
    ChartActivity chart = getChartTab();
    if (chart != null) {
      chart.showChartSettingsDialog();
    }
  }

  /**
   * Asks the map tab to show the map in satellite mode.
   */
  public void toggleSatelliteView() {
    MapActivity mapTab = getMapTab();
    if (mapTab != null) {
      mapTab.setSatelliteView(!mapTab.isSatelliteView());
    }
  }

  /**
   * Asks the map tab to jump to the current location.
   */
  public void showMyLocation() {
    MapActivity mapTab = getMapTab();
    if (mapTab != null) {
      mapTab.showMyLocation();
    }
  }

  /**
   * Returns the map tab instance if available, or null otherwise.
   */
  private MapActivity getMapTab() {
    return (MapActivity) getLocalActivityManager().getActivity(MAP_TAB_TAG);
  }

  /**
   * Returns the chart tab instance if available, or null otherwise.
   */
  private ChartActivity getChartTab() {
    return (ChartActivity) getLocalActivityManager().getActivity(CHART_TAB_TAG);
  }
}