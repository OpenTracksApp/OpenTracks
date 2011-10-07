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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtilsFactory;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.io.file.TempFileCleaner;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * The super activity that embeds our sub activities.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
@SuppressWarnings("deprecation")
public class MyTracks extends TabActivity implements OnTouchListener {
  private TrackDataHub dataHub;

  /**
   * Menu manager.
   */
  private MenuManager menuManager;

  /**
   * Preferences.
   */
  private SharedPreferences preferences;

  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;

  /**
   * Google Analytics tracker
   */
  private GoogleAnalyticsTracker tracker;

  /*
   * Tabs/View navigation:
   */

  private NavControls navControls;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

  /*
   * Recording service interaction:
   */

  private final Runnable serviceBindCallback = new Runnable() {
    @Override
    public void run() {
      synchronized (serviceConnection) {
        ITrackRecordingService service = serviceConnection.getServiceIfBound();
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

  private TrackRecordingServiceConnection serviceConnection;

  /*
   * Application lifetime events:
   * ============================
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "MyTracks.onCreate");
    super.onCreate(savedInstanceState);
    ApiFeatures apiFeatures = ApiFeatures.getInstance();
    if (!SystemUtils.isRelease(this)) {
      apiFeatures.getApiPlatformAdapter().enableStrictMode();
    }

    tracker = GoogleAnalyticsTracker.getInstance();
    // Start the tracker in manual dispatch mode...
    tracker.start(getString(R.string.google_analytics_id), getApplicationContext());
    tracker.setProductVersion("android-mytracks", SystemUtils.getMyTracksVersion(this));
    tracker.trackPageView("/appstart");
    tracker.dispatch();

    providerUtils = MyTracksProviderUtilsFactory.get(this);
    preferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    dataHub = ((MyTracksApplication) getApplication()).getTrackDataHub();
    menuManager = new MenuManager(this);
    serviceConnection = new TrackRecordingServiceConnection(this, serviceBindCallback);

    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(apiFeatures).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec("tab1")
        .setIndicator("Map", res.getDrawable(
            android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MapActivity.class)));
    tabHost.addTab(tabHost.newTabSpec("tab2")
        .setIndicator("Stats", res.getDrawable(R.drawable.menu_stats))
        .setContent(new Intent(this, StatsActivity.class)));
    tabHost.addTab(tabHost.newTabSpec("tab3")
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

    // This will show the eula until the user accepts or quits the app.
    Eula.showEulaRequireAcceptance(this);
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "MyTracks.onStart");
    super.onStart();
    dataHub.start();

    // Ensure that service is running and bound if we're supposed to be recording
    if (ServiceUtils.isRecording(this, null, preferences)) {
      serviceConnection.startAndBind();
    }

    Intent intent = getIntent();
    String action = intent.getAction();
    Uri data = intent.getData();
    if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action))
        && TracksColumns.CONTENT_ITEMTYPE.equals(intent.getType())
        && UriUtils.matchesContentUri(data, TracksColumns.DATABASE_CONTENT_URI)) {
      long trackId = ContentUris.parseId(data);
      dataHub.loadTrack(trackId);
    }
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(TAG, "MyTracks.onResume");
    serviceConnection.bindIfRunning();
    super.onResume();
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(TAG, "MyTracks.onPause");
    super.onPause();
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "MyTracks.onStop");

    dataHub.stop();

    tracker.dispatch();
    tracker.stop();

    // Clean up any temporary track files.
    TempFileCleaner.clean();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "MyTracks.onDestroy");
    serviceConnection.unbind();
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return menuManager.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menuManager.onPrepareOptionsMenu(menu, providerUtils.getLastTrack() != null,
        ServiceUtils.isRecording(this, serviceConnection.getServiceIfBound(), preferences),
        dataHub.isATrackSelected());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return menuManager.onOptionsItemSelected(item)
        ? true
        : super.onOptionsItemSelected(item);
  }

  /*
   * Key events:
   * ===========
   */

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (ServiceUtils.isRecording(this, serviceConnection.getServiceIfBound(), preferences)) {
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
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    Log.d(TAG, "MyTracks.onActivityResult");
    long trackId = dataHub.getSelectedTrackId();
    if (results != null) {
      trackId = results.getLongExtra("trackid", trackId);
    }

    switch (requestCode) {
      case Constants.SHOW_TRACK: {
        if (results != null) {
          if (trackId >= 0) {
            dataHub.loadTrack(trackId);

            // The track list passed the requested action as result code. Hand
            // it off to the onActivityResult for further processing:
            if (resultCode != Constants.SHOW_TRACK) {
              onActivityResult(resultCode, Activity.RESULT_OK, results);
            }
          }
        }
        break;
      }
      case Constants.SHOW_WAYPOINT: {
        if (results != null) {
          final long waypointId = results.getLongExtra("waypointid", -1);
          if (waypointId >= 0) {
            MapActivity map =
                (MapActivity) getLocalActivityManager().getActivity("tab1");
            if (map != null) {
              getTabHost().setCurrentTab(0);
              map.showWaypoint(waypointId);
            }
          }
        }
        break;
      }
      case Constants.WELCOME: {
        CheckUnits.check(this);
        break;
      }

      default: {
        Log.w(TAG, "Warning unhandled request code: " + requestCode);
      }
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
    ITrackRecordingService trackRecordingService = serviceConnection.getServiceIfBound();
    if (trackRecordingService == null) {
      throw new IllegalStateException("The recording service is not bound.");
    }
    try {
      long waypointId = trackRecordingService.insertWaypoint(request);
      if (waypointId >= 0) {
        Toast.makeText(this, R.string.status_statistics_inserted,
            Toast.LENGTH_LONG).show();
      }
      return waypointId;
    } catch (RemoteException e) {
      Toast.makeText(this, R.string.error_unable_to_insert_marker,
          Toast.LENGTH_LONG).show();
      throw e;
    }
  }

  private void startRecordingNewTrack(
      ITrackRecordingService trackRecordingService) {
    try {
      long recordingTrackId = trackRecordingService.startNewTrack();
      // Select the recording track.
      dataHub.loadTrack(recordingTrackId);
      Toast.makeText(this, getString(R.string.status_now_recording),
          Toast.LENGTH_SHORT).show();
      // TODO: We catch Exception, because after eliminating the service process
      // all exceptions it may throw are no longer wrapped in a RemoteException.
    } catch (Exception e) {
      Toast.makeText(this,
          getString(R.string.error_unable_to_start_recording),
          Toast.LENGTH_SHORT).show();
      Log.w(TAG, "Unable to start recording.", e);
    }
  }

  /**
   * Starts the track recording service (if not already running) and binds to
   * it. Starts recording a new track.
   */
  void startRecording() {
    synchronized (serviceConnection) {
      startNewTrackRequested = true;
      serviceConnection.startAndBind();

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
    SharedPreferences sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    long currentTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1);

    ITrackRecordingService trackRecordingService = serviceConnection.getServiceIfBound();
    if (trackRecordingService != null) {
      try {
        trackRecordingService.endCurrentTrack();
      } catch (Exception e) {
        Log.e(TAG, "Unable to stop recording.", e);
      }
    }

    serviceConnection.stop();

    if (currentTrackId > 0) {
      Intent intent = new Intent(MyTracks.this, TrackDetails.class);
      intent.putExtra("trackid", currentTrackId);
      intent.putExtra("hasCancelButton", false);
      startActivity(intent);
    }
  }

  long getSelectedTrackId() {
    return dataHub.getSelectedTrackId();
  }
}
