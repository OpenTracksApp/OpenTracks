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
import static com.google.android.apps.mytracks.DialogManager.DIALOG_IMPORT_PROGRESS;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.io.file.GpxImporter;
import com.google.android.apps.mytracks.io.file.TempFileCleaner;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * The super activity that embeds our sub activities.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracks extends TabActivity implements OnTouchListener,
    ProgressIndicator {
  private TrackDataHub dataHub;

  /*
   * Dialogs manager.
   */
  private DialogManager dialogManager;

  /*
   * Menu manager.
   */
  private MenuManager menuManager;

  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  private ITrackRecordingService trackRecordingService;

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;

  private SharedPreferences sharedPreferences;

  /**
   * The connection to the track recording service.
   */
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.d(Constants.TAG, "MyTracks: Service now connected.");
      // Delay setting the service until we are done with initialization.
      ITrackRecordingService trackRecordingService =
          ITrackRecordingService.Stub.asInterface(service);
      try {
        // TODO: Send a start service intent and broadcast service started
        // message to avoid the hack below and a race condition.
        if (startNewTrackRequested) {
          startNewTrackRequested = false;
          startRecordingNewTrack(trackRecordingService);
        }
      } finally {
        MyTracks.this.trackRecordingService = trackRecordingService;
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.d(TAG, "MyTracks: Service now disconnected.");
      trackRecordingService = null;
    }
  };

  /**
   * Whether {@link #serviceConnection} is bound or not.
   */
  private boolean isBound = false;

  /*
   * Tabs/View navigation:
   */

  private NavControls navControls;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

  /**
   * Checks whether we have a track recording session in progress.
   * In some cases, when the service has crashed or has been restarted
   * by the system, we fall back to the shared preferences.
   *
   * @return true if the activity is bound to the track recording service and
   *         the service is recording a track or in case the service is down,
   *         based on settings from the shared preferences.
   */
  public boolean isRecording() {
    if (trackRecordingService == null) {
      // Fall back to alternative check method.
      return dataHub.isRecording();
    }
    try {
      return trackRecordingService.isRecording();
      // TODO: We catch Exception, because after eliminating the service process
      // all exceptions it may throw are no longer wrapped in a RemoteException.
    } catch (Exception e) {
      Log.e(TAG, "MyTracks: Remote exception.", e);

      // Fall back to alternative check method.
      return dataHub.isRecording();
    }
  }

  /*
   * Application lifetime events:
   * ============================
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "MyTracks.onCreate");
    super.onCreate(savedInstanceState);
    instance = this;
    ApiFeatures apiFeatures = ApiFeatures.getInstance();
    if (!SystemUtils.isRelease(this)) {
      apiFeatures.getApiPlatformAdapter().enableStrictMode();
    }

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    dataHub = new TrackDataHub(this, sharedPreferences, providerUtils);
    menuManager = new MenuManager(this);
    dialogManager = new DialogManager(this);

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

    // Check if we got invoked via the VIEW intent:
    Intent intent = getIntent();
    String action;
    if (intent != null && (action = intent.getAction()) != null) {
      if (action.equals(Intent.ACTION_MAIN)) {
        // Do nothing.
      } else if (action.equals(Intent.ACTION_VIEW)) {
        if (intent.getScheme() != null && intent.getScheme().equals("file")) {
          Log.w(TAG, "Received a VIEW intent with file scheme. Importing.");
          importGpxFile(intent.getData().getPath());
        } else {
          Log.w(TAG, "Received a VIEW intent with unsupported scheme " + intent.getScheme());
        }
      } else {
        Log.w(TAG, "Received an intent with unsupported action " + action);
      }
    } else {
      Log.d(TAG, "Received an intent with no action.");
    }
  }

  private void importGpxFile(final String fileName) {
    dialogManager.showDialogSafely(DIALOG_IMPORT_PROGRESS);
    Thread t = new Thread() {
      @Override
      public void run() {
        int message = R.string.success;

        long[] trackIdsImported = null;

        try {
          try {
            InputStream is = new FileInputStream(fileName);
            trackIdsImported = GpxImporter.importGPXFile(is, providerUtils);
          } catch (SAXException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (ParserConfigurationException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (IOException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_unable_to_read_file;
          } catch (NullPointerException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_invalid_gpx_format;
          } catch (OutOfMemoryError e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_out_of_memory;
          }
          if (trackIdsImported != null && trackIdsImported.length > 0) {
            // select last track from import file
            dataHub.loadTrack(trackIdsImported[trackIdsImported.length - 1]);
          } else {
            dialogManager.showMessageDialog(message, false/* success */);
          }
        } finally {
          runOnUiThread(new Runnable() {
            public void run() {
              dismissDialog(DIALOG_IMPORT_PROGRESS);
            }
          });
        }
      }
    };
    t.start();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "MyTracks.onDestroy");

    dataHub.destroy();

    tryUnbindTrackRecordingService();
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "MyTracks.onStop");

    dataHub.stop();

    // Clean up any temporary track files.
    TempFileCleaner.clean();
    super.onStop();
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(TAG, "MyTracks.onPause");
    tryUnbindTrackRecordingService();
    super.onPause();
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(TAG, "MyTracks.onResume");
    tryBindTrackRecordingService();
    super.onResume();
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "MyTracks.onStart");
    super.onStart();
    dataHub.start();

    // Ensure that service is running if we're supposed to be recording
    if (dataHub.isRecording()) {
      Intent startIntent = new Intent(this, TrackRecordingService.class);
      startService(startIntent);
    }
  }

  /*
   * Menu events:
   * ============
   */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return menuManager.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menuManager.onPrepareOptionsMenu(menu, providerUtils.getLastTrack() != null,
        isRecording(), dataHub.isATrackSelected());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return menuManager.onOptionsItemSelected(item)
        ? true
        : super.onOptionsItemSelected(item);
  }

  /*
   * Dialog events:
   * ==============
   */

  @Override
  protected Dialog onCreateDialog(int id, Bundle args) {
    return dialogManager.onCreateDialog(id, args);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return dialogManager.onCreateDialog(id, null);
  }

  /*
   * Key events:
   * ===========
   */

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (isRecording()) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
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
            // it off to the onAcitivtyResult for further processing:
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
      case Constants.DELETE_TRACK: {
        if (results != null && resultCode == RESULT_OK) {
          deleteTrack(trackId);
        }
        break;
      }
      case Constants.EDIT_DETAILS: {
        if (results != null && resultCode == RESULT_OK) {
          Intent intent = new Intent(this, TrackDetails.class);
          intent.putExtra("trackid", trackId);
          startActivity(intent);
        }
        break;
      }
      case Constants.CLEAR_MAP: {
        dataHub.unloadCurrentTrack();
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
  // ProgressIndicator implementation

  @Override
  public void setProgressMessage(int resId) {
    dialogManager.setProgressMessage(getString(resId));
  }

  @Override
  public void clearProgressMessage() {
    dialogManager.setProgressMessage("");
  }

  @Override
  public void setProgressValue(final int percent) {
    dialogManager.setProgressValue(percent);
  }

  /**
   * Deletes the track with the given id.
   * Prompts the user if he want to really delete the track first.
   * If the selected track is deleted, the selection will be removed.
   */
  public void deleteTrack(final long trackId) {
    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.track_will_be_permanently_deleted));
    builder.setTitle(getString(R.string.are_you_sure_question));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(getString(R.string.yes),
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int i) {
        dialog.dismiss();
        providerUtils.deleteTrack(trackId);
        if (trackId == dataHub.getSelectedTrackId()) {
          dataHub.unloadCurrentTrack();
        }
      }});
    builder.setNegativeButton(getString(R.string.no),
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int i) {
        dialog.dismiss();
      }
    });
    dialog = builder.create();
    dialog.show();
  }

  /**
   * Inserts a waypoint marker.
   *
   * @return Id of the inserted statistics marker.
   * @throws RemoteException If the call on the service failed.
   */
  public long insertWaypoint(WaypointCreationRequest request) throws RemoteException {
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
  public void startRecording() {
    if (trackRecordingService == null) {
      startNewTrackRequested = true;
      Intent startIntent = new Intent(this, TrackRecordingService.class);
      startService(startIntent);
      tryBindTrackRecordingService();
    } else {
      startRecordingNewTrack(trackRecordingService);
    }
  }

  /**
   * Stops the track recording service and unbinds from it. Will display a toast
   * "Stopped recording" and pop up the Track Details activity.
   */
  public void stopRecording() {
    if (trackRecordingService != null) {
      // Save the track id as the shared preference will overwrite the recording track id.
      long currentTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1);
      try {
        trackRecordingService.endCurrentTrack();
        // TODO: We catch Exception, because after eliminating the service process
        // all exceptions it may throw are no longer wrapped in a RemoteException.
      } catch (Exception e) {
        Log.e(TAG, "Unable to stop recording.", e);
      }

      if (currentTrackId > 0) {
        Intent intent = new Intent(MyTracks.this, TrackDetails.class);
        intent.putExtra("trackid", currentTrackId);
        intent.putExtra("hasCancelButton", false);
        startActivity(intent);
      }
    }
    tryUnbindTrackRecordingService();
    try {
      stopService(new Intent(MyTracks.this, TrackRecordingService.class));
    } catch (SecurityException e) {
      Log.e(TAG, "Encountered a security exception when trying to stop service.", e);
    }
    trackRecordingService = null;
  }
  void clearSelectedTrack() {
    dataHub.unloadCurrentTrack();
  }

  long getSelectedTrackId() {
    return dataHub.getSelectedTrackId();
  }

  /**
   * Binds to track recording service if it is running.
   */
  private void tryBindTrackRecordingService() {
    Log.d(TAG,
        "MyTracks: Trying to bind to track recording service...");
    bindService(new Intent(this, TrackRecordingService.class),
        serviceConnection, 0);
    Log.d(TAG, "MyTracks: ...bind finished!");
    isBound = true;
  }

  /**
   * Tries to unbind the track recording service. Catches exception silently in
   * case service is not registered anymore.
   */
  private void tryUnbindTrackRecordingService() {
    if (isBound) {
      Log.d(TAG, "MyTracks: Trying to unbind from track recording service...");
      try {
        unbindService(serviceConnection);
        Log.d(TAG, "MyTracks: ...unbind finished!");
      } catch (IllegalArgumentException e) {
        Log.d(TAG, "MyTracks: Tried unbinding, but service was not registered.", e);
      }
      isBound = false;
    }
  }

  public DialogManager getDialogManager() {
    return dialogManager;
  }

  // @VisibleForTesting
  long getRecordingTrackId() {
    return sharedPreferences.getLong(getString(R.string.recording_track_key), -1);
  }
  
  // @VisibleForTesting
  SharedPreferences getSharedPreferences() {
    return sharedPreferences;
  }
  
  // @VisibleForTesting
  ITrackRecordingService getTrackRecordingService() {
    return trackRecordingService;
  }

  public TrackDataHub getDataHub() {
    return dataHub;
  }

  // TODO: Remove very soon
  private static MyTracks instance;

  public static MyTracks getInstance() {
    return instance;
  }

  public static void clearInstance() {
    instance = null;
  }
}
